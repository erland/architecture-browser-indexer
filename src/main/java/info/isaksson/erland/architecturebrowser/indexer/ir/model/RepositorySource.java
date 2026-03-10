package info.isaksson.erland.architecturebrowser.indexer.ir.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RepositorySource(
    String repositoryId,
    String acquisitionType,
    String path,
    String remoteUrl,
    String branch,
    String revision,
    Instant acquiredAt,
    Map<String, Object> metadata
) {
    public static RepositorySource localPath(String repositoryId, String path, Instant acquiredAt) {
        return new RepositorySource(repositoryId, "local-path", path, null, null, null, acquiredAt, Map.of());
    }
}
