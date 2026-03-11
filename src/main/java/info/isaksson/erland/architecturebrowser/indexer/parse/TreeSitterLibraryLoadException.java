package info.isaksson.erland.architecturebrowser.indexer.parse;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

final class TreeSitterLibraryLoadException extends RuntimeException {
    private final Map<String, Object> metadata;

    TreeSitterLibraryLoadException(String message, Throwable cause, Map<String, Object> metadata) {
        super(message, cause);
        this.metadata = Map.copyOf(new LinkedHashMap<>(metadata));
    }

    Map<String, Object> metadata() {
        return metadata;
    }
}
