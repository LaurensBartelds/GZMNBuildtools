package nl.gzmn.gZMNBuildtools.integration.worldedit;

import com.sk89q.worldedit.function.pattern.Pattern;
import nl.gzmn.gZMNBuildtools.integration.worldedit.pattern.Band;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;

class BandTest {

    private final Pattern grass = mock(Pattern.class);
    private final Pattern stone = mock(Pattern.class);
    private final Pattern snow = mock(Pattern.class);

    private final List<Band> bands = Band.sorted(List.of(
            new Band(60, snow), new Band(0, grass), new Band(30, stone)));

    @Test
    void picksHighestThresholdNotAboveValue() {
        assertSame(grass, Band.select(bands, 10));
        assertSame(stone, Band.select(bands, 30));
        assertSame(stone, Band.select(bands, 59.9));
        assertSame(snow, Band.select(bands, 85));
    }

    @Test
    void valuesBelowFirstThresholdUseFirstBand() {
        assertSame(grass, Band.select(bands, -5));
    }
}
