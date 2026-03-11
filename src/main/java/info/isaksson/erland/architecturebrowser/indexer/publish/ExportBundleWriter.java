package info.isaksson.erland.architecturebrowser.indexer.publish;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureIndexDocument;
import info.isaksson.erland.architecturebrowser.indexer.publish.model.ExportBundle;
import info.isaksson.erland.architecturebrowser.indexer.publish.model.ExportContract;
import info.isaksson.erland.architecturebrowser.indexer.publish.model.ExportManifest;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ExportBundleWriter {
    private final ObjectMapper objectMapper;

    public ExportBundleWriter() {
        this.objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        this.objectMapper.findAndRegisterModules();
    }

    public ExportBundle createBundle(ArchitectureIndexDocument document, String producerVersion, String payloadFileName) {
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
                    "outcome", document.runMetadata().outcome().name()
                )
            );
            return new ExportBundle(document, manifest);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to create export bundle", ex);
        }
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
