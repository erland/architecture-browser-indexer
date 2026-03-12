package info.isaksson.erland.architecturebrowser.indexer.parse;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public record TreeSitterConfiguration(
    boolean enabled,
    Path configuredLibraryDirectory
) {
    private static final String ENABLED_PROPERTY = "archbrowser.treesitter.enabled";
    private static final String LIB_DIR_PROPERTY = "archbrowser.treesitter.lib.dir";
    private static final String LIB_DIR_ENV = "ARCH_BROWSER_TREE_SITTER_LIB_DIR";

    public static TreeSitterConfiguration fromEnvironment() {
        String enabledRaw = System.getProperty(ENABLED_PROPERTY,
            System.getenv().getOrDefault("ARCH_BROWSER_TREE_SITTER_ENABLED", "true"));
        boolean enabled = !"false".equalsIgnoreCase(enabledRaw);

        String libraryDirRaw = System.getProperty(LIB_DIR_PROPERTY, System.getenv(LIB_DIR_ENV));
        Path libraryDirectory = libraryDirRaw == null || libraryDirRaw.isBlank() ? null : Path.of(libraryDirRaw);
        return new TreeSitterConfiguration(enabled, libraryDirectory);
    }

    public Optional<Path> libraryDirectory() {
        return Optional.ofNullable(configuredLibraryDirectory);
    }

    public Map<String, Object> runtimeMetadata() {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("enabled", enabled);
        metadata.put("configuredLibraryDirectory", configuredLibraryDirectory == null ? null : configuredLibraryDirectory.toString());
        metadata.put("osName", System.getProperty("os.name"));
        metadata.put("osArch", System.getProperty("os.arch"));
        metadata.put("javaVersion", System.getProperty("java.version"));
        metadata.put("envLibraryDirectory", System.getenv(LIB_DIR_ENV));
        metadata.put("propertyLibraryDirectory", System.getProperty(LIB_DIR_PROPERTY));
        if (configuredLibraryDirectory != null) {
            metadata.put("libraryDirectoryExists", Files.exists(configuredLibraryDirectory));
            metadata.put("libraryDirectoryIsDirectory", Files.isDirectory(configuredLibraryDirectory));
        }
        return metadata;
    }
}
