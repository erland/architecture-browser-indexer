package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact;
import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractionMode;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityOrigin;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.SourceReference;
import info.isaksson.erland.architecturebrowser.indexer.parse.ParseLanguage;
import info.isaksson.erland.architecturebrowser.indexer.parse.SourceParseResult;
import info.isaksson.erland.architecturebrowser.indexer.parse.SyntaxNode;
import info.isaksson.erland.architecturebrowser.indexer.parse.SyntaxTree;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class SqlStructuralExtractor implements StructuralExtractor {
    private static final Pattern CREATE_TABLE_PATTERN = Pattern.compile("(?i)\\bcreate\\s+table\\s+(?:if\\s+not\\s+exists\\s+)?([A-Za-z_][\\w.$]*)");
    private static final Pattern INTO_TABLE_PATTERN = Pattern.compile("(?i)\\b(?:insert\\s+into|update|delete\\s+from|truncate\\s+table)\\s+([A-Za-z_][\\w.$]*)");
    private static final Pattern FROM_TABLE_PATTERN = Pattern.compile("(?i)\\b(?:from|join)\\s+([A-Za-z_][\\w.$]*)");

    @Override
    public ParseLanguage language() {
        return ParseLanguage.SQL;
    }

    @Override
    public ExtractionAccumulator extract(SourceParseResult parseResult, ExtractionAccumulator accumulator) {
        accumulator.incrementFilesVisited();
        if (!parseResult.successful() || parseResult.syntaxTree() == null) {
            accumulator.addDiagnostic(ExtractionSupport.extractionWarning(
                parseResult,
                "extract.sql.syntax-tree-required",
                "SQL structural extraction requires a successful Tree-sitter syntax tree."
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
        accumulator.incrementFilesExtracted("sql", extractionMode);

        String repositoryScopeId = "scope:repo";
        var fileScope = ExtractionSupport.fileScope(repositoryScopeId, relativePath);
        accumulator.addScope(fileScope);
        var fileEntity = ExtractionSupport.fileModuleEntity(fileScope.id(), relativePath, "sql");
        accumulator.addEntity(fileEntity);

        Set<String> seenDefinitions = new LinkedHashSet<>();
        Set<String> seenReferences = new LinkedHashSet<>();

        for (SyntaxNode node : SyntaxTreeExtractionSupport.descendantsByType(syntaxTree.root(), Set.of(
            "statement",
            "create_table_statement",
            "insert_statement",
            "update_statement",
            "delete_statement",
            "select_statement",
            "truncate_statement"
        ))) {
            String snippet = node.textSnippet();
            if (snippet == null || snippet.isBlank()) {
                continue;
            }
            int line = node.startLine() + 1;
            SourceReference ref = ExtractionSupport.sourceRef(relativePath, line, snippet, Map.of("language", "sql", "kind", node.type()));

            String createTable = firstMatch(CREATE_TABLE_PATTERN, snippet);
            if (createTable != null && seenDefinitions.add(createTable.toLowerCase())) {
                ExtractedEntityFact table = sqlTableEntity(createTable, relativePath, line, ref, parseResult, extractionMode, EntityOrigin.OBSERVED, fileScope.id());
                accumulator.addEntity(table);
                accumulator.addRelationship(ExtractionSupport.containsRelationship(fileEntity.id(), table.id(), ref));
            }

            for (String tableName : referencedTables(snippet)) {
                if (!seenReferences.add((line + ":" + tableName).toLowerCase())) {
                    continue;
                }
                ExtractedEntityFact referenced = sqlTableEntity(tableName, relativePath, line, ref, parseResult, extractionMode, EntityOrigin.INFERRED, null);
                accumulator.addEntity(referenced);
                accumulator.addRelationship(ExtractionSupport.dependencyRelationship(
                    fileEntity.id(),
                    referenced.id(),
                    tableName,
                    ref,
                    "sql"
                ));
            }
        }
        return accumulator;
    }

    private static ExtractedEntityFact sqlTableEntity(
        String tableName,
        String relativePath,
        int line,
        SourceReference ref,
        SourceParseResult parseResult,
        ExtractionMode extractionMode,
        EntityOrigin origin,
        String scopeId
    ) {
        return new ExtractedEntityFact(
            IdUtils.externalEntityId("sql-table", tableName),
            EntityKind.DATASTORE,
            origin,
            tableName,
            tableName,
            scopeId,
            List.of(ref),
            Map.of(
                "language", "sql",
                "tableName", tableName,
                "parseStatus", parseResult.status().name(),
                "extractionMode", extractionMode.name()
            )
        );
    }

    private static List<String> referencedTables(String snippet) {
        Set<String> names = new LinkedHashSet<>();
        addMatches(names, INTO_TABLE_PATTERN, snippet);
        addMatches(names, FROM_TABLE_PATTERN, snippet);
        return List.copyOf(names);
    }

    private static void addMatches(Set<String> names, Pattern pattern, String snippet) {
        Matcher matcher = pattern.matcher(snippet);
        while (matcher.find()) {
            String candidate = matcher.group(1);
            if (candidate != null && !candidate.isBlank()) {
                names.add(candidate);
            }
        }
    }

    private static String firstMatch(Pattern pattern, String snippet) {
        Matcher matcher = pattern.matcher(snippet);
        return matcher.find() ? matcher.group(1) : null;
    }
}
