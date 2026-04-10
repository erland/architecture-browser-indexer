package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;

final class AngularDependencyInjectionDetectionSupport {
    private AngularDependencyInjectionDetectionSupport() {
    }

    static boolean looksLikeAngularDiSource(String relativePath, String sourceText, Map<String, ExtractedEntityFact> namedEntities) {
        String lowerPath = relativePath == null ? "" : relativePath.toLowerCase(Locale.ROOT);
        String lowerSource = sourceText == null ? "" : sourceText.toLowerCase(Locale.ROOT);
        if (lowerPath.contains("/app/") || lowerPath.contains(".component.") || lowerPath.contains(".service.") || lowerPath.contains(".module.")) {
            return true;
        }
        if (lowerSource.contains("@injectable") || lowerSource.contains("@component") || lowerSource.contains("@ngmodule") || lowerSource.contains("@directive")) {
            return true;
        }
        return namedEntities.values().stream().anyMatch(AngularDependencyInjectionDetectionSupport::isAngularEntity);
    }

    static boolean isAngularProviderOwner(ExtractedEntityFact entity) {
        return isAngularEntity(entity) && entity.metadata().get("angularProviders") instanceof java.util.List<?>;
    }

    static boolean isAngularInjectableConsumer(ExtractedEntityFact entity) {
        return isAngularEntity(entity)
            && entity.kind() == EntityKind.CLASS
            && !Objects.toString(AngularSourceSupport.primaryRef(entity, "").snippet(), "").isBlank();
    }

    private static boolean isAngularEntity(ExtractedEntityFact entity) {
        return entity != null && "angular".equals(entity.metadata().get("framework"));
    }
}
