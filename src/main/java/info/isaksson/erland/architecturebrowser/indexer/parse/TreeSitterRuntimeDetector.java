package info.isaksson.erland.architecturebrowser.indexer.parse;

public final class TreeSitterRuntimeDetector {
    private TreeSitterRuntimeDetector() {
    }

    public static TreeSitterRuntimeStatus detect() {
        try {
            Class.forName("io.github.treesitter.jtreesitter.Parser");
            return TreeSitterRuntimeStatus.available(
                "Found io.github.treesitter.jtreesitter.Parser on the classpath");
        } catch (ClassNotFoundException e) {
            return TreeSitterRuntimeStatus.unavailable(
                "Tree-sitter Java runtime not found on the classpath; install a compatible runtime and grammars to enable parsing");
        }
    }
}
