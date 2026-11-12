package nl.gzmn.gZMNBuildtools.modules.loft.engine;

import nl.gzmn.gZMNBuildtools.shared.params.BrushParam;
import nl.gzmn.gZMNBuildtools.shared.params.ParamSpec;

import java.util.List;

/** The {@code /gzloft build} command shape: one fill pattern plus its parameters. */
public final class LoftSpec implements ParamSpec {

    public static final LoftSpec INSTANCE = new LoftSpec();

    private static final List<String> PATTERNS = List.of("pattern");
    private static final List<BrushParam> PARAMS = List.of(
            new BrushParam("outline", 1, 0, 1, "1 = surface only"),
            new BrushParam("fill", 0, 0, 1, "1 = solid inside"),
            new BrushParam("smooth", 1, 0, 1, "1 = frames blend between points"),
            new BrushParam("lowpoly", 0, 0, 1, "1 = fewer samples and section segments"),
            new BrushParam("stack-down", 0, 0, 1, "1 = stack each column down to the ground"),
            new BrushParam("density", 3, 1, 8, "curve samples per block"));

    private LoftSpec() {
    }

    @Override
    public String id() {
        return "loft";
    }

    @Override
    public List<String> patternNames() {
        return PATTERNS;
    }

    @Override
    public List<BrushParam> params() {
        return PARAMS;
    }

    @Override
    public String usage() {
        return "/gzloft build <pattern> [outline=1] [fill=0] [smooth=1] [lowpoly=0] [stack-down=0] [density=3]";
    }
}
