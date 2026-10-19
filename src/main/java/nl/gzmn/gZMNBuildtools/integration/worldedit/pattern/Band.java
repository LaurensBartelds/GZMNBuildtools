package nl.gzmn.gZMNBuildtools.integration.worldedit.pattern;

import com.sk89q.worldedit.function.pattern.Pattern;

import java.util.Comparator;
import java.util.List;

/** A pattern that applies from {@code threshold} upwards (until the next band). */
public record Band(double threshold, Pattern pattern) {

    /** Returns the bands sorted ascending by threshold. */
    public static List<Band> sorted(List<Band> bands) {
        return bands.stream().sorted(Comparator.comparingDouble(Band::threshold)).toList();
    }

    /**
     * Picks the band with the highest threshold not above {@code value}; values
     * below the first threshold use the first band. {@code bands} must be sorted.
     */
    public static Pattern select(List<Band> bands, double value) {
        Pattern chosen = bands.get(0).pattern();
        for (Band band : bands) {
            if (value >= band.threshold()) {
                chosen = band.pattern();
            } else {
                break;
            }
        }
        return chosen;
    }
}
