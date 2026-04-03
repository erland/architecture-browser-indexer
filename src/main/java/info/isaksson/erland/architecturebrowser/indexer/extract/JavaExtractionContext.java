package info.isaksson.erland.architecturebrowser.indexer.extract;

import java.util.Map;

record JavaExtractionContext(
    String relativePath,
    String packageName,
    String sourceText,
    Map<String, String> importsBySimpleName,
    Map<String, JavaDeclaredType> declaredTypes,
    Map<String, JavaDeclaredType> resolutionDeclaredTypes
) {}
