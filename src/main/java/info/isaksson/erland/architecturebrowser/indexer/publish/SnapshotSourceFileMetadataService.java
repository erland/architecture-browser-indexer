package info.isaksson.erland.architecturebrowser.indexer.publish;

import info.isaksson.erland.architecturebrowser.indexer.worker.source.SourceLanguageDetectionService;

public final class SnapshotSourceFileMetadataService {
    private final SourceLanguageDetectionService languageDetectionService;

    public SnapshotSourceFileMetadataService() {
        this(new SourceLanguageDetectionService());
    }

    SnapshotSourceFileMetadataService(SourceLanguageDetectionService languageDetectionService) {
        this.languageDetectionService = languageDetectionService;
    }

    public String detectLanguage(String relativePath) {
        return languageDetectionService.detectLanguage(relativePath);
    }

    public int countLines(String textContent) {
        if (textContent == null || textContent.isEmpty()) {
            return 0;
        }
        int count = 1;
        for (int i = 0; i < textContent.length(); i++) {
            if (textContent.charAt(i) == '\n') {
                count++;
            }
        }
        if (textContent.endsWith("\n")) {
            count--;
        }
        return count;
    }

    public String detectContentType(String relativePath, String language) {
        if (language == null || language.isBlank()) {
            return "text/plain";
        }
        return switch (language) {
            case "java" -> "text/x-java-source";
            case "javascript", "jsx", "typescript", "tsx" -> "text/plain";
            case "json" -> "application/json";
            case "yaml" -> "text/yaml";
            case "xml" -> "application/xml";
            case "sql" -> "text/x-sql";
            case "properties" -> "text/x-java-properties";
            case "markdown" -> "text/markdown";
            case "plaintext" -> "text/plain";
            default -> "text/plain";
        };
    }
}
