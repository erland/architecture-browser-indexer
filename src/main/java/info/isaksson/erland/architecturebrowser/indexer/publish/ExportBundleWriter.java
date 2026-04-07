package info.isaksson.erland.architecturebrowser.indexer.publish;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureIndexDocument;
import info.isaksson.erland.architecturebrowser.indexer.publish.model.ExportBundle;
import info.isaksson.erland.architecturebrowser.indexer.publish.model.ExportContract;
import info.isaksson.erland.architecturebrowser.indexer.publish.model.ExportManifest;
import info.isaksson.erland.architecturebrowser.indexer.publish.model.ExportSnapshotSourceFile;
import info.isaksson.erland.architecturebrowser.indexer.publish.model.ExportSnapshotSourceFiles;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ExportBundleWriter {
    private final ObjectMapper objectMapper;
    private final SnapshotSourceFileReferenceCollector snapshotSourceFileReferenceCollector;
    private final SnapshotSourceFileReader snapshotSourceFileReader;

    public ExportBundleWriter() {
        this.objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        this.objectMapper.findAndRegisterModules();
        this.snapshotSourceFileReferenceCollector = new SnapshotSourceFileReferenceCollector();
        this.snapshotSourceFileReader = new SnapshotSourceFileReader();
    }

    public ExportBundle createBundle(ArchitectureIndexDocument document, String producerVersion, String payloadFileName) {
        return createBundle(document, producerVersion, payloadFileName, null);
    }

    public ExportBundle createBundle(ArchitectureIndexDocument document, String producerVersion, String payloadFileName, Path indexedSourceRoot) {
        try {
            byte[] payloadBytes = objectMapper.writeValueAsBytes(document);
            String sha256 = sha256Hex(payloadBytes);
            ExportContract contract = ExportContractSupport.defaultContract(producerVersion, document);
            Map<String, Object> compatibility = new LinkedHashMap<>(ExportCompatibility.evaluate(contract, document));
            compatibility.put("targetRecommendation", document.runMetadata().outcome().name().equals("FAILED") ? "do-not-import" : "safe-to-import");
            contract = new ExportContract(
                contract.contractVersion(),
                contract.schemaVersion(),
                contract.producer(),
                contract.payloadType(),
                contract.acceptedTargets(),
                compatibility
            );
            List<String> referencedRelativePaths = snapshotSourceFileReferenceCollector.collectReferencedRelativePaths(document);
            SnapshotSourceFileReadResult sourceFileReadResult = snapshotSourceFileReader.readReferencedTextFiles(indexedSourceRoot, referencedRelativePaths);
            ExportSnapshotSourceFiles snapshotSourceFiles = new ExportSnapshotSourceFiles(
                "snapshot-source-files/v1",
                mapReadFiles(sourceFileReadResult),
                Map.of(
                    "referencedRelativePaths", referencedRelativePaths,
                    "referencedFileCount", referencedRelativePaths.size(),
                    "readReferencedFileCount", sourceFileReadResult.files().size(),
                    "skippedReferencedFileCount", sourceFileReadResult.skippedFiles().size(),
                    "skippedReferencedFiles", sourceFileReadResult.skippedRelativePaths(),
                    "skippedReferencedFileDetails", sourceFileReadResult.skippedFiles(),
                    "maxReferencedFiles", SnapshotSourceFileReader.DEFAULT_MAX_REFERENCED_FILES,
                    "maxReferencedFileSizeBytes", SnapshotSourceFileReader.DEFAULT_MAX_FILE_SIZE_BYTES
                )
            );
            String snapshotSourceFilesArtifactFileName = toSnapshotSourceFilesArtifactFileName(payloadFileName);
            byte[] snapshotSourceFilesBytes = objectMapper.writeValueAsBytes(snapshotSourceFiles);
            String snapshotSourceFilesSha256 = sha256Hex(snapshotSourceFilesBytes);
            ExportManifest manifest = new ExportManifest(
                sha256.substring(0, 12),
                Instant.now(),
                payloadFileName,
                "application/json",
                payloadBytes.length,
                sha256,
                contract,
                Map.of(
                    "entityCount", document.entities().size(),
                    "relationshipCount", document.relationships().size(),
                    "scopeCount", document.scopes().size(),
                    "outcome", document.runMetadata().outcome().name(),
                    "snapshotSourceReferencedFileCount", referencedRelativePaths.size(),
                    "snapshotSourceFilesArtifact", Map.of(
                        "fileName", snapshotSourceFilesArtifactFileName,
                        "contentType", "application/json",
                        "sizeBytes", snapshotSourceFilesBytes.length,
                        "sha256", snapshotSourceFilesSha256,
                        "contractVersion", snapshotSourceFiles.contractVersion(),
                        "fileCount", snapshotSourceFiles.files().size()
                    )
                )
            );
            return new ExportBundle(
                document,
                manifest,
                snapshotSourceFiles
            );
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to create export bundle", ex);
        }
    }


    private static List<ExportSnapshotSourceFile> mapReadFiles(SnapshotSourceFileReadResult readResult) {
        return readResult.files().stream()
            .map(file -> new ExportSnapshotSourceFile(
                file.relativePath(),
                file.language(),
                file.sizeBytes(),
                file.totalLineCount(),
                file.contentType(),
                file.textContent()
            ))
            .toList();
    }

    static String toSnapshotSourceFilesArtifactFileName(String payloadFileName) {
        if (payloadFileName == null || payloadFileName.isBlank()) {
            return "snapshot-source-files.json";
        }
        return payloadFileName.endsWith(".json")
            ? payloadFileName.substring(0, payloadFileName.length() - ".json".length()) + ".source-files.json"
            : payloadFileName + ".source-files.json";
    }

    public void writeBundle(Path outputJson, ExportBundle bundle) throws IOException {
        Path parent = outputJson.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        objectMapper.writeValue(outputJson.toFile(), bundle.document());

        String fileName = outputJson.getFileName().toString();
        String manifestName = fileName.endsWith(".json")
            ? fileName.substring(0, fileName.length() - ".json".length()) + ".manifest.json"
            : fileName + ".manifest.json";
        Path manifestPath = outputJson.resolveSibling(manifestName);
        objectMapper.writeValue(manifestPath.toFile(), bundle.manifest());

        @SuppressWarnings("unchecked")
        Map<String, Object> snapshotSourceFilesArtifact = (Map<String, Object>) bundle.manifest().metadata().get("snapshotSourceFilesArtifact");
        String artifactFileName = snapshotSourceFilesArtifact != null && snapshotSourceFilesArtifact.get("fileName") instanceof String fileNameValue
            ? fileNameValue
            : toSnapshotSourceFilesArtifactFileName(fileName);
        Path snapshotSourceFilesPath = outputJson.resolveSibling(artifactFileName);
        objectMapper.writeValue(snapshotSourceFilesPath.toFile(), bundle.snapshotSourceFiles());
    }

    public String writeBundlePreviewJson(ExportBundle bundle) throws IOException {
        return objectMapper.writeValueAsString(bundle.manifest());
    }

    private static String sha256Hex(byte[] payload) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(payload));
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to compute SHA-256", ex);
        }
    }
}
