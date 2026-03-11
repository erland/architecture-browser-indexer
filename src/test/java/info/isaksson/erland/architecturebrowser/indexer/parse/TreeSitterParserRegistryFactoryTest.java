package info.isaksson.erland.architecturebrowser.indexer.parse;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TreeSitterParserRegistryFactoryTest {

    @Test
    void activeDefaultLanguagesAreLimitedToImplementedBackends() {
        Set<ParseLanguage> activeLanguages = TreeSitterParserRegistryFactory.defaultLanguageDescriptors().stream()
            .map(TreeSitterLanguageDescriptor::language)
            .collect(Collectors.toSet());

        assertEquals(Set.of(
            ParseLanguage.JAVA,
            ParseLanguage.TYPESCRIPT,
            ParseLanguage.JSON,
            ParseLanguage.YAML,
            ParseLanguage.SQL,
            ParseLanguage.PROPERTIES,
            ParseLanguage.XML
        ), activeLanguages);
    }

    @Test
    void knownFutureLanguagesRemainDocumentedButInactive() {
        Set<ParseLanguage> futureLanguages = TreeSitterParserRegistryFactory.knownFutureLanguageDescriptors().stream()
            .map(TreeSitterLanguageDescriptor::language)
            .collect(Collectors.toSet());

        assertTrue(futureLanguages.contains(ParseLanguage.JAVASCRIPT));
        assertTrue(!futureLanguages.contains(ParseLanguage.JAVA));
        assertTrue(!futureLanguages.contains(ParseLanguage.TYPESCRIPT));
        assertTrue(!futureLanguages.contains(ParseLanguage.JSON));
        assertTrue(!futureLanguages.contains(ParseLanguage.YAML));
        assertTrue(!futureLanguages.contains(ParseLanguage.SQL));
        assertTrue(!futureLanguages.contains(ParseLanguage.PROPERTIES));
        assertTrue(!futureLanguages.contains(ParseLanguage.XML));
    }
}
