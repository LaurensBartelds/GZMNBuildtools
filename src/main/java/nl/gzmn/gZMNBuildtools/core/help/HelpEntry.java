package nl.gzmn.gZMNBuildtools.core.help;

import org.bukkit.Material;
import org.bukkit.permissions.Permissible;

import static nl.gzmn.gZMNBuildtools.api.Messages.tr;

/**
 * One feature in the {@code /gz} help hub. Title and description are English
 * templates (wrapped in {@code tr}) and are translated when shown.
 *
 * @param command    what clicking does: runs it (it opens a menu or shows a list) or puts it in chat
 * @param example    a ready-to-edit example, or null
 * @param permission needed to see the entry, or null for everyone
 * @param runs       true when clicking runs {@code command}; false suggests the example (or command)
 */
public record HelpEntry(String module, Category category, Material icon, String command, String title,
                        String description, String example, String permission, boolean runs) {

    /** How the hub groups features. */
    public enum Category {
        BRUSHES(tr("Brushes"), Material.BRUSH),
        TERRAIN(tr("Terrain"), Material.GRASS_BLOCK),
        SHAPES(tr("Shapes and generators"), Material.END_ROD),
        BUILDS(tr("Builds and selections"), Material.STONE_BRICKS),
        OTHER(tr("Other"), Material.BOOK);

        private final String label;
        private final Material icon;

        Category(String label, Material icon) {
            this.label = label;
            this.icon = icon;
        }

        public String label() {
            return label;
        }

        public Material icon() {
            return icon;
        }
    }

    public static Builder of(Category category, Material icon, String command, String title, String description) {
        return new Builder(category, icon, command, title, description);
    }

    public boolean visibleTo(Permissible who) {
        return permission == null || who.hasPermission(permission);
    }

    /** What to put in the chat box when clicking does not run anything. */
    public String suggestion() {
        return example != null ? example : command + " ";
    }

    /** Builds an entry; {@link #build(String)} is called by the module context with the module's id. */
    public static final class Builder {
        private final Category category;
        private final Material icon;
        private final String command;
        private final String title;
        private final String description;
        private String example;
        private String permission;
        private boolean runs;

        private Builder(Category category, Material icon, String command, String title, String description) {
            this.category = category;
            this.icon = icon;
            this.command = command;
            this.title = title;
            this.description = description;
        }

        public Builder example(String example) {
            this.example = example;
            return this;
        }

        public Builder permission(String permission) {
            this.permission = permission;
            return this;
        }

        /** Clicking runs the command (for commands that open a menu or list on their own). */
        public Builder runs() {
            this.runs = true;
            return this;
        }

        public HelpEntry build(String module) {
            return new HelpEntry(module, category, icon, command, title, description, example, permission, runs);
        }
    }
}
