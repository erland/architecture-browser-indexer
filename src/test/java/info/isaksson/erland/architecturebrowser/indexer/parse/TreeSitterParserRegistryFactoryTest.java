package info.isaksson.erland.architecturebrowser.indexer.parse;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TreeSitterParserRegistryFactoryTest {
    @Test
    void activeDefaultLanguagesAreLimitedToImplementedBackends() {
        Set<ParseLanguage> active = TreeSitterParserRegistryFactory.defaultLanguageDescriptors().stream()
            .map(TreeSitterLanguageDescriptor::language)
            .collect(java.util.stream.Collectors.toSet());

        assertEquals(Set.of(ParseLanguage.JAVA, ParseLanguage.TYPESCRIPT), active);
    }

    @Test
    void knownFutureLanguagesRemainDocumentedButInactive() {
        Set<ParseLanguage> future = TreeSitterParserRegistryFactory.knownFutureLanguageDescriptors().stream()
            .map(TreeSitterLanguageDescriptor::language)
            .collect(java.util.stream.Collectors.toSet());

        assertTrue(future.contains(ParseLanguage.JAVASCRIPT));
        assertTrue(future.contains(ParseLanguage.JSON));
        assertTrue(future.contains(ParseLanguage.YAML));
    }
}
