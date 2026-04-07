package info.isaksson.erland.architecturebrowser.indexer.publish.model;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExportSnapshotSourceFilesTest {

    @Test
    void suppressesDuplicateFilesByRelativePathWhilePreservingFirstSeenOrder() {
        ExportSnapshotSourceFile first = new ExportSnapshotSourceFile(
            "src/main/java/example/Foo.java",
            "java",
            12,
            1,
            "text/x-java-source",
            "class Foo {}"
        );
        ExportSnapshotSourceFile duplicatePathDifferentContent = new ExportSnapshotSourceFile(
            "src/main/java/example/Foo.java",
            "java",
            99,
            9,
            "text/x-java-source",
            "class FooChanged {}"
        );
        ExportSnapshotSourceFile second = new ExportSnapshotSourceFile(
            "src/main/java/example/Bar.java",
            "java",
            12,
            1,
            "text/x-java-source",
            "class Bar {}"
        );

        ExportSnapshotSourceFiles result = new ExportSnapshotSourceFiles(
            "snapshot-source-files/v1",
            List.of(first, duplicatePathDifferentContent, second),
            Map.of("inputFileCount", 3)
        );

        assertEquals(List.of(first, second), result.files());
        assertEquals(2, result.files().size());
    }

    @Test
    void ignoresNullEntriesWhenNormalizingFiles() {
        ExportSnapshotSourceFile file = new ExportSnapshotSourceFile(
            "src/test/resources/example.json",
            "json",
            2,
            1,
            "application/json",
            "{}"
        );

        ExportSnapshotSourceFiles result = new ExportSnapshotSourceFiles(
            null,
            java.util.Arrays.asList(null, file, null),
            null
        );

        assertEquals("snapshot-source-files/v1", result.contractVersion());
        assertEquals(List.of(file), result.files());
        assertEquals(Map.of(), result.metadata());
    }
}
