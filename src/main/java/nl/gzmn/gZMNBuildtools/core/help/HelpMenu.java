package nl.gzmn.gZMNBuildtools.core.help;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import nl.gzmn.gZMNBuildtools.api.Messages;
import nl.gzmn.gZMNBuildtools.shared.menu.Items;
import nl.gzmn.gZMNBuildtools.shared.menu.Menu;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

import static nl.gzmn.gZMNBuildtools.api.Messages.tr;

/** The {@code /gz} menu: one row per category, one icon per feature. */
public final class HelpMenu extends Menu {

    private static final int ROWS = 6;

    private final HelpHub hub;
    private final Messages messages;
    private final List<HelpEntry> entries;
    private int page;

    HelpMenu(Player viewer, HelpHub hub, Messages messages, List<HelpEntry> entries) {
        super(viewer, ROWS, Component.text("GZMNBuildtools", NamedTextColor.DARK_GREEN));
        this.hub = hub;
        this.messages = messages;
        this.entries = entries;
    }

    private String t(String template, Object... args) {
        return messages.text(viewer, template, args);
    }

    /** Rows of (category, entries), splitting a category over several rows when it has more than 8. */
    private List<Object[]> rows() {
        List<Object[]> rows = new ArrayList<>();
        for (HelpEntry.Category category : HelpEntry.Category.values()) {
            List<HelpEntry> in = entries.stream().filter(e -> e.category() == category).toList();
            for (int i = 0; i < in.size(); i += 8) {
                rows.add(new Object[]{category, in.subList(i, Math.min(in.size(), i + 8))});
            }
        }
        return rows;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void render() {
        List<Object[]> rows = rows();
        int perPage = ROWS - 1;
        int pages = Math.max(1, (rows.size() + perPage - 1) / perPage);
        page = Math.min(page, pages - 1);
        for (int r = 0; r < perPage && page * perPage + r < rows.size(); r++) {
            Object[] row = rows.get(page * perPage + r);
            HelpEntry.Category category = (HelpEntry.Category) row[0];
            item(r * 9, Items.icon(category.icon(), t(category.label()), NamedTextColor.GOLD));
            List<HelpEntry> in = (List<HelpEntry>) row[1];
            for (int i = 0; i < in.size(); i++) {
                HelpEntry e = in.get(i);
                List<String> lore = new ArrayList<>(wrap(t(e.description())));
                if (e.example() != null) {
                    lore.add(t(tr("Example: %s"), e.example()));
                }
                lore.add(e.runs() ? t(tr("Click to use")) : t(tr("Click to get the command in chat")));
                button(r * 9 + 1 + i, Items.icon(e.icon(), t(e.title()) + "  " + e.command(), NamedTextColor.AQUA,
                        lore.toArray(String[]::new)), click -> hub.use(viewer, e));
            }
        }
        int bottom = (ROWS - 1) * 9;
        button(bottom + 4, Items.icon(Material.WRITABLE_BOOK, t(tr("Show as a list in chat")), NamedTextColor.YELLOW,
                t(tr("Or search: /gz <word>"))), click -> {
            viewer.closeInventory();
            hub.list(viewer, entries);
        });
        if (page > 0) {
            button(bottom, Items.icon(Material.ARROW, t(tr("Previous page")), NamedTextColor.WHITE), click -> {
                page--;
                refresh();
            });
        }
        if (page < pages - 1) {
            button(bottom + 8, Items.icon(Material.ARROW, t(tr("Next page")), NamedTextColor.WHITE), click -> {
                page++;
                refresh();
            });
        }
    }

    /** Splits a description over lines of about 40 characters for item lore. */
    static List<String> wrap(String text) {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            if (line.length() > 0 && line.length() + word.length() + 1 > 40) {
                lines.add(line.toString());
                line.setLength(0);
            }
            if (line.length() > 0) {
                line.append(' ');
            }
            line.append(word);
        }
        if (line.length() > 0) {
            lines.add(line.toString());
        }
        return lines;
    }
}
