package info.isaksson.erland.architecturebrowser.indexer.parse;

import java.util.ArrayList;
import java.util.List;

public final class TreeSitterParserRegistryFactory {
    private static final List<TreeSitterLanguageDescriptor> DEFAULT_LANGUAGES = List.of(
        new TreeSitterLanguageDescriptor(ParseLanguage.JAVA, "java", List.of(".java"), true),
        new TreeSitterLanguageDescriptor(ParseLanguage.JAVASCRIPT, "javascript", List.of(".js", ".jsx"), true),
        new TreeSitterLanguageDescriptor(ParseLanguage.TYPESCRIPT, "typescript", List.of(".ts", ".tsx"), true),
        new TreeSitterLanguageDescriptor(ParseLanguage.JSON, "json", List.of(".json"), false),
        new TreeSitterLanguageDescriptor(ParseLanguage.YAML, "yaml", List.of(".yaml", ".yml"), false),
        new TreeSitterLanguageDescriptor(ParseLanguage.SQL, "sql", List.of(".sql"), false),
        new TreeSitterLanguageDescriptor(ParseLanguage.PROPERTIES, "properties", List.of(".properties"), false),
        new TreeSitterLanguageDescriptor(ParseLanguage.XML, "xml", List.of(".xml"), false)
    );

    private TreeSitterParserRegistryFactory() {
    }

    public static List<TreeSitterLanguageDescriptor> defaultLanguageDescriptors() {
        return DEFAULT_LANGUAGES;
    }

    public static ParserRegistry createDefaultRegistry() {
        TreeSitterRuntimeStatus runtimeStatus = TreeSitterRuntimeDetector.detect();
        List<SourceParser> parsers = new ArrayList<>();
        for (TreeSitterLanguageDescriptor descriptor : DEFAULT_LANGUAGES) {
            parsers.add(new TreeSitterUnavailableParser(descriptor.language(), runtimeStatus));
        }
        return new ParserRegistry(parsers);
    }
}
