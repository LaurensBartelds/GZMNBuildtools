package nl.gzmn.gZMNBuildtools.ui.gradient;

import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.block.BlockType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import nl.gzmn.gZMNBuildtools.gradient.model.*;
import nl.gzmn.gZMNBuildtools.common.MessageManager;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;


public class GradientPreviewRenderer {

    private static final int PREVIEW_SLOTS = 9;
    private static final int MAX_WORLD_PREVIEW_BLOCKS = 10000;
    private static final int PREVIEW_DURATION_TICKS = 200;

    private final Plugin plugin;
    private final Map<UUID, BukkitTask> activeWorldPreviews = new HashMap<>();

    public GradientPreviewRenderer(Plugin plugin) {
        this.plugin = plugin;
    }

    
    public void renderPreviewBar(Inventory inventory, int startSlot, GradientDefinition gradient) {
        if (gradient == null || gradient.getStops().size() < 2) {
            renderEmptyPreview(inventory, startSlot);
            return;
        }

        
        Random previewRandom = gradient.getInterpolationMode() == GradientDefinition.InterpolationMode.BLENDED
                ? new Random(42L)
                : null;

        for (int i = 0; i < PREVIEW_SLOTS; i++) {
            double position = i / (double) (PREVIEW_SLOTS - 1);
            BlockType blockType;
            if (previewRandom != null) {
                previewRandom.setSeed(42L + i); 
                blockType = gradient.getBlockAt(position, previewRandom);
            } else {
                blockType = gradient.getBlockAt(position);
            }

            ItemStack item = createPreviewItem(blockType, position);
            inventory.setItem(startSlot + i, item);
        }
    }

    
    public void renderPreviewBar(Inventory inventory, int startSlot, GradientPreset preset,
            GradientDefinition.GradientDirection direction) {
        GradientDefinition gradient = GradientDefinition.fromPreset(preset, direction);
        renderPreviewBar(inventory, startSlot, gradient);
    }

    
    public void renderPreviewBar(Inventory inventory, int startSlot, List<BlockType> blocks,
            GradientDefinition.GradientDirection direction,
            GradientDefinition.InterpolationMode mode) {
        if (blocks == null || blocks.size() < 2) {
            renderEmptyPreview(inventory, startSlot);
            return;
        }

        List<GradientDefinition.GradientStop> stops = new ArrayList<>();
        for (int i = 0; i < blocks.size(); i++) {
            double position = blocks.size() > 1 ? (double) i / (blocks.size() - 1) : 0.5;
            stops.add(new GradientDefinition.GradientStop(blocks.get(i), position));
        }

        GradientDefinition gradient = new GradientDefinition(stops, direction, mode);
        renderPreviewBar(inventory, startSlot, gradient);
    }

    
    public void renderPreviewBarMultiBlock(Inventory inventory, int startSlot, List<List<BlockType>> multiBlockStops,
            GradientDefinition.GradientDirection direction,
            GradientDefinition.InterpolationMode mode) {
        if (multiBlockStops == null || multiBlockStops.size() < 2) {
            renderEmptyPreview(inventory, startSlot);
            return;
        }

        List<GradientDefinition.GradientStop> stops = new ArrayList<>();
        for (int i = 0; i < multiBlockStops.size(); i++) {
            double position = multiBlockStops.size() > 1 ? (double) i / (multiBlockStops.size() - 1) : 0.5;
            stops.add(new GradientDefinition.GradientStop(multiBlockStops.get(i), position));
        }

        GradientDefinition gradient = new GradientDefinition(stops, direction, mode);
        renderPreviewBar(inventory, startSlot, gradient);
    }

    
    public void renderEmptyPreview(Inventory inventory, int startSlot) {
        ItemStack placeholder = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = placeholder.getItemMeta();
        meta.displayName(Component.text("Add at least 2 blocks").color(NamedTextColor.GRAY));
        placeholder.setItemMeta(meta);

        for (int i = 0; i < PREVIEW_SLOTS; i++) {
            inventory.setItem(startSlot + i, placeholder);
        }
    }

    
    private ItemStack createPreviewItem(BlockType blockType, double position) {
        Material material = Material.BARRIER;
        if (blockType != null) {
            String materialName = blockType.id().replace("minecraft:", "").toUpperCase();
            Material matched = Material.matchMaterial(materialName);
            if (matched != null) {
                material = matched;
            }
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text((int) (position * 100) + "%").color(NamedTextColor.AQUA));
        item.setItemMeta(meta);
        return item;
    }

    
    public void showWorldPreview(Player player, Region region, GradientDefinition gradient,
            GradientType gradientType, GradientContext context) {
        cancelWorldPreview(player);

        long blockCount = region.getVolume();
        if (blockCount > MAX_WORLD_PREVIEW_BLOCKS) {
            MessageManager.warn(player, "Selection too large for preview (%d blocks, max %d). Showing partial preview.",
                    blockCount, MAX_WORLD_PREVIEW_BLOCKS);
        }

        World world = player.getWorld();
        UUID playerId = player.getUniqueId();

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            private int ticksRemaining = PREVIEW_DURATION_TICKS;
            private int blocksShown = 0;

            @Override
            public void run() {
                if (ticksRemaining <= 0 || !player.isOnline()) {
                    cancelWorldPreview(player);
                    return;
                }

                blocksShown = 0;
                for (BlockVector3 position : region) {
                    if (blocksShown >= MAX_WORLD_PREVIEW_BLOCKS)
                        break;

                    double gradientPosition = gradientType.calculatePosition(position, region, context);
                    BlockType blockType = gradient.getBlockAt(gradientPosition);

                    if (blockType != null) {
                        Color particleColor = getBlockColor(blockType);
                        Location loc = new Location(world,
                                position.x() + 0.5,
                                position.y() + 0.5,
                                position.z() + 0.5);

                        Particle.DustOptions dust = new Particle.DustOptions(particleColor, 0.8f);
                        player.spawnParticle(Particle.DUST, loc, 1, 0, 0, 0, 0, dust);
                    }
                    blocksShown++;
                }

                ticksRemaining -= 5;
            }
        }, 0L, 5L);

        activeWorldPreviews.put(playerId, task);
    }

    
    public void cancelWorldPreview(Player player) {
        BukkitTask task = activeWorldPreviews.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }
    }

    
    public void cancelAllPreviews() {
        for (BukkitTask task : activeWorldPreviews.values()) {
            task.cancel();
        }
        activeWorldPreviews.clear();
    }

    
    private Color getBlockColor(BlockType blockType) {
        String id = blockType.id().toLowerCase();

        if (id.contains("white"))
            return Color.WHITE;
        if (id.contains("light_gray"))
            return Color.SILVER;
        if (id.contains("gray") && !id.contains("light"))
            return Color.GRAY;
        if (id.contains("black"))
            return Color.fromRGB(30, 30, 30);
        if (id.contains("red"))
            return Color.RED;
        if (id.contains("orange"))
            return Color.ORANGE;
        if (id.contains("yellow"))
            return Color.YELLOW;
        if (id.contains("lime"))
            return Color.LIME;
        if (id.contains("green"))
            return Color.GREEN;
        if (id.contains("cyan"))
            return Color.fromRGB(0, 200, 200);
        if (id.contains("light_blue"))
            return Color.fromRGB(100, 150, 255);
        if (id.contains("blue"))
            return Color.BLUE;
        if (id.contains("purple"))
            return Color.PURPLE;
        if (id.contains("magenta"))
            return Color.FUCHSIA;
        if (id.contains("pink"))
            return Color.fromRGB(255, 180, 200);
        if (id.contains("brown"))
            return Color.fromRGB(139, 90, 43);
        if (id.contains("sand"))
            return Color.fromRGB(220, 200, 150);
        if (id.contains("stone"))
            return Color.fromRGB(128, 128, 128);
        if (id.contains("deepslate"))
            return Color.fromRGB(80, 80, 90);
        if (id.contains("dirt") || id.contains("coarse"))
            return Color.fromRGB(130, 90, 60);
        if (id.contains("grass"))
            return Color.fromRGB(100, 180, 60);
        if (id.contains("cobble"))
            return Color.fromRGB(100, 100, 100);
        if (id.contains("moss"))
            return Color.fromRGB(80, 120, 50);
        if (id.contains("ice") || id.contains("snow"))
            return Color.fromRGB(200, 220, 255);
        if (id.contains("terracotta"))
            return Color.fromRGB(150, 90, 70);

        return Color.fromRGB(150, 150, 150);
    }

    
    public boolean hasActivePreview(Player player) {
        return activeWorldPreviews.containsKey(player.getUniqueId());
    }
}
