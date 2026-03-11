package info.isaksson.erland.architecturebrowser.indexer.parse;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class TreeSitterParserRegistryFactory {
    private static final List<TreeSitterLanguageDescriptor> ACTIVE_DEFAULT_LANGUAGES = List.of(
        new TreeSitterLanguageDescriptor(ParseLanguage.JAVA, "java", "tree-sitter-java", "tree_sitter_java", List.of(".java"), true),
        new TreeSitterLanguageDescriptor(ParseLanguage.TYPESCRIPT, "typescript", "tree-sitter-typescript", "tree_sitter_typescript", List.of(".ts", ".tsx"), true),
        new TreeSitterLanguageDescriptor(ParseLanguage.JSON, "json", "tree-sitter-json", "tree_sitter_json", List.of(".json"), true),
        new TreeSitterLanguageDescriptor(ParseLanguage.YAML, "yaml", "tree-sitter-yaml", "tree_sitter_yaml", List.of(".yaml", ".yml"), true),
        new TreeSitterLanguageDescriptor(ParseLanguage.SQL, "sql", "tree-sitter-sql", "tree_sitter_sql", List.of(".sql"), true),
        new TreeSitterLanguageDescriptor(ParseLanguage.PROPERTIES, "properties", "tree-sitter-properties", "tree_sitter_properties", List.of(".properties"), true),
        new TreeSitterLanguageDescriptor(ParseLanguage.XML, "xml", "tree-sitter-xml", "tree_sitter_xml", List.of(".xml"), true)
    );

    private static final List<TreeSitterLanguageDescriptor> KNOWN_FUTURE_LANGUAGES = List.of(
        new TreeSitterLanguageDescriptor(ParseLanguage.JAVASCRIPT, "javascript", "tree-sitter-javascript", "tree_sitter_javascript", List.of(".js", ".jsx"), false)
    );

    private TreeSitterParserRegistryFactory() {
    }

    public static List<TreeSitterLanguageDescriptor> defaultLanguageDescriptors() {
        return ACTIVE_DEFAULT_LANGUAGES;
    }

    public static List<TreeSitterLanguageDescriptor> knownFutureLanguageDescriptors() {
        return KNOWN_FUTURE_LANGUAGES;
    }

    public static ParserRegistry createDefaultRegistry() {
        return createDefaultRegistry(TreeSitterConfiguration.fromEnvironment());
    }

    public static ParserRegistry createDefaultRegistry(TreeSitterConfiguration configuration) {
        TreeSitterRuntimeStatus runtimeStatus = TreeSitterRuntimeDetector.detect(configuration);
        if (!runtimeStatus.available()) {
            return unavailableRegistry(runtimeStatus);
        }

        JTreeSitterLanguageLoader loader = new JTreeSitterLanguageLoader(configuration);
        List<SourceParser> parsers = new ArrayList<>();
        for (TreeSitterLanguageDescriptor descriptor : ACTIVE_DEFAULT_LANGUAGES) {
            try {
                parsers.add(new JTreeSitterSourceParser(descriptor.language(), descriptor, loader.load(descriptor)));
            } catch (TreeSitterLibraryLoadException e) {
                Map<String, Object> metadata = new LinkedHashMap<>(runtimeStatus.metadata());
                metadata.put("runtimeAvailable", true);
                metadata.put("language", descriptor.language().inventoryKey());
                metadata.put("sharedLibraryBaseName", descriptor.sharedLibraryBaseName());
                metadata.putAll(e.metadata());
                parsers.add(new UnavailableSourceParser(
                    descriptor.language(),
                    runtimeStatus.languageUnavailable(descriptor.language(), e.getMessage(), metadata)
                ));
            } catch (Throwable t) {
                Map<String, Object> metadata = new LinkedHashMap<>(runtimeStatus.metadata());
                metadata.put("runtimeAvailable", true);
                metadata.put("language", descriptor.language().inventoryKey());
                metadata.put("sharedLibraryBaseName", descriptor.sharedLibraryBaseName());
                metadata.put("exceptionClass", t.getClass().getName());
                metadata.put("exceptionMessage", Objects.toString(t.getMessage(), "<no message>"));
                parsers.add(new UnavailableSourceParser(
                    descriptor.language(),
                    runtimeStatus.languageUnavailable(
                        descriptor.language(),
                        "Failed to load Tree-sitter language library for " + descriptor.language().inventoryKey() + ": " + Objects.toString(t.getMessage(), t.getClass().getSimpleName()),
                        metadata)
                ));
            }
        }
        return new ParserRegistry(parsers);
    }

    private static ParserRegistry unavailableRegistry(TreeSitterRuntimeStatus runtimeStatus) {
        List<SourceParser> parsers = new ArrayList<>();
        for (TreeSitterLanguageDescriptor descriptor : ACTIVE_DEFAULT_LANGUAGES) {
            parsers.add(new UnavailableSourceParser(
                descriptor.language(),
                runtimeStatus.languageUnavailable(
                    descriptor.language(),
                    runtimeStatus.detail(),
                    Map.of("runtimeAvailable", false, "language", descriptor.language().inventoryKey())
                )));
        }
        return new ParserRegistry(parsers);
    }
}
