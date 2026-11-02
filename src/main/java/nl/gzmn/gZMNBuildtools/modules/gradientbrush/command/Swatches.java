package nl.gzmn.gZMNBuildtools.modules.gradientbrush.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextColor;
import nl.gzmn.gZMNBuildtools.modules.gradientbrush.engine.Palette;
import nl.gzmn.gZMNBuildtools.shared.palette.BlockColors;

/** Renders a palette as coloured blocks in chat or the action bar. */
public final class Swatches {

    private Swatches() {
    }

    public static Component of(Palette palette, BlockColors colors) {
        TextComponent.Builder out = Component.text();
        for (Palette.Stop stop : palette.stops()) {
            int rgb = colors.get(stop.blocks().get(0).block()).map(BlockColors.Entry::sideRgb).orElse(0x888888);
            out.append(Component.text("█", TextColor.color(rgb))
                    .hoverEvent(HoverEvent.showText(Component.text(stop.toString()))));
        }
        return out.build();
    }
}
