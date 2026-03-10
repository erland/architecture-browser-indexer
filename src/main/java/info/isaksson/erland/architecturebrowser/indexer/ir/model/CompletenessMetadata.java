package info.isaksson.erland.architecturebrowser.indexer.ir.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CompletenessMetadata(
    CompletenessStatus status,
    int indexedFileCount,
    int totalFileCount,
    int degradedFileCount,
    List<String> omittedPaths,
    List<String> notes
) {
}
