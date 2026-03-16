package info.isaksson.erland.architecturebrowser.indexer.extract;

import java.util.List;

record JavaMemberExtractionResult(
    List<String> emittedEntityIds,
    int emittedRelationshipCount,
    boolean handled
) {
    static JavaMemberExtractionResult notHandled() {
        return new JavaMemberExtractionResult(List.of(), 0, false);
    }

    static JavaMemberExtractionResult handled(List<String> emittedEntityIds, int emittedRelationshipCount) {
        return new JavaMemberExtractionResult(List.copyOf(emittedEntityIds), emittedRelationshipCount, true);
    }
}
