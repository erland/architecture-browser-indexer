package info.isaksson.erland.architecturebrowser.indexer.extract;

record JavaOwnerContext(
    String owningTypeEntityId,
    String owningQualifiedName,
    String owningTypeSnippet
) {
    static JavaOwnerContext root() {
        return new JavaOwnerContext(null, null, null);
    }

    static JavaOwnerContext fromTraversalOwnership(JavaSyntaxTreeTraversal.JavaTraversalOwnership ownership) {
        if (ownership == null) {
            return root();
        }
        return new JavaOwnerContext(
            ownership.owningTypeEntityId(),
            ownership.owningQualifiedName(),
            ownership.owningTypeSnippet()
        );
    }

    JavaSyntaxTreeTraversal.JavaTraversalOwnership toTraversalOwnership() {
        return new JavaSyntaxTreeTraversal.JavaTraversalOwnership(
            owningTypeEntityId,
            owningQualifiedName,
            owningTypeSnippet
        );
    }
}
