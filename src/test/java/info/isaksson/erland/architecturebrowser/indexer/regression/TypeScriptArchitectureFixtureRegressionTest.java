package info.isaksson.erland.architecturebrowser.indexer.regression;

import info.isaksson.erland.architecturebrowser.indexer.ir.ArchitectureIrValidator;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureIndexDocument;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TypeScriptArchitectureFixtureRegressionTest extends AbstractTypeScriptArchitectureFixtureTestSupport {

    @Test
    void layeredReactFixtureStillProducesBroadArchitecturePicture() {
        ArchitectureIndexDocument document = buildDocument(TypeScriptArchitectureFixtureFixtures.layeredReactFixture());

        assertTrue(ArchitectureIrValidator.validate(document).isValid());
        assertTrue(document.entities().stream().anyMatch(entity -> "OrderService".equals(entity.name())));
        assertTrue(document.entities().stream().anyMatch(entity -> "OrdersStore".equals(entity.name())));
        assertTrue(document.entities().stream().anyMatch(entity -> "OrdersPage".equals(entity.name())));
        assertTrue(!dependencyViewList(document, "typeDependencies").isEmpty());
        assertTrue(!dependencyViewList(document, "moduleDependencies").isEmpty() || !dependencyViewList(document, "packageDependencies").isEmpty());
    }
}
