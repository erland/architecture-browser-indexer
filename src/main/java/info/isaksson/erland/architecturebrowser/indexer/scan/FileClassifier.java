package info.isaksson.erland.architecturebrowser.indexer.scan;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

final class FileClassifier {
    Classification classify(Path file, long maxMarkerReadBytes) {
        String filename = file.getFileName() == null ? file.toString() : file.getFileName().toString();
        String loweredFilename = filename.toLowerCase(Locale.ROOT);
        String extension = extensionOf(loweredFilename);
        String type = classifyType(loweredFilename, extension);
        String language = classifyLanguage(loweredFilename, extension);
        Set<String> markers = new LinkedHashSet<>();
        readTechnologyMarkers(file, loweredFilename, maxMarkerReadBytes, markers);
        if (language != null) {
            markers.add(language);
        }
        return new Classification(extension, type, language, markers);
    }

    private static String classifyType(String filename, String extension) {
        if ("pom.xml".equals(filename) || "package.json".equals(filename) || filename.startsWith("build.gradle")
            || filename.endsWith(".properties") || filename.endsWith(".yaml") || filename.endsWith(".yml")
            || filename.endsWith(".json")) {
            return "config";
        }
        if ("sql".equals(extension)) {
            return "sql";
        }
        if (Set.of("java", "kt", "js", "jsx", "ts", "tsx").contains(extension)) {
            return "source";
        }
        if (Set.of("md", "txt", "rst").contains(extension)) {
            return "documentation";
        }
        return extension.isBlank() ? "unknown" : extension;
    }

    private static String classifyLanguage(String filename, String extension) {
        return switch (extension) {
            case "java" -> "java";
            case "js", "jsx" -> "javascript";
            case "ts", "tsx" -> "typescript";
            case "sql" -> "sql";
            case "json" -> "json";
            case "yaml", "yml" -> "yaml";
            case "properties" -> "properties";
            default -> {
                if ("pom.xml".equals(filename)) {
                    yield "xml";
                }
                yield null;
            }
        };
    }

    private static void readTechnologyMarkers(Path file, String filename, long maxMarkerReadBytes, Set<String> markers) {
        if (!shouldInspectContent(filename)) {
            return;
        }
        try {
            long size = Files.size(file);
            byte[] bytes = Files.readAllBytes(file);
            String content = new String(bytes, StandardCharsets.UTF_8);
            if (content.length() > maxMarkerReadBytes && size > maxMarkerReadBytes) {
                content = content.substring(0, (int) maxMarkerReadBytes);
            }
            String lowered = content.toLowerCase(Locale.ROOT);
            if (filename.equals("pom.xml") || filename.startsWith("build.gradle")) {
                markers.add("build");
                if (lowered.contains("spring-boot") || lowered.contains("org.springframework")) {
                    markers.add("spring");
                }
                if (lowered.contains("quarkus")) {
                    markers.add("quarkus");
                }
            }
            if (filename.equals("package.json")) {
                markers.add("node");
                if (lowered.contains("\"react\"")) {
                    markers.add("react");
                }
                if (lowered.contains("\"next\"")) {
                    markers.add("nextjs");
                }
                if (lowered.contains("\"typescript\"")) {
                    markers.add("typescript");
                }
            }
            if (filename.endsWith(".properties") || filename.endsWith(".yaml") || filename.endsWith(".yml")) {
                if (lowered.contains("spring.")) {
                    markers.add("spring");
                }
                if (lowered.contains("quarkus.")) {
                    markers.add("quarkus");
                }
            }
        } catch (IOException | RuntimeException ignored) {
            // Best-effort marker detection only.
        }
    }

    private static boolean shouldInspectContent(String filename) {
        return filename.equals("pom.xml") || filename.equals("package.json") || filename.startsWith("build.gradle")
            || filename.endsWith(".properties") || filename.endsWith(".yaml") || filename.endsWith(".yml");
    }

    private static String extensionOf(String filename) {
        int lastDot = filename.lastIndexOf('.');
        if (lastDot < 0 || lastDot == filename.length() - 1) {
            return "";
        }
        return filename.substring(lastDot + 1);
    }

    record Classification(String extension, String type, String language, Set<String> markers) {
    }
}
