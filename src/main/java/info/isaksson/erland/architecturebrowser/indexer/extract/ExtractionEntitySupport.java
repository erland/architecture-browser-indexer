package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityOrigin;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.SourceReference;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class ExtractionEntitySupport {
    private ExtractionEntitySupport() {
    }

    static ExtractedEntityFact inferredEntity(String idPrefix, EntityKind kind, String language, String name, String relativePath, int line, Map<String, Object> metadata) {
        LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
        if (metadata != null) {
            merged.putAll(metadata);
        }
        merged.putIfAbsent("language", language);
        merged.putIfAbsent("displayName", name.contains(".") ? name.substring(name.lastIndexOf('.') + 1) : name);
        String id = "entity:" + idPrefix + ":" + Integer.toHexString(name.hashCode());
        SourceReference ref = ExtractionRelationshipSupport.sourceRef(relativePath, line, name, Map.of(
            "language", language,
            "displayName", merged.get("displayName"),
            "external", merged.getOrDefault("external", false)
        ));
        return new ExtractedEntityFact(
            id,
            kind,
            EntityOrigin.INFERRED,
            name,
            String.valueOf(merged.get("displayName")),
            null,
            List.of(ref),
            Map.copyOf(merged)
        );
    }

    static ExtractedEntityFact inferredTypeEntity(String language, EntityKind kind, String name, String relativePath, int line, Map<String, Object> metadata) {
        return inferredEntity(language + "-type-" + kind.name().toLowerCase(), kind, language, name, relativePath, line, metadataWithDisplayName(name, metadata));
    }

    static ExtractedEntityFact inferredModuleEntity(String language, String name, String relativePath, int line, Map<String, Object> metadata) {
        return inferredEntity(language, EntityKind.MODULE, language, name, relativePath, line, metadataWithDisplayName(name, metadata));
    }

    private static Map<String, Object> metadataWithDisplayName(String name, Map<String, Object> metadata) {
        LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
        if (metadata != null) {
            merged.putAll(metadata);
        }
        merged.putIfAbsent("displayName", name.contains(".") ? name.substring(name.lastIndexOf('.') + 1) : name);
        return Map.copyOf(merged);
    }
}
