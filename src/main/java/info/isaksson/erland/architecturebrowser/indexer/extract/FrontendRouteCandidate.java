package info.isaksson.erland.architecturebrowser.indexer.extract;

import java.util.List;
import java.util.Objects;

record FrontendRouteCandidate(
    String framework,
    String path,
    String fullPath,
    int start,
    int end,
    int startLine,
    String snippet,
    List<String> targets,
    List<String> lazyLoads,
    List<String> guards,
    List<String> resolvers,
    String declarationKind,
    String redirectTarget
) {
    FrontendRouteCandidate {
        framework = framework == null || framework.isBlank() ? "react" : framework;
        path = path == null ? "" : path;
        fullPath = fullPath == null ? path : fullPath;
        snippet = Objects.toString(snippet, "");
        targets = targets == null ? List.of() : List.copyOf(targets);
        lazyLoads = lazyLoads == null ? List.of() : List.copyOf(lazyLoads);
        guards = guards == null ? List.of() : List.copyOf(guards);
        resolvers = resolvers == null ? List.of() : List.copyOf(resolvers);
        declarationKind = declarationKind == null || declarationKind.isBlank() ? "route-object" : declarationKind;
        redirectTarget = redirectTarget == null ? "" : redirectTarget;
    }

    FrontendRouteCandidate withFullPath(String updatedFullPath) {
        return new FrontendRouteCandidate(framework, path, updatedFullPath, start, end, startLine, snippet, targets, lazyLoads, guards, resolvers,
            declarationKind, redirectTarget);
    }
}
