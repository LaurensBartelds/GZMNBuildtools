package nl.gzmn.gZMNBuildtools.modules.pixelart.command;

import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockType;
import nl.gzmn.gZMNBuildtools.api.Messages;
import nl.gzmn.gZMNBuildtools.core.Limits;
import nl.gzmn.gZMNBuildtools.modules.pixelart.engine.TextFont;
import nl.gzmn.gZMNBuildtools.shared.preview.PreviewService;
import nl.gzmn.gZMNBuildtools.shared.worldedit.BlockKnowledge;
import nl.gzmn.gZMNBuildtools.shared.worldedit.PlayerEdits;
import org.bukkit.Bukkit;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** {@code /gztext} and {@code /gzfont}: build text from blocks on the wall you look at. */
public class TextCommand {

    public static final String PERMISSION = PixelArtCommand.PERMISSION;

    private static final int RANGE = 200;

    private final JavaPlugin plugin;
    private final Messages messages;
    private final BlockKnowledge blocks;
    private final PreviewService previews;
    private final Limits limits;
    private final Map<UUID, String> fontOf = new ConcurrentHashMap<>();

    public TextCommand(JavaPlugin plugin, Messages messages, BlockKnowledge blocks, PreviewService previews,
                       Limits limits) {
        this.plugin = plugin;
        this.messages = messages;
        this.blocks = blocks;
        this.previews = previews;
        this.limits = limits;
    }

    private int maxSide(Player player) {
        return limits.get(player, Limits.Kind.IMAGE,
                Math.max(1, plugin.getConfig().getInt("pixel-art.max-size", 256)));
    }

    public void usage(Player player) {
        messages.info(player, "Usage: /gztext <block> <size> <text> [-p]  (size 1-16; font: /gzfont %s)",
                TextFont.DEFAULT);
    }

    public void font(Player player, String rawArgs) {
        List<String> tokens = rawArgs == null || rawArgs.isBlank()
                ? List.of() : Arrays.asList(rawArgs.trim().split("\\s+"));
        if (tokens.isEmpty()) {
            messages.info(player, "Font: %s (available: %s).", currentFont(player),
                    String.join(", ", TextFont.fonts()));
            return;
        }
        if (tokens.get(0).equalsIgnoreCase("-c")) {
            List<String> fonts = TextFont.fonts();
            int i = fonts.indexOf(currentFont(player));
            fontOf.put(player.getUniqueId(), fonts.get((i + 1) % fonts.size()));
            messages.success(player, "Font is now %s.", currentFont(player));
            return;
        }
        String name = tokens.get(0).toLowerCase(java.util.Locale.ROOT);
        if (!TextFont.fonts().contains(name)) {
            messages.error(player, "Unknown font '%s'. Use one of: %s.", name, String.join(", ", TextFont.fonts()));
            return;
        }
        fontOf.put(player.getUniqueId(), name);
        messages.success(player, "Font is now %s.", name);
    }

    private String currentFont(Player player) {
        return fontOf.getOrDefault(player.getUniqueId(), TextFont.DEFAULT);
    }

    public void text(Player player, String rawArgs) {
        List<String> tokens = rawArgs == null || rawArgs.isBlank()
                ? new ArrayList<>() : new ArrayList<>(Arrays.asList(rawArgs.trim().split("\\s+")));
        boolean preview = tokens.removeIf(t -> t.equalsIgnoreCase("-p") || t.equalsIgnoreCase("preview"));
        if (tokens.size() < 3) {
            usage(player);
            return;
        }
        String blockId = tokens.get(0).toLowerCase(java.util.Locale.ROOT);
        int size;
        try {
            size = Integer.parseInt(tokens.get(1));
        } catch (NumberFormatException e) {
            messages.error(player, "Size must be a number between 1 and 16.");
            return;
        }
        if (size < 1 || size > 16) {
            messages.error(player, "Size must be a number between 1 and 16.");
            return;
        }
        String text = String.join(" ", tokens.subList(2, tokens.size()));
        BlockType type = blocks.type(blockId);
        if (type == null) {
            messages.error(player, "Unknown block '%s'.", blockId);
            return;
        }
        Optional<BlockVector3> target = PlayerEdits.lookedAt(player, RANGE);
        if (target.isEmpty()) {
            messages.error(player, "Look at the block where the text should go.");
            return;
        }
        boolean[][] grid = TextFont.render(text, size);
        int width = grid[0].length;
        int height = grid.length;
        int maxSide = maxSide(player);
        if (width > maxSide || height > maxSide) {
            messages.error(player, "That text would be %dx%d blocks; the limit is %d.", width, height, maxSide);
            return;
        }
        BlockFace facing = player.getFacing();
        int fx = facing.getModX();
        int fz = facing.getModZ();
        int rx = -fz;
        int rz = fx;
        Map<BlockVector3, BlockState> states = new LinkedHashMap<>();
        BlockState state = type.getDefaultState();
        for (int py = 0; py < height; py++) {
            for (int px = 0; px < width; px++) {
                if (grid[py][px]) {
                    int across = px - width / 2;
                    int up = height - 1 - py;
                    states.put(BlockVector3.at(target.get().x() + rx * across, target.get().y() + 1 + up,
                            target.get().z() + rz * across), state);
                }
            }
        }
        if (preview) {
            Bukkit.getScheduler().runTask(plugin, () -> previews.show(player, PlayerEdits.ghost(states),
                    "the text", () -> place(player, states)));
        } else {
            place(player, states);
        }
    }

    private void place(Player player, Map<BlockVector3, BlockState> states) {
        PlayerEdits.async(player, editSession -> {
            states.forEach((pos, state) -> editSession.setBlock(pos.x(), pos.y(), pos.z(), state));
            messages.success(player, "Built the text from %d blocks. //undo reverts it.", states.size());
            return true;
        }, e -> messages.error(player, "Placing the text failed: %s", e.getMessage()));
    }
}
