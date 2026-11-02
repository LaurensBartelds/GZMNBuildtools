package nl.gzmn.gZMNBuildtools.modules.generators.engine;

import nl.gzmn.gZMNBuildtools.shared.params.BrushArguments;
import nl.gzmn.gZMNBuildtools.shared.shapes.BlockSink;
import nl.gzmn.gZMNBuildtools.shared.shapes.PartSink;
import nl.gzmn.gZMNBuildtools.shared.shapes.Rocks;
import nl.gzmn.gZMNBuildtools.shared.shapes.Shapes;
import nl.gzmn.gZMNBuildtools.shared.shapes.TreeStyle;
import nl.gzmn.gZMNBuildtools.shared.shapes.Trees;

/** Runs a {@link GeneratorType} with parsed arguments into a {@link BlockSink}. */
public final class Generators {

    private Generators() {
    }

    /**
     * @param origin player position for point-based shapes
     * @param a      selection point 1 (selection-based shapes)
     * @param b      selection point 2 (selection-based shapes)
     */
    public static void generate(BrushArguments<GeneratorType> args, int[] origin, int[] a, int[] b, long seed,
                                PartSink parts) {
        BlockSink sink = (x, y, z) -> parts.accept(x, y, z, 0, (char) 0);
        switch (args.type()) {
            case ARCH -> Shapes.curve(a[0], a[1], a[2], b[0], b[1], b[2], args.get("height"), args.get("thickness"),
                    sink);
            case CABLE -> Shapes.curve(a[0], a[1], a[2], b[0], b[1], b[2], -args.get("sag"), args.get("thickness"),
                    sink);
            case TORUS -> Shapes.torus(origin[0], origin[1], origin[2], args.get("major"), args.get("minor"),
                    args.get("hollow") >= 0.5, sink);
            case CONE -> Shapes.cone(origin[0], origin[1], origin[2], args.get("radius"), args.getInt("height"),
                    args.get("hollow") >= 0.5, sink);
            case DOME -> Shapes.dome(origin[0], origin[1], origin[2], args.get("radius"),
                    args.get("hollow") >= 0.5, sink);
            case SPIRAL -> Shapes.spiral(origin[0], origin[1], origin[2], args.get("radius"), args.getInt("height"),
                    args.get("turns"), args.get("thickness"), sink);
            case TREE -> Trees.generate(TreeStyle.valueOf(args.choice("style").toUpperCase(java.util.Locale.ROOT)),
                    origin[0], origin[1], origin[2], args.getInt("height"), args.getInt("thickness"),
                    args.getInt("leaves"), seed, parts);
            case ROCK -> Rocks.generate(origin[0], origin[1] - 1, origin[2], args.get("radius"), args.get("flatness"),
                    args.get("roughness"), args.get("sink"), seed, sink);
        }
    }
}
