package nl.gzmn.gZMNBuildtools.palette;

/**
 * A colour in the OKLab perceptual colour space, where straight-line
 * distances and interpolation match how different colours look.
 */
public record OkLab(double l, double a, double b) {

    public static OkLab fromRgb(int rgb) {
        double r = toLinear((rgb >> 16) & 0xFF);
        double g = toLinear((rgb >> 8) & 0xFF);
        double bl = toLinear(rgb & 0xFF);
        double lc = Math.cbrt(0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * bl);
        double mc = Math.cbrt(0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * bl);
        double sc = Math.cbrt(0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * bl);
        return new OkLab(
                0.2104542553 * lc + 0.7936177850 * mc - 0.0040720468 * sc,
                1.9779984951 * lc - 2.4285922050 * mc + 0.4505937099 * sc,
                0.0259040371 * lc + 0.7827717662 * mc - 0.8086757660 * sc);
    }

    public static OkLab fromHex(String hex) {
        String h = hex.startsWith("#") ? hex.substring(1) : hex;
        if (!h.matches("[0-9a-fA-F]{6}")) {
            throw new IllegalArgumentException("Not a #RRGGBB colour: " + hex);
        }
        return fromRgb(Integer.parseInt(h, 16));
    }

    public int toRgb() {
        double lc = l + 0.3963377774 * a + 0.2158037573 * b;
        double mc = l - 0.1055613458 * a - 0.0638541728 * b;
        double sc = l - 0.0894841775 * a - 1.2914855480 * b;
        double lr = lc * lc * lc;
        double mr = mc * mc * mc;
        double sr = sc * sc * sc;
        int r = toSrgb(4.0767416621 * lr - 3.3077115913 * mr + 0.2309699292 * sr);
        int g = toSrgb(-1.2684380046 * lr + 2.6097574011 * mr - 0.3413193965 * sr);
        int bl = toSrgb(-0.0041960863 * lr - 0.7034186147 * mr + 1.7076147010 * sr);
        return (r << 16) | (g << 8) | bl;
    }

    public String toHex() {
        return String.format("#%06X", toRgb());
    }

    public double distance(OkLab other) {
        double dl = l - other.l;
        double da = a - other.a;
        double db = b - other.b;
        return Math.sqrt(dl * dl + da * da + db * db);
    }

    /** Distance in hue/chroma only, ignoring lightness. */
    public double chromaDistance(OkLab other) {
        double da = a - other.a;
        double db = b - other.b;
        return Math.sqrt(da * da + db * db);
    }

    public double chroma() {
        return Math.sqrt(a * a + b * b);
    }

    public OkLab lerp(OkLab to, double t) {
        return new OkLab(l + (to.l - l) * t, a + (to.a - a) * t, b + (to.b - b) * t);
    }

    private static double toLinear(int channel) {
        double c = channel / 255.0;
        return c <= 0.04045 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
    }

    private static int toSrgb(double linear) {
        double c = linear <= 0.0031308 ? linear * 12.92 : 1.055 * Math.pow(Math.max(0, linear), 1 / 2.4) - 0.055;
        return (int) Math.max(0, Math.min(255, Math.round(c * 255)));
    }
}
