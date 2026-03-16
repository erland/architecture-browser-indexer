package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact;
import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractionMode;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityOrigin;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.SourceReference;
import info.isaksson.erland.architecturebrowser.indexer.naming.DisplayNamePolicy;
import info.isaksson.erland.architecturebrowser.indexer.parse.ParseLanguage;
import info.isaksson.erland.architecturebrowser.indexer.parse.SourceParseResult;
import info.isaksson.erland.architecturebrowser.indexer.parse.SyntaxNode;
import info.isaksson.erland.architecturebrowser.indexer.parse.SyntaxTree;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class JavaSyntaxTreeExtractionStage {

    private final JavaSyntaxTreeTraversal syntaxTreeTraversal = new JavaSyntaxTreeTraversal();
    private final JavaEntityMapper entityMapper = new JavaEntityMapper();
    private final JavaRelationshipEvidenceEmitter relationshipEvidenceEmitter = new JavaRelationshipEvidenceEmitter();
    private final JavaDependencyEmissionFlow dependencyEmissionFlow = new JavaDependencyEmissionFlow(relationshipEvidenceEmitter);
    private final JavaJaxRsSemantics jaxRsSemantics = new JavaJaxRsSemantics();
    private final JavaJpaSemantics jpaSemantics = new JavaJpaSemantics();
    private final JavaCdiSemantics cdiSemantics = new JavaCdiSemantics();
    private final JavaWritePathSemantics writePathSemantics = new JavaWritePathSemantics();
    private final JavaTypeSemanticsFlow typeSemanticsFlow = new JavaTypeSemanticsFlow(jaxRsSemantics, jpaSemantics);
    private final JavaTypeDeclarationFlow typeDeclarationFlow = new JavaTypeDeclarationFlow(entityMapper, dependencyEmissionFlow, typeSemanticsFlow);
    private final JavaJaxRsMethodSemantics jaxRsMethodSemantics = new JavaJaxRsMethodSemantics(jaxRsSemantics);
    private final JavaJpaMethodSemantics jpaMethodSemantics = new JavaJpaMethodSemantics(jpaSemantics);
    private final JavaCdiMethodSemantics cdiMethodSemantics = new JavaCdiMethodSemantics(cdiSemantics);
    private final JavaWritePathMethodSemantics writePathMethodSemantics = new JavaWritePathMethodSemantics(writePathSemantics);
    private final JavaMethodSemanticsFlow methodSemanticsFlow = new JavaMethodSemanticsFlow(jaxRsMethodSemantics, jpaMethodSemantics, cdiMethodSemantics, writePathMethodSemantics);
    private final JavaJpaFieldSemantics jpaFieldSemantics = new JavaJpaFieldSemantics(jpaSemantics);
    private final JavaFieldExtractionFlow fieldExtractionFlow = new JavaFieldExtractionFlow(entityMapper, dependencyEmissionFlow, jpaFieldSemantics);
    private final JavaMethodExtractionFlow methodExtractionFlow = new JavaMethodExtractionFlow(entityMapper, dependencyEmissionFlow, methodSemanticsFlow);
    private final JavaMemberExtractionFlow memberExtractionFlow = new JavaMemberExtractionFlow();

    ParseLanguage language() {
        return ParseLanguage.JAVA;
    }

    ExtractionAccumulator extract(SourceParseResult parseResult, ExtractionAccumulator accumulator) {
        accumulator.incrementFilesVisited();
        if (!parseResult.successful() || parseResult.syntaxTree() == null) {
            accumulator.addDiagnostic(ExtractionSupport.extractionWarning(
                parseResult,
                "extract.java.syntax-tree-required",
                "Java structural extraction requires a successful Tree-sitter syntax tree; no regex fallback is used."
            ));
            return accumulator;
        }
        return extractFromSyntaxTree(parseResult, accumulator, parseResult.request().relativePath(), parseResult.syntaxTree());
    }

    private ExtractionAccumulator extractFromSyntaxTree(
        SourceParseResult parseResult,
        ExtractionAccumulator accumulator,
        String relativePath,
        SyntaxTree syntaxTree
    ) {
        ExtractionMode extractionMode = ExtractionMode.SYNTAX_TREE;
        accumulator.incrementFilesExtracted("java", extractionMode);

        String repositoryScopeId = "scope:repo";
        var fileScope = ExtractionSupport.fileScope(repositoryScopeId, relativePath);
        accumulator.addScope(fileScope);

        SyntaxNode root = syntaxTree.root();
        String packageName = SyntaxTreeExtractionSupport.findAllByType(root, Set.of("package_declaration")).stream()
            .findFirst()
            .flatMap(node -> SyntaxTreeExtractionSupport.extractQualifiedName(node.textSnippet()))
            .orElse(derivePackageFromPath(relativePath));

        var packageScope = ExtractionSupport.packageScope(repositoryScopeId, packageName, relativePath, "java");
        accumulator.addScope(packageScope);

        var fileEntity = ExtractionSupport.fileModuleEntity(fileScope.id(), relativePath, "java");
        accumulator.addEntity(fileEntity);

        Map<String, String> importsBySimpleName = new LinkedHashMap<>();
        for (SyntaxNode importNode : SyntaxTreeExtractionSupport.findAllByType(root, Set.of("import_declaration"))) {
            String imported = importQualifiedName(importNode.textSnippet()).orElse(null);
            if (imported == null || imported.isBlank()) {
                continue;
            }
            int line = SyntaxTreeExtractionSupport.oneBasedLine(importNode);
            String simpleName = simpleName(imported);
            if (simpleName != null && !simpleName.isBlank()) {
                importsBySimpleName.putIfAbsent(simpleName, imported);
            }
            var external = ExtractionSupport.externalDependencyEntity("java", imported, relativePath, line);
            accumulator.addEntity(external);
            accumulator.addRelationship(ExtractionSupport.dependencyRelationship(
                fileEntity.id(), external.id(), imported,
                ExtractionSupport.sourceRef(relativePath, line, importNode.textSnippet(), Map.of("language", "java", "kind", "import")),
                "java",
                relationshipEvidenceEmitter.dependencyMetadata("import", "evidence")
            ));
        }

        Map<String, JavaDeclaredType> declaredTypes = JavaDeclarationDiscovery.discoverDeclaredTypes(
            parseResult,
            relativePath,
            packageName,
            extractionMode,
            packageScope.id(),
            root
        );

        JavaExtractionContext extractionContext = new JavaExtractionContext(
            relativePath,
            packageName,
            parseResult.request() == null ? null : parseResult.request().sourceText(),
            Map.copyOf(importsBySimpleName),
            Map.copyOf(declaredTypes)
        );

        syntaxTreeTraversal.traverse(
            root,
            new JavaSyntaxTreeTraversal.JavaTraversalOwnership(null, null, null),
            (node, ownership) -> handleTraversalNode(
                parseResult,
                accumulator,
                relativePath,
                packageName,
                extractionMode,
                packageScope.id(),
                fileScope.id(),
                fileEntity.id(),
                node,
                ownership,
                importsBySimpleName,
                declaredTypes,
                extractionContext
            )
        );
        return accumulator;
    }

    private JavaSyntaxTreeTraversal.JavaTraversalOwnership handleTraversalNode(
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
        }        JavaOwnerContext ownerContext = JavaOwnerContext.fromTraversalOwnership(ownership);
        JavaTypeTraversalResult typeTraversalResult = handleTypeNode(
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
            return new JavaSyntaxTreeTraversal.JavaTraversalOwnership(
                typeTraversalResult.owningTypeEntityId(),
                typeTraversalResult.owningQualifiedName(),
                typeTraversalResult.owningTypeSnippet()
            );
        }

        memberExtractionFlow.handleMemberNode(
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



    private JavaTypeTraversalResult handleTypeNode(JavaTypeNodeRequest request) {
        return typeDeclarationFlow.handleTypeNode(request);
    }

    private final class JavaMemberExtractionFlow {

        JavaMemberExtractionResult handleMemberNode(JavaMemberNodeRequest request) {
            if (isJavaFieldDeclaration(request.node())) {
                return fieldExtractionFlow.handleFieldNode(request);
            }
            if (isJavaMethodLikeDeclaration(request.node())) {
                return methodExtractionFlow.handleMethodNode(request);
            }
            return JavaMemberExtractionResult.notHandled();
        }
    }

    private static List<DetectedWritePath> detectJpaWriteOperations(ExtractedEntityFact methodEntity, String snippet) {
        if (snippet == null || snippet.isBlank()) {
            return List.of();
        }
        List<DetectedWritePath> result = new ArrayList<>();
        Matcher matcher = Pattern.compile("([A-Za-z_$][\\w$]*)\\s*\\.\\s*(persist|merge|remove)\\s*\\(([^)]*)\\)", Pattern.DOTALL).matcher(snippet);
        while (matcher.find()) {
            result.add(new DetectedWritePath(matcher.group(2).toLowerCase(Locale.ROOT), "entity-manager", matcher.group(3).strip(), matcher.group(1), null));
        }
        return List.copyOf(result);
    }

    private static List<DetectedWritePath> detectRepositoryWriteOperations(ExtractedEntityFact methodEntity, String snippet) {
        if (snippet == null || snippet.isBlank()) {
            return List.of();
        }
        List<DetectedWritePath> result = new ArrayList<>();
        Matcher callMatcher = Pattern.compile("([A-Za-z_$][\\w$]*)\\s*\\.\\s*(saveAndFlush|save|update|delete|remove)\\s*\\(([^)]*)\\)", Pattern.DOTALL).matcher(snippet);
        while (callMatcher.find()) {
            String operation = normalizeWriteOperation(callMatcher.group(2));
            result.add(new DetectedWritePath(operation, "repository-call", callMatcher.group(3).strip(), callMatcher.group(1), null));
        }
        String ownerQualifiedName = String.valueOf(methodEntity.metadata().getOrDefault("ownerQualifiedName", ""));
        String loweredOwner = ownerQualifiedName.toLowerCase(Locale.ROOT);
        String methodName = methodEntity.name() == null ? "" : methodEntity.name();
        if (loweredOwner.contains("repository") || loweredOwner.contains("repo")) {
            String operation = normalizeWriteOperation(methodName);
            if (operation != null) {
                List<String> parameterTypes = metadataStringList(methodEntity.metadata().get("parameterTypes"));
                String params = String.valueOf(methodEntity.metadata().getOrDefault("parameters", ""));
                List<String> paramNames = extractParameterNames(params);
                for (int i = 0; i < Math.min(parameterTypes.size(), paramNames.size()); i++) {
                    String type = JavaRelationshipEvidenceEmitter.normalizeTypeReference(parameterTypes.get(i));
                    if (!type.isBlank()) {
                        result.add(new DetectedWritePath(operation, "repository-method", paramNames.get(i), null, type));
                    }
                }
            }
        }
        return List.copyOf(result);
    }

    private static String normalizeWriteOperation(String rawOperation) {
        if (rawOperation == null || rawOperation.isBlank()) {
            return null;
        }
        String value = rawOperation.toLowerCase(Locale.ROOT);
        if (value.contains("save")) return "persist";
        if (value.contains("merge") || value.contains("update")) return "merge";
        if (value.contains("delete") || value.contains("remove")) return "remove";
        if (value.equals("persist")) return "persist";
        return null;
    }

    private static Map<String, String> collectMethodVariableTypes(ExtractedEntityFact methodEntity, String snippet) {
        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        String params = String.valueOf(methodEntity.metadata().getOrDefault("parameters", ""));
        List<String> paramTypes = metadataStringList(methodEntity.metadata().get("parameterTypes"));
        List<String> paramNames = extractParameterNames(params);
        for (int i = 0; i < Math.min(paramTypes.size(), paramNames.size()); i++) {
            String type = JavaRelationshipEvidenceEmitter.normalizeTypeReference(paramTypes.get(i));
            if (!type.isBlank()) {
                result.putIfAbsent(paramNames.get(i), type);
            }
        }
        if (snippet != null && !snippet.isBlank()) {
            Matcher matcher = Pattern.compile("([A-Za-z_$][\\w.$]*(?:\\s*<[^>{}]+>)?)\\s+([A-Za-z_$][\\w$]*)\\s*=", Pattern.DOTALL).matcher(snippet);
            while (matcher.find()) {
                String type = JavaRelationshipEvidenceEmitter.normalizeTypeReference(matcher.group(1));
                String name = matcher.group(2);
                if (!type.isBlank() && !isJavaPrimitiveOrKeyword(type)) {
                    result.putIfAbsent(name, type);
                }
            }
        }
        return Map.copyOf(result);
    }

    private static Optional<String> resolveWriteTargetEntityType(String argumentExpression, Map<String, String> variableTypes) {
        if (argumentExpression == null || argumentExpression.isBlank()) {
            return Optional.empty();
        }
        String arg = argumentExpression.strip();
        Matcher newMatcher = Pattern.compile("new\\s+([A-Za-z_$][\\w.$]*)\\b").matcher(arg);
        if (newMatcher.find()) {
            return Optional.of(newMatcher.group(1));
        }
        Matcher identifierMatcher = Pattern.compile("([A-Za-z_$][\\w$]*)").matcher(arg);
        while (identifierMatcher.find()) {
            String candidate = identifierMatcher.group(1);
            if (variableTypes.containsKey(candidate)) {
                return Optional.of(variableTypes.get(candidate));
            }
        }
        return Optional.empty();
    }

    private static String exactNodeSnippet(String sourceText, SyntaxNode node) {
        return JavaSourceReferenceSupport.exactNodeSnippet(sourceText, node);
    }

    private static List<String> extractParameterNames(String parameterSnippet) {
        return JavaDeclaredTypeSupport.extractParameterNames(parameterSnippet);
    }

    private record DetectedWritePath(String operation, String writeKind, String argumentExpression, String viaField, String viaType) {}
    private static List<PublishedCdiEvent> detectCdiPublishedEvents(String methodSnippet, String ownerTypeSnippet) {
        if (methodSnippet == null || methodSnippet.isBlank()) {
            return List.of();
        }
        java.util.LinkedHashMap<String, PublishedCdiEvent> events = new java.util.LinkedHashMap<>();
        Matcher matcher = Pattern.compile("([A-Za-z_$][\\w$]*)\\s*\\.\\s*(fireAsync|fire)\\s*\\((.*?)\\)", Pattern.DOTALL).matcher(methodSnippet);
        while (matcher.find()) {
            String publisherField = matcher.group(1);
            boolean async = "fireAsync".equals(matcher.group(2));
            String args = matcher.group(3) == null ? "" : matcher.group(3);
            String eventType = extractCdiEventTypeFromField(ownerTypeSnippet, publisherField)
                .or(() -> extractCdiEventTypeFromArguments(args))
                .orElse(null);
            if (eventType == null || eventType.isBlank()) {
                continue;
            }
            events.putIfAbsent(publisherField + ":" + eventType + ":" + async, new PublishedCdiEvent(eventType, async, publisherField));
        }
        return List.copyOf(events.values());
    }

    private static Optional<String> extractCdiEventTypeFromField(String ownerTypeSnippet, String publisherField) {
        if (ownerTypeSnippet == null || ownerTypeSnippet.isBlank() || publisherField == null || publisherField.isBlank()) {
            return Optional.empty();
        }
        Matcher matcher = Pattern.compile("(?:^|[;{}\\s])(?:@[A-Za-z_][\\w.]*\\s*(?:\\([^)]*\\))?\\s*)*(?:public|protected|private)?\\s*(?:static\\s+|final\\s+|transient\\s+)*?(?:[A-Za-z_][\\w.]*\\.)?Event\\s*<\\s*([^>]+?)\\s*>\\s+" + Pattern.quote(publisherField) + "\\b", Pattern.DOTALL).matcher(ownerTypeSnippet);
        if (matcher.find()) {
            return Optional.ofNullable(matcher.group(1)).map(String::trim).filter(value -> !value.isBlank());
        }
        return Optional.empty();
    }

    private static Optional<String> extractCdiEventTypeFromArguments(String args) {
        if (args == null || args.isBlank()) {
            return Optional.empty();
        }
        Matcher constructorMatcher = Pattern.compile("new\\s+([A-Za-z_$][\\w.$]*)\\b").matcher(args);
        if (constructorMatcher.find()) {
            return Optional.of(constructorMatcher.group(1));
        }
        return Optional.empty();
    }

    private static Optional<ObservedCdiEvent> detectCdiObservedEvent(ExtractedEntityFact methodEntity, String snippet) {
        String parameters = String.valueOf(methodEntity.metadata().getOrDefault("parameters", ""));
        if (parameters == null || parameters.isBlank() || "()".equals(parameters.strip())) {
            parameters = snippet;
        }
        if (parameters == null || parameters.isBlank()) {
            return Optional.empty();
        }
        String normalized = parameters.replace('\n', ' ').replace('\r', ' ');
        Matcher matcher = Pattern.compile("((?:@[A-Za-z_][\\w.]*\\s*(?:\\([^)]*\\))?\\s*)*)@(?:[A-Za-z_][\\w.]*\\.)?(ObservesAsync|Observes)\\b(?:\\s*\\([^)]*\\))?\\s+([A-Za-z_$][\\w.$<>]*)").matcher(normalized);
        if (!matcher.find()) {
            return Optional.empty();
        }
        boolean async = "ObservesAsync".equals(matcher.group(2));
        String eventType = matcher.group(3) == null ? "" : matcher.group(3).trim();
        String annotationPrefix = matcher.group(1) == null ? "" : matcher.group(1);
        java.util.LinkedHashSet<String> qualifiers = new java.util.LinkedHashSet<>(SyntaxTreeExtractionSupport.extractAnnotationsFromSnippet(annotationPrefix));
        qualifiers.removeIf(value -> value == null || value.endsWith("Observes") || value.endsWith("ObservesAsync"));
        return eventType.isBlank() ? Optional.empty() : Optional.of(new ObservedCdiEvent(eventType, async, List.copyOf(qualifiers)));
    }

    private ResolvedJavaType resolveJavaTypeReference(
        ExtractionAccumulator accumulator,
        String referencedType,
        EntityKind fallbackTargetKind,
        String relativePath,
        String packageName,
        int line,
        Map<String, String> importsBySimpleName,
        Map<String, JavaDeclaredType> declaredTypes
    ) {
        JavaRelationshipEvidenceEmitter.ResolvedJavaType resolved = relationshipEvidenceEmitter.resolveJavaTypeReference(
            accumulator, referencedType, fallbackTargetKind, relativePath, packageName, line, importsBySimpleName, declaredTypes
        );
        return resolved == null ? null : new ResolvedJavaType(resolved.entityId(), resolved.label(), resolved.kind());
    }

    private static List<String> extractExtendedTypes(SyntaxNode typeNode) {
        return JavaRelationshipEvidenceEmitter.extractExtendedTypes(typeNode);
    }

    private static List<String> extractReferencedTypes(String declaredTypeText) {
        return JavaRelationshipEvidenceEmitter.extractReferencedTypes(declaredTypeText);
    }


    private static boolean isJavaPrimitiveOrKeyword(String candidate) {
        return Set.of(
            "byte", "short", "int", "long", "float", "double", "boolean", "char", "void", "var", "this", "super"
        ).contains(candidate);
    }

    static boolean isConstructor(ExtractedEntityFact methodEntity) {
        if (methodEntity == null) {
            return false;
        }
        Object ownerQualifiedName = methodEntity.metadata().get("ownerQualifiedName");
        if (ownerQualifiedName == null || String.valueOf(ownerQualifiedName).isBlank()) {
            return false;
        }
        String ownerSimpleName = simpleName(String.valueOf(ownerQualifiedName));
        return ownerSimpleName != null && ownerSimpleName.equals(methodEntity.name());
    }

    static String simpleName(String qualifiedName) {
        if (qualifiedName == null || qualifiedName.isBlank()) {
            return null;
        }
        int idx = qualifiedName.lastIndexOf('.');
        return idx >= 0 ? qualifiedName.substring(idx + 1) : qualifiedName;
    }

    private record ResolvedJavaType(String entityId, String label, EntityKind kind) {
    }

    private record PublishedCdiEvent(String eventType, boolean async, String publisherField) {
    }

    private record ObservedCdiEvent(String eventType, boolean async, List<String> qualifiers) {
    }


    private static boolean containsJpaPropertyAnnotation(String snippet) {
        return hasAnyJpaAnnotation(snippet, "Id", "EmbeddedId", "Version", "Embedded", "Column", "OneToOne", "OneToMany", "ManyToOne", "ManyToMany", "JoinColumn", "JoinTable");
    }

    private static boolean hasAnyJpaAnnotation(String snippet, String... simpleNames) {
        if (snippet == null || snippet.isBlank()) {
            return false;
        }
        for (String simpleName : simpleNames) {
            if (containsAnnotationSnippet(snippet, simpleName)) {
                return true;
            }
        }
        return false;
    }

    private static String deriveJavaPropertyName(String methodName, String parameterSnippet) {
        if (methodName == null || methodName.isBlank()) {
            return null;
        }
        String params = parameterSnippet == null ? "" : parameterSnippet.strip();
        if (!(params.isBlank() || "()".equals(params))) {
            return null;
        }
        if (methodName.startsWith("get") && methodName.length() > 3 && Character.isUpperCase(methodName.charAt(3))) {
            return decapitalizeJavaProperty(methodName.substring(3));
        }
        if (methodName.startsWith("is") && methodName.length() > 2 && Character.isUpperCase(methodName.charAt(2))) {
            return decapitalizeJavaProperty(methodName.substring(2));
        }
        return null;
    }

    private static String decapitalizeJavaProperty(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        if (value.length() > 1 && Character.isUpperCase(value.charAt(0)) && Character.isUpperCase(value.charAt(1))) {
            return value;
        }
        return Character.toLowerCase(value.charAt(0)) + value.substring(1);
    }


    private static boolean isJpaPersistentType(String snippet, ExtractedEntityFact entity) {
        if (hasAnnotation(metadataStringList(entity == null ? null : entity.metadata().get("annotations")), "Entity")
            || hasAnnotation(metadataStringList(entity == null ? null : entity.metadata().get("annotations")), "Embeddable")
            || hasAnnotation(metadataStringList(entity == null ? null : entity.metadata().get("annotations")), "MappedSuperclass")) {
            return true;
        }
        String lower = snippet == null ? "" : snippet.toLowerCase(Locale.ROOT);
        return lower.contains("@entity") || lower.contains("@embeddable") || lower.contains("@mappedsuperclass");
    }

    private static Optional<String> detectJpaTypeKind(String snippet, ExtractedEntityFact entity) {
        List<String> annotations = metadataStringList(entity == null ? null : entity.metadata().get("annotations"));
        if (hasAnnotation(annotations, "Embeddable") || containsAnnotationSnippet(snippet, "Embeddable")) {
            return Optional.of("embeddable");
        }
        if (hasAnnotation(annotations, "MappedSuperclass") || containsAnnotationSnippet(snippet, "MappedSuperclass")) {
            return Optional.of("mapped-superclass");
        }
        if (hasAnnotation(annotations, "Entity") || containsAnnotationSnippet(snippet, "Entity")) {
            return Optional.of("entity");
        }
        return Optional.empty();
    }

    private static boolean hasAnnotation(List<String> annotations, String simpleName) {
        if (annotations == null || simpleName == null || simpleName.isBlank()) {
            return false;
        }
        String expected = simpleName.toLowerCase(Locale.ROOT);
        return annotations.stream().map(value -> value == null ? "" : value.toLowerCase(Locale.ROOT)).anyMatch(value -> value.endsWith(expected));
    }

    private static boolean containsAnnotationSnippet(String snippet, String simpleName) {
        if (snippet == null || simpleName == null || simpleName.isBlank()) {
            return false;
        }
        return snippet.toLowerCase(Locale.ROOT).contains("@" + simpleName.toLowerCase(Locale.ROOT));
    }

    private static Optional<String> extractJpaTableName(String snippet) {
        return extractAnnotationStringAttribute(snippet, "Table", "name");
    }

    private static Optional<String> extractJpaColumnName(String snippet) {
        return extractAnnotationStringAttribute(snippet, "Column", "name");
    }

    private static Optional<String> extractJpaJoinColumn(String snippet) {
        return extractAnnotationStringAttribute(snippet, "JoinColumn", "name");
    }

    private static Optional<String> extractJpaJoinTable(String snippet) {
        return extractAnnotationStringAttribute(snippet, "JoinTable", "name");
    }

    private static Optional<String> extractJpaMappedBy(String snippet) {
        return extractAnnotationStringAttribute(snippet, "OneToMany", "mappedBy")
            .or(() -> extractAnnotationStringAttribute(snippet, "OneToOne", "mappedBy"))
            .or(() -> extractAnnotationStringAttribute(snippet, "ManyToMany", "mappedBy"));
    }

    private static Optional<String> extractJpaInheritanceStrategy(String snippet) {
        if (snippet == null || snippet.isBlank()) {
            return Optional.empty();
        }
        Matcher strategy = Pattern.compile("@(?:[A-Za-z_][\\w.]*\\.)?Inheritance\\s*\\([^)]*strategy\\s*=\\s*(?:[A-Za-z_][\\w.]*\\.)?([A-Z_]+)", Pattern.DOTALL).matcher(snippet);
        if (strategy.find()) {
            return Optional.of(strategy.group(1));
        }
        return Optional.empty();
    }

    private static Optional<String> detectJpaAssociation(String snippet) {
        return detectJpaAssociation(List.of(), snippet);
    }

    private static Optional<String> detectJpaAssociation(List<String> annotations, String snippet) {
        if (hasAnnotation(annotations, "OneToOne") || containsAnnotationSnippet(snippet, "OneToOne")) return Optional.of("one-to-one");
        if (hasAnnotation(annotations, "OneToMany") || containsAnnotationSnippet(snippet, "OneToMany")) return Optional.of("one-to-many");
        if (hasAnnotation(annotations, "ManyToOne") || containsAnnotationSnippet(snippet, "ManyToOne")) return Optional.of("many-to-one");
        if (hasAnnotation(annotations, "ManyToMany") || containsAnnotationSnippet(snippet, "ManyToMany")) return Optional.of("many-to-many");
        return Optional.empty();
    }

    private static Optional<String> inferJavaMethodReturnTypeFromSnippet(String snippet, String methodName) {
        if (snippet == null || snippet.isBlank() || methodName == null || methodName.isBlank()) {
            return Optional.empty();
        }
        Pattern pattern = Pattern.compile("(?:public|protected|private)?\\s*(?:static\\s+)?(?:final\\s+)?([A-Za-z_$][\\w$<>., ?]+?)\\s+" + Pattern.quote(methodName) + "\\s*\\(");
        Matcher matcher = pattern.matcher(snippet.replace("\n", " "));
        if (matcher.find()) {
            return Optional.ofNullable(matcher.group(1)).map(String::trim).filter(value -> !value.isBlank());
        }
        return Optional.empty();
    }

    private static Optional<String> extractAnnotationStringAttribute(String snippet, String annotationSimpleName, String attributeName) {
        if (snippet == null || snippet.isBlank()) {
            return Optional.empty();
        }
        String annotationPattern = "@(?:[A-Za-z_][\\w.]*\\.)?" + Pattern.quote(annotationSimpleName) + "\\s*\\((.*?)\\)";
        Matcher matcher = Pattern.compile(annotationPattern, Pattern.DOTALL).matcher(snippet);
        while (matcher.find()) {
            String body = matcher.group(1);
            Matcher named = Pattern.compile(Pattern.quote(attributeName) + "\\s*=\\s*\"([^\"]*)\"").matcher(body);
            if (named.find()) {
                return Optional.of(named.group(1));
            }
            if ("name".equals(attributeName)) {
                Matcher positional = Pattern.compile("^\\s*\"([^\"]*)\"").matcher(body.strip());
                if (positional.find()) {
                    return Optional.of(positional.group(1));
                }
            }
        }
        return Optional.empty();
    }

    static boolean isJavaTypeDeclaration(SyntaxNode node) {
        return node != null && Set.of(
            "class_declaration", "interface_declaration", "enum_declaration", "record_declaration"
        ).contains(node.type());
    }

    private static boolean isJavaFieldDeclaration(SyntaxNode node) {
        return node != null && Set.of("field_declaration", "constant_declaration").contains(node.type());
    }

    private static boolean isJavaMethodLikeDeclaration(SyntaxNode node) {
        return node != null && Set.of("method_declaration", "constructor_declaration").contains(node.type());
    }



    private static boolean isJaxRsResource(ExtractedEntityFact entity) {
        return metadataStringList(entity.metadata().get("annotations")).stream()
            .map(value -> value.toLowerCase(Locale.ROOT))
            .anyMatch(value -> value.endsWith("path"));
    }

    private static Optional<String> jaxRsHttpMethod(List<String> annotations) {
        for (String annotation : annotations) {
            String value = annotation.toLowerCase(Locale.ROOT);
            if (value.endsWith("get")) return Optional.of("GET");
            if (value.endsWith("post")) return Optional.of("POST");
            if (value.endsWith("put")) return Optional.of("PUT");
            if (value.endsWith("delete")) return Optional.of("DELETE");
            if (value.endsWith("patch")) return Optional.of("PATCH");
            if (value.endsWith("head")) return Optional.of("HEAD");
            if (value.endsWith("options")) return Optional.of("OPTIONS");
        }
        return Optional.empty();
    }

    private static Optional<String> extractJaxRsPath(String snippet) {
        if (snippet == null || snippet.isBlank()) {
            return Optional.empty();
        }
        Matcher valueMatcher = Pattern.compile("@(?:[A-Za-z_][\\w.]*\\.)?Path\\s*\\(\\s*(?:value\\s*=\\s*)?\"([^\"]*)\"").matcher(snippet);
        if (valueMatcher.find()) {
            return Optional.ofNullable(valueMatcher.group(1));
        }
        Matcher bareMatcher = Pattern.compile("@(?:[A-Za-z_][\\w.]*\\.)?Path\\s*\\(\\s*\\)").matcher(snippet);
        if (bareMatcher.find()) {
            return Optional.of("/");
        }
        return Optional.empty();
    }

    private static String normalizeJaxRsPath(String value) {
        if (value == null || value.isBlank()) {
            return "/";
        }
        String normalized = value.strip();
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        normalized = normalized.replaceAll("//+", "/");
        if (normalized.length() > 1 && normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private static String normalizeJaxRsEndpointPath(String classPath, String methodPath) {
        String base = normalizeJaxRsPath(classPath);
        String method = normalizeJaxRsPath(methodPath);
        if ("/".equals(base) && "/".equals(method)) {
            return "/";
        }
        if ("/".equals(base)) {
            return method;
        }
        if ("/".equals(method)) {
            return base;
        }
        return (base.endsWith("/") ? base.substring(0, base.length() - 1) : base)
            + (method.startsWith("/") ? method : "/" + method);
    }

    private static List<Map<String, String>> extractJaxRsParameterDetails(String parameterSnippet) {
        if (parameterSnippet == null || parameterSnippet.isBlank() || "()".equals(parameterSnippet.strip())) {
            return List.of();
        }
        String inner = parameterSnippet.strip();
        if (inner.startsWith("(")) {
            inner = inner.substring(1);
        }
        if (inner.endsWith(")")) {
            inner = inner.substring(0, inner.length() - 1);
        }
        if (inner.isBlank()) {
            return List.of();
        }
        List<Map<String, String>> result = new ArrayList<>();
        for (String part : splitTopLevelCommaSeparated(inner)) {
            String snippet = part.strip();
            if (snippet.isBlank()) {
                continue;
            }
            LinkedHashMap<String, String> detail = new LinkedHashMap<>();
            detail.put("name", extractParameterName(snippet));
            detail.put("declaredType", extractParameterDeclaredType(snippet));
            detail.put("parameterKind", classifyJaxRsParameter(snippet));
            result.add(Map.copyOf(detail));
        }
        return List.copyOf(result);
    }

    private static List<String> splitTopLevelCommaSeparated(String value) {
        List<String> result = new ArrayList<>();
        if (value == null || value.isBlank()) {
            return List.of();
        }
        int depth = 0;
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch == '<' || ch == '(' || ch == '[') {
                depth++;
            } else if (ch == '>' || ch == ')' || ch == ']') {
                depth = Math.max(0, depth - 1);
            } else if (ch == ',' && depth == 0) {
                String part = current.toString().trim();
                if (!part.isEmpty()) {
                    result.add(part);
                }
                current.setLength(0);
                continue;
            }
            current.append(ch);
        }
        String tail = current.toString().trim();
        if (!tail.isEmpty()) {
            result.add(tail);
        }
        return List.copyOf(result);
    }

    private static String extractParameterName(String snippet) {
        Matcher matcher = Pattern.compile("([A-Za-z_$][\\w$]*)\\s*$").matcher(snippet);
        return matcher.find() ? matcher.group(1) : "";
    }

    private static String extractParameterDeclaredType(String snippet) {
        String value = snippet == null ? "" : snippet
            .replaceAll("@[A-Za-z_][\\w.]*\\s*(\\([^)]*\\))?", " ")
            .replaceAll("\\bfinal\\b", " ")
            .trim();
        Matcher matcher = Pattern.compile("([A-Za-z_$][\\w.$]*(?:\\s*<[^>{}]+>)?(?:\\s*\\[\\])*)\\s+[A-Za-z_$][\\w$]*$").matcher(value);
        return matcher.find() ? matcher.group(1).replaceAll("\\s+", " ").trim() : "";
    }

    private static String classifyJaxRsParameter(String snippet) {
        String lower = snippet == null ? "" : snippet.toLowerCase(Locale.ROOT);
        if (lower.contains("@pathparam")) return "PATH";
        if (lower.contains("@queryparam")) return "QUERY";
        if (lower.contains("@headerparam")) return "HEADER";
        if (lower.contains("@cookieparam")) return "COOKIE";
        if (lower.contains("@matrixparam")) return "MATRIX";
        if (lower.contains("@formparam")) return "FORM";
        if (lower.contains("@beanparam")) return "BEAN";
        if (lower.contains("@context")) return "CONTEXT";
        return "BODY";
    }

    @SuppressWarnings("unchecked")
    private static List<String> metadataStringList(Object value) {
        return JavaDeclaredTypeSupport.metadataStringList(value);
    }

    static int lineOf(SourceReference ref, SyntaxNode fallbackNode) {
        return JavaSourceReferenceSupport.lineOf(ref, fallbackNode);
    }

    private static java.util.Optional<String> importQualifiedName(String snippet) {
        return SyntaxTreeExtractionSupport.extractQualifiedName(
            snippet == null ? null : snippet.replaceFirst("^\\s*import\\s+", "").replaceFirst(";\\s*$", "")
        );
    }

    private static String derivePackageFromPath(String relativePath) {
        int marker = relativePath.indexOf("/java/");
        if (marker >= 0) {
            String candidate = relativePath.substring(marker + 6);
            int slash = candidate.lastIndexOf('/');
            if (slash > 0) {
                return candidate.substring(0, slash).replace('/', '.');
            }
        }
        int slash = relativePath.lastIndexOf('/');
        return slash > 0 ? relativePath.substring(0, slash).replace('/', '.') : "default";
    }


    final class JavaJaxRsSemantics {
void addJaxRsResourceMetadata(
        ExtractionAccumulator accumulator,
        JavaTypeContext typeContext
    ) {
        String relativePath = typeContext.extractionContext().relativePath();
        SyntaxNode typeNode = typeContext.typeNode();
        ExtractedEntityFact typeEntity = typeContext.typeEntity();
        if (typeEntity == null || typeEntity.kind() != EntityKind.CLASS || !isJaxRsResource(typeEntity)) {
            return;
        }
        String basePath = extractJaxRsPath(typeEntity.sourceRefs().isEmpty() ? (typeNode == null ? "" : typeNode.textSnippet()) : typeEntity.sourceRefs().getFirst().snippet())
            .orElse("/");
        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>(typeEntity.metadata());
        metadata.put("framework", "jax-rs");
        metadata.put("jaxRsResource", true);
        metadata.put("jaxRsBasePath", normalizeJaxRsPath(basePath));
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
        List<String> annotations = metadataStringList(methodEntity.metadata().get("annotations"));
        String httpMethod = jaxRsHttpMethod(annotations).orElse(null);
        if (httpMethod == null) {
            return;
        }
        SourceReference methodRef = methodEntity.sourceRefs().isEmpty()
            ? ExtractionSupport.sourceRef(relativePath, SyntaxTreeExtractionSupport.oneBasedLine(methodNode), methodNode.textSnippet(), Map.of("language", "java", "kind", methodNode.type()))
            : methodEntity.sourceRefs().getFirst();
        String classSnippet = ownerTypeSnippet == null ? "" : ownerTypeSnippet;
        String classPath = extractJaxRsPath(classSnippet).orElse("");
        String methodPath = extractJaxRsPath(methodRef.snippet()).orElse("");
        String resolvedPath = normalizeJaxRsEndpointPath(classPath, methodPath);
        String endpointName = httpMethod + " " + resolvedPath;
        int endpointLine = methodRef.startLine() == null ? SyntaxTreeExtractionSupport.oneBasedLine(methodNode) : methodRef.startLine();
        String endpointId = IdUtils.scopedEntityId("java-endpoint", relativePath, ownerQualifiedName + "#" + endpointName, endpointLine);
        List<Map<String, String>> parameterDetails = extractJaxRsParameterDetails(String.valueOf(methodEntity.metadata().getOrDefault("parameters", "()")));
        LinkedHashMap<String, Object> endpointMetadata = new LinkedHashMap<>();
        endpointMetadata.put("language", "java");
        endpointMetadata.put("framework", "jax-rs");
        endpointMetadata.put("httpMethod", httpMethod);
        endpointMetadata.put("path", resolvedPath);
        endpointMetadata.put("classLevelPath", normalizeJaxRsPath(classPath));
        endpointMetadata.put("methodLevelPath", normalizeJaxRsPath(methodPath));
        endpointMetadata.put("resourceQualifiedName", ownerQualifiedName);
        endpointMetadata.put("methodName", methodEntity.name());
        endpointMetadata.put("methodQualifiedName", ownerQualifiedName + "#" + methodEntity.name());
        endpointMetadata.put("parameterDetails", parameterDetails);
        endpointMetadata.put("annotations", annotations);
        accumulator.addEntity(new ExtractedEntityFact(
            endpointId,
            EntityKind.ENDPOINT,
            EntityOrigin.OBSERVED,
            endpointName,
            endpointName,
            methodEntity.scopeId(),
            List.of(methodRef),
            Map.copyOf(endpointMetadata)
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
        LinkedHashMap<String, Object> methodMetadata = new LinkedHashMap<>(methodEntity.metadata());
        methodMetadata.put("framework", "jax-rs");
        methodMetadata.put("jaxRsEndpoint", true);
        methodMetadata.put("httpMethod", httpMethod);
        methodMetadata.put("path", resolvedPath);
        methodMetadata.put("parameterDetails", parameterDetails);
        accumulator.addEntity(new ExtractedEntityFact(
            methodEntity.id(),
            methodEntity.kind(),
            methodEntity.origin(),
            methodEntity.name(),
            methodEntity.displayName(),
            methodEntity.scopeId(),
            List.of(methodRef),
            Map.copyOf(methodMetadata)
        ));
    }
    }

    final class JavaJpaSemantics {
void addJpaTypeMetadata(
        ExtractionAccumulator accumulator,
        JavaTypeContext typeContext
    ) {
        String relativePath = typeContext.extractionContext().relativePath();
        String typeSnippet = JavaTypeSemanticsFlow.typeNodeSnippet(typeContext.typeNode(), typeContext.typeEntity());
        ExtractedEntityFact typeEntity = typeContext.typeEntity();
        if (typeEntity == null || typeEntity.kind() != EntityKind.CLASS || !isJpaPersistentType(typeSnippet, typeEntity)) {
            return;
        }
        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>(typeEntity.metadata());
        metadata.put("framework", "jpa");
        String jpaKind = detectJpaTypeKind(typeSnippet, typeEntity).orElse("entity");
        metadata.put("jpaKind", jpaKind);
        metadata.put("jpaEntity", "entity".equals(jpaKind));
        metadata.put("jpaEmbeddable", "embeddable".equals(jpaKind));
        metadata.put("jpaMappedSuperclass", "mapped-superclass".equals(jpaKind));
        extractJpaTableName(typeSnippet).ifPresent(table -> metadata.put("tableName", table));
        extractJpaInheritanceStrategy(typeSnippet).ifPresent(strategy -> metadata.put("inheritanceStrategy", strategy));
        SourceReference ref = typeEntity.sourceRefs().isEmpty()
            ? ExtractionSupport.sourceRef(relativePath, 1, typeSnippet, Map.of("language", "java", "kind", "class_declaration"))
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

void addJpaFieldFacts(
        ExtractionAccumulator accumulator,
        JavaFieldContext fieldContext
    ) {
        JavaExtractionContext extractionContext = fieldContext.extractionContext();
        String relativePath = extractionContext.relativePath();
        String packageName = extractionContext.packageName();
        SyntaxNode fieldNode = fieldContext.fieldNode();
        ExtractedEntityFact fieldEntity = fieldContext.fieldEntity();
        String ownerTypeEntityId = fieldContext.ownerTypeEntityId();
        String ownerQualifiedName = fieldContext.ownerQualifiedName();
        String ownerTypeSnippet = fieldContext.ownerTypeSnippet();
        Map<String, String> importsBySimpleName = extractionContext.importsBySimpleName();
        Map<String, JavaDeclaredType> declaredTypes = extractionContext.declaredTypes();
        if (fieldEntity == null || ownerTypeEntityId == null || !isJpaPersistentType(ownerTypeSnippet, null)) {
            return;
        }
        String snippet = fieldEntity.sourceRefs().isEmpty() ? (fieldNode == null ? "" : fieldNode.textSnippet()) : fieldEntity.sourceRefs().getFirst().snippet();
        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>(fieldEntity.metadata());
        metadata.put("framework", "jpa");
        boolean changed = false;
        if (hasAnnotation(metadataStringList(fieldEntity.metadata().get("annotations")), "Id") || hasAnnotation(metadataStringList(fieldEntity.metadata().get("annotations")), "EmbeddedId")) {
            metadata.put("jpaId", true);
            changed = true;
        }
        if (hasAnnotation(metadataStringList(fieldEntity.metadata().get("annotations")), "Version")) {
            metadata.put("jpaVersion", true);
            changed = true;
        }
        if (hasAnnotation(metadataStringList(fieldEntity.metadata().get("annotations")), "Embedded") || hasAnnotation(metadataStringList(fieldEntity.metadata().get("annotations")), "EmbeddedId")) {
            metadata.put("jpaEmbedded", true);
            changed = true;
        }
        extractJpaColumnName(snippet).ifPresent(column -> {
            metadata.put("columnName", column);
        });
        if (snippet != null && snippet.contains("nullable = false")) {
            metadata.put("nullable", false);
            changed = true;
        }
        if (snippet != null && snippet.contains("nullable = true")) {
            metadata.put("nullable", true);
            changed = true;
        }
        if (snippet != null && snippet.contains("unique = true")) {
            metadata.put("unique", true);
            changed = true;
        }
        if (extractJpaColumnName(snippet).isPresent()) {
            changed = true;
        }
        Optional<String> association = detectJpaAssociation(snippet);
        if (association.isPresent()) {
            String associationKind = association.get();
            metadata.put("jpaAssociation", associationKind);
            extractJpaMappedBy(snippet).ifPresent(mappedBy -> metadata.put("mappedBy", mappedBy));
            extractJpaJoinColumn(snippet).ifPresent(joinColumn -> metadata.put("joinColumn", joinColumn));
            extractJpaJoinTable(snippet).ifPresent(joinTable -> metadata.put("joinTable", joinTable));
            changed = true;

            String declaredType = String.valueOf(fieldEntity.metadata().getOrDefault("declaredType", ""));
            List<String> referencedTypes = extractReferencedTypes(declaredType);
            if (!referencedTypes.isEmpty()) {
                ResolvedJavaType target = resolveJavaTypeReference(
                    accumulator,
                    referencedTypes.getLast(),
                    EntityKind.CLASS,
                    relativePath,
                    packageName,
                    lineOf(fieldEntity.sourceRefs().isEmpty() ? null : fieldEntity.sourceRefs().getFirst(), fieldNode),
                    importsBySimpleName,
                    declaredTypes
                );
                if (target != null && !ownerTypeEntityId.equals(target.entityId())) {
                    SourceReference ref = fieldEntity.sourceRefs().isEmpty()
                        ? ExtractionSupport.sourceRef(relativePath, SyntaxTreeExtractionSupport.oneBasedLine(fieldNode), snippet, Map.of("language", "java", "kind", "field_declaration"))
                        : fieldEntity.sourceRefs().getFirst();
                    LinkedHashMap<String, Object> relationshipMetadata = new LinkedHashMap<>();
                    relationshipMetadata.put("framework", "jpa");
                    relationshipMetadata.put("relationshipType", "hasAssociation");
                    relationshipMetadata.put("jpaAssociation", associationKind);
                    extractJpaMappedBy(snippet).ifPresent(mappedBy -> relationshipMetadata.put("mappedBy", mappedBy));
                    extractJpaJoinColumn(snippet).ifPresent(joinColumn -> relationshipMetadata.put("joinColumn", joinColumn));
                    extractJpaJoinTable(snippet).ifPresent(joinTable -> relationshipMetadata.put("joinTable", joinTable));
                    relationshipMetadata.put("ownerQualifiedName", ownerQualifiedName == null ? "" : ownerQualifiedName);
                    accumulator.addRelationship(ExtractionSupport.dependencyRelationship(
                        ownerTypeEntityId,
                        target.entityId(),
                        target.label(),
                        ref,
                        "java",
                        Map.copyOf(relationshipMetadata)
                    ));
                }
            }
        }
        if (Boolean.TRUE.equals(metadata.get("jpaEmbedded"))) {
            String declaredType = String.valueOf(fieldEntity.metadata().getOrDefault("declaredType", ""));
            List<String> referencedTypes = extractReferencedTypes(declaredType);
            if (!referencedTypes.isEmpty()) {
                ResolvedJavaType target = resolveJavaTypeReference(
                    accumulator,
                    referencedTypes.getLast(),
                    EntityKind.CLASS,
                    relativePath,
                    packageName,
                    lineOf(fieldEntity.sourceRefs().isEmpty() ? null : fieldEntity.sourceRefs().getFirst(), fieldNode),
                    importsBySimpleName,
                    declaredTypes
                );
                if (target != null && !ownerTypeEntityId.equals(target.entityId())) {
                    SourceReference ref = fieldEntity.sourceRefs().isEmpty()
                        ? ExtractionSupport.sourceRef(relativePath, SyntaxTreeExtractionSupport.oneBasedLine(fieldNode), snippet, Map.of("language", "java", "kind", "field_declaration"))
                        : fieldEntity.sourceRefs().getFirst();
                    accumulator.addRelationship(ExtractionSupport.typedRelationship(
                        info.isaksson.erland.architecturebrowser.indexer.ir.model.RelationshipKind.DEPENDS_ON,
                        "embeds-jpa-type",
                        ownerTypeEntityId,
                        target.entityId(),
                        target.label(),
                        ref,
                        "java",
                        Map.of("framework", "jpa", "relationshipType", "embeds", "ownerQualifiedName", ownerQualifiedName == null ? "" : ownerQualifiedName)
                    ));
                }
            }
        }
        if (changed) {
            SourceReference ref = fieldEntity.sourceRefs().isEmpty()
                ? ExtractionSupport.sourceRef(relativePath, SyntaxTreeExtractionSupport.oneBasedLine(fieldNode), snippet, Map.of("language", "java", "kind", "field_declaration"))
                : fieldEntity.sourceRefs().getFirst();
            accumulator.addEntity(new ExtractedEntityFact(
                fieldEntity.id(),
                fieldEntity.kind(),
                fieldEntity.origin(),
                fieldEntity.name(),
                fieldEntity.displayName(),
                fieldEntity.scopeId(),
                List.of(ref),
                Map.copyOf(metadata)
            ));
        }
    }

void addJpaMethodFacts(
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
        SourceReference ref = methodContext.sourceRef();
        String snippet = methodContext.snippet();
        Map<String, String> importsBySimpleName = extractionContext.importsBySimpleName();
        Map<String, JavaDeclaredType> declaredTypes = extractionContext.declaredTypes();
        if (methodEntity == null || ownerTypeEntityId == null || !isJpaPersistentType(ownerTypeSnippet, null) || isConstructor(methodEntity)) {
            return;
        }
        List<String> annotations = metadataStringList(methodEntity.metadata().get("annotations"));
        if (methodNode != null) {
            java.util.LinkedHashSet<String> mergedAnnotations = new java.util.LinkedHashSet<>(annotations);
            SyntaxTreeExtractionSupport.descendantsByType(methodNode, Set.of("marker_annotation", "annotation")).stream()
                .flatMap(annotationNode -> SyntaxTreeExtractionSupport.extractAnnotationsFromSnippet(annotationNode.textSnippet()).stream())
                .forEach(mergedAnnotations::add);
            SyntaxTreeExtractionSupport.extractAnnotationsFromSnippet(methodNode.textSnippet()).forEach(mergedAnnotations::add);
            annotations = List.copyOf(mergedAnnotations);
        }
        String parameters = String.valueOf(methodEntity.metadata().getOrDefault("parameters", ""));
        String methodName = methodEntity.name();
        if (!annotations.stream().anyMatch(a -> a != null && !a.isBlank()) && !containsJpaPropertyAnnotation(snippet)) {
            return;
        }
        String propertyName = deriveJavaPropertyName(methodName, parameters);
        if (propertyName == null || propertyName.isBlank()) {
            return;
        }
        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>(methodEntity.metadata());
        metadata.put("framework", "jpa");
        metadata.put("jpaPropertyAccess", true);
        metadata.put("jpaPropertyName", propertyName);
        boolean changed = true;
        if (hasAnnotation(annotations, "Id") || hasAnnotation(annotations, "EmbeddedId")) {
            metadata.put("jpaId", true);
            changed = true;
        }
        if (hasAnnotation(annotations, "Version")) {
            metadata.put("jpaVersion", true);
            changed = true;
        }
        if (hasAnnotation(annotations, "Embedded") || hasAnnotation(annotations, "EmbeddedId")) {
            metadata.put("jpaEmbedded", true);
            changed = true;
        }
        extractJpaColumnName(snippet).ifPresent(column -> metadata.put("columnName", column));
        if (snippet != null && snippet.contains("nullable = false")) {
            metadata.put("nullable", false);
            changed = true;
        }
        if (snippet != null && snippet.contains("nullable = true")) {
            metadata.put("nullable", true);
            changed = true;
        }
        if (snippet != null && snippet.contains("unique = true")) {
            metadata.put("unique", true);
            changed = true;
        }
        if (extractJpaColumnName(snippet).isPresent()) {
            changed = true;
        }
        Optional<String> association = detectJpaAssociation(annotations, snippet);
        if (association.isEmpty()) {
            String loweredSnippet = snippet == null ? "" : snippet.toLowerCase(java.util.Locale.ROOT);
            if (loweredSnippet.contains("manytoone") || loweredSnippet.contains("many_to_one") || loweredSnippet.contains("many-to-one")) {
                association = Optional.of("many-to-one");
            } else if (loweredSnippet.contains("onetomany") || loweredSnippet.contains("one_to_many") || loweredSnippet.contains("one-to-many")) {
                association = Optional.of("one-to-many");
            } else if (loweredSnippet.contains("onetoone") || loweredSnippet.contains("one_to_one") || loweredSnippet.contains("one-to-one")) {
                association = Optional.of("one-to-one");
            } else if (loweredSnippet.contains("manytomany") || loweredSnippet.contains("many_to_many") || loweredSnippet.contains("many-to-many")) {
                association = Optional.of("many-to-many");
            }
        }
        String declaredType = String.valueOf(methodEntity.metadata().getOrDefault("returnType", ""));
        if (declaredType == null || declaredType.isBlank()) {
            declaredType = inferJavaMethodReturnTypeFromSnippet(snippet, methodName).orElse("");
        }
        if (association.isPresent()) {
            String associationKind = association.get();
            metadata.put("jpaAssociation", associationKind);
            extractJpaMappedBy(snippet).ifPresent(mappedBy -> metadata.put("mappedBy", mappedBy));
            extractJpaJoinColumn(snippet).ifPresent(joinColumn -> metadata.put("joinColumn", joinColumn));
            extractJpaJoinTable(snippet).ifPresent(joinTable -> metadata.put("joinTable", joinTable));
            changed = true;
            List<String> referencedTypes = extractReferencedTypes(declaredType);
            if (!referencedTypes.isEmpty()) {
                ResolvedJavaType target = resolveJavaTypeReference(
                    accumulator,
                    referencedTypes.getLast(),
                    EntityKind.CLASS,
                    relativePath,
                    packageName,
                    lineOf(methodEntity.sourceRefs().isEmpty() ? null : methodEntity.sourceRefs().getFirst(), methodNode),
                    importsBySimpleName,
                    declaredTypes
                );
                if (target != null && !ownerTypeEntityId.equals(target.entityId())) {
                    LinkedHashMap<String, Object> relationshipMetadata = new LinkedHashMap<>();
                    relationshipMetadata.put("framework", "jpa");
                    relationshipMetadata.put("relationshipType", "hasAssociation");
                    relationshipMetadata.put("jpaAssociation", associationKind);
                    extractJpaMappedBy(snippet).ifPresent(mappedBy -> relationshipMetadata.put("mappedBy", mappedBy));
                    extractJpaJoinColumn(snippet).ifPresent(joinColumn -> relationshipMetadata.put("joinColumn", joinColumn));
                    extractJpaJoinTable(snippet).ifPresent(joinTable -> relationshipMetadata.put("joinTable", joinTable));
                    relationshipMetadata.put("ownerQualifiedName", ownerQualifiedName == null ? "" : ownerQualifiedName);
                    relationshipMetadata.put("ownerMemberKind", "method");
                    relationshipMetadata.put("ownerMemberName", methodName);
                    relationshipMetadata.put("ownerPropertyName", propertyName);
                    accumulator.addRelationship(ExtractionSupport.dependencyRelationship(
                        ownerTypeEntityId,
                        target.entityId(),
                        target.label(),
                        ref,
                        "java",
                        Map.copyOf(relationshipMetadata)
                    ));
                }
            }
        }
        if (Boolean.TRUE.equals(metadata.get("jpaEmbedded"))) {
            List<String> referencedTypes = extractReferencedTypes(declaredType);
            if (!referencedTypes.isEmpty()) {
                ResolvedJavaType target = resolveJavaTypeReference(
                    accumulator,
                    referencedTypes.getLast(),
                    EntityKind.CLASS,
                    relativePath,
                    packageName,
                    lineOf(methodEntity.sourceRefs().isEmpty() ? null : methodEntity.sourceRefs().getFirst(), methodNode),
                    importsBySimpleName,
                    declaredTypes
                );
                if (target != null && !ownerTypeEntityId.equals(target.entityId())) {
                    accumulator.addRelationship(ExtractionSupport.typedRelationship(
                        info.isaksson.erland.architecturebrowser.indexer.ir.model.RelationshipKind.DEPENDS_ON,
                        "embeds-jpa-property-type",
                        ownerTypeEntityId,
                        target.entityId(),
                        target.label(),
                        ref,
                        "java",
                        Map.of(
                            "framework", "jpa",
                            "relationshipType", "embeds",
                            "ownerQualifiedName", ownerQualifiedName == null ? "" : ownerQualifiedName,
                            "ownerMemberKind", "method",
                            "ownerMemberName", methodName,
                            "ownerPropertyName", propertyName
                        )
                    ));
                }
            }
        }
        if (changed) {
            accumulator.addEntity(new ExtractedEntityFact(
                methodEntity.id(),
                methodEntity.kind(),
                methodEntity.origin(),
                methodEntity.name(),
                methodEntity.displayName(),
                methodEntity.scopeId(),
                List.of(ref),
                Map.copyOf(metadata)
            ));
        }
    }

void addJpaInheritanceFacts(
        ExtractionAccumulator accumulator,
        String relativePath,
        String packageName,
        SyntaxNode typeNode,
        ExtractedEntityFact typeEntity,
        Map<String, String> importsBySimpleName,
        Map<String, JavaDeclaredType> declaredTypes
    ) {
        String typeSnippet = JavaTypeSemanticsFlow.typeNodeSnippet(typeNode, typeEntity);
        if (typeEntity == null || !isJpaPersistentType(typeSnippet, typeEntity)) {
            return;
        }
        int line = SyntaxTreeExtractionSupport.oneBasedLine(typeNode);
        SourceReference ref = ExtractionSupport.sourceRef(relativePath, line, typeSnippet, Map.of("language", "java", "kind", typeNode.type()));
        for (String parentType : extractExtendedTypes(typeNode)) {
            ResolvedJavaType resolved = resolveJavaTypeReference(accumulator, parentType, EntityKind.CLASS, relativePath, packageName, line, importsBySimpleName, declaredTypes);
            if (resolved == null || typeEntity.id().equals(resolved.entityId())) {
                continue;
            }
            accumulator.addRelationship(ExtractionSupport.typedRelationship(
                info.isaksson.erland.architecturebrowser.indexer.ir.model.RelationshipKind.EXTENDS,
                "inherits-persistence-model",
                typeEntity.id(),
                resolved.entityId(),
                resolved.label(),
                ref,
                "java",
                Map.of("framework", "jpa", "relationshipType", "inheritsPersistenceModel")
            ));
        }
    }
    }

    final class JavaCdiSemantics {
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
        String exactMethodSnippet = exactNodeSnippet(sourceText, methodNode);
        if (exactMethodSnippet != null && !exactMethodSnippet.isBlank()) {
            snippet = exactMethodSnippet;
        }

        LinkedHashMap<String, Object> methodMetadata = new LinkedHashMap<>(methodEntity.metadata());
        boolean methodChanged = false;

        for (PublishedCdiEvent publication : detectCdiPublishedEvents(snippet, ownerTypeSnippet)) {
            ResolvedJavaType target = resolveJavaTypeReference(
                accumulator,
                publication.eventType(),
                EntityKind.CLASS,
                relativePath,
                packageName,
                lineOf(ref, methodNode),
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

        Optional<ObservedCdiEvent> observer = detectCdiObservedEvent(methodEntity, snippet);
        if (observer.isPresent()) {
            ObservedCdiEvent observed = observer.get();
            ResolvedJavaType target = resolveJavaTypeReference(
                accumulator,
                observed.eventType(),
                EntityKind.CLASS,
                relativePath,
                packageName,
                lineOf(ref, methodNode),
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

    final class JavaWritePathSemantics {
void addWritePathFacts(
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
        SourceReference ref = methodContext.sourceRef();
        String snippet = methodContext.snippet();
        Map<String, String> importsBySimpleName = extractionContext.importsBySimpleName();
        Map<String, JavaDeclaredType> declaredTypes = extractionContext.declaredTypes();
        if (methodEntity == null || ownerTypeEntityId == null) {
            return;
        }

        List<DetectedWritePath> detections = new ArrayList<>();
        detections.addAll(detectJpaWriteOperations(methodEntity, snippet));
        detections.addAll(detectRepositoryWriteOperations(methodEntity, snippet));
        if (detections.isEmpty()) {
            return;
        }

        Map<String, String> variableTypes = collectMethodVariableTypes(methodEntity, snippet);
        LinkedHashMap<String, Object> methodMetadata = new LinkedHashMap<>(methodEntity.metadata());
        java.util.LinkedHashSet<String> writeOperations = new java.util.LinkedHashSet<>(metadataStringList(methodMetadata.get("writeOperations")));
        java.util.LinkedHashSet<String> writeTargets = new java.util.LinkedHashSet<>(metadataStringList(methodMetadata.get("writeEntityTypes")));
        boolean changed = false;

        for (DetectedWritePath detection : detections) {
            String entityType = resolveWriteTargetEntityType(detection.argumentExpression(), variableTypes).orElse(null);
            if (entityType == null || entityType.isBlank()) {
                continue;
            }
            ResolvedJavaType target = resolveJavaTypeReference(
                accumulator,
                entityType,
                EntityKind.CLASS,
                relativePath,
                packageName,
                lineOf(ref, methodNode),
                importsBySimpleName,
                declaredTypes
            );
            if (target == null) {
                continue;
            }
            LinkedHashMap<String, Object> relationshipMetadata = new LinkedHashMap<>();
            relationshipMetadata.put("framework", "jpa");
            relationshipMetadata.put("relationshipType", "writePath");
            relationshipMetadata.put("writeOperation", detection.operation());
            relationshipMetadata.put("writeKind", detection.writeKind());
            relationshipMetadata.put("writerMethod", methodEntity.name());
            relationshipMetadata.put("writerQualifiedName", ownerQualifiedName == null ? "" : ownerQualifiedName);
            relationshipMetadata.put("entityType", target.label());
            if (detection.viaField() != null && !detection.viaField().isBlank()) {
                relationshipMetadata.put("writeViaField", detection.viaField());
            }
            if (detection.viaType() != null && !detection.viaType().isBlank()) {
                relationshipMetadata.put("writeViaType", detection.viaType());
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
            writeOperations.add(detection.operation());
            writeTargets.add(target.label());
            changed = true;
        }

        if (changed) {
            methodMetadata.put("writePath", true);
            methodMetadata.put("writeOperations", List.copyOf(writeOperations));
            methodMetadata.put("writeEntityTypes", List.copyOf(writeTargets));
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

}
