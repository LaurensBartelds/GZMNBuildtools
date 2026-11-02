package nl.gzmn.gZMNBuildtools.shared.worldedit;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.command.tool.brush.Brush;
import com.sk89q.worldedit.math.BlockVector3;

/**
 * A brush that can work out what a click would change without changing
 * anything, so the hover preview can show it as ghost blocks.
 */
public interface PreviewableBrush extends Brush {

    /**
     * Sends the changes a click at {@code position} would make to {@code sink}.
     * Must not modify the world and should give the same result as a real click.
     */
    void preview(EditSession editSession, BlockVector3 position, double size, BuildEditor.StateSink sink);

    /**
     * Anything besides position and size that changes the result (settings the
     * brush reads on every click); the preview is recomputed when it changes.
     */
    default Object previewState() {
        return null;
    }
}
