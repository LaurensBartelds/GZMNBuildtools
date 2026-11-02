package nl.gzmn.gZMNBuildtools.modules.pixelart.engine;

import nl.gzmn.gZMNBuildtools.shared.palette.OkLab;

import java.util.List;

/**
 * Turns image pixels into blocks: each pixel gets the block whose colour is
 * closest in OKLab, optionally with Floyd-Steinberg error diffusion so
 * gradients and shading survive the limited block palette.
 */
public final class ImageQuantizer {

    /** A block and the colour it shows. */
    public record Swatch(String block, OkLab colour) {
    }

    private final List<Swatch> swatches;

    public ImageQuantizer(List<Swatch> swatches) {
        if (swatches.isEmpty()) {
            throw new IllegalArgumentException("No blocks to build the image from");
        }
        this.swatches = List.copyOf(swatches);
    }

    /**
     * @param argb   pixels, row by row from the top-left
     * @param dither spread each pixel's colour error to its neighbours
     * @return a block id per pixel, or null where the pixel is (mostly) transparent
     */
    public String[] quantize(int[] argb, int width, int height, boolean dither) {
        String[] out = new String[width * height];
        double[][] lab = new double[width * height][];
        for (int i = 0; i < argb.length; i++) {
            if ((argb[i] >>> 24) >= 128) {
                OkLab c = OkLab.fromRgb(argb[i] & 0xFFFFFF);
                lab[i] = new double[]{c.l(), c.a(), c.b()};
            }
        }
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int i = y * width + x;
                if (lab[i] == null) {
                    continue;
                }
                OkLab wanted = new OkLab(lab[i][0], lab[i][1], lab[i][2]);
                Swatch best = nearest(wanted);
                out[i] = best.block();
                if (dither) {
                    double el = wanted.l() - best.colour().l();
                    double ea = wanted.a() - best.colour().a();
                    double eb = wanted.b() - best.colour().b();
                    spread(lab, width, height, x + 1, y, el, ea, eb, 7 / 16.0);
                    spread(lab, width, height, x - 1, y + 1, el, ea, eb, 3 / 16.0);
                    spread(lab, width, height, x, y + 1, el, ea, eb, 5 / 16.0);
                    spread(lab, width, height, x + 1, y + 1, el, ea, eb, 1 / 16.0);
                }
            }
        }
        return out;
    }

    private static void spread(double[][] lab, int width, int height, int x, int y, double el, double ea, double eb,
                               double share) {
        if (x < 0 || x >= width || y >= height) {
            return;
        }
        double[] c = lab[y * width + x];
        if (c != null) {
            c[0] += el * share;
            c[1] += ea * share;
            c[2] += eb * share;
        }
    }

    Swatch nearest(OkLab colour) {
        Swatch best = swatches.get(0);
        double bestDistance = Double.MAX_VALUE;
        for (Swatch s : swatches) {
            double d = s.colour().distance(colour);
            if (d < bestDistance) {
                bestDistance = d;
                best = s;
            }
        }
        return best;
    }
}
