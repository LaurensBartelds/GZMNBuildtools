package nl.gzmn.gZMNBuildtools.modules.pixelart;

import nl.gzmn.gZMNBuildtools.modules.pixelart.engine.TextFont;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextFontTest {

    @Test
    void rendersGlyphShape() {
        boolean[][] grid = TextFont.render("A", 1);
        assertEquals(7, grid.length);
        assertEquals(5, grid[0].length);
        assertTrue(grid[0][1]);
        assertFalse(grid[0][0]);
        assertTrue(grid[3][0]);
        assertTrue(grid[3][4]);
        assertTrue(grid[6][0]);
    }

    @Test
    void sizeScalesPixels() {
        boolean[][] grid = TextFont.render("A", 2);
        assertEquals(14, grid.length);
        assertEquals(10, grid[0].length);
        assertTrue(grid[0][2]);
        assertTrue(grid[1][3]);
        assertFalse(grid[0][0]);
    }

    @Test
    void widthGrowsWithTextAndLowercaseIsUppercased() {
        boolean[][] ab = TextFont.render("ab", 1);
        assertEquals(7, ab.length);
        assertEquals(11, ab[0].length);
        boolean[][] AB = TextFont.render("AB", 1);
        assertEquals(AB.length, ab.length);
        for (int y = 0; y < AB.length; y++) {
            for (int x = 0; x < AB[0].length; x++) {
                assertEquals(AB[y][x], ab[y][x]);
            }
        }
    }

    @Test
    void unknownCharacterRendersAsQuestionMark() {
        boolean[][] unknown = TextFont.render("@", 1);
        boolean[][] question = TextFont.render("?", 1);
        for (int y = 0; y < 7; y++) {
            for (int x = 0; x < 5; x++) {
                assertEquals(question[y][x], unknown[y][x]);
            }
        }
    }
}
