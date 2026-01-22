package nl.gzmn.gZMNBuildtools.gradient.service;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.stream.Collectors;


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

            
            String name = material.name();
            if (name.contains("DOOR") || name.contains("FENCE") || name.contains("WALL") ||
                    name.contains("PANE") || name.contains("TORCH") || name.contains("FLOWER")
                    || name.contains("PLANT")) {
                continue;
            }

            try {
                BlockData itemData = material.createBlockData();
                Color color = itemData.getMapColor();

                
                
                
                

                float[] hsv = new float[3];
                java.awt.Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), hsv);

                blockColors.put(material, new BlockColorInfo(material, color, hsv[0], hsv[1], hsv[2]));

            } catch (Exception ignored) {
                
            }
        }
    }

    
    public List<Material> getGradientSuggestions(Material input, int count) {
        BlockColorInfo inputInfo = blockColors.get(input);
        if (inputInfo == null)
            return Collections.emptyList();

        
        double hueThreshold = 0.1; 
        double satThreshold = 0.3; 

        List<BlockColorInfo> candidates = blockColors.values().stream()
                .filter(info -> Math.abs(info.hue - inputInfo.hue) < hueThreshold ||
                        Math.abs(info.hue - inputInfo.hue) > (1.0 - hueThreshold)) 
                .filter(info -> Math.abs(info.saturation - inputInfo.saturation) < satThreshold)
                .sorted(Comparator.comparingDouble(info -> info.brightness))
                .collect(Collectors.toList());

        
        if (candidates.size() < count * 2) {
            candidates = blockColors.values().stream()
                    .filter(info -> Math.abs(info.saturation - inputInfo.saturation) < 0.5) 
                    .sorted(Comparator.comparingDouble(info -> info.brightness))
                    .collect(Collectors.toList());
        }

        
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

        
        for (int i = Math.max(0, centerIndex - count); i < centerIndex; i++) {
            result.add(candidates.get(i).material);
        }

        

        
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
