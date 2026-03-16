package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;
import info.isaksson.erland.architecturebrowser.indexer.parse.ParseIssue;
import info.isaksson.erland.architecturebrowser.indexer.parse.ParseLanguage;
import info.isaksson.erland.architecturebrowser.indexer.parse.ParseStatus;
import info.isaksson.erland.architecturebrowser.indexer.parse.SourceParseRequest;
import info.isaksson.erland.architecturebrowser.indexer.parse.SourceParseResult;
import info.isaksson.erland.architecturebrowser.indexer.parse.SyntaxNode;
import info.isaksson.erland.architecturebrowser.indexer.parse.SyntaxTree;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class JavaSyntaxTreeExtractionStageTestSupport {
    private JavaSyntaxTreeExtractionStageTestSupport() {}

    static ExtractionAccumulator extract(String relativePath, String source, SyntaxNode... children) {
        SourceParseResult parseResult = new SourceParseResult(
            new SourceParseRequest(Path.of(relativePath), relativePath, ParseLanguage.JAVA, source),
            ParseStatus.SUCCESS,
            new SyntaxTree(ParseLanguage.JAVA, "tree-sitter-jtreesitter", program(source, children), false, Math.max(1, source.split("\\R", -1).length)),
            List.<ParseIssue>of(),
            Map.of("parserBackend", "tree-sitter-jtreesitter")
        );
        return new JavaSyntaxTreeExtractionStage().extract(parseResult, new ExtractionAccumulator());
    }

    static ExtractedEntityFact classByQualifiedName(ExtractionAccumulator accumulator, String qualifiedName) {
        return accumulator.entities().stream()
            .filter(entity -> entity.kind() == EntityKind.CLASS)
            .filter(entity -> qualifiedName.equals(entity.metadata().get("qualifiedName")))
            .findFirst()
            .orElseThrow();
    }

    static ExtractedEntityFact fieldByOwner(ExtractionAccumulator accumulator, String ownerQualifiedName, String fieldName) {
        return accumulator.entities().stream()
            .filter(entity -> entity.kind() == EntityKind.FIELD)
            .filter(entity -> fieldName.equals(entity.name()))
            .filter(entity -> ownerQualifiedName.equals(entity.metadata().get("ownerQualifiedName")))
            .findFirst()
            .orElseThrow();
    }

    static ExtractedEntityFact methodByOwner(ExtractionAccumulator accumulator, String ownerQualifiedName, String methodName) {
        return accumulator.entities().stream()
            .filter(entity -> entity.kind() == EntityKind.FUNCTION)
            .filter(entity -> methodName.equals(entity.name()))
            .filter(entity -> ownerQualifiedName.equals(entity.metadata().get("ownerQualifiedName")))
            .findFirst()
            .orElseThrow();
    }

    static SyntaxNode program(String source, SyntaxNode... children) {
        return new SyntaxNode("program", true, 0, source.length(), 0, 0, 0, 0, false, false, source, List.of(children));
    }

    static SyntaxNode packageDecl(int line, String snippet, String qualifiedName) {
        return new SyntaxNode("package_declaration", true, 0, 0, line, 0, line, 0, false, false, snippet, List.of(
            new SyntaxNode("scoped_identifier", true, 0, 0, line, 0, line, 0, false, false, qualifiedName, List.of())
        ));
    }

    static SyntaxNode importDecl(int line, String snippet) {
        return new SyntaxNode("import_declaration", true, 0, 0, line, 0, line, 0, false, false, snippet, List.of());
    }

    static SyntaxNode interfaceDecl(int line, String name, String snippet) {
        return new SyntaxNode("interface_declaration", true, 0, 0, line, 0, line, 0, false, false, snippet, List.of(
            new SyntaxNode("identifier", true, 0, 0, line, 0, line, 0, false, false, name, List.of())
        ));
    }

    static SyntaxNode classDecl(int line, String name, String snippet, SyntaxNode... membersAndTypes) {
        ArrayList<SyntaxNode> children = new ArrayList<>();
        children.add(new SyntaxNode("identifier", true, 0, 0, line, 0, line, 0, false, false, name, List.of()));
        children.addAll(List.of(membersAndTypes));
        return new SyntaxNode("class_declaration", true, 0, 0, line, 0, line, 0, false, false, snippet, List.copyOf(children));
    }

    static SyntaxNode fieldDecl(int line, String snippet, String declaredTypeSnippet, String name, SyntaxNode... annotations) {
        ArrayList<SyntaxNode> children = new ArrayList<>();
        children.addAll(List.of(annotations));
        children.add(declaredTypeNode(line, declaredTypeSnippet));
        children.add(new SyntaxNode("variable_declarator", true, 0, 0, line, 0, line, 0, false, false, name, List.of(
            new SyntaxNode("identifier", true, 0, 0, line, 0, line, 0, false, false, name, List.of())
        )));
        return new SyntaxNode("field_declaration", true, 0, 0, line, 0, line, 0, false, false, snippet, List.copyOf(children));
    }

    static SyntaxNode methodDecl(int line, String snippet, String returnType, String name, String parameters, SyntaxNode... annotations) {
        ArrayList<SyntaxNode> children = new ArrayList<>();
        children.addAll(List.of(annotations));
        children.add(typeIdentifier(line, returnType));
        children.add(new SyntaxNode("identifier", true, 0, 0, line, 0, line, 0, false, false, name, List.of()));
        children.add(new SyntaxNode("formal_parameters", true, 0, 0, line, 0, line, 0, false, false, parameters, List.of()));
        return new SyntaxNode("method_declaration", true, 0, 0, line, 0, line, 0, false, false, snippet, List.copyOf(children));
    }

    static SyntaxNode annotation(int line, String snippet) {
        return new SyntaxNode("annotation", true, 0, 0, line, 0, line, 0, false, false, snippet, List.of());
    }

    static SyntaxNode markerAnnotation(int line, String snippet) {
        return new SyntaxNode("marker_annotation", true, 0, 0, line, 0, line, 0, false, false, snippet, List.of());
    }

    static SyntaxNode typeIdentifier(int line, String snippet) {
        return new SyntaxNode("type_identifier", true, 0, 0, line, 0, line, 0, false, false, snippet, List.of());
    }

    static SyntaxNode declaredTypeNode(int line, String snippet) {
        if (!snippet.contains("<")) {
            return typeIdentifier(line, snippet);
        }
        String rawType = snippet.substring(0, snippet.indexOf('<'));
        String genericArgument = snippet.substring(snippet.indexOf('<') + 1, snippet.lastIndexOf('>'));
        return new SyntaxNode("generic_type", true, 0, 0, line, 0, line, 0, false, false, snippet, List.of(
            typeIdentifier(line, rawType),
            typeIdentifier(line, genericArgument)
        ));
    }
}
