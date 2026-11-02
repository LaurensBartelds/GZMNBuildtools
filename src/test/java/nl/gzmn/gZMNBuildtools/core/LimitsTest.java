package nl.gzmn.gZMNBuildtools.core;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LimitsTest {

    private static Limits limits(String yaml, List<String> problems) throws Exception {
        YamlConfiguration config = new YamlConfiguration();
        config.loadFromString(yaml);
        Limits limits = new Limits();
        problems.addAll(limits.reload(config));
        return limits;
    }

    private static final String CONFIG = """
            limits:
              default:
                radius: 12
                blocks: 100000
              ranks:
                builder:
                  radius: 32
                architect:
                  radius: 64
                  image: 512
            """;

    @Test
    void highestRankWinsAndUnsetLimitsFallBack() throws Exception {
        Limits limits = limits(CONFIG, new java.util.ArrayList<>());

        Limits.Resolved guest = limits.resolve(p -> false);
        assertEquals(12, guest.get(Limits.Kind.RADIUS, 32), "default rank");
        assertEquals(100000, guest.get(Limits.Kind.BLOCKS, 500000));
        assertEquals(256, guest.get(Limits.Kind.IMAGE, 256), "no rank sets it: the module's own setting");
        assertNull(guest.source(Limits.Kind.IMAGE));

        Set<String> builderPerms = Set.of("gzmnbuildtools.limits.builder");
        Limits.Resolved builder = limits.resolve(builderPerms::contains);
        assertEquals(32, builder.get(Limits.Kind.RADIUS, 32));
        assertEquals("builder", builder.source(Limits.Kind.RADIUS));
        assertEquals(100000, builder.get(Limits.Kind.BLOCKS, 500000), "still the default for what builder leaves out");

        Set<String> both = Set.of("gzmnbuildtools.limits.builder", "gzmnbuildtools.limits.architect");
        Limits.Resolved architect = limits.resolve(both::contains);
        assertEquals(64, architect.get(Limits.Kind.RADIUS, 32));
        assertEquals(512, architect.get(Limits.Kind.IMAGE, 256));
        assertEquals(List.of("default", "builder", "architect"), architect.ranks());
    }

    @Test
    void unlimitedLiftsEverything() throws Exception {
        Limits limits = limits(CONFIG, new java.util.ArrayList<>());
        Limits.Resolved admin = limits.resolve(Limits.UNLIMITED_PERMISSION::equals);
        for (Limits.Kind kind : Limits.Kind.values()) {
            assertEquals(Limits.UNLIMITED, admin.get(kind, 10));
        }
        assertTrue(Limits.UNLIMITED < Integer.MAX_VALUE / 2, "safe to add to");
    }

    @Test
    void noConfigMeansModuleSettings() throws Exception {
        Limits limits = limits("other: 1\n", new java.util.ArrayList<>());
        assertEquals(32, limits.resolve(p -> !p.equals(Limits.UNLIMITED_PERMISSION)).get(Limits.Kind.RADIUS, 32));
        assertEquals(30, limits.of(java.util.UUID.randomUUID()).get(Limits.Kind.LOADOUTS, 30),
                "players not seen yet get the default rank");
    }

    @Test
    void reportsMistakes() throws Exception {
        List<String> problems = new java.util.ArrayList<>();
        Limits limits = limits("""
                limits:
                  default:
                    radius: -3
                    colour: 5
                  ranks:
                    unlimited:
                      radius: 99
                    vip:
                      radius: lots
                      blocks: 5000
                """, problems);
        assertEquals(4, problems.size(), problems.toString());
        assertTrue(problems.stream().anyMatch(p -> p.contains("colour")));
        assertTrue(problems.stream().anyMatch(p -> p.contains("reserved")));
        Set<String> vip = Set.of("gzmnbuildtools.limits.vip");
        assertEquals(5000, limits.resolve(vip::contains).get(Limits.Kind.BLOCKS, 1), "good keys still apply");
        assertEquals(32, limits.resolve(vip::contains).get(Limits.Kind.RADIUS, 32), "bad values are ignored");
    }
}
