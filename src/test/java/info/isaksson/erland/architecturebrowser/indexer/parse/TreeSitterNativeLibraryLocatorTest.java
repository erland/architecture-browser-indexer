package info.isaksson.erland.architecturebrowser.indexer.parse;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TreeSitterNativeLibraryLocatorTest {
    @Test
    void mapsMacArm64ToBundledPlatformDirectoryName() {
        String previousOs = System.getProperty("os.name");
        String previousArch = System.getProperty("os.arch");
        try {
            System.setProperty("os.name", "Mac OS X");
            System.setProperty("os.arch", "aarch64");

            TreeSitterNativeLibraryLocator locator = new TreeSitterNativeLibraryLocator(
                new TreeSitterConfiguration(true, null));

            assertEquals("macos-aarch64", locator.platformDirectoryName());
        } finally {
            restoreProperty("os.name", previousOs);
            restoreProperty("os.arch", previousArch);
        }
    }

    @Test
    void returnsConfiguredDirectoryPathWhenLibraryExists(@TempDir Path tempDir) throws IOException {
        Path libraryFile = tempDir.resolve(System.mapLibraryName("tree-sitter-java"));
        Files.createFile(libraryFile);

        TreeSitterNativeLibraryLocator locator = new TreeSitterNativeLibraryLocator(
            new TreeSitterConfiguration(true, tempDir));

        TreeSitterNativeLibraryLocator.ResolvedLibrary resolved = locator.resolve(
            new TreeSitterLanguageDescriptor(
                ParseLanguage.JAVA,
                "java",
                "tree-sitter-java",
                "tree_sitter_java",
                java.util.List.of(".java"),
                true));

        assertEquals(libraryFile.toAbsolutePath().normalize(), resolved.lookupPath());
        assertEquals("filesystem", resolved.resolutionMode());
    }

    private static void restoreProperty(String name, String value) {
        if (value == null) {
            System.clearProperty(name);
        } else {
            System.setProperty(name, value);
        }
    }
}
