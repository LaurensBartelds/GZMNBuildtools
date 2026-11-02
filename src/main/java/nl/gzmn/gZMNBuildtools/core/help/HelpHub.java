package nl.gzmn.gZMNBuildtools.core.help;

import com.mojang.brigadier.arguments.StringArgumentType;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import nl.gzmn.gZMNBuildtools.api.Messages;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permissible;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.function.Supplier;

import static nl.gzmn.gZMNBuildtools.api.Messages.tr;

/**
 * {@code /gz}: one place that shows everything GZMNBuildtools can do, built
 * from the help entries the running modules contributed. A menu for players,
 * a clickable chat list with {@code /gz list}, and {@code /gz <word>} to search.
 */
@SuppressWarnings("UnstableApiUsage")
public final class HelpHub {

    private final Messages messages;
    private final Supplier<List<HelpEntry>> entries;

    public HelpHub(Messages messages, Supplier<List<HelpEntry>> entries) {
        this.messages = messages;
        this.entries = entries;
    }

    /** The entries someone may use, grouped by category in the hub's order. */
    public List<HelpEntry> visible(Permissible who) {
        List<HelpEntry> out = new ArrayList<>();
        for (HelpEntry.Category category : HelpEntry.Category.values()) {
            for (HelpEntry e : entries.get()) {
                if (e.category() == category && e.visibleTo(who)) {
                    out.add(e);
                }
            }
        }
        return out;
    }

    /**
     * Entries whose command, module, category, title or description contains
     * every word of the query, in either English or the reader's language.
     */
    public static List<HelpEntry> search(List<HelpEntry> entries, String query, Function<String, String> translate) {
        String[] words = query.toLowerCase(Locale.ROOT).trim().split("\\s+");
        List<HelpEntry> out = new ArrayList<>();
        for (HelpEntry e : entries) {
            String haystack = String.join(" ", e.command(), e.module(), e.title(), e.description(),
                    e.category().label(), translate.apply(e.title()), translate.apply(e.description()),
                    translate.apply(e.category().label()),
                    e.example() == null ? "" : e.example()).toLowerCase(Locale.ROOT);
            boolean all = true;
            for (String word : words) {
                if (!haystack.contains(word)) {
                    all = false;
                    break;
                }
            }
            if (all) {
                out.add(e);
            }
        }
        return out;
    }

    private String t(CommandSender sender, String template, Object... args) {
        return messages.text(sender instanceof Player p ? p : null, template, args);
    }

    /** The clickable chat list. */
    public void list(CommandSender sender, List<HelpEntry> shown) {
        if (shown.isEmpty()) {
            sender.sendMessage(messages.prefixed(Component.text(t(sender, tr("Nothing found. /gz list shows everything.")),
                    NamedTextColor.RED)));
            return;
        }
        sender.sendMessage(messages.prefixed(Component.text(t(sender,
                tr("GZMNBuildtools (click a command to use it, hover for details):")), NamedTextColor.YELLOW)));
        HelpEntry.Category current = null;
        for (HelpEntry e : shown) {
            if (e.category() != current) {
                current = e.category();
                sender.sendMessage(Component.text(t(sender, current.label()), NamedTextColor.GOLD));
            }
            Component hover = Component.text(t(sender, e.description()), NamedTextColor.GRAY);
            if (e.example() != null) {
                hover = hover.append(Component.newline())
                        .append(Component.text(t(sender, tr("Example: %s"), e.example()), NamedTextColor.DARK_AQUA));
            }
            sender.sendMessage(Component.text("  " + e.command(), NamedTextColor.AQUA)
                    .append(Component.text(" - " + t(sender, e.title()), NamedTextColor.GRAY))
                    .hoverEvent(HoverEvent.showText(hover))
                    .clickEvent(e.runs() ? ClickEvent.runCommand(e.command()) : ClickEvent.suggestCommand(e.suggestion())));
        }
    }

    /** What clicking an entry in the menu does. */
    public void use(Player player, HelpEntry e) {
        player.closeInventory();
        if (e.runs()) {
            player.performCommand(e.command().substring(1));
            return;
        }
        messages.info(player, "%s: %s", messages.text(player, e.title()), messages.text(player, e.description()));
        player.sendMessage(Component.text("  " + messages.text(player, "[Click to type %s]", e.suggestion().trim()),
                NamedTextColor.AQUA).clickEvent(ClickEvent.suggestCommand(e.suggestion())));
    }

    public void open(Player player) {
        new HelpMenu(player, this, messages, visible(player)).open();
    }

    public void register(Commands commands) {
        commands.register(Commands.literal("gz")
                .executes(ctx -> {
                    CommandSender sender = ctx.getSource().getSender();
                    if (sender instanceof Player player) {
                        open(player);
                    } else {
                        list(sender, visible(sender));
                    }
                    return 1;
                })
                .then(Commands.literal("list").executes(ctx -> {
                    list(ctx.getSource().getSender(), visible(ctx.getSource().getSender()));
                    return 1;
                }))
                .then(Commands.argument("search", StringArgumentType.greedyString())
                        .suggests((ctx, builder) -> {
                            String typed = builder.getRemainingLowerCase();
                            for (HelpEntry e : visible(ctx.getSource().getSender())) {
                                String word = e.command().substring(1).split(" ")[0];
                                if (word.startsWith(typed)) {
                                    builder.suggest(word);
                                }
                            }
                            return builder.buildFuture();
                        })
                        .executes(ctx -> {
                            search(ctx.getSource(), StringArgumentType.getString(ctx, "search"));
                            return 1;
                        }))
                .build(), "Everything GZMNBuildtools can do: a menu, /gz list or /gz <word>.", List.of("gzhelp", "gzmn"));
    }

    private void search(CommandSourceStack source, String query) {
        CommandSender sender = source.getSender();
        list(sender, search(visible(sender), query, text -> t(sender, text)));
    }
}
