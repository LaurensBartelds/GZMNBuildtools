package nl.gzmn.gZMNBuildtools.modules.gradientbrush.fawe;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.command.tool.brush.Brush;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import nl.gzmn.gZMNBuildtools.modules.gradientbrush.SmartGradientService;

import java.util.UUID;
import java.util.function.Consumer;

/**
 * The build-aware gradient brush. It reads its owner's settings on every
 * click, so changes made with commands, the GUI or the web editor apply
 * without rebinding.
 */
public class SmartGradientBrush implements Brush {

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
}
