package info.isaksson.erland.architecturebrowser.indexer.regression;

import info.isaksson.erland.architecturebrowser.indexer.extract.StructuralExtractionService;
import info.isaksson.erland.architecturebrowser.indexer.extract.StructuralExtractorRegistry;
import info.isaksson.erland.architecturebrowser.indexer.extract.model.StructuralExtractionResult;
import info.isaksson.erland.architecturebrowser.indexer.interpret.InterpretationRegistry;
import info.isaksson.erland.architecturebrowser.indexer.interpret.InterpretationService;
import info.isaksson.erland.architecturebrowser.indexer.interpret.model.InterpretationResult;
import info.isaksson.erland.architecturebrowser.indexer.ir.ArchitectureIrFactory;
import info.isaksson.erland.architecturebrowser.indexer.ir.ArchitectureIrValidator;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureEntity;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureIndexDocument;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureRelationship;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RepositorySource;
import info.isaksson.erland.architecturebrowser.indexer.parse.ParseBatchResult;
import info.isaksson.erland.architecturebrowser.indexer.parse.ParseStatus;
import info.isaksson.erland.architecturebrowser.indexer.parse.ParserRegistry;
import info.isaksson.erland.architecturebrowser.indexer.parse.SourceParseResult;
import info.isaksson.erland.architecturebrowser.indexer.parse.TreeSitterConfiguration;
import info.isaksson.erland.architecturebrowser.indexer.parse.TreeSitterParserRegistryFactory;
import info.isaksson.erland.architecturebrowser.indexer.parse.TreeSitterParsingService;
import info.isaksson.erland.architecturebrowser.indexer.scan.FileInventory;
import info.isaksson.erland.architecturebrowser.indexer.scan.FileInventoryScanner;
import info.isaksson.erland.architecturebrowser.indexer.scan.InventoryScanOptions;
import info.isaksson.erland.architecturebrowser.indexer.topology.TopologyService;
import info.isaksson.erland.architecturebrowser.indexer.topology.model.TopologyResult;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaJpaRelationshipNormalizationFixtureRegressionTest {

    @Test
    void exportsCanonicalNormalizedAssociationsForRepresentativeJpaDomains() {
        ArchitectureIndexDocument document = buildDocumentFromFixture();
        assertTrue(ArchitectureIrValidator.validate(document).isValid());

        ArchitectureEntity project = entityByQualifiedName(document, "com.example.work.domain.Project");
        ArchitectureEntity task = entityByQualifiedName(document, "com.example.work.domain.Task");
        ArchitectureEntity taskDetails = entityByQualifiedName(document, "com.example.work.domain.TaskDetails");
        ArchitectureEntity team = entityByQualifiedName(document, "com.example.work.domain.Team");
        ArchitectureEntity userAccount = entityByQualifiedName(document, "com.example.work.domain.UserAccount");
        ArchitectureEntity board = entityByQualifiedName(document, "com.example.work.domain.Board");
        ArchitectureEntity column = entityByQualifiedName(document, "com.example.work.domain.ColumnEntity");
        ArchitectureEntity workOrder = entityByQualifiedName(document, "com.example.work.domain.WorkOrder");
        ArchitectureEntity addressValue = entityByQualifiedName(document, "com.example.work.domain.AddressValue");

        ArchitectureRelationship tasksAssociation = normalizedAssociationByMembers(document, "project", "tasks");
        assertEquals(project.id(), tasksAssociation.fromEntityId());
        assertEquals(task.id(), tasksAssociation.toEntityId());
        assertEquals("merged-bidirectional-peer-association", tasksAssociation.metadata().get("jpaAssociationHandling"));
        assertEquals("containment", tasksAssociation.normalizedAssociation().associationKind());
        assertEquals("one-to-many", tasksAssociation.normalizedAssociation().associationCardinality());
        assertEquals(Boolean.TRUE, tasksAssociation.normalizedAssociation().bidirectional());
        assertEquals(2, tasksAssociation.normalizedAssociation().evidenceRelationshipIds().size());
        assertEquals("1", tasksAssociation.normalizedAssociation().sourceLowerBound());
        assertEquals("1", tasksAssociation.normalizedAssociation().sourceUpperBound());
        assertEquals("0", tasksAssociation.normalizedAssociation().targetLowerBound());
        assertEquals("*", tasksAssociation.normalizedAssociation().targetUpperBound());

        ArchitectureRelationship archivedTasksAssociation = normalizedAssociationByMembers(document, "archivedFromProject", "archivedTasks");
        assertEquals(project.id(), archivedTasksAssociation.fromEntityId());
        assertEquals(task.id(), archivedTasksAssociation.toEntityId());
        assertEquals("merged-bidirectional-peer-association", archivedTasksAssociation.metadata().get("jpaAssociationHandling"));
        assertEquals("association", archivedTasksAssociation.normalizedAssociation().associationKind());
        assertEquals(2, archivedTasksAssociation.normalizedAssociation().evidenceRelationshipIds().size());

        ArchitectureRelationship taskDetailsAssociation = normalizedAssociationByMembers(document, "task", "details");
        assertNotNull(taskDetailsAssociation.normalizedAssociation());
        assertEquals("one-to-one", taskDetailsAssociation.normalizedAssociation().associationCardinality());
        assertEquals("containment", taskDetailsAssociation.normalizedAssociation().associationKind());
        assertEquals(Boolean.TRUE, taskDetailsAssociation.normalizedAssociation().bidirectional());

        ArchitectureRelationship teamMembersAssociation = normalizedAssociationByMembers(document, "members", "teams");
        assertEquals(team.id(), teamMembersAssociation.fromEntityId());
        assertEquals(userAccount.id(), teamMembersAssociation.toEntityId());
        assertNotNull(teamMembersAssociation.normalizedAssociation());
        assertEquals("many-to-many", teamMembersAssociation.normalizedAssociation().associationCardinality());
        assertEquals("association", teamMembersAssociation.normalizedAssociation().associationKind());
        assertEquals(Boolean.TRUE, teamMembersAssociation.normalizedAssociation().bidirectional());

        ArchitectureRelationship boardColumnsAssociation = normalizedAssociationByMembers(document, "columns", null);
        assertEquals(board.id(), boardColumnsAssociation.fromEntityId());
        assertEquals(column.id(), boardColumnsAssociation.toEntityId());
        assertNotNull(boardColumnsAssociation.normalizedAssociation());
        assertEquals(Boolean.FALSE, boardColumnsAssociation.normalizedAssociation().bidirectional());
        assertEquals("unidirectional-peer-association", boardColumnsAssociation.metadata().get("jpaAssociationHandling"));

        assertTrue(document.relationships().stream().anyMatch(rel ->
                addressValue.id().equals(rel.toEntityId())
                    && "embedded-value".equals(rel.metadata().get("jpaAssociationHandling"))
                    && rel.normalizedAssociation() == null),
            () -> "Expected embedded-value relationship to remain outside peer normalization. Relationships=" + describeRelationships(document));

        assertTrue(document.relationships().stream().anyMatch(rel ->
                workOrder.id().equals(rel.fromEntityId())
                    && "value-collection".equals(rel.metadata().get("jpaAssociationHandling"))
                    && rel.normalizedAssociation() == null),
            () -> "Expected value-collection relationship to remain outside peer normalization. Relationships=" + describeRelationships(document));
    }

    @Test
    @SuppressWarnings("unchecked")
    void exportsRepresentativeRelationshipCatalogForPlatformConsumers() {
        ArchitectureIndexDocument document = buildDocumentFromFixture();
        assertTrue(ArchitectureIrValidator.validate(document).isValid());

        Map<String, Object> metadata = document.metadata();
        Map<String, Object> dependencyViews = (Map<String, Object>) metadata.get("dependencyViews");
        assertNotNull(dependencyViews);

        List<Map<String, Object>> entityAssociationRelationships = (List<Map<String, Object>>) dependencyViews.get("entityAssociationRelationships");
        assertNotNull(entityAssociationRelationships);
        assertEquals(5, entityAssociationRelationships.size(), () -> "Unexpected entityAssociationRelationships=" + entityAssociationRelationships);

        Set<String> owningMembers = entityAssociationRelationships.stream()
            .map(entry -> String.valueOf(entry.get("owningSideMemberId")))
            .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<String> inverseMembers = entityAssociationRelationships.stream()
            .map(entry -> String.valueOf(entry.get("inverseSideMemberId")))
            .collect(Collectors.toCollection(LinkedHashSet::new));
        assertTrue(owningMembers.containsAll(Set.of("project", "archivedFromProject", "task", "members", "columns")));
        assertTrue(inverseMembers.containsAll(Set.of("tasks", "archivedTasks", "details", "teams")));

        assertTrue(entityAssociationRelationships.stream().anyMatch(entry ->
            "containment".equals(entry.get("associationKind")) && "tasks".equals(entry.get("inverseSideMemberId"))));
        assertTrue(entityAssociationRelationships.stream().anyMatch(entry ->
            "association".equals(entry.get("associationKind")) && "teams".equals(entry.get("inverseSideMemberId"))));
        assertTrue(entityAssociationRelationships.stream().anyMatch(entry ->
            Boolean.FALSE.equals(entry.get("bidirectional")) && "columns".equals(entry.get("owningSideMemberId"))));
        assertFalse(entityAssociationRelationships.stream().anyMatch(entry -> "shippingAddress".equals(entry.get("owningSideMemberId"))));
        assertFalse(entityAssociationRelationships.stream().anyMatch(entry -> "tags".equals(entry.get("owningSideMemberId"))));

        Map<String, Object> relationshipCatalogs = (Map<String, Object>) dependencyViews.get("relationshipCatalogs");
        assertNotNull(relationshipCatalogs);
        Map<String, Object> entityAssociations = (Map<String, Object>) relationshipCatalogs.get("entityAssociations");
        assertNotNull(entityAssociations);
        assertEquals("entityAssociationRelationships", entityAssociations.get("id"));
        assertEquals(Boolean.TRUE, entityAssociations.get("available"));
        assertEquals(Integer.valueOf(5), entityAssociations.get("relationshipCount"));

        Map<String, Object> javaBrowserViews = (Map<String, Object>) dependencyViews.get("javaBrowserViews");
        assertNotNull(javaBrowserViews);
        List<Map<String, Object>> views = (List<Map<String, Object>>) javaBrowserViews.get("views");
        assertTrue(views.stream().anyMatch(view ->
            "javaEntityModelGraph".equals(view.get("id"))
                && "entityAssociationRelationships".equals(view.get("relationshipCatalogView"))
                && "entityAssociationRelationships".equals(view.get("preferredDependencyView"))));

        assertTrue(document.viewpoints().stream().anyMatch(viewpoint ->
            "persistence-model".equals(viewpoint.id())
                && viewpoint.preferredDependencyViews() != null
                && viewpoint.preferredDependencyViews().contains("entityAssociationRelationships")));
    }

    private static ArchitectureRelationship normalizedAssociationByMembers(ArchitectureIndexDocument document, String owningMember, String inverseMember) {
        return document.relationships().stream()
            .filter(rel -> rel.normalizedAssociation() != null)
            .filter(rel -> owningMember.equals(rel.normalizedAssociation().owningSideMemberId()))
            .filter(rel -> inverseMember == null || inverseMember.equals(rel.normalizedAssociation().inverseSideMemberId()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Missing normalized relationship owningMember=" + owningMember + " inverseMember=" + inverseMember + " in " + describeRelationships(document)));
    }

    private static ArchitectureEntity entityByQualifiedName(ArchitectureIndexDocument document, String qualifiedName) {
        return document.entities().stream()
            .filter(entity -> qualifiedName.equals(entity.metadata().get("qualifiedName")))
            .findFirst()
            .orElseThrow();
    }

    private static ArchitectureIndexDocument buildDocumentFromFixture() {
        Path fixtureRoot = Path.of("src/test/resources/fixtures/java/java-jpa-relationship-normalization");
        FileInventory inventory = new FileInventoryScanner().scan(fixtureRoot, InventoryScanOptions.defaults());

        TreeSitterConfiguration configuration = TreeSitterConfiguration.fromEnvironment();
        ParserRegistry parserRegistry = TreeSitterParserRegistryFactory.createDefaultRegistry(configuration);
        ParseBatchResult parseBatch = new TreeSitterParsingService(parserRegistry).parseInventory(fixtureRoot, inventory);

        assertTrue(parseBatch.results().stream().allMatch(result -> result.status() == ParseStatus.SUCCESS),
            () -> "Expected successful parse for JPA relationship fixture. Results=" + summarizeFailures(parseBatch));
        assertTrue(parseBatch.results().stream().allMatch(JavaJpaRelationshipNormalizationFixtureRegressionTest::usesRealTreeSitterBackend),
            () -> "Expected real Tree-sitter backend for JPA relationship fixture. Results=" + parseBatch.results());

        StructuralExtractionResult extraction = new StructuralExtractionService(StructuralExtractorRegistry.defaultRegistry()).extract(parseBatch);
        InterpretationResult interpretation = new InterpretationService(InterpretationRegistry.defaultRegistry()).interpret(extraction);
        TopologyResult topology = new TopologyService().infer(inventory, extraction, interpretation);

        return ArchitectureIrFactory.createInventoryDocument(
            RepositorySource.localPath("fixture", fixtureRoot.toAbsolutePath().toString(), Instant.parse("2026-04-07T12:00:00Z")),
            "0.1.0-SNAPSHOT",
            inventory,
            List.of(),
            parseBatch,
            extraction,
            interpretation,
            topology
        );
    }

    private static boolean usesRealTreeSitterBackend(SourceParseResult result) {
        return "tree-sitter-jtreesitter".equals(String.valueOf(result.metadata().get("parserBackend")));
    }

    private static String summarizeFailures(ParseBatchResult parseBatch) {
        return parseBatch.results().stream()
            .filter(result -> result.status() != ParseStatus.SUCCESS)
            .map(result -> result.request().relativePath() + ":" + result.status() + " metadata=" + result.metadata() + " issues=" + result.issues())
            .toList()
            .toString();
    }

    private static String describeRelationships(ArchitectureIndexDocument document) {
        return document.relationships().stream()
            .map(rel -> rel.id() + " " + rel.fromEntityId() + "->" + rel.toEntityId() + " label=" + rel.label() + " normalized=" + rel.normalizedAssociation() + " metadata=" + rel.metadata())
            .toList()
            .toString();
    }
}
