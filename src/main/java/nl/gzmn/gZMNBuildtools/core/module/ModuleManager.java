package nl.gzmn.gZMNBuildtools.core.module;

import io.papermc.paper.command.brigadier.Commands;
import nl.gzmn.gZMNBuildtools.core.PluginContext;
import nl.gzmn.gZMNBuildtools.core.help.HelpEntry;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Starts modules in dependency order, honouring the config switches. A module
 * that fails is logged and skipped, together with the modules that need it;
 * everything else keeps working.
 */
public final class ModuleManager {

    private final ModuleHost host;
    private final Map<String, GzmnModule> modules = new LinkedHashMap<>();
    private final Map<String, ModuleStatus> statuses = new LinkedHashMap<>();
    private final Map<String, Map<Class<?>, Object>> services = new HashMap<>();
    private final Map<String, ModuleContext> contexts = new HashMap<>();
    private final List<Consumer<Commands>> commandRegistrars = new ArrayList<>();
    private final List<HelpEntry> help = new ArrayList<>();
    private final List<String> enableOrder = new ArrayList<>();

    public ModuleManager(ModuleHost host) {
        this.host = host;
    }

    public ModuleManager add(GzmnModule module) {
        if (modules.putIfAbsent(module.id(), module) != null) {
            throw new IllegalArgumentException("Duplicate module id " + module.id());
        }
        return this;
    }

    /** Enables every module that is switched on and has its dependencies running. */
    public void enableAll() {
        for (GzmnModule module : order()) {
            if (statuses.containsKey(module.id())) {
                continue;
            }
            enable(module);
        }
    }

    /** Dependency-first order; modules in a cycle or with unknown dependencies are marked failed. */
    List<GzmnModule> order() {
        List<GzmnModule> ordered = new ArrayList<>();
        Set<String> done = new HashSet<>();
        for (GzmnModule module : modules.values()) {
            visit(module, done, new HashSet<>(), ordered);
        }
        return ordered;
    }

    private boolean visit(GzmnModule module, Set<String> done, Set<String> path, List<GzmnModule> ordered) {
        if (done.contains(module.id())) {
            return statuses.get(module.id()) == null;
        }
        if (!path.add(module.id())) {
            fail(module, "dependency cycle through " + String.join(" -> ", path));
            return false;
        }
        boolean ok = true;
        for (String dependency : module.dependencies()) {
            GzmnModule needed = modules.get(dependency);
            if (needed == null) {
                fail(module, "unknown dependency '" + dependency + "'");
                ok = false;
            } else if (!visit(needed, done, path, ordered)) {
                ok = false;
            }
        }
        path.remove(module.id());
        done.add(module.id());
        ordered.add(module);
        return ok;
    }

    private void fail(GzmnModule module, String reason) {
        statuses.putIfAbsent(module.id(), status(module, ModuleState.FAILED, reason));
    }

    private void enable(GzmnModule module) {
        if (!host.enabledInConfig(module.id())) {
            statuses.put(module.id(), status(module, ModuleState.DISABLED, "turned off in config.yml"));
            return;
        }
        for (String dependency : module.dependencies()) {
            ModuleStatus dep = statuses.get(dependency);
            if (dep == null || dep.state() != ModuleState.ENABLED) {
                statuses.put(module.id(), status(module, ModuleState.MISSING_DEPENDENCY,
                        "needs '" + dependency + "', which is not running"));
                return;
            }
        }
        Context context = new Context(module);
        try {
            module.enable(context);
        } catch (ModuleDisabledException e) {
            cleanUp(module);
            statuses.put(module.id(), status(module, ModuleState.DISABLED, e.getMessage()));
            return;
        } catch (Throwable t) {
            cleanUp(module);
            host.logger().log(Level.SEVERE, "Module '" + module.id() + "' failed to start; it is disabled", t);
            statuses.put(module.id(), status(module, ModuleState.FAILED, String.valueOf(t)));
            return;
        }
        // Only a module that started cleanly contributes anything.
        services.put(module.id(), context.provided);
        context.listeners.forEach(host::registerListener);
        commandRegistrars.addAll(context.registrars);
        help.addAll(context.help);
        contexts.put(module.id(), context);
        enableOrder.add(module.id());
        statuses.put(module.id(), status(module, ModuleState.ENABLED, null));
    }

    private void cleanUp(GzmnModule module) {
        try {
            module.disable();
        } catch (Throwable t) {
            host.logger().log(Level.WARNING, "Module '" + module.id() + "' failed to clean up", t);
        }
    }

    /** Help hub entries of the enabled modules, in the order they were added. */
    public List<HelpEntry> helpEntries() {
        return List.copyOf(help);
    }

    /** Command registrars of the enabled modules, to run in the COMMANDS lifecycle event. */
    public List<Consumer<Commands>> commandRegistrars() {
        return List.copyOf(commandRegistrars);
    }

    public void reloadAll() {
        for (String id : enableOrder) {
            try {
                modules.get(id).reload(contexts.get(id));
            } catch (Throwable t) {
                host.logger().log(Level.WARNING, "Module '" + id + "' failed to reload", t);
            }
        }
    }

    /** Disables enabled modules in reverse start order. */
    public void disableAll() {
        for (int i = enableOrder.size() - 1; i >= 0; i--) {
            cleanUp(modules.get(enableOrder.get(i)));
        }
        enableOrder.clear();
    }

    public List<ModuleStatus> statuses() {
        List<ModuleStatus> out = new ArrayList<>();
        for (GzmnModule module : modules.values()) {
            out.add(statuses.getOrDefault(module.id(), status(module, ModuleState.DISABLED, "not started")));
        }
        return out;
    }

    public boolean isEnabled(String id) {
        ModuleStatus s = statuses.get(id);
        return s != null && s.state() == ModuleState.ENABLED;
    }

    private static ModuleStatus status(GzmnModule module, ModuleState state, String reason) {
        return new ModuleStatus(module.id(), module.description(), module.dependencies(), state, reason);
    }

    private final class Context implements ModuleContext {
        private final GzmnModule module;
        private final Map<Class<?>, Object> provided = new HashMap<>();
        private final List<Listener> listeners = new ArrayList<>();
        private final List<Consumer<Commands>> registrars = new ArrayList<>();
        private final List<HelpEntry> help = new ArrayList<>();

        Context(GzmnModule module) {
            this.module = module;
        }

        @Override
        public JavaPlugin plugin() {
            return host.plugin();
        }

        @Override
        public PluginContext core() {
            return host.core();
        }

        @Override
        public Logger logger() {
            return host.logger();
        }

        @Override
        public void commands(Consumer<Commands> registrar) {
            registrars.add(registrar);
        }

        @Override
        public void listen(Listener listener) {
            listeners.add(listener);
        }

        @Override
        public void help(HelpEntry.Builder entry) {
            help.add(entry.build(module.id()));
        }

        @Override
        public <T> void provide(Class<T> type, T service) {
            provided.put(type, service);
        }

        @Override
        public <T> T require(Class<T> type) {
            for (String dependency : module.dependencies()) {
                Object service = services.getOrDefault(dependency, Map.of()).get(type);
                if (service != null) {
                    return type.cast(service);
                }
            }
            throw new IllegalStateException("Module '" + module.id() + "' needs a " + type.getSimpleName()
                    + " but none of its dependencies " + module.dependencies() + " provides one");
        }
    }
}
