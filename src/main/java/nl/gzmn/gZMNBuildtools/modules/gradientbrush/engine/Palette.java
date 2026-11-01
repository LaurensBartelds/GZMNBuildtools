package nl.gzmn.gZMNBuildtools.modules.gradientbrush.engine;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Evenly spaced gradient stops, each a weighted mix of blocks. The block for a
 * gradient position is chosen with a dither threshold, so neighbouring stops
 * blend into each other instead of forming hard bands.
 */
public record Palette(List<Stop> stops) {

    public record Weighted(String block, double weight) {
    }

    public record Stop(List<Weighted> blocks) {
        public Stop {
            if (blocks.isEmpty()) {
                throw new IllegalArgumentException("A palette stop needs at least one block");
            }
            blocks = List.copyOf(blocks);
        }

        public static Stop of(String block) {
            return new Stop(List.of(new Weighted(block, 1)));
        }

        /** Weighted choice for a roll in [0, 1). */
        public String pick(double roll) {
            double total = blocks.stream().mapToDouble(Weighted::weight).sum();
            double r = roll * total;
            for (Weighted w : blocks) {
                r -= w.weight();
                if (r < 0) {
                    return w.block();
                }
            }
            return blocks.get(blocks.size() - 1).block();
        }

        @Override
        public String toString() {
            if (blocks.size() == 1) {
                return blocks.get(0).block();
            }
            return blocks.stream().map(w -> Math.round(w.weight() * 100) + "%" + w.block())
                    .collect(Collectors.joining(","));
        }
    }

    public Palette {
        if (stops.isEmpty()) {
            throw new IllegalArgumentException("A palette needs at least one block");
        }
        stops = List.copyOf(stops);
    }

    public static Palette ofBlocks(List<String> blocks) {
        List<Stop> stops = new ArrayList<>();
        blocks.forEach(b -> stops.add(Stop.of(b)));
        return new Palette(stops);
    }

    /**
     * @param t         gradient position, 0 (start) to 1 (end)
     * @param threshold dither threshold in [0, 1)
     * @param roll      random-looking value in [0, 1) for mixed stops
     * @param softness  0 = hard bands, 1 = each band fully dithered into the next
     */
    public String blockAt(double t, double threshold, double roll, double softness) {
        int n = stops.size();
        if (n == 1) {
            return stops.get(0).pick(roll);
        }
        double f = Math.max(0, Math.min(1, t)) * (n - 1);
        int i = Math.min(n - 2, (int) Math.floor(f));
        double frac = f - i;
        double blend;
        if (softness <= 0) {
            blend = frac < 0.5 ? 0 : 1;
        } else {
            blend = Math.max(0, Math.min(1, (frac - 0.5) / softness + 0.5));
        }
        return stops.get(blend > threshold ? i + 1 : i).pick(roll);
    }

    /** Every distinct block used by the palette. */
    public Set<String> blocks() {
        Set<String> out = new LinkedHashSet<>();
        stops.forEach(s -> s.blocks().forEach(w -> out.add(w.block())));
        return out;
    }

    public Palette reversed() {
        List<Stop> copy = new ArrayList<>(stops);
        java.util.Collections.reverse(copy);
        return new Palette(copy);
    }

    @Override
    public String toString() {
        return stops.stream().map(Stop::toString).collect(Collectors.joining(" > "));
    }
}
