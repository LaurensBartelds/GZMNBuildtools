package nl.gzmn.gZMNBuildtools.command;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import io.papermc.paper.command.brigadier.MessageComponentSerializer;
import io.papermc.paper.command.brigadier.argument.CustomArgumentType;
import net.kyori.adventure.text.Component;

/**
 * Custom Brigadier argument type that reads bracket-delimited block expressions.
 * Supports: [stone], [stone][dirt], [cobblestone,stone][andesite]
 * Stops reading when all brackets are balanced and whitespace is encountered.
 */
@SuppressWarnings("UnstableApiUsage")
public class BracketBlocksArgumentType implements CustomArgumentType<String, String> {

    private static final SimpleCommandExceptionType EXPECTED_BRACKET = new SimpleCommandExceptionType(
            MessageComponentSerializer.message().serialize(
                    Component.text("Expected '[' to start block expression")));

    public static BracketBlocksArgumentType bracketBlocks() {
        return new BracketBlocksArgumentType();
    }

    public static String getString(com.mojang.brigadier.context.CommandContext<?> context, String name) {
        return context.getArgument(name, String.class);
    }

    @Override
    public String parse(StringReader reader) throws CommandSyntaxException {
        if (!reader.canRead() || reader.peek() != '[') {
            throw EXPECTED_BRACKET.createWithContext(reader);
        }

        int start = reader.getCursor();
        int depth = 0;

        while (reader.canRead()) {
            char c = reader.peek();
            if (c == '[') {
                depth++;
            } else if (c == ']') {
                depth--;
                if (depth == 0) {
                    reader.skip();
                    // Check if another bracket follows immediately
                    if (reader.canRead() && reader.peek() == '[') {
                        continue;
                    }
                    break;
                }
            }
            reader.skip();
        }

        return reader.getString().substring(start, reader.getCursor());
    }

    @Override
    public ArgumentType<String> getNativeType() {
        return StringArgumentType.string();
    }
}
