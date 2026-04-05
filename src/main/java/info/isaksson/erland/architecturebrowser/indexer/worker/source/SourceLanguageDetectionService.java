package info.isaksson.erland.architecturebrowser.indexer.worker.source;

import java.util.Locale;

/**
 * Detects a lightweight viewer language identifier from a repository-relative path.
 */
public class SourceLanguageDetectionService {

    public String detectLanguage(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return null;
        }
        String normalized = relativePath.replace('\\', '/').trim();
        if (normalized.isBlank()) {
            return null;
        }
        String fileName = normalized.substring(normalized.lastIndexOf('/') + 1).toLowerCase(Locale.ROOT);
        String extension = extensionOf(fileName);
        return switch (extension) {
            case "java" -> "java";
            case "js" -> "javascript";
            case "jsx" -> "jsx";
            case "ts" -> "typescript";
            case "tsx" -> "tsx";
            case "json" -> "json";
            case "yaml", "yml" -> "yaml";
            case "properties" -> "properties";
            case "xml" -> "xml";
            case "sql" -> "sql";
            case "md" -> "markdown";
            case "txt" -> "plaintext";
            default -> {
                if ("pom.xml".equals(fileName)) {
                    yield "xml";
                }
                yield null;
            }
        };
    }

    private static String extensionOf(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot < 0 || lastDot == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(lastDot + 1);
    }
}
