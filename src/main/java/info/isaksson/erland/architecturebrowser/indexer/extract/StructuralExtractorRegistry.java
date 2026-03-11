package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.parse.ParseLanguage;

import java.util.ArrayList;
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

    public List<StructuralExtractor> extractors() {
        return List.copyOf(extractorsByLanguage.values());
    }

    public Optional<StructuralExtractor> find(ParseLanguage language) {
        return Optional.ofNullable(extractorsByLanguage.get(language));
    }

    public Optional<StructuralExtractor> extractorFor(ParseLanguage language) {
        return find(language);
    }

    public StructuralExtractorRegistry withAdditionalExtractors(List<StructuralExtractor> additionalExtractors) {
        ArrayList<StructuralExtractor> merged = new ArrayList<>(extractors());
        if (additionalExtractors != null) {
            merged.addAll(additionalExtractors);
        }
        return new StructuralExtractorRegistry(List.copyOf(merged));
    }

    public static StructuralExtractorRegistry defaultRegistry() {
        return new StructuralExtractorRegistry(List.of(
            new JavaStructuralExtractor(),
            new TypeScriptStructuralExtractor(),
            new SqlStructuralExtractor(),
            new ConfigStructuralExtractor(ParseLanguage.JSON),
            new ConfigStructuralExtractor(ParseLanguage.YAML),
            new ConfigStructuralExtractor(ParseLanguage.PROPERTIES),
            new ConfigStructuralExtractor(ParseLanguage.XML)
        ));
    }
}
