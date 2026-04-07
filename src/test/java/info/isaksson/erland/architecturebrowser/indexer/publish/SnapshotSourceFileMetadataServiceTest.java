package info.isaksson.erland.architecturebrowser.indexer.publish;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SnapshotSourceFileMetadataServiceTest {

    private final SnapshotSourceFileMetadataService service = new SnapshotSourceFileMetadataService();

    @Test
    void detectsLanguageFromRelativePath() {
        assertEquals("java", service.detectLanguage("src/main/java/demo/App.java"));
        assertEquals("xml", service.detectLanguage("pom.xml"));
        assertEquals("json", service.detectLanguage("package-lock.json"));
        assertNull(service.detectLanguage("README"));
    }

    @Test
    void countsLogicalLinesForViewerMetadata() {
        assertEquals(0, service.countLines(""));
        assertEquals(1, service.countLines("one"));
        assertEquals(1, service.countLines("one\n"));
        assertEquals(2, service.countLines("one\ntwo\n"));
        assertEquals(2, service.countLines("one\ntwo"));
    }

    @Test
    void mapsLanguageToContentType() {
        assertEquals("text/x-java-source", service.detectContentType("App.java", "java"));
        assertEquals("application/json", service.detectContentType("a.json", "json"));
        assertEquals("application/xml", service.detectContentType("pom.xml", "xml"));
        assertEquals("text/plain", service.detectContentType("readme", null));
    }
}
