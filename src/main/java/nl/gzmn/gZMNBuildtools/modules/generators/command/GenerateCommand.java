package nl.gzmn.gZMNBuildtools.modules.generators.command;

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
import com.sk89q.worldedit.world.block.BlockState;
import io.papermc.paper.math.Position;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import nl.gzmn.gZMNBuildtools.api.Messages;
import nl.gzmn.gZMNBuildtools.core.Limits;
import nl.gzmn.gZMNBuildtools.modules.generators.engine.GeneratorType;
import nl.gzmn.gZMNBuildtools.modules.generators.engine.Generators;
import nl.gzmn.gZMNBuildtools.shared.params.BrushArguments;
import nl.gzmn.gZMNBuildtools.shared.params.BrushParam;
import nl.gzmn.gZMNBuildtools.shared.preview.PreviewService;
import nl.gzmn.gZMNBuildtools.shared.worldedit.PartPlacer;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;

/** {@code /gzgen}: generates arches, cables, tori, cones, domes and spirals with any pattern. */
public class GenerateCommand {

    public static final String PERMISSION = "gzmnbuildtools.generate";

    private final JavaPlugin plugin;
    private final Messages messages;
    private final PreviewService previews;
    private final Limits limits;

    public GenerateCommand(JavaPlugin plugin, Messages messages, PreviewService previews, Limits limits) {
        this.plugin = plugin;
        this.messages = messages;
        this.previews = previews;
        this.limits = limits;
    }

    private int maxBlocks(Player player) {
        return limits.get(player, Limits.Kind.BLOCKS,
                Math.max(1, plugin.getConfig().getInt("generation.max-blocks", 500_000)));
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
            messages.info(player, "  %s (default %s): %s%s", p.name(), p.display(p.defaultValue()),
                    p.description(), p.isChoice() ? " [" + String.join(", ", p.choices()) + "]" : "");
        }
    }

    public void generate(Player player, String typeId, String rawRest) {
        generate(player, typeId, rawRest, ThreadLocalRandom.current().nextLong());
    }

    /** @param seed fixes random shapes (trees, rocks) so an applied preview matches what was shown */
    private void generate(Player player, String typeId, String rawRest, long seed) {
        // "-p" (or "preview") anywhere in the arguments shows ghost blocks first.
        List<String> tokens = new ArrayList<>(Arrays.asList(rawRest.trim().split("\\s+")));
        boolean preview = tokens.removeIf(t -> t.equalsIgnoreCase("-p") || t.equalsIgnoreCase("preview"));
        String rest = String.join(" ", tokens);
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

        PartPlacer placer;
        try {
            List<Pattern> patterns = new ArrayList<>();
            for (String text : args.patterns()) {
                patterns.add(parsePattern(actor, session, text));
            }
            placer = new PartPlacer(patterns);
        } catch (InputParseException e) {
            messages.error(player, "Invalid pattern: %s", e.getMessage());
            return;
        }

        int[] count = {0};
        Generators.generate(args, origin, a, b, seed, (x, y, z, part, axis) -> count[0]++);
        if (count[0] > maxBlocks(player)) {
            messages.error(player, "That shape has %d blocks, above your limit of %d.", count[0], maxBlocks(player));
            return;
        }

        final int[] fa = a;
        final int[] fb = b;
        int minY = world.getMinY();
        int maxY = world.getMaxY();
        if (preview) {
            Fawe.instance().getQueueHandler().async(() -> {
                Map<Position, BlockData> ghost = new HashMap<>();
                try {
                    Generators.generate(args, origin, fa, fb, seed, (x, y, z, part, axis) -> {
                        BlockState state = y >= minY && y <= maxY ? placer.state(x, y, z, part, axis) : null;
                        if (state != null) {
                            ghost.put(Position.block(x, y, z), BukkitAdapter.adapt(state));
                        }
                    });
                } catch (RuntimeException e) {
                    plugin.getLogger().log(Level.WARNING, "Generator preview failed for " + player.getName(), e);
                    messages.error(player, "Preview failed: %s", e.getMessage());
                    return;
                }
                plugin.getServer().getScheduler().runTask(plugin, () ->
                        previews.show(player, ghost, "a " + typeId.toLowerCase(Locale.ROOT),
                                () -> generate(player, typeId, rest, seed)));
            });
            return;
        }
        Fawe.instance().getQueueHandler().async(() -> {
            try (EditSession editSession = session.createEditSession(actor)) {
                Generators.generate(args, origin, fa, fb, seed, (x, y, z, part, axis) -> {
                    BlockState state = y >= minY && y <= maxY ? placer.state(x, y, z, part, axis) : null;
                    if (state != null) {
                        editSession.setBlock(x, y, z, state);
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
        int words = typed.split(" ", -1).length;
        if (words <= type.get().patternSlots()) {
            String head = typed.contains(" ") ? typed.substring(0, typed.lastIndexOf(' ') + 1) : "";
            String last = typed.substring(head.length()).toLowerCase(Locale.ROOT);
            List<String> options = type.get() == GeneratorType.TREE
                    ? (words == 1 ? List.of("oak_log", "spruce_log", "birch_log", "jungle_log", "dark_oak_log")
                    : List.of("oak_leaves", "spruce_leaves", "birch_leaves", "azalea_leaves", "cherry_leaves"))
                    : List.of("stone", "stone_bricks", "oak_planks", "iron_chain", "#stripes[y][1][stone][andesite]");
            return options.stream().filter(s -> s.startsWith(last)).map(s -> head + s).toList();
        }
        String prefix = typed.substring(0, typed.lastIndexOf(' ') + 1);
        String current = typed.substring(prefix.length()).toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (BrushParam p : type.get().params()) {
            String option = p.name() + "=";
            if (current.startsWith(option) && p.isChoice()) {
                p.choices().stream().map(c -> option + c).filter(c -> c.startsWith(current))
                        .forEach(c -> out.add(prefix + c));
            } else if (option.startsWith(current) && !typed.contains(option)) {
                out.add(prefix + option);
            }
        }
        return out;
    }
}
