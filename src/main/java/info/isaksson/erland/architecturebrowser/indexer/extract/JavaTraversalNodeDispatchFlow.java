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

    JavaSyntaxTreeTraversal.JavaTraversalOwnership handleNode(
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
            return ownership;
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
            return typeTraversalResult.ownerContext().toTraversalOwnership();
        }

        if (isJavaFieldDeclaration(node)) {
            fieldExtractionFlow.handleFieldNode(
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
            return ownerContext.toTraversalOwnership();
        }

        if (isJavaMethodLikeDeclaration(node)) {
            methodExtractionFlow.handleMethodNode(
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
        }
        return ownerContext.toTraversalOwnership();
    }

    private static boolean isJavaFieldDeclaration(SyntaxNode node) {
        return node != null && Set.of("field_declaration", "constant_declaration").contains(node.type());
    }

    private static boolean isJavaMethodLikeDeclaration(SyntaxNode node) {
        return node != null && Set.of("method_declaration", "constructor_declaration").contains(node.type());
    }
}
