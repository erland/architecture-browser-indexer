package info.isaksson.erland.architecturebrowser.indexer.parse;

public record TreeSitterRuntimeStatus(
    boolean available,
    String detail
) {
    public static TreeSitterRuntimeStatus available(String detail) {
        return new TreeSitterRuntimeStatus(true, detail);
    }

    public static TreeSitterRuntimeStatus unavailable(String detail) {
        return new TreeSitterRuntimeStatus(false, detail);
    }
}
