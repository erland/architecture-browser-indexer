package info.isaksson.erland.architecturebrowser.indexer.normalize;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureEntity;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureRelationship;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityOrigin;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RelationshipKind;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SqlConfigArchitectureNormalizationRuleTest {

    private final ArchitectureEntityNormalizationService entityService = ArchitectureEntityNormalizationService.defaultService();
    private final ArchitectureRelationshipNormalizationService relationshipService = ArchitectureRelationshipNormalizationService.defaultService();

    @Test
    void mapsSqlAndConfigEvidenceToCanonicalRoles() {
        ArchitectureEntity sqlTable = entity("entity:sql:orders", EntityKind.DATASTORE, Map.of(
            "language", "sql",
            "tableName", "orders"
        ));
        ArchitectureEntity configEntry = entity("entity:cfg:db.url", EntityKind.CONFIG_ARTIFACT, Map.of(
            "language", "yaml",
            "configKey", "datasource.url",
            "configValue", "jdbc:postgresql://orders-db/orders"
        ));
        ArchitectureEntity externalDb = entity("entity:cfg:db", EntityKind.DATASTORE, Map.of(
            "language", "yaml",
            "external", true,
            "sourceConfigKey", "datasource.url"
        ));
        ArchitectureEntity configFile = entity("entity:file:application.yml", EntityKind.MODULE, Map.of(
            "language", "yaml"
        ));

        Map<String, ArchitectureEntity> entities = Map.of(
            sqlTable.id(), sqlTable,
            configEntry.id(), configEntry,
            externalDb.id(), externalDb,
            configFile.id(), configFile
        );

        assertEquals(List.of("persistent-entity"), entityService.normalizeEntity(sqlTable, entities).architecturalRoles());
        assertEquals(List.of("persistent"), entityService.normalizeEntity(sqlTable, entities).architecturalTraits());
        assertEquals(List.of("configuration-provider"), entityService.normalizeEntity(configEntry, entities).architecturalRoles());
        assertEquals(List.of("configuration-driven"), entityService.normalizeEntity(configEntry, entities).architecturalTraits());
        assertEquals(List.of("external-dependency"), entityService.normalizeEntity(externalDb, entities).architecturalRoles());
        assertEquals(List.of("module-boundary"), entityService.normalizeEntity(configFile, entities).architecturalRoles());
    }

    @Test
    void mapsConfigRelationshipsToExternalAndPersistenceSemantics() {
        ArchitectureEntity configEntry = entityWithRoles("entity:cfg", EntityKind.CONFIG_ARTIFACT, Map.of("language", "yaml"), List.of("configuration-provider"));
        ArchitectureEntity external = entityWithRoles("entity:ext", EntityKind.EXTERNAL_SYSTEM, Map.of("language", "yaml", "external", true), List.of("external-dependency"));
        ArchitectureEntity datastore = entityWithRoles("entity:db", EntityKind.DATASTORE, Map.of("language", "sql", "tableName", "orders"), List.of("persistent-entity"));
        Map<String, ArchitectureEntity> entities = Map.of(configEntry.id(), configEntry, external.id(), external, datastore.id(), datastore);

        ArchitectureRelationship cfgToExternal = relationship("rel:cfg:ext", RelationshipKind.CALLS, configEntry.id(), external.id(), Map.of("sourceLanguage", "yaml"));
        ArchitectureRelationship cfgToDb = relationship("rel:cfg:db", RelationshipKind.READS, configEntry.id(), datastore.id(), Map.of("sourceLanguage", "yaml"));

        assertEquals(List.of("calls-external-system"), relationshipService.normalizeRelationship(cfgToExternal, entities, Map.of()).architecturalSemantics());
        assertEquals(List.of("accesses-persistence"), relationshipService.normalizeRelationship(cfgToDb, entities, Map.of()).architecturalSemantics());
    }

    private static ArchitectureEntity entity(String id, EntityKind kind, Map<String, Object> metadata) {
        return new ArchitectureEntity(id, kind, EntityOrigin.OBSERVED, id, id, "scope:repo", List.of(), metadata);
    }

    private static ArchitectureEntity entityWithRoles(String id, EntityKind kind, Map<String, Object> metadata, List<String> roles) {
        return new ArchitectureEntity(id, kind, EntityOrigin.OBSERVED, id, id, "scope:repo", List.of(), metadata, roles, null);
    }

    private static ArchitectureRelationship relationship(String id, RelationshipKind kind, String fromId, String toId, Map<String, Object> metadata) {
        return new ArchitectureRelationship(id, kind, fromId, toId, id, List.of(), metadata);
    }
}
