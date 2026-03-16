package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact;
import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractionMode;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityOrigin;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.SourceReference;
import info.isaksson.erland.architecturebrowser.indexer.naming.DisplayNamePolicy;
import info.isaksson.erland.architecturebrowser.indexer.parse.SourceParseResult;
import info.isaksson.erland.architecturebrowser.indexer.parse.SyntaxNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class JavaEntityMapper {

    List<ExtractedEntityFact> toFieldEntities(
        SourceParseResult parseResult,
        String relativePath,
        ExtractionMode extractionMode,
        String fileScopeId,
        SyntaxNode fieldNode,
        String owningQualifiedName
    ) {
        List<String> fieldNames = SyntaxTreeExtractionSupport.javaFieldNames(fieldNode);
        if (fieldNames.isEmpty()) {
            return List.of();
        }
        String declaredType = SyntaxTreeExtractionSupport.javaFieldDeclaredType(fieldNode);
        List<String> annotations = SyntaxTreeExtractionSupport.descendantsByType(fieldNode, Set.of(
            "marker_annotation", "annotation"
        )).stream().flatMap(node -> SyntaxTreeExtractionSupport.extractAnnotationsFromSnippet(node.textSnippet()).stream()).distinct().toList();
        List<String> modifiers = SyntaxTreeExtractionSupport.javaModifiers(fieldNode);
        int line = SyntaxTreeExtractionSupport.oneBasedLine(fieldNode);
        SourceReference ref = ExtractionSupport.sourceRef(relativePath, line, fieldNode.textSnippet(), Map.of("language", "java", "kind", fieldNode.type()));
        List<ExtractedEntityFact> result = new ArrayList<>();
        for (String fieldName : fieldNames) {
            String canonicalName = owningQualifiedName == null || owningQualifiedName.isBlank()
                ? fieldName
                : owningQualifiedName + "#" + fieldName;
            result.add(new ExtractedEntityFact(
                IdUtils.scopedEntityId("java", relativePath, canonicalName, line),
                EntityKind.FIELD,
                EntityOrigin.OBSERVED,
                fieldName,
                DisplayNamePolicy.entityDisplayName(EntityKind.FIELD, canonicalName, "java"),
                fileScopeId,
                List.of(ref),
                Map.of(
                    "language", "java",
                    "declaredType", declaredType == null ? "" : declaredType,
                    "annotations", annotations,
                    "modifiers", modifiers,
                    "ownerQualifiedName", owningQualifiedName == null ? "" : owningQualifiedName,
                    "parseStatus", parseResult.status().name(),
                    "extractionMode", extractionMode.name()
                )
            ));
        }
        return List.copyOf(result);
    }

    ExtractedEntityFact toTypeEntity(
        SourceParseResult parseResult,
        String relativePath,
        String packageName,
        ExtractionMode extractionMode,
        String packageScopeId,
        SyntaxNode typeNode,
        String owningQualifiedName
    ) {
        String typeName = SyntaxTreeExtractionSupport.declarationName(typeNode);
        if (typeName == null || typeName.isBlank()) {
            return null;
        }
        int line = SyntaxTreeExtractionSupport.oneBasedLine(typeNode);
        EntityKind kind = "interface_declaration".equals(typeNode.type()) ? EntityKind.INTERFACE : EntityKind.CLASS;
        String declarationKind = javaDeclarationKind(typeNode.type());
        String qualifiedName = owningQualifiedName == null || owningQualifiedName.isBlank()
            ? (packageName == null || packageName.isBlank() ? typeName : packageName + "." + typeName)
            : owningQualifiedName + "." + typeName;
        List<String> annotations = SyntaxTreeExtractionSupport.descendantsByType(typeNode, Set.of(
            "marker_annotation", "annotation"
        )).stream().flatMap(node -> SyntaxTreeExtractionSupport.extractAnnotationsFromSnippet(node.textSnippet()).stream()).distinct().toList();
        SourceReference ref = ExtractionSupport.sourceRef(relativePath, line, typeNode.textSnippet(), Map.of("language", "java", "kind", typeNode.type()));
        return new ExtractedEntityFact(
            IdUtils.scopedEntityId("java", relativePath, qualifiedName, line),
            kind,
            EntityOrigin.OBSERVED,
            typeName,
            DisplayNamePolicy.entityDisplayName(kind, qualifiedName, "java"),
            packageScopeId,
            List.of(ref),
            Map.of(
                "language", "java",
                "qualifiedName", qualifiedName,
                "packageName", packageName,
                "declarationKind", declarationKind,
                "annotations", annotations,
                "parseStatus", parseResult.status().name(),
                "extractionMode", extractionMode.name()
            )
        );
    }

    ExtractedEntityFact toMethodEntity(
        SourceParseResult parseResult,
        String relativePath,
        ExtractionMode extractionMode,
        String fileScopeId,
        SyntaxNode methodNode,
        String owningQualifiedName
    ) {
        String methodName = SyntaxTreeExtractionSupport.javaMethodLikeName(methodNode);
        if (methodName == null || methodName.isBlank()) {
            return null;
        }
        String parameterSnippet = SyntaxTreeExtractionSupport.parameterSnippet(methodNode);
        int line = SyntaxTreeExtractionSupport.oneBasedLine(methodNode);
        SourceReference ref = ExtractionSupport.sourceRef(relativePath, line, methodNode.textSnippet(), Map.of("language", "java", "kind", methodNode.type()));
        List<String> annotations = SyntaxTreeExtractionSupport.descendantsByType(methodNode, Set.of(
            "marker_annotation", "annotation"
        )).stream().flatMap(node -> SyntaxTreeExtractionSupport.extractAnnotationsFromSnippet(node.textSnippet()).stream()).distinct().toList();
        return new ExtractedEntityFact(
            IdUtils.scopedEntityId("java", relativePath, (owningQualifiedName == null || owningQualifiedName.isBlank() ? methodName : owningQualifiedName + "#" + methodName), line),
            EntityKind.FUNCTION,
            EntityOrigin.OBSERVED,
            methodName,
            SyntaxTreeExtractionSupport.javaMethodDisplayName(methodName, parameterSnippet),
            fileScopeId,
            List.of(ref),
            Map.of(
                "language", "java",
                "parameters", parameterSnippet,
                "returnType", SyntaxTreeExtractionSupport.javaMethodReturnType(methodNode),
                "parameterTypes", SyntaxTreeExtractionSupport.javaMethodParameterDeclaredTypes(methodNode),
                "annotations", annotations,
                "ownerQualifiedName", owningQualifiedName == null ? "" : owningQualifiedName,
                "parseStatus", parseResult.status().name(),
                "extractionMode", extractionMode.name()
            )
        );
    }

    private static String javaDeclarationKind(String nodeType) {
        return switch (nodeType) {
            case "interface_declaration" -> "interface";
            case "enum_declaration" -> "enum";
            case "record_declaration" -> "record";
            case "class_declaration" -> "class";
            default -> "type";
        };
    }
}
