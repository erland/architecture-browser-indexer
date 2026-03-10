package info.isaksson.erland.architecturebrowser.indexer.ir.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SourceReference(
    String path,
    Integer startLine,
    Integer endLine,
    String snippet,
    Map<String, Object> metadata
) {
}
