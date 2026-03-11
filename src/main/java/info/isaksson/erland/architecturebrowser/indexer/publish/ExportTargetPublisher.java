package info.isaksson.erland.architecturebrowser.indexer.publish;

import info.isaksson.erland.architecturebrowser.indexer.publish.model.ExportBundle;
import info.isaksson.erland.architecturebrowser.indexer.publish.model.ExportTarget;

public interface ExportTargetPublisher {
    ExportTarget target();

    void publish(ExportBundle bundle) throws Exception;
}
