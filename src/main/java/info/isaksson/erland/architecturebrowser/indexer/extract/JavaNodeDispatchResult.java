package info.isaksson.erland.architecturebrowser.indexer.extract;

record JavaNodeDispatchResult(
    JavaSyntaxTreeTraversal.JavaTraversalOwnership ownership,
    JavaTypeTraversalResult typeTraversalResult,
    JavaMemberExtractionResult memberExtractionResult,
    boolean handled
) {
    static JavaNodeDispatchResult notHandled(JavaSyntaxTreeTraversal.JavaTraversalOwnership ownership) {
        return new JavaNodeDispatchResult(
            ownership,
            JavaTypeTraversalResult.notHandled(JavaOwnerContext.fromTraversalOwnership(ownership)),
            JavaMemberExtractionResult.notHandled(),
            false
        );
    }

    static JavaNodeDispatchResult handledType(JavaTypeTraversalResult typeTraversalResult) {
        JavaSyntaxTreeTraversal.JavaTraversalOwnership ownership = typeTraversalResult.ownerContext() == null
            ? null
            : typeTraversalResult.ownerContext().toTraversalOwnership();
        return new JavaNodeDispatchResult(
            ownership,
            typeTraversalResult,
            JavaMemberExtractionResult.notHandled(),
            true
        );
    }

    static JavaNodeDispatchResult handledMember(
        JavaSyntaxTreeTraversal.JavaTraversalOwnership ownership,
        JavaMemberExtractionResult memberExtractionResult
    ) {
        return new JavaNodeDispatchResult(
            ownership,
            JavaTypeTraversalResult.notHandled(JavaOwnerContext.fromTraversalOwnership(ownership)),
            memberExtractionResult,
            memberExtractionResult != null && memberExtractionResult.handled()
        );
    }
}
