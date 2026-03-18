package info.isaksson.erland.architecturebrowser.indexer.ir;

import info.isaksson.erland.architecturebrowser.indexer.ir.json.ArchitectureIrJson;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureEntity;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureIndexDocument;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureRelationship;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureViewpoint;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ViewpointScenarioExamplesContractTest {

    private static final Path EXAMPLE_DIR = Path.of("src/test/resources/export-contract");
    private static final Path DOC_EXAMPLE_DIR = Path.of("docs/export-format/examples");

    @Test
    void restPersistenceScenarioShowsGroundedApiRequestAndPersistenceSemantics() throws IOException {
        ArchitectureIndexDocument document = readExample("java-rest-persistence-export.json");
        assertValid(document, "java-rest-persistence-export.json");

        Map<String, ArchitectureViewpoint> viewpointsById = viewpointsById(document);
        assertEquals("available", viewpoint(viewpointsById, "api-surface").availability());
        assertEquals("available", viewpoint(viewpointsById, "request-handling").availability());
        assertEquals("available", viewpoint(viewpointsById, "persistence-model").availability());

        Map<String, ArchitectureEntity> entitiesById = entitiesById(document);
        assertEntityRole(entitiesById, "entity:class:order-resource", "api-entrypoint");
        assertEntityTrait(entitiesById, "entity:class:order-resource", "externally-exposed");
        assertEntityRole(entitiesById, "entity:class:order-service", "application-service");
        assertEntityRole(entitiesById, "entity:class:order-repository", "persistence-access");
        assertEntityRole(entitiesById, "entity:class:order-entity", "persistent-entity");
        assertEntityTrait(entitiesById, "entity:class:order-entity", "persistent");

        Map<String, ArchitectureRelationship> relationshipsById = relationshipsById(document);
        assertRelationshipSemantic(relationshipsById, "rel:resource:exposes:endpoint", "serves-request");
        assertRelationshipSemantic(relationshipsById, "rel:resource:uses:service", "invokes-use-case");
        assertRelationshipSemantic(relationshipsById, "rel:service:uses:repository", "accesses-persistence");
    }

    @Test
    void persistenceOnlyScenarioDoesNotOverclaimApiOrRequestHandling() throws IOException {
        ArchitectureIndexDocument document = readExample("java-persistence-only-export.json");
        assertValid(document, "java-persistence-only-export.json");

        Map<String, ArchitectureViewpoint> viewpointsById = viewpointsById(document);
        assertEquals("unavailable", viewpoint(viewpointsById, "api-surface").availability());
        assertEquals("unavailable", viewpoint(viewpointsById, "request-handling").availability());
        assertEquals("available", viewpoint(viewpointsById, "persistence-model").availability());

        Map<String, ArchitectureEntity> entitiesById = entitiesById(document);
        assertEntityRole(entitiesById, "entity:class:reconciliation-service", "application-service");
        assertEntityRole(entitiesById, "entity:class:ledger-entry-repository", "persistence-access");
        assertEntityRole(entitiesById, "entity:class:ledger-entry", "persistent-entity");
        assertEntityTrait(entitiesById, "entity:class:ledger-entry", "persistent");

        Map<String, ArchitectureRelationship> relationshipsById = relationshipsById(document);
        assertRelationshipSemantic(relationshipsById, "rel:service:uses:repository", "accesses-persistence");
        assertRelationshipSemantic(relationshipsById, "rel:entity:stored-in:db", "stored-in");
    }

    @Test
    void externalIntegrationScenarioShowsGroundedIntegrationViewpoint() throws IOException {
        ArchitectureIndexDocument document = readExample("java-external-integration-export.json");
        assertValid(document, "java-external-integration-export.json");

        Map<String, ArchitectureViewpoint> viewpointsById = viewpointsById(document);
        assertEquals("available", viewpoint(viewpointsById, "api-surface").availability());
        assertEquals("available", viewpoint(viewpointsById, "request-handling").availability());
        assertEquals("available", viewpoint(viewpointsById, "integration-map").availability());

        Map<String, ArchitectureEntity> entitiesById = entitiesById(document);
        assertEntityRole(entitiesById, "entity:class:customer-resource", "api-entrypoint");
        assertEntityTrait(entitiesById, "entity:class:customer-resource", "externally-exposed");
        assertEntityRole(entitiesById, "entity:class:customer-service", "application-service");
        assertEntityRole(entitiesById, "entity:class:crm-client", "integration-adapter");

        Map<String, ArchitectureRelationship> relationshipsById = relationshipsById(document);
        assertRelationshipSemantic(relationshipsById, "rel:resource:exposes:endpoint", "serves-request");
        assertRelationshipSemantic(relationshipsById, "rel:resource:uses:service", "invokes-use-case");
        assertRelationshipSemantic(relationshipsById, "rel:service:uses:crm-client", "calls-external-system");
        assertRelationshipSemantic(relationshipsById, "rel:crm-client:calls:crm", "calls-external-system");
    }

    @Test
    void scenarioExamplesAreMirroredIntoDocumentationDirectory() throws IOException {
        List<String> scenarioFiles = List.of(
            "java-rest-persistence-export.json",
            "java-persistence-only-export.json",
            "java-external-integration-export.json"
        );

        for (String fileName : scenarioFiles) {
            String testResource = Files.readString(EXAMPLE_DIR.resolve(fileName));
            String docResource = Files.readString(DOC_EXAMPLE_DIR.resolve(fileName));
            assertEquals(testResource, docResource, () -> fileName + " should stay identical in docs and test resources");
        }
    }

    private static ArchitectureIndexDocument readExample(String fileName) throws IOException {
        return ArchitectureIrJson.fromJson(Files.readString(EXAMPLE_DIR.resolve(fileName)));
    }

    private static void assertValid(ArchitectureIndexDocument document, String fileName) {
        ArchitectureIrValidator.ValidationResult validation = ArchitectureIrValidator.validate(document);
        assertTrue(validation.isValid(), () -> fileName + " should validate: " + validation.messages());
    }

    private static Map<String, ArchitectureViewpoint> viewpointsById(ArchitectureIndexDocument document) {
        assertNotNull(document.viewpoints(), "expected viewpoints");
        return document.viewpoints().stream().collect(Collectors.toMap(ArchitectureViewpoint::id, Function.identity()));
    }

    private static Map<String, ArchitectureEntity> entitiesById(ArchitectureIndexDocument document) {
        return document.entities().stream().collect(Collectors.toMap(ArchitectureEntity::id, Function.identity()));
    }

    private static Map<String, ArchitectureRelationship> relationshipsById(ArchitectureIndexDocument document) {
        return document.relationships().stream().collect(Collectors.toMap(ArchitectureRelationship::id, Function.identity()));
    }

    private static ArchitectureViewpoint viewpoint(Map<String, ArchitectureViewpoint> viewpointsById, String viewpointId) {
        ArchitectureViewpoint viewpoint = viewpointsById.get(viewpointId);
        assertNotNull(viewpoint, () -> "missing viewpoint: " + viewpointId);
        return viewpoint;
    }

    private static void assertEntityRole(Map<String, ArchitectureEntity> entitiesById, String entityId, String role) {
        ArchitectureEntity entity = entitiesById.get(entityId);
        assertNotNull(entity, () -> "missing entity: " + entityId);
        assertContains(entity.architecturalRoles(), role, entityId + " should contain architectural role " + role);
    }

    private static void assertEntityTrait(Map<String, ArchitectureEntity> entitiesById, String entityId, String trait) {
        ArchitectureEntity entity = entitiesById.get(entityId);
        assertNotNull(entity, () -> "missing entity: " + entityId);
        assertContains(entity.architecturalTraits(), trait, entityId + " should contain architectural trait " + trait);
    }

    private static void assertRelationshipSemantic(Map<String, ArchitectureRelationship> relationshipsById, String relationshipId, String semantic) {
        ArchitectureRelationship relationship = relationshipsById.get(relationshipId);
        assertNotNull(relationship, () -> "missing relationship: " + relationshipId);
        assertContains(relationship.architecturalSemantics(), semantic, relationshipId + " should contain architectural semantic " + semantic);
    }

    private static void assertContains(List<String> values, String expected, String message) {
        assertNotNull(values, message + " (list was null)");
        assertTrue(values.contains(expected), message + "; actual=" + values);
    }
}
