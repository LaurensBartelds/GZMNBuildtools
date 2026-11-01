package nl.gzmn.gZMNBuildtools.modules.materialswap.fawe;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import nl.gzmn.gZMNBuildtools.modules.materialswap.engine.MaterialSwap;
import nl.gzmn.gZMNBuildtools.modules.materialswap.engine.SwapOptions;
import nl.gzmn.gZMNBuildtools.shared.worldedit.BuildEditor;
import nl.gzmn.gZMNBuildtools.shared.worldedit.PreviewableBrush;

import java.util.List;
import java.util.function.Consumer;

/** Swaps the clicked build's material within the brush radius. */
public class MaterialSwapBrush implements PreviewableBrush {

    private final BuildEditor editor;
    private final MaterialSwap swap;
    private final SwapOptions options;
    private final Consumer<BuildEditor.Outcome> feedback;

    public MaterialSwapBrush(BuildEditor editor, MaterialSwap swap, SwapOptions options,
                             Consumer<BuildEditor.Outcome> feedback) {
        this.editor = editor;
        this.swap = swap;
        this.options = options;
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

    private BuildEditor.Outcome run(EditSession editSession, BlockVector3 position, double size,
                                    BuildEditor.StateSink sink) {
        return editor.run(editSession, position, swap.mode(options), List.of(swap.targetMaterial(options)),
                size, (scan, source) -> swap.chooser(options), sink);
    }
}
