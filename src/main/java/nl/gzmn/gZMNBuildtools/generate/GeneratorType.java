package nl.gzmn.gZMNBuildtools.generate;

import nl.gzmn.gZMNBuildtools.brush.BrushParam;
import nl.gzmn.gZMNBuildtools.brush.ParamSpec;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** The shapes offered by {@code /gzgen}. */
public enum GeneratorType implements ParamSpec {

    ARCH("Arch from selection point 1 to point 2", true,
            new BrushParam("height", 8, -256, 256, "rise of the middle above the endpoints"),
            new BrushParam("thickness", 1, 1, 16, "width of the arch")),
    CABLE("Sagging cable/rope from selection point 1 to point 2", true,
            new BrushParam("sag", 4, 0, 256, "how far the middle hangs below the endpoints"),
            new BrushParam("thickness", 1, 1, 16, "width of the cable")),
    TORUS("Flat ring around your position", false,
            new BrushParam("major", 8, 1, 128, "distance from the centre to the middle of the tube"),
            new BrushParam("minor", 2, 0.5, 64, "radius of the tube"),
            new BrushParam("hollow", 0, 0, 1, "1 = hollow")),
    CONE("Cone standing on your position", false,
            new BrushParam("radius", 6, 0.5, 128, "base radius"),
            new BrushParam("height", 12, 1, 256, "height"),
            new BrushParam("hollow", 0, 0, 1, "1 = hollow")),
    DOME("Half sphere on your position", false,
            new BrushParam("radius", 8, 1, 128, "radius"),
            new BrushParam("hollow", 0, 0, 1, "1 = hollow")),
    SPIRAL("Helix rising from your position", false,
            new BrushParam("radius", 6, 1, 128, "radius of the helix"),
            new BrushParam("height", 16, 1, 256, "total height"),
            new BrushParam("turns", 2, 0.25, 64, "number of full turns"),
            new BrushParam("thickness", 1, 1, 16, "width of the helix"));

    private final String description;
    private final boolean usesSelection;
    private final List<BrushParam> params;

    GeneratorType(String description, boolean usesSelection, BrushParam... params) {
        this.description = description;
        this.usesSelection = usesSelection;
        this.params = List.of(params);
    }

    @Override
    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String description() {
        return description;
    }

    /** Whether the shape spans the two points of the cuboid selection (else it uses the player position). */
    public boolean usesSelection() {
        return usesSelection;
    }

    @Override
    public boolean usesPattern() {
        return true;
    }

    @Override
    public List<BrushParam> params() {
        return params;
    }

    @Override
    public String usage() {
        StringBuilder sb = new StringBuilder("/gzgen ").append(id()).append(" <pattern>");
        for (BrushParam p : params) {
            sb.append(" [").append(p.name()).append('=').append(ParamSpec.format(p.defaultValue())).append(']');
        }
        return sb.toString();
    }

    public static Optional<GeneratorType> byId(String id) {
        return Arrays.stream(values()).filter(t -> t.id().equalsIgnoreCase(id)).findFirst();
    }
}
