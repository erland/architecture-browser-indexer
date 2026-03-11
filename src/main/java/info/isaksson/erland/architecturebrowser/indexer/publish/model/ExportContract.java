package info.isaksson.erland.architecturebrowser.indexer.publish.model;

import java.util.List;
import java.util.Map;

public record ExportContract(
    String contractVersion,
    String schemaVersion,
    String producer,
    String payloadType,
    List<String> acceptedTargets,
    Map<String, Object> compatibility
) {
    public ExportContract {
        acceptedTargets = acceptedTargets == null ? List.of() : List.copyOf(acceptedTargets);
        compatibility = compatibility == null ? Map.of() : Map.copyOf(compatibility);
    }
}
