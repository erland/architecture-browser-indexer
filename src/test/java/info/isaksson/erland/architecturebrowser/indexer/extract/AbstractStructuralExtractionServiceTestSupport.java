package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.StructuralExtractionResult;
import info.isaksson.erland.architecturebrowser.indexer.parse.ParseBatchResult;

abstract class AbstractStructuralExtractionServiceTestSupport {
    protected static StructuralExtractionResult extract(ParseBatchResult batchResult) {
        return new StructuralExtractionService(StructuralExtractorRegistry.defaultRegistry()).extract(batchResult);
    }
}
