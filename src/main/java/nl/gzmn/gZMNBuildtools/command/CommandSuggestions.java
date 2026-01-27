package nl.gzmn.gZMNBuildtools.command;

import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.sk89q.worldedit.world.block.BlockType;

import java.util.concurrent.CompletableFuture;

public class CommandSuggestions {

    public static CompletableFuture<Suggestions> suggestBlocks(SuggestionsBuilder builder) {
        String remaining = builder.getRemaining();
        String prefix;
        String searchTerm;
        String suffix;

        // Detect if we're inside a bracket
        boolean insideBracket = remaining.lastIndexOf('[') > remaining.lastIndexOf(']');

        if (insideBracket) {
            int lastComma = remaining.lastIndexOf(',');
            int lastOpen = remaining.lastIndexOf('[');
            int splitPos = Math.max(lastComma, lastOpen);
            prefix = remaining.substring(0, splitPos + 1);
            searchTerm = remaining.substring(splitPos + 1).toLowerCase();
            suffix = "]";
        } else if (remaining.endsWith("]")) {
            prefix = remaining + "[";
            searchTerm = "";
            suffix = "]";
        } else if (remaining.isEmpty()) {
            prefix = "[";
            searchTerm = "";
            suffix = "]";
        } else {
            prefix = "[";
            searchTerm = remaining.toLowerCase();
            suffix = "]";
        }

        final String fPrefix = prefix;
        final String fSearch = searchTerm;
        final String fSuffix = suffix;
        BlockType.REGISTRY.values().stream()
                .map(bt -> bt.toString().replace("minecraft:", ""))
                .filter(id -> id.startsWith(fSearch))
                .sorted()
                .limit(50)
                .forEach(id -> builder.suggest(fPrefix + id + fSuffix));

        return builder.buildFuture();
    }
}
