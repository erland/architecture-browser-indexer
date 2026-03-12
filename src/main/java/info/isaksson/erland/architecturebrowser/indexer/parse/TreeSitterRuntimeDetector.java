package info.isaksson.erland.architecturebrowser.indexer.parse;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class TreeSitterRuntimeDetector {
    private TreeSitterRuntimeDetector() {
    }

    public static TreeSitterRuntimeStatus detect(TreeSitterConfiguration configuration) {
        Map<String, Object> metadata = new LinkedHashMap<>(configuration.runtimeMetadata());
        configuration.libraryDirectory().ifPresent(path -> {
            metadata.put("libraryDirectory", path.toString());
            metadata.put("libraryDirectoryContents", summarizeDirectory(path));
            metadata.put("expectedRuntimeLibrary", path.resolve(libraryFileName("tree-sitter")).toString());
        });

        if (!configuration.enabled()) {
            return TreeSitterRuntimeStatus.unavailable(
                "Tree-sitter parsing is disabled by configuration",
                metadata
            );
        }

        try {
            Class.forName("io.github.treesitter.jtreesitter.Parser");
            metadata.put("runtimeClass", "io.github.treesitter.jtreesitter.Parser");
            return TreeSitterRuntimeStatus.available(
                "Found official java-tree-sitter runtime on the classpath",
                metadata
            );
        } catch (ClassNotFoundException e) {
            metadata.put("runtimeClass", "io.github.treesitter.jtreesitter.Parser");
            return TreeSitterRuntimeStatus.unavailable(
                "Official java-tree-sitter runtime not found on the classpath; add io.github.tree-sitter:jtreesitter and install compatible language shared libraries",
                metadata
            );
        }
    }

    private static List<String> summarizeDirectory(Path path) {
        if (!Files.isDirectory(path)) {
            return List.of();
        }
        try (var stream = Files.list(path)) {
            return stream
                .map(entry -> entry.getFileName().toString())
                .sorted()
                .limit(20)
                .collect(Collectors.toList());
        } catch (Exception ex) {
            return List.of("<error: " + ex.getMessage() + ">");
        }
    }

    private static String libraryFileName(String baseName) {
        String osName = System.getProperty("os.name", "").toLowerCase();
        if (osName.contains("win")) {
            return baseName + ".dll";
        }
        if (osName.contains("mac") || osName.contains("darwin")) {
            return "lib" + baseName + ".dylib";
        }
        return "lib" + baseName + ".so";
    }
}
