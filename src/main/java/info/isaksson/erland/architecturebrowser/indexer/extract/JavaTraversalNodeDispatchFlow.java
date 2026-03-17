package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractionMode;
import info.isaksson.erland.architecturebrowser.indexer.parse.SourceParseResult;
import info.isaksson.erland.architecturebrowser.indexer.parse.SyntaxNode;

import java.util.Map;
import java.util.Set;

final class JavaTraversalNodeDispatchFlow {

    private final JavaTypeDeclarationFlow typeDeclarationFlow;
    private final JavaFieldExtractionFlow fieldExtractionFlow;
    private final JavaMethodExtractionFlow methodExtractionFlow;

    JavaTraversalNodeDispatchFlow(
        JavaTypeDeclarationFlow typeDeclarationFlow,
        JavaFieldExtractionFlow fieldExtractionFlow,
        JavaMethodExtractionFlow methodExtractionFlow
    ) {
        this.typeDeclarationFlow = typeDeclarationFlow;
        this.fieldExtractionFlow = fieldExtractionFlow;
        this.methodExtractionFlow = methodExtractionFlow;
    }

    JavaNodeDispatchResult handleNode(
        SourceParseResult parseResult,
        ExtractionAccumulator accumulator,
        String relativePath,
        String packageName,
        ExtractionMode extractionMode,
        String packageScopeId,
        String fileScopeId,
        String fileEntityId,
        SyntaxNode node,
        JavaSyntaxTreeTraversal.JavaTraversalOwnership ownership,
        Map<String, String> importsBySimpleName,
        Map<String, JavaDeclaredType> declaredTypes,
        JavaExtractionContext extractionContext
    ) {
        if (node == null) {
            return JavaNodeDispatchResult.notHandled(ownership);
        }

        JavaOwnerContext ownerContext = JavaOwnerContext.fromTraversalOwnership(ownership);
        JavaTypeTraversalResult typeTraversalResult = typeDeclarationFlow.handleTypeNode(
            new JavaTypeNodeRequest(
                parseResult,
                accumulator,
                relativePath,
                packageName,
                extractionMode,
                packageScopeId,
                fileEntityId,
                node,
                ownerContext,
                importsBySimpleName,
                declaredTypes,
                extractionContext
            )
        );
        if (typeTraversalResult.handled()) {
            return JavaNodeDispatchResult.handledType(typeTraversalResult);
        }

        if (isJavaFieldDeclaration(node)) {
            JavaMemberExtractionResult fieldResult = fieldExtractionFlow.handleFieldNode(
                new JavaMemberNodeRequest(
                    parseResult,
                    accumulator,
                    relativePath,
                    packageName,
                    extractionMode,
                    fileScopeId,
                    fileEntityId,
                    node,
                    ownerContext,
                    importsBySimpleName,
                    declaredTypes,
                    extractionContext
                )
            );
            return JavaNodeDispatchResult.handledMember(ownerContext.toTraversalOwnership(), fieldResult);
        }

        if (isJavaMethodLikeDeclaration(node)) {
            JavaMemberExtractionResult methodResult = methodExtractionFlow.handleMethodNode(
                new JavaMemberNodeRequest(
                    parseResult,
                    accumulator,
                    relativePath,
                    packageName,
                    extractionMode,
                    fileScopeId,
                    fileEntityId,
                    node,
                    ownerContext,
                    importsBySimpleName,
                    declaredTypes,
                    extractionContext
                )
            );
            return JavaNodeDispatchResult.handledMember(ownerContext.toTraversalOwnership(), methodResult);
        }
        return JavaNodeDispatchResult.notHandled(ownerContext.toTraversalOwnership());
    }

    private static boolean isJavaFieldDeclaration(SyntaxNode node) {
        return node != null && Set.of("field_declaration", "constant_declaration").contains(node.type());
    }

    private static boolean isJavaMethodLikeDeclaration(SyntaxNode node) {
        return node != null && Set.of("method_declaration", "constructor_declaration").contains(node.type());
    }
}
