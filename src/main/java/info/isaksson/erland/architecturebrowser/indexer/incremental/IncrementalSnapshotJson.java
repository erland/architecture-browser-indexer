package info.isaksson.erland.architecturebrowser.indexer.incremental;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import info.isaksson.erland.architecturebrowser.indexer.incremental.model.IncrementalSnapshot;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class IncrementalSnapshotJson {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
        .enable(SerializationFeature.INDENT_OUTPUT)
        .findAndRegisterModules();

    private IncrementalSnapshotJson() {
    }

    public static void write(Path path, IncrementalSnapshot snapshot) throws IOException {
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        OBJECT_MAPPER.writeValue(path.toFile(), snapshot);
    }

    public static IncrementalSnapshot read(Path path) throws IOException {
        return OBJECT_MAPPER.readValue(path.toFile(), IncrementalSnapshot.class);
    }
}
