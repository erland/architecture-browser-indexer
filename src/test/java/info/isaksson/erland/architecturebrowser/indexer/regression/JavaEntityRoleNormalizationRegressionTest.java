package info.isaksson.erland.architecturebrowser.indexer.regression;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureEntity;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureIndexDocument;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaEntityRoleNormalizationRegressionTest {

    @Test
    void backendFixtureExportsCanonicalJavaRolesAndTraits() {
        ArchitectureIndexDocument document = JavaBackendArchitectureFixtureTestData.buildDocumentFromFixture();

        ArchitectureEntity resource = entityByQualifiedName(document, "com.example.orders.api.OrderResource");
        assertEquals(java.util.List.of("api-entrypoint"), resource.architecturalRoles());
        assertEquals(java.util.List.of("externally-exposed"), resource.architecturalTraits());

        ArchitectureEntity service = entityByQualifiedName(document, "com.example.orders.service.OrderService");
        assertEquals(java.util.List.of("application-service"), service.architecturalRoles());

        ArchitectureEntity repository = entityByQualifiedName(document, "com.example.orders.repo.OrderRepository");
        assertEquals(java.util.List.of("persistence-access"), repository.architecturalRoles());

        ArchitectureEntity orderEntity = entityByQualifiedName(document, "com.example.orders.domain.OrderEntity");
        assertEquals(java.util.List.of("persistent-entity"), orderEntity.architecturalRoles());
        assertEquals(java.util.List.of("persistent"), orderEntity.architecturalTraits());

        ArchitectureEntity addressValue = entityByQualifiedName(document, "com.example.orders.domain.AddressValue");
        assertTrue(addressValue.architecturalRoles() == null || addressValue.architecturalRoles().isEmpty(),
            () -> "Embeddable value objects should stay conservative in Step 5. Roles=" + addressValue.architecturalRoles());
    }

    private static ArchitectureEntity entityByQualifiedName(ArchitectureIndexDocument document, String qualifiedName) {
        return document.entities().stream()
            .filter(entity -> qualifiedName.equals(entity.metadata().get("qualifiedName")))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Missing entity for qualifiedName=" + qualifiedName));
    }
}
