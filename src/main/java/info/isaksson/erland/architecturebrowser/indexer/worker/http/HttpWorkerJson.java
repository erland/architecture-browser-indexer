package info.isaksson.erland.architecturebrowser.indexer.worker.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import info.isaksson.erland.architecturebrowser.indexer.worker.model.WorkerJobRequest;

import java.io.IOException;
import java.io.InputStream;

public final class HttpWorkerJson {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
        .enable(SerializationFeature.INDENT_OUTPUT)
        .findAndRegisterModules();

    private HttpWorkerJson() {
    }

    public static WorkerJobRequest readWorkerRequest(InputStream inputStream) throws IOException {
        return OBJECT_MAPPER.readValue(inputStream, WorkerJobRequest.class);
    }

    public static byte[] writeBytes(Object value) throws IOException {
        return OBJECT_MAPPER.writeValueAsBytes(value);
    }

    @SuppressWarnings("unchecked")
    public static java.util.Map<String, Object> readMap(byte[] jsonBytes) throws IOException {
        return OBJECT_MAPPER.readValue(jsonBytes, java.util.Map.class);
    }
}
