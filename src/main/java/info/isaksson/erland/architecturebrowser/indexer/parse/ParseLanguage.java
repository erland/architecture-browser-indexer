package info.isaksson.erland.architecturebrowser.indexer.parse;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

public enum ParseLanguage {
    JAVA("java"),
    JAVASCRIPT("javascript"),
    TYPESCRIPT("typescript"),
    JSON("json"),
    YAML("yaml"),
    SQL("sql"),
    PROPERTIES("properties"),
    XML("xml");

    private final String inventoryKey;

    ParseLanguage(String inventoryKey) {
        this.inventoryKey = inventoryKey;
    }

    public String inventoryKey() {
        return inventoryKey;
    }

    public static Optional<ParseLanguage> fromInventoryLanguage(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        String normalized = value.toLowerCase(Locale.ROOT);
        return Arrays.stream(values()).filter(candidate -> candidate.inventoryKey.equals(normalized)).findFirst();
    }
}
