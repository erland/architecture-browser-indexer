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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class TypeScriptStructuralExtractor implements StructuralExtractor {
    private static final Pattern IMPORT_PATTERN = Pattern.compile("(?m)^\\s*import\\s+(?:type\\s+)?(?:.+?)\\s+from\\s+['\"]([^'\"]+)['\"]\\s*;?");
    private static final Pattern CLASS_PATTERN = Pattern.compile("(?m)^\\s*(?:export\\s+)?(?:abstract\\s+)?class\\s+([A-Za-z_][A-Za-z0-9_]*)");
    private static final Pattern INTERFACE_PATTERN = Pattern.compile("(?m)^\\s*(?:export\\s+)?interface\\s+([A-Za-z_][A-Za-z0-9_]*)");
    private static final Pattern FUNCTION_PATTERN = Pattern.compile("(?m)^\\s*(?:export\\s+)?function\\s+([A-Za-z_][A-Za-z0-9_]*)\\s*\\(");
    private static final Pattern CONST_FUNCTION_PATTERN = Pattern.compile("(?m)^\\s*(?:export\\s+)?const\\s+([A-Za-z_][A-Za-z0-9_]*)\\s*=\\s*(?:async\\s*)?(?:\\([^)]*\\)|[A-Za-z_][A-Za-z0-9_]*)\\s*=>");
    private static final Pattern DECORATOR_PATTERN = Pattern.compile("(?m)^\\s*@([A-Za-z_][A-Za-z0-9_]*)");

    @Override
    public ParseLanguage language() {
        return ParseLanguage.TYPESCRIPT;
    }

    @Override
    public ExtractionAccumulator extract(SourceParseResult parseResult, ExtractionAccumulator accumulator) {
        accumulator.incrementFilesVisited();
        String relativePath = parseResult.request().relativePath();
        String source = parseResult.request().sourceText();
        if (source == null) {
            accumulator.addDiagnostic(ExtractionSupport.extractionWarning(parseResult, "extract.typescript.missing-source", "TypeScript source text was not available for extraction"));
            return accumulator;
        }
        accumulator.incrementFilesExtracted("typescript");

        String repositoryScopeId = "scope:repo";
        var fileScope = ExtractionSupport.fileScope(repositoryScopeId, relativePath);
        accumulator.addScope(fileScope);
        var fileEntity = ExtractionSupport.fileModuleEntity(fileScope.id(), relativePath, "typescript");
        accumulator.addEntity(fileEntity);

        Matcher importMatcher = IMPORT_PATTERN.matcher(source);
        while (importMatcher.find()) {
            String imported = importMatcher.group(1);
            int line = lineOf(source, importMatcher.start(1));
            var external = ExtractionSupport.externalDependencyEntity("typescript", imported, relativePath, line);
            accumulator.addEntity(external);
            accumulator.addRelationship(ExtractionSupport.dependencyRelationship(
                fileEntity.id(), external.id(), imported,
                ExtractionSupport.sourceRef(relativePath, line, importMatcher.group().trim(), Map.of("language", "typescript", "kind", "import")),
                "typescript"
            ));
        }

        extractNamedEntities(source, relativePath, parseResult, accumulator, fileEntity.id(), CLASS_PATTERN, EntityKind.CLASS, "class");
        extractNamedEntities(source, relativePath, parseResult, accumulator, fileEntity.id(), INTERFACE_PATTERN, EntityKind.INTERFACE, "interface");
        extractNamedEntities(source, relativePath, parseResult, accumulator, fileEntity.id(), FUNCTION_PATTERN, EntityKind.FUNCTION, "function");
        extractNamedEntities(source, relativePath, parseResult, accumulator, fileEntity.id(), CONST_FUNCTION_PATTERN, EntityKind.FUNCTION, "arrow-function");
        return accumulator;
    }

    private static void extractNamedEntities(
        String source,
        String relativePath,
        SourceParseResult parseResult,
        ExtractionAccumulator accumulator,
        String fileEntityId,
        Pattern pattern,
        EntityKind kind,
        String matchedKind
    ) {
        Matcher matcher = pattern.matcher(source);
        while (matcher.find()) {
            String name = matcher.group(1);
            int line = lineOf(source, matcher.start(1));
            SourceReference ref = ExtractionSupport.sourceRef(relativePath, line, matcher.group().trim(), Map.of("language", "typescript", "kind", matchedKind));
            ExtractedEntityFact entity = new ExtractedEntityFact(
                IdUtils.scopedEntityId("typescript", relativePath, name, line),
                kind,
                EntityOrigin.OBSERVED,
                name,
                relativePath + "#" + name,
                IdUtils.scopeId("file", relativePath),
                List.of(ref),
                Map.of(
                    "language", "typescript",
                    "decorators", decoratorsNearLine(source, line),
                    "parseStatus", parseResult.status().name()
                )
            );
            accumulator.addEntity(entity);
            accumulator.addRelationship(ExtractionSupport.containsRelationship(fileEntityId, entity.id(), ref));
        }
    }

    private static int lineOf(String source, int index) {
        return 1 + (int) source.substring(0, Math.max(0, index)).chars().filter(ch -> ch == '\n').count();
    }

    private static List<String> decoratorsNearLine(String source, int line) {
        String[] lines = source.split("\\R");
        List<String> result = new ArrayList<>();
        for (int i = Math.max(0, line - 3); i < Math.min(lines.length, line); i++) {
            Matcher matcher = DECORATOR_PATTERN.matcher(lines[i]);
            while (matcher.find()) {
                result.add(matcher.group(1));
            }
        }
        return List.copyOf(result);
    }
}
