package nl.gzmn.gZMNBuildtools.modules.pixelart;

import nl.gzmn.gZMNBuildtools.modules.pixelart.engine.BlockSets;
import nl.gzmn.gZMNBuildtools.modules.pixelart.engine.ImageQuantizer;
import nl.gzmn.gZMNBuildtools.modules.pixelart.engine.PixelArtOptions;
import nl.gzmn.gZMNBuildtools.modules.pixelart.image.ImageLoader;
import nl.gzmn.gZMNBuildtools.shared.palette.BlockColors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import javax.imageio.ImageIO;

import static org.junit.jupiter.api.Assertions.*;

class PixelArtTest {

    private static final BlockColors COLORS = BlockColors.bundled();

    private static ImageQuantizer quantizer(String set) {
        Predicate<String> allowed = BlockSets.of(List.of(set));
        return new ImageQuantizer(COLORS.ids().stream().filter(allowed)
                .filter(id -> !id.contains("glazed") && !id.contains("carpet"))
                .map(id -> new ImageQuantizer.Swatch(id, COLORS.side(id).orElseThrow()))
                .toList());
    }

    @Test
    void solidColoursMapToTheMatchingBlock() {
        int red = COLORS.get("red_wool").orElseThrow().sideRgb() | 0xFF000000;
        int white = COLORS.get("white_wool").orElseThrow().sideRgb() | 0xFF000000;
        String[] out = quantizer("wool").quantize(new int[]{red, white, 0x00000000, red}, 2, 2, false);
        assertEquals("red_wool", out[0]);
        assertEquals("white_wool", out[1]);
        assertNull(out[2], "transparent pixels stay empty");
    }

    @Test
    void ditheringMixesNeighboursForInBetweenColours() {
        int black = COLORS.get("black_concrete").orElseThrow().sideRgb();
        int white = COLORS.get("white_concrete").orElseThrow().sideRgb();
        int mid = 0xFF000000 | ((((black >> 16 & 255) + (white >> 16 & 255)) / 2) << 16)
                | ((((black >> 8 & 255) + (white >> 8 & 255)) / 2) << 8) | (((black & 255) + (white & 255)) / 2);
        int[] pixels = new int[16 * 16];
        Arrays.fill(pixels, mid);
        ImageQuantizer q = new ImageQuantizer(List.of(
                new ImageQuantizer.Swatch("black_concrete", COLORS.side("black_concrete").orElseThrow()),
                new ImageQuantizer.Swatch("white_concrete", COLORS.side("white_concrete").orElseThrow())));
        assertEquals(1, Arrays.stream(q.quantize(pixels, 16, 16, false)).distinct().count());
        long blacks = Arrays.stream(q.quantize(pixels, 16, 16, true)).filter("black_concrete"::equals).count();
        assertTrue(blacks > 60 && blacks < 196, "dithered mix, got " + blacks + " black of 256");
    }

    @Test
    void optionsParse() {
        PixelArtOptions o = PixelArtOptions.parse("logo.png width=32 floor blocks=wool,concrete dither=off -p", 256);
        assertEquals("logo.png", o.source());
        assertEquals(32, o.width());
        assertTrue(o.floor());
        assertEquals(List.of("wool", "concrete"), o.blockSets());
        assertFalse(o.dither());
        assertTrue(o.preview());
        assertEquals(16, o.heightFor(200, 100));
        PixelArtOptions map = PixelArtOptions.parse("art.png map", 256);
        assertTrue(map.map() && map.floor());
        assertEquals(128, map.width());
        assertThrows(IllegalArgumentException.class, () -> PixelArtOptions.parse("", 256));
        assertThrows(IllegalArgumentException.class, () -> PixelArtOptions.parse("a.png width=999", 256));
        assertThrows(IllegalArgumentException.class, () -> PixelArtOptions.parse("a.png blocks=gold", 256));
        assertThrows(IllegalArgumentException.class, () -> PixelArtOptions.parse("a.png map wall", 256));
    }

    @Test
    void onlyPublicAddressesAreAllowed() throws Exception {
        for (String blocked : List.of("127.0.0.1", "10.1.2.3", "192.168.1.1", "172.16.0.5", "169.254.169.254",
                "0.0.0.0", "100.64.1.1", "::1", "fd00::1", "fe80::1")) {
            assertFalse(ImageLoader.isPublic(InetAddress.getByName(blocked)), blocked);
        }
        assertTrue(ImageLoader.isPublic(InetAddress.getByName("8.8.8.8")));
        assertTrue(ImageLoader.isPublic(InetAddress.getByName("2606:4700::1111")));
    }

    @Test
    void localImagesStayInTheirFolderAndAreSizeChecked(@TempDir Path dir) throws Exception {
        BufferedImage image = new BufferedImage(40, 20, BufferedImage.TYPE_INT_ARGB);
        ByteArrayOutputStream png = new ByteArrayOutputStream();
        ImageIO.write(image, "png", png);
        Files.write(dir.resolve("art.png"), png.toByteArray());
        Files.writeString(dir.resolve("notes.txt"), "hi");

        ImageLoader loader = new ImageLoader(dir, new ImageLoader.Limits(1 << 20, 1024, false, false));
        assertEquals(40, loader.load("art.png").getWidth());
        assertEquals(List.of("art.png"), loader.localImages());
        assertThrows(IllegalArgumentException.class, () -> loader.load("../outside.png"));
        assertThrows(IllegalArgumentException.class, () -> loader.load("notes.txt"));
        assertThrows(IllegalArgumentException.class, () -> loader.load("missing.png"));
        assertThrows(IllegalArgumentException.class, () -> loader.load("https://example.com/a.png"),
                "web loading is off in these limits");
        assertThrows(IllegalArgumentException.class, () -> ImageLoader.decode(png.toByteArray(), 30),
                "images over the dimension limit are refused before decoding");
        assertEquals(10 * 5, ImageLoader.scaledPixels(loader.load("art.png"), 10, 5).length);
    }
}
