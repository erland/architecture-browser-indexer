package info.isaksson.erland.architecturebrowser.indexer.parse;

public record ParseIssue(
    String code,
    String message,
    Integer startLine,
    Integer endLine,
    boolean fatal
) {
}
