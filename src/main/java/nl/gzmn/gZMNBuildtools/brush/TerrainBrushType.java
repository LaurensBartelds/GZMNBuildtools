package nl.gzmn.gZMNBuildtools.brush;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** The terrain brushes offered by {@code /terrainbrush}, with their parameters. */
public enum TerrainBrushType implements ParamSpec {

    RAISE("Raise terrain with a smooth falloff", false,
            new BrushParam("height", 3, 1, 64, "maximum blocks raised at the centre"),
            new BrushParam("hardness", 0, 0, 0.95, "0 = soft dome, near 1 = flat plateau")),
    LOWER("Lower terrain with a smooth falloff", false,
            new BrushParam("depth", 3, 1, 64, "maximum blocks lowered at the centre"),
            new BrushParam("hardness", 0, 0, 0.95, "0 = soft bowl, near 1 = flat pit")),
    FLATTEN("Pull terrain towards the height of the clicked column", false,
            new BrushParam("strength", 1, 0.05, 1, "fraction of the height difference removed per click"),
            new BrushParam("hardness", 0.5, 0, 0.95, "size of the fully flattened core")),
    SMOOTH("Smooth bumps and hard edges out of terrain", false,
            new BrushParam("kernel", 2, 1, 8, "neighbourhood size averaged over"),
            new BrushParam("iterations", 2, 1, 10, "smoothing passes per click"),
            new BrushParam("strength", 1, 0.05, 1, "how far towards the smoothed height to move")),
    TERRACE("Cut terrain into flat steps", false,
            new BrushParam("step", 3, 1, 32, "height of each terrace step"),
            new BrushParam("offset", 0, 0, 31, "shifts the step levels up"),
            new BrushParam("hardness", 0.4, 0, 0.95, "size of the fully terraced core")),
    ROUGHEN("Add natural noise to the terrain height", false,
            new BrushParam("amplitude", 2, 1, 32, "maximum height change"),
            new BrushParam("scale", 6, 1, 128, "size of the noise features")),
    OVERLAY("Paint the top surface layers with the brush pattern", true,
            new BrushParam("depth", 1, 1, 16, "how many blocks deep to paint")),
    BLOB("Place an organic, noisy blob of the brush pattern", true,
            new BrushParam("roughness", 0.4, 0, 1, "how lumpy the blob is"),
            new BrushParam("frequency", 1, 0.1, 5, "how many lumps there are")),
    SPIKE("Place a tapering spike of the brush pattern", true,
            new BrushParam("height", 12, 1, 128, "length of the spike"),
            new BrushParam("sharpness", 1.5, 0.3, 4, "above 1 = thin needle, below 1 = blunt cone"),
            new BrushParam("tilt", 0, 0, 1, "random lean as a fraction of the height"),
            new BrushParam("down", 0, 0, 1, "1 = hang downward (stalactite)"));

    private final String description;
    private final boolean usesPattern;
    private final List<BrushParam> params;

    TerrainBrushType(String description, boolean usesPattern, BrushParam... params) {
        this.description = description;
        this.usesPattern = usesPattern;
        this.params = List.of(params);
    }

    @Override
    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String description() {
        return description;
    }

    /** Whether the brush places a fill pattern (as opposed to reshaping existing terrain). */
    @Override
    public boolean usesPattern() {
        return usesPattern;
    }

    @Override
    public List<BrushParam> params() {
        return params;
    }

    @Override
    public String usage() {
        StringBuilder sb = new StringBuilder("/terrainbrush ").append(id()).append(" <radius>");
        if (usesPattern) {
            sb.append(" <pattern>");
        }
        for (BrushParam p : params) {
            sb.append(" [").append(p.name()).append('=').append(ParamSpec.format(p.defaultValue())).append(']');
        }
        return sb.toString();
    }

    public static Optional<TerrainBrushType> byId(String id) {
        return Arrays.stream(values()).filter(t -> t.id().equalsIgnoreCase(id)).findFirst();
    }
}
