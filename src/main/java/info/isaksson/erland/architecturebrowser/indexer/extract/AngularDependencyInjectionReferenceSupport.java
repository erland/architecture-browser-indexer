package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class AngularDependencyInjectionReferenceSupport {
    private static final Pattern INJECT_TOKEN_PATTERN = Pattern.compile("@Inject\\s*\\(\\s*([A-Za-z_$][\\w.$]*)\\s*\\)");
    private static final Pattern TYPE_ANNOTATION_PATTERN = Pattern.compile(":\\s*([A-Za-z_$][\\w.$]*)");
    private static final Set<String> IGNORED_TYPES = Set.of(
        "string", "number", "boolean", "object", "unknown", "any", "void", "null", "undefined"
    );

    private AngularDependencyInjectionReferenceSupport() {
    }

    static List<String> extractConstructorParameterBlocks(String snippet) {
        if (snippet == null || snippet.isBlank()) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        int searchFrom = 0;
        while (searchFrom < snippet.length()) {
            int constructorIndex = snippet.indexOf("constructor", searchFrom);
            if (constructorIndex < 0) {
                break;
            }
            int parenStart = snippet.indexOf('(', constructorIndex);
            if (parenStart < 0) {
                break;
            }
            int parenEnd = AngularLiteralSupport.findMatchingParen(snippet, parenStart);
            if (parenEnd < 0) {
                break;
            }
            String parameters = snippet.substring(parenStart + 1, parenEnd).trim();
            if (!parameters.isBlank()) {
                result.add(parameters);
            }
            searchFrom = parenEnd + 1;
        }
        return List.copyOf(result);
    }

    static AngularInjectionReference parseInjectionReference(String parameter) {
        if (parameter == null || parameter.isBlank()) {
            return null;
        }
        String raw = parameter.strip();
        Matcher injectMatcher = INJECT_TOKEN_PATTERN.matcher(raw);
        if (injectMatcher.find()) {
            String tokenName = AngularReferenceSupport.normalizeReference(injectMatcher.group(1));
            if (!tokenName.isBlank()) {
                return new AngularInjectionReference(tokenName, tokenName, inferTokenKind(tokenName), "token");
            }
        }
        Matcher typeMatcher = TYPE_ANNOTATION_PATTERN.matcher(raw);
        if (typeMatcher.find()) {
            String typeName = AngularReferenceSupport.normalizeReference(typeMatcher.group(1));
            if (!typeName.isBlank() && !IGNORED_TYPES.contains(typeName.toLowerCase(Locale.ROOT))) {
                return new AngularInjectionReference(typeName, typeName, inferProviderTargetKind(typeName), "type");
            }
        }
        return null;
    }

    static EntityKind inferProviderTargetKind(String raw) {
        if (raw == null) {
            return EntityKind.CLASS;
        }
        String trimmed = raw.strip();
        if (trimmed.endsWith("()")) {
            return EntityKind.FUNCTION;
        }
        return EntityKind.CLASS;
    }

    static EntityKind inferTokenKind(String raw) {
        if (raw == null || raw.isBlank()) {
            return EntityKind.MODULE;
        }
        return raw.equals(raw.toUpperCase(Locale.ROOT)) || raw.endsWith("TOKEN") || raw.contains("CONFIG")
            ? EntityKind.MODULE
            : EntityKind.CLASS;
    }
}
