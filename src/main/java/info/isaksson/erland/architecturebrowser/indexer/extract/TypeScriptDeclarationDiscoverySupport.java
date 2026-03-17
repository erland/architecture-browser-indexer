package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;
import info.isaksson.erland.architecturebrowser.indexer.parse.SyntaxNode;

import java.util.List;
import java.util.Set;

final class TypeScriptDeclarationDiscoverySupport {
    private TypeScriptDeclarationDiscoverySupport() {
    }

    static TypeScriptDiscoveredDeclarations discover(SyntaxNode root) {
        return new TypeScriptDiscoveredDeclarations(
            discoverNamedTypeDeclarations(root),
            SyntaxTreeExtractionSupport.findAllByType(root, Set.of("class_declaration")),
            SyntaxTreeExtractionSupport.findAllByType(root, Set.of("interface_declaration")),
            SyntaxTreeExtractionSupport.findAllByType(root, Set.of("function_declaration")),
            discoverArrowFunctionDeclarators(root)
        );
    }

    private static List<DiscoveredTypeDeclaration> discoverNamedTypeDeclarations(SyntaxNode root) {
        return List.of(
            new DiscoveredTypeDeclaration("type_alias_declaration", EntityKind.INTERFACE, "type_alias_declaration", "typeAlias"),
            new DiscoveredTypeDeclaration("enum_declaration", EntityKind.CLASS, "enum_declaration", "enum"),
            new DiscoveredTypeDeclaration("class_declaration", EntityKind.CLASS, "class_declaration", "class"),
            new DiscoveredTypeDeclaration("interface_declaration", EntityKind.INTERFACE, "interface_declaration", "interface")
        ).stream()
            .flatMap(specification -> SyntaxTreeExtractionSupport.findAllByType(root, Set.of(specification.nodeType())).stream()
                .map(node -> specification.withNode(node)))
            .toList();
    }

    private static List<SyntaxNode> discoverArrowFunctionDeclarators(SyntaxNode root) {
        return SyntaxTreeExtractionSupport.findAllByType(root, Set.of("variable_declarator")).stream()
            .filter(variableDeclarator -> SyntaxTreeExtractionSupport.containsDescendantType(variableDeclarator, "arrow_function"))
            .toList();
    }

    record TypeScriptDiscoveredDeclarations(
        List<DiscoveredTypeDeclaration> namedTypeDeclarations,
        List<SyntaxNode> classDeclarations,
        List<SyntaxNode> interfaceDeclarations,
        List<SyntaxNode> functionDeclarations,
        List<SyntaxNode> arrowFunctionDeclarators
    ) {
    }

    record DiscoveredTypeDeclaration(
        SyntaxNode node,
        String nodeType,
        EntityKind entityKind,
        String matchedKind,
        String declarationKind
    ) {
        private DiscoveredTypeDeclaration(String nodeType, EntityKind entityKind, String matchedKind, String declarationKind) {
            this(null, nodeType, entityKind, matchedKind, declarationKind);
        }

        DiscoveredTypeDeclaration withNode(SyntaxNode node) {
            return new DiscoveredTypeDeclaration(node, nodeType, entityKind, matchedKind, declarationKind);
        }
    }
}
