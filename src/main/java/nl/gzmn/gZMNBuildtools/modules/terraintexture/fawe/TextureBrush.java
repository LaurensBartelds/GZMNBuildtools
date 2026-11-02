package nl.gzmn.gZMNBuildtools.modules.terraintexture.fawe;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import nl.gzmn.gZMNBuildtools.modules.terraintexture.engine.TexturePreset;
import nl.gzmn.gZMNBuildtools.shared.build.Bounds;
import nl.gzmn.gZMNBuildtools.shared.worldedit.BuildEditor;
import nl.gzmn.gZMNBuildtools.shared.worldedit.PreviewableBrush;

import java.util.function.IntConsumer;

/** Textures the terrain inside the brush sphere with a preset. */
public class TextureBrush implements PreviewableBrush {

    private final TextureRunner runner;
    private final TexturePreset preset;
    private final long seed;
    private final IntConsumer feedback;

    public TextureBrush(TextureRunner runner, TexturePreset preset, long seed, IntConsumer feedback) {
        this.runner = runner;
        this.preset = preset;
        this.seed = seed;
        this.feedback = feedback;
    }

    @Override
    public void build(EditSession editSession, BlockVector3 position, Pattern pattern, double size) {
        feedback.accept(run(editSession, position, size, BuildEditor.StateSink.into(editSession)));
    }

    @Override
    public void preview(EditSession editSession, BlockVector3 position, double size, BuildEditor.StateSink sink) {
        run(editSession, position, size, sink);
    }

    private int run(EditSession editSession, BlockVector3 position, double size, BuildEditor.StateSink sink) {
        int r = (int) Math.ceil(size);
        double r2 = size * size;
        int cx = position.x();
        int cy = position.y();
        int cz = position.z();
        Bounds region = new Bounds(cx - r, cy - r, cz - r, cx + r, cy + r, cz + r);
        return runner.run(editSession, preset, seed, region, (x, y, z) -> {
            double dx = x - cx;
            double dy = y - cy;
            double dz = z - cz;
            return dx * dx + dy * dy + dz * dz <= r2;
        }, sink);
    }
}
