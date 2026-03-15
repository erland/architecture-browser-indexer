package info.isaksson.erland.architecturebrowser.indexer.ir;

import info.isaksson.erland.architecturebrowser.indexer.extract.IdUtils;
import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact;
import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedRelationshipFact;
import info.isaksson.erland.architecturebrowser.indexer.extract.model.StructuralExtractionResult;
import info.isaksson.erland.architecturebrowser.indexer.interpret.model.InterpretationResult;
import info.isaksson.erland.architecturebrowser.indexer.interpret.model.InterpretedEntityFact;
import info.isaksson.erland.architecturebrowser.indexer.interpret.model.InterpretedRelationshipFact;
import info.isaksson.erland.architecturebrowser.indexer.topology.model.TopologyResult;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureEntity;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureIndexDocument;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureRelationship;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.CompletenessMetadata;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.CompletenessStatus;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.Diagnostic;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.DiagnosticPhase;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.DiagnosticSeverity;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityOrigin;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.LogicalScope;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RelationshipKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RepositorySource;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RunMetadata;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RunOutcome;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ScopeKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.SourceReference;
import info.isaksson.erland.architecturebrowser.indexer.parse.ParseBatchResult;
import info.isaksson.erland.architecturebrowser.indexer.parse.ParseDiagnostics;
import info.isaksson.erland.architecturebrowser.indexer.parse.TreeSitterParsingService;
import info.isaksson.erland.architecturebrowser.indexer.scan.FileInventory;
import info.isaksson.erland.architecturebrowser.indexer.scan.FileInventoryEntry;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class ArchitectureIrFactory {
    private ArchitectureIrFactory() {
    }

    public static ArchitectureIndexDocument createInventoryDocument(
        RepositorySource source,
        String indexerVersion,
        FileInventory inventory,
        List<Diagnostic> acquisitionDiagnostics
    ) {
        return createInventoryDocument(source, indexerVersion, inventory, acquisitionDiagnostics, null);
    }

    public static ArchitectureIndexDocument createInventoryDocument(
        RepositorySource source,
        String indexerVersion,
        FileInventory inventory,
        List<Diagnostic> acquisitionDiagnostics,
        ParseBatchResult parseBatchResult
    ) {
        return createInventoryDocument(source, indexerVersion, inventory, acquisitionDiagnostics, parseBatchResult, null);
    }

    public static ArchitectureIndexDocument createInventoryDocument(
        RepositorySource source,
        String indexerVersion,
        FileInventory inventory,
        List<Diagnostic> acquisitionDiagnostics,
        ParseBatchResult parseBatchResult,
        StructuralExtractionResult extractionResult
    ) {
        return createInventoryDocument(source, indexerVersion, inventory, acquisitionDiagnostics, parseBatchResult, extractionResult, null);
    }

    public static ArchitectureIndexDocument createInventoryDocument(
        RepositorySource source,
        String indexerVersion,
        FileInventory inventory,
        List<Diagnostic> acquisitionDiagnostics,
        ParseBatchResult parseBatchResult,
        StructuralExtractionResult extractionResult,
        InterpretationResult interpretationResult
    ) {
        return createInventoryDocument(source, indexerVersion, inventory, acquisitionDiagnostics, parseBatchResult, extractionResult, interpretationResult, null);
    }

    public static ArchitectureIndexDocument createInventoryDocument(
        RepositorySource source,
        String indexerVersion,
        FileInventory inventory,
        List<Diagnostic> acquisitionDiagnostics,
        ParseBatchResult parseBatchResult,
        StructuralExtractionResult extractionResult,
        InterpretationResult interpretationResult,
        TopologyResult topologyResult
    ) {
        Instant generatedAt = Instant.now();

        LogicalScope repositoryScope = new LogicalScope(
            "scope:repo",
            ScopeKind.REPOSITORY,
            source.repositoryId(),
            source.repositoryId(),
            null,
            List.of(),
            Map.of("acquisitionType", source.acquisitionType())
        );

        SourceReference firstSource = inventory.entries().stream()
            .filter(entry -> !entry.ignored())
            .findFirst()
            .map(entry -> new SourceReference(entry.relativePath(), null, null, null, Map.of("type", entry.type())))
            .orElse(null);

        ArchitectureEntity inventoryEntity = new ArchitectureEntity(
            "entity:inventory:root",
            EntityKind.MODULE,
            EntityOrigin.INFERRED,
            "Repository inventory",
            source.repositoryId() + ":inventory",
            repositoryScope.id(),
            firstSource == null ? List.of() : List.of(firstSource),
            Map.of(
                "indexedFileCount", inventory.indexedFiles(),
                "totalFileCount", inventory.totalFiles(),
                "detectedLanguages", inventory.detectedLanguages(),
                "detectedTechnologyMarkers", inventory.detectedTechnologyMarkers()
            )
        );

        List<Diagnostic> diagnostics = new ArrayList<>();
        if (acquisitionDiagnostics == null || acquisitionDiagnostics.isEmpty()) {
            diagnostics.add(new Diagnostic(
                "diag:inventory:scan-complete",
                DiagnosticSeverity.INFO,
                DiagnosticPhase.ACQUISITION,
                "inventory.scan.complete",
                "Acquisition and file inventory completed",
                false,
                null,
                repositoryScope.id(),
                inventoryEntity.id(),
                inventoryEntity.sourceRefs(),
                Map.of("totalFiles", inventory.totalFiles(), "ignoredFiles", inventory.ignoredFiles())
            ));
        } else {
            diagnostics.addAll(acquisitionDiagnostics);
        }
        if (parseBatchResult != null) {
            diagnostics.addAll(ParseDiagnostics.toDiagnostics(parseBatchResult));
        }
        if (extractionResult != null) {
            diagnostics.addAll(extractionResult.diagnostics());
        }
        if (interpretationResult != null) {
            diagnostics.addAll(interpretationResult.diagnostics());
        }
        if (topologyResult != null) {
            diagnostics.addAll(topologyResult.diagnostics());
        }

        Map<String, LogicalScope> scopesById = new LinkedHashMap<>();
        scopesById.put(repositoryScope.id(), repositoryScope);
        if (extractionResult != null) {
            for (LogicalScope scope : extractionResult.scopes()) {
                scopesById.put(scope.id(), scope);
            }
        }
        if (topologyResult != null) {
            for (LogicalScope scope : topologyResult.scopes()) {
                scopesById.put(scope.id(), scope);
            }
        }
        List<LogicalScope> scopes = List.copyOf(scopesById.values());

        Map<String, ArchitectureEntity> entitiesById = new LinkedHashMap<>();
        entitiesById.put(inventoryEntity.id(), inventoryEntity);
        if (extractionResult != null) {
            for (ExtractedEntityFact entity : extractionResult.entities()) {
                entitiesById.put(entity.id(), new ArchitectureEntity(
                    entity.id(), entity.kind(), entity.origin(), entity.name(), entity.displayName(), normalizeScopeId(entity.scopeId(), repositoryScope.id()), entity.sourceRefs(), entity.metadata()
                ));
            }
        }
        if (interpretationResult != null) {
            for (InterpretedEntityFact entity : interpretationResult.entities()) {
                entitiesById.put(entity.id(), new ArchitectureEntity(
                    entity.id(), entity.kind(), entity.origin(), entity.name(), entity.displayName(), normalizeScopeId(entity.scopeId(), repositoryScope.id()), entity.sourceRefs(), entity.metadata()
                ));
            }
        }
        if (topologyResult != null) {
            for (ArchitectureEntity entity : topologyResult.entities()) {
                entitiesById.put(entity.id(), entity);
            }
        }
        Map<String, ArchitectureRelationship> relationshipsById = new LinkedHashMap<>();
        if (extractionResult != null) {
            for (ExtractedRelationshipFact relationship : extractionResult.relationships()) {
                ArchitectureRelationship architectureRelationship = new ArchitectureRelationship(
                    relationship.id(), relationship.kind(), relationship.fromEntityId(), relationship.toEntityId(), relationship.label(), relationship.sourceRefs(), relationship.metadata()
                );
                relationshipsById.put(architectureRelationship.id(), architectureRelationship);
            }
        }
        if (interpretationResult != null) {
            for (InterpretedRelationshipFact relationship : interpretationResult.relationships()) {
                ArchitectureRelationship architectureRelationship = new ArchitectureRelationship(
                    relationship.id(), relationship.kind(), relationship.fromEntityId(), relationship.toEntityId(), relationship.label(), relationship.sourceRefs(), relationship.metadata()
                );
                relationshipsById.put(architectureRelationship.id(), architectureRelationship);
            }
        }
        if (topologyResult != null) {
            for (ArchitectureRelationship relationship : topologyResult.relationships()) {
                relationshipsById.put(relationship.id(), relationship);
            }
        }
        Map<String, ArchitectureEntity> observedTypesByQualifiedName = observedTypesByQualifiedName(entitiesById);
        List<ArchitectureRelationship> relationships = enrichDependencyRelationshipMetadata(List.copyOf(relationshipsById.values()), entitiesById, observedTypesByQualifiedName);
        relationships = ensurePackageDependencyRelationships(relationships, entitiesById, observedTypesByQualifiedName);

        List<String> completenessNotes = new ArrayList<>();
        if (extractionResult == null) {
            completenessNotes.add("Inventory-only payload produced before structural extraction is implemented");
        } else if (interpretationResult == null) {
            completenessNotes.add("Structural extraction included syntax-tree-based extraction without interpretation");
        } else if (topologyResult == null) {
            completenessNotes.add("Structural extraction and first-pass interpretation rules were included");
        } else {
            completenessNotes.add("Structural extraction, interpretation, logical scoping, and relationship inference were included");
        }

        RunAssessment assessment = RunAssessment.assess(inventory, parseBatchResult, diagnostics, completenessNotes);
        CompletenessMetadata completeness = assessment.completeness();

        Map<String, Object> documentMetadata = new LinkedHashMap<>();
        documentMetadata.put("inventoryEntries", inventory.entries());
        documentMetadata.put("inventorySummary", Map.of(
            "totalFiles", inventory.totalFiles(),
            "indexedFiles", inventory.indexedFiles(),
            "ignoredFiles", inventory.ignoredFiles(),
            "detectedLanguages", inventory.detectedLanguages(),
            "detectedTechnologyMarkers", inventory.detectedTechnologyMarkers()
        ));
        if (parseBatchResult != null) {
            documentMetadata.put("parseSummary", TreeSitterParsingService.summarize(parseBatchResult));
        }
        if (extractionResult != null) {
            documentMetadata.put("extractionSummary", extractionResult.summary());
        }
        if (interpretationResult != null) {
            documentMetadata.put("interpretationSummary", interpretationResult.summary());
        }
        if (topologyResult != null) {
            documentMetadata.put("topologySummary", topologyResult.summary());
        }
        Map<String, Object> dependencyViews = buildDependencyViews(relationships, entitiesById, observedTypesByQualifiedName);
        entitiesById = enrichPackageEntities(entitiesById, dependencyViews);
        List<ArchitectureEntity> entities = List.copyOf(entitiesById.values());
        documentMetadata.put("dependencyViews", dependencyViews);
        documentMetadata.put("diagnosticSummary", assessment.diagnosticSummary());
        documentMetadata.put("partialResult", assessment.partialResult());

        RunMetadata runMetadata = new RunMetadata(
            generatedAt,
            generatedAt,
            assessment.outcome(),
            inventory.detectedTechnologyMarkers().stream().sorted().toList(),
            Map.of(
                "mode", topologyResult != null ? "cli-topology" : (interpretationResult != null ? "cli-interpretation" : (extractionResult == null ? "cli-inventory" : "cli-structural-extraction")),
                "inventoryOnly", extractionResult == null,
                "structuralExtraction", extractionResult != null,
                "interpretation", interpretationResult != null,
                "topology", topologyResult != null,
                "degradedPaths", assessment.degradedPaths()
            )
        );

        return new ArchitectureIndexDocument(
            ArchitectureIrVersions.CURRENT_SCHEMA_VERSION,
            indexerVersion,
            runMetadata,
            source,
            List.copyOf(scopes),
            List.copyOf(entities),
            List.copyOf(relationships),
            diagnostics,
            completeness,
            Map.copyOf(documentMetadata)
        );
    }


    private static List<ArchitectureRelationship> enrichDependencyRelationshipMetadata(
        List<ArchitectureRelationship> relationships,
        Map<String, ArchitectureEntity> entitiesById,
        Map<String, ArchitectureEntity> observedTypesByQualifiedName
    ) {
        List<ArchitectureRelationship> enriched = new ArrayList<>(relationships.size());
        for (ArchitectureRelationship relationship : relationships) {
            ArchitectureEntity source = canonicalDependencyEntity(entitiesById.get(relationship.fromEntityId()), observedTypesByQualifiedName);
            ArchitectureEntity target = canonicalDependencyEntity(entitiesById.get(relationship.toEntityId()), observedTypesByQualifiedName);
            boolean packageRollup = hasRollup(relationship, "package-package");
            boolean dependencyRelationship = isDependencyRelationship(relationship.kind());
            boolean packageDependencyRelationship = packageRollup || isPackageDependencyRelationship(relationship, source, target);
            if (!dependencyRelationship && !packageDependencyRelationship) {
                enriched.add(relationship);
                continue;
            }
            Map<String, Object> metadata = new LinkedHashMap<>();
            if (relationship.metadata() != null) {
                metadata.putAll(relationship.metadata());
            }
            if (isTypeDependencyRelationship(relationship, source, target)) {
                metadata.put("dependencyView", "type");
                metadata.put("dependencySourceTypeId", source == null ? relationship.fromEntityId() : source.id());
                metadata.put("dependencyTargetTypeId", target == null ? relationship.toEntityId() : target.id());
                metadata.put("dependencySourceBoundary", boundaryForEntity(source));
                metadata.put("dependencyTargetBoundary", boundaryForEntity(target));
                metadata.put("dependencyTargetInternal", isInternalEntity(target));
                metadata.put("dependencyTargetExternal", isExternalEntity(target));
                metadata.put("dependencyTargetClassification", typeClassificationForEntity(target));
                String sourcePackageName = packageNameForDependencyEntity(source);
                String targetPackageName = packageNameForDependencyEntity(target);
                if (sourcePackageName != null) {
                    metadata.put("dependencySourcePackageName", sourcePackageName);
                    metadata.put("dependencySourcePackageBoundary", packageBoundaryForName(sourcePackageName, entitiesById));
                }
                if (targetPackageName != null) {
                    metadata.put("dependencyTargetPackageName", targetPackageName);
                    metadata.put("dependencyTargetPackageBoundary", packageBoundaryForName(targetPackageName, entitiesById));
                    metadata.put("dependencyTargetPackageClassification", packageClassificationForName(targetPackageName, entitiesById));
                }
            } else if (packageDependencyRelationship) {
                metadata.put("dependencyView", "package");
                metadata.put("dependencySourcePackageId", relationship.fromEntityId());
                metadata.put("dependencyTargetPackageId", relationship.toEntityId());
                String sourcePackageName = source == null ? null : source.name();
                String targetPackageName = target == null ? null : target.name();
                if (sourcePackageName != null) {
                    metadata.put("dependencySourcePackageName", sourcePackageName);
                    metadata.put("dependencySourcePackageBoundary", packageBoundaryForName(sourcePackageName, entitiesById));
                }
                if (targetPackageName != null) {
                    metadata.put("dependencyTargetPackageName", targetPackageName);
                    metadata.put("dependencyTargetPackageBoundary", packageBoundaryForName(targetPackageName, entitiesById));
                    metadata.put("dependencyTargetPackageClassification", packageClassificationForName(targetPackageName, entitiesById));
                }
                metadata.put("dependencyTargetBoundary", targetPackageName == null ? "unknown" : packageBoundaryForName(targetPackageName, entitiesById));
            } else if (isImportEvidenceRelationship(relationship, source, target)) {
                metadata.putIfAbsent("dependencyView", "evidence");
                metadata.put("dependencyTargetInternal", isInternalEntity(target));
                metadata.put("dependencyTargetExternal", isExternalEntity(target));
                metadata.put("dependencyTargetBoundary", boundaryForEntity(target));
                metadata.put("dependencyTargetClassification", typeClassificationForEntity(target));
            }
            enriched.add(new ArchitectureRelationship(
                relationship.id(),
                relationship.kind(),
                relationship.fromEntityId(),
                relationship.toEntityId(),
                relationship.label(),
                relationship.sourceRefs(),
                Map.copyOf(metadata)
            ));
        }
        return List.copyOf(enriched);
    }

    private static List<ArchitectureRelationship> ensurePackageDependencyRelationships(
        List<ArchitectureRelationship> relationships,
        Map<String, ArchitectureEntity> entitiesById,
        Map<String, ArchitectureEntity> observedTypesByQualifiedName
    ) {
        Map<String, ArchitectureRelationship> byId = new LinkedHashMap<>();
        for (ArchitectureRelationship relationship : relationships) {
            byId.put(relationship.id(), relationship);
        }

        Map<String, ArchitectureRelationship> synthetic = new LinkedHashMap<>();
        for (ArchitectureRelationship relationship : relationships) {
            ArchitectureEntity source = canonicalDependencyEntity(entitiesById.get(relationship.fromEntityId()), observedTypesByQualifiedName);
            ArchitectureEntity target = canonicalDependencyEntity(entitiesById.get(relationship.toEntityId()), observedTypesByQualifiedName);
            if (!isTypeDependencyRelationship(relationship, source, target)) {
                continue;
            }
            String sourcePackageName = packageNameForDependencyEntity(source);
            String targetPackageName = packageNameForDependencyEntity(target);
            if (sourcePackageName == null || targetPackageName == null || sourcePackageName.equals(targetPackageName)) {
                continue;
            }
            String sourcePackageEntityId = findPackageEntityIdByName(sourcePackageName, entitiesById);
            String targetPackageEntityId = findPackageEntityIdByName(targetPackageName, entitiesById);
            if (sourcePackageEntityId == null || targetPackageEntityId == null || sourcePackageEntityId.equals(targetPackageEntityId)) {
                continue;
            }
            String syntheticId = IdUtils.relationshipId("ir-package-uses", sourcePackageEntityId, targetPackageEntityId, "");
            if (byId.containsKey(syntheticId) || synthetic.containsKey(syntheticId)) {
                continue;
            }
            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("rollup", "package-package");
            metadata.put("dependencyView", "package");
            metadata.put("dependencySourcePackageId", sourcePackageEntityId);
            metadata.put("dependencyTargetPackageId", targetPackageEntityId);
            metadata.put("dependencySourcePackageName", sourcePackageName);
            metadata.put("dependencyTargetPackageName", targetPackageName);
            metadata.put("dependencySourcePackageBoundary", packageBoundaryForName(sourcePackageName, entitiesById));
            metadata.put("dependencyTargetPackageBoundary", packageBoundaryForName(targetPackageName, entitiesById));
            metadata.put("dependencyTargetBoundary", packageBoundaryForName(targetPackageName, entitiesById));
            metadata.put("dependencyTargetPackageClassification", packageClassificationForName(targetPackageName, entitiesById));
            if (relationship.metadata() != null) {
                Object dependencySource = relationship.metadata().get("dependencySource");
                Object dependencyCategory = relationship.metadata().get("dependencyCategory");
                if (dependencySource != null) {
                    metadata.put("dependencySource", dependencySource);
                }
                if (dependencyCategory != null) {
                    metadata.put("dependencyCategory", dependencyCategory);
                }
            }
            synthetic.put(syntheticId, new ArchitectureRelationship(
                syntheticId,
                RelationshipKind.USES,
                sourcePackageEntityId,
                targetPackageEntityId,
                relationship.label(),
                relationship.sourceRefs(),
                Map.copyOf(metadata)
            ));
        }

        if (synthetic.isEmpty()) {
            return relationships;
        }
        List<ArchitectureRelationship> merged = new ArrayList<>(relationships.size() + synthetic.size());
        merged.addAll(relationships);
        merged.addAll(synthetic.values());
        return List.copyOf(merged);
    }

    private static String findPackageEntityIdByName(String packageName, Map<String, ArchitectureEntity> entitiesById) {
        if (packageName == null || packageName.isBlank()) {
            return null;
        }
        for (ArchitectureEntity entity : entitiesById.values()) {
            if (isPackageEntity(entity) && packageName.equals(entity.name())) {
                return entity.id();
            }
        }
        return null;
    }

    private static Map<String, ArchitectureEntity> enrichPackageEntities(
        Map<String, ArchitectureEntity> entitiesById,
        Map<String, Object> dependencyViews
    ) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> packageMetrics = dependencyViews == null
            ? List.of()
            : (List<Map<String, Object>>) dependencyViews.getOrDefault("packageMetrics", List.of());
        if (packageMetrics.isEmpty()) {
            return entitiesById;
        }
        Map<String, Map<String, Object>> metricsByPackageName = new LinkedHashMap<>();
        for (Map<String, Object> metric : packageMetrics) {
            Object packageName = metric.get("packageName");
            if (packageName instanceof String s && !s.isBlank()) {
                metricsByPackageName.put(s, metric);
            }
        }
        Map<String, ArchitectureEntity> enriched = new LinkedHashMap<>();
        for (ArchitectureEntity entity : entitiesById.values()) {
            if (!isPackageEntity(entity)) {
                enriched.put(entity.id(), entity);
                continue;
            }
            Map<String, Object> metric = metricsByPackageName.get(entity.name());
            if (metric == null) {
                enriched.put(entity.id(), entity);
                continue;
            }
            Map<String, Object> metadata = new LinkedHashMap<>();
            if (entity.metadata() != null) {
                metadata.putAll(entity.metadata());
            }
            metadata.putAll(metric);
            enriched.put(entity.id(), new ArchitectureEntity(
                entity.id(),
                entity.kind(),
                entity.origin(),
                entity.name(),
                entity.displayName(),
                entity.scopeId(),
                entity.sourceRefs(),
                Map.copyOf(metadata)
            ));
        }
        return Map.copyOf(enriched);
    }

    private static Map<String, Object> buildDependencyViews(
        List<ArchitectureRelationship> relationships,
        Map<String, ArchitectureEntity> entitiesById,
        Map<String, ArchitectureEntity> observedTypesByQualifiedName
    ) {
        Map<String, NormalizedTypeDependency> typeDependenciesByKey = new LinkedHashMap<>();
        Map<String, NormalizedPackageDependency> packageDependenciesByKey = new LinkedHashMap<>();
        for (ArchitectureRelationship relationship : relationships) {
            ArchitectureEntity source = canonicalDependencyEntity(entitiesById.get(relationship.fromEntityId()), observedTypesByQualifiedName);
            ArchitectureEntity target = canonicalDependencyEntity(entitiesById.get(relationship.toEntityId()), observedTypesByQualifiedName);
            if (isTypeDependencyRelationship(relationship, source, target)) {
                String sourceTypeId = source == null ? relationship.fromEntityId() : source.id();
                String targetTypeId = target == null ? relationship.toEntityId() : target.id();
                String typeKey = relationship.kind().name() + "|" + sourceTypeId + "|" + targetTypeId;
                typeDependenciesByKey.computeIfAbsent(typeKey, ignored -> new NormalizedTypeDependency(
                    sourceTypeId,
                    targetTypeId,
                    relationship.kind(),
                    source == null ? null : source.name(),
                    target == null ? null : target.name(),
                    isInternalEntity(target),
                    isExternalEntity(target),
                    boundaryForEntity(source),
                    boundaryForEntity(target),
                    typeClassificationForEntity(target)
                )).addEvidence(relationship);

                String sourcePackageName = packageNameForDependencyEntity(source);
                String targetPackageName = packageNameForDependencyEntity(target);
                if (sourcePackageName != null && targetPackageName != null && !sourcePackageName.equals(targetPackageName)) {
                    String packageKey = relationship.kind().name() + "|" + sourcePackageName + "|" + targetPackageName;
                    packageDependenciesByKey.computeIfAbsent(packageKey, ignored -> new NormalizedPackageDependency(
                        sourcePackageName,
                        targetPackageName,
                        relationship.kind(),
                        isInternalEntity(target),
                        isExternalEntity(target),
                        packageBoundaryForName(sourcePackageName, entitiesById),
                        packageBoundaryForName(targetPackageName, entitiesById),
                        packageClassificationForName(targetPackageName, entitiesById)
                    )).addEvidence(relationship, source, target);
                }
            }
        }
        List<Map<String, Object>> typeDependencies = new ArrayList<>();
        for (NormalizedTypeDependency dependency : typeDependenciesByKey.values()) {
            typeDependencies.add(dependency.toMetadataMap());
        }
        List<Map<String, Object>> packageDependencies = new ArrayList<>();
        for (NormalizedPackageDependency dependency : packageDependenciesByKey.values()) {
            packageDependencies.add(dependency.toMetadataMap());
        }
        List<Map<String, Object>> packageMetrics = buildPackageMetrics(entitiesById, packageDependencies);
        Map<String, Object> dependencyViews = new LinkedHashMap<>();
        dependencyViews.put("typeDependencies", List.copyOf(typeDependencies));
        dependencyViews.put("packageDependencies", List.copyOf(packageDependencies));
        dependencyViews.put("packageMetrics", List.copyOf(packageMetrics));
        dependencyViews.put("boundarySummary", buildBoundarySummary(typeDependencies, packageDependencies));
        return Map.copyOf(dependencyViews);
    }



    private static List<Map<String, Object>> buildPackageMetrics(
        Map<String, ArchitectureEntity> entitiesById,
        List<Map<String, Object>> packageDependencies
    ) {
        Map<String, PackageMetrics> metricsByPackage = new LinkedHashMap<>();
        for (ArchitectureEntity entity : entitiesById.values()) {
            if (isPackageEntity(entity)) {
                String packageName = entity.name();
                metricsByPackage.putIfAbsent(packageName, new PackageMetrics(
                    packageName,
                    stringMetadata(entity, "language", "unknown"),
                    deriveSourceRoot(packageName, entity.sourceRefs()),
                    boundaryForEntity(entity),
                    "observed-source-package"
                ));
            }
        }
        for (ArchitectureEntity entity : entitiesById.values()) {
            String packageName = packageNameForEntityMetrics(entity);
            PackageMetrics metrics = metricsByPackage.get(packageName);
            if (metrics == null) {
                continue;
            }
            metrics.observeEntity(entity);
        }
        for (Map<String, Object> dependency : packageDependencies) {
            Object sourcePackageName = dependency.get("sourcePackageName");
            Object targetPackageName = dependency.get("targetPackageName");
            if (sourcePackageName instanceof String s) {
                PackageMetrics sourceMetrics = metricsByPackage.get(s);
                if (sourceMetrics != null) {
                    sourceMetrics.observeOutgoingDependency();
                }
            }
            if (targetPackageName instanceof String s) {
                PackageMetrics targetMetrics = metricsByPackage.get(s);
                if (targetMetrics != null) {
                    targetMetrics.observeIncomingDependency();
                }
            }
        }
        List<Map<String, Object>> results = new ArrayList<>();
        for (PackageMetrics metrics : metricsByPackage.values()) {
            results.add(metrics.toMetadataMap());
        }
        return List.copyOf(results);
    }

    private static String packageNameForEntityMetrics(ArchitectureEntity entity) {
        if (entity == null || isPackageEntity(entity) || !isInternalEntity(entity)) {
            return null;
        }
        if (entity.metadata() != null) {
            Object explicitPackage = entity.metadata().get("packageName");
            if (explicitPackage instanceof String packageName && !packageName.isBlank()) {
                return packageName;
            }
            Object qualifiedName = entity.metadata().get("qualifiedName");
            if (qualifiedName instanceof String qualified && !qualified.isBlank()) {
                String derived = packageNameFromQualifiedName(qualified);
                if (derived != null) {
                    return derived;
                }
            }
            Object ownerQualifiedName = entity.metadata().get("ownerQualifiedName");
            if (ownerQualifiedName instanceof String ownerQualified && !ownerQualified.isBlank()) {
                String derived = packageNameFromQualifiedName(ownerQualified);
                if (derived != null) {
                    return derived;
                }
            }
        }
        return packageNameFromQualifiedName(entity.name());
    }

    private static String stringMetadata(ArchitectureEntity entity, String key, String defaultValue) {
        if (entity == null || entity.metadata() == null) {
            return defaultValue;
        }
        Object value = entity.metadata().get(key);
        if (value instanceof String s && !s.isBlank()) {
            return s;
        }
        return defaultValue;
    }

    private static String deriveSourceRoot(String packageName, List<SourceReference> sourceRefs) {
        if (sourceRefs == null || sourceRefs.isEmpty()) {
            return null;
        }
        String packagePath = packageName == null ? null : packageName.replace('.', '/');
        for (SourceReference ref : sourceRefs) {
            if (ref == null || ref.path() == null || ref.path().isBlank()) {
                continue;
            }
            String path = ref.path().replace('\\', '/');
            if (packagePath != null && !packagePath.isBlank()) {
                String marker = "/" + packagePath + "/";
                int idx = path.indexOf(marker);
                if (idx > 0) {
                    return path.substring(0, idx);
                }
                if (path.endsWith("/" + packagePath)) {
                    return path.substring(0, path.length() - packagePath.length() - 1);
                }
            }
            int slash = path.lastIndexOf('/');
            if (slash > 0) {
                return path.substring(0, slash);
            }
        }
        return null;
    }

    private static Map<String, Object> buildBoundarySummary(
        List<Map<String, Object>> typeDependencies,
        List<Map<String, Object>> packageDependencies
    ) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("typeInternalCount", countBoundary(typeDependencies, "targetBoundary", "internal"));
        summary.put("typeExternalCount", countBoundary(typeDependencies, "targetBoundary", "external"));
        summary.put("packageInternalCount", countBoundary(packageDependencies, "targetBoundary", "internal"));
        summary.put("packageExternalCount", countBoundary(packageDependencies, "targetBoundary", "external"));
        return Map.copyOf(summary);
    }

    private static int countBoundary(List<Map<String, Object>> dependencies, String key, String expectedValue) {
        int count = 0;
        for (Map<String, Object> dependency : dependencies) {
            if (Objects.equals(expectedValue, dependency.get(key))) {
                count++;
            }
        }
        return count;
    }

    private static Map<String, ArchitectureEntity> observedTypesByQualifiedName(Map<String, ArchitectureEntity> entitiesById) {
        Map<String, ArchitectureEntity> observed = new LinkedHashMap<>();
        for (ArchitectureEntity entity : entitiesById.values()) {
            if (!isTypeEntity(entity) || entity.origin() != EntityOrigin.OBSERVED) {
                continue;
            }
            String qualifiedName = qualifiedNameForEntity(entity);
            if (qualifiedName != null && !qualifiedName.isBlank()) {
                observed.putIfAbsent(qualifiedName, entity);
            }
        }
        return Map.copyOf(observed);
    }

    private static ArchitectureEntity canonicalDependencyEntity(
        ArchitectureEntity entity,
        Map<String, ArchitectureEntity> observedTypesByQualifiedName
    ) {
        if (entity == null || !isTypeEntity(entity) || entity.origin() == EntityOrigin.OBSERVED) {
            return entity;
        }
        String qualifiedName = qualifiedNameForEntity(entity);
        if (qualifiedName == null || qualifiedName.isBlank()) {
            return entity;
        }
        return observedTypesByQualifiedName.getOrDefault(qualifiedName, entity);
    }

    private static String qualifiedNameForEntity(ArchitectureEntity entity) {
        if (entity == null) {
            return null;
        }
        if (entity.metadata() != null) {
            Object qualifiedName = entity.metadata().get("qualifiedName");
            if (qualifiedName instanceof String q && !q.isBlank()) {
                return q;
            }
        }
        String name = entity.name();
        return (name == null || name.isBlank()) ? null : name;
    }


    private static boolean hasRollup(ArchitectureRelationship relationship, String expectedRollup) {
        return relationship != null
            && relationship.metadata() != null
            && Objects.equals(expectedRollup, relationship.metadata().get("rollup"));
    }

    private static boolean isDependencyRelationship(RelationshipKind kind) {
        return kind == RelationshipKind.DEPENDS_ON || kind == RelationshipKind.EXTENDS || kind == RelationshipKind.IMPLEMENTS;
    }

    private static boolean isTypeDependencyRelationship(
        ArchitectureRelationship relationship,
        ArchitectureEntity source,
        ArchitectureEntity target
    ) {
        return isDependencyRelationship(relationship.kind()) && isTypeEntity(source) && isTypeEntity(target);
    }

    private static boolean isPackageDependencyRelationship(
        ArchitectureRelationship relationship,
        ArchitectureEntity source,
        ArchitectureEntity target
    ) {
        if (source == null || target == null) {
            return false;
        }
        Object rollup = relationship.metadata() == null ? null : relationship.metadata().get("rollup");
        return (relationship.kind() == RelationshipKind.USES || isDependencyRelationship(relationship.kind()))
            && Objects.equals("package-package", rollup)
            && isPackageEntity(source)
            && isPackageEntity(target);
    }

    private static boolean isImportEvidenceRelationship(
        ArchitectureRelationship relationship,
        ArchitectureEntity source,
        ArchitectureEntity target
    ) {
        if (relationship.kind() != RelationshipKind.DEPENDS_ON || source == null || target == null) {
            return false;
        }
        Object dependencySource = relationship.metadata() == null ? null : relationship.metadata().get("dependencySource");
        return source.kind() == EntityKind.MODULE && target.kind() == EntityKind.MODULE && Objects.equals("import", dependencySource);
    }

    private static String packageNameForDependencyEntity(ArchitectureEntity entity) {
        if (entity == null) {
            return null;
        }
        if (entity.metadata() != null) {
            Object explicitPackage = entity.metadata().get("packageName");
            if (explicitPackage instanceof String packageName && !packageName.isBlank()) {
                return packageName;
            }
            Object qualifiedName = entity.metadata().get("qualifiedName");
            if (qualifiedName instanceof String qualified && !qualified.isBlank()) {
                String derived = packageNameFromQualifiedName(qualified);
                if (derived != null) {
                    return derived;
                }
            }
        }
        return packageNameFromQualifiedName(entity.name());
    }

    private static String packageNameFromQualifiedName(String qualifiedName) {
        if (qualifiedName == null || qualifiedName.isBlank() || !qualifiedName.contains(".")) {
            return null;
        }
        return qualifiedName.substring(0, qualifiedName.lastIndexOf('.'));
    }

    private static boolean isPackageEntity(ArchitectureEntity entity) {
        return entity != null
            && entity.kind() == EntityKind.MODULE
            && entity.metadata() != null
            && Objects.equals("package", entity.metadata().get("logicalRole"));
    }

    private static boolean isTypeEntity(ArchitectureEntity entity) {
        return entity != null && (entity.kind() == EntityKind.CLASS || entity.kind() == EntityKind.INTERFACE);
    }

    private static String boundaryForEntity(ArchitectureEntity entity) {
        if (isInternalEntity(entity)) {
            return "internal";
        }
        if (isExternalEntity(entity)) {
            return "external";
        }
        return "unknown";
    }

    private static String typeClassificationForEntity(ArchitectureEntity entity) {
        if (entity == null) {
            return "unknown";
        }
        if (isInternalEntity(entity)) {
            return "observed-source-type";
        }
        if (isExternalEntity(entity)) {
            return "external-or-inferred-type";
        }
        return "unknown";
    }

    private static String packageBoundaryForName(String packageName, Map<String, ArchitectureEntity> entitiesById) {
        if (packageName == null || packageName.isBlank()) {
            return "unknown";
        }
        return findPackageEntityIdByName(packageName, entitiesById) != null ? "internal" : "external";
    }

    private static String packageClassificationForName(String packageName, Map<String, ArchitectureEntity> entitiesById) {
        if (packageName == null || packageName.isBlank()) {
            return "unknown";
        }
        return findPackageEntityIdByName(packageName, entitiesById) != null ? "observed-source-package" : "external-package";
    }

    private static boolean isInternalEntity(ArchitectureEntity entity) {
        if (entity == null) {
            return false;
        }
        Object external = entity.metadata() == null ? null : entity.metadata().get("external");
        return !Boolean.TRUE.equals(external) && entity.origin() == EntityOrigin.OBSERVED;
    }

    private static boolean isExternalEntity(ArchitectureEntity entity) {
        if (entity == null) {
            return false;
        }
        Object external = entity.metadata() == null ? null : entity.metadata().get("external");
        return Boolean.TRUE.equals(external) || entity.origin() == EntityOrigin.INFERRED;
    }

    private static final class NormalizedTypeDependency {
        private final String sourceTypeId;
        private final String targetTypeId;
        private final RelationshipKind relationshipKind;
        private final String sourceTypeName;
        private final String targetTypeName;
        private final boolean internalTarget;
        private final boolean externalTarget;
        private final String sourceBoundary;
        private final String targetBoundary;
        private final String targetClassification;
        private final Set<String> dependencySources = new LinkedHashSet<>();
        private final Set<String> dependencyCategories = new LinkedHashSet<>();
        private final Set<String> evidenceRelationshipIds = new LinkedHashSet<>();
        private final Set<String> evidenceLabels = new LinkedHashSet<>();

        private NormalizedTypeDependency(
            String sourceTypeId,
            String targetTypeId,
            RelationshipKind relationshipKind,
            String sourceTypeName,
            String targetTypeName,
            boolean internalTarget,
            boolean externalTarget,
            String sourceBoundary,
            String targetBoundary,
            String targetClassification
        ) {
            this.sourceTypeId = sourceTypeId;
            this.targetTypeId = targetTypeId;
            this.relationshipKind = relationshipKind;
            this.sourceTypeName = sourceTypeName;
            this.targetTypeName = targetTypeName;
            this.internalTarget = internalTarget;
            this.externalTarget = externalTarget;
            this.sourceBoundary = sourceBoundary;
            this.targetBoundary = targetBoundary;
            this.targetClassification = targetClassification;
        }

        private void addEvidence(ArchitectureRelationship relationship) {
            evidenceRelationshipIds.add(relationship.id());
            if (relationship.label() != null && !relationship.label().isBlank()) {
                evidenceLabels.add(relationship.label());
            }
            if (relationship.metadata() != null) {
                addIfPresent(dependencySources, relationship.metadata().get("dependencySource"));
                addIfPresent(dependencyCategories, relationship.metadata().get("dependencyCategory"));
            }
        }

        private Map<String, Object> toMetadataMap() {
            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("sourceTypeId", sourceTypeId);
            metadata.put("targetTypeId", targetTypeId);
            metadata.put("relationshipKind", relationshipKind.name());
            if (sourceTypeName != null) {
                metadata.put("sourceTypeName", sourceTypeName);
            }
            if (targetTypeName != null) {
                metadata.put("targetTypeName", targetTypeName);
            }
            metadata.put("dependencySources", List.copyOf(dependencySources));
            metadata.put("dependencyCategories", List.copyOf(dependencyCategories));
            metadata.put("sourceBoundary", sourceBoundary);
            metadata.put("targetBoundary", targetBoundary);
            metadata.put("targetClassification", targetClassification);
            metadata.put("internalTarget", internalTarget);
            metadata.put("externalTarget", externalTarget);
            metadata.put("evidenceRelationshipCount", evidenceRelationshipIds.size());
            metadata.put("evidenceRelationshipIds", List.copyOf(evidenceRelationshipIds));
            metadata.put("evidenceLabels", List.copyOf(evidenceLabels));
            return Map.copyOf(metadata);
        }

        private static void addIfPresent(Set<String> sink, Object value) {
            if (value == null) {
                return;
            }
            String asString = Objects.toString(value, "").trim();
            if (!asString.isEmpty()) {
                sink.add(asString);
            }
        }
    }

    private static final class NormalizedPackageDependency {
        private final String sourcePackageName;
        private final String targetPackageName;
        private final RelationshipKind relationshipKind;
        private final boolean internalTarget;
        private final boolean externalTarget;
        private final String sourceBoundary;
        private final String targetBoundary;
        private final String targetPackageClassification;
        private final Set<String> dependencySources = new LinkedHashSet<>();
        private final Set<String> dependencyCategories = new LinkedHashSet<>();
        private final Set<String> evidenceRelationshipIds = new LinkedHashSet<>();
        private final Set<String> evidenceLabels = new LinkedHashSet<>();
        private final Set<String> sourceTypeIds = new LinkedHashSet<>();
        private final Set<String> targetTypeIds = new LinkedHashSet<>();

        private NormalizedPackageDependency(
            String sourcePackageName,
            String targetPackageName,
            RelationshipKind relationshipKind,
            boolean internalTarget,
            boolean externalTarget,
            String sourceBoundary,
            String targetBoundary,
            String targetPackageClassification
        ) {
            this.sourcePackageName = sourcePackageName;
            this.targetPackageName = targetPackageName;
            this.relationshipKind = relationshipKind;
            this.internalTarget = internalTarget;
            this.externalTarget = externalTarget;
            this.sourceBoundary = sourceBoundary;
            this.targetBoundary = targetBoundary;
            this.targetPackageClassification = targetPackageClassification;
        }

        private void addEvidence(ArchitectureRelationship relationship, ArchitectureEntity source, ArchitectureEntity target) {
            evidenceRelationshipIds.add(relationship.id());
            if (relationship.label() != null && !relationship.label().isBlank()) {
                evidenceLabels.add(relationship.label());
            }
            if (source != null) {
                sourceTypeIds.add(source.id());
            }
            if (target != null) {
                targetTypeIds.add(target.id());
            }
            if (relationship.metadata() != null) {
                NormalizedTypeDependency.addIfPresent(dependencySources, relationship.metadata().get("dependencySource"));
                NormalizedTypeDependency.addIfPresent(dependencyCategories, relationship.metadata().get("dependencyCategory"));
            }
        }

        private Map<String, Object> toMetadataMap() {
            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("sourcePackageName", sourcePackageName);
            metadata.put("targetPackageName", targetPackageName);
            metadata.put("relationshipKind", relationshipKind.name());
            metadata.put("dependencySources", List.copyOf(dependencySources));
            metadata.put("dependencyCategories", List.copyOf(dependencyCategories));
            metadata.put("sourceBoundary", sourceBoundary);
            metadata.put("targetBoundary", targetBoundary);
            metadata.put("targetPackageClassification", targetPackageClassification);
            metadata.put("internalTarget", internalTarget);
            metadata.put("externalTarget", externalTarget);
            metadata.put("underlyingRelationshipCount", evidenceRelationshipIds.size());
            metadata.put("sourceTypeCount", sourceTypeIds.size());
            metadata.put("targetTypeCount", targetTypeIds.size());
            metadata.put("evidenceRelationshipIds", List.copyOf(evidenceRelationshipIds));
            metadata.put("evidenceLabels", List.copyOf(evidenceLabels));
            return Map.copyOf(metadata);
        }
    }

    private static final class PackageMetrics {
        private final String packageName;
        private final String language;
        private final String sourceRoot;
        private final String packageBoundary;
        private final String packageClassification;
        private int declaredTypeCount;
        private int classCount;
        private int interfaceCount;
        private int enumCount;
        private int recordCount;
        private int fieldCount;
        private int functionCount;
        private int incomingDependencyCount;
        private int outgoingDependencyCount;

        private PackageMetrics(
            String packageName,
            String language,
            String sourceRoot,
            String packageBoundary,
            String packageClassification
        ) {
            this.packageName = packageName;
            this.language = language;
            this.sourceRoot = sourceRoot;
            this.packageBoundary = packageBoundary;
            this.packageClassification = packageClassification;
        }

        private void observeEntity(ArchitectureEntity entity) {
            if (entity == null) {
                return;
            }
            switch (entity.kind()) {
                case CLASS -> {
                    declaredTypeCount++;
                    Object declarationKind = entity.metadata() == null ? null : entity.metadata().get("declarationKind");
                    if (Objects.equals("enum", declarationKind)) {
                        enumCount++;
                    } else if (Objects.equals("record", declarationKind)) {
                        recordCount++;
                    } else {
                        classCount++;
                    }
                }
                case INTERFACE -> {
                    declaredTypeCount++;
                    interfaceCount++;
                }
                case FIELD -> fieldCount++;
                case FUNCTION -> functionCount++;
                default -> {
                }
            }
        }

        private void observeIncomingDependency() {
            incomingDependencyCount++;
        }

        private void observeOutgoingDependency() {
            outgoingDependencyCount++;
        }

        private Map<String, Object> toMetadataMap() {
            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("packageName", packageName);
            metadata.put("qualifiedName", packageName);
            metadata.put("language", language);
            if (sourceRoot != null && !sourceRoot.isBlank()) {
                metadata.put("sourceRoot", sourceRoot);
            }
            metadata.put("packageBoundary", packageBoundary);
            metadata.put("packageClassification", packageClassification);
            metadata.put("declaredTypeCount", declaredTypeCount);
            metadata.put("classCount", classCount);
            metadata.put("interfaceCount", interfaceCount);
            metadata.put("enumCount", enumCount);
            metadata.put("recordCount", recordCount);
            metadata.put("fieldCount", fieldCount);
            metadata.put("functionCount", functionCount);
            metadata.put("incomingDependencyCount", incomingDependencyCount);
            metadata.put("outgoingDependencyCount", outgoingDependencyCount);
            return Map.copyOf(metadata);
        }
    }

    public static ArchitectureIndexDocument createPlaceholderDocument(RepositorySource source, String indexerVersion) {
        return createInventoryDocument(
            source,
            indexerVersion,
            new FileInventory(List.of(), 0, 0, 0, java.util.Set.of(), java.util.Set.of()),
            List.of()
        );
    }

    private static String normalizeScopeId(String scopeId, String repositoryScopeId) {
        if (scopeId == null || scopeId.isBlank()) {
            return repositoryScopeId;
        }
        return scopeId;
    }
}
