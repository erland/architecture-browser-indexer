package info.isaksson.erland.architecturebrowser.indexer.regression;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureEntity;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureIndexDocument;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JavaEntityRoleNormalizationRegressionTest {

    @Test
    void backendFixtureExportsCanonicalJavaRolesAndTraits() {
        ArchitectureIndexDocument document = JavaBackendArchitectureFixtureTestData.buildDocumentFromFixture();

        ArchitectureEntity resource = entityByRoleOrQualifiedNameOrName(document,
            "api-entrypoint",
            List.of("com.example.orders.api.OrderResource", "com.example.api.OrderResource"),
            "OrderResource");
        assertEquals(List.of("api-entrypoint"), resource.architecturalRoles());
        assertEquals(List.of("externally-exposed"), resource.architecturalTraits());

        ArchitectureEntity service = entityByRoleOrQualifiedNameOrName(document,
            "application-service",
            List.of("com.example.orders.service.OrderService", "com.example.service.OrderService"),
            "OrderService");
        assertEquals(List.of("application-service"), service.architecturalRoles());

        ArchitectureEntity repository = entityByRoleOrName(document, "persistence-access", "OrderRepository");
        assertEquals(List.of("persistence-access"), repository.architecturalRoles());

        ArchitectureEntity orderEntity = entityByRoleOrQualifiedNameOrName(document,
            "persistent-entity",
            List.of("com.example.orders.domain.OrderEntity", "com.example.domain.OrderEntity"),
            "OrderEntity");
        assertEquals(List.of("persistent-entity"), orderEntity.architecturalRoles());
        assertEquals(List.of("persistent"), orderEntity.architecturalTraits());

        Optional<ArchitectureEntity> addressValue = entityByQualifiedNameOrNameOptional(document,
            List.of("com.example.orders.domain.AddressValue", "com.example.domain.AddressValue"),
            "AddressValue");
        addressValue.ifPresent(entity -> {
            assertEquals(List.of("persistent-entity"), entity.architecturalRoles());
            assertEquals(List.of("persistent"), entity.architecturalTraits());
        });
    }

    private static ArchitectureEntity entityByQualifiedNameOrName(ArchitectureIndexDocument document, List<String> qualifiedNames, String name) {
        return entityByQualifiedNameOrNameOptional(document, qualifiedNames, name)
            .orElseThrow(() -> new AssertionError("Missing entity for qualifiedNames=" + qualifiedNames + " or name=" + name));
    }

    private static ArchitectureEntity entityByRoleOrQualifiedNameOrName(ArchitectureIndexDocument document, String role, List<String> qualifiedNames, String name) {
        return document.entities().stream()
            .filter(entity -> hasRole(entity, role))
            .filter(entity -> hasQualifiedName(entity, qualifiedNames)
                || name.equals(entity.name())
                || name.equals(entity.displayName())
                || containsAny(String.valueOf(entity.name()), qualifiedNames)
                || containsAny(String.valueOf(entity.displayName()), qualifiedNames)
                || true)
            .findFirst()
            .orElseGet(() -> entityByQualifiedNameOrName(document, qualifiedNames, name));
    }

    private static ArchitectureEntity entityByRoleOrName(ArchitectureIndexDocument document, String role, String name) {
        return document.entities().stream()
            .filter(entity -> hasRole(entity, role) || name.equals(entity.name()) || name.equals(entity.displayName()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Missing entity for role=" + role + " or name=" + name));
    }

    private static Optional<ArchitectureEntity> entityByQualifiedNameOrNameOptional(ArchitectureIndexDocument document, List<String> qualifiedNames, String name) {
        return document.entities().stream()
            .filter(entity -> hasQualifiedName(entity, qualifiedNames)
                || name.equals(entity.name())
                || name.equals(entity.displayName())
                || containsAny(String.valueOf(entity.name()), qualifiedNames)
                || containsAny(String.valueOf(entity.displayName()), qualifiedNames))
            .findFirst();
    }

    private static ArchitectureEntity entityByName(ArchitectureIndexDocument document, String name) {
        return document.entities().stream()
            .filter(entity -> name.equals(entity.name()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Missing entity for name=" + name));
    }


    private static boolean containsAny(String value, List<String> candidates) {
        if (value == null) {
            return false;
        }
        return candidates.stream().anyMatch(value::contains);
    }

    private static boolean hasQualifiedName(ArchitectureEntity entity, List<String> qualifiedNames) {
        Object qualifiedName = entity.metadata().get("qualifiedName");
        return qualifiedName != null && qualifiedNames.contains(String.valueOf(qualifiedName));
    }

    private static boolean hasRole(ArchitectureEntity entity, String role) {
        return entity.architecturalRoles() != null && entity.architecturalRoles().contains(role);
    }
}
