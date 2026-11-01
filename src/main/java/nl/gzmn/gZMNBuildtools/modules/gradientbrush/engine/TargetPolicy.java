package nl.gzmn.gZMNBuildtools.modules.gradientbrush.engine;

import java.util.Set;

/**
 * Decides which blocks belong to the build being painted (the flood fill) and
 * which of those get repainted.
 */
public final class TargetPolicy {

    private final TargetMode mode;
    private final BlockRules rules;
    private final Set<String> targetGroups;

    /**
     * @param targetGroups material groups that are always part of the build:
     *                     the clicked material plus the palette's own materials
     *                     (so a second stroke over painted blocks still works)
     */
    public TargetPolicy(TargetMode mode, BlockRules rules, Set<String> targetGroups) {
        this.mode = mode;
        this.rules = rules;
        this.targetGroups = Set.copyOf(targetGroups);
    }

    public TargetMode mode() {
        return mode;
    }

    public Set<String> targetGroups() {
        return targetGroups;
    }

    /** Whether the flood fill may pass through this block. */
    public boolean member(String id) {
        if (id == null) {
            return false;
        }
        boolean targeted = targetGroups.contains(rules.group(id));
        return switch (mode) {
            case CLICKED -> targeted && rules.isStructural(id);
            case MAIN, STRUCTURAL -> rules.isStructural(id) && (targeted || !rules.isTerrain(id));
            case ALL -> !isLiquid(id) && (targeted || !rules.isTerrain(id));
        };
    }

    /** Whether a member block is repainted, given the build's main material groups. */
    public boolean paint(String id, Set<String> mainGroups) {
        if (id == null) {
            return false;
        }
        return switch (mode) {
            case CLICKED, STRUCTURAL -> rules.isStructural(id);
            case MAIN -> rules.isStructural(id)
                    && (mainGroups.contains(rules.group(id)) || targetGroups.contains(rules.group(id)));
            case ALL -> !isLiquid(id);
        };
    }

    private static boolean isLiquid(String id) {
        return id.equals("water") || id.equals("lava") || id.equals("bubble_column");
    }
}
