package nl.gzmn.gZMNBuildtools.modules.smear.command;

import nl.gzmn.gZMNBuildtools.api.Messages;
import nl.gzmn.gZMNBuildtools.core.Limits;
import nl.gzmn.gZMNBuildtools.modules.smear.fawe.SmearBrush;
import nl.gzmn.gZMNBuildtools.shared.worldedit.FaweBrushBinder;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/** {@code /gzsmear}: binds a brush that smears blocks along the stroke direction. */
public class SmearCommand {

    public static final String PERMISSION = "gzmnbuildtools.smear";

    private static final List<String> OPTIONS = List.of("amount=", "direction=");
    private static final List<String> DIRECTIONS = List.of("stroke", "north", "east", "south", "west", "up", "down");

    private final JavaPlugin plugin;
    private final Messages messages;
    private final Limits limits;

    public SmearCommand(JavaPlugin plugin, Messages messages, Limits limits) {
        this.plugin = plugin;
        this.messages = messages;
        this.limits = limits;
    }

    private int maxRadius(Player player) {
        return limits.get(player, Limits.Kind.RADIUS,
                Math.max(1, plugin.getConfig().getInt("smear.max-radius", 16)));
    }

    public void bind(Player player, String rawArgs) {
        int amount = 2;
        String direction = "stroke";
        if (rawArgs != null && !rawArgs.isBlank()) {
            for (String token : Arrays.asList(rawArgs.trim().split("\\s+"))) {
                int eq = token.indexOf('=');
                if (eq <= 0) {
                    messages.error(player, "Unknown option '%s'. Use amount=<n> or direction=<dir>.", token);
                    return;
                }
                String key = token.substring(0, eq).toLowerCase(Locale.ROOT);
                String value = token.substring(eq + 1).toLowerCase(Locale.ROOT);
                switch (key) {
                    case "amount" -> {
                        try {
                            amount = Integer.parseInt(value);
                        } catch (NumberFormatException e) {
                            messages.error(player, "amount must be a number between -16 and 16.");
                            return;
                        }
                        if (amount < -16 || amount == 0 || amount > 16) {
                            messages.error(player, "amount must be a number between -16 and 16.");
                            return;
                        }
                    }
                    case "direction" -> {
                        if (!DIRECTIONS.contains(value)) {
                            messages.error(player, "direction must be one of %s.", String.join(", ", DIRECTIONS));
                            return;
                        }
                        direction = value;
                    }
                    default -> {
                        messages.error(player, "Unknown option '%s'. Use amount=<n> or direction=<dir>.", key);
                        return;
                    }
                }
            }
        }
        try {
            FaweBrushBinder.bind(player, new SmearBrush(() -> player, amount, direction), PERMISSION, 3,
                    maxRadius(player), "gzmn smear amount=" + amount + " direction=" + direction, null);
            messages.success(player, "Smear brush bound (radius %d, amount %d, direction %s). /none unbinds it.",
                    Math.min(3, maxRadius(player)), amount, direction);
        } catch (Exception e) {
            messages.error(player, "Could not bind the smear brush: %s", e.getMessage());
        }
    }

    public List<String> suggestions(String typed) {
        String prefix = typed.contains(" ") ? typed.substring(0, typed.lastIndexOf(' ') + 1) : "";
        String current = typed.substring(prefix.length()).toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String option : OPTIONS) {
            if (option.startsWith(current) && !typed.contains(option)) {
                out.add(prefix + option);
            }
            if (current.startsWith("direction=")) {
                DIRECTIONS.stream().map(d -> "direction=" + d).filter(d -> d.startsWith(current))
                        .forEach(d -> out.add(prefix + d));
            }
        }
        return out;
    }
}
