package nl.gzmn.gZMNBuildtools.modules.patterns;

import com.sk89q.worldedit.WorldEdit;
import nl.gzmn.gZMNBuildtools.core.help.HelpEntry;
import nl.gzmn.gZMNBuildtools.core.module.GzmnModule;
import nl.gzmn.gZMNBuildtools.core.module.ModuleContext;
import nl.gzmn.gZMNBuildtools.modules.patterns.parser.GzmnMaskParsers;
import nl.gzmn.gZMNBuildtools.modules.patterns.parser.GzmnPatternParsers;
import org.bukkit.Material;

import static nl.gzmn.gZMNBuildtools.api.Messages.tr;

/** Terrain masks (#slope, #depth, ...) and patterns (#byslope, #stripes, ...) for WorldEdit. */
public final class PatternsModule implements GzmnModule {

    @Override
    public String id() {
        return "masks-and-patterns";
    }

    @Override
    public String description() {
        return "Terrain masks (#slope, #depth, ...) and patterns (#byslope, #stripes, ...) for WorldEdit.";
    }

    @Override
    public void enable(ModuleContext context) {
        context.help(HelpEntry.of(HelpEntry.Category.BUILDS, Material.SPYGLASS, "//replace #slope",
                        tr("Masks and patterns"),
                        tr("Extra FAWE masks and patterns: #slope, #depth, #byslope, #stripes, #gradient and more, "
                                + "for any WorldEdit command."))
                .example("//replace #slope[30][90] stone"));
        WorldEdit worldEdit = WorldEdit.getInstance();
        GzmnPatternParsers.create(worldEdit).forEach(worldEdit.getPatternFactory()::register);
        GzmnMaskParsers.create(worldEdit).forEach(worldEdit.getMaskFactory()::register);
    }
}
