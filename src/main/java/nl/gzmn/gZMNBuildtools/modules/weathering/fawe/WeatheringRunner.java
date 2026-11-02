package nl.gzmn.gZMNBuildtools.modules.weathering.fawe;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.registry.state.Property;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockTypes;
import nl.gzmn.gZMNBuildtools.modules.weathering.engine.WeatherOptions;
import nl.gzmn.gZMNBuildtools.modules.weathering.engine.Weathering;
import nl.gzmn.gZMNBuildtools.shared.build.BuildPainter;
import nl.gzmn.gZMNBuildtools.shared.worldedit.BuildEditor;

import java.util.List;

/** Weathers the build at a click: block changes first, then vines on the walls that are left. */
public final class WeatheringRunner {

    /** What a run did. */
    public record Result(BuildEditor.Outcome outcome, int vines) {
    }

    private final BuildEditor editor;
    private final Weathering weathering;

    public WeatheringRunner(BuildEditor editor) {
        this.editor = editor;
        this.weathering = new Weathering(editor.blocks().catalog());
    }

    /** @param radius brush radius, or null for the whole build */
    public Result run(EditSession editSession, BlockVector3 click, WeatherOptions options, Double radius,
                      BuildEditor.StateSink sink) {
        Weathering.Pass[] pass = new Weathering.Pass[1];
        BuildEditor.Outcome outcome = editor.run(editSession, click, options.target(), List.of(), radius,
                (scan, source) -> {
                    pass[0] = weathering.pass(scan, source, options);
                    return pass[0].chooser();
                }, sink);
        int[] vines = {0};
        if (outcome.ok() && pass[0] != null) {
            BuildPainter.StrokeFilter stroke = radius == null ? BuildPainter.StrokeFilter.ALL
                    : BuildPainter.StrokeFilter.sphere(click.x(), click.y(), click.z(), radius);
            pass[0].vines(stroke, (x, y, z, side) -> {
                sink.accept(x, y, z, vine(side));
                vines[0]++;
            });
        }
        return new Result(outcome, vines[0]);
    }

    private static BlockState vine(String side) {
        BlockState vine = BlockTypes.VINE.getDefaultState();
        Property<Boolean> property = vine.getBlockType().getProperty(side);
        return property == null ? vine : vine.with(property, Boolean.TRUE);
    }
}
