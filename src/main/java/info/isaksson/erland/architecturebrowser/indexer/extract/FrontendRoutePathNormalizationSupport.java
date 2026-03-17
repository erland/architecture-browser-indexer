package info.isaksson.erland.architecturebrowser.indexer.extract;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

final class FrontendRoutePathNormalizationSupport {

    List<FrontendRouteCandidate> normalize(List<FrontendRouteCandidate> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }
        List<FrontendRouteCandidate> ordered = new ArrayList<>(candidates);
        ordered.sort(Comparator.comparingInt(FrontendRouteCandidate::start));
        List<FrontendRouteCandidate> resolvedCandidates = new ArrayList<>();
        for (int i = 0; i < ordered.size(); i++) {
            FrontendRouteCandidate candidate = ordered.get(i);
            FrontendRouteCandidate parent = findParent(candidate, ordered, i);
            String fullPath = fullRoutePath(candidate, parent);
            resolvedCandidates.add(candidate.withFullPath(fullPath));
        }
        return List.copyOf(resolvedCandidates);
    }

    FrontendRouteCandidate findParent(FrontendRouteCandidate candidate, List<FrontendRouteCandidate> candidates, int candidateIndex) {
        FrontendRouteCandidate parent = null;
        for (int i = 0; i < candidates.size(); i++) {
            if (i == candidateIndex) {
                continue;
            }
            FrontendRouteCandidate possibleParent = candidates.get(i);
            if (!possibleParent.framework().equals(candidate.framework())) {
                continue;
            }
            if (possibleParent.start() < candidate.start() && possibleParent.end() > candidate.end()) {
                if (parent == null || (possibleParent.end() - possibleParent.start()) < (parent.end() - parent.start())) {
                    parent = possibleParent;
                }
            }
        }
        return parent;
    }

    String fullRoutePath(FrontendRouteCandidate candidate, FrontendRouteCandidate parent) {
        String own = normalizedPath(candidate.path());
        if (parent == null) {
            return own;
        }
        String parentPath = normalizedPath(parent.fullPath() == null ? parent.path() : parent.fullPath());
        if ("/".equals(own)) {
            return parentPath;
        }
        if ("/".equals(parentPath)) {
            return own;
        }
        if (own.isBlank()) {
            return parentPath;
        }
        return normalizedPath(parentPath + "/" + own.replaceFirst("^/", ""));
    }

    String normalizedPath(String raw) {
        String path = raw == null ? "" : raw.strip();
        if (path.isBlank()) {
            return "/";
        }
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        path = path.replaceAll("//+", "/");
        return path.isBlank() ? "/" : path;
    }
}
