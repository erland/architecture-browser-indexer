package info.isaksson.erland.architecturebrowser.indexer.interpret;

import info.isaksson.erland.architecturebrowser.indexer.extract.StructuralExtractor;
import info.isaksson.erland.architecturebrowser.indexer.extract.StructuralExtractorRegistry;
import info.isaksson.erland.architecturebrowser.indexer.extract.ExtractionAccumulator;
import info.isaksson.erland.architecturebrowser.indexer.parse.ParseLanguage;
import info.isaksson.erland.architecturebrowser.indexer.parse.SourceParseResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExtensionSeamTest {

    @Test
    void structuralExtractorRegistryCanBeExtendedWithoutReplacingDefaults() {
        StructuralExtractor noop = new StructuralExtractor() {
            @Override
            public ParseLanguage language() {
                return ParseLanguage.JAVASCRIPT;
            }

            @Override
            public ExtractionAccumulator extract(SourceParseResult parseResult, ExtractionAccumulator accumulator) {
                return accumulator;
            }
        };

        StructuralExtractorRegistry extended = StructuralExtractorRegistry.defaultRegistry()
            .withAdditionalExtractors(List.of(noop));

        assertEquals(true, extended.extractorFor(ParseLanguage.JAVA).isPresent());
        assertEquals(true, extended.extractorFor(ParseLanguage.JAVASCRIPT).isPresent());
    }

    @Test
    void interpretationRegistryCanBeExtendedWithoutReplacingDefaults() {
        InterpretationRule noop = new InterpretationRule() {
            @Override
            public String ruleId() {
                return "noop";
            }

            @Override
            public void apply(InterpretationContext context, InterpretationAccumulator accumulator) {
            }
        };

        InterpretationRegistry extended = InterpretationRegistry.defaultRegistry()
            .withAdditionalRules(List.of(noop));

        assertEquals(InterpretationRegistry.defaultRegistry().rules().size() + 1, extended.rules().size());
    }
}
