package info.isaksson.erland.architecturebrowser.indexer.ir;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureEntity;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityOrigin;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArchitectureEntityNormalizedContractFieldsTest {

    @Test
    void architecturalRolesAndTraitsAreCanonicalizedForDeterministicOutput() {
        ArchitectureEntity entity = new ArchitectureEntity(
            "entity:order-service",
            EntityKind.CLASS,
            EntityOrigin.OBSERVED,
            "OrderService",
            "OrderService",
            "scope:repo",
            List.of(),
            Map.of(),
            List.of("application-service", " application-service ", "api-entrypoint"),
            List.of(" stateless ", "transactional", "stateless")
        );

        assertEquals(List.of("api-entrypoint", "application-service"), entity.architecturalRoles());
        assertEquals(List.of("stateless", "transactional"), entity.architecturalTraits());
    }

    @Test
    void legacyConstructorKeepsNormalizedFieldsOptional() {
        ArchitectureEntity entity = new ArchitectureEntity(
            "entity:order-service",
            EntityKind.CLASS,
            EntityOrigin.OBSERVED,
            "OrderService",
            "OrderService",
            "scope:repo",
            List.of(),
            Map.of()
        );

        assertNull(entity.architecturalRoles());
        assertNull(entity.architecturalTraits());
    }

    @Test
    void validatorRejectsBlankAndDuplicateNormalizedFieldsWhenPresent() {
        ArchitectureEntity entity = new ArchitectureEntity(
            "entity:order-service",
            EntityKind.CLASS,
            EntityOrigin.OBSERVED,
            "OrderService",
            "OrderService",
            "scope:repo",
            List.of(),
            Map.of(),
            List.of("application-service", "application-service", " "),
            List.of("transactional", "transactional")
        );

        var result = ArchitectureIrValidator.validate(new info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureIndexDocument(
            ArchitectureIrVersions.CURRENT_SCHEMA_VERSION,
            "0.1.0-SNAPSHOT",
            TestArchitectureDocuments.runMetadata(),
            TestArchitectureDocuments.repositorySource(),
            List.of(),
            List.of(entity),
            List.of(),
            List.of(),
            TestArchitectureDocuments.completeness(),
            Map.of()
        ));

        assertTrue(result.isValid(), () -> "canonical constructor should strip blank/duplicate normalized values before validation: " + result.messages());
    }
}
