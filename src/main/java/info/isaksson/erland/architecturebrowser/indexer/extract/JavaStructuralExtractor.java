package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityOrigin;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.SourceReference;
import info.isaksson.erland.architecturebrowser.indexer.parse.ParseLanguage;
import info.isaksson.erland.architecturebrowser.indexer.parse.SourceParseResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class JavaStructuralExtractor implements StructuralExtractor {
    private static final Pattern PACKAGE_PATTERN = Pattern.compile("(?m)^\\s*package\\s+([a-zA-Z_][\\w.]*)\\s*;");
    private static final Pattern IMPORT_PATTERN = Pattern.compile("(?m)^\\s*import\\s+(static\\s+)?([a-zA-Z_][\\w.*]*)\\s*;");
    private static final Pattern TYPE_PATTERN = Pattern.compile(
        "(?m)^\\s*(?:@[\\w.]+(?:\\([^)]*\\))?\\s*)*" +
            "(?:(?:public|protected|private|abstract|final|sealed|non-sealed|static|strictfp)\\s+)*" +
            "(class|interface|enum|record)\\s+([A-Za-z_][A-Za-z0-9_]*)"
    );
    private static final Pattern METHOD_PATTERN = Pattern.compile(
        "(?m)^\\s*(?:@[\\w.]+(?:\\([^)]*\\))?\\s*)*" +
            "(?:(?:public|protected|private|abstract|final|static|synchronized|native|default|strictfp)\\s+)+" +
            "[\\w<>, ?\\[\\].@]+\\s+([a-zA-Z_][A-Za-z0-9_]*)\\s*\\(([^;{}]*)\\)\\s*(?:throws\\s+[^\\{]+)?\\{"
    );
    private static final Pattern ANNOTATION_PATTERN = Pattern.compile("@([A-Za-z_][\\w.]*)");

    @Override
    public ParseLanguage language() {
        return ParseLanguage.JAVA;
    }

    @Override
    public ExtractionAccumulator extract(SourceParseResult parseResult, ExtractionAccumulator accumulator) {
        accumulator.incrementFilesVisited();
        String relativePath = parseResult.request().relativePath();
        String source = parseResult.request().sourceText();
        if (source == null) {
            accumulator.addDiagnostic(ExtractionSupport.extractionWarning(parseResult, "extract.java.missing-source", "Java source text was not available for extraction"));
            return accumulator;
        }
        accumulator.incrementFilesExtracted("java");

        String repositoryScopeId = "scope:repo";
        var fileScope = ExtractionSupport.fileScope(repositoryScopeId, relativePath);
        accumulator.addScope(fileScope);
        String packageName = matchFirst(PACKAGE_PATTERN, source, 1).orElse(derivePackageFromPath(relativePath));
        var packageScope = ExtractionSupport.packageScope(repositoryScopeId, packageName, relativePath, "java");
        accumulator.addScope(packageScope);

        var fileEntity = ExtractionSupport.fileModuleEntity(fileScope.id(), relativePath, "java");
        accumulator.addEntity(fileEntity);

        Matcher importMatcher = IMPORT_PATTERN.matcher(source);
        while (importMatcher.find()) {
            String imported = importMatcher.group(2);
            int line = lineOf(source, importMatcher.start());
            var external = ExtractionSupport.externalDependencyEntity("java", imported, relativePath, line);
            accumulator.addEntity(external);
            accumulator.addRelationship(ExtractionSupport.dependencyRelationship(
                fileEntity.id(), external.id(), imported,
                ExtractionSupport.sourceRef(relativePath, line, importMatcher.group().trim(), Map.of("language", "java", "kind", "import")),
                "java"
            ));
        }

        List<String> topAnnotations = collectTopAnnotations(source);
        Matcher typeMatcher = TYPE_PATTERN.matcher(source);
        while (typeMatcher.find()) {
            String kindKeyword = typeMatcher.group(1);
            String typeName = typeMatcher.group(2);
            int line = lineOf(source, typeMatcher.start(2));
            EntityKind kind = "interface".equals(kindKeyword) ? EntityKind.INTERFACE : EntityKind.CLASS;
            String qualifiedName = packageName == null || packageName.isBlank() ? typeName : packageName + "." + typeName;
            SourceReference ref = ExtractionSupport.sourceRef(relativePath, line, typeMatcher.group().trim(), Map.of("language", "java", "kind", kindKeyword));
            ExtractedEntityFact entity = new ExtractedEntityFact(
                IdUtils.scopedEntityId("java", relativePath, qualifiedName, line),
                kind,
                EntityOrigin.OBSERVED,
                typeName,
                qualifiedName,
                packageScope.id(),
                List.of(ref),
                Map.of(
                    "language", "java",
                    "qualifiedName", qualifiedName,
                    "packageName", packageName,
                    "annotations", topAnnotations,
                    "parseStatus", parseResult.status().name()
                )
            );
            accumulator.addEntity(entity);
            accumulator.addRelationship(ExtractionSupport.containsRelationship(fileEntity.id(), entity.id(), ref));
        }

        Matcher methodMatcher = METHOD_PATTERN.matcher(source);
        while (methodMatcher.find()) {
            String methodName = methodMatcher.group(1);
            int line = lineOf(source, methodMatcher.start(1));
            SourceReference ref = ExtractionSupport.sourceRef(relativePath, line, methodMatcher.group().trim(), Map.of("language", "java", "kind", "method"));
            ExtractedEntityFact entity = new ExtractedEntityFact(
                IdUtils.scopedEntityId("java", relativePath, methodName, line),
                EntityKind.FUNCTION,
                EntityOrigin.OBSERVED,
                methodName,
                methodName + "()",
                fileScope.id(),
                List.of(ref),
                Map.of(
                    "language", "java",
                    "parameters", methodMatcher.group(2).trim(),
                    "annotations", annotationsNearLine(source, line),
                    "parseStatus", parseResult.status().name()
                )
            );
            accumulator.addEntity(entity);
            accumulator.addRelationship(ExtractionSupport.containsRelationship(fileEntity.id(), entity.id(), ref));
        }
        return accumulator;
    }

    private static Optional<String> matchFirst(Pattern pattern, String source, int group) {
        Matcher matcher = pattern.matcher(source);
        return matcher.find() ? Optional.ofNullable(matcher.group(group)) : Optional.empty();
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

    private static int lineOf(String source, int index) {
        return 1 + (int) source.substring(0, Math.max(0, index)).chars().filter(ch -> ch == '\n').count();
    }

    private static List<String> collectTopAnnotations(String source) {
        List<String> result = new ArrayList<>();
        Matcher matcher = ANNOTATION_PATTERN.matcher(source);
        while (matcher.find()) {
            result.add(matcher.group(1));
            if (result.size() >= 10) {
                break;
            }
        }
        return List.copyOf(result);
    }

    private static List<String> annotationsNearLine(String source, int line) {
        String[] lines = source.split("\\R");
        List<String> result = new ArrayList<>();
        for (int i = Math.max(0, line - 3); i < Math.min(lines.length, line); i++) {
            Matcher matcher = ANNOTATION_PATTERN.matcher(lines[i]);
            while (matcher.find()) {
                result.add(matcher.group(1));
            }
        }
        return List.copyOf(result);
    }
}
