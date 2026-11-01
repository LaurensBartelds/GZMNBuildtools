package nl.gzmn.gZMNBuildtools.modules.pixelart.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * How to build an image: its size in blocks, whether it stands as a wall or
 * lies on the floor, which blocks to use, dithering, and flat map-art mode.
 */
public record PixelArtOptions(String source, int width, int height, boolean floor, List<String> blockSets,
                              boolean dither, boolean map, boolean preview) {

    public static final String USAGE =
            "<image> [width=64] [height=auto] [wall|floor] [blocks=all|wool|concrete|terracotta|flat|stone|wood] "
                    + "[dither=on|off] [map] [-p]";
    public static final List<String> BLOCK_SETS = List.of("all", "wool", "concrete", "terracotta", "flat", "stone", "wood");

    /**
     * Parses the command arguments; {@code height} 0 means "keep the image's proportions".
     *
     * @throws IllegalArgumentException with a player-facing message
     */
    public static PixelArtOptions parse(String input, int maxSide) {
        String source = null;
        int width = 0;
        int height = 0;
        Boolean floor = null;
        List<String> sets = new ArrayList<>();
        Boolean dither = null;
        boolean map = false;
        boolean preview = false;
        for (String token : input == null ? new String[0] : input.trim().split("\\s+")) {
            if (token.isEmpty()) {
                continue;
            }
            String lower = token.toLowerCase(Locale.ROOT);
            int eq = lower.indexOf('=');
            if (source == null && eq < 0 && !lower.equals("-p")) {
                source = token;
                continue;
            }
            switch (eq < 0 ? lower : lower.substring(0, eq)) {
                case "-p", "preview" -> preview = true;
                case "wall" -> floor = false;
                case "floor" -> floor = true;
                case "map" -> map = true;
                case "width", "w" -> width = size(lower.substring(eq + 1), "width", maxSide);
                case "height", "h" -> height = size(lower.substring(eq + 1), "height", maxSide);
                case "dither" -> dither = switch (lower.substring(eq + 1)) {
                    case "on", "yes", "true" -> true;
                    case "off", "no", "false" -> false;
                    default -> throw new IllegalArgumentException("dither must be on or off");
                };
                case "blocks" -> {
                    for (String set : lower.substring(eq + 1).split(",")) {
                        if (!BLOCK_SETS.contains(set)) {
                            throw new IllegalArgumentException("Unknown block set '" + set + "'. Use: "
                                    + String.join(", ", BLOCK_SETS));
                        }
                        sets.add(set);
                    }
                }
                default -> throw new IllegalArgumentException("Unknown option '" + token + "'. Usage: " + USAGE);
            }
        }
        if (source == null) {
            throw new IllegalArgumentException("Name an image. Usage: " + USAGE);
        }
        if (map && Boolean.FALSE.equals(floor)) {
            throw new IllegalArgumentException("Map art lies on the floor; leave out 'wall'.");
        }
        if (width == 0) {
            width = map ? Math.min(128, maxSide) : Math.min(64, maxSide);
        }
        return new PixelArtOptions(source, width, height, map || Boolean.TRUE.equals(floor),
                sets.isEmpty() ? List.of("all") : List.copyOf(sets),
                dither == null || dither, map, preview);
    }

    private static int size(String value, String name, int max) {
        try {
            int n = Integer.parseInt(value);
            if (n < 1 || n > max) {
                throw new IllegalArgumentException(name + " must be between 1 and " + max);
            }
            return n;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(name + " must be a whole number");
        }
    }

    /** The height in blocks for an image of the given proportions. */
    public int heightFor(int imageWidth, int imageHeight) {
        if (height > 0) {
            return height;
        }
        return Math.max(1, (int) Math.round(width * (double) imageHeight / imageWidth));
    }
}
