package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;

record AngularInjectionReference(String targetName, String label, EntityKind kind, String referenceKind) {
}
