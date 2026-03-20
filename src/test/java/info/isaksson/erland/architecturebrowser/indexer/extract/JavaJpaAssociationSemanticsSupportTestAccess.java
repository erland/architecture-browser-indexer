package info.isaksson.erland.architecturebrowser.indexer.extract;

import java.lang.reflect.Method;
import java.util.Map;

final class JavaJpaAssociationSemanticsSupportTestAccess {
    private JavaJpaAssociationSemanticsSupportTestAccess() {}

    @SuppressWarnings("unchecked")
    static Map<String, Object> deriveAssociationBounds(String associationKind, String snippet) {
        try {
            Method method = JavaJpaAssociationSemanticsSupport.class.getDeclaredMethod("deriveAssociationBounds", String.class, String.class);
            method.setAccessible(true);
            return (Map<String, Object>) method.invoke(null, associationKind, snippet);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Unable to invoke deriveAssociationBounds", ex);
        }
    }
}
