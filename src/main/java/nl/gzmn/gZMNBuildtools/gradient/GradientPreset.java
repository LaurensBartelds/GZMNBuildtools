package nl.gzmn.gZMNBuildtools.gradient;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Represents a predefined gradient configuration.
 * Presets provide quick access to common color/material combinations.
 */
public class GradientPreset {

    private final String id;
    private final String displayName;
    private final String description;
    private final Material icon;
    private final List<String> blockIds;
    private final PresetCategory category;

    /**
     * Categories for organizing presets in the UI.
     */
    public enum PresetCategory {
        GRAYSCALE("Grayscale", Material.GRAY_WOOL, "Black, white, and gray tones"),
        WARM("Warm Tones", Material.ORANGE_WOOL, "Reds, oranges, and yellows"),
        COOL("Cool Tones", Material.BLUE_WOOL, "Blues, cyans, and purples"),
        NATURE("Nature", Material.OAK_LEAVES, "Greens and natural colors"),
        EARTH("Earth Tones", Material.BROWN_WOOL, "Browns, tans, and sandy colors"),
        STONE("Stone & Rock", Material.STONE, "Stone and rock materials"),
        CUSTOM("Custom", Material.NAME_TAG, "User-created gradients");

        private final String displayName;
        private final Material icon;
        private final String description;

        PresetCategory(String displayName, Material icon, String description) {
            this.displayName = displayName;
            this.icon = icon;
            this.description = description;
        }

        public String getDisplayName() {
            return displayName;
        }

        public Material getIcon() {
            return icon;
        }

        public String getDescription() {
            return description;
        }
    }

    private GradientPreset(Builder builder) {
        this.id = builder.id;
        this.displayName = builder.displayName;
        this.description = builder.description;
        this.icon = builder.icon;
        this.blockIds = Collections.unmodifiableList(new ArrayList<>(builder.blockIds));
        this.category = builder.category;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public Material getIcon() {
        return icon;
    }

    public List<String> getBlockIds() {
        return blockIds;
    }

    public PresetCategory getCategory() {
        return category;
    }

    /**
     * Get the block IDs as a comma-separated string for parsing.
     */
    public String getBlocksString() {
        return String.join(",", blockIds);
    }

    /**
     * Create a new builder for a preset.
     */
    public static Builder builder(String id) {
        return new Builder(id);
    }

    /**
     * Builder for GradientPreset.
     */
    public static class Builder {
        private final String id;
        private String displayName;
        private String description = "";
        private Material icon = Material.PAPER;
        private List<String> blockIds = new ArrayList<>();
        private PresetCategory category = PresetCategory.STONE;

        public Builder(String id) {
            this.id = id;
            this.displayName = id;
        }

        public Builder displayName(String displayName) {
            this.displayName = displayName;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder icon(Material icon) {
            this.icon = icon;
            return this;
        }

        public Builder blocks(String... blockIds) {
            this.blockIds = Arrays.asList(blockIds);
            return this;
        }

        public Builder blocks(List<String> blockIds) {
            this.blockIds = blockIds;
            return this;
        }

        public Builder category(PresetCategory category) {
            this.category = category;
            return this;
        }

        public GradientPreset build() {
            if (blockIds.size() < 2) {
                throw new IllegalArgumentException("Preset must have at least 2 blocks");
            }
            return new GradientPreset(this);
        }
    }

    @Override
    public String toString() {
        return String.format("GradientPreset{id='%s', blocks=%s}", id, blockIds);
    }
}
