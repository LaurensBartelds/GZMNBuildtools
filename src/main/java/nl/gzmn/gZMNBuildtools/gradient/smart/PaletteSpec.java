package nl.gzmn.gZMNBuildtools.gradient.smart;

import nl.gzmn.gZMNBuildtools.palette.BlockColors;
import nl.gzmn.gZMNBuildtools.palette.OkLab;
import nl.gzmn.gZMNBuildtools.palette.PaletteGenerator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;

/**
 * A palette as typed by a player, resolved into concrete blocks when used.
 *
 * <ul>
 *   <li>{@code auto} - an aged/shaded ramp of the build's own main material</li>
 *   <li>{@code aged:<block>}, {@code shades:<block>} - generated from one block</li>
 *   <li>{@code <a>..<b>} - an even colour ramp between two blocks</li>
 *   <li>{@code preset:<id>} - a gradient preset</li>
 *   <li>{@code stone,andesite,#3a3a3a} or {@code [stone][50%andesite,50%tuff]} - explicit stops;
 *       {@code #RRGGBB} picks the closest block</li>
 * </ul>
 */
public final class PaletteSpec {

    public enum Kind { AUTO, AGED, SHADES, BETWEEN, PRESET, LIST }

    /** What a spec needs from its surroundings to become blocks. */
    public interface Context {
        PaletteGenerator generator();

        Optional<List<String>> preset(String id);

        /** Main material of the build being painted, or null when unknown. */
        String buildMaterial();
    }

    public static final int DEFAULT_STEPS = 5;

    private final String text;
    private final Kind kind;
    private final List<String> args;
    private final List<List<String>> stops;

    private PaletteSpec(String text, Kind kind, List<String> args, List<List<String>> stops) {
        this.text = text;
        this.kind = kind;
        this.args = args;
        this.stops = stops;
    }

    /**
     * Parses a spec; block names are checked with {@code isBlock}.
     *
     * @throws IllegalArgumentException with a player-facing message
     */
    public static PaletteSpec parse(String raw, Function<String, Boolean> isBlock) {
        String text = raw.trim();
        String lower = text.toLowerCase(Locale.ROOT);
        if (lower.isEmpty()) {
            throw new IllegalArgumentException("Empty palette");
        }
        if (lower.equals("auto")) {
            return new PaletteSpec(text, Kind.AUTO, List.of(), List.of());
        }
        for (Kind kind : List.of(Kind.AGED, Kind.SHADES, Kind.PRESET)) {
            String prefix = kind.name().toLowerCase(Locale.ROOT) + ":";
            if (lower.startsWith(prefix)) {
                String arg = BlockColors.normalize(text.substring(prefix.length()));
                if (kind != Kind.PRESET) {
                    requireBlock(arg, isBlock);
                }
                return new PaletteSpec(text, kind, List.of(arg), List.of());
            }
        }
        int dots = lower.indexOf("..");
        if (dots > 0 && !lower.contains("[") && !lower.contains(",")) {
            String from = parseBlockOrColour(text.substring(0, dots), isBlock);
            String to = parseBlockOrColour(text.substring(dots + 2), isBlock);
            return new PaletteSpec(text, Kind.BETWEEN, List.of(from, to), List.of());
        }
        List<List<String>> stops = new ArrayList<>();
        if (lower.startsWith("[")) {
            int depth = 0;
            int start = -1;
            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                if (c == '[' && depth++ == 0) {
                    start = i + 1;
                } else if (c == ']' && --depth == 0) {
                    stops.add(splitStop(text.substring(start, i), isBlock));
                }
            }
            if (depth != 0) {
                throw new IllegalArgumentException("Unbalanced [ ] in palette " + text);
            }
        } else {
            for (String part : text.split(",")) {
                stops.add(List.of(parseWeighted(part, isBlock)));
            }
        }
        if (stops.isEmpty()) {
            throw new IllegalArgumentException("Empty palette");
        }
        return new PaletteSpec(text, Kind.LIST, List.of(), stops);
    }

    private static List<String> splitStop(String inner, Function<String, Boolean> isBlock) {
        List<String> blocks = new ArrayList<>();
        for (String part : inner.split(",")) {
            blocks.add(parseWeighted(part, isBlock));
        }
        return blocks;
    }

    /** Returns "weight%block" or "block" with the block validated. */
    private static String parseWeighted(String part, Function<String, Boolean> isBlock) {
        String p = part.trim();
        int pct = p.indexOf('%');
        if (pct > 0) {
            String weight = p.substring(0, pct);
            try {
                Double.parseDouble(weight);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Bad weight '" + weight + "' in " + part);
            }
            return weight + "%" + parseBlockOrColour(p.substring(pct + 1), isBlock);
        }
        return parseBlockOrColour(p, isBlock);
    }

    private static String parseBlockOrColour(String raw, Function<String, Boolean> isBlock) {
        String p = raw.trim();
        if (p.startsWith("#")) {
            OkLab.fromHex(p);
            return p.toUpperCase(Locale.ROOT);
        }
        String id = BlockColors.normalize(p);
        requireBlock(id, isBlock);
        return id;
    }

    private static void requireBlock(String id, Function<String, Boolean> isBlock) {
        if (!isBlock.apply(id)) {
            throw new IllegalArgumentException("Unknown block '" + id + "'");
        }
    }

    public Kind kind() {
        return kind;
    }

    public String text() {
        return text;
    }

    /** Whether resolving needs the build (so it is redone per build). */
    public boolean dependsOnBuild() {
        return kind == Kind.AUTO;
    }

    /**
     * @param steps desired number of stops; 0 keeps explicit lists as typed
     *              and uses {@link #DEFAULT_STEPS} for generated palettes
     */
    public Palette resolve(Context ctx, int steps) {
        PaletteGenerator gen = ctx.generator();
        int count = steps > 0 ? steps : DEFAULT_STEPS;
        return switch (kind) {
            case AUTO -> {
                String material = ctx.buildMaterial();
                if (material == null || gen.colors().get(material).isEmpty()) {
                    throw new IllegalArgumentException("Could not find the build's material for an auto palette");
                }
                yield Palette.ofBlocks(gen.aged(material, count));
            }
            case AGED -> Palette.ofBlocks(gen.aged(args.get(0), count));
            case SHADES -> Palette.ofBlocks(gen.shades(args.get(0), count));
            case BETWEEN -> Palette.ofBlocks(gen.between(colourToBlock(gen, args.get(0)),
                    colourToBlock(gen, args.get(1)), count));
            case PRESET -> {
                List<String> blocks = ctx.preset(args.get(0))
                        .orElseThrow(() -> new IllegalArgumentException("Unknown preset '" + args.get(0) + "'"));
                yield Palette.ofBlocks(steps > blocks.size() ? gen.fill(blocks, steps) : blocks);
            }
            case LIST -> resolveList(gen, steps);
        };
    }

    private Palette resolveList(PaletteGenerator gen, int steps) {
        boolean simple = stops.stream().allMatch(s -> s.size() == 1 && !s.get(0).contains("%"));
        if (simple) {
            List<String> blocks = stops.stream().map(s -> colourToBlock(gen, s.get(0))).toList();
            return Palette.ofBlocks(steps > blocks.size() ? gen.fill(blocks, steps) : blocks);
        }
        List<Palette.Stop> out = new ArrayList<>();
        for (List<String> stop : stops) {
            List<Palette.Weighted> weighted = new ArrayList<>();
            for (String entry : stop) {
                int pct = entry.indexOf('%');
                double weight = pct > 0 ? Double.parseDouble(entry.substring(0, pct)) : 1;
                String block = colourToBlock(gen, pct > 0 ? entry.substring(pct + 1) : entry);
                weighted.add(new Palette.Weighted(block, weight));
            }
            out.add(new Palette.Stop(weighted));
        }
        return new Palette(out);
    }

    private static String colourToBlock(PaletteGenerator gen, String entry) {
        if (!entry.startsWith("#")) {
            return entry;
        }
        return gen.nearest(OkLab.fromHex(entry), new HashSet<>())
                .orElseThrow(() -> new IllegalArgumentException("No block matches colour " + entry));
    }

    @Override
    public String toString() {
        return text;
    }
}
