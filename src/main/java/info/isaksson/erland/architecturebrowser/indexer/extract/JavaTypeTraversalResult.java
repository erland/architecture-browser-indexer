package info.isaksson.erland.architecturebrowser.indexer.extract;

record JavaTypeTraversalResult(
    JavaOwnerContext ownerContext,
    boolean handled
) {
    static JavaTypeTraversalResult notHandled(JavaOwnerContext ownerContext) {
        return new JavaTypeTraversalResult(ownerContext, false);
    }

    static JavaTypeTraversalResult handled(JavaOwnerContext ownerContext) {
        return new JavaTypeTraversalResult(ownerContext, true);
    }

    String owningTypeEntityId() {
        return ownerContext == null ? null : ownerContext.owningTypeEntityId();
    }

    String owningQualifiedName() {
        return ownerContext == null ? null : ownerContext.owningQualifiedName();
    }

    String owningTypeSnippet() {
        return ownerContext == null ? null : ownerContext.owningTypeSnippet();
    }
}
