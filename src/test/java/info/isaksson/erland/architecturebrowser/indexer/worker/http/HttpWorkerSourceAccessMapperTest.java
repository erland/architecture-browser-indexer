package info.isaksson.erland.architecturebrowser.indexer.worker.http;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class HttpWorkerSourceAccessMapperTest {
    @Test
    void mapsSourceAccessFromWorkerSummary() {
        Instant createdAt = Instant.parse("2026-04-05T18:00:00Z");
        Instant expiresAt = Instant.parse("2026-04-12T18:00:00Z");

        HttpWorkerSourceAccess sourceAccess = HttpWorkerSourceAccessMapper.fromSummary(Map.of(
            "status", "SUCCESS",
            "sourceAccess", Map.of(
                "lookupKeyKind", "SOURCE_HANDLE",
                "sourceHandle", "src_01JQ7D2S3R7M7A6K9N8Y4K1B7C",
                "retainedRootKind", "RETAINED_GIT_CHECKOUT",
                "acquisitionType", "GIT",
                "repositoryId", "repo-123",
                "sourceRevision", "abc123def",
                "retentionPolicy", "ttl-7d",
                "createdAt", createdAt.toString(),
                "expiresAt", expiresAt.toString()
            )
        ));

        assertNotNull(sourceAccess);
        assertEquals("SOURCE_HANDLE", sourceAccess.lookupKeyKind());
        assertEquals("src_01JQ7D2S3R7M7A6K9N8Y4K1B7C", sourceAccess.sourceHandle());
        assertEquals("RETAINED_GIT_CHECKOUT", sourceAccess.retainedRootKind());
        assertEquals("GIT", sourceAccess.acquisitionType());
        assertEquals("repo-123", sourceAccess.repositoryId());
        assertEquals("abc123def", sourceAccess.sourceRevision());
        assertEquals("ttl-7d", sourceAccess.retentionPolicy());
        assertEquals(createdAt, sourceAccess.createdAt());
        assertEquals(expiresAt, sourceAccess.expiresAt());
    }

    @Test
    void returnsNullWhenSourceAccessIsMissing() {
        assertNull(HttpWorkerSourceAccessMapper.fromSummary(Map.of("status", "SUCCESS")));
        assertNull(HttpWorkerSourceAccessMapper.fromSummary(null));
    }
}
