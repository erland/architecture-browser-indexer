package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.SourceReference;
import info.isaksson.erland.architecturebrowser.indexer.parse.SyntaxNode;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Transitional compatibility facade kept thin while Java semantics move behind explicit domain supports.
 */
final class JavaExtractionSemanticsSupport {
    private JavaExtractionSemanticsSupport() {
    }

    record DetectedWritePath(String operation, String writeKind, String argumentExpression, String viaField, String viaType) {}
    record PublishedCdiEvent(String eventType, boolean async, String publisherField) {}
    record ObservedCdiEvent(String eventType, boolean async, List<String> qualifiers) {}
    record ResolvedJavaType(String entityId, String label, EntityKind kind) {}

    static List<DetectedWritePath> detectJpaWriteOperations(ExtractedEntityFact methodEntity, String snippet) {
        return JavaWritePathDetectionSupport.detectJpaWriteOperations(methodEntity, snippet).stream()
            .map(it -> new DetectedWritePath(it.operation(), it.writeKind(), it.argumentExpression(), it.viaField(), it.viaType()))
            .toList();
    }

    static List<DetectedWritePath> detectRepositoryWriteOperations(ExtractedEntityFact methodEntity, String snippet) {
        return JavaWritePathDetectionSupport.detectRepositoryWriteOperations(methodEntity, snippet).stream()
            .map(it -> new DetectedWritePath(it.operation(), it.writeKind(), it.argumentExpression(), it.viaField(), it.viaType()))
            .toList();
    }

    static String normalizeWriteOperation(String rawOperation) { return JavaWritePathDetectionSupport.normalizeWriteOperation(rawOperation); }
    static Map<String, String> collectMethodVariableTypes(ExtractedEntityFact methodEntity, String snippet) { return JavaWritePathDetectionSupport.collectMethodVariableTypes(methodEntity, snippet); }
    static Optional<String> resolveWriteTargetEntityType(String argumentExpression, Map<String, String> variableTypes) { return JavaWritePathDetectionSupport.resolveWriteTargetEntityType(argumentExpression, variableTypes); }
    static String exactNodeSnippet(String sourceText, SyntaxNode node) { return JavaGenericSyntaxSupport.exactNodeSnippet(sourceText, node); }
    static List<String> extractParameterNames(String parameterSnippet) { return JavaDeclaredTypeSupport.extractParameterNames(parameterSnippet); }

    static List<PublishedCdiEvent> detectCdiPublishedEvents(String methodSnippet, String ownerTypeSnippet) {
        return JavaCdiDomainSemanticsSupport.detectCdiPublishedEvents(methodSnippet, ownerTypeSnippet).stream()
            .map(it -> new PublishedCdiEvent(it.eventType(), it.async(), it.publisherField()))
            .toList();
    }

    static Optional<PublishedCdiEvent> extractCdiEventTypeFromField(String ownerTypeSnippet, String publisherField) {
        return JavaCdiDomainSemanticsSupport.extractCdiEventTypeFromField(ownerTypeSnippet, publisherField)
            .map(type -> new PublishedCdiEvent(type, false, publisherField));
    }

    static Optional<String> extractCdiEventTypeFromArguments(String args) { return JavaCdiDomainSemanticsSupport.extractCdiEventTypeFromArguments(args); }

    static Optional<ObservedCdiEvent> detectCdiObservedEvent(ExtractedEntityFact methodEntity, String snippet) {
        return JavaCdiDomainSemanticsSupport.detectCdiObservedEvent(methodEntity, snippet)
            .map(it -> new ObservedCdiEvent(it.eventType(), it.async(), it.qualifiers()));
    }

    static List<String> extractExtendedTypes(SyntaxNode typeNode) { return JavaGenericSyntaxSupport.extractExtendedTypes(typeNode); }
    static boolean isConstructor(ExtractedEntityFact methodEntity) { return JavaGenericSyntaxSupport.isConstructor(methodEntity); }
    static String simpleName(String qualifiedName) { return JavaGenericSyntaxSupport.simpleName(qualifiedName); }
    static boolean containsJpaPropertyAnnotation(String snippet) { return JavaJpaDomainSemanticsSupport.containsJpaPropertyAnnotation(snippet); }
    static boolean hasAnyJpaAnnotation(String snippet, String... simpleNames) { return JavaJpaDomainSemanticsSupport.hasAnyJpaAnnotation(snippet, simpleNames); }
    static String deriveJavaPropertyName(String methodName, String parameterSnippet) { return JavaJpaDomainSemanticsSupport.deriveJavaPropertyName(methodName, parameterSnippet); }
    static boolean isJpaPersistentType(String snippet, ExtractedEntityFact entity) { return JavaJpaDomainSemanticsSupport.isJpaPersistentType(snippet, entity); }
    static Optional<String> detectJpaTypeKind(String snippet, ExtractedEntityFact entity) { return JavaJpaDomainSemanticsSupport.detectJpaTypeKind(snippet, entity); }
    static boolean hasAnnotation(List<String> annotations, String simpleName) { return JavaJpaDomainSemanticsSupport.hasAnnotation(annotations, simpleName); }
    static boolean containsAnnotationSnippet(String snippet, String simpleName) { return JavaJpaDomainSemanticsSupport.containsAnnotationSnippet(snippet, simpleName); }
    static Optional<String> extractJpaTableName(String snippet) { return JavaJpaDomainSemanticsSupport.extractJpaTableName(snippet); }
    static Optional<String> extractJpaColumnName(String snippet) { return JavaJpaDomainSemanticsSupport.extractJpaColumnName(snippet); }
    static Optional<String> extractJpaJoinColumn(String snippet) { return JavaJpaDomainSemanticsSupport.extractJpaJoinColumn(snippet); }
    static Optional<String> extractJpaJoinTable(String snippet) { return JavaJpaDomainSemanticsSupport.extractJpaJoinTable(snippet); }
    static Optional<String> extractJpaMappedBy(String snippet) { return JavaJpaDomainSemanticsSupport.extractJpaMappedBy(snippet); }
    static Optional<String> extractJpaInheritanceStrategy(String snippet) { return JavaJpaDomainSemanticsSupport.extractJpaInheritanceStrategy(snippet); }
    static Optional<String> detectJpaAssociation(String snippet) { return JavaJpaDomainSemanticsSupport.detectJpaAssociation(snippet); }
    static Optional<String> detectJpaAssociation(List<String> annotations, String snippet) { return JavaJpaDomainSemanticsSupport.detectJpaAssociation(annotations, snippet); }
    static Optional<String> inferJavaMethodReturnTypeFromSnippet(String snippet, String methodName) { return JavaGenericSyntaxSupport.inferJavaMethodReturnTypeFromSnippet(snippet, methodName); }
    static boolean isJavaTypeDeclaration(SyntaxNode node) { return JavaGenericSyntaxSupport.isJavaTypeDeclaration(node); }
    static boolean isJaxRsResource(ExtractedEntityFact entity) { return JavaJaxRsDomainSemanticsSupport.isJaxRsResource(entity); }
    static Optional<String> jaxRsHttpMethod(List<String> annotations) { return JavaJaxRsDomainSemanticsSupport.jaxRsHttpMethod(annotations); }
    static Optional<String> extractJaxRsPath(String snippet) { return JavaJaxRsDomainSemanticsSupport.extractJaxRsPath(snippet); }
    static String normalizeJaxRsPath(String value) { return JavaJaxRsDomainSemanticsSupport.normalizeJaxRsPath(value); }
    static String normalizeJaxRsEndpointPath(String classPath, String methodPath) { return JavaJaxRsDomainSemanticsSupport.normalizeJaxRsEndpointPath(classPath, methodPath); }
    static List<Map<String, String>> extractJaxRsParameterDetails(String parameterSnippet) { return JavaJaxRsDomainSemanticsSupport.extractJaxRsParameterDetails(parameterSnippet); }
    static List<String> splitTopLevelCommaSeparated(String value) { return JavaJaxRsDomainSemanticsSupport.splitTopLevelCommaSeparated(value); }
    static String extractParameterName(String snippet) { return JavaJaxRsDomainSemanticsSupport.extractParameterName(snippet); }
    static String extractParameterDeclaredType(String snippet) { return JavaJaxRsDomainSemanticsSupport.extractParameterDeclaredType(snippet); }
    static String classifyJaxRsParameter(String snippet) { return JavaJaxRsDomainSemanticsSupport.classifyJaxRsParameter(snippet); }
    static int lineOf(SourceReference ref, SyntaxNode fallbackNode) { return JavaGenericSyntaxSupport.lineOf(ref, fallbackNode); }
    static Optional<String> importQualifiedName(String snippet) { return JavaGenericSyntaxSupport.importQualifiedName(snippet); }
    static String derivePackageFromPath(String relativePath) { return JavaGenericSyntaxSupport.derivePackageFromPath(relativePath); }
}
