package nl.gzmn.gZMNBuildtools.modules.materialswap.engine;

import nl.gzmn.gZMNBuildtools.shared.palette.BlockColors;

import java.util.Locale;
import java.util.function.Predicate;

/**
 * What a material swap does: the new material, optionally only replacing one
 * existing material ({@code from}), and whether shapes and mossy/cracked forms are kept.
 */
public record SwapOptions(String to, String from, int radius, boolean keepShapes, boolean keepTextures) {

    public static final String USAGE = "<to-block> [from=<block>] [radius=4] [shapes=keep|full] [textures=keep|plain]";

    /**
     * Parses "{@code deepslate_bricks from=stone_bricks radius=6}".
     *
     * @throws IllegalArgumentException with a player-facing message
     */
    public static SwapOptions parse(String input, Predicate<String> isBlock, int maxRadius, int defaultRadius) {
        String to = null;
        String from = null;
        int radius = defaultRadius;
        boolean shapes = true;
        boolean textures = true;
        for (String token : input == null ? new String[0] : input.trim().split("\\s+")) {
            if (token.isEmpty()) {
                continue;
            }
            int eq = token.indexOf('=');
            if (eq < 0) {
                if (to != null) {
                    throw new IllegalArgumentException("Expected key=value but got '" + token + "'. Usage: " + USAGE);
                }
                to = block(token, isBlock);
                continue;
            }
            String key = token.substring(0, eq).toLowerCase(Locale.ROOT);
            String value = token.substring(eq + 1).toLowerCase(Locale.ROOT);
            switch (key) {
                case "from" -> from = block(value, isBlock);
                case "radius", "size" -> {
                    try {
                        radius = Integer.parseInt(value);
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException("radius must be a whole number");
                    }
                    if (radius < 1 || radius > maxRadius) {
                        throw new IllegalArgumentException("radius must be between 1 and " + maxRadius);
                    }
                }
                case "shapes" -> shapes = flag(value, "keep", "full", "shapes");
                case "textures" -> textures = flag(value, "keep", "plain", "textures");
                default -> throw new IllegalArgumentException("Unknown option '" + key + "'. Usage: " + USAGE);
            }
        }
        if (to == null) {
            throw new IllegalArgumentException("Name the new material. Usage: " + USAGE);
        }
        return new SwapOptions(to, from, radius, shapes, textures);
    }

    private static String block(String raw, Predicate<String> isBlock) {
        String id = BlockColors.normalize(raw);
        if (!isBlock.test(id)) {
            throw new IllegalArgumentException("Unknown block '" + id + "'");
        }
        return id;
    }

    private static boolean flag(String value, String yes, String no, String name) {
        if (value.equals(yes)) {
            return true;
        }
        if (value.equals(no)) {
            return false;
        }
        throw new IllegalArgumentException(name + " must be " + yes + " or " + no);
    }

    public String describe() {
        return to + (from == null ? "" : " (replacing " + from + ")") + ", radius " + radius
                + (keepShapes ? "" : ", full blocks") + (keepTextures ? "" : ", plain textures");
    }
}
