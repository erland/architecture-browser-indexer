package info.isaksson.erland.architecturebrowser.indexer.worker.http;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HttpWorkerSourceFileReadJsonTest {

    @Test
    void deserializesSourceFileReadRequest() throws Exception {
        HttpWorkerSourceFileReadRequest request = HttpWorkerJson.readSourceFileReadRequest(new ByteArrayInputStream(("""
            {
              \"sourceHandle\" : \"src_123\",
              \"path\" : \"src/App.java\",
              \"startLine\" : 10,
              \"endLine\" : 20
            }
            """).getBytes(StandardCharsets.UTF_8)));

        assertEquals("src_123", request.sourceHandle());
        assertEquals("src/App.java", request.path());
        assertEquals(10, request.startLine());
        assertEquals(20, request.endLine());
    }

    @Test
    void serializesSourceFileReadResponse() throws Exception {
        Map<?, ?> serialized = HttpWorkerJson.readMap(HttpWorkerJson.writeBytes(new HttpWorkerSourceFileReadResponse(
            "src_123",
            "src/App.java",
            "tsx",
            42,
            512L,
            10,
            20,
            "class App {}\n"
        )));

        assertEquals("src_123", serialized.get("sourceHandle"));
        assertEquals("src/App.java", serialized.get("path"));
        assertEquals("tsx", serialized.get("language"));
        assertEquals(42, serialized.get("totalLineCount"));
        assertEquals(512, serialized.get("fileSizeBytes"));
        assertEquals(10, serialized.get("requestedStartLine"));
        assertEquals(20, serialized.get("requestedEndLine"));
        assertEquals("class App {}\n", serialized.get("sourceText"));
    }
}
