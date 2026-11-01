package nl.gzmn.gZMNBuildtools.modules.terraintexture.engine;

/** An inclusive number range written as "a..b", "a.." (at least), "..b" (at most) or "a". */
public record Range(double min, double max) {

    public static final Range ANY = new Range(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);

    public static Range parse(String text) {
        String t = text.trim();
        int dots = t.indexOf("..");
        try {
            if (dots < 0) {
                double v = Double.parseDouble(t);
                return new Range(v, v);
            }
            String lo = t.substring(0, dots).trim();
            String hi = t.substring(dots + 2).trim();
            return new Range(lo.isEmpty() ? Double.NEGATIVE_INFINITY : Double.parseDouble(lo),
                    hi.isEmpty() ? Double.POSITIVE_INFINITY : Double.parseDouble(hi));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Not a range: '" + text + "' (use 10..20, 10.., ..20 or 15)");
        }
    }

    public boolean contains(double value) {
        return value >= min && value <= max;
    }
}
