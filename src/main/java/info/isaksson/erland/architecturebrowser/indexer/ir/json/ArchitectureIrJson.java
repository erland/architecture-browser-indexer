package info.isaksson.erland.architecturebrowser.indexer.ir.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureIndexDocument;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ArchitectureIrJson {
    private static final ObjectMapper MAPPER = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .enable(SerializationFeature.INDENT_OUTPUT)
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    private ArchitectureIrJson() {
    }

    public static String toJson(ArchitectureIndexDocument document) {
        try {
            return MAPPER.writeValueAsString(document);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize architecture IR", e);
        }
    }

    public static String toPrettyJson(Object value) {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize JSON", e);
        }
    }

    public static ArchitectureIndexDocument fromJson(String json) {
        try {
            return MAPPER.readValue(json, ArchitectureIndexDocument.class);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to deserialize architecture IR", e);
        }
    }

    public static void write(ArchitectureIndexDocument document, Path path) {
        try {
            Files.writeString(path, toJson(document));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write architecture IR to " + path, e);
        }
    }

    public static ArchitectureIndexDocument read(Path path) {
        try {
            return fromJson(Files.readString(path));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read architecture IR from " + path, e);
        }
    }
}
