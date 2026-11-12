package nl.gzmn.gZMNBuildtools.modules.pixelart.engine;

import java.util.HashMap;
import java.util.Map;

/**
 * A bundled 5x7 bitmap font (letters, digits, punctuation) that renders text
 * to a boolean pixel grid. Pure and deterministic; unknown characters render
 * as a question mark.
 */
public final class TextFont {

    public static final String DEFAULT = "default";
    public static final int GLYPH_W = 5;
    public static final int GLYPH_H = 7;
    public static final int SPACING = 1;

    private static final Map<Character, String[]> GLYPHS = new HashMap<>();

    static {
        glyph(' ', "     /     /     /     /     /     /     ");
        glyph('A', " ### /#   #/#   #/#####/#   #/#   #/#   #");
        glyph('B', "#### /#   #/#   #/#### /#   #/#   #/#### ");
        glyph('C', " ### /#   #/#    /#    /#    /#   #/ ### ");
        glyph('D', "#### /#   #/#   #/#   #/#   #/#   #/#### ");
        glyph('E', "#####/#    /#    /#### /#    /#    /#####");
        glyph('F', "#####/#    /#    /#### /#    /#    /#    ");
        glyph('G', " ### /#   #/#    /#  ##/#   #/#   #/ ### ");
        glyph('H', "#   #/#   #/#   #/#####/#   #/#   #/#   #");
        glyph('I', "#####/  #  /  #  /  #  /  #  /  #  /#####");
        glyph('J', "#####/   # /   # /   # /   #/#   #/ ### ");
        glyph('K', "#   #/#  # /# #  /##   /# #  /#  # /#   #");
        glyph('L', "#    /#    /#    /#    /#    /#    /#####");
        glyph('M', "#   #/## ##/# # #/#   #/#   #/#   #/#   #");
        glyph('N', "#   #/##  #/# # #/#  ##/#   #/#   #/#   #");
        glyph('O', " ### /#   #/#   #/#   #/#   #/#   #/ ### ");
        glyph('P', "#### /#   #/#   #/#### /#    /#    /#    ");
        glyph('Q', " ### /#   #/#   #/#   #/# # #/#  #/ ## #");
        glyph('R', "#### /#   #/#   #/#### /# #  /#  # /#   #");
        glyph('S', " ####/#    /#    / ### /    #/    #/#### ");
        glyph('T', "#####/  #  /  #  /  #  /  #  /  #  /  #  ");
        glyph('U', "#   #/#   #/#   #/#   #/#   #/#   #/ ### ");
        glyph('V', "#   #/#   #/#   #/#   #/#   #/#   #/ # # ");
        glyph('W', "#   #/#   #/#   #/#   #/# # #/## ##/#   #");
        glyph('X', "#   #/#   #/#   #/ # # /#   #/#   #/#   #");
        glyph('Y', "#   #/#   #/ # # /  #  /  #  /  #  /  #  ");
        glyph('Z', "#####/   # /  #  /  #  / #   /#    /#####");
        glyph('0', " ### /#   #/#  ##/# # #/#   #/#   #/ ### ");
        glyph('1', "  #  / ##  /  #  /  #  /  #  /  #  /#####");
        glyph('2', " ### /#   #/    #/   # /  #  / #   /#####");
        glyph('3', "#### /    #/   # /  ## /    #/#   #/ ### ");
        glyph('4', "   # /  ## / # # /#  #/#####/   #/   #");
        glyph('5', "#####/#    /#### /    #/    #/#   #/ ### ");
        glyph('6', " ### /#    /#    /#### /#   #/#   #/ ### ");
        glyph('7', "#####/    #/   # /  #  / #   / #   / #   ");
        glyph('8', " ### /#   #/#   #/ ### /#   #/#   #/ ### ");
        glyph('9', " ### /#   #/#   #/ ####/    #/    #/ ### ");
        glyph('.', "     /     /     /     /     / ##  / ##  ");
        glyph(',', "     /     /     /     / ##  / ##  / #   ");
        glyph('!', "  #  /  #  /  #  /  #  /  #  /     /  #  ");
        glyph('?', " ### /#   #/    #/   # /     /  #  /  #  ");
        glyph(':', "     / ##  / ##  /     / ##  / ##  /     ");
        glyph(';', "     / ##  / ##  /     / ##  / ##  / #   ");
        glyph('\'', "  #  /  #  / #   /     /     /     /     ");
        glyph('-', "     /     /     /#####/     /     /     ");
        glyph('+', "     /  #  /  #  /#####/  #  /  #  /     ");
        glyph('/', "    #/   # /  #  /  #  / #   /#    /     ");
        glyph('(', "   # /  #  / #   / #   / #   /  #  /   # ");
        glyph(')', " #   /  #  /   # /   # /   # /  #  / #   ");
        glyph('=', "     /     /#####/     /#####/     /     ");
        glyph('%', "#   #/   # /  #  /  #  / #   /#   #/#   #");
    }

    private TextFont() {
    }

    private static void glyph(char c, String rows) {
        GLYPHS.put(c, rows.split("/", -1));
    }

    /** All font names this build knows. */
    public static java.util.List<String> fonts() {
        return java.util.List.of(DEFAULT);
    }

    /**
     * Renders {@code text} (uppercased) at {@code size} (each font pixel becomes
     * size x size blocks) to a grid of {@code [height][width]} booleans, top row first.
     */
    public static boolean[][] render(String text, int size) {
        String upper = text.toUpperCase(java.util.Locale.ROOT);
        int width = Math.max(1, upper.length() * (GLYPH_W + SPACING) - SPACING) * size;
        int height = GLYPH_H * size;
        boolean[][] grid = new boolean[height][width];
        int ox = 0;
        for (char c : upper.toCharArray()) {
            String[] glyph = GLYPHS.getOrDefault(c, GLYPHS.get('?'));
            for (int row = 0; row < GLYPH_H; row++) {
                for (int col = 0; col < GLYPH_W; col++) {
                    if (glyph[row].charAt(col) == '#') {
                        for (int sy = 0; sy < size; sy++) {
                            for (int sx = 0; sx < size; sx++) {
                                grid[row * size + sy][ox + col * size + sx] = true;
                            }
                        }
                    }
                }
            }
            ox += (GLYPH_W + SPACING) * size;
        }
        return grid;
    }
}
