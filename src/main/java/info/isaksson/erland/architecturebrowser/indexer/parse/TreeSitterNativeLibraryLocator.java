package info.isaksson.erland.architecturebrowser.indexer.parse;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

final class TreeSitterNativeLibraryLocator {
    private final TreeSitterConfiguration configuration;

    TreeSitterNativeLibraryLocator(TreeSitterConfiguration configuration) {
        this.configuration = configuration;
    }

    ResolvedLibrary resolve(TreeSitterLanguageDescriptor descriptor) {
        return resolveLibraryBaseName(descriptor.sharedLibraryBaseName(), descriptor.language().inventoryKey());
    }

    Optional<ResolvedLibrary> tryResolveRuntimeLibrary() {
        try {
            return Optional.of(resolveLibraryBaseName("tree-sitter", "runtime"));
        } catch (IllegalStateException e) {
            return Optional.empty();
        }
    }

    private ResolvedLibrary resolveLibraryBaseName(String libraryBaseName, String label) {
        String mappedFileName = System.mapLibraryName(libraryBaseName);
        List<Path> candidates = new ArrayList<>();

        configuration.libraryDirectory()
            .ifPresent(dir -> candidates.add(dir.resolve(mappedFileName)));

        Path workingDirectory = Path.of("").toAbsolutePath().normalize();
        String platformDirectory = platformDirectoryName();

        candidates.add(workingDirectory.resolve("lib").resolve(platformDirectory).resolve(mappedFileName));
        candidates.add(workingDirectory.resolve("lib").resolve(mappedFileName));

        for (Path candidate : candidates) {
            if (Files.exists(candidate)) {
                return new ResolvedLibrary(candidate.toAbsolutePath().normalize(), "filesystem");
            }
        }

        if (configuration.libraryDirectory().isPresent()) {
            Path configured = configuration.libraryDirectory().orElseThrow().resolve(mappedFileName).toAbsolutePath().normalize();
            throw new IllegalStateException(
                "Library not found for " + label
                    + ". Looked in configured directory: " + configured
                    + ". Also looked in bundled directories under " + workingDirectory.resolve("lib").toAbsolutePath().normalize());
        }

        return new ResolvedLibrary(Path.of(mappedFileName), "system");
    }

    String platformDirectoryName() {
        String os = System.getProperty("os.name", "unknown").toLowerCase();
        String arch = System.getProperty("os.arch", "unknown").toLowerCase();

        String normalizedOs;
        if (os.contains("mac") || os.contains("darwin")) {
            normalizedOs = "macos";
        } else if (os.contains("win")) {
            normalizedOs = "windows";
        } else if (os.contains("nux") || os.contains("linux")) {
            normalizedOs = "linux";
        } else {
            normalizedOs = sanitize(os);
        }

        String normalizedArch;
        if ("aarch64".equals(arch) || "arm64".equals(arch)) {
            normalizedArch = "aarch64";
        } else if ("x86_64".equals(arch) || "amd64".equals(arch) || "x64".equals(arch)) {
            normalizedArch = "x86_64";
        } else {
            normalizedArch = sanitize(arch);
        }

        return normalizedOs + "-" + normalizedArch;
    }

    private static String sanitize(String value) {
        return value.replaceAll("[^a-z0-9]+", "-");
    }

    record ResolvedLibrary(Path lookupPath, String resolutionMode) {
    }
}
