package info.isaksson.erland.architecturebrowser.indexer.parse;

import java.util.List;

public record TreeSitterLanguageDescriptor(
    ParseLanguage language,
    String grammarName,
    List<String> fileExtensions,
    boolean enabledByDefault
) {
    public TreeSitterLanguageDescriptor {
        fileExtensions = fileExtensions == null ? List.of() : List.copyOf(fileExtensions);
    }
}
