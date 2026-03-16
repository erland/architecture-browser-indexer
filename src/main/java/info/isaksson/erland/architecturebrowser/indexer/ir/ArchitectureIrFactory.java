package info.isaksson.erland.architecturebrowser.indexer.ir;

import info.isaksson.erland.architecturebrowser.indexer.extract.IdUtils;
import info.isaksson.erland.architecturebrowser.indexer.extract.model.StructuralExtractionResult;
import info.isaksson.erland.architecturebrowser.indexer.interpret.model.InterpretationResult;
import info.isaksson.erland.architecturebrowser.indexer.topology.model.TopologyResult;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureEntity;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureIndexDocument;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureRelationship;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.CompletenessMetadata;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.CompletenessStatus;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.Diagnostic;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityOrigin;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.LogicalScope;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RelationshipKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RepositorySource;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RunMetadata;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RunOutcome;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.SourceReference;
import info.isaksson.erland.architecturebrowser.indexer.parse.ParseBatchResult;
import info.isaksson.erland.architecturebrowser.indexer.scan.FileInventory;

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
        ArchitectureIrAssemblyInputs inputs = new ArchitectureIrAssemblyInputs(
            source,
            inventory,
            acquisitionDiagnostics,
            parseBatchResult,
            extractionResult,
            interpretationResult,
            topologyResult
        );
        ArchitectureIrAssemblyState assembly = ArchitectureIrAssemblyStateBuilder.build(inputs);
        List<String> completenessNotes = defaultCompletenessNotes(extractionResult, interpretationResult, topologyResult);
        RunAssessment assessment = RunAssessment.assess(inventory, parseBatchResult, assembly.diagnostics(), completenessNotes);
        CompletenessMetadata completeness = assessment.completeness();
        Map<String, Object> documentMetadata = ArchitectureIrDocumentMetadataBuilder.build(inputs, assembly, assessment);
        RunMetadata runMetadata = ArchitectureIrRunMetadataBuilder.build(
            generatedAt,
            inventory,
            extractionResult,
            interpretationResult,
            topologyResult,
            assessment
        );

        return new ArchitectureIndexDocument(
            ArchitectureIrVersions.CURRENT_SCHEMA_VERSION,
            indexerVersion,
            runMetadata,
            source,
            assembly.scopes(),
            assembly.entities(),
            assembly.relationships(),
            assembly.diagnostics(),
            completeness,
            documentMetadata
        );
    }


    static List<ArchitectureRelationship> enrichDependencyRelationshipMetadata(
        List<ArchitectureRelationship> relationships,
        Map<String, ArchitectureEntity> entitiesById,
        Map<String, ArchitectureEntity> observedTypesByQualifiedName
    ) {
        List<ArchitectureRelationship> enriched = new ArrayList<>(relationships.size());
        for (ArchitectureRelationship relationship : relationships) {
            ArchitectureEntity source = canonicalDependencyEntity(entitiesById.get(relationship.fromEntityId()), observedTypesByQualifiedName);
            ArchitectureEntity target = canonicalDependencyEntity(entitiesById.get(relationship.toEntityId()), observedTypesByQualifiedName);
            boolean packageRollup = hasRollup(relationship, "package-package");
            boolean moduleRollup = hasRollup(relationship, "module-module");
            boolean dependencyRelationship = isDependencyRelationship(relationship.kind());
            boolean packageDependencyRelationship = packageRollup || isPackageDependencyRelationship(relationship, source, target);
            boolean moduleDependencyRelationship = moduleRollup || isModuleDependencyRelationship(relationship, source, target);
            if (!dependencyRelationship && !packageDependencyRelationship && !moduleDependencyRelationship) {
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
            } else if (moduleDependencyRelationship) {
                metadata.put("dependencyView", "module");
                metadata.put("dependencySourceModuleId", relationship.fromEntityId());
                metadata.put("dependencyTargetModuleId", relationship.toEntityId());
                String sourceModuleName = source == null ? null : moduleNameForDependencyEntity(source);
                String targetModuleName = target == null ? null : moduleNameForDependencyEntity(target);
                if (sourceModuleName != null) {
                    metadata.put("dependencySourceModuleName", sourceModuleName);
                    metadata.put("dependencySourceModuleBoundary", moduleBoundaryForName(sourceModuleName, entitiesById));
                }
                if (targetModuleName != null) {
                    metadata.put("dependencyTargetModuleName", targetModuleName);
                    metadata.put("dependencyTargetModuleBoundary", moduleBoundaryForName(targetModuleName, entitiesById));
                    metadata.put("dependencyTargetModuleClassification", moduleClassificationForName(targetModuleName, entitiesById));
                }
                metadata.put("dependencyTargetBoundary", targetModuleName == null ? "unknown" : moduleBoundaryForName(targetModuleName, entitiesById));
                metadata.put("sameModule", Objects.equals(sourceModuleName, targetModuleName));
            } else if (isImportEvidenceRelationship(relationship, source, target)) {
                metadata.putIfAbsent("dependencyView", "evidence");
                metadata.put("dependencyTier", "supporting-evidence");
                metadata.put("architecturePrimary", false);
                metadata.put("recommendedForArchitectureViews", false);
                metadata.put("dependencyTargetInternal", isInternalEntity(target));
                metadata.put("dependencyTargetExternal", isExternalEntity(target));
                metadata.put("dependencyTargetBoundary", boundaryForEntity(target));
                metadata.put("dependencyTargetClassification", typeClassificationForEntity(target));
                metadata.put("evidenceKind", "file-import");
                metadata.put("evidenceSourceEntityId", relationship.fromEntityId());
                metadata.put("evidenceTargetEntityId", relationship.toEntityId());
                String sourceModuleName = source == null ? null : source.name();
                String targetModuleName = target == null ? null : target.name();
                if (sourceModuleName != null) {
                    metadata.put("evidenceSourceName", sourceModuleName);
                }
                if (targetModuleName != null) {
                    metadata.put("evidenceTargetName", targetModuleName);
                }
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

    static List<ArchitectureRelationship> ensurePackageDependencyRelationships(
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

    static Map<String, ArchitectureEntity> enrichPackageEntities(
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

    static Map<String, Object> buildDependencyViews(
        List<ArchitectureRelationship> relationships,
        Map<String, ArchitectureEntity> entitiesById,
        Map<String, ArchitectureEntity> observedTypesByQualifiedName
    ) {
        Map<String, NormalizedTypeDependency> typeDependenciesByKey = new LinkedHashMap<>();
        Map<String, NormalizedPackageDependency> packageDependenciesByKey = new LinkedHashMap<>();
        Map<String, NormalizedModuleDependency> moduleDependenciesByKey = new LinkedHashMap<>();
        Map<String, EvidenceDependency> evidenceDependenciesByKey = new LinkedHashMap<>();
        for (ArchitectureRelationship relationship : relationships) {
            ArchitectureEntity rawSource = entitiesById.get(relationship.fromEntityId());
            ArchitectureEntity rawTarget = entitiesById.get(relationship.toEntityId());
            ArchitectureEntity source = canonicalDependencyEntity(rawSource, observedTypesByQualifiedName);
            ArchitectureEntity target = canonicalDependencyEntity(rawTarget, observedTypesByQualifiedName);
            if (isImportEvidenceRelationship(relationship, rawSource, rawTarget)) {
                String sourceEntityId = rawSource == null ? relationship.fromEntityId() : rawSource.id();
                String targetEntityId = rawTarget == null ? relationship.toEntityId() : rawTarget.id();
                String evidenceKey = relationship.kind().name() + "|" + sourceEntityId + "|" + targetEntityId;
                evidenceDependenciesByKey.computeIfAbsent(evidenceKey, ignored -> new EvidenceDependency(
                    sourceEntityId,
                    targetEntityId,
                    relationship.kind(),
                    rawSource == null ? null : rawSource.name(),
                    rawTarget == null ? null : rawTarget.name(),
                    boundaryForEntity(rawSource),
                    boundaryForEntity(rawTarget),
                    typeClassificationForEntity(rawTarget)
                )).addEvidence(relationship);
            }
            if (isTypeDependencyRelationship(relationship, source, target)) {
                String sourceTypeId = source == null ? relationship.fromEntityId() : source.id();
                String targetTypeId = target == null ? relationship.toEntityId() : target.id();
                String typeKey = relationship.kind().name() + "|" + sourceTypeId + "|" + targetTypeId;
                typeDependenciesByKey.computeIfAbsent(typeKey, ignored -> new NormalizedTypeDependency(
                    sourceTypeId,
                    targetTypeId,
                    relationship.kind(),
                    qualifiedNameForEntity(source),
                    qualifiedNameForEntity(target),
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
                String sourceModuleName = moduleNameForDependencyEntity(source);
                String targetModuleName = moduleNameForDependencyEntity(target);
                if (sourceModuleName != null && targetModuleName != null) {
                    String moduleKey = relationship.kind().name() + "|" + sourceModuleName + "|" + targetModuleName;
                    moduleDependenciesByKey.computeIfAbsent(moduleKey, ignored -> new NormalizedModuleDependency(
                        sourceModuleName,
                        targetModuleName,
                        relationship.kind(),
                        isInternalEntity(target),
                        isExternalEntity(target),
                        moduleBoundaryForName(sourceModuleName, entitiesById),
                        moduleBoundaryForName(targetModuleName, entitiesById),
                        moduleClassificationForName(targetModuleName, entitiesById),
                        Objects.equals(sourceModuleName, targetModuleName)
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
        List<Map<String, Object>> moduleDependencies = new ArrayList<>();
        for (NormalizedModuleDependency dependency : moduleDependenciesByKey.values()) {
            moduleDependencies.add(dependency.toMetadataMap());
        }
        List<Map<String, Object>> evidenceDependencies = new ArrayList<>();
        for (EvidenceDependency dependency : evidenceDependenciesByKey.values()) {
            evidenceDependencies.add(dependency.toMetadataMap());
        }
        List<Map<String, Object>> packageMetrics = buildPackageMetrics(entitiesById, packageDependencies);
        List<Map<String, Object>> frameworkTypeDependencies = filterDependenciesByViewKind(typeDependencies, "framework");
        List<Map<String, Object>> frameworkModuleDependencies = filterDependenciesByViewKind(moduleDependencies, "framework");
        List<Map<String, Object>> compositionTypeDependencies = filterDependenciesByViewKind(typeDependencies, "composition");
        List<Map<String, Object>> compositionModuleDependencies = filterDependenciesByViewKind(moduleDependencies, "composition");
        List<Map<String, Object>> routeTypeDependencies = filterDependenciesByViewKind(typeDependencies, "route");
        List<Map<String, Object>> routeModuleDependencies = filterDependenciesByViewKind(moduleDependencies, "route");
        List<Map<String, Object>> providerTypeDependencies = filterDependenciesByViewKind(typeDependencies, "provider-di");
        List<Map<String, Object>> providerModuleDependencies = filterDependenciesByViewKind(moduleDependencies, "provider-di");
        List<Map<String, Object>> hookTypeDependencies = filterDependenciesByViewKind(typeDependencies, "hook");
        List<Map<String, Object>> hookModuleDependencies = filterDependenciesByViewKind(moduleDependencies, "hook");
        List<Map<String, Object>> endpointTypeDependencies = filterDependenciesByViewKind(typeDependencies, "endpoint");
        List<Map<String, Object>> endpointModuleDependencies = filterDependenciesByViewKind(moduleDependencies, "endpoint");
        List<Map<String, Object>> entityModelTypeDependencies = filterDependenciesByViewKind(typeDependencies, "entity-model");
        List<Map<String, Object>> entityModelModuleDependencies = filterDependenciesByViewKind(moduleDependencies, "entity-model");
        List<Map<String, Object>> observerTypeDependencies = filterDependenciesByViewKind(typeDependencies, "observer-event");
        List<Map<String, Object>> observerModuleDependencies = filterDependenciesByViewKind(moduleDependencies, "observer-event");
        List<Map<String, Object>> writePathTypeDependencies = filterDependenciesByViewKind(typeDependencies, "write-path");
        List<Map<String, Object>> writePathModuleDependencies = filterDependenciesByViewKind(moduleDependencies, "write-path");
        Map<String, Object> dependencyViews = new LinkedHashMap<>();
        dependencyViews.put("typeDependencies", List.copyOf(typeDependencies));
        dependencyViews.put("packageDependencies", List.copyOf(packageDependencies));
        dependencyViews.put("moduleDependencies", List.copyOf(moduleDependencies));
        dependencyViews.put("evidenceDependencies", List.copyOf(evidenceDependencies));
        dependencyViews.put("frameworkTypeDependencies", List.copyOf(frameworkTypeDependencies));
        dependencyViews.put("frameworkModuleDependencies", List.copyOf(frameworkModuleDependencies));
        dependencyViews.put("compositionTypeDependencies", List.copyOf(compositionTypeDependencies));
        dependencyViews.put("compositionModuleDependencies", List.copyOf(compositionModuleDependencies));
        dependencyViews.put("routeTypeDependencies", List.copyOf(routeTypeDependencies));
        dependencyViews.put("routeModuleDependencies", List.copyOf(routeModuleDependencies));
        dependencyViews.put("providerTypeDependencies", List.copyOf(providerTypeDependencies));
        dependencyViews.put("providerModuleDependencies", List.copyOf(providerModuleDependencies));
        dependencyViews.put("hookTypeDependencies", List.copyOf(hookTypeDependencies));
        dependencyViews.put("hookModuleDependencies", List.copyOf(hookModuleDependencies));
        dependencyViews.put("endpointTypeDependencies", List.copyOf(endpointTypeDependencies));
        dependencyViews.put("endpointModuleDependencies", List.copyOf(endpointModuleDependencies));
        dependencyViews.put("entityModelTypeDependencies", List.copyOf(entityModelTypeDependencies));
        dependencyViews.put("entityModelModuleDependencies", List.copyOf(entityModelModuleDependencies));
        dependencyViews.put("observerTypeDependencies", List.copyOf(observerTypeDependencies));
        dependencyViews.put("observerModuleDependencies", List.copyOf(observerModuleDependencies));
        dependencyViews.put("writePathTypeDependencies", List.copyOf(writePathTypeDependencies));
        dependencyViews.put("writePathModuleDependencies", List.copyOf(writePathModuleDependencies));
        dependencyViews.put("packageMetrics", List.copyOf(packageMetrics));
        dependencyViews.put("boundarySummary", buildBoundarySummary(typeDependencies, packageDependencies, moduleDependencies));
        List<String> recommendedEntryPoints = new ArrayList<>(List.of("packageDependencies", "typeDependencies", "moduleDependencies"));
        List<String> primaryArchitectureViews = new ArrayList<>(List.of("packageDependencies", "typeDependencies", "moduleDependencies"));
        if (!frameworkTypeDependencies.isEmpty()) {
            recommendedEntryPoints.add("frameworkTypeDependencies");
            primaryArchitectureViews.add("frameworkTypeDependencies");
        }
        if (!frameworkModuleDependencies.isEmpty()) {
            recommendedEntryPoints.add("frameworkModuleDependencies");
            primaryArchitectureViews.add("frameworkModuleDependencies");
        }
        if (!endpointTypeDependencies.isEmpty()) {
            recommendedEntryPoints.add("endpointTypeDependencies");
            primaryArchitectureViews.add("endpointTypeDependencies");
        }
        if (!entityModelTypeDependencies.isEmpty()) {
            recommendedEntryPoints.add("entityModelTypeDependencies");
            primaryArchitectureViews.add("entityModelTypeDependencies");
        }
        if (!observerTypeDependencies.isEmpty()) {
            recommendedEntryPoints.add("observerTypeDependencies");
            primaryArchitectureViews.add("observerTypeDependencies");
        }
        if (!writePathTypeDependencies.isEmpty()) {
            recommendedEntryPoints.add("writePathTypeDependencies");
            primaryArchitectureViews.add("writePathTypeDependencies");
        }
        recommendedEntryPoints.add("evidenceDependencies");
        dependencyViews.put("recommendedEntryPoints", List.copyOf(recommendedEntryPoints));
        dependencyViews.put("primaryArchitectureViews", List.copyOf(primaryArchitectureViews));
        dependencyViews.put("frontendArchitectureViews", Map.of(
            "frameworkAware", List.of("frameworkTypeDependencies", "frameworkModuleDependencies"),
            "composition", List.of("compositionTypeDependencies", "compositionModuleDependencies"),
            "routing", List.of("routeTypeDependencies", "routeModuleDependencies"),
            "providerAndDi", List.of("providerTypeDependencies", "providerModuleDependencies"),
            "hooks", List.of("hookTypeDependencies", "hookModuleDependencies")
        ));
        dependencyViews.put("javaFrameworkArchitectureViews", Map.of(
            "endpoints", List.of("endpointTypeDependencies", "endpointModuleDependencies"),
            "entityModel", List.of("entityModelTypeDependencies", "entityModelModuleDependencies"),
            "observerEvents", List.of("observerTypeDependencies", "observerModuleDependencies"),
            "writePaths", List.of("writePathTypeDependencies", "writePathModuleDependencies")
        ));
        Map<String, Object> frontendBrowserViews = buildFrontendBrowserViews(
            compositionTypeDependencies,
            compositionModuleDependencies,
            routeTypeDependencies,
            routeModuleDependencies,
            providerTypeDependencies,
            providerModuleDependencies,
            hookTypeDependencies,
            hookModuleDependencies
        );
        if (!frontendBrowserViews.isEmpty()) {
            dependencyViews.put("frontendBrowserViews", frontendBrowserViews);
        }
        Map<String, Object> javaBrowserViews = buildJavaBrowserViews(
            endpointTypeDependencies,
            endpointModuleDependencies,
            entityModelTypeDependencies,
            entityModelModuleDependencies,
            observerTypeDependencies,
            observerModuleDependencies,
            writePathTypeDependencies,
            writePathModuleDependencies
        );
        if (!javaBrowserViews.isEmpty()) {
            dependencyViews.put("javaBrowserViews", javaBrowserViews);
        }
        Map<String, Object> browserViewCatalog = buildBrowserViewCatalog(frontendBrowserViews, javaBrowserViews);
        if (!browserViewCatalog.isEmpty()) {
            dependencyViews.put("browserViewCatalog", browserViewCatalog);
        }
        dependencyViews.put("evidenceStatus", Map.of(
            "fileImportDependencies", "supporting-evidence",
            "recommendedForArchitectureViews", false,
            "description", "File import dependencies are retained for traceability and drill-down, but higher-level architecture views should prefer package, type, module, and framework-aware dependency rollups."
        ));
        return Map.copyOf(dependencyViews);
    }



    private static List<Map<String, Object>> filterDependenciesByViewKind(List<Map<String, Object>> dependencies, String viewKind) {
        List<Map<String, Object>> filtered = new ArrayList<>();
        for (Map<String, Object> dependency : dependencies) {
            if (hasArchitectureViewKind(dependency, viewKind)) {
                filtered.add(dependency);
            }
        }
        return List.copyOf(filtered);
    }

    @SuppressWarnings("unchecked")
    private static boolean hasArchitectureViewKind(Map<String, Object> dependency, String viewKind) {
        if (dependency == null || viewKind == null || viewKind.isBlank()) {
            return false;
        }
        Object value = dependency.get("architectureViewKinds");
        if (value instanceof List<?> list) {
            return list.stream().filter(Objects::nonNull).map(String::valueOf).anyMatch(viewKind::equals);
        }
        return false;
    }

    private static void addFrameworkMetadata(
        Set<String> frameworks,
        Set<String> frameworkRelationships,
        Set<String> architectureViewKinds,
        ArchitectureRelationship relationship
    ) {
        if (relationship == null || relationship.metadata() == null) {
            return;
        }
        Object framework = relationship.metadata().get("framework");
        NormalizedTypeDependency.addIfPresent(frameworks, framework);
        Object frameworkRelationship = relationship.metadata().get("frameworkRelationship");
        Object relationshipType = relationship.metadata().get("relationshipType");
        NormalizedTypeDependency.addIfPresent(frameworkRelationships, frameworkRelationship);
        NormalizedTypeDependency.addIfPresent(frameworkRelationships, relationshipType);
        addArchitectureViewKinds(architectureViewKinds, relationship.kind(), framework, relationship.metadata().get("dependencySource"), frameworkRelationship, relationshipType);
    }

    private static void addArchitectureViewKinds(Set<String> sink, RelationshipKind relationshipKind, Object framework, Object dependencySource, Object frameworkRelationship, Object relationshipType) {
        String dependencySourceValue = dependencySource == null ? "" : String.valueOf(dependencySource).trim();
        String frameworkRelationshipValue = frameworkRelationship == null ? "" : String.valueOf(frameworkRelationship).trim();
        String relationshipTypeValue = relationshipType == null ? "" : String.valueOf(relationshipType).trim();
        String frameworkValue = framework == null ? "" : String.valueOf(framework).trim();
        boolean frameworkSpecificSource = dependencySourceValue.startsWith("react:") || dependencySourceValue.startsWith("angular:");
        boolean javaFrameworkSemantic = "jax-rs".equals(frameworkValue) || "jpa".equals(frameworkValue) || "cdi".equals(frameworkValue);
        if (!frameworkRelationshipValue.isEmpty() || !relationshipTypeValue.isEmpty() || frameworkSpecificSource || javaFrameworkSemantic) {
            sink.add("framework");
        }
        if (relationshipKind == RelationshipKind.EXPOSES || "jax-rs".equals(frameworkValue) && ("endpoint".equals(relationshipTypeValue) || !String.valueOf(relationshipKind).isEmpty() && (relationshipKind == RelationshipKind.EXPOSES))) {
            sink.add("endpoint");
        }
        String javaSemanticKey = !frameworkRelationshipValue.isEmpty() ? frameworkRelationshipValue : relationshipTypeValue;
        switch (javaSemanticKey) {
            case "publishesEvent", "observesEvent", "eventObservedBy" -> sink.add("observer-event");
            case "hasAssociation", "embeds", "inheritsPersistenceModel" -> sink.add("entity-model");
            case "writePath" -> sink.add("write-path");
        }
        String key = !frameworkRelationshipValue.isEmpty() ? frameworkRelationshipValue : dependencySourceValue;
        if (key.isEmpty()) {
            return;
        }
        switch (key) {
            case "renders", "templateRenders", "usesDirective", "usesPipe", "declares", "imports", "exports", "bootstraps" -> sink.add("composition");
            case "targets", "childOf", "lazyLoads", "guards", "resolves" -> sink.add("route");
            case "provides", "providedBy", "injects", "resolvesTo", "providesContext", "consumesContext" -> sink.add("provider-di");
            case "usesHook" -> sink.add("hook");
            default -> {
                if (dependencySourceValue.contains("route")) {
                    sink.add("route");
                }
            }
        }
    }


    private static Map<String, Object> buildFrontendBrowserViews(
        List<Map<String, Object>> compositionTypeDependencies,
        List<Map<String, Object>> compositionModuleDependencies,
        List<Map<String, Object>> routeTypeDependencies,
        List<Map<String, Object>> routeModuleDependencies,
        List<Map<String, Object>> providerTypeDependencies,
        List<Map<String, Object>> providerModuleDependencies,
        List<Map<String, Object>> hookTypeDependencies,
        List<Map<String, Object>> hookModuleDependencies
    ) {
        List<FrontendBrowserViewDefinition> definitions = List.of(
            new FrontendBrowserViewDefinition(
                "angularModuleGraph",
                "Angular module graph",
                "Angular module, standalone component, and template composition relationships for browser-native graph exploration.",
                "angular",
                "composition",
                "compositionTypeDependencies",
                "compositionModuleDependencies",
                List.of("declares", "imports", "exports", "bootstraps", "templateRenders", "usesDirective", "usesPipe"),
                compositionTypeDependencies,
                compositionModuleDependencies
            ),
            new FrontendBrowserViewDefinition(
                "angularProviderGraph",
                "Angular provider graph",
                "Angular provider, injection-token, and dependency-injection relationships for browser-native graph exploration.",
                "angular",
                "provider-di",
                "providerTypeDependencies",
                "providerModuleDependencies",
                List.of("provides", "providedBy", "injects", "resolvesTo"),
                providerTypeDependencies,
                providerModuleDependencies
            ),
            new FrontendBrowserViewDefinition(
                "routeGraph",
                "Frontend route graph",
                "Angular and React routing relationships for browser-native navigation and path analysis.",
                "frontend",
                "route",
                "routeTypeDependencies",
                "routeModuleDependencies",
                List.of("targets", "childOf", "lazyLoads", "guards", "resolves"),
                routeTypeDependencies,
                routeModuleDependencies
            ),
            new FrontendBrowserViewDefinition(
                "reactComponentCompositionGraph",
                "React component composition graph",
                "React render/composition relationships for browser-native component graph exploration.",
                "react",
                "composition",
                "compositionTypeDependencies",
                "compositionModuleDependencies",
                List.of("renders"),
                compositionTypeDependencies,
                compositionModuleDependencies
            ),
            new FrontendBrowserViewDefinition(
                "reactContextGraph",
                "React context graph",
                "React provider/consumer context relationships for browser-native context exploration.",
                "react",
                "provider-di",
                "providerTypeDependencies",
                "providerModuleDependencies",
                List.of("providesContext", "consumesContext"),
                providerTypeDependencies,
                providerModuleDependencies
            ),
            new FrontendBrowserViewDefinition(
                "reactHookGraph",
                "React hook graph",
                "React custom-hook usage relationships for browser-native hook exploration.",
                "react",
                "hook",
                "hookTypeDependencies",
                "hookModuleDependencies",
                List.of("usesHook"),
                hookTypeDependencies,
                hookModuleDependencies
            )
        );

        List<Map<String, Object>> views = new ArrayList<>();
        List<String> availableViews = new ArrayList<>();
        for (FrontendBrowserViewDefinition definition : definitions) {
            Map<String, Object> descriptor = definition.toMetadataMap();
            views.add(descriptor);
            if (Boolean.TRUE.equals(descriptor.get("available"))) {
                availableViews.add(definition.id());
            }
        }
        if (availableViews.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("views", List.copyOf(views));
        result.put("availableViews", List.copyOf(availableViews));
        result.put("defaultViewId", availableViews.get(0));
        result.put("description", "Browser-facing frontend graph descriptors derived from existing dependency rollups.");
        return Map.copyOf(result);
    }


    private static Map<String, Object> buildJavaBrowserViews(
        List<Map<String, Object>> endpointTypeDependencies,
        List<Map<String, Object>> endpointModuleDependencies,
        List<Map<String, Object>> entityModelTypeDependencies,
        List<Map<String, Object>> entityModelModuleDependencies,
        List<Map<String, Object>> observerTypeDependencies,
        List<Map<String, Object>> observerModuleDependencies,
        List<Map<String, Object>> writePathTypeDependencies,
        List<Map<String, Object>> writePathModuleDependencies
    ) {
        List<JavaBrowserViewDefinition> definitions = List.of(
            new JavaBrowserViewDefinition(
                "javaEndpointGraph",
                "Java endpoint graph",
                "JAX-RS resource and endpoint relationships prepared for browser-native backend API exploration.",
                "jax-rs",
                "endpoint",
                "endpointTypeDependencies",
                "endpointModuleDependencies",
                List.of("exposesEndpoint", "endpoint"),
                endpointTypeDependencies,
                endpointModuleDependencies
            ),
            new JavaBrowserViewDefinition(
                "javaEntityModelGraph",
                "Java entity model graph",
                "JPA entity, embeddable, inheritance, and association relationships prepared for browser-native persistence-model exploration.",
                "jpa",
                "entity-model",
                "entityModelTypeDependencies",
                "entityModelModuleDependencies",
                List.of("hasAssociation", "embeds", "inheritsPersistenceModel"),
                entityModelTypeDependencies,
                entityModelModuleDependencies
            ),
            new JavaBrowserViewDefinition(
                "javaEventFlowGraph",
                "Java CDI event flow graph",
                "CDI publisher, event, and observer relationships prepared for browser-native asynchronous flow exploration.",
                "cdi",
                "observer-event",
                "observerTypeDependencies",
                "observerModuleDependencies",
                List.of("publishesEvent", "observesEvent", "eventObservedBy"),
                observerTypeDependencies,
                observerModuleDependencies
            ),
            new JavaBrowserViewDefinition(
                "javaWritePathGraph",
                "Java write path graph",
                "Service and repository write-path relationships prepared for browser-native persistence flow exploration.",
                "jpa",
                "write-path",
                "writePathTypeDependencies",
                "writePathModuleDependencies",
                List.of("writePath"),
                writePathTypeDependencies,
                writePathModuleDependencies
            )
        );

        List<Map<String, Object>> views = new ArrayList<>();
        List<String> availableViews = new ArrayList<>();
        for (JavaBrowserViewDefinition definition : definitions) {
            Map<String, Object> descriptor = definition.toMetadataMap();
            views.add(descriptor);
            if (Boolean.TRUE.equals(descriptor.get("available"))) {
                availableViews.add(definition.id());
            }
        }
        if (availableViews.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("views", List.copyOf(views));
        result.put("availableViews", List.copyOf(availableViews));
        result.put("defaultViewId", availableViews.get(0));
        result.put("description", "Browser-facing Java backend graph descriptors derived from framework-aware dependency rollups.");
        result.put("recommendedEntryPoints", List.of(
            "javaEndpointGraph",
            "javaEntityModelGraph",
            "javaEventFlowGraph",
            "javaWritePathGraph"
        ));
        return Map.copyOf(result);
    }

    private static Map<String, Object> buildBrowserViewCatalog(
        Map<String, Object> frontendBrowserViews,
        Map<String, Object> javaBrowserViews
    ) {
        List<Map<String, Object>> groups = new ArrayList<>();
        List<String> availableFamilies = new ArrayList<>();
        addBrowserViewFamily(groups, availableFamilies, "frontend", "Frontend browser views", frontendBrowserViews);
        addBrowserViewFamily(groups, availableFamilies, "java", "Java backend browser views", javaBrowserViews);
        if (availableFamilies.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("families", List.copyOf(groups));
        result.put("availableFamilies", List.copyOf(availableFamilies));
        result.put("defaultFamily", availableFamilies.get(0));
        result.put("description", "High-level browser view families exported with the architecture index document.");
        return Map.copyOf(result);
    }

    private static void addBrowserViewFamily(
        List<Map<String, Object>> groups,
        List<String> availableFamilies,
        String id,
        String title,
        Map<String, Object> browserViews
    ) {
        if (browserViews == null || browserViews.isEmpty()) {
            return;
        }
        List<String> availableViews = stringList(browserViews.get("availableViews"));
        Map<String, Object> group = new LinkedHashMap<>();
        group.put("id", id);
        group.put("title", title);
        group.put("available", !availableViews.isEmpty());
        group.put("availableViewIds", availableViews);
        group.put("defaultViewId", browserViews.get("defaultViewId"));
        group.put("viewCount", ((List<?>) browserViews.getOrDefault("views", List.of())).size());
        group.put("description", browserViews.get("description"));
        groups.add(Map.copyOf(group));
        if (!availableViews.isEmpty()) {
            availableFamilies.add(id);
        }
    }

    private static List<String> distinctStringValues(List<Map<String, Object>> dependencies, String key) {
        if (dependencies == null || dependencies.isEmpty()) {
            return List.of();
        }
        Set<String> values = new LinkedHashSet<>();
        for (Map<String, Object> dependency : dependencies) {
            Object value = dependency.get(key);
            if (value != null) {
                values.add(String.valueOf(value));
            }
        }
        return List.copyOf(values);
    }

    @SafeVarargs
    private static List<String> distinctListValues(String key, List<Map<String, Object>>... dependencyGroups) {
        Set<String> values = new LinkedHashSet<>();
        if (dependencyGroups != null) {
            for (List<Map<String, Object>> group : dependencyGroups) {
                if (group == null) {
                    continue;
                }
                for (Map<String, Object> dependency : group) {
                    values.addAll(stringList(dependency.get(key)));
                }
            }
        }
        return List.copyOf(values);
    }

    @SuppressWarnings("unchecked")
    private static List<String> stringList(Object value) {
        if (value instanceof List<?> list) {
            return list.stream().filter(Objects::nonNull).map(String::valueOf).toList();
        }
        return List.of();
    }

    private static List<Map<String, Object>> filterDependenciesForFrontendBrowserView(
        List<Map<String, Object>> dependencies,
        String framework,
        List<String> frameworkRelationships
    ) {
        if (dependencies == null || dependencies.isEmpty()) {
            return List.of();
        }
        return dependencies.stream()
            .filter(dependency -> {
                List<String> frameworks = stringList(dependency.get("frameworks"));
                if (!"frontend".equals(framework) && !frameworks.contains(framework)) {
                    return false;
                }
                if (frameworkRelationships == null || frameworkRelationships.isEmpty()) {
                    return true;
                }
                List<String> relationships = stringList(dependency.get("frameworkRelationships"));
                return relationships.stream().anyMatch(frameworkRelationships::contains);
            })
            .toList();
    }

    private record FrontendBrowserViewDefinition(
        String id,
        String title,
        String description,
        String framework,
        String architectureViewKind,
        String typeDependencyView,
        String moduleDependencyView,
        List<String> frameworkRelationships,
        List<Map<String, Object>> typeDependencies,
        List<Map<String, Object>> moduleDependencies
    ) {
        private Map<String, Object> toMetadataMap() {
            List<Map<String, Object>> filteredTypeDependencies = filterDependenciesForFrontendBrowserView(typeDependencies, framework, frameworkRelationships);
            List<Map<String, Object>> filteredModuleDependencies = filterDependenciesForFrontendBrowserView(moduleDependencies, framework, frameworkRelationships);
            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("id", id);
            metadata.put("title", title);
            metadata.put("description", description);
            metadata.put("framework", framework);
            metadata.put("architectureViewKind", architectureViewKind);
            metadata.put("typeDependencyView", typeDependencyView);
            metadata.put("moduleDependencyView", moduleDependencyView);
            metadata.put("frameworkRelationships", List.copyOf(frameworkRelationships));
            metadata.put("available", !filteredTypeDependencies.isEmpty() || !filteredModuleDependencies.isEmpty());
            metadata.put("typeDependencyCount", filteredTypeDependencies.size());
            metadata.put("moduleDependencyCount", filteredModuleDependencies.size());
            metadata.put("preferredDependencyView", !filteredTypeDependencies.isEmpty() ? typeDependencyView : moduleDependencyView);
            metadata.put("browserViewKind", "graph");
            metadata.put("recommendedForArchitectureViews", true);
            return Map.copyOf(metadata);
        }
    }

    private record JavaBrowserViewDefinition(
        String id,
        String title,
        String description,
        String framework,
        String architectureViewKind,
        String typeDependencyView,
        String moduleDependencyView,
        List<String> frameworkRelationships,
        List<Map<String, Object>> typeDependencies,
        List<Map<String, Object>> moduleDependencies
    ) {
        private Map<String, Object> toMetadataMap() {
            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("id", id);
            metadata.put("title", title);
            metadata.put("description", description);
            metadata.put("framework", framework);
            metadata.put("architectureViewKind", architectureViewKind);
            metadata.put("typeDependencyView", typeDependencyView);
            metadata.put("moduleDependencyView", moduleDependencyView);
            metadata.put("frameworkRelationships", List.copyOf(frameworkRelationships));
            metadata.put("available", !typeDependencies.isEmpty() || !moduleDependencies.isEmpty());
            metadata.put("typeDependencyCount", typeDependencies.size());
            metadata.put("moduleDependencyCount", moduleDependencies.size());
            metadata.put("preferredDependencyView", !typeDependencies.isEmpty() ? typeDependencyView : moduleDependencyView);
            metadata.put("browserViewKind", "graph");
            metadata.put("recommendedForArchitectureViews", true);
            metadata.put("typeRelationshipKinds", distinctStringValues(typeDependencies, "relationshipKind"));
            metadata.put("moduleRelationshipKinds", distinctStringValues(moduleDependencies, "relationshipKind"));
            metadata.put("availableFrameworks", distinctListValues("frameworks", typeDependencies, moduleDependencies));
            metadata.put("availableArchitectureViewKinds", distinctListValues("architectureViewKinds", typeDependencies, moduleDependencies));
            return Map.copyOf(metadata);
        }
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
        List<Map<String, Object>> packageDependencies,
        List<Map<String, Object>> moduleDependencies
    ) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("typeInternalCount", countBoundary(typeDependencies, "targetBoundary", "internal"));
        summary.put("typeExternalCount", countBoundary(typeDependencies, "targetBoundary", "external"));
        summary.put("packageInternalCount", countBoundary(packageDependencies, "targetBoundary", "internal"));
        summary.put("packageExternalCount", countBoundary(packageDependencies, "targetBoundary", "external"));
        summary.put("moduleInternalCount", countBoundary(moduleDependencies, "targetBoundary", "internal"));
        summary.put("moduleExternalCount", countBoundary(moduleDependencies, "targetBoundary", "external"));
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

    static Map<String, ArchitectureEntity> observedTypesByQualifiedName(Map<String, ArchitectureEntity> entitiesById) {
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
        return kind == RelationshipKind.DEPENDS_ON
            || kind == RelationshipKind.EXTENDS
            || kind == RelationshipKind.IMPLEMENTS
            || kind == RelationshipKind.EXPOSES;
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

    private static boolean isModuleDependencyRelationship(
        ArchitectureRelationship relationship,
        ArchitectureEntity source,
        ArchitectureEntity target
    ) {
        if (source == null || target == null) {
            return false;
        }
        Object rollup = relationship.metadata() == null ? null : relationship.metadata().get("rollup");
        return (relationship.kind() == RelationshipKind.USES || isDependencyRelationship(relationship.kind()))
            && Objects.equals("module-module", rollup)
            && isSourceRootEntity(source)
            && isSourceRootEntity(target);
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

    private static String moduleNameForDependencyEntity(ArchitectureEntity entity) {
        if (entity == null) {
            return null;
        }
        if (isSourceRootEntity(entity)) {
            return entity.name();
        }
        if (entity.metadata() != null) {
            Object explicit = entity.metadata().get("sourceRoot");
            if (explicit instanceof String s && !s.isBlank()) {
                return s;
            }
            Object relativePath = entity.metadata().get("relativePath");
            if (relativePath instanceof String s && !s.isBlank()) {
                String derived = moduleRootFromRelativePath(s);
                if (derived != null) {
                    return derived;
                }
            }
        }
        if (isInternalEntity(entity)) {
            for (SourceReference ref : entity.sourceRefs()) {
                if (ref == null || ref.path() == null || ref.path().isBlank()) {
                    continue;
                }
                String derived = moduleRootFromRelativePath(ref.path());
                if (derived != null) {
                    return derived;
                }
            }
        }
        return packageNameForDependencyEntity(entity);
    }

    private static String moduleBoundaryForName(String moduleName, Map<String, ArchitectureEntity> entitiesById) {
        if (moduleName == null || moduleName.isBlank()) {
            return "unknown";
        }
        return findSourceRootEntityIdByName(moduleName, entitiesById) != null ? "internal" : "external";
    }

    private static String moduleClassificationForName(String moduleName, Map<String, ArchitectureEntity> entitiesById) {
        if (moduleName == null || moduleName.isBlank()) {
            return "unknown";
        }
        return findSourceRootEntityIdByName(moduleName, entitiesById) != null ? "observed-source-root" : "external-module-or-package";
    }

    private static String findSourceRootEntityIdByName(String moduleName, Map<String, ArchitectureEntity> entitiesById) {
        for (ArchitectureEntity entity : entitiesById.values()) {
            if (isSourceRootEntity(entity) && Objects.equals(moduleName, entity.name())) {
                return entity.id();
            }
        }
        return null;
    }

    private static String moduleRootFromRelativePath(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return null;
        }
        String normalized = relativePath.replace('\\', '/');
        String[] parts = normalized.split("/");
        if (parts.length >= 3 && "src".equals(parts[0]) && ("main".equals(parts[1]) || "test".equals(parts[1]))) {
            return parts[0] + "/" + parts[1] + "/" + parts[2];
        }
        return parts.length > 0 ? parts[0] : null;
    }

    private static boolean isSourceRootEntity(ArchitectureEntity entity) {
        return entity != null
            && entity.kind() == EntityKind.MODULE
            && entity.metadata() != null
            && Objects.equals("source-root", entity.metadata().get("logicalRole"));
    }

    private static boolean isPackageEntity(ArchitectureEntity entity) {
        return entity != null
            && entity.kind() == EntityKind.MODULE
            && entity.metadata() != null
            && Objects.equals("package", entity.metadata().get("logicalRole"));
    }

    private static boolean isTypeEntity(ArchitectureEntity entity) {
        if (entity == null) {
            return false;
        }
        if (entity.kind() == EntityKind.CLASS || entity.kind() == EntityKind.INTERFACE || entity.kind() == EntityKind.ENDPOINT) {
            return true;
        }
        if (entity.kind() == EntityKind.FUNCTION || entity.kind() == EntityKind.UI_MODULE) {
            String language = stringMetadata(entity, "language", "");
            if ("typescript".equalsIgnoreCase(language)) {
                return true;
            }
            if (entity.metadata() != null && entity.metadata().get("framework") != null) {
                return true;
            }
        }
        if (entity.kind() == EntityKind.MODULE && entity.metadata() != null) {
            String targetClassification = stringMetadata(entity, "targetClassification", "");
            if ("angular-di-token".equals(targetClassification)) {
                return true;
            }
            if (Boolean.TRUE.equals(entity.metadata().get("angularToken"))) {
                return true;
            }
            if (Boolean.TRUE.equals(entity.metadata().get("angularDiValue"))) {
                return true;
            }
        }
        return false;
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
        private final Set<String> frameworks = new LinkedHashSet<>();
        private final Set<String> frameworkRelationships = new LinkedHashSet<>();
        private final Set<String> architectureViewKinds = new LinkedHashSet<>();
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
                addFrameworkMetadata(frameworks, frameworkRelationships, architectureViewKinds, relationship);
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
            metadata.put("frameworks", List.copyOf(frameworks));
            metadata.put("frameworkRelationships", List.copyOf(frameworkRelationships));
            metadata.put("architectureViewKinds", List.copyOf(architectureViewKinds));
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
        private final Set<String> frameworks = new LinkedHashSet<>();
        private final Set<String> frameworkRelationships = new LinkedHashSet<>();
        private final Set<String> architectureViewKinds = new LinkedHashSet<>();
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
                addFrameworkMetadata(frameworks, frameworkRelationships, architectureViewKinds, relationship);
            }
        }

        private Map<String, Object> toMetadataMap() {
            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("sourcePackageName", sourcePackageName);
            metadata.put("targetPackageName", targetPackageName);
            metadata.put("relationshipKind", relationshipKind.name());
            metadata.put("dependencySources", List.copyOf(dependencySources));
            metadata.put("dependencyCategories", List.copyOf(dependencyCategories));
            metadata.put("frameworks", List.copyOf(frameworks));
            metadata.put("frameworkRelationships", List.copyOf(frameworkRelationships));
            metadata.put("architectureViewKinds", List.copyOf(architectureViewKinds));
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

    private static final class NormalizedModuleDependency {
        private final String sourceModuleName;
        private final String targetModuleName;
        private final RelationshipKind relationshipKind;
        private final boolean internalTarget;
        private final boolean externalTarget;
        private final String sourceBoundary;
        private final String targetBoundary;
        private final String targetModuleClassification;
        private final boolean sameModule;
        private final Set<String> dependencySources = new LinkedHashSet<>();
        private final Set<String> dependencyCategories = new LinkedHashSet<>();
        private final Set<String> frameworks = new LinkedHashSet<>();
        private final Set<String> frameworkRelationships = new LinkedHashSet<>();
        private final Set<String> architectureViewKinds = new LinkedHashSet<>();
        private final Set<String> evidenceRelationshipIds = new LinkedHashSet<>();
        private final Set<String> evidenceLabels = new LinkedHashSet<>();
        private final Set<String> sourceTypeIds = new LinkedHashSet<>();
        private final Set<String> targetTypeIds = new LinkedHashSet<>();

        private NormalizedModuleDependency(
            String sourceModuleName,
            String targetModuleName,
            RelationshipKind relationshipKind,
            boolean internalTarget,
            boolean externalTarget,
            String sourceBoundary,
            String targetBoundary,
            String targetModuleClassification,
            boolean sameModule
        ) {
            this.sourceModuleName = sourceModuleName;
            this.targetModuleName = targetModuleName;
            this.relationshipKind = relationshipKind;
            this.internalTarget = internalTarget;
            this.externalTarget = externalTarget;
            this.sourceBoundary = sourceBoundary;
            this.targetBoundary = targetBoundary;
            this.targetModuleClassification = targetModuleClassification;
            this.sameModule = sameModule;
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
                addFrameworkMetadata(frameworks, frameworkRelationships, architectureViewKinds, relationship);
            }
        }

        private Map<String, Object> toMetadataMap() {
            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("sourceModuleName", sourceModuleName);
            metadata.put("targetModuleName", targetModuleName);
            metadata.put("relationshipKind", relationshipKind.name());
            metadata.put("dependencySources", List.copyOf(dependencySources));
            metadata.put("dependencyCategories", List.copyOf(dependencyCategories));
            metadata.put("frameworks", List.copyOf(frameworks));
            metadata.put("frameworkRelationships", List.copyOf(frameworkRelationships));
            metadata.put("architectureViewKinds", List.copyOf(architectureViewKinds));
            metadata.put("sourceBoundary", sourceBoundary);
            metadata.put("targetBoundary", targetBoundary);
            metadata.put("targetModuleClassification", targetModuleClassification);
            metadata.put("internalTarget", internalTarget);
            metadata.put("externalTarget", externalTarget);
            metadata.put("sameModule", sameModule);
            metadata.put("underlyingRelationshipCount", evidenceRelationshipIds.size());
            metadata.put("sourceTypeCount", sourceTypeIds.size());
            metadata.put("targetTypeCount", targetTypeIds.size());
            metadata.put("evidenceRelationshipIds", List.copyOf(evidenceRelationshipIds));
            metadata.put("evidenceLabels", List.copyOf(evidenceLabels));
            return Map.copyOf(metadata);
        }
    }

    private static final class EvidenceDependency {
        private final String sourceEntityId;
        private final String targetEntityId;
        private final RelationshipKind relationshipKind;
        private final String sourceName;
        private final String targetName;
        private final String sourceBoundary;
        private final String targetBoundary;
        private final String targetClassification;
        private final Set<String> dependencySources = new LinkedHashSet<>();
        private final Set<String> dependencyCategories = new LinkedHashSet<>();
        private final Set<String> frameworks = new LinkedHashSet<>();
        private final Set<String> frameworkRelationships = new LinkedHashSet<>();
        private final Set<String> architectureViewKinds = new LinkedHashSet<>();
        private final Set<String> evidenceRelationshipIds = new LinkedHashSet<>();
        private final Set<String> evidenceLabels = new LinkedHashSet<>();

        private EvidenceDependency(
            String sourceEntityId,
            String targetEntityId,
            RelationshipKind relationshipKind,
            String sourceName,
            String targetName,
            String sourceBoundary,
            String targetBoundary,
            String targetClassification
        ) {
            this.sourceEntityId = sourceEntityId;
            this.targetEntityId = targetEntityId;
            this.relationshipKind = relationshipKind;
            this.sourceName = sourceName;
            this.targetName = targetName;
            this.sourceBoundary = sourceBoundary;
            this.targetBoundary = targetBoundary;
            this.targetClassification = targetClassification;
        }

        private void addEvidence(ArchitectureRelationship relationship) {
            Object dependencySource = relationship.metadata() == null ? null : relationship.metadata().get("dependencySource");
            if (dependencySource instanceof String s && !s.isBlank()) {
                dependencySources.add(s);
            }
            Object dependencyCategory = relationship.metadata() == null ? null : relationship.metadata().get("dependencyCategory");
            if (dependencyCategory instanceof String s && !s.isBlank()) {
                dependencyCategories.add(s);
            }
            addFrameworkMetadata(frameworks, frameworkRelationships, architectureViewKinds, relationship);
            evidenceRelationshipIds.add(relationship.id());
            if (relationship.label() != null && !relationship.label().isBlank()) {
                evidenceLabels.add(relationship.label());
            }
        }

        private Map<String, Object> toMetadataMap() {
            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("sourceEntityId", sourceEntityId);
            metadata.put("targetEntityId", targetEntityId);
            metadata.put("relationshipKind", relationshipKind.name());
            metadata.put("sourceName", sourceName);
            metadata.put("targetName", targetName);
            metadata.put("sourceBoundary", sourceBoundary);
            metadata.put("targetBoundary", targetBoundary);
            metadata.put("targetClassification", targetClassification);
            metadata.put("dependencySources", List.copyOf(dependencySources));
            metadata.put("dependencyCategories", List.copyOf(dependencyCategories));
            metadata.put("frameworks", List.copyOf(frameworks));
            metadata.put("frameworkRelationships", List.copyOf(frameworkRelationships));
            metadata.put("architectureViewKinds", List.copyOf(architectureViewKinds));
            metadata.put("underlyingRelationshipCount", evidenceRelationshipIds.size());
            metadata.put("evidenceRelationshipIds", List.copyOf(evidenceRelationshipIds));
            metadata.put("evidenceLabels", List.copyOf(evidenceLabels));
            metadata.put("dependencyTier", "supporting-evidence");
            metadata.put("architecturePrimary", false);
            metadata.put("recommendedForArchitectureViews", false);
            metadata.put("evidenceKind", "file-import");
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

    private static List<String> defaultCompletenessNotes(
        StructuralExtractionResult extractionResult,
        InterpretationResult interpretationResult,
        TopologyResult topologyResult
    ) {
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
        return List.copyOf(completenessNotes);
    }

    public static ArchitectureIndexDocument createPlaceholderDocument(RepositorySource source, String indexerVersion) {
        return createInventoryDocument(
            source,
            indexerVersion,
            new FileInventory(List.of(), 0, 0, 0, java.util.Set.of(), java.util.Set.of()),
            List.of()
        );
    }

    static String normalizeScopeId(String scopeId, String repositoryScopeId) {
        if (scopeId == null || scopeId.isBlank()) {
            return repositoryScopeId;
        }
        return scopeId;
    }
}
