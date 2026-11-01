package nl.gzmn.gZMNBuildtools.modules.splines.engine;

import nl.gzmn.gZMNBuildtools.shared.params.BrushArguments;
import nl.gzmn.gZMNBuildtools.shared.params.BrushParam;
import nl.gzmn.gZMNBuildtools.shared.params.ParamSpec;
import nl.gzmn.gZMNBuildtools.shared.shapes.BlockSink;
import nl.gzmn.gZMNBuildtools.shared.shapes.PartSink;
import nl.gzmn.gZMNBuildtools.shared.shapes.Splines;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** What /gzspline builds along the curve through your points. */
public enum SplineType implements ParamSpec {

    TUBE("Round tube: pipes, cables, tunnels (hollow=1)",
            new BrushParam("radius", 1.5, 0.5, 16, "tube radius"),
            new BrushParam("hollow", 0, 0, 1, "1 = hollow")),
    WALL("Wall rising from the curve",
            new BrushParam("height", 4, 1, 64, "wall height"),
            new BrushParam("thickness", 1, 1, 8, "wall thickness")),
    ROAD("Road following the curve, cutting through hills and filling dips",
            new BrushParam("width", 3, 1, 16, "road width"),
            new BrushParam("clear", 3, 0, 16, "air cleared above the road"),
            new BrushParam("fill", 2, 0, 16, "support filled in below the road"));

    private final String description;
    private final List<BrushParam> params;

    SplineType(String description, BrushParam... params) {
        this.description = description;
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
        return List.of("pattern");
    }

    @Override
    public List<BrushParam> params() {
        return params;
    }

    @Override
    public String usage() {
        StringBuilder sb = new StringBuilder("/gzspline build ").append(id()).append(" <pattern>");
        for (BrushParam p : params) {
            sb.append(" [").append(p.name()).append('=').append(p.display(p.defaultValue())).append(']');
        }
        return sb.toString();
    }

    public static Optional<SplineType> byId(String id) {
        return Arrays.stream(values()).filter(t -> t.id().equalsIgnoreCase(id)).findFirst();
    }

    /** Builds this shape along the sampled curve; road parts use {@link Splines} part numbers. */
    public void build(BrushArguments<SplineType> args, List<double[]> samples, PartSink sink) {
        BlockSink single = (x, y, z) -> sink.accept(x, y, z, 0, (char) 0);
        switch (this) {
            case TUBE -> Splines.tube(samples, args.get("radius"), args.get("hollow") >= 0.5, single);
            case WALL -> Splines.wall(samples, args.getInt("height"), args.getInt("thickness"), single);
            case ROAD -> Splines.road(samples, args.getInt("width"), args.getInt("clear"), args.getInt("fill"), sink);
        }
    }
}
