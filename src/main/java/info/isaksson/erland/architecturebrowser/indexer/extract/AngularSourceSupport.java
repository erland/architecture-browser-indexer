package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.SourceReference;

import java.util.Map;

public final class AngularSourceSupport {
    private AngularSourceSupport() {
    }

    public static SourceReference primaryRef(ExtractedEntityFact entity, String relativePath) {
        return entity.sourceRefs().isEmpty()
            ? ExtractionSupport.sourceRef(relativePath, 1, entity.name(), Map.of("language", "typescript", "framework", "angular"))
            : entity.sourceRefs().getFirst();
    }

    public static int refLine(ExtractedEntityFact entity) {
        return entity.sourceRefs().isEmpty() || entity.sourceRefs().getFirst().startLine() == null
            ? 1
            : entity.sourceRefs().getFirst().startLine();
    }

    public static SourceReference templateRef(String relativePath, Integer line, String template) {
        int safeLine = line == null ? 1 : line;
        return ExtractionSupport.sourceRef(relativePath, safeLine, template, Map.of("language", "typescript", "framework", "angular", "kind", "template"));
    }
}
