package info.isaksson.erland.architecturebrowser.indexer.regression;

import info.isaksson.erland.architecturebrowser.indexer.extract.StructuralExtractionService;
import info.isaksson.erland.architecturebrowser.indexer.extract.StructuralExtractorRegistry;
import info.isaksson.erland.architecturebrowser.indexer.extract.model.StructuralExtractionResult;
import info.isaksson.erland.architecturebrowser.indexer.interpret.InterpretationRegistry;
import info.isaksson.erland.architecturebrowser.indexer.interpret.InterpretationService;
import info.isaksson.erland.architecturebrowser.indexer.interpret.model.InterpretationResult;
import info.isaksson.erland.architecturebrowser.indexer.ir.ArchitectureIrFactory;
import info.isaksson.erland.architecturebrowser.indexer.ir.ArchitectureIrValidator;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureIndexDocument;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RepositorySource;
import info.isaksson.erland.architecturebrowser.indexer.parse.ParseBatchResult;
import info.isaksson.erland.architecturebrowser.indexer.parse.ParseLanguage;
import info.isaksson.erland.architecturebrowser.indexer.parse.ParseStatus;
import info.isaksson.erland.architecturebrowser.indexer.parse.SourceParseRequest;
import info.isaksson.erland.architecturebrowser.indexer.parse.SourceParseResult;
import info.isaksson.erland.architecturebrowser.indexer.parse.SyntaxNode;
import info.isaksson.erland.architecturebrowser.indexer.parse.SyntaxTree;
import info.isaksson.erland.architecturebrowser.indexer.scan.FileInventory;
import info.isaksson.erland.architecturebrowser.indexer.scan.FileInventoryEntry;
import info.isaksson.erland.architecturebrowser.indexer.topology.TopologyService;
import info.isaksson.erland.architecturebrowser.indexer.topology.model.TopologyResult;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

abstract class AbstractArchitectureDependencyFixtureTestSupport {

    protected static ArchitectureIndexDocument buildDocument(List<JavaFixtureFile> files) {
        FileInventory inventory = new FileInventory(
            files.stream()
                .map(file -> new FileInventoryEntry(file.path(), file.source().length(), "java", "source", "java", false, List.of("java")))
                .toList(),
            files.size(),
            files.size(),
            0,
            Set.of("java"),
            Set.of("java")
        );

        ParseBatchResult parseBatchResult = new ParseBatchResult(
            files.stream()
                .map(file -> new SourceParseResult(
                    new SourceParseRequest(Path.of(file.path()), file.path(), ParseLanguage.JAVA, file.source()),
                    ParseStatus.SUCCESS,
                    new SyntaxTree(ParseLanguage.JAVA, "tree-sitter-jtreesitter", file.root(), false, file.root().nodeCount()),
                    List.of(),
                    Map.of("parserBackend", "tree-sitter-jtreesitter")
                ))
                .toList(),
            Map.of(ParseLanguage.JAVA, files.size()),
            Map.of(ParseStatus.SUCCESS, files.size())
        );

        StructuralExtractionResult extraction = new StructuralExtractionService(StructuralExtractorRegistry.defaultRegistry()).extract(parseBatchResult);
        InterpretationResult interpretation = new InterpretationService(InterpretationRegistry.defaultRegistry()).interpret(extraction);
        TopologyResult topology = new TopologyService().infer(inventory, extraction, interpretation);

        return ArchitectureIrFactory.createInventoryDocument(
            RepositorySource.localPath("fixture", "/tmp/fixture", Instant.parse("2026-03-15T12:00:00Z")),
            "0.1.0-SNAPSHOT",
            inventory,
            List.of(),
            parseBatchResult,
            extraction,
            interpretation,
            topology
        );
    }

    @SuppressWarnings("unchecked")
    protected static List<Map<String, Object>> dependencyViewList(ArchitectureIndexDocument document, String key) {
        Map<String, Object> dependencyViews = (Map<String, Object>) document.metadata().get("dependencyViews");
        return (List<Map<String, Object>>) dependencyViews.get(key);
    }

    protected static boolean hasPackageDependency(List<Map<String, Object>> dependencies, String sourcePackage, String targetPackage, String dependencySource,
                                                boolean internalTarget, boolean externalTarget) {
        return dependencies.stream().anyMatch(dep ->
            sourcePackage.equals(dep.get("sourcePackageName"))
                && targetPackage.equals(dep.get("targetPackageName"))
                && ((List<?>) dep.get("dependencySources")).contains(dependencySource)
                && Boolean.valueOf(internalTarget).equals(dep.get("internalTarget"))
                && Boolean.valueOf(externalTarget).equals(dep.get("externalTarget"))
        );
    }

    protected static JavaFixtureFile javaFile(String path, String packageName, List<String> imports, List<JavaTypeSpec> types) {
        List<String> lines = new ArrayList<>();
        lines.add("package " + packageName + ";");
        imports.forEach(imp -> lines.add("import " + imp + ";"));
        types.forEach(type -> lines.add(type.render()));
        String source = String.join("\n", lines) + "\n";

        List<SyntaxNode> rootChildren = new ArrayList<>();
        rootChildren.add(packageDeclarationNode(source, packageName));
        imports.forEach(imp -> rootChildren.add(importDeclarationNode(source, imp)));
        types.forEach(type -> rootChildren.add(type.toSyntaxNode(source)));

        int[] end = lineAndColumn(source, source.length());
        SyntaxNode root = new SyntaxNode("program", true, 0, source.length(), 0, 0, end[0], end[1], false, false, source, rootChildren);
        return new JavaFixtureFile(path, source, root);
    }

    protected static SyntaxNode packageDeclarationNode(String source, String packageName) {
        String snippet = "package " + packageName + ";";
        int start = source.indexOf(snippet);
        int end = start + snippet.length();
        int[] from = lineAndColumn(source, start);
        int[] to = lineAndColumn(source, end);
        int idStart = source.indexOf(packageName, start);
        int idEnd = idStart + packageName.length();
        int[] idFrom = lineAndColumn(source, idStart);
        int[] idTo = lineAndColumn(source, idEnd);
        return new SyntaxNode("package_declaration", true, start, end, from[0], from[1], to[0], to[1], false, false, snippet, List.of(
            new SyntaxNode("scoped_identifier", true, idStart, idEnd, idFrom[0], idFrom[1], idTo[0], idTo[1], false, false, packageName, List.of())
        ));
    }

    protected static SyntaxNode importDeclarationNode(String source, String qualifiedImport) {
        String snippet = "import " + qualifiedImport + ";";
        int start = source.indexOf(snippet);
        int end = start + snippet.length();
        int[] from = lineAndColumn(source, start);
        int[] to = lineAndColumn(source, end);
        int idStart = source.indexOf(qualifiedImport, start);
        int idEnd = idStart + qualifiedImport.length();
        int[] idFrom = lineAndColumn(source, idStart);
        int[] idTo = lineAndColumn(source, idEnd);
        return new SyntaxNode("import_declaration", true, start, end, from[0], from[1], to[0], to[1], false, false, snippet, List.of(
            new SyntaxNode("scoped_identifier", true, idStart, idEnd, idFrom[0], idFrom[1], idTo[0], idTo[1], false, false, qualifiedImport, List.of())
        ));
    }

    protected static JavaTypeSpec javaClass(String name, List<String> extendsTypes, List<String> implementsTypes, List<JavaFieldSpec> fields, List<JavaMethodSpec> methods) {
        return new JavaTypeSpec("class_declaration", name, extendsTypes, implementsTypes, fields, methods);
    }

    protected static JavaTypeSpec javaInterface(String name, List<String> extendsTypes, List<JavaFieldSpec> fields, List<JavaMethodSpec> methods) {
        return new JavaTypeSpec("interface_declaration", name, extendsTypes, List.of(), fields, methods);
    }

    protected static JavaFieldSpec field(String type, String name) {
        return new JavaFieldSpec(type, name);
    }

    protected static JavaMethodSpec method(String returnType, String name, List<JavaParameterSpec> parameters) {
        return new JavaMethodSpec(returnType, name, parameters);
    }

    protected static int[] lineAndColumn(String source, int offset) {
        int line = 0;
        int col = 0;
        for (int i = 0; i < offset && i < source.length(); i++) {
            if (source.charAt(i) == '\n') {
                line++;
                col = 0;
            } else {
                col++;
            }
        }
        return new int[]{line, col};
    }

    protected record JavaFixtureFile(String path, String source, SyntaxNode root) {}

    protected record JavaTypeSpec(String nodeType, String name, List<String> extendsTypes, List<String> implementsTypes,
                                List<JavaFieldSpec> fields, List<JavaMethodSpec> methods) {
        private String render() {
            String keyword = nodeType.equals("interface_declaration") ? "interface" : "class";
            StringBuilder sb = new StringBuilder();
            sb.append("public ").append(keyword).append(' ').append(name);
            if (!extendsTypes.isEmpty()) {
                sb.append(" extends ").append(String.join(", ", extendsTypes));
            }
            if (!implementsTypes.isEmpty()) {
                sb.append(" implements ").append(String.join(", ", implementsTypes));
            }
            sb.append(" {\n");
            for (JavaFieldSpec field : fields) {
                sb.append("  ").append(field.render()).append("\n");
            }
            for (JavaMethodSpec method : methods) {
                sb.append("  ").append(method.render()).append("\n");
            }
            sb.append("}");
            return sb.toString();
        }

        private SyntaxNode toSyntaxNode(String source) {
            String snippet = render();
            int start = source.indexOf(snippet);
            int end = start + snippet.length();
            int[] from = lineAndColumn(source, start);
            int[] to = lineAndColumn(source, end);

            List<SyntaxNode> children = new ArrayList<>();
            int nameStart = source.indexOf(name, start);
            int nameEnd = nameStart + name.length();
            int[] nameFrom = lineAndColumn(source, nameStart);
            int[] nameTo = lineAndColumn(source, nameEnd);
            children.add(new SyntaxNode("identifier", true, nameStart, nameEnd, nameFrom[0], nameFrom[1], nameTo[0], nameTo[1], false, false, name, List.of()));

            int searchCursor = nameEnd;
            for (String typeName : extendsTypes) {
                int typeStart = source.indexOf(typeName, searchCursor);
                int typeEnd = typeStart + typeName.length();
                int[] typeFrom = lineAndColumn(source, typeStart);
                int[] typeTo = lineAndColumn(source, typeEnd);
                children.add(new SyntaxNode("type_identifier", true, typeStart, typeEnd, typeFrom[0], typeFrom[1], typeTo[0], typeTo[1], false, false, typeName, List.of()));
                searchCursor = typeEnd;
            }
            for (String typeName : implementsTypes) {
                int typeStart = source.indexOf(typeName, searchCursor);
                int typeEnd = typeStart + typeName.length();
                int[] typeFrom = lineAndColumn(source, typeStart);
                int[] typeTo = lineAndColumn(source, typeEnd);
                children.add(new SyntaxNode("type_identifier", true, typeStart, typeEnd, typeFrom[0], typeFrom[1], typeTo[0], typeTo[1], false, false, typeName, List.of()));
                searchCursor = typeEnd;
            }
            fields.forEach(field -> children.add(field.toSyntaxNode(source, start)));
            methods.forEach(method -> children.add(method.toSyntaxNode(source, start)));

            return new SyntaxNode(nodeType, true, start, end, from[0], from[1], to[0], to[1], false, false, snippet, children);
        }
    }

    protected record JavaFieldSpec(String type, String name) {
        private String render() {
            return "private " + type + " " + name + ";";
        }

        private SyntaxNode toSyntaxNode(String source, int typeStartSearch) {
            String snippet = render();
            int start = source.indexOf(snippet, typeStartSearch);
            int end = start + snippet.length();
            int[] from = lineAndColumn(source, start);
            int[] to = lineAndColumn(source, end);
            String declSnippet = type + " " + name;
            int declStart = source.indexOf(declSnippet, start);
            int declEnd = declStart + declSnippet.length();
            int[] declFrom = lineAndColumn(source, declStart);
            int[] declTo = lineAndColumn(source, declEnd);
            int typeOffset = source.indexOf(type, declStart);
            int typeEnd = typeOffset + type.length();
            int[] typeFrom = lineAndColumn(source, typeOffset);
            int[] typeTo = lineAndColumn(source, typeEnd);
            int nameOffset = source.indexOf(name, typeEnd);
            int nameEnd = nameOffset + name.length();
            int[] nameFrom = lineAndColumn(source, nameOffset);
            int[] nameTo = lineAndColumn(source, nameEnd);
            return new SyntaxNode("field_declaration", true, start, end, from[0], from[1], to[0], to[1], false, false, snippet, List.of(
                new SyntaxNode("variable_declarator", true, declStart, declEnd, declFrom[0], declFrom[1], declTo[0], declTo[1], false, false, declSnippet, List.of(
                    new SyntaxNode("type_identifier", true, typeOffset, typeEnd, typeFrom[0], typeFrom[1], typeTo[0], typeTo[1], false, false, type, List.of()),
                    new SyntaxNode("identifier", true, nameOffset, nameEnd, nameFrom[0], nameFrom[1], nameTo[0], nameTo[1], false, false, name, List.of())
                ))
            ));
        }
    }

    protected record JavaMethodSpec(String returnType, String name, List<JavaParameterSpec> parameters) {
        private String render() {
            String params = parameters.stream().map(JavaParameterSpec::render).collect(Collectors.joining(", "));
            return "public " + returnType + " " + name + "(" + params + ") { return null; }";
        }

        private SyntaxNode toSyntaxNode(String source, int typeStartSearch) {
            String snippet = render();
            int start = source.indexOf(snippet, typeStartSearch);
            int end = start + snippet.length();
            int[] from = lineAndColumn(source, start);
            int[] to = lineAndColumn(source, end);

            int returnTypeStart = source.indexOf(returnType, start);
            int returnTypeEnd = returnTypeStart + returnType.length();
            int[] returnFrom = lineAndColumn(source, returnTypeStart);
            int[] returnTo = lineAndColumn(source, returnTypeEnd);
            int nameStart = source.indexOf(name, returnTypeEnd);
            int nameEnd = nameStart + name.length();
            int[] nameFrom = lineAndColumn(source, nameStart);
            int[] nameTo = lineAndColumn(source, nameEnd);

            List<SyntaxNode> parameterNodes = new ArrayList<>();
            int paramSearch = nameEnd;
            for (JavaParameterSpec parameter : parameters) {
                parameterNodes.add(parameter.toSyntaxNode(source, paramSearch));
                paramSearch = parameterNodes.get(parameterNodes.size() - 1).endByte();
            }
            int paramsStart = source.indexOf('(', nameEnd);
            int paramsEnd = source.indexOf(')', paramsStart) + 1;
            int[] paramsFrom = lineAndColumn(source, paramsStart);
            int[] paramsTo = lineAndColumn(source, paramsEnd);

            List<SyntaxNode> children = new ArrayList<>();
            children.add(new SyntaxNode("type_identifier", true, returnTypeStart, returnTypeEnd, returnFrom[0], returnFrom[1], returnTo[0], returnTo[1], false, false, returnType, List.of()));
            children.add(new SyntaxNode("identifier", true, nameStart, nameEnd, nameFrom[0], nameFrom[1], nameTo[0], nameTo[1], false, false, name, List.of()));
            children.add(new SyntaxNode("formal_parameters", true, paramsStart, paramsEnd, paramsFrom[0], paramsFrom[1], paramsTo[0], paramsTo[1], false, false,
                source.substring(paramsStart, paramsEnd), parameterNodes));
            return new SyntaxNode("method_declaration", true, start, end, from[0], from[1], to[0], to[1], false, false, snippet, children);
        }
    }

    protected record JavaParameterSpec(String type, String name) {
        private String render() {
            return type + " " + name;
        }

        private SyntaxNode toSyntaxNode(String source, int searchFrom) {
            String snippet = render();
            int start = source.indexOf(snippet, searchFrom);
            int end = start + snippet.length();
            int[] from = lineAndColumn(source, start);
            int[] to = lineAndColumn(source, end);
            int typeStart = source.indexOf(type, start);
            int typeEnd = typeStart + type.length();
            int[] typeFrom = lineAndColumn(source, typeStart);
            int[] typeTo = lineAndColumn(source, typeEnd);
            int nameStart = source.indexOf(name, typeEnd);
            int nameEnd = nameStart + name.length();
            int[] nameFrom = lineAndColumn(source, nameStart);
            int[] nameTo = lineAndColumn(source, nameEnd);
            return new SyntaxNode("formal_parameter", true, start, end, from[0], from[1], to[0], to[1], false, false, snippet, List.of(
                new SyntaxNode("type_identifier", true, typeStart, typeEnd, typeFrom[0], typeFrom[1], typeTo[0], typeTo[1], false, false, type, List.of()),
                new SyntaxNode("identifier", true, nameStart, nameEnd, nameFrom[0], nameFrom[1], nameTo[0], nameTo[1], false, false, name, List.of())
            ));
        }
    }
}
