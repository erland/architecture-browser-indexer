package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.SourceReference;
import info.isaksson.erland.architecturebrowser.indexer.parse.SyntaxNode;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

final class JavaCdiSemanticsSupport {
    private final JavaRelationshipEvidenceEmitter relationshipEvidenceEmitter;

    JavaCdiSemanticsSupport(JavaRelationshipEvidenceEmitter relationshipEvidenceEmitter) {
        this.relationshipEvidenceEmitter = relationshipEvidenceEmitter;
    }

    private JavaRelationshipEvidenceEmitter.ResolvedJavaType resolveJavaTypeReference(
        ExtractionAccumulator accumulator,
        String referencedType,
        EntityKind fallbackTargetKind,
        String relativePath,
        String packageName,
        int line,
        Map<String, String> importsBySimpleName,
        Map<String, JavaDeclaredType> declaredTypes
    ) {
        return relationshipEvidenceEmitter.resolveJavaTypeReference(
            accumulator, referencedType, fallbackTargetKind, relativePath, packageName, line, importsBySimpleName, declaredTypes
        );
    }
void addCdiEventFacts(
        ExtractionAccumulator accumulator,
        JavaMethodContext methodContext
    ) {
        JavaExtractionContext extractionContext = methodContext.extractionContext();
        String relativePath = extractionContext.relativePath();
        String packageName = extractionContext.packageName();
        SyntaxNode methodNode = methodContext.methodNode();
        ExtractedEntityFact methodEntity = methodContext.methodEntity();
        String ownerTypeEntityId = methodContext.ownerTypeEntityId();
        String ownerQualifiedName = methodContext.ownerQualifiedName();
        String ownerTypeSnippet = methodContext.ownerTypeSnippet();
        String sourceText = extractionContext.sourceText();
        SourceReference ref = methodContext.sourceRef();
        String snippet = methodContext.snippet();
        Map<String, String> importsBySimpleName = extractionContext.importsBySimpleName();
        Map<String, JavaDeclaredType> declaredTypes = extractionContext.declaredTypes();
        if (methodEntity == null || ownerTypeEntityId == null || ownerQualifiedName == null || ownerQualifiedName.isBlank()) {
            return;
        }
        String exactMethodSnippet = JavaGenericSyntaxSupport.exactNodeSnippet(sourceText, methodNode);
        if (exactMethodSnippet != null && !exactMethodSnippet.isBlank()) {
            snippet = exactMethodSnippet;
        }

        LinkedHashMap<String, Object> methodMetadata = new LinkedHashMap<>(methodEntity.metadata());
        boolean methodChanged = false;

        for (JavaCdiDomainSemanticsSupport.PublishedCdiEvent publication : JavaCdiDomainSemanticsSupport.detectCdiPublishedEvents(snippet, ownerTypeSnippet)) {
            JavaRelationshipEvidenceEmitter.ResolvedJavaType target = resolveJavaTypeReference(
                accumulator,
                publication.eventType(),
                EntityKind.CLASS,
                relativePath,
                packageName,
                JavaGenericSyntaxSupport.lineOf(ref, methodNode),
                importsBySimpleName,
                declaredTypes
            );
            if (target == null) {
                continue;
            }
            LinkedHashMap<String, Object> relationshipMetadata = new LinkedHashMap<>();
            relationshipMetadata.put("framework", "cdi");
            relationshipMetadata.put("relationshipType", "publishesEvent");
            relationshipMetadata.put("frameworkRelationship", "publishesEvent");
            relationshipMetadata.put("dependencySource", "eventPublish");
            relationshipMetadata.put("eventType", target.label());
            relationshipMetadata.put("publisherMethod", methodEntity.name());
            relationshipMetadata.put("publisherQualifiedName", ownerQualifiedName);
            relationshipMetadata.put("publisherAsync", publication.async());
            if (publication.publisherField() != null && !publication.publisherField().isBlank()) {
                relationshipMetadata.put("publisherField", publication.publisherField());
            }
            accumulator.addRelationship(ExtractionSupport.dependencyRelationship(
                ownerTypeEntityId,
                target.entityId(),
                target.label(),
                ref,
                "java",
                Map.copyOf(relationshipMetadata)
            ));
            LinkedHashMap<String, Object> methodRelationshipMetadata = new LinkedHashMap<>(relationshipMetadata);
            methodRelationshipMetadata.put("dependencySource", "eventPublishMethod");
            methodRelationshipMetadata.put("ownerMemberKind", "method");
            methodRelationshipMetadata.put("ownerMemberName", methodEntity.name());
            accumulator.addRelationship(ExtractionSupport.dependencyRelationship(
                methodEntity.id(),
                target.entityId(),
                target.label(),
                ref,
                "java",
                Map.copyOf(methodRelationshipMetadata)
            ));
            methodMetadata.put("framework", "cdi");
            methodMetadata.put("cdiEventPublisher", true);
            methodMetadata.put("cdiPublishedEventType", target.label());
            methodMetadata.put("cdiPublisherAsync", publication.async());
            methodChanged = true;
        }

        Optional<JavaCdiDomainSemanticsSupport.ObservedCdiEvent> observer = JavaCdiDomainSemanticsSupport.detectCdiObservedEvent(methodEntity, snippet);
        if (observer.isPresent()) {
            JavaCdiDomainSemanticsSupport.ObservedCdiEvent observed = observer.get();
            JavaRelationshipEvidenceEmitter.ResolvedJavaType target = resolveJavaTypeReference(
                accumulator,
                observed.eventType(),
                EntityKind.CLASS,
                relativePath,
                packageName,
                JavaGenericSyntaxSupport.lineOf(ref, methodNode),
                importsBySimpleName,
                declaredTypes
            );
            if (target != null) {
                LinkedHashMap<String, Object> eventToObserverMetadata = new LinkedHashMap<>();
                eventToObserverMetadata.put("framework", "cdi");
                eventToObserverMetadata.put("relationshipType", "eventObservedBy");
                eventToObserverMetadata.put("frameworkRelationship", "observesEvent");
                eventToObserverMetadata.put("eventType", target.label());
                eventToObserverMetadata.put("observerQualifiedName", ownerQualifiedName);
                eventToObserverMetadata.put("observerMethod", methodEntity.name());
                eventToObserverMetadata.put("observerAsync", observed.async());
                if (!observed.qualifiers().isEmpty()) {
                    eventToObserverMetadata.put("observerQualifiers", observed.qualifiers());
                }
                accumulator.addRelationship(ExtractionSupport.dependencyRelationship(
                    target.entityId(),
                    ownerTypeEntityId,
                    ownerQualifiedName,
                    ref,
                    "java",
                    Map.copyOf(eventToObserverMetadata)
                ));
                LinkedHashMap<String, Object> methodToEventMetadata = new LinkedHashMap<>();
                methodToEventMetadata.put("framework", "cdi");
                methodToEventMetadata.put("relationshipType", "observesEvent");
                methodToEventMetadata.put("frameworkRelationship", "observesEvent");
                methodToEventMetadata.put("eventType", target.label());
                methodToEventMetadata.put("observerAsync", observed.async());
                methodToEventMetadata.put("ownerMemberKind", "method");
                methodToEventMetadata.put("ownerMemberName", methodEntity.name());
                if (!observed.qualifiers().isEmpty()) {
                    methodToEventMetadata.put("observerQualifiers", observed.qualifiers());
                }
                accumulator.addRelationship(ExtractionSupport.dependencyRelationship(
                    methodEntity.id(),
                    target.entityId(),
                    target.label(),
                    ref,
                    "java",
                    Map.copyOf(methodToEventMetadata)
                ));
                methodMetadata.put("framework", "cdi");
                methodMetadata.put("cdiObserver", true);
                methodMetadata.put("cdiObservedEventType", target.label());
                methodMetadata.put("observerAsync", observed.async());
                methodMetadata.put("observerQualifiers", observed.qualifiers());
                methodChanged = true;
            }
        }

        if (methodChanged) {
            accumulator.addEntity(new ExtractedEntityFact(
                methodEntity.id(),
                methodEntity.kind(),
                methodEntity.origin(),
                methodEntity.name(),
                methodEntity.displayName(),
                methodEntity.scopeId(),
                List.of(ref),
                Map.copyOf(methodMetadata)
            ));
        }
    }
    
}
