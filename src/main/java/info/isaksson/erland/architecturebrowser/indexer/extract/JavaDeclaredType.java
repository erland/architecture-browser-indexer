package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;

record JavaDeclaredType(String entityId, String qualifiedName, EntityKind kind) {
}
