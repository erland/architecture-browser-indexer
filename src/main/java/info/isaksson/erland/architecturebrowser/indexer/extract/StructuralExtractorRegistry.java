package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.parse.ParseLanguage;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class StructuralExtractorRegistry {
    private final Map<ParseLanguage, StructuralExtractor> extractorsByLanguage;

    public StructuralExtractorRegistry(List<StructuralExtractor> extractors) {
        Map<ParseLanguage, StructuralExtractor> map = new LinkedHashMap<>();
        if (extractors != null) {
            for (StructuralExtractor extractor : extractors) {
                map.put(extractor.language(), extractor);
            }
        }
        this.extractorsByLanguage = Map.copyOf(map);
    }

    public Optional<StructuralExtractor> find(ParseLanguage language) {
        return Optional.ofNullable(extractorsByLanguage.get(language));
    }

    public static StructuralExtractorRegistry defaultRegistry() {
        return new StructuralExtractorRegistry(List.of(
            new JavaStructuralExtractor(),
            new TypeScriptStructuralExtractor()
        ));
    }
}
