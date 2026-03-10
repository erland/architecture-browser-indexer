package info.isaksson.erland.architecturebrowser.indexer.acquisition;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.Diagnostic;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RepositorySource;

import java.nio.file.Path;
import java.util.List;

public record AcquisitionResult(
    RepositorySource repositorySource,
    Path acquiredRoot,
    List<Diagnostic> diagnostics,
    boolean temporaryWorkspace
) {
}
