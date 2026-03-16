package info.isaksson.erland.architecturebrowser.indexer.extract;

final class JavaOwnershipSupport {

    private JavaOwnershipSupport() {}

    static String dependencySourceEntityId(JavaOwnerContext ownerContext, String fileEntityId) {
        if (ownerContext == null || ownerContext.owningTypeEntityId() == null) {
            return fileEntityId;
        }
        return ownerContext.owningTypeEntityId();
    }
}
