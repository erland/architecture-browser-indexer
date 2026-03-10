package info.isaksson.erland.architecturebrowser.indexer.ir.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RunMetadata(
    Instant startedAt,
    Instant completedAt,
    RunOutcome outcome,
    List<String> detectedTechnologies,
    Map<String, Object> metadata
) {
}
