package nl.gzmn.gZMNBuildtools.modules.revolve.engine;

import nl.gzmn.gZMNBuildtools.shared.params.BrushParam;
import nl.gzmn.gZMNBuildtools.shared.params.ParamSpec;

import java.util.List;

/** The {@code /gzrevolve} parameters (the pattern is an optional leading token, not a slot). */
public final class RevolveSpec implements ParamSpec {

    public static final RevolveSpec INSTANCE = new RevolveSpec();

    private static final List<BrushParam> PARAMS = List.of(
            new BrushParam("degrees", 360, 1, 720, "total angle to revolve"),
            new BrushParam("steps", 16, 1, 64, "number of copies around the axis"),
            BrushParam.choice("axis", "rotation axis", "y", "x", "z"));

    private RevolveSpec() {
    }

    @Override
    public String id() {
        return "revolve";
    }

    @Override
    public List<String> patternNames() {
        return List.of();
    }

    @Override
    public List<BrushParam> params() {
        return PARAMS;
    }

    @Override
    public String usage() {
        return "/gzrevolve [pattern] [degrees=360] [steps=16] [axis=y]";
    }
}
