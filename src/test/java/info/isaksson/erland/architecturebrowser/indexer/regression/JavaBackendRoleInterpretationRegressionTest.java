package info.isaksson.erland.architecturebrowser.indexer.regression;

import info.isaksson.erland.architecturebrowser.indexer.extract.StructuralExtractionService;
import info.isaksson.erland.architecturebrowser.indexer.extract.StructuralExtractorRegistry;
import info.isaksson.erland.architecturebrowser.indexer.extract.model.StructuralExtractionResult;
import info.isaksson.erland.architecturebrowser.indexer.interpret.InterpretationRegistry;
import info.isaksson.erland.architecturebrowser.indexer.interpret.InterpretationService;
import info.isaksson.erland.architecturebrowser.indexer.interpret.model.InterpretationResult;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;
import info.isaksson.erland.architecturebrowser.indexer.parse.ParseBatchResult;
import info.isaksson.erland.architecturebrowser.indexer.parse.ParseStatus;
import info.isaksson.erland.architecturebrowser.indexer.parse.ParserRegistry;
import info.isaksson.erland.architecturebrowser.indexer.parse.TreeSitterConfiguration;
import info.isaksson.erland.architecturebrowser.indexer.parse.TreeSitterParserRegistryFactory;
import info.isaksson.erland.architecturebrowser.indexer.parse.TreeSitterParsingService;
import info.isaksson.erland.architecturebrowser.indexer.scan.FileInventory;
import info.isaksson.erland.architecturebrowser.indexer.scan.FileInventoryScanner;
import info.isaksson.erland.architecturebrowser.indexer.scan.InventoryScanOptions;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaBackendRoleInterpretationRegressionTest {

    @Test
    void baselineFixtureProducesRicherBackendRoleInterpretation() {
        InterpretationResult result = interpretFixture();

        assertTrue(result.entities().stream().anyMatch(entity -> entity.kind() == EntityKind.SERVICE
            && "resource".equals(entity.metadata().get("entityRole"))
            && "jax-rs-resource".equals(entity.metadata().get("backendProfile"))
            && String.valueOf(entity.metadata().get("frameworks")).contains("jax-rs")),
            () -> "Expected JAX-RS resource role. Entities=" + result.entities());

        assertTrue(result.entities().stream().anyMatch(entity -> entity.kind() == EntityKind.SERVICE
            && "service".equals(entity.metadata().get("entityRole"))
            && String.valueOf(entity.metadata().get("backendProfile")).contains("application-service")
            && String.valueOf(entity.metadata().get("frameworks")).contains("cdi")),
            () -> "Expected application service role. Entities=" + result.entities());

        assertTrue(result.entities().stream().anyMatch(entity -> entity.kind() == EntityKind.PERSISTENCE_ADAPTER
            && "repository".equals(entity.metadata().get("entityRole"))
            && "repository".equals(entity.metadata().get("backendProfile"))),
            () -> "Expected repository role. Entities=" + result.entities());
    }

    private static InterpretationResult interpretFixture() {
        Path fixtureRoot = Path.of("src/test/resources/fixtures/java/java-backend-baseline");
        FileInventory inventory = new FileInventoryScanner().scan(fixtureRoot, InventoryScanOptions.defaults());

        TreeSitterConfiguration configuration = TreeSitterConfiguration.fromEnvironment();
        ParserRegistry parserRegistry = TreeSitterParserRegistryFactory.createDefaultRegistry(configuration);
        ParseBatchResult parseBatch = new TreeSitterParsingService(parserRegistry).parseInventory(fixtureRoot, inventory);

        assertFalse(parseBatch.results().isEmpty(), "Expected parsed Java fixture files.");
        assertTrue(parseBatch.results().stream().allMatch(result -> result.status() == ParseStatus.SUCCESS),
            () -> "Expected successful parse for Java fixture. Results=" + parseBatch.results());

        StructuralExtractionResult extraction = new StructuralExtractionService(StructuralExtractorRegistry.defaultRegistry()).extract(parseBatch);
        return new InterpretationService(InterpretationRegistry.defaultRegistry()).interpret(extraction);
    }
}
