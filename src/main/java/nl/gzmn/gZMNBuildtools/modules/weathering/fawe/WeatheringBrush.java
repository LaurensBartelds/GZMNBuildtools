package nl.gzmn.gZMNBuildtools.modules.weathering.fawe;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import nl.gzmn.gZMNBuildtools.modules.weathering.engine.WeatherOptions;
import nl.gzmn.gZMNBuildtools.shared.worldedit.BuildEditor;
import nl.gzmn.gZMNBuildtools.shared.worldedit.PreviewableBrush;

import java.util.function.Consumer;

/** Weathers the clicked build within the brush radius. */
public class WeatheringBrush implements PreviewableBrush {

    private final WeatheringRunner runner;
    private final WeatherOptions options;
    private final Consumer<WeatheringRunner.Result> feedback;

    public WeatheringBrush(WeatheringRunner runner, WeatherOptions options, Consumer<WeatheringRunner.Result> feedback) {
        this.runner = runner;
        this.options = options;
        this.feedback = feedback;
    }

    @Override
    public void build(EditSession editSession, BlockVector3 position, Pattern pattern, double size) {
        feedback.accept(runner.run(editSession, position, options, size, BuildEditor.StateSink.into(editSession)));
    }

    @Override
    public void preview(EditSession editSession, BlockVector3 position, double size, BuildEditor.StateSink sink) {
        runner.run(editSession, position, options, size, sink);
    }
}
