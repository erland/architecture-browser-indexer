package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.parse.SyntaxNode;

import java.util.LinkedHashMap;
import java.util.Map;

final class AngularDecoratorMetadataExtractor {
    private AngularDecoratorMetadataExtractor() {
    }

    static Map<String, Object> extract(SyntaxNode declarationNode) {
        return AngularDecoratorModelExtractor.extract(declarationNode)
            .map(AngularDecoratorMetadataExtractor::toMetadata)
            .orElse(Map.of());
    }

    private static Map<String, Object> toMetadata(AngularDecoratorModel model) {
        return switch (model.decoratorName()) {
            case "Component" -> componentMetadata(model);
            case "Directive" -> directiveMetadata(model);
            case "Pipe" -> pipeMetadata(model);
            case "NgModule" -> ngModuleMetadata(model);
            case "Injectable" -> injectableMetadata(model);
            default -> Map.of();
        };
    }

    private static Map<String, Object> componentMetadata(AngularDecoratorModel model) {
        Map<String, String> payload = model.fields();
        Map<String, Object> metadata = angularMetadataBase(model);
        putIfNotBlank(metadata, "angularSelector", stringLiteral(payload.get("selector")));
        putIfNotBlank(metadata, "angularTemplateUrl", stringLiteral(payload.get("templateUrl")));
        String inlineTemplate = stringLiteral(payload.get("template"));
        if (payload.containsKey("template")) {
            metadata.put("angularHasInlineTemplate", !payload.get("template").isBlank());
        }
        putIfNotBlank(metadata, "angularInlineTemplate", inlineTemplate);
        putIfPresent(metadata, "angularStyleUrls", arrayOrSingleton(payload.get("styleUrls")));
        putIfPresent(metadata, "angularStandalone", booleanLiteral(payload.get("standalone")));
        putIfPresent(metadata, "angularImports", arrayOrSingleton(payload.get("imports")));
        putIfPresent(metadata, "angularProviders", arrayOrSingleton(payload.get("providers")));
        return Map.copyOf(metadata);
    }

    private static Map<String, Object> directiveMetadata(AngularDecoratorModel model) {
        Map<String, String> payload = model.fields();
        Map<String, Object> metadata = angularMetadataBase(model);
        putIfNotBlank(metadata, "angularSelector", stringLiteral(payload.get("selector")));
        putIfPresent(metadata, "angularStandalone", booleanLiteral(payload.get("standalone")));
        putIfPresent(metadata, "angularProviders", arrayOrSingleton(payload.get("providers")));
        return Map.copyOf(metadata);
    }

    private static Map<String, Object> pipeMetadata(AngularDecoratorModel model) {
        Map<String, String> payload = model.fields();
        Map<String, Object> metadata = angularMetadataBase(model);
        putIfNotBlank(metadata, "angularPipeName", stringLiteral(payload.get("name")));
        putIfPresent(metadata, "angularStandalone", booleanLiteral(payload.get("standalone")));
        return Map.copyOf(metadata);
    }

    private static Map<String, Object> ngModuleMetadata(AngularDecoratorModel model) {
        Map<String, String> payload = model.fields();
        Map<String, Object> metadata = angularMetadataBase(model);
        putIfPresent(metadata, "angularImports", arrayOrSingleton(payload.get("imports")));
        putIfPresent(metadata, "angularDeclarations", arrayOrSingleton(payload.get("declarations")));
        putIfPresent(metadata, "angularExports", arrayOrSingleton(payload.get("exports")));
        putIfPresent(metadata, "angularProviders", arrayOrSingleton(payload.get("providers")));
        putIfPresent(metadata, "angularBootstrap", arrayOrSingleton(payload.get("bootstrap")));
        return Map.copyOf(metadata);
    }

    private static Map<String, Object> injectableMetadata(AngularDecoratorModel model) {
        Map<String, String> payload = model.fields();
        Map<String, Object> metadata = angularMetadataBase(model);
        putIfNotBlank(metadata, "angularProvidedIn", stringLiteral(payload.get("providedIn")));
        return Map.copyOf(metadata);
    }

    private static Map<String, Object> angularMetadataBase(AngularDecoratorModel model) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("framework", "angular");
        metadata.put("angularDecorator", model.decoratorName());
        metadata.put("angularKind", model.angularKind());
        return metadata;
    }

    private static String stringLiteral(String raw) {
        return AngularLiteralSupport.stringLiteralContent(raw);
    }

    private static Boolean booleanLiteral(String raw) {
        if (raw == null) {
            return null;
        }
        return switch (raw.trim()) {
            case "true" -> Boolean.TRUE;
            case "false" -> Boolean.FALSE;
            default -> null;
        };
    }

    private static Object arrayOrSingleton(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String value = raw.trim();
        if (value.startsWith("[") && value.endsWith("]")) {
            String body = value.substring(1, value.length() - 1).trim();
            if (body.isBlank()) {
                return java.util.List.of();
            }
            java.util.LinkedHashSet<String> values = new java.util.LinkedHashSet<>();
            for (String entry : AngularLiteralSupport.splitTopLevel(body, ',')) {
                String literal = stringLiteral(entry);
                values.add(literal.isBlank() ? entry.trim() : literal);
            }
            return java.util.List.copyOf(values);
        }
        String literal = stringLiteral(value);
        return literal.isBlank() ? value : literal;
    }

    private static void putIfNotBlank(Map<String, Object> metadata, String key, String value) {
        if (value != null && !value.isBlank()) {
            metadata.put(key, value);
        }
    }

    private static void putIfPresent(Map<String, Object> metadata, String key, Object value) {
        if (value == null) {
            return;
        }
        if (value instanceof String s && s.isBlank()) {
            return;
        }
        metadata.put(key, value);
    }
}
