package nl.gzmn.gZMNBuildtools.command.brush;

import org.junit.jupiter.api.Test;
import java.util.Base64;
import static org.junit.jupiter.api.Assertions.*;

public class CodeDecoderTest {

    @Test
    public void testValidCode() {
        String data = "up|linear|stone,cobblestone|andesite";
        String code = "gzmn@" + Base64.getEncoder().encodeToString(data.getBytes());
        String expected = "#gradient[up][linear][stone,cobblestone][andesite]";
        assertEquals(expected, CodeDecoder.decodeToPattern(code));
    }

    @Test
    public void testInvalidPrefix() {
        String data = "up|linear|stone";
        String code = "wrong@" + Base64.getEncoder().encodeToString(data.getBytes());
        assertNull(CodeDecoder.decodeToPattern(code));
    }

    @Test
    public void testInvalidBase64() {
        String code = "gzmn@!@#$";
        assertNull(CodeDecoder.decodeToPattern(code));
    }

    @Test
    public void testNotEnoughParts() {
        String data = "up|linear";
        String code = "gzmn@" + Base64.getEncoder().encodeToString(data.getBytes());
        assertNull(CodeDecoder.decodeToPattern(code));
    }
}
