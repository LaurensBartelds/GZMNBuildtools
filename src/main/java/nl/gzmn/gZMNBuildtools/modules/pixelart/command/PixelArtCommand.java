package nl.gzmn.gZMNBuildtools.modules.pixelart.command;

import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockType;
import nl.gzmn.gZMNBuildtools.api.Messages;
import nl.gzmn.gZMNBuildtools.core.Limits;
import nl.gzmn.gZMNBuildtools.modules.pixelart.engine.BlockSets;
import nl.gzmn.gZMNBuildtools.modules.pixelart.engine.ImageQuantizer;
import nl.gzmn.gZMNBuildtools.modules.pixelart.engine.PixelArtOptions;
import nl.gzmn.gZMNBuildtools.modules.pixelart.image.ImageLoader;
import nl.gzmn.gZMNBuildtools.shared.palette.BlockColors;
import nl.gzmn.gZMNBuildtools.shared.palette.OkLab;
import nl.gzmn.gZMNBuildtools.shared.preview.PreviewService;
import nl.gzmn.gZMNBuildtools.shared.worldedit.BlockKnowledge;
import nl.gzmn.gZMNBuildtools.shared.worldedit.PlayerEdits;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.logging.Level;

/**
 * {@code /gzpixelart}: builds a picture out of blocks, standing as a wall that
 * faces you or lying on the floor, or as flat map art (north up, matched to the
 * colours blocks show on a map).
 */
public class PixelArtCommand {

    public static final String PERMISSION = "gzmnbuildtools.pixelart";
    private static final int RANGE = 200;
    /** Brightness of the flat (middle) shade on a map. */
    private static final double MAP_FLAT_SHADE = 220 / 255.0;

    private final JavaPlugin plugin;
    private final Messages messages;
    private final BlockKnowledge blocks;
    private final PreviewService previews;
    private final ImageLoader loader;
    private final Limits limits;
    private volatile List<ImageQuantizer.Swatch> mapSwatches;

    public PixelArtCommand(JavaPlugin plugin, Messages messages, BlockKnowledge blocks, PreviewService previews,
                           ImageLoader loader, Limits limits) {
        this.limits = limits;
        this.plugin = plugin;
        this.messages = messages;
        this.blocks = blocks;
        this.previews = previews;
        this.loader = loader;
    }

    private int maxSide(Player player) {
        return Math.max(8, limits.get(player, Limits.Kind.IMAGE, plugin.getConfig().getInt("pixel-art.max-size", 256)));
    }

    public void help(Player player) {
        messages.info(player, "Build a picture from blocks: /gzpixelart %s", PixelArtOptions.USAGE);
        messages.info(player, "Put images in plugins/GZMNBuildtools/images, or use a direct https link.");
        List<String> images = loader.localImages();
        if (!images.isEmpty()) {
            messages.info(player, "Images here: %s", String.join(", ", images));
        }
    }

    public void build(Player player, String input) {
        PixelArtOptions options;
        try {
            options = PixelArtOptions.parse(input, maxSide(player));
        } catch (IllegalArgumentException e) {
            messages.error(player, "%s", e.getMessage());
            return;
        }
        Optional<BlockVector3> target = PlayerEdits.lookedAt(player, RANGE);
        if (target.isEmpty()) {
            messages.error(player, "Look at the block where the picture should go.");
            return;
        }
        BlockFace facing = options.map() ? BlockFace.NORTH : player.getFacing();
        messages.info(player, "Loading %s...", options.source());
        int maxSide = maxSide(player);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Map<BlockVector3, String> placement;
            try {
                placement = plan(options, target.get(), facing, maxSide);
            } catch (IllegalArgumentException e) {
                messages.error(player, "%s", e.getMessage());
                return;
            } catch (IOException e) {
                messages.error(player, "Could not load the image: %s", e.getMessage());
                return;
            } catch (RuntimeException e) {
                plugin.getLogger().log(Level.WARNING, "Pixel art failed for " + player.getName(), e);
                messages.error(player, "Building the picture failed: %s", e.getMessage());
                return;
            }
            Map<BlockVector3, BlockState> states = new LinkedHashMap<>();
            placement.forEach((pos, id) -> {
                BlockType type = blocks.type(id);
                if (type != null) {
                    states.put(pos, type.getDefaultState());
                }
            });
            if (options.preview()) {
                Bukkit.getScheduler().runTask(plugin, () -> previews.show(player, PlayerEdits.ghost(states),
                        "the picture", () -> place(player, states)));
            } else {
                place(player, states);
            }
        });
    }

    private void place(Player player, Map<BlockVector3, BlockState> states) {
        PlayerEdits.async(player, editSession -> {
            states.forEach((pos, state) -> editSession.setBlock(pos.x(), pos.y(), pos.z(), state));
            messages.success(player, "Built the picture from %d blocks. //undo reverts it.", states.size());
            return true;
        }, e -> messages.error(player, "Placing the picture failed: %s", e.getMessage()));
    }

    /** Loads, scales and quantizes the image, and works out where each block goes. */
    Map<BlockVector3, String> plan(PixelArtOptions options, BlockVector3 target, BlockFace facing, int maxSide)
            throws IOException {
        BufferedImage image = loader.load(options.source());
        int width = options.width();
        int height = options.heightFor(image.getWidth(), image.getHeight());
        if (height > maxSide) {
            throw new IllegalArgumentException("That would be " + height + " blocks tall; the limit is " + maxSide
                    + ". Set a smaller width or a height.");
        }
        int[] pixels = ImageLoader.scaledPixels(image, width, height);
        String[] ids = new ImageQuantizer(swatches(options)).quantize(pixels, width, height, options.dither());

        int fx = facing.getModX();
        int fz = facing.getModZ();
        int rx = -fz;
        int rz = fx;
        Map<BlockVector3, String> out = new LinkedHashMap<>();
        for (int py = 0; py < height; py++) {
            for (int px = 0; px < width; px++) {
                String id = ids[py * width + px];
                if (id == null) {
                    continue;
                }
                int across = px - width / 2;
                int up = height - 1 - py;
                BlockVector3 pos = options.floor()
                        ? BlockVector3.at(target.x() + rx * across + fx * up, target.y(), target.z() + rz * across + fz * up)
                        : BlockVector3.at(target.x() + rx * across, target.y() + 1 + up, target.z() + rz * across);
                out.put(pos, id);
            }
        }
        return out;
    }

    private List<ImageQuantizer.Swatch> swatches(PixelArtOptions options) {
        Predicate<String> allowed = BlockSets.of(options.blockSets());
        if (options.map()) {
            return mapSwatches().stream().filter(s -> allowed.test(s.block())).toList();
        }
        List<ImageQuantizer.Swatch> out = new ArrayList<>();
        for (String id : blocks.generator().candidates()) {
            if (allowed.test(id)) {
                BlockColors.Entry entry = blocks.colors().get(id).orElseThrow();
                out.add(new ImageQuantizer.Swatch(id, options.floor() ? entry.top() : entry.side()));
            }
        }
        if (out.isEmpty()) {
            throw new IllegalArgumentException("No blocks in that block set.");
        }
        return out;
    }

    /** One block per map colour, preferring concrete, wool and terracotta. */
    private List<ImageQuantizer.Swatch> mapSwatches() {
        List<ImageQuantizer.Swatch> cached = mapSwatches;
        if (cached != null) {
            return cached;
        }
        Map<Integer, String> byColour = new LinkedHashMap<>();
        List<String> ordered = new ArrayList<>(blocks.generator().candidates());
        ordered.sort((a, b) -> Integer.compare(rank(a), rank(b)));
        for (String id : ordered) {
            Material material = Material.matchMaterial(id);
            if (material == null || !material.isBlock()) {
                continue;
            }
            Color c = material.createBlockData().getMapColor();
            int rgb = (int) (c.getRed() * MAP_FLAT_SHADE) << 16 | (int) (c.getGreen() * MAP_FLAT_SHADE) << 8
                    | (int) (c.getBlue() * MAP_FLAT_SHADE);
            if (rgb != 0) {
                byColour.putIfAbsent(rgb, id);
            }
        }
        List<ImageQuantizer.Swatch> out = new ArrayList<>();
        byColour.forEach((rgb, id) -> out.add(new ImageQuantizer.Swatch(id, OkLab.fromRgb(rgb))));
        mapSwatches = out;
        return out;
    }

    private static int rank(String id) {
        if (id.endsWith("_concrete")) {
            return 0;
        }
        if (id.endsWith("_wool")) {
            return 1;
        }
        if (id.endsWith("terracotta")) {
            return 2;
        }
        return 3;
    }

    public List<String> suggestions(String typed) {
        String prefix = typed.contains(" ") ? typed.substring(0, typed.lastIndexOf(' ') + 1) : "";
        String current = typed.substring(prefix.length()).toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        if (prefix.isEmpty()) {
            loader.localImages().stream().filter(n -> n.toLowerCase(Locale.ROOT).startsWith(current)).forEach(out::add);
            if ("https://".startsWith(current)) {
                out.add("https://");
            }
            return out;
        }
        if (current.startsWith("blocks=")) {
            PixelArtOptions.BLOCK_SETS.stream().map(s -> "blocks=" + s).filter(s -> s.startsWith(current))
                    .forEach(s -> out.add(prefix + s));
            return out;
        }
        for (String option : List.of("width=", "height=", "wall", "floor", "blocks=", "dither=off", "map", "-p")) {
            if (option.startsWith(current) && !typed.contains(option)) {
                out.add(prefix + option);
            }
        }
        return out;
    }
}
