package info.isaksson.erland.architecturebrowser.indexer.incremental;

import info.isaksson.erland.architecturebrowser.indexer.incremental.model.FileFingerprint;
import info.isaksson.erland.architecturebrowser.indexer.incremental.model.IncrementalDiff;
import info.isaksson.erland.architecturebrowser.indexer.incremental.model.IncrementalSnapshot;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class IncrementalDiffService {

    public IncrementalDiff diff(IncrementalSnapshot previous, IncrementalSnapshot current) {
        if (previous == null) {
            return new IncrementalDiff(current.filesByPath().keySet().stream().sorted().toList(), List.of(), List.of(), List.of());
        }

        Set<String> allPaths = new LinkedHashSet<>();
        allPaths.addAll(previous.filesByPath().keySet());
        allPaths.addAll(current.filesByPath().keySet());

        List<String> added = new ArrayList<>();
        List<String> changed = new ArrayList<>();
        List<String> removed = new ArrayList<>();
        List<String> unchanged = new ArrayList<>();

        for (String path : allPaths) {
            FileFingerprint before = previous.filesByPath().get(path);
            FileFingerprint after = current.filesByPath().get(path);
            if (before == null && after != null) {
                added.add(path);
            } else if (before != null && after == null) {
                removed.add(path);
            } else if (before != null && after != null) {
                if (before.contentHash().equals(after.contentHash())
                    && before.sizeBytes() == after.sizeBytes()
                    && safeEquals(before.detectedLanguage(), after.detectedLanguage())
                    && safeEquals(before.fileType(), after.fileType())) {
                    unchanged.add(path);
                } else {
                    changed.add(path);
                }
            }
        }

        return new IncrementalDiff(
            added.stream().sorted().toList(),
            changed.stream().sorted().toList(),
            removed.stream().sorted().toList(),
            unchanged.stream().sorted().toList()
        );
    }

    private static boolean safeEquals(String left, String right) {
        return left == null ? right == null : left.equals(right);
    }
}
