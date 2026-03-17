package info.isaksson.erland.architecturebrowser.indexer.extract;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

record JavaEndpointSemantics(
    String httpMethod,
    String path,
    String classLevelPath,
    String methodLevelPath,
    String resourceQualifiedName,
    String methodName,
    String methodQualifiedName,
    List<Map<String, String>> parameterDetails,
    List<String> annotations
) {
    Map<String, Object> endpointMetadata() {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("language", "java");
        metadata.put("framework", "jax-rs");
        metadata.put("httpMethod", httpMethod);
        metadata.put("path", path);
        metadata.put("classLevelPath", classLevelPath);
        metadata.put("methodLevelPath", methodLevelPath);
        metadata.put("resourceQualifiedName", resourceQualifiedName);
        metadata.put("methodName", methodName);
        metadata.put("methodQualifiedName", methodQualifiedName);
        metadata.put("parameterDetails", parameterDetails == null ? List.of() : List.copyOf(parameterDetails));
        metadata.put("annotations", annotations == null ? List.of() : List.copyOf(annotations));
        return Map.copyOf(metadata);
    }

    Map<String, Object> methodMetadata(Map<String, Object> base) {
        Map<String, Object> metadata = new LinkedHashMap<>(base == null ? Map.of() : base);
        metadata.put("framework", "jax-rs");
        metadata.put("jaxRsEndpoint", true);
        metadata.put("httpMethod", httpMethod);
        metadata.put("path", path);
        metadata.put("parameterDetails", parameterDetails == null ? List.of() : List.copyOf(parameterDetails));
        return Map.copyOf(metadata);
    }
}
