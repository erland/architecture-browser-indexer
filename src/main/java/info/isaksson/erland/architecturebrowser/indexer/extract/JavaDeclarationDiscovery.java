package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact;
import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractionMode;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;
import info.isaksson.erland.architecturebrowser.indexer.parse.SourceParseResult;
import info.isaksson.erland.architecturebrowser.indexer.parse.SyntaxNode;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class JavaDeclarationDiscovery {

    private static final JavaEntityMapper ENTITY_MAPPER = new JavaEntityMapper();

    private JavaDeclarationDiscovery() {
    }

    static Map<String, JavaDeclaredType> discoverDeclaredTypes(
        SourceParseResult parseResult,
        String relativePath,
        String packageName,
        ExtractionMode extractionMode,
        String packageScopeId,
        SyntaxNode root
    ) {
        LinkedHashMap<String, JavaDeclaredType> declaredTypes = new LinkedHashMap<>();
        collectDeclaredTypes(
            parseResult,
            relativePath,
            packageName,
            extractionMode,
            packageScopeId,
            root,
            null,
            declaredTypes
        );
        return Map.copyOf(declaredTypes);
    }

    private static void collectDeclaredTypes(
        SourceParseResult parseResult,
        String relativePath,
        String packageName,
        ExtractionMode extractionMode,
        String packageScopeId,
        SyntaxNode node,
        String owningQualifiedName,
        Map<String, JavaDeclaredType> declaredTypes
    ) {
        if (node == null) {
            return;
        }
        String nextOwningQualifiedName = owningQualifiedName;
        if (JavaStructuralExtractor.isJavaTypeDeclaration(node)) {
            JavaDeclaredType declaredType = toDeclaredType(
                parseResult,
                relativePath,
                packageName,
                extractionMode,
                packageScopeId,
                node,
                owningQualifiedName
            );
            if (declaredType != null) {
                String qualifiedName = declaredType.qualifiedName();
                declaredTypes.putIfAbsent(qualifiedName, declaredType);
                String simpleName = JavaStructuralExtractor.simpleName(qualifiedName);
                if (simpleName != null && !simpleName.isBlank()) {
                    declaredTypes.putIfAbsent(simpleName, declaredType);
                }
                nextOwningQualifiedName = qualifiedName;
            }
        }
        for (SyntaxNode child : node.children()) {
            collectDeclaredTypes(
                parseResult,
                relativePath,
                packageName,
                extractionMode,
                packageScopeId,
                child,
                nextOwningQualifiedName,
                declaredTypes
            );
        }
    }
    private static JavaDeclaredType toDeclaredType(
        SourceParseResult parseResult,
        String relativePath,
        String packageName,
        ExtractionMode extractionMode,
        String packageScopeId,
        SyntaxNode typeNode,
        String owningQualifiedName
    ) {
        ExtractedEntityFact typeEntity = ENTITY_MAPPER.toTypeEntity(
            parseResult,
            relativePath,
            packageName,
            extractionMode,
            packageScopeId,
            typeNode,
            owningQualifiedName
        );
        if (typeEntity != null) {
            String qualifiedName = String.valueOf(typeEntity.metadata().get("qualifiedName"));
            return new JavaDeclaredType(typeEntity.id(), qualifiedName, typeEntity.kind());
        }

        String typeName = fallbackTypeName(typeNode);
        if (typeName == null || typeName.isBlank()) {
            return null;
        }
        String qualifiedName = owningQualifiedName == null || owningQualifiedName.isBlank()
            ? (packageName == null || packageName.isBlank() ? typeName : packageName + "." + typeName)
            : owningQualifiedName + "." + typeName;
        EntityKind kind = "interface_declaration".equals(typeNode.type()) ? EntityKind.INTERFACE : EntityKind.CLASS;
        int line = SyntaxTreeExtractionSupport.oneBasedLine(typeNode);
        String entityId = IdUtils.scopedEntityId("java", relativePath, qualifiedName, line);
        return new JavaDeclaredType(entityId, qualifiedName, kind);
    }

    private static String fallbackTypeName(SyntaxNode typeNode) {
        String declarationName = SyntaxTreeExtractionSupport.declarationName(typeNode);
        if (declarationName != null && !declarationName.isBlank()) {
            return declarationName;
        }
        String snippet = typeNode == null ? null : typeNode.textSnippet();
        if (snippet == null || snippet.isBlank()) {
            return null;
        }
        String trimmed = snippet.trim();
        if (trimmed.matches("[A-Za-z_$][\\w$]*")) {
            return trimmed;
        }
        Matcher matcher = Pattern.compile("\\b(class|interface|enum|record)\\s+([A-Za-z_$][\\w$]*)").matcher(trimmed);
        if (matcher.find()) {
            return matcher.group(2);
        }
        return null;
    }

}
