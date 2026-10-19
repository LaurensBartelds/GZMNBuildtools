package nl.gzmn.gZMNBuildtools.command;

import com.fastasyncworldedit.core.Fawe;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.World;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import nl.gzmn.gZMNBuildtools.api.Messages;
import nl.gzmn.gZMNBuildtools.brush.BrushArguments;
import nl.gzmn.gZMNBuildtools.brush.BrushParam;
import nl.gzmn.gZMNBuildtools.brush.ParamSpec;
import nl.gzmn.gZMNBuildtools.generate.GeneratorType;
import nl.gzmn.gZMNBuildtools.generate.Generators;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.logging.Level;

/** {@code /gzgen}: generates arches, cables, tori, cones, domes and spirals with any pattern. */
public class GenerateCommand {

    public static final String PERMISSION = "gzmnbuildtools.generate";

    private final JavaPlugin plugin;
    private final Messages messages;

    public GenerateCommand(JavaPlugin plugin, Messages messages) {
        this.plugin = plugin;
        this.messages = messages;
    }

    private int maxBlocks() {
        return Math.max(1, plugin.getConfig().getInt("generation.max-blocks", 500_000));
    }

    public void list(Player player) {
        messages.info(player, "Generators (click to fill in):");
        for (GeneratorType type : GeneratorType.values()) {
            player.sendMessage(Component.text("  " + type.id(), NamedTextColor.AQUA)
                    .append(Component.text(" - " + type.description(), NamedTextColor.GRAY))
                    .clickEvent(ClickEvent.suggestCommand("/gzgen " + type.id() + " "))
                    .hoverEvent(HoverEvent.showText(Component.text(type.usage()))));
        }
    }

    public void help(Player player, String typeId) {
        Optional<GeneratorType> type = GeneratorType.byId(typeId);
        if (type.isEmpty()) {
            messages.error(player, "Unknown generator '%s'.", typeId);
            list(player);
            return;
        }
        messages.info(player, "%s", type.get().usage());
        for (BrushParam p : type.get().params()) {
            messages.info(player, "  %s (default %s): %s", p.name(), ParamSpec.format(p.defaultValue()),
                    p.description());
        }
    }

    public void generate(Player player, String typeId, String rest) {
        Optional<GeneratorType> type = GeneratorType.byId(typeId);
        if (type.isEmpty()) {
            messages.error(player, "Unknown generator '%s'.", typeId);
            list(player);
            return;
        }
        BrushArguments<GeneratorType> args;
        try {
            args = BrushArguments.parse(type.get(), rest);
        } catch (IllegalArgumentException e) {
            messages.error(player, "%s", e.getMessage());
            return;
        }

        com.sk89q.worldedit.entity.Player actor = BukkitAdapter.adapt(player);
        LocalSession session = WorldEdit.getInstance().getSessionManager().get(actor);
        World world = actor.getWorld();

        int[] origin;
        int[] a = null;
        int[] b = null;
        BlockVector3 here = actor.getLocation().toBlockPoint();
        origin = new int[]{here.x(), here.y(), here.z()};
        if (type.get().usesSelection()) {
            try {
                Region region = session.getSelection(world);
                if (!(region instanceof CuboidRegion cuboid)) {
                    messages.error(player, "%s needs a cuboid selection; its two points are the ends.", typeId);
                    return;
                }
                a = toArray(cuboid.getPos1());
                b = toArray(cuboid.getPos2());
            } catch (IncompleteRegionException e) {
                messages.error(player, "Select the two end points with //pos1 and //pos2 first.");
                return;
            }
        }

        Pattern pattern;
        try {
            pattern = parsePattern(actor, session, args.pattern());
        } catch (InputParseException e) {
            messages.error(player, "Invalid pattern: %s", e.getMessage());
            return;
        }

        int[] count = {0};
        Generators.generate(args, origin, a, b, (x, y, z) -> count[0]++);
        if (count[0] > maxBlocks()) {
            messages.error(player, "That shape has %d blocks, above the limit of %d (generation.max-blocks).",
                    count[0], maxBlocks());
            return;
        }

        final int[] fa = a;
        final int[] fb = b;
        Fawe.instance().getQueueHandler().async(() -> {
            try (EditSession editSession = session.createEditSession(actor)) {
                int minY = world.getMinY();
                int maxY = world.getMaxY();
                Generators.generate(args, origin, fa, fb, (x, y, z) -> {
                    if (y >= minY && y <= maxY) {
                        editSession.setBlock(x, y, z, pattern);
                    }
                });
                session.remember(editSession);
                messages.success(player, "Generated %s (%d blocks). Use //undo to revert.",
                        typeId.toLowerCase(Locale.ROOT), editSession.getBlockChangeCount());
            } catch (RuntimeException e) {
                plugin.getLogger().log(Level.WARNING, "Generator " + typeId + " failed for " + player.getName(), e);
                messages.error(player, "Generation failed: %s", e.getMessage());
            }
        });
    }

    private static int[] toArray(BlockVector3 v) {
        return new int[]{v.x(), v.y(), v.z()};
    }

    private Pattern parsePattern(com.sk89q.worldedit.entity.Player actor, LocalSession session, String input)
            throws InputParseException {
        ParserContext context = new ParserContext();
        context.setActor(actor);
        context.setWorld(actor.getWorld());
        context.setExtent(actor.getWorld());
        context.setSession(session);
        context.setRestricted(true);
        return WorldEdit.getInstance().getPatternFactory().parseFromInput(input, context);
    }

    public List<String> typeSuggestions() {
        return Arrays.stream(GeneratorType.values()).map(GeneratorType::id).toList();
    }

    public List<String> argumentSuggestions(String typeId, String typed) {
        Optional<GeneratorType> type = GeneratorType.byId(typeId);
        if (type.isEmpty()) {
            return List.of();
        }
        if (!typed.contains(" ")) {
            return List.of("stone", "stone_bricks", "oak_planks", "iron_chain", "#stripes[y][1][stone][andesite]")
                    .stream().filter(s -> s.startsWith(typed.toLowerCase(Locale.ROOT))).toList();
        }
        String prefix = typed.substring(0, typed.lastIndexOf(' ') + 1);
        String current = typed.substring(prefix.length()).toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (BrushParam p : type.get().params()) {
            String option = p.name() + "=";
            if (option.startsWith(current) && !typed.contains(option)) {
                out.add(prefix + option);
            }
        }
        return out;
    }
}
