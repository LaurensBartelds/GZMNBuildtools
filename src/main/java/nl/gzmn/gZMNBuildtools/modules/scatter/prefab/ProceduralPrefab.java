package nl.gzmn.gZMNBuildtools.modules.scatter.prefab;

import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;
import nl.gzmn.gZMNBuildtools.modules.scatter.engine.Footprint;
import nl.gzmn.gZMNBuildtools.shared.build.PositionHash;
import nl.gzmn.gZMNBuildtools.shared.shapes.Rocks;
import nl.gzmn.gZMNBuildtools.shared.shapes.TreeStyle;
import nl.gzmn.gZMNBuildtools.shared.shapes.Trees;
import nl.gzmn.gZMNBuildtools.shared.worldedit.BuildEditor;
import nl.gzmn.gZMNBuildtools.shared.worldedit.PartPlacer;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Random;

/** Built-in prefabs that are generated fresh every time: {@code tree:<style>} and {@code rock}. */
public final class ProceduralPrefab implements Prefab {

    private static final List<String> ROCK_MIX = List.of("stone", "stone", "stone", "stone", "andesite", "andesite",
            "cobblestone", "mossy_cobblestone");

    private final String name;
    private final TreeStyle style;
    private final Footprint footprint;

    private ProceduralPrefab(String name, TreeStyle style, Footprint footprint) {
        this.name = name;
        this.style = style;
        this.footprint = footprint;
    }

    /** Names of the built-in prefabs. */
    public static List<String> names() {
        List<String> out = new ArrayList<>();
        TreeStyle.ids().forEach(id -> out.add("tree:" + id));
        out.add("rock");
        return out;
    }

    public static Optional<Prefab> byName(String raw) {
        String name = raw.toLowerCase(Locale.ROOT);
        if (name.equals("rock") || name.equals("boulder")) {
            return Optional.of(new ProceduralPrefab("rock", null, new Footprint(List.of(new int[]{0, 0}), 3, 4)));
        }
        if (name.equals("tree")) {
            name = "tree:oak";
        }
        if (name.startsWith("tree:")) {
            String style = name.substring(5);
            if (!TreeStyle.ids().contains(style)) {
                return Optional.empty();
            }
            TreeStyle s = TreeStyle.valueOf(style.toUpperCase(Locale.ROOT));
            int height = baseHeight(s) + 3;
            double reach = s == TreeStyle.SPRUCE ? 4 : s == TreeStyle.PALM ? 5 : 5.5;
            return Optional.of(new ProceduralPrefab(name, s, new Footprint(List.of(new int[]{0, 0}), reach, height)));
        }
        return Optional.empty();
    }

    private static int baseHeight(TreeStyle style) {
        return switch (style) {
            case SPRUCE -> 10;
            case PALM -> 8;
            case BIRCH -> 7;
            case WILLOW -> 6;
            case DEAD -> 5;
            case OAK -> 6;
        };
    }

    private static String wood(TreeStyle style) {
        return switch (style) {
            case SPRUCE -> "spruce";
            case BIRCH -> "birch";
            case WILLOW -> "dark_oak";
            case PALM -> "jungle";
            case OAK, DEAD -> "oak";
        };
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public Footprint footprint() {
        return footprint;
    }

    @Override
    public int groundLayers() {
        return 0;
    }

    @Override
    public void emit(int x, int y, int z, int turns, boolean flip, long seed, BuildEditor.StateSink sink) {
        Random random = new Random(seed);
        if (style == null) {
            double radius = 1.5 + random.nextDouble() * 1.6;
            Rocks.generate(x, y - 1, z, radius, 0.25 + random.nextDouble() * 0.25, 0.35, 0.3, random.nextLong(),
                    (bx, by, bz) -> sink.accept(bx, by, bz, state(ROCK_MIX.get(
                            (int) (PositionHash.hash01(bx, by, bz, seed) * ROCK_MIX.size())))));
            return;
        }
        String wood = wood(style);
        PartPlacer placer = new PartPlacer(List.<Pattern>of(state(wood + "_log"), state(wood + "_leaves")));
        int height = baseHeight(style) + random.nextInt(4);
        int leaves = style == TreeStyle.SPRUCE ? 3 : 2 + random.nextInt(2);
        Trees.generate(style, x, y, z, height, 1, leaves, random.nextLong(), (bx, by, bz, part, axis) -> {
            BlockState state = placer.state(bx, by, bz, part, axis);
            if (state != null) {
                sink.accept(bx, by, bz, state);
            }
        });
    }

    private static BlockState state(String id) {
        BlockType type = BlockTypes.get("minecraft:" + id);
        return (type == null ? BlockTypes.STONE : type).getDefaultState();
    }
}
