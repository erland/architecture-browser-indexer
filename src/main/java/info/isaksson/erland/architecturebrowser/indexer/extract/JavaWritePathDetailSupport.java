package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class JavaWritePathDetailSupport {
    record DetectedWritePath(String operation, String writeKind, String argumentExpression, String viaField, String viaType) {}
    List<DetectedWritePath> detectWritePaths(ExtractedEntityFact methodEntity, String snippet) { List<DetectedWritePath> result = new ArrayList<>(); Matcher m = Pattern.compile("([A-Za-z_$][\\w$]*)\\s*\\.\\s*(persist|merge|remove|saveAndFlush|save|update|delete)\\s*\\(([^)]*)\\)", Pattern.DOTALL).matcher(snippet == null ? "" : snippet); while (m.find()) result.add(new DetectedWritePath(normalize(m.group(2)), m.group(2).startsWith("save")||m.group(2).startsWith("update")||m.group(2).startsWith("delete")?"repository-call":"entity-manager", m.group(3).strip(), m.group(1), null)); return List.copyOf(result); }
    Map<String,String> collectMethodVariableTypes(ExtractedEntityFact methodEntity, String snippet) { LinkedHashMap<String,String> result = new LinkedHashMap<>(); List<String> types = JavaJpaDetailSupport.metadataStringList(methodEntity.metadata().get("parameterTypes")); String params = String.valueOf(methodEntity.metadata().getOrDefault("parameters","")); List<String> names = new ArrayList<>(); Matcher m = Pattern.compile("([A-Za-z_$][\\w$]*)").matcher(params); while (m.find()) names.add(m.group(1)); for (int i=0;i<Math.min(types.size(), names.size());i++) result.putIfAbsent(names.get(i), JavaRelationshipEvidenceEmitter.normalizeTypeReference(types.get(i))); Matcher locals = Pattern.compile("([A-Za-z_$][\\w.$]*(?:\\s*<[^>{}]+>)?)\\s+([A-Za-z_$][\\w$]*)\\s*=").matcher(snippet == null ? "" : snippet); while (locals.find()) result.putIfAbsent(locals.group(2), JavaRelationshipEvidenceEmitter.normalizeTypeReference(locals.group(1))); return Map.copyOf(result); }
    Optional<String> resolveWriteTargetEntityType(String argumentExpression, Map<String,String> variableTypes) { if (argumentExpression == null || argumentExpression.isBlank()) return Optional.empty(); Matcher n = Pattern.compile("new\\s+([A-Za-z_$][\\w.$<>]*)").matcher(argumentExpression); if (n.find()) return Optional.of(JavaRelationshipEvidenceEmitter.normalizeTypeReference(n.group(1))); String direct = variableTypes.get(argumentExpression.strip()); return direct == null ? Optional.empty() : Optional.of(direct); }
    LinkedHashMap<String,Object> relationshipMetadata(DetectedWritePath detection, String methodName, String ownerQualifiedName, String entityType) { LinkedHashMap<String,Object> m = new LinkedHashMap<>(); m.put("framework","jpa"); m.put("relationshipType","writePath"); m.put("writeOperation",detection.operation()); m.put("writeKind",detection.writeKind()); m.put("writerMethod",methodName); m.put("writerQualifiedName",ownerQualifiedName == null ? "" : ownerQualifiedName); m.put("entityType",entityType); if (detection.viaField()!=null) m.put("writeViaField",detection.viaField()); return m; }
    private static String normalize(String raw) { String value = raw.toLowerCase(Locale.ROOT); if (value.contains("save")) return "persist"; if (value.contains("update") || value.contains("merge")) return "merge"; if (value.contains("delete") || value.contains("remove")) return "remove"; return value; }
}
