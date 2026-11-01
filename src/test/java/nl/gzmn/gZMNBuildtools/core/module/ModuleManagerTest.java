package nl.gzmn.gZMNBuildtools.core.module;

import nl.gzmn.gZMNBuildtools.core.PluginContext;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class ModuleManagerTest {

    /** Host with config switches and a record of registered listeners. */
    static final class FakeHost implements ModuleHost {
        final Set<String> off;
        final List<Listener> listeners = new ArrayList<>();
        final Logger logger = Logger.getLogger("test");

        FakeHost(String... off) {
            this.off = Set.of(off);
            logger.setLevel(java.util.logging.Level.OFF);
        }

        public JavaPlugin plugin() {
            return null;
        }

        public PluginContext core() {
            return null;
        }

        public Logger logger() {
            return logger;
        }

        public boolean enabledInConfig(String id) {
            return !off.contains(id);
        }

        public void registerListener(Listener listener) {
            listeners.add(listener);
        }
    }

    /** A module built from lambdas, recording the order modules start and stop. */
    record TestModule(String id, List<String> dependencies, List<String> log, Consumer<ModuleContext> onEnable)
            implements GzmnModule {
        TestModule(String id, List<String> log, String... deps) {
            this(id, List.of(deps), log, ctx -> {
            });
        }

        public String description() {
            return "test " + id;
        }

        public void enable(ModuleContext context) {
            onEnable.accept(context);
            log.add("enable " + id);
        }

        public void disable() {
            log.add("disable " + id);
        }
    }

    private static ModuleState state(ModuleManager manager, String id) {
        return manager.statuses().stream().filter(s -> s.id().equals(id)).findFirst().orElseThrow().state();
    }

    @Test
    void startsDependenciesFirstAndStopsInReverse() {
        List<String> log = new ArrayList<>();
        ModuleManager manager = new ModuleManager(new FakeHost())
                .add(new TestModule("web", log, "brush"))
                .add(new TestModule("brush", log))
                .add(new TestModule("splines", log));
        manager.enableAll();
        assertEquals(List.of("enable brush", "enable web", "enable splines"), log);
        manager.disableAll();
        assertEquals(List.of("disable splines", "disable web", "disable brush"), log.subList(3, 6));
    }

    @Test
    void switchedOffModulesAndTheirDependentsDoNotStart() {
        List<String> log = new ArrayList<>();
        ModuleManager manager = new ModuleManager(new FakeHost("brush"))
                .add(new TestModule("brush", log))
                .add(new TestModule("web", log, "brush"))
                .add(new TestModule("splines", log));
        manager.enableAll();
        assertEquals(ModuleState.DISABLED, state(manager, "brush"));
        assertEquals(ModuleState.MISSING_DEPENDENCY, state(manager, "web"));
        assertEquals(ModuleState.ENABLED, state(manager, "splines"));
        assertEquals(List.of("enable splines"), log);
    }

    @Test
    void aFailingModuleIsIsolatedAndContributesNothing() {
        List<String> log = new ArrayList<>();
        Listener listener = new Listener() {
        };
        ModuleManager manager = new ModuleManager(new FakeHost())
                .add(new TestModule("broken", List.of(), log, ctx -> {
                    ctx.listen(listener);
                    ctx.commands(c -> {
                    });
                    throw new IllegalStateException("boom");
                }))
                .add(new TestModule("needs-broken", log, "broken"))
                .add(new TestModule("fine", List.of(), log, ctx -> ctx.commands(c -> {
                })));
        manager.enableAll();
        assertEquals(ModuleState.FAILED, state(manager, "broken"));
        assertEquals(ModuleState.MISSING_DEPENDENCY, state(manager, "needs-broken"));
        assertEquals(ModuleState.ENABLED, state(manager, "fine"));
        assertEquals(1, manager.commandRegistrars().size(), "only the healthy module's commands");
        assertTrue(log.contains("disable broken"), "a failed module gets a chance to clean up");
    }

    @Test
    void modulesCanSwitchThemselvesOff() {
        GzmnModule polite = new GzmnModule() {
            public String id() {
                return "polite";
            }

            public String description() {
                return "";
            }

            public void enable(ModuleContext context) throws ModuleDisabledException {
                throw new ModuleDisabledException("web-editor.enabled is false");
            }
        };
        ModuleManager manager = new ModuleManager(new FakeHost()).add(polite);
        manager.enableAll();
        ModuleStatus status = manager.statuses().get(0);
        assertEquals(ModuleState.DISABLED, status.state());
        assertEquals("web-editor.enabled is false", status.reason());
    }

    @Test
    void servicesAreOnlyVisibleThroughDeclaredDependencies() {
        List<String> log = new ArrayList<>();
        Object[] seen = new Object[1];
        ModuleManager manager = new ModuleManager(new FakeHost())
                .add(new TestModule("brush", List.of(), log, ctx -> ctx.provide(CharSequence.class, "service")))
                .add(new TestModule("web", List.of("brush"), log, ctx -> seen[0] = ctx.require(CharSequence.class)))
                .add(new TestModule("sneaky", List.of(), log, ctx -> ctx.require(CharSequence.class)));
        manager.enableAll();
        assertEquals("service", seen[0]);
        assertEquals(ModuleState.FAILED, state(manager, "sneaky"));
    }

    @Test
    void cyclesAndUnknownDependenciesFail() {
        List<String> log = new ArrayList<>();
        ModuleManager manager = new ModuleManager(new FakeHost())
                .add(new TestModule("a", log, "b"))
                .add(new TestModule("b", log, "a"))
                .add(new TestModule("c", log, "nope"))
                .add(new TestModule("d", log));
        manager.enableAll();
        assertNotEquals(ModuleState.ENABLED, state(manager, "a"));
        assertNotEquals(ModuleState.ENABLED, state(manager, "b"));
        assertEquals(ModuleState.FAILED, state(manager, "c"));
        assertEquals(ModuleState.ENABLED, state(manager, "d"));
        assertThrows(IllegalArgumentException.class, () -> manager.add(new TestModule("d", log)));
    }
}
