package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityOrigin;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.SourceReference;
import info.isaksson.erland.architecturebrowser.indexer.parse.SyntaxNode;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class JavaJaxRsSemanticsSupport {
void addJaxRsResourceMetadata(
        ExtractionAccumulator accumulator,
        JavaTypeContext typeContext
    ) {
        String relativePath = typeContext.extractionContext().relativePath();
        SyntaxNode typeNode = typeContext.typeNode();
        ExtractedEntityFact typeEntity = typeContext.typeEntity();
        if (typeEntity == null || typeEntity.kind() != EntityKind.CLASS || !JavaJaxRsDomainSemanticsSupport.isJaxRsResource(typeEntity)) {
            return;
        }
        String basePath = JavaJaxRsDomainSemanticsSupport.extractJaxRsPath(typeEntity.sourceRefs().isEmpty() ? (typeNode == null ? "" : typeNode.textSnippet()) : typeEntity.sourceRefs().getFirst().snippet())
            .orElse("/");
        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>(typeEntity.metadata());
        metadata.put("framework", "jax-rs");
        metadata.put("jaxRsResource", true);
        metadata.put("jaxRsBasePath", JavaJaxRsDomainSemanticsSupport.normalizeJaxRsPath(basePath));
        metadata.put("jaxRsResourceQualifiedName", String.valueOf(typeEntity.metadata().getOrDefault("qualifiedName", typeEntity.name())));
        SourceReference ref = typeEntity.sourceRefs().isEmpty()
            ? ExtractionSupport.sourceRef(relativePath, SyntaxTreeExtractionSupport.oneBasedLine(typeNode), typeNode.textSnippet(), Map.of("language", "java", "kind", "class_declaration"))
            : typeEntity.sourceRefs().getFirst();
        accumulator.addEntity(new ExtractedEntityFact(
            typeEntity.id(),
            typeEntity.kind(),
            typeEntity.origin(),
            typeEntity.name(),
            typeEntity.displayName(),
            typeEntity.scopeId(),
            List.of(ref),
            Map.copyOf(metadata)
        ));
    }

void addJaxRsEndpointFacts(
        ExtractionAccumulator accumulator,
        JavaMethodContext methodContext
    ) {
        String relativePath = methodContext.extractionContext().relativePath();
        SyntaxNode methodNode = methodContext.methodNode();
        String ownerTypeEntityId = methodContext.ownerTypeEntityId();
        String ownerQualifiedName = methodContext.ownerQualifiedName();
        String ownerTypeSnippet = methodContext.ownerTypeSnippet();
        ExtractedEntityFact methodEntity = methodContext.methodEntity();
        if (methodEntity == null || ownerTypeEntityId == null || ownerQualifiedName == null || ownerQualifiedName.isBlank()) {
            return;
        }
        List<String> annotations = JavaDeclaredTypeSupport.metadataStringList(methodEntity.metadata().get("annotations"));
        String httpMethod = JavaJaxRsDomainSemanticsSupport.jaxRsHttpMethod(annotations).orElse(null);
        if (httpMethod == null) {
            return;
        }
        SourceReference methodRef = methodEntity.sourceRefs().isEmpty()
            ? ExtractionSupport.sourceRef(relativePath, SyntaxTreeExtractionSupport.oneBasedLine(methodNode), methodNode.textSnippet(), Map.of("language", "java", "kind", methodNode.type()))
            : methodEntity.sourceRefs().getFirst();
        String classSnippet = ownerTypeSnippet == null ? "" : ownerTypeSnippet;
        String classPath = JavaJaxRsDomainSemanticsSupport.extractJaxRsPath(classSnippet).orElse("");
        String methodPath = JavaJaxRsDomainSemanticsSupport.extractJaxRsPath(methodRef.snippet()).orElse("");
        String resolvedPath = JavaJaxRsDomainSemanticsSupport.normalizeJaxRsEndpointPath(classPath, methodPath);
        String endpointName = httpMethod + " " + resolvedPath;
        int endpointLine = methodRef.startLine() == null ? SyntaxTreeExtractionSupport.oneBasedLine(methodNode) : methodRef.startLine();
        String endpointId = IdUtils.scopedEntityId("java-endpoint", relativePath, ownerQualifiedName + "#" + endpointName, endpointLine);
        List<Map<String, String>> parameterDetails = JavaJaxRsDomainSemanticsSupport.extractJaxRsParameterDetails(String.valueOf(methodEntity.metadata().getOrDefault("parameters", "()")));
        JavaEndpointSemantics endpointSemantics = new JavaEndpointSemantics(
            httpMethod,
            resolvedPath,
            JavaJaxRsDomainSemanticsSupport.normalizeJaxRsPath(classPath),
            JavaJaxRsDomainSemanticsSupport.normalizeJaxRsPath(methodPath),
            ownerQualifiedName,
            methodEntity.name(),
            ownerQualifiedName + "#" + methodEntity.name(),
            parameterDetails,
            annotations
        );
        accumulator.addEntity(new ExtractedEntityFact(
            endpointId,
            EntityKind.ENDPOINT,
            EntityOrigin.OBSERVED,
            endpointName,
            endpointName,
            methodEntity.scopeId(),
            List.of(methodRef),
            endpointSemantics.endpointMetadata()
        ));
        accumulator.addRelationship(ExtractionSupport.typedRelationship(
            info.isaksson.erland.architecturebrowser.indexer.ir.model.RelationshipKind.EXPOSES,
            "exposes-jaxrs-endpoint",
            ownerTypeEntityId,
            endpointId,
            endpointName,
            methodRef,
            "java",
            Map.of("framework", "jax-rs", "httpMethod", httpMethod, "path", resolvedPath)
        ));
        Map<String, Object> methodMetadata = endpointSemantics.methodMetadata(methodEntity.metadata());
        accumulator.addEntity(new ExtractedEntityFact(
            methodEntity.id(),
            methodEntity.kind(),
            methodEntity.origin(),
            methodEntity.name(),
            methodEntity.displayName(),
            methodEntity.scopeId(),
            List.of(methodRef),
            methodMetadata
        ));
    }
    
}
