package nl.gzmn.gZMNBuildtools.modules.typereplace;

import com.mojang.brigadier.arguments.StringArgumentType;
import io.papermc.paper.command.brigadier.Commands;
import nl.gzmn.gZMNBuildtools.core.help.HelpEntry;
import nl.gzmn.gZMNBuildtools.core.module.GzmnModule;
import nl.gzmn.gZMNBuildtools.core.module.ModuleContext;
import nl.gzmn.gZMNBuildtools.modules.typereplace.command.TypeReplaceCommand;
import nl.gzmn.gZMNBuildtools.modules.typereplace.menu.TypeReplaceUIManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;

import static nl.gzmn.gZMNBuildtools.api.Messages.tr;

/** Swap whole block families while keeping shapes (/typereplace). */
@SuppressWarnings("UnstableApiUsage")
public final class TypeReplaceModule implements GzmnModule {

    private TypeReplaceCommand typeReplaceCommand;
    private TypeReplaceUIManager typeReplaceUIManager;

    @Override
    public String id() {
        return "type-replace";
    }

    @Override
    public String description() {
        return "Swap whole block families while keeping shapes (/typereplace).";
    }

    @Override
    public void enable(ModuleContext context) throws Exception {
        context.help(HelpEntry.of(HelpEntry.Category.BUILDS, Material.STONE_BRICK_STAIRS, "/typereplace",
                        tr("Type replace"),
                        tr("Swap one material for another in your selection, keeping every stair, slab and wall "
                                + "shape."))
                .permission("gzmnbuildtools.typereplace").runs());
        typeReplaceCommand = new TypeReplaceCommand();
        typeReplaceCommand.setMessages(context.core().messages());
        typeReplaceCommand.setBlockFamilies(context.core().blockFamilies());
        typeReplaceUIManager = new TypeReplaceUIManager(context.plugin(), typeReplaceCommand);
        typeReplaceUIManager.setMessages(context.core().messages());
        typeReplaceUIManager.setBlockFamilies(context.core().blockFamilies());
        context.listen(typeReplaceUIManager);
        context.commands(this::registerTypeReplace);
    }

    private void registerTypeReplace(Commands commands) {
        commands.register(
                Commands.literal("typereplace")
                        .requires(source -> source.getSender() instanceof Player &&
                                source.getSender().hasPermission(
                                        "gzmnbuildtools.typereplace"))
                        .executes(ctx -> {
                            typeReplaceUIManager.openTypeReplaceUI(
                                    (Player) ctx.getSource().getSender());
                            return 1;
                        })
                        .then(Commands.argument("from_material", StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    typeReplaceCommand.getSuggestions()
                                            .stream()
                                            .filter(s -> s.toLowerCase()
                                                    .startsWith(builder
                                                            .getRemainingLowerCase()))
                                            .forEach(builder::suggest);
                                    return builder.buildFuture();
                                })
                                .then(Commands.argument("to_material",
                                                StringArgumentType.word())
                                        .suggests((ctx, builder) -> {
                                            typeReplaceCommand
                                                    .getSuggestions()
                                                    .stream()
                                                    .filter(s -> s.toLowerCase()
                                                            .startsWith(builder
                                                                    .getRemainingLowerCase()))
                                                    .forEach(builder::suggest);
                                            return builder.buildFuture();
                                        })
                                        .executes(ctx -> {
                                            String from = StringArgumentType
                                                    .getString(ctx, "from_material");
                                            String to = StringArgumentType
                                                    .getString(ctx, "to_material");
                                            typeReplaceCommand.execute(
                                                    (Player) ctx.getSource()
                                                            .getSender(),
                                                    from, to);
                                            return 1;
                                        })))
                        .build(),
                "Replace entire block type families (stairs, slabs, walls, etc.) preserving variants.",
                List.of("tr", "typerep"));
    }
}
