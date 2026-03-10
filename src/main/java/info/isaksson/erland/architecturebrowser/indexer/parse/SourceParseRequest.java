package info.isaksson.erland.architecturebrowser.indexer.parse;

import java.nio.file.Path;

public record SourceParseRequest(
    Path absolutePath,
    String relativePath,
    ParseLanguage language,
    String sourceText
) {
}
