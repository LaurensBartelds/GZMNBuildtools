package nl.gzmn.gZMNBuildtools.modules.terrainbrush.fawe;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.command.tool.brush.Brush;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import nl.gzmn.gZMNBuildtools.modules.terrainbrush.engine.HeightOperation;
import nl.gzmn.gZMNBuildtools.modules.terrainbrush.engine.TerrainBrushType;
import nl.gzmn.gZMNBuildtools.shared.params.BrushArguments;
import nl.gzmn.gZMNBuildtools.shared.shapes.TreeStyle;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.LongFunction;

/** Creates the FAWE {@link Brush} for parsed terrain brush arguments. */
public final class TerrainBrushFactory {

    private TerrainBrushFactory() {
    }

    /** @param patterns the parsed fill patterns (the first is the tool's fill; more are passed to the brush) */
    public static Brush create(BrushArguments<TerrainBrushType> args, List<Pattern> patterns) {
        return switch (args.type()) {
            case RAISE -> new HeightmapBrush(HeightOperation.raise(args.get("height"), args.get("hardness")));
            case LOWER -> new HeightmapBrush(HeightOperation.raise(-args.get("depth"), args.get("hardness")));
            case FLATTEN -> new HeightmapBrush(HeightOperation.flatten(args.get("strength"), args.get("hardness")));
            case SMOOTH -> new HeightmapBrush(HeightOperation.smooth(args.getInt("kernel"),
                    args.getInt("iterations"), args.get("strength")));
            case TERRACE -> new HeightmapBrush(HeightOperation.terrace(args.getInt("step"),
                    args.getInt("offset"), args.get("hardness")));
            case ROUGHEN -> new RandomSeedBrush(seed -> new HeightmapBrush(
                    HeightOperation.roughen(args.get("amplitude"), args.get("scale"), seed)));
            case OVERLAY -> new OverlayBrush(args.getInt("depth"));
            case BLOB -> new BlobBrush(args.get("roughness"), args.get("frequency"));
            case SPIKE -> new SpikeBrush(args.getInt("height"), args.get("sharpness"), args.get("tilt"),
                    args.get("down") >= 0.5);
            case TREE -> new TreeBrush(TreeStyle.valueOf(args.choice("style").toUpperCase(Locale.ROOT)),
                    args.getInt("height"), args.getInt("thickness"), args.getInt("leaves"), args.getInt("count"),
                    patterns.get(1));
            case ROCK -> new RockBrush(args.get("flatness"), args.get("roughness"), args.get("sink"),
                    args.getInt("count"));
            case SNOW -> new SnowBrush(args.get("strength"), args.get("fill") >= 0.5);
        };
    }

    /** Uses a fresh noise seed for every click so repeated strokes do not stack identically. */
    private record RandomSeedBrush(LongFunction<Brush> factory) implements Brush {
        @Override
        public void build(EditSession editSession, BlockVector3 position, Pattern pattern, double size)
                throws MaxChangedBlocksException {
            factory.apply(ThreadLocalRandom.current().nextLong()).build(editSession, position, pattern, size);
        }
    }
}
