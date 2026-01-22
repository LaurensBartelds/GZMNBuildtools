package nl.gzmn.gZMNBuildtools.gradient;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service to manage block colors and provide suggestions based on color
 * similarity.
 */
public class BlockColorService {

    private final Map<Material, BlockColorInfo> blockColors = new HashMap<>();
    private final List<Material> sortedByBrightness = new ArrayList<>();

    public BlockColorService(Plugin plugin) {
        plugin.getLogger().info("[BlockColorService] Initializing...");
        initializeBlockColors();
        plugin.getLogger().info("[BlockColorService] Initialized with " + blockColors.size() + " blocks.");
    }

    private void initializeBlockColors() {
        for (Material material : Material.values()) {
            if (!material.isBlock() || !material.isItem() || material.isAir() || material.name().contains("LEGACY")) {
                continue;
            }

            // Filter out some unlikely blocks for gradients
            String name = material.name();
            if (name.contains("DOOR") || name.contains("FENCE") || name.contains("WALL") ||
                    name.contains("PANE") || name.contains("TORCH") || name.contains("FLOWER")
                    || name.contains("PLANT")) {
                continue;
            }

            try {
                BlockData itemData = material.createBlockData();
                Color color = itemData.getMapColor();

                // Skip transparent/invalid colors if mostly black/transparent and not black
                // concrete/wool etc
                // MapColor actually handles most, but let's check.
                // Note: getMapColor returns a Bukkit Color.

                float[] hsv = new float[3];
                java.awt.Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), hsv);

                blockColors.put(material, new BlockColorInfo(material, color, hsv[0], hsv[1], hsv[2]));

            } catch (Exception ignored) {
                // Some blocks might fail to create block data or get color
            }
        }
    }

    /**
     * Get suggestions for lighter and darker blocks with similar tone.
     * 
     * @param input The input material
     * @param count Number of lighter and darker options to return
     * @return List of suggested materials, ordered from darkest to lightest
     *         surrounding the input
     */
    public List<Material> getGradientSuggestions(Material input, int count) {
        BlockColorInfo inputInfo = blockColors.get(input);
        if (inputInfo == null)
            return Collections.emptyList();

        // Filter for blocks with similar Hue and Saturation
        double hueThreshold = 0.1; // 10% tolerance
        double satThreshold = 0.3; // 30% tolerance

        List<BlockColorInfo> candidates = blockColors.values().stream()
                .filter(info -> Math.abs(info.hue - inputInfo.hue) < hueThreshold ||
                        Math.abs(info.hue - inputInfo.hue) > (1.0 - hueThreshold)) // Wrap around for Red
                .filter(info -> Math.abs(info.saturation - inputInfo.saturation) < satThreshold)
                .sorted(Comparator.comparingDouble(info -> info.brightness))
                .collect(Collectors.toList());

        // If we don't have enough candidates with strict filters, relax them?
        if (candidates.size() < count * 2) {
            candidates = blockColors.values().stream()
                    .filter(info -> Math.abs(info.saturation - inputInfo.saturation) < 0.5) // Relaxed saturation
                    .sorted(Comparator.comparingDouble(info -> info.brightness))
                    .collect(Collectors.toList());
        }

        // Find index of input or closest to input brightness
        int centerIndex = -1;
        double minDiff = Double.MAX_VALUE;
        for (int i = 0; i < candidates.size(); i++) {
            if (candidates.get(i).material == input) {
                centerIndex = i;
                break;
            }
            double diff = Math.abs(candidates.get(i).brightness - inputInfo.brightness);
            if (diff < minDiff) {
                minDiff = diff;
                centerIndex = i;
            }
        }

        List<Material> result = new ArrayList<>();

        // Add darker
        for (int i = Math.max(0, centerIndex - count); i < centerIndex; i++) {
            result.add(candidates.get(i).material);
        }

        // Add input (optional? User asked for options close to that)
        // result.add(input);

        // Add lighter
        for (int i = centerIndex + 1; i < Math.min(candidates.size(), centerIndex + 1 + count); i++) {
            result.add(candidates.get(i).material);
        }

        return result;
    }

    private static class BlockColorInfo {
        Material material;
        Color color;
        float hue;
        float saturation;
        float brightness;

        public BlockColorInfo(Material material, Color color, float hue, float saturation, float brightness) {
            this.material = material;
            this.color = color;
            this.hue = hue;
            this.saturation = saturation;
            this.brightness = brightness;
        }
    }
}
