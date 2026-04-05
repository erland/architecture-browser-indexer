package info.isaksson.erland.architecturebrowser.indexer.worker.source;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SourceLanguageDetectionServiceTest {

    private final SourceLanguageDetectionService service = new SourceLanguageDetectionService();

    @Test
    void detectsViewerFriendlyLanguagesFromPath() {
        assertEquals("java", service.detectLanguage("src/main/java/App.java"));
        assertEquals("javascript", service.detectLanguage("web/app.js"));
        assertEquals("jsx", service.detectLanguage("web/App.jsx"));
        assertEquals("typescript", service.detectLanguage("web/app.ts"));
        assertEquals("tsx", service.detectLanguage("web/App.tsx"));
        assertEquals("json", service.detectLanguage("package.json"));
        assertEquals("yaml", service.detectLanguage("application.yml"));
        assertEquals("properties", service.detectLanguage("application.properties"));
        assertEquals("xml", service.detectLanguage("pom.xml"));
        assertEquals("sql", service.detectLanguage("schema.sql"));
        assertEquals("markdown", service.detectLanguage("README.md"));
        assertEquals("plaintext", service.detectLanguage("notes.txt"));
    }

    @Test
    void returnsNullForUnknownOrMissingLanguage() {
        assertNull(service.detectLanguage(null));
        assertNull(service.detectLanguage("   "));
        assertNull(service.detectLanguage("Dockerfile"));
        assertNull(service.detectLanguage("src/.env"));
    }
}
