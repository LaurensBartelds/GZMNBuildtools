package nl.gzmn.gZMNBuildtools.modules.terrainbrush.engine;

import nl.gzmn.gZMNBuildtools.shared.params.BrushParam;
import nl.gzmn.gZMNBuildtools.shared.params.ParamSpec;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** The terrain brushes offered by {@code /terrainbrush}, with their parameters. */
public enum TerrainBrushType implements ParamSpec {

    RAISE("Raise terrain with a smooth falloff", Slots.NONE,
            new BrushParam("height", 3, 1, 64, "maximum blocks raised at the centre"),
            new BrushParam("hardness", 0, 0, 0.95, "0 = soft dome, near 1 = flat plateau")),
    LOWER("Lower terrain with a smooth falloff", Slots.NONE,
            new BrushParam("depth", 3, 1, 64, "maximum blocks lowered at the centre"),
            new BrushParam("hardness", 0, 0, 0.95, "0 = soft bowl, near 1 = flat pit")),
    FLATTEN("Pull terrain towards the height of the clicked column", Slots.NONE,
            new BrushParam("strength", 1, 0.05, 1, "fraction of the height difference removed per click"),
            new BrushParam("hardness", 0.5, 0, 0.95, "size of the fully flattened core")),
    SMOOTH("Smooth bumps and hard edges out of terrain", Slots.NONE,
            new BrushParam("kernel", 2, 1, 8, "neighbourhood size averaged over"),
            new BrushParam("iterations", 2, 1, 10, "smoothing passes per click"),
            new BrushParam("strength", 1, 0.05, 1, "how far towards the smoothed height to move")),
    TERRACE("Cut terrain into flat steps", Slots.NONE,
            new BrushParam("step", 3, 1, 32, "height of each terrace step"),
            new BrushParam("offset", 0, 0, 31, "shifts the step levels up"),
            new BrushParam("hardness", 0.4, 0, 0.95, "size of the fully terraced core")),
    ROUGHEN("Add natural noise to the terrain height", Slots.NONE,
            new BrushParam("amplitude", 2, 1, 32, "maximum height change"),
            new BrushParam("scale", 6, 1, 128, "size of the noise features")),
    OVERLAY("Paint the top surface layers with the brush pattern", Slots.FILL,
            new BrushParam("depth", 1, 1, 16, "how many blocks deep to paint")),
    BLOB("Place an organic, noisy blob of the brush pattern", Slots.FILL,
            new BrushParam("roughness", 0.4, 0, 1, "how lumpy the blob is"),
            new BrushParam("frequency", 1, 0.1, 5, "how many lumps there are")),
    SPIKE("Place a tapering spike of the brush pattern", Slots.FILL,
            new BrushParam("height", 12, 1, 128, "length of the spike"),
            new BrushParam("sharpness", 1.5, 0.3, 4, "above 1 = thin needle, below 1 = blunt cone"),
            new BrushParam("tilt", 0, 0, 1, "random lean as a fraction of the height"),
            new BrushParam("down", 0, 0, 1, "1 = hang downward (stalactite)")),
    TREE("Plant trees (log and leaves patterns); count > 1 scatters a forest", List.of("log", "leaves"),
            BrushParam.choice("style", "tree shape", nl.gzmn.gZMNBuildtools.shared.shapes.TreeStyle.ids()
                    .toArray(String[]::new)),
            new BrushParam("height", 10, 3, 64, "trunk height"),
            new BrushParam("thickness", 1, 1, 5, "trunk radius"),
            new BrushParam("leaves", 3, 1, 10, "size of the leaf clusters"),
            new BrushParam("count", 1, 1, 30, "trees per click, scattered within the radius")),
    ROCK("Place boulders embedded in the ground", Slots.FILL,
            new BrushParam("flatness", 0.35, 0, 0.8, "0 = round, 0.8 = flat slab"),
            new BrushParam("roughness", 0.35, 0, 1, "how lumpy the surface is"),
            new BrushParam("sink", 0.3, 0, 0.8, "how deep it is buried"),
            new BrushParam("count", 1, 1, 20, "rocks per click; their size varies up to the radius"));

    /** Pattern slot lists; nested so the enum constants can use them. */
    private static final class Slots {
        static final List<String> NONE = List.of();
        static final List<String> FILL = List.of("pattern");
    }

    private final String description;
    private final List<String> patterns;
    private final List<BrushParam> params;

    TerrainBrushType(String description, List<String> patterns, BrushParam... params) {
        this.description = description;
        this.patterns = patterns;
        this.params = List.of(params);
    }

    @Override
    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String description() {
        return description;
    }

    @Override
    public List<String> patternNames() {
        return patterns;
    }

    @Override
    public List<BrushParam> params() {
        return params;
    }

    @Override
    public String usage() {
        StringBuilder sb = new StringBuilder("/terrainbrush ").append(id()).append(" <radius>");
        patterns.forEach(name -> sb.append(" <").append(name).append('>'));
        for (BrushParam p : params) {
            sb.append(" [").append(p.name()).append('=').append(p.display(p.defaultValue())).append(']');
        }
        return sb.toString();
    }

    public static Optional<TerrainBrushType> byId(String id) {
        return Arrays.stream(values()).filter(t -> t.id().equalsIgnoreCase(id)).findFirst();
    }
}
