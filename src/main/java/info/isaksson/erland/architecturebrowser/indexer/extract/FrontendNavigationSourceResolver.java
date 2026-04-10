package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.SourceReference;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

final class FrontendNavigationSourceResolver {
    ExtractedEntityFact findNavigationSourceEntity(Map<String, ExtractedEntityFact> namedEntities, String relativePath, int line, String framework) {
        if (namedEntities == null || namedEntities.isEmpty()) {
            return null;
        }
        List<ExtractedEntityFact> sameFileEntities = namedEntities.values().stream()
            .filter(entity -> entity.sourceRefs().stream().anyMatch(ref -> relativePath.equals(ref.path())))
            .toList();
        if (sameFileEntities.isEmpty()) {
            return null;
        }
        Comparator<ExtractedEntityFact> comparator = Comparator
            .comparingInt((ExtractedEntityFact entity) -> frameworkScore(entity, framework))
            .thenComparingInt(entity -> lineContainmentScore(entity, line))
            .thenComparingInt(entity -> distanceToLine(entity, line));
        return sameFileEntities.stream()
            .max(comparator)
            .orElse(null);
    }

    private int frameworkScore(ExtractedEntityFact entity, String framework) {
        int score = 0;
        if (framework.equals(entity.metadata().get("framework"))) {
            score += 10;
        }
        if ("page-or-router".equals(entity.metadata().get("uiProfile"))) {
            score += 5;
        }
        return score;
    }

    private int lineContainmentScore(ExtractedEntityFact entity, int line) {
        int best = Integer.MIN_VALUE;
        for (SourceReference ref : entity.sourceRefs()) {
            Integer startLine = ref.startLine();
            Integer endLine = ref.endLine();
            if (startLine == null) {
                continue;
            }
            int score;
            if (endLine != null && startLine <= line && line <= endLine) {
                score = 100;
            } else if (startLine <= line) {
                score = 50;
            } else {
                score = -Math.abs(startLine - line);
            }
            if (score > best) {
                best = score;
            }
        }
        return best == Integer.MIN_VALUE ? 0 : best;
    }

    private int distanceToLine(ExtractedEntityFact entity, int line) {
        return entity.sourceRefs().stream()
            .map(SourceReference::startLine)
            .filter(Objects::nonNull)
            .mapToInt(startLine -> -Math.abs(line - startLine))
            .max()
            .orElse(Integer.MIN_VALUE);
    }
}
