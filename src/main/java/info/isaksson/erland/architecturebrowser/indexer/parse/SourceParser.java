package info.isaksson.erland.architecturebrowser.indexer.parse;

public interface SourceParser {
    ParseLanguage language();

    SourceParseResult parse(SourceParseRequest request);
}
