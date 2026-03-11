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

final class ConfigStructuralExtractor implements StructuralExtractor {
    private final ParseLanguage language;

    ConfigStructuralExtractor(ParseLanguage language) {
        this.language = language;
    }

    @Override
    public ParseLanguage language() {
        return language;
    }

    @Override
    public ExtractionAccumulator extract(SourceParseResult parseResult, ExtractionAccumulator accumulator) {
        accumulator.incrementFilesVisited();
        if (!parseResult.successful() || parseResult.syntaxTree() == null) {
            accumulator.addDiagnostic(ExtractionSupport.extractionWarning(
                parseResult,
                "extract." + language.inventoryKey() + ".syntax-tree-required",
                language.inventoryKey() + " structural extraction requires a successful Tree-sitter syntax tree."
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
        String languageKey = language.inventoryKey();
        accumulator.incrementFilesExtracted(languageKey, extractionMode);

        String repositoryScopeId = "scope:repo";
        var fileScope = ExtractionSupport.fileScope(repositoryScopeId, relativePath);
        accumulator.addScope(fileScope);
        var fileEntity = ExtractionSupport.fileModuleEntity(fileScope.id(), relativePath, languageKey);
        accumulator.addEntity(fileEntity);

        Set<String> seenConfigKeys = new LinkedHashSet<>();
        for (SyntaxNode node : SyntaxTreeExtractionSupport.descendantsByType(syntaxTree.root(), candidateNodeTypes())) {
            ConfigExtractionSupport.parseCandidate(relativePath, languageKey, node).ifPresent(candidate -> {
                if (!seenConfigKeys.add(candidate.key() + "@" + candidate.line())) {
                    return;
                }
                SourceReference ref = ExtractionSupport.sourceRef(
                    relativePath,
                    candidate.line(),
                    candidate.snippet(),
                    Map.of("language", languageKey, "kind", "config-entry")
                );
                String qualifiedName = relativePath + "#" + candidate.key();
                EntityKind kind = ConfigExtractionSupport.datastoreLike(candidate.key(), candidate.value())
                    ? EntityKind.DATASTORE
                    : EntityKind.CONFIG_ARTIFACT;

                ExtractedEntityFact configEntity = new ExtractedEntityFact(
                    IdUtils.scopedEntityId(languageKey, relativePath, candidate.key(), candidate.line()),
                    kind,
                    EntityOrigin.OBSERVED,
                    candidate.key(),
                    qualifiedName,
                    fileScope.id(),
                    List.of(ref),
                    Map.of(
                        "language", languageKey,
                        "configKey", candidate.key(),
                        "configValue", candidate.value(),
                        "parseStatus", parseResult.status().name(),
                        "extractionMode", extractionMode.name()
                    )
                );
                accumulator.addEntity(configEntity);
                accumulator.addRelationship(ExtractionSupport.containsRelationship(fileEntity.id(), configEntity.id(), ref));

                for (String target : ConfigExtractionSupport.dependencyTargets(candidate.key(), candidate.value())) {
                    ExtractedEntityFact external = new ExtractedEntityFact(
                        IdUtils.externalEntityId(languageKey, target),
                        ConfigExtractionSupport.datastoreLike(candidate.key(), candidate.value()) ? EntityKind.DATASTORE : EntityKind.EXTERNAL_SYSTEM,
                        EntityOrigin.INFERRED,
                        target,
                        target,
                        null,
                        List.of(ref),
                        Map.of("language", languageKey, "external", true, "sourceConfigKey", candidate.key())
                    );
                    accumulator.addEntity(external);
                    accumulator.addRelationship(ExtractionSupport.dependencyRelationship(
                        configEntity.id(),
                        external.id(),
                        target,
                        ref,
                        languageKey
                    ));
                }
            });
        }

        return accumulator;
    }

    private Set<String> candidateNodeTypes() {
        return switch (language) {
            case JSON -> Set.of("pair");
            case YAML -> Set.of("block_mapping_pair", "flow_pair");
            case PROPERTIES -> Set.of("property");
            case XML -> Set.of("element", "attribute");
            default -> Set.of();
        };
    }
}
