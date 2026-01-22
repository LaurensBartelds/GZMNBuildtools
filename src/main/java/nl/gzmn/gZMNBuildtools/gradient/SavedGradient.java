package nl.gzmn.gZMNBuildtools.gradient;

import java.util.UUID;

/**
 * parsed data object for a saved gradient.
 */
public class SavedGradient {
    private final String id;
    private final String name;
    private final UUID authorInfo;
    private final String authorName;
    private final GradientPreset preset;
    private final boolean isPublic;
    private final long createdAt;

    public SavedGradient(String id, String name, UUID authorInfo, String authorName, GradientPreset preset,
            boolean isPublic, long createdAt) {
        this.id = id;
        this.name = name;
        this.authorInfo = authorInfo;
        this.authorName = authorName;
        this.preset = preset;
        this.isPublic = isPublic;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public UUID getAuthorInfo() {
        return authorInfo;
    }

    public String getAuthorName() {
        return authorName;
    }

    public GradientPreset getPreset() {
        return preset;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public long getCreatedAt() {
        return createdAt;
    }
}
