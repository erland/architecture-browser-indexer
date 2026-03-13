package info.isaksson.erland.architecturebrowser.indexer.naming;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ScopeKind;

public final class DisplayNamePolicy {
    private DisplayNamePolicy() {
    }

    public static String scopeDisplayName(ScopeKind kind, String canonicalName, String language) {
        if (canonicalName == null || canonicalName.isBlank()) {
            return canonicalName;
        }
        return switch (kind) {
            case DIRECTORY, FILE, MODULE, COMPONENT -> basenamePath(canonicalName);
            case PACKAGE -> basenameSegment(canonicalName, ".");
            case REPOSITORY -> canonicalName;
        };
    }

    public static String entityDisplayName(EntityKind kind, String canonicalName, String language) {
        if (canonicalName == null || canonicalName.isBlank()) {
            return canonicalName;
        }
        return switch (kind) {
            case FUNCTION, CONFIG_ARTIFACT -> memberOrKeyName(canonicalName, language);
            case MODULE, CLASS, INTERFACE, SERVICE, PERSISTENCE_ADAPTER, UI_MODULE, STARTUP_POINT -> typeOrModuleName(canonicalName, language);
            case ENDPOINT, DATASTORE, EXTERNAL_SYSTEM -> canonicalName;
        };
    }

    private static String memberOrKeyName(String value, String language) {
        if (value.contains("#")) {
            return basenameSegment(value, "#");
        }
        if (looksQualifiedName(value, language)) {
            return basenameSegment(value, ".");
        }
        if (looksPath(value)) {
            return basenamePath(value);
        }
        return value;
    }

    private static String typeOrModuleName(String value, String language) {
        if (value.contains("#")) {
            return basenameSegment(value, "#");
        }
        if (looksPath(value)) {
            return basenamePath(value);
        }
        if (looksQualifiedName(value, language)) {
            return basenameSegment(value, ".");
        }
        return value;
    }

    private static boolean looksPath(String value) {
        return value.contains("/") || value.contains("\\");
    }

    private static boolean looksQualifiedName(String value, String language) {
        if (!value.contains(".")) {
            return false;
        }
        if (looksPath(value) || value.contains(" ") || value.startsWith(".")) {
            return false;
        }
        if ("java".equalsIgnoreCase(language)) {
            return true;
        }
        String[] segments = value.split("\\.");
        if (segments.length < 2) {
            return false;
        }
        for (String segment : segments) {
            if (segment.isBlank()) {
                return false;
            }
            char first = segment.charAt(0);
            if (!(Character.isLetter(first) || first == '_')) {
                return false;
            }
            for (int i = 1; i < segment.length(); i++) {
                char ch = segment.charAt(i);
                if (!(Character.isLetterOrDigit(ch) || ch == '_' || ch == '$')) {
                    return false;
                }
            }
        }
        return true;
    }

    private static String basenamePath(String value) {
        return basenameSegment(value.replace('\\', '/'), "/");
    }

    private static String basenameSegment(String value, String delimiter) {
        int index = value.lastIndexOf(delimiter);
        return index >= 0 && index + delimiter.length() < value.length() ? value.substring(index + delimiter.length()) : value;
    }
}
