package nl.gzmn.gZMNBuildtools.modules.hover;

import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import nl.gzmn.gZMNBuildtools.api.Messages;
import nl.gzmn.gZMNBuildtools.core.module.GzmnModule;
import nl.gzmn.gZMNBuildtools.core.module.ModuleContext;
import nl.gzmn.gZMNBuildtools.modules.hover.live.HoverService;
import org.bukkit.entity.Player;

import java.util.List;

/** /gzhover: a live ghost preview of the held brush's stroke wherever you look. */
@SuppressWarnings("UnstableApiUsage")
public final class HoverPreviewModule implements GzmnModule {

    public static final String PERMISSION = "gzmnbuildtools.hover";

    private HoverService service;
    private Messages messages;

    @Override
    public String id() {
        return "hover-preview";
    }

    @Override
    public String description() {
        return "Live ghost preview of brush strokes where you look (/gzhover).";
    }

    @Override
    public void enable(ModuleContext context) {
        messages = context.core().messages();
        service = new HoverService(context.plugin(), context.core().previews(), context.core().playerData());
        context.listen(service);
        service.start();
        context.commands(this::register);
    }

    @Override
    public void disable() {
        if (service != null) {
            service.stop();
        }
    }

    private void set(Player player, boolean on) {
        service.setEnabled(player, on);
        if (on) {
            messages.success(player, "Hover preview on. Hold a GZMN brush (gradient, swap, weathering, texture or scatter) "
                    + "and look at a build: ghost blocks show what a click would change.");
        } else {
            messages.success(player, "Hover preview off.");
        }
        player.sendMessage(Component.text(on ? messages.text(player, "[Turn off]") : messages.text(player, "[Turn on]"),
                        NamedTextColor.AQUA)
                .clickEvent(ClickEvent.runCommand("/gzhover " + (on ? "off" : "on"))));
    }

    private void register(Commands commands) {
        commands.register(Commands.literal("gzhover")
                .requires(source -> source.getSender() instanceof Player
                        && source.getSender().hasPermission(PERMISSION))
                .executes(ctx -> {
                    Player player = (Player) ctx.getSource().getSender();
                    set(player, !service.enabled(player));
                    return 1;
                })
                .then(Commands.literal("on").executes(ctx -> {
                    set((Player) ctx.getSource().getSender(), true);
                    return 1;
                }))
                .then(Commands.literal("off").executes(ctx -> {
                    set((Player) ctx.getSource().getSender(), false);
                    return 1;
                }))
                .build(), "Toggle the live brush preview where you look.", List.of("hover"));
    }
}
