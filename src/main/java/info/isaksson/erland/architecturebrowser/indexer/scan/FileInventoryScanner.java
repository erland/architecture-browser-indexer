package info.isaksson.erland.architecturebrowser.indexer.scan;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public final class FileInventoryScanner {
    private final FileClassifier classifier;

    public FileInventoryScanner() {
        this(new FileClassifier());
    }

    FileInventoryScanner(FileClassifier classifier) {
        this.classifier = classifier;
    }

    public FileInventory scan(Path rootPath) {
        return scan(rootPath, InventoryScanOptions.defaults());
    }

    public FileInventory scan(Path rootPath, InventoryScanOptions options) {
        if (rootPath == null) {
            throw new IllegalArgumentException("rootPath must not be null");
        }
        if (!Files.isDirectory(rootPath)) {
            throw new IllegalArgumentException("rootPath must be a directory: " + rootPath);
        }

        List<FileInventoryEntry> entries = new ArrayList<>();
        Set<String> detectedLanguages = new LinkedHashSet<>();
        Set<String> detectedMarkers = new LinkedHashSet<>();

        try (Stream<Path> stream = Files.walk(rootPath)) {
            stream.filter(Files::isRegularFile)
                .sorted(Comparator.comparing(path -> normalize(rootPath.relativize(path))))
                .forEach(path -> {
                    String relativePath = normalize(rootPath.relativize(path));
                    boolean ignored = isIgnored(rootPath.relativize(path), options);
                    FileClassifier.Classification classification = classifier.classify(path, options.maxMarkerReadBytes());
                    entries.add(new FileInventoryEntry(
                        relativePath,
                        safeSize(path),
                        classification.extension(),
                        classification.type(),
                        classification.language(),
                        ignored,
                        List.copyOf(classification.markers())
                    ));
                    if (!ignored && classification.language() != null) {
                        detectedLanguages.add(classification.language());
                    }
                    if (!ignored) {
                        detectedMarkers.addAll(classification.markers());
                    }
                });
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to scan file inventory for: " + rootPath, exception);
        }

        int totalFiles = entries.size();
        int ignoredFiles = (int) entries.stream().filter(FileInventoryEntry::ignored).count();
        int indexedFiles = totalFiles - ignoredFiles;
        return new FileInventory(List.copyOf(entries), totalFiles, indexedFiles, ignoredFiles, Set.copyOf(detectedLanguages), Set.copyOf(detectedMarkers));
    }

    private static boolean isIgnored(Path relativePath, InventoryScanOptions options) {
        for (Path segment : relativePath) {
            String name = segment.toString();
            if (options.ignoredDirectoryNames().contains(name) || options.ignoredFileNames().contains(name)) {
                return true;
            }
        }
        return false;
    }

    private static long safeSize(Path file) {
        try {
            return Files.size(file);
        } catch (IOException exception) {
            return -1L;
        }
    }

    private static String normalize(Path path) {
        return path.toString().replace('\\', '/');
    }
}
