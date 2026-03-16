package info.isaksson.erland.architecturebrowser.indexer.interpret;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;

import java.util.Map;

record JavaBackendInterpretationClassification(EntityKind roleKind, Map<String, Object> metadata, String interpretationLabel) {
}
