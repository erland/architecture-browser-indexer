package info.isaksson.erland.architecturebrowser.indexer.publish;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureEntity;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureIndexDocument;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureRelationship;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.CompletenessMetadata;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.CompletenessStatus;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.Diagnostic;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.DiagnosticPhase;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.DiagnosticSeverity;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityOrigin;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.LogicalScope;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RelationshipKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RepositorySource;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RunMetadata;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RunOutcome;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ScopeKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.SourceReference;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SnapshotSourceFileExportBehaviorTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void writesStableSnapshotSourceFileSidecarWithUniqueFilesMetadataAndSkipReasons() throws Exception {
        Path sourceRoot = Files.createTempDirectory("snapshot-source-export-behavior");
        Files.createDirectories(sourceRoot.resolve("src/main/java/demo"));
        Files.createDirectories(sourceRoot.resolve("src/main/resources"));
        Files.writeString(
            sourceRoot.resolve("src/main/java/demo/App.java"),
            "package demo;\npublic class App {}\n",
            StandardCharsets.UTF_8
        );
        Files.writeString(
            sourceRoot.resolve("src/main/resources/application.yml"),
            "app:\n  name: demo\n",
            StandardCharsets.UTF_8
        );
        Files.writeString(
            sourceRoot.resolve("src/main/resources/app.min.js"),
            "const x=1;",
            StandardCharsets.UTF_8
        );

        ArchitectureIndexDocument document = new ArchitectureIndexDocument(
            "1.0",
            "test-indexer",
            new RunMetadata(
                Instant.parse("2026-04-06T10:15:00Z"),
                Instant.parse("2026-04-06T10:16:00Z"),
                RunOutcome.SUCCESS,
                List.of("java", "yaml", "javascript"),
                Map.of()
            ),
            RepositorySource.localPath("sample", sourceRoot.toString(), Instant.parse("2026-04-06T10:14:00Z")),
            List.of(
                new LogicalScope(
                    "scope:module:app",
                    ScopeKind.MODULE,
                    "app",
                    "App",
                    null,
                    List.of(sourceRef("src/main/java/demo/App.java")),
                    Map.of()
                )
            ),
            List.of(
                new ArchitectureEntity(
                    "entity:app",
                    EntityKind.CLASS,
                    EntityOrigin.OBSERVED,
                    "demo.App",
                    "App",
                    "scope:module:app",
                    List.of(sourceRef("src/main/java/demo/App.java"), sourceRef("src/main/resources/application.yml")),
                    Map.of()
                )
            ),
            List.of(
                new ArchitectureRelationship(
                    "rel:uses-config",
                    RelationshipKind.USES,
                    "entity:app",
                    "entity:config",
                    "uses",
                    List.of(sourceRef("src/main/resources/application.yml"), sourceRef("src/main/resources/app.min.js")),
                    Map.of()
                )
            ),
            List.of(
                new Diagnostic(
                    "diag:missing-file",
                    DiagnosticSeverity.WARNING,
                    DiagnosticPhase.PUBLICATION,
                    "D001",
                    "Missing example",
                    false,
                    "src/main/resources/missing.yml",
                    null,
                    null,
                    List.of(sourceRef("src/main/resources/missing.yml")),
                    Map.of()
                )
            ),
            new CompletenessMetadata(CompletenessStatus.COMPLETE, 1, 1, 1, List.of(), List.of()),
            Map.of()
        );

        ExportBundleWriter writer = new ExportBundleWriter();
        var bundle = writer.createBundle(document, "0.1.0-SNAPSHOT", "sample-export.json", sourceRoot);

        Path outputDir = Files.createTempDirectory("snapshot-source-export-behavior-bundle");
        Path output = outputDir.resolve("sample-export.json");
        writer.writeBundle(output, bundle);

        Path sidecar = outputDir.resolve("sample-export.source-files.json");
        assertTrue(Files.exists(sidecar));

        JsonNode root = objectMapper.readTree(Files.readString(sidecar));
        assertEquals("snapshot-source-files/v1", root.get("contractVersion").asText());
        assertEquals(2, root.get("files").size());
        assertEquals("src/main/java/demo/App.java", root.get("files").get(0).get("relativePath").asText());
        assertEquals("java", root.get("files").get(0).get("language").asText());
        assertEquals(2, root.get("files").get(0).get("totalLineCount").asInt());
        assertEquals("src/main/resources/application.yml", root.get("files").get(1).get("relativePath").asText());
        assertEquals("yaml", root.get("files").get(1).get("language").asText());

        JsonNode metadata = root.get("metadata");
        assertEquals(4, metadata.get("referencedFileCount").asInt());
        assertEquals(2, metadata.get("readReferencedFileCount").asInt());
        assertEquals(2, metadata.get("skippedReferencedFileCount").asInt());
        assertEquals("src/main/resources/app.min.js", metadata.get("skippedReferencedFiles").get(0).asText());
        assertEquals("src/main/resources/missing.yml", metadata.get("skippedReferencedFiles").get(1).asText());
        assertEquals("minified_asset", metadata.get("skippedReferencedFileDetails").get(0).get("reason").asText());
        assertEquals("missing_file", metadata.get("skippedReferencedFileDetails").get(1).get("reason").asText());

        @SuppressWarnings("unchecked")
        Map<String, Object> sourceArtifact = (Map<String, Object>) bundle.manifest().metadata().get("snapshotSourceFilesArtifact");
        assertEquals("sample-export.source-files.json", sourceArtifact.get("fileName"));
        assertEquals(2, sourceArtifact.get("fileCount"));
        assertTrue(((String) sourceArtifact.get("sha256")).length() >= 32);
    }

    private static SourceReference sourceRef(String path) {
        return new SourceReference(path, 1, 1, null, Map.of());
    }
}
