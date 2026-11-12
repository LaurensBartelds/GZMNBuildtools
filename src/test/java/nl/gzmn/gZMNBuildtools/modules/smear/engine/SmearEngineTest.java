package nl.gzmn.gZMNBuildtools.modules.smear.engine;

import nl.gzmn.gZMNBuildtools.modules.smear.engine.SmearEngine.BlockGrid;
import nl.gzmn.gZMNBuildtools.modules.smear.engine.SmearEngine.Cell;
import nl.gzmn.gZMNBuildtools.modules.smear.engine.SmearEngine.Plan;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SmearEngineTest {

    private static BlockGrid stoneWithBump() {
        return (x, y, z) -> {
            if (x < 0 || x > 4 || y < 0 || y > 4 || z < 0 || z > 4) {
                return null;
            }
            return (x == 2 && y == 2 && z == 2) ? "gold_block" : "stone";
        };
    }

    @Test
    void smearsLeadingSurfaceForward() {
        Plan plan = SmearEngine.smear(stoneWithBump(), new int[]{2, 2, 2}, 2, new int[]{1, 0, 0}, 2);
        assertEquals(new Cell(4, 2, 2), plan.changes().get(new Cell(5, 2, 2)));
        assertEquals(new Cell(4, 2, 2), plan.changes().get(new Cell(6, 2, 2)));
        assertNull(plan.changes().get(new Cell(7, 2, 2)));
    }

    @Test
    void negativeAmountDragsBack() {
        Plan plan = SmearEngine.smear(stoneWithBump(), new int[]{2, 2, 2}, 2, new int[]{1, 0, 0}, -2);
        assertEquals(new Cell(4, 2, 2), plan.changes().get(new Cell(3, 2, 2)));
        assertEquals(new Cell(4, 2, 2), plan.changes().get(new Cell(2, 2, 2)));
        assertNull(plan.changes().get(new Cell(5, 2, 2)));
    }

    @Test
    void smearsAlongNegativeAxis() {
        Plan plan = SmearEngine.smear(stoneWithBump(), new int[]{2, 2, 2}, 2, new int[]{-1, 0, 0}, 1);
        assertEquals(new Cell(0, 2, 2), plan.changes().get(new Cell(-1, 2, 2)));
    }

    @Test
    void verticalSmearUsesHeightLines() {
        Plan plan = SmearEngine.smear(stoneWithBump(), new int[]{2, 2, 2}, 2, new int[]{0, 1, 0}, 1);
        assertEquals(new Cell(2, 4, 2), plan.changes().get(new Cell(2, 5, 2)));
    }
}
