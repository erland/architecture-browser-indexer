package info.isaksson.erland.architecturebrowser.indexer.worker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import info.isaksson.erland.architecturebrowser.indexer.worker.model.WorkerJobRequest;
import info.isaksson.erland.architecturebrowser.indexer.worker.model.WorkerJobResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class WorkerJobJson {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
        .enable(SerializationFeature.INDENT_OUTPUT)
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .findAndRegisterModules();

    private WorkerJobJson() {
    }

    public static WorkerJobRequest readRequest(Path path) throws IOException {
        return OBJECT_MAPPER.readValue(path.toFile(), WorkerJobRequest.class);
    }

    public static void writeResult(Path path, WorkerJobResult result) throws IOException {
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        OBJECT_MAPPER.writeValue(path.toFile(), result);
    }
}
