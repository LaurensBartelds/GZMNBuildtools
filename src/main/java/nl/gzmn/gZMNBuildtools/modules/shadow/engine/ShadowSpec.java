package nl.gzmn.gZMNBuildtools.modules.shadow.engine;

import nl.gzmn.gZMNBuildtools.shared.params.BrushParam;
import nl.gzmn.gZMNBuildtools.shared.params.ParamSpec;

import java.util.List;

/** The {@code /gzshadow} command shape: one fill pattern plus its parameters. */
public final class ShadowSpec implements ParamSpec {

    public static final ShadowSpec INSTANCE = new ShadowSpec();

    private static final List<String> PATTERNS = List.of("pattern");
    private static final List<BrushParam> PARAMS = List.of(
            BrushParam.choice("dir", "direction the shadow is cast", "down", "north", "east", "south", "west"),
            new BrushParam("dist", 32, 1, 64, "how far the shadow may travel, in blocks"));

    private ShadowSpec() {
    }

    @Override
    public String id() {
        return "shadow";
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
        return "/gzshadow <pattern> [dir=down] [dist=32]";
    }
}
