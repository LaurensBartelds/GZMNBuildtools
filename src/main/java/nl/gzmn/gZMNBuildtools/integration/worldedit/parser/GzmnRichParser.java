package nl.gzmn.gZMNBuildtools.integration.worldedit.parser;

import com.fastasyncworldedit.core.extension.factory.parser.RichParser;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.ParserContext;

import java.util.stream.Stream;

/**
 * A {@link RichParser} defined by lambdas, which also accepts the bare alias
 * without brackets (e.g. {@code #sky} or {@code #concave}) and then parses it
 * with zero arguments.
 */
public final class GzmnRichParser<E> extends RichParser<E> {

    @FunctionalInterface
    public interface Parse<E> {
        E parse(String[] args, ParserContext context) throws InputParseException;
    }

    @FunctionalInterface
    public interface Suggest {
        Stream<String> suggest(String input, int index, ParserContext context);
    }

    private final Parse<E> parse;
    private final Suggest suggest;

    public GzmnRichParser(WorldEdit worldEdit, String alias, Suggest suggest, Parse<E> parse) {
        super(worldEdit, alias);
        this.parse = parse;
        this.suggest = suggest;
    }

    @Override
    public E parseFromInput(String input, ParserContext context) throws InputParseException {
        if (input.equals(getPrefix())) {
            return parse.parse(new String[0], context);
        }
        return super.parseFromInput(input, context);
    }

    @Override
    protected Stream<String> getSuggestions(String argumentInput, int index, ParserContext context) {
        return suggest.suggest(argumentInput, index, context);
    }

    @Override
    protected E parseFromInput(String[] arguments, ParserContext context) throws InputParseException {
        return parse.parse(arguments, context);
    }
}
