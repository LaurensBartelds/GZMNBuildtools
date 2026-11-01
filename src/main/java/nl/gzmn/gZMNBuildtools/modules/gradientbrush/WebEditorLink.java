package nl.gzmn.gZMNBuildtools.modules.gradientbrush;

import org.bukkit.entity.Player;

/** Supplied by the web editor module so /gb web can hand out personal links. */
public interface WebEditorLink {

    /** A new personal link for the player. */
    String create(Player player);

    long lifetimeMinutes();
}
