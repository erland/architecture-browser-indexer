package info.isaksson.erland.architecturebrowser.indexer.extract;

record JavaTypeTraversalResult(
    String owningTypeEntityId,
    String owningQualifiedName,
    String owningTypeSnippet,
    boolean handled
) {
    static JavaTypeTraversalResult notHandled(String owningTypeEntityId, String owningQualifiedName, String owningTypeSnippet) {
        return new JavaTypeTraversalResult(owningTypeEntityId, owningQualifiedName, owningTypeSnippet, false);
    }

    static JavaTypeTraversalResult handled(String owningTypeEntityId, String owningQualifiedName, String owningTypeSnippet) {
        return new JavaTypeTraversalResult(owningTypeEntityId, owningQualifiedName, owningTypeSnippet, true);
    }
}
