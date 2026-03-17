package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact;
import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractionMode;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityOrigin;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.SourceReference;
import info.isaksson.erland.architecturebrowser.indexer.naming.DisplayNamePolicy;
import info.isaksson.erland.architecturebrowser.indexer.parse.SourceParseResult;
import info.isaksson.erland.architecturebrowser.indexer.parse.SyntaxNode;

import java.util.List;
import java.util.Map;
import java.util.Set;

final class TypeScriptMethodDeclarationSemanticsSupport {
    private TypeScriptMethodDeclarationSemanticsSupport() {
    }

    static ExtractedEntityFact toTypeScriptMethodEntity(
        SourceParseResult parseResult,
        String relativePath,
        ExtractionMode extractionMode,
        String fileScopeId,
        SyntaxNode methodNode,
        String ownerQualifiedName,
        String ownerDeclarationKind
    ) {
        String methodName = SyntaxTreeExtractionSupport.declarationName(methodNode);
        if (methodName == null || methodName.isBlank()) {
            return null;
        }
        String parameterSnippet = SyntaxTreeExtractionSupport.parameterSnippet(methodNode);
        int line = SyntaxTreeExtractionSupport.oneBasedLine(methodNode);
        SourceReference ref = ExtractionSupport.sourceRef(relativePath, line, methodNode.textSnippet(), Map.of("language", "typescript", "kind", methodNode.type()));
        List<String> decorators = SyntaxTreeExtractionSupport.descendantsByType(methodNode, Set.of("decorator")).stream()
            .flatMap(node -> SyntaxTreeExtractionSupport.extractAnnotationsFromSnippet(node.textSnippet()).stream())
            .distinct()
            .toList();
        String canonicalName = ownerQualifiedName == null || ownerQualifiedName.isBlank() ? methodName : ownerQualifiedName + "#" + methodName;
        return new ExtractedEntityFact(
            IdUtils.scopedEntityId("typescript", relativePath, canonicalName, line),
            EntityKind.FUNCTION,
            EntityOrigin.OBSERVED,
            methodName,
            DisplayNamePolicy.entityDisplayName(EntityKind.FUNCTION, canonicalName, "typescript"),
            fileScopeId,
            List.of(ref),
            Map.of(
                "language", "typescript",
                "parameters", parameterSnippet,
                "returnType", SyntaxTreeExtractionSupport.typeScriptMethodReturnType(methodNode),
                "parameterTypes", SyntaxTreeExtractionSupport.typeScriptMethodParameterDeclaredTypes(methodNode),
                "decorators", decorators,
                "ownerQualifiedName", ownerQualifiedName == null ? "" : ownerQualifiedName,
                "ownerDeclarationKind", ownerDeclarationKind == null ? "" : ownerDeclarationKind,
                "parseStatus", parseResult.status().name(),
                "extractionMode", extractionMode.name()
            )
        );
    }
}
