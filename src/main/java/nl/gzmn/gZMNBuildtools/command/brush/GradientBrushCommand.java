package nl.gzmn.gZMNBuildtools.command.brush;

import com.fastasyncworldedit.core.Fawe;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import nl.gzmn.gZMNBuildtools.api.Messages;
import nl.gzmn.gZMNBuildtools.api.PresetRegistry;
import nl.gzmn.gZMNBuildtools.gradient.smart.Dither;
import nl.gzmn.gZMNBuildtools.gradient.smart.GradientAxis;
import nl.gzmn.gZMNBuildtools.gradient.smart.GradientSettings;
import nl.gzmn.gZMNBuildtools.gradient.smart.Palette;
import nl.gzmn.gZMNBuildtools.gradient.smart.PaletteSpec;
import nl.gzmn.gZMNBuildtools.integration.worldedit.brush.FaweBrushBinder;
import nl.gzmn.gZMNBuildtools.integration.worldedit.gradient.SmartGradientBrush;
import nl.gzmn.gZMNBuildtools.integration.worldedit.gradient.SmartGradientService;
import nl.gzmn.gZMNBuildtools.integration.worldedit.gradient.Swatches;
import nl.gzmn.gZMNBuildtools.palette.BlockColors;
import nl.gzmn.gZMNBuildtools.preview.PreviewService;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;

/**
 * {@code /gradientbrush} (alias {@code /gb}): the build-aware gradient brush.
 * Settings live per player in the service, so the bound brush, GUI and web
 * editor always agree.
 */
public class GradientBrushCommand {

    public static final String PERMISSION = "gzmnbuildtools.brush";
    private static final int TARGET_RANGE = 200;

    private final JavaPlugin plugin;
    private final Messages messages;
    private final SmartGradientService service;
    private final PresetRegistry presets;
    private final PreviewService previews;

    public GradientBrushCommand(JavaPlugin plugin, Messages messages, SmartGradientService service,
                                PresetRegistry presets, PreviewService previews) {
        this.plugin = plugin;
        this.messages = messages;
        this.service = service;
        this.presets = presets;
        this.previews = previews;
    }

    public SmartGradientService service() {
        return service;
    }

    // ---- settings -------------------------------------------------------------------------------------------

    /**
     * Applies "[palette] [key=value ...]" to the player's settings.
     *
     * @return false (after telling the player) when something is invalid
     */
    public boolean apply(Player player, String input) {
        GradientSettings s = service.settings().get(player.getUniqueId());
        String[] tokens = input == null || input.isBlank() ? new String[0] : input.trim().split("\\s+");
        try {
            for (int i = 0; i < tokens.length; i++) {
                String token = tokens[i];
                int eq = token.indexOf('=');
                if (eq > 0 && !token.startsWith("[")) {
                    s = s.with(token.substring(0, eq), token.substring(eq + 1), service.maxRadius());
                } else if (i == 0) {
                    service.parseSpec(token);
                    s = s.withPalette(token);
                } else {
                    throw new IllegalArgumentException("Expected key=value but got '" + token + "'");
                }
            }
        } catch (IllegalArgumentException e) {
            messages.error(player, "%s", e.getMessage());
            return false;
        }
        service.settings().set(player.getUniqueId(), s);
        return true;
    }

    public void status(Player player) {
        GradientSettings s = service.settings().get(player.getUniqueId());
        messages.info(player, "Smart gradient brush:");
        sendPaletteLine(player, s);
        for (Map.Entry<String, String> e : s.describe().entrySet()) {
            if (!e.getKey().equals("palette")) {
                player.sendMessage(Component.text("  " + e.getKey() + ": ", NamedTextColor.GRAY)
                        .append(Component.text(e.getValue(), NamedTextColor.WHITE)));
            }
        }
        player.sendMessage(Component.text("  /gb <palette> [options]", NamedTextColor.AQUA)
                .append(Component.text(" binds it; /gb opens the menu; /gb paint paints a whole build.",
                        NamedTextColor.GRAY)));
    }

    private void sendPaletteLine(Player player, GradientSettings s) {
        Component line = Component.text("  palette: ", NamedTextColor.GRAY)
                .append(Component.text(s.palette(), NamedTextColor.WHITE));
        try {
            PaletteSpec spec = service.parseSpec(s.palette());
            if (!spec.dependsOnBuild()) {
                line = line.append(Component.text(" ")).append(
                        Swatches.of(spec.resolve(service.context(null), s.steps()), service.colors()));
            } else {
                line = line.append(Component.text(" (from the build you click)", NamedTextColor.DARK_GRAY));
            }
        } catch (IllegalArgumentException e) {
            line = line.append(Component.text(" (" + e.getMessage() + ")", NamedTextColor.RED));
        }
        player.sendMessage(line);
    }

    public void set(Player player, String input) {
        if (apply(player, input)) {
            messages.success(player, "Gradient settings updated; your brush uses them right away.");
            sendPaletteLine(player, service.settings().get(player.getUniqueId()));
        }
    }

    public void reset(Player player) {
        service.settings().reset(player.getUniqueId());
        service.forget(player.getUniqueId());
        messages.success(player, "Gradient settings reset to defaults (auto palette).");
    }

    public void rescan(Player player) {
        service.forget(player.getUniqueId());
        messages.success(player, "The next stroke rescans the build.");
    }

    // ---- binding ----------------------------------------------------------------------------------------------

    public void bind(Player player, String input) {
        if (!apply(player, input)) {
            return;
        }
        GradientSettings s = service.settings().get(player.getUniqueId());
        try {
            UUID owner = player.getUniqueId();
            FaweBrushBinder.bind(player, new SmartGradientBrush(service, owner, outcome -> feedback(owner, outcome)),
                    PERMISSION, s.radius(), "gzmn gradient " + s.palette(), null);
        } catch (WorldEditException e) {
            messages.error(player, "Cannot bind a brush to this item: %s", e.getMessage());
            return;
        } catch (RuntimeException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to bind gradient brush for " + player.getName(), e);
            messages.error(player, "Could not bind the brush; see the server log for details.");
            return;
        }
        service.forget(player.getUniqueId());
        messages.success(player, "Smart gradient brush bound (radius %d). Click any build to paint it.", s.radius());
        sendPaletteLine(player, s);
        messages.info(player, "It fits the gradient to the build you click, keeps stairs/slabs/walls and leaves "
                + "glass, lights and other materials alone. //size changes the radius.");
    }

    private void feedback(UUID owner, SmartGradientService.Outcome outcome) {
        Player player = plugin.getServer().getPlayer(owner);
        if (player == null) {
            return;
        }
        if (!outcome.ok()) {
            player.sendActionBar(Component.text(outcome.problem(), NamedTextColor.RED));
            return;
        }
        player.sendActionBar(summary(outcome));
    }

    private Component summary(SmartGradientService.Outcome o) {
        return Swatches.of(o.palette(), service.colors())
                .append(Component.text("  " + o.scan().bounds() + " ", NamedTextColor.WHITE))
                .append(Component.text(SmartGradientService.axisName(o.frame()) + " · " + o.scan().mainGroup()
                        + " · " + o.painted() + " painted" + (o.scan().truncated() ? " · large area" : ""),
                        NamedTextColor.GRAY));
    }

    // ---- whole build ------------------------------------------------------------------------------------------

    private Optional<BlockVector3> target(Player player) {
        Block block = player.getTargetBlockExact(TARGET_RANGE);
        if (block == null || block.getType().isAir()) {
            messages.error(player, "Look at a block of the build first.");
            return Optional.empty();
        }
        return Optional.of(BlockVector3.at(block.getX(), block.getY(), block.getZ()));
    }

    public void paint(Player player, String input) {
        if (input != null && !input.isBlank() && !apply(player, input)) {
            return;
        }
        Optional<BlockVector3> target = target(player);
        if (target.isEmpty()) {
            return;
        }
        GradientSettings s = service.settings().get(player.getUniqueId());
        com.sk89q.worldedit.entity.Player actor = BukkitAdapter.adapt(player);
        LocalSession session = WorldEdit.getInstance().getSessionManager().get(actor);
        String world = player.getWorld().getName();
        Fawe.instance().getQueueHandler().async(() -> {
            try (EditSession editSession = session.createEditSession(actor)) {
                SmartGradientService.Outcome o = service.paint(editSession, world, player.getUniqueId(),
                        target.get(), null, s, null);
                if (!o.ok()) {
                    messages.error(player, "%s", o.problem());
                    return;
                }
                session.remember(editSession);
                player.sendMessage(summary(o));
                messages.success(player, "Painted %d blocks of a %s build (%s). //undo reverts it.",
                        o.painted(), o.scan().bounds().toString(), SmartGradientService.describeMaterials(o.scan(), 3));
            } catch (RuntimeException e) {
                plugin.getLogger().log(Level.WARNING, "Gradient paint failed for " + player.getName(), e);
                messages.error(player, "Painting failed: %s", e.getMessage());
            }
        });
    }

    /** Shows the whole-build paint as ghost blocks; [Apply] places exactly those blocks. */
    public void preview(Player player, String input) {
        if (input != null && !input.isBlank() && !apply(player, input)) {
            return;
        }
        Optional<BlockVector3> target = target(player);
        if (target.isEmpty()) {
            return;
        }
        GradientSettings s = service.settings().get(player.getUniqueId());
        com.sk89q.worldedit.entity.Player actor = BukkitAdapter.adapt(player);
        LocalSession session = WorldEdit.getInstance().getSessionManager().get(actor);
        String world = player.getWorld().getName();
        Fawe.instance().getQueueHandler().async(() -> {
            Map<BlockVector3, com.sk89q.worldedit.world.block.BlockState> changes = new java.util.LinkedHashMap<>();
            SmartGradientService.Outcome o;
            try (EditSession editSession = session.createEditSession(actor)) {
                o = service.paint(editSession, world, player.getUniqueId(), target.get(), null, s,
                        (x, y, z, state) -> changes.put(BlockVector3.at(x, y, z), state));
            } catch (RuntimeException e) {
                plugin.getLogger().log(Level.WARNING, "Gradient preview failed for " + player.getName(), e);
                messages.error(player, "Preview failed: %s", e.getMessage());
                return;
            }
            if (!o.ok()) {
                messages.error(player, "%s", o.problem());
                return;
            }
            Map<io.papermc.paper.math.Position, org.bukkit.block.data.BlockData> ghost = new java.util.HashMap<>();
            changes.forEach((pos, state) -> ghost.put(io.papermc.paper.math.Position.block(pos.x(), pos.y(), pos.z()),
                    BukkitAdapter.adapt(state)));
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                player.sendMessage(summary(o));
                previews.show(player, ghost, "the gradient on this " + o.scan().bounds() + " build",
                        () -> applyChanges(player, changes));
            });
        });
    }

    private void applyChanges(Player player, Map<BlockVector3, com.sk89q.worldedit.world.block.BlockState> changes) {
        com.sk89q.worldedit.entity.Player actor = BukkitAdapter.adapt(player);
        LocalSession session = WorldEdit.getInstance().getSessionManager().get(actor);
        Fawe.instance().getQueueHandler().async(() -> {
            try (EditSession editSession = session.createEditSession(actor)) {
                changes.forEach((pos, state) -> editSession.setBlock(pos.x(), pos.y(), pos.z(), state));
                session.remember(editSession);
                messages.success(player, "Applied %d blocks. //undo reverts it.", changes.size());
            } catch (RuntimeException e) {
                plugin.getLogger().log(Level.WARNING, "Applying gradient preview failed for " + player.getName(), e);
                messages.error(player, "Applying failed: %s", e.getMessage());
            }
        });
    }

    public void info(Player player) {
        Optional<BlockVector3> target = target(player);
        if (target.isEmpty()) {
            return;
        }
        GradientSettings s = service.settings().get(player.getUniqueId());
        com.sk89q.worldedit.entity.Player actor = BukkitAdapter.adapt(player);
        LocalSession session = WorldEdit.getInstance().getSessionManager().get(actor);
        String world = player.getWorld().getName();
        Fawe.instance().getQueueHandler().async(() -> {
            try (EditSession editSession = session.createEditSession(actor)) {
                int[] changes = {0};
                SmartGradientService.Outcome o = service.paint(editSession, world, player.getUniqueId(),
                        target.get(), null, s, (x, y, z, state) -> changes[0]++);
                if (!o.ok()) {
                    messages.error(player, "%s", o.problem());
                    return;
                }
                messages.info(player, "Build: %s, %d blocks%s", o.scan().bounds().toString(), o.scan().size(),
                        o.scan().truncated() ? " (scan limit reached; set range=min..max to fix the gradient)" : "");
                messages.info(player, "Materials: %s", SmartGradientService.describeMaterials(o.scan(), 5));
                messages.info(player, "Gradient runs %s (axis %s); painting would change %d blocks.",
                        SmartGradientService.axisName(o.frame()), s.axis().id(), changes[0]);
                player.sendMessage(Component.text("Palette: ", NamedTextColor.YELLOW)
                        .append(Swatches.of(o.palette(), service.colors()))
                        .append(Component.text(" " + o.palette(), NamedTextColor.GRAY)));
                player.sendMessage(Component.text("[Paint whole build]", NamedTextColor.GREEN)
                        .clickEvent(ClickEvent.runCommand("/gb paint"))
                        .append(Component.text("  "))
                        .append(Component.text("[Preview]", NamedTextColor.YELLOW)
                                .clickEvent(ClickEvent.runCommand("/gb preview")))
                        .append(Component.text("  "))
                        .append(Component.text("[Suggest palettes]", NamedTextColor.AQUA)
                                .clickEvent(ClickEvent.runCommand("/gb suggest " + o.scan().mainGroup()))));
            } catch (RuntimeException e) {
                plugin.getLogger().log(Level.WARNING, "Gradient info failed for " + player.getName(), e);
                messages.error(player, "Could not inspect the build: %s", e.getMessage());
            }
        });
    }

    // ---- suggestions --------------------------------------------------------------------------------------------

    /** Clickable palettes generated from a block (the argument, the held block, or the block looked at). */
    public void suggest(Player player, String blockArg) {
        String block = blockArg;
        if (block == null || block.isBlank()) {
            Material held = player.getInventory().getItemInMainHand().getType();
            if (held.isBlock() && !held.isAir()) {
                block = held.getKey().getKey();
            } else {
                Block looked = player.getTargetBlockExact(TARGET_RANGE);
                block = looked == null ? null : looked.getType().getKey().getKey();
            }
        }
        if (block == null) {
            messages.error(player, "Hold a block, look at one, or name one: /gb suggest stone_bricks");
            return;
        }
        String base = BlockColors.normalize(block);
        if (service.colors().get(base).isEmpty()) {
            messages.error(player, "No colour data for %s.", base);
            return;
        }
        messages.info(player, "Palettes from %s (click to use):", base);
        List<String> specs = new ArrayList<>(List.of("aged:" + base, "shades:" + base));
        List<String> shades = service.generator().shades(base, 7);
        specs.add(shades.get(0) + ".." + shades.get(shades.size() - 1));
        specs.add(base + ".." + darkestOf(base));
        java.util.Set<String> shown = new java.util.HashSet<>();
        for (String spec : specs.stream().distinct().toList()) {
            try {
                Palette p = service.parseSpec(spec).resolve(service.context(base), 0);
                if (!shown.add(p.toString())) {
                    continue;
                }
                player.sendMessage(Component.text("  ")
                        .append(Swatches.of(p, service.colors()))
                        .append(Component.text(" " + spec, NamedTextColor.AQUA))
                        .hoverEvent(HoverEvent.showText(Component.text(p.toString())))
                        .clickEvent(ClickEvent.runCommand("/gb set " + spec)));
            } catch (IllegalArgumentException ignored) {
                // A generated spec that does not apply to this block (e.g. no aged variants) is simply skipped.
            }
        }
    }

    private String darkestOf(String base) {
        List<String> ramp = service.generator().shades(base, 9);
        return ramp.get(0).equals(base) ? "deepslate_tiles" : ramp.get(0);
    }

    // ---- legacy codes ---------------------------------------------------------------------------------------------

    public void code(Player player, String code) {
        Optional<CodeDecoder.Decoded> decoded = CodeDecoder.decode(code.trim());
        if (decoded.isEmpty()) {
            messages.error(player, "Invalid or corrupted brush code.");
            return;
        }
        CodeDecoder.Decoded d = decoded.get();
        StringBuilder palette = new StringBuilder();
        d.layers().forEach(layer -> palette.append('[').append(layer.replace(" ", "")).append(']'));
        GradientSettings s = service.settings().get(player.getUniqueId());
        try {
            service.parseSpec(palette.toString());
            s = s.withPalette(palette.toString());
            GradientAxis axis = GradientAxis.parse(d.direction()).orElse(GradientAxis.AUTO);
            s = s.withAxis(axis);
            s = switch (d.mode().toLowerCase(Locale.ROOT)) {
                case "discrete" -> s.withDither(Dither.NONE).withSoftness(0);
                case "blended", "noise" -> s.withDither(Dither.NOISE).withSoftness(1);
                case "smooth" -> s.withDither(Dither.ORDERED).withSoftness(0.6);
                default -> s.withDither(Dither.ORDERED).withSoftness(1);
            };
        } catch (IllegalArgumentException e) {
            messages.error(player, "That code uses something this server does not know: %s", e.getMessage());
            return;
        }
        service.settings().set(player.getUniqueId(), s);
        messages.success(player, "Imported the web editor gradient.");
        bind(player, "");
    }

    // ---- tab completion ---------------------------------------------------------------------------------------------

    /** Suggestions for "[palette] [key=value ...]". */
    public List<String> suggestions(String typed) {
        String prefix = typed.contains(" ") ? typed.substring(0, typed.lastIndexOf(' ') + 1) : "";
        String current = typed.substring(prefix.length()).toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        int eq = current.indexOf('=');
        if (eq > 0) {
            String key = current.substring(0, eq);
            List<String> values = switch (key) {
                case "axis" -> List.of("auto", "up", "down", "x", "-x", "z", "-z", "radial", "inward");
                case "target" -> List.of("clicked", "main", "structural", "all");
                case "dither" -> List.of("ordered", "noise", "none");
                case "softness" -> List.of("0", "0.3", "0.6", "1");
                case "shapes" -> List.of("keep", "full");
                case "steps" -> List.of("3", "5", "7", "9");
                case "radius" -> List.of("3", "4", "6", "8", "12");
                case "range" -> List.of("auto", "60..120");
                default -> List.of();
            };
            for (String v : values) {
                if ((key + "=" + v).startsWith(current)) {
                    out.add(prefix + key + "=" + v);
                }
            }
            return out;
        }
        if (prefix.isEmpty()) {
            for (String spec : List.of("auto", "aged:", "shades:", "preset:")) {
                if (spec.startsWith(current)) {
                    out.add(spec);
                }
            }
            if (current.startsWith("preset:")) {
                presets.ids().stream().map(id -> "preset:" + id).filter(id -> id.startsWith(current)).forEach(out::add);
            } else if (current.startsWith("aged:") || current.startsWith("shades:")) {
                String head = current.substring(0, current.indexOf(':') + 1);
                String rest = current.substring(head.length());
                service.generator().candidates().stream().filter(id -> id.startsWith(rest)).limit(30)
                        .forEach(id -> out.add(head + id));
            } else if (!current.isEmpty()) {
                int comma = Math.max(current.lastIndexOf(','), current.lastIndexOf(".."));
                String head = comma < 0 ? "" : current.substring(0, current.charAt(comma) == ',' ? comma + 1 : comma + 2);
                String rest = current.substring(head.length());
                service.generator().candidates().stream().filter(id -> id.startsWith(rest)).limit(30)
                        .forEach(id -> out.add(head + id));
            }
        }
        for (String option : GradientSettings.OPTIONS) {
            if ((option + "=").startsWith(current) && !typed.contains(option + "=")) {
                out.add(prefix + option + "=");
            }
        }
        return out;
    }
}
