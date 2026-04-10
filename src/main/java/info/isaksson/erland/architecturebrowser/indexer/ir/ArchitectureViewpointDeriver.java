package info.isaksson.erland.architecturebrowser.indexer.ir;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureViewpoint;

interface ArchitectureViewpointDeriver {
    ArchitectureViewpoint derive(ViewpointEvidence evidence);
}
