package nl.gzmn.gZMNBuildtools.modules.terraintexture.engine;

import java.util.List;

/**
 * One line of a preset: when a block's depth, slope, height and noise all fall
 * in range, it becomes one of {@code place} (weighted) or, when {@code bands}
 * is set, the band for its height (badlands-style stripes).
 */
public record TextureRule(Range depth, Range slope, Range height, Range noise, List<Weighted> place,
                          List<String> bands, int bandHeight) {

    public record Weighted(String block, double weight) {
    }

    public boolean matches(int depth, double slope, int y, double noise) {
        return this.depth.contains(depth) && this.slope.contains(slope) && height.contains(y)
                && this.noise.contains(noise);
    }

    /** The block for this position; {@code roll} is a stable random number in [0, 1). */
    public String choose(int y, double roll) {
        if (!bands.isEmpty()) {
            return bands.get(Math.floorMod(Math.floorDiv(y, Math.max(1, bandHeight)), bands.size()));
        }
        double total = place.stream().mapToDouble(Weighted::weight).sum();
        double r = roll * total;
        for (Weighted w : place) {
            r -= w.weight();
            if (r < 0) {
                return w.block();
            }
        }
        return place.get(place.size() - 1).block();
    }
}
