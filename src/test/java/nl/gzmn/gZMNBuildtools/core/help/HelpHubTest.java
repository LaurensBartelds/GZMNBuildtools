package nl.gzmn.gZMNBuildtools.core.help;

import nl.gzmn.gZMNBuildtools.common.AdventureMessages;
import org.bukkit.Material;
import org.bukkit.permissions.Permissible;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HelpHubTest {

    private static final HelpEntry GRADIENT = HelpEntry.of(HelpEntry.Category.BRUSHES, Material.BRUSH, "/gb",
            "Smart gradient brush", "Paints a colour gradient over the build you click.")
            .permission("gzmnbuildtools.brush").runs().build("gradient-brush");
    private static final HelpEntry TEXTURE = HelpEntry.of(HelpEntry.Category.TERRAIN, Material.MOSS_BLOCK, "/gztexture",
            "Terrain texturing", "Repaint terrain with a ready-made look.").build("terrain-texture");
    private static final HelpEntry LIMITS = HelpEntry.of(HelpEntry.Category.OTHER, Material.CLOCK, "/gzlimits",
            "Your limits", "See your limits.").runs().build("core");
    private static final HelpEntry SCATTER = HelpEntry.of(HelpEntry.Category.TERRAIN, Material.OAK_SAPLING,
            "/gzscatter", "Prefab scatter", "Scatter trees and rocks.").example("/gzscatter tree:oak 8")
            .permission("gzmnbuildtools.scatter").build("prefab-scatter");

    /** A permission holder that has exactly the given permissions. */
    private static Permissible holding(String... permissions) {
        Set<String> has = Set.of(permissions);
        return (Permissible) Proxy.newProxyInstance(Permissible.class.getClassLoader(), new Class<?>[]{Permissible.class},
                (proxy, method, args) -> method.getName().equals("hasPermission") ? has.contains(String.valueOf(args[0]))
                        : null);
    }

    @Test
    void groupsByCategoryAndHidesWhatYouCannotUse() {
        HelpHub hub = new HelpHub(new AdventureMessages(false), () -> List.of(LIMITS, SCATTER, TEXTURE, GRADIENT));
        assertEquals(List.of(GRADIENT, SCATTER, TEXTURE, LIMITS),
                hub.visible(holding("gzmnbuildtools.brush", "gzmnbuildtools.scatter")));
        assertEquals(List.of(TEXTURE, LIMITS), hub.visible(holding()));
    }

    @Test
    void searchesCommandsTitlesAndTranslations() {
        List<HelpEntry> all = List.of(GRADIENT, TEXTURE, LIMITS, SCATTER);
        Map<String, String> dutch = Map.of("Terrain texturing", "Terrein textureren");
        assertEquals(List.of(TEXTURE), HelpHub.search(all, "texture", t -> t));
        assertEquals(List.of(TEXTURE), HelpHub.search(all, "terrein", t -> dutch.getOrDefault(t, t)));
        assertEquals(List.of(TEXTURE, SCATTER), HelpHub.search(all, "terrain", t -> t), "so do categories");
        assertEquals(List.of(SCATTER), HelpHub.search(all, "trees oak", t -> t), "every word must match");
        assertTrue(HelpHub.search(all, "teleport", t -> t).isEmpty());
    }

    @Test
    void clickingSuggestsTheExampleOrTheCommand() {
        assertEquals("/gzscatter tree:oak 8", SCATTER.suggestion());
        assertEquals("/gztexture ", TEXTURE.suggestion());
    }

    @Test
    void wrapsLongDescriptionsForItemLore() {
        List<String> lines = HelpMenu.wrap("Swap a build to another material with a brush or in one go, keeping shapes "
                + "and mossy or cracked forms.");
        assertTrue(lines.size() >= 3);
        assertTrue(lines.stream().allMatch(l -> l.length() <= 40), lines.toString());
        assertEquals("Swap a build to another material with a brush or in one go, keeping shapes and mossy or "
                + "cracked forms.", String.join(" ", lines));
    }
}
