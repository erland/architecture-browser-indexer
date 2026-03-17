package info.isaksson.erland.architecturebrowser.indexer.regression;

import info.isaksson.erland.architecturebrowser.indexer.ir.ArchitectureIrValidator;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureIndexDocument;
import org.junit.jupiter.api.Test;

import static info.isaksson.erland.architecturebrowser.indexer.testing.ArchitectureContractAssertions.assertHasUiModuleProfile;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FrontendFrameworkBaselineRegressionTest extends AbstractFrontendFrameworkBaselineTestSupport {
    @Test
    void currentFrontendFrameworkBaselineStillProducesExpectedOverallPicture() {
        ArchitectureIndexDocument document = buildRoleInterpretationDocument();
        assertTrue(ArchitectureIrValidator.validate(document).isValid());
        assertHasUiModuleProfile(document.entities(), "OrderListComponent", "angular-component");
        assertHasUiModuleProfile(document.entities(), "UserCard", "react-function-component");
        assertHasUiModuleProfile(document.entities(), "AuthProvider", "react-context");
    }
}
