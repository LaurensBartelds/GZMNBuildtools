package nl.gzmn.gZMNBuildtools.shared.worldedit;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.command.tool.brush.Brush;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;

/**
 * Caps a brush's size at the owner's radius limit, so {@code /size} or
 * scrolling cannot take it past what their rank allows.
 */
public class LimitedBrush implements Brush {

    private final Brush brush;
    private final double maxRadius;

    LimitedBrush(Brush brush, double maxRadius) {
        this.brush = brush;
        this.maxRadius = maxRadius;
    }

    /** Wraps {@code brush}; previewable brushes stay previewable. */
    public static Brush limit(Brush brush, double maxRadius) {
        return brush instanceof PreviewableBrush p ? new Previewable(p, maxRadius) : new LimitedBrush(brush, maxRadius);
    }

    /** The brush inside a limit, or the brush itself. */
    public static Brush unwrap(Brush brush) {
        return brush instanceof LimitedBrush limited ? limited.brush : brush;
    }

    public double maxRadius() {
        return maxRadius;
    }

    protected double cap(double size) {
        return Math.min(size, maxRadius);
    }

    @Override
    public void build(EditSession editSession, BlockVector3 position, Pattern pattern, double size)
            throws MaxChangedBlocksException {
        brush.build(editSession, position, pattern, cap(size));
    }

    private static final class Previewable extends LimitedBrush implements PreviewableBrush {

        private final PreviewableBrush inner;

        Previewable(PreviewableBrush brush, double maxRadius) {
            super(brush, maxRadius);
            this.inner = brush;
        }

        @Override
        public void preview(EditSession editSession, BlockVector3 position, double size, BuildEditor.StateSink sink) {
            inner.preview(editSession, position, cap(size), sink);
        }

        @Override
        public Object previewState() {
            return inner.previewState();
        }
    }
}
