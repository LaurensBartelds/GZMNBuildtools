package nl.gzmn.gZMNBuildtools.modules.gradientbrush;

import java.util.List;

/** A resolved palette for display: each stop is a weighted mix of blocks with their colours. */
public record PaletteView(List<List<Entry>> stops, String text) {

    public record Entry(String block, double weight, int rgb) {
    }
}
