package nl.gzmn.gZMNBuildtools.modules.roof.engine;

import nl.gzmn.gZMNBuildtools.shared.params.BrushParam;
import nl.gzmn.gZMNBuildtools.shared.params.ParamSpec;

import java.util.List;

/** The {@code /gzroof} command shape: one fill pattern plus its parameters. */
public final class RoofSpec implements ParamSpec {

    public static final RoofSpec INSTANCE = new RoofSpec();

    private static final List<String> PATTERNS = List.of("pattern");
    private static final List<BrushParam> PARAMS = List.of(
            BrushParam.choice("style", "roof shape", "gable", "hip", "pyramid", "flat"),
            new BrushParam("height", 0, 0, 32, "ridge height, 0 = automatic"),
            new BrushParam("overhang", 1, 0, 8, "blocks the eaves extend past the walls"),
            BrushParam.choice("axis", "ridge direction", "auto", "x", "z"),
            new BrushParam("noise", 0, 0, 4, "random height jitter on the slopes"),
            new BrushParam("stack-down", 0, 0, 1, "1 = fill below the roof down to the walls"),
            new BrushParam("level-ends", 0, 0, 1, "1 = flat level at the gable ends"));

    private RoofSpec() {
    }

    @Override
    public String id() {
        return "roof";
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
        return "/gzroof <pattern> [style=gable] [height=0] [overhang=1] [axis=auto] [noise=0] "
                + "[stack-down=0] [level-ends=0]";
    }
}
