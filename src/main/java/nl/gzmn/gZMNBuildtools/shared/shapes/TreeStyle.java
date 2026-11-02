package nl.gzmn.gZMNBuildtools.shared.shapes;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/** Shapes of generated trees. */
public enum TreeStyle {
    OAK, SPRUCE, BIRCH, WILLOW, DEAD, PALM;

    public static List<String> ids() {
        return Arrays.stream(values()).map(s -> s.name().toLowerCase(Locale.ROOT)).toList();
    }
}
