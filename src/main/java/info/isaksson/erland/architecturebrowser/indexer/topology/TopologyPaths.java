package info.isaksson.erland.architecturebrowser.indexer.topology;

import java.nio.file.Path;

final class TopologyPaths {
    private TopologyPaths() {
    }

    static void buildDirectoryHierarchy(String relativePath, java.util.Map<String, info.isaksson.erland.architecturebrowser.indexer.ir.model.LogicalScope> inferredScopes, java.util.Map<String, String> fileDirectoryScopeIds) {
        Path path = Path.of(relativePath);
        String previousScopeId = "scope:repo";
        String normalized = "";
        for (int i = 0; i < path.getNameCount() - 1; i++) {
            normalized = normalized.isEmpty() ? path.getName(i).toString() : normalized + "/" + path.getName(i);
            String parentPath = parentPath(normalized);
            String parentScopeId = parentPath == null ? "scope:repo" : info.isaksson.erland.architecturebrowser.indexer.extract.IdUtils.scopeId("directory", parentPath);
            info.isaksson.erland.architecturebrowser.indexer.ir.model.LogicalScope scope = TopologySupport.directoryScope(normalized, parentScopeId);
            inferredScopes.putIfAbsent(scope.id(), scope);
            previousScopeId = scope.id();
        }
        if (!previousScopeId.equals("scope:repo")) {
            fileDirectoryScopeIds.put(relativePath, previousScopeId);
        }
    }

    static String parentPath(String path) {
        if (path == null || path.isBlank() || !path.contains("/")) {
            return null;
        }
        return path.substring(0, path.lastIndexOf('/'));
    }

    static String parentQualifiedName(String qualifiedName) {
        if (qualifiedName == null || qualifiedName.isBlank() || !qualifiedName.contains(".")) {
            return null;
        }
        return qualifiedName.substring(0, qualifiedName.lastIndexOf('.'));
    }

    static String sourceRootEntityId(String filePath) {
        String root = moduleRoot(filePath);
        return root == null ? null : info.isaksson.erland.architecturebrowser.indexer.extract.IdUtils.externalEntityId("logical-module", root);
    }

    static String moduleRoot(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return null;
        }
        String[] parts = relativePath.split("/");
        if (parts.length >= 3 && "src".equals(parts[0]) && ("main".equals(parts[1]) || "test".equals(parts[1]))) {
            return parts[0] + "/" + parts[1] + "/" + parts[2];
        }
        if (parts.length >= 2 && "src".equals(parts[0]) && !(
            "main".equals(parts[1]) || "test".equals(parts[1])
        )) {
            return parts[0] + "/" + parts[1];
        }
        return parts.length > 0 ? parts[0] : null;
    }

    static String parentPackageName(String packageName) {
        if (packageName == null || packageName.isBlank()) {
            return null;
        }
        if (packageName.contains("/")) {
            return parentPath(packageName);
        }
        if (!packageName.contains(".")) {
            return null;
        }
        return packageName.substring(0, packageName.lastIndexOf('.'));
    }
}
