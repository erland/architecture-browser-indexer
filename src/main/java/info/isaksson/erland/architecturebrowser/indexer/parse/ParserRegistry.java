package info.isaksson.erland.architecturebrowser.indexer.parse;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class ParserRegistry {
    private final Map<ParseLanguage, SourceParser> parsers;

    public ParserRegistry(Collection<? extends SourceParser> parsers) {
        Map<ParseLanguage, SourceParser> byLanguage = new LinkedHashMap<>();
        for (SourceParser parser : parsers) {
            byLanguage.put(parser.language(), parser);
        }
        this.parsers = Map.copyOf(byLanguage);
    }

    public Optional<SourceParser> find(ParseLanguage language) {
        return Optional.ofNullable(parsers.get(language));
    }

    public Map<ParseLanguage, SourceParser> parsers() {
        return parsers;
    }
}
