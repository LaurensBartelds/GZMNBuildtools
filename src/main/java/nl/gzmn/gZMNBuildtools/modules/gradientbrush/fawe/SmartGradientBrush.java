package nl.gzmn.gZMNBuildtools.modules.gradientbrush.fawe;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import nl.gzmn.gZMNBuildtools.modules.gradientbrush.SmartGradientService;
import nl.gzmn.gZMNBuildtools.shared.worldedit.BuildEditor;
import nl.gzmn.gZMNBuildtools.shared.worldedit.PreviewableBrush;

import java.util.UUID;
import java.util.function.Consumer;

/**
 * The build-aware gradient brush. It reads its owner's settings on every
 * click, so changes made with commands, the GUI or the web editor apply
 * without rebinding.
 */
public class SmartGradientBrush implements PreviewableBrush {

    private final SmartGradientService service;
    private final UUID owner;
    private final Consumer<SmartGradientService.Outcome> feedback;

    public SmartGradientBrush(SmartGradientService service, UUID owner,
                              Consumer<SmartGradientService.Outcome> feedback) {
        this.service = service;
        this.owner = owner;
        this.feedback = feedback;
    }

    @Override
    public void build(EditSession editSession, BlockVector3 position, Pattern pattern, double size) {
        String world = editSession.getWorld() == null ? "" : editSession.getWorld().getName();
        SmartGradientService.Outcome outcome = service.paint(editSession, world, owner, position, size,
                service.settings().get(owner), null);
        feedback.accept(outcome);
    }

    @Override
    public void preview(EditSession editSession, BlockVector3 position, double size, BuildEditor.StateSink sink) {
        String world = editSession.getWorld() == null ? "" : editSession.getWorld().getName();
        service.peek(editSession, world, owner, position, size, service.settings().get(owner), sink::accept);
    }

    @Override
    public Object previewState() {
        return service.settings().get(owner);
    }
}
