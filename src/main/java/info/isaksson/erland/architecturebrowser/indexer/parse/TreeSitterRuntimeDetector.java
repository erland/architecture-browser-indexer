package info.isaksson.erland.architecturebrowser.indexer.parse;

import java.util.LinkedHashMap;
import java.util.Map;

public final class TreeSitterRuntimeDetector {
    private TreeSitterRuntimeDetector() {
    }

    public static TreeSitterRuntimeStatus detect(TreeSitterConfiguration configuration) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("enabled", configuration.enabled());
        configuration.libraryDirectory().ifPresent(path -> metadata.put("libraryDirectory", path.toString()));

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
}
