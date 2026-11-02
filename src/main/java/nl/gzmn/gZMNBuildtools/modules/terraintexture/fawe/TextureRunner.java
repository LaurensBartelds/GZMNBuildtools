package nl.gzmn.gZMNBuildtools.modules.terraintexture.fawe;

import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;
import nl.gzmn.gZMNBuildtools.modules.terraintexture.engine.TexturePreset;
import nl.gzmn.gZMNBuildtools.modules.terraintexture.engine.TexturePresets;
import nl.gzmn.gZMNBuildtools.modules.terraintexture.engine.Texturer;
import nl.gzmn.gZMNBuildtools.shared.build.Bounds;
import nl.gzmn.gZMNBuildtools.shared.worldedit.BlockKnowledge;
import nl.gzmn.gZMNBuildtools.shared.worldedit.BuildEditor;
import nl.gzmn.gZMNBuildtools.shared.worldedit.ExtentBlockSource;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Runs a texture preset over part of a FAWE extent. */
public final class TextureRunner {

    private final BlockKnowledge blocks;
    private final Set<String> canopy;
    private final Texturer.Plants plants;
    private final Map<String, Boolean> ground = new ConcurrentHashMap<>();
    private volatile TexturePresets presets = TexturePresets.EMPTY;

    /**
     * @param canopy blocks that never count as ground (leaves, logs), so tree
     *               cover does not hide the soil underneath it
     */
    public TextureRunner(BlockKnowledge blocks, Set<String> canopy, Texturer.Plants plants) {
        this.blocks = blocks;
        this.canopy = canopy;
        this.plants = plants;
    }

    public TexturePresets presets() {
        return presets;
    }

    public void presets(TexturePresets presets) {
        this.presets = presets;
    }

    public static long defaultSeed(TexturePreset preset) {
        return preset.id().hashCode() * 0x9E3779B97F4A7C15L;
    }

    /** @return the number of blocks changed */
    public int run(Extent extent, TexturePreset preset, long seed, Bounds region, Texturer.Inside inside,
                   BuildEditor.StateSink sink) {
        Set<String> presetBlocks = presets.allBlocks();
        Texturer texturer = new Texturer(preset, this::isGround,
                id -> blocks.rules().isTerrain(id) || presetBlocks.contains(id) || preset.blocks().contains(id),
                plants, seed);
        return texturer.apply(new ExtentBlockSource(extent), region, inside,
                (x, y, z, block) -> sink.accept(x, y, z, state(block)));
    }

    private boolean isGround(String id) {
        return ground.computeIfAbsent(id, k -> {
            BlockType type = blocks.type(k);
            return type != null && type.getMaterial().isSolid() && !canopy.contains(k);
        });
    }

    private BlockState state(String block) {
        if (block == null) {
            return BlockTypes.AIR.getDefaultState();
        }
        BlockType type = blocks.type(block);
        return type == null ? BlockTypes.AIR.getDefaultState() : type.getDefaultState();
    }
}
