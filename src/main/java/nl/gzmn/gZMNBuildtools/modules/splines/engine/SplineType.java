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
            new BrushParam("fill", 2, 0, 16, "support filled in below the road")),
    RIVER("River flowing downhill along the curve, carving its valley (pattern = river bed)",
            new BrushParam("width", 7, 2, 32, "water width"),
            new BrushParam("depth", 3, 1, 12, "deepest point of the channel"),
            new BrushParam("sink", 1, 0, 8, "how far the water sits below your points"),
            new BrushParam("valley", 10, 0, 48, "how high ground is cut away above the water"),
            new BrushParam("slope", 1, 0.25, 4, "bank steepness, blocks up per block out"));

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

    /**
     * Builds this shape along the sampled curve; road and river parts use
     * {@link Splines} part numbers.
     *
     * @return for rivers, which way it flows; null for the other shapes
     */
    public Splines.RiverFlow build(BrushArguments<SplineType> args, List<double[]> samples, PartSink sink) {
        BlockSink single = (x, y, z) -> sink.accept(x, y, z, 0, (char) 0);
        switch (this) {
            case TUBE -> Splines.tube(samples, args.get("radius"), args.get("hollow") >= 0.5, single);
            case WALL -> Splines.wall(samples, args.getInt("height"), args.getInt("thickness"), single);
            case ROAD -> Splines.road(samples, args.getInt("width"), args.getInt("clear"), args.getInt("fill"), sink);
            case RIVER -> {
                return Splines.river(samples, new Splines.RiverShape(args.get("width"), args.getInt("depth"),
                        args.getInt("sink"), args.getInt("valley"), args.get("slope")), sink);
            }
        }
        return null;
    }
}
