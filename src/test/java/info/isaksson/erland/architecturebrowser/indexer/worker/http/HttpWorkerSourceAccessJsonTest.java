package info.isaksson.erland.architecturebrowser.indexer.worker.http;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class HttpWorkerSourceAccessJsonTest {

    @Test
    void writesSourceAccessInRunResponseJson() throws Exception {
        HttpWorkerRunResponse response = new HttpWorkerRunResponse(
            "job-001",
            "SUCCESS",
            Instant.parse("2026-04-05T10:00:00Z"),
            Instant.parse("2026-04-05T10:00:05Z"),
            "/tmp/out/architecture-index.json",
            "/tmp/out/snapshot.json",
            null,
            Map.of(),
            Map.of("message", "ok"),
            new HttpWorkerSourceAccess(
                null,
                "src_01JQ7D2S3R7M7A6K9N8Y4K1B7C",
                "RETAINED_GIT_CHECKOUT",
                "GIT",
                "repo-1",
                "abc123",
                "ttl-7d",
                Instant.parse("2026-04-05T10:00:00Z"),
                Instant.parse("2026-04-12T10:00:00Z")
            )
        );

        Map<?, ?> serialized = HttpWorkerJson.readMap(HttpWorkerJson.writeBytes(response));
        Map<?, ?> sourceAccess = assertInstanceOf(Map.class, serialized.get("sourceAccess"));
        assertEquals("SOURCE_HANDLE", sourceAccess.get("lookupKeyKind"));
        assertEquals("src_01JQ7D2S3R7M7A6K9N8Y4K1B7C", sourceAccess.get("sourceHandle"));
        assertEquals("GIT", sourceAccess.get("acquisitionType"));
    }
}
