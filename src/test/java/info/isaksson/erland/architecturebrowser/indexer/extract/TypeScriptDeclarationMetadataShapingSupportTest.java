package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact;
import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractionMode;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;
import info.isaksson.erland.architecturebrowser.indexer.parse.ParseLanguage;
import info.isaksson.erland.architecturebrowser.indexer.parse.ParseStatus;
import info.isaksson.erland.architecturebrowser.indexer.parse.SourceParseRequest;
import info.isaksson.erland.architecturebrowser.indexer.parse.SourceParseResult;
import info.isaksson.erland.architecturebrowser.indexer.parse.SyntaxNode;
import info.isaksson.erland.architecturebrowser.indexer.parse.SyntaxTree;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TypeScriptDeclarationMetadataShapingSupportTest {
    @Test
    void shapesNamedMethodAndPropertyMetadataThroughDedicatedSupports() {
        SourceParseRequest request = new SourceParseRequest(
            Path.of("/tmp/src/app/user.ts"),
            "src/app/user.ts",
            ParseLanguage.TYPESCRIPT,
            ""
        );
        SourceParseResult parseResult = new SourceParseResult(
            request,
            ParseStatus.SUCCESS,
            new SyntaxTree(ParseLanguage.TYPESCRIPT, "test", syntaxTree("program", ""), false, 1),
            List.of(),
            Map.of()
        );
        ExtractionAccumulator accumulator = new ExtractionAccumulator();

        SyntaxNode classNode = syntaxTree(
            "class_declaration",
            "@Injectable() export class UserService {}",
            child("type_identifier", "UserService"),
            child("decorator", "@Injectable()")
        );
        ExtractedEntityFact named = TypeScriptNamedDeclarationSemanticsSupport.addNamedEntityFromNode(
            parseResult,
            accumulator,
            "entity:file:1",
            "src/app/user.ts",
            classNode,
            EntityKind.CLASS,
            "class_declaration",
            "class",
            ExtractionMode.SYNTAX_TREE
        );
        assertEquals("UserService", named.name());
        assertEquals("UserService", named.metadata().get("qualifiedName"));
        assertEquals(List.of("Injectable"), named.metadata().get("decorators"));

        SyntaxNode methodNode = syntaxTree(
            "method_definition",
            "@Memoize() listUsers(filter: UserFilter): Promise<User[]> {}",
            child("property_identifier", "listUsers"),
            child("formal_parameters", "(filter: UserFilter)"),
            child("type_annotation", ": Promise<User[]>"),
            child("decorator", "@Memoize()")
        );
        ExtractedEntityFact method = TypeScriptMethodDeclarationSemanticsSupport.toTypeScriptMethodEntity(
            parseResult,
            "src/app/user.ts",
            ExtractionMode.SYNTAX_TREE,
            "scope:file:1",
            methodNode,
            "UserService",
            "class"
        );
        assertEquals("listUsers", method.name());
        assertEquals("UserService", method.metadata().get("ownerQualifiedName"));
        assertEquals(List.of("Memoize"), method.metadata().get("decorators"));

        SyntaxNode propertyNode = syntaxTree(
            "public_field_definition",
            "@Input() public readonly users?: User[]",
            child("property_identifier", "users"),
            child("type_annotation", ": User[]"),
            child("accessibility_modifier", "public"),
            child("readonly", "readonly"),
            child("?", "?"),
            child("decorator", "@Input()")
        );
        ExtractedEntityFact property = TypeScriptPropertyDeclarationSemanticsSupport.toTypeScriptPropertyEntity(
            parseResult,
            "src/app/user.ts",
            ExtractionMode.SYNTAX_TREE,
            "scope:file:1",
            propertyNode,
            "UserService",
            "class"
        );
        assertEquals("users", property.name());
        assertEquals("User[]", property.metadata().get("declaredType"));
        assertEquals(true, property.metadata().get("optional"));
        assertEquals(true, property.metadata().get("readonly"));
        assertEquals("public", property.metadata().get("accessibility"));
        assertEquals(List.of("Input"), property.metadata().get("decorators"));
        assertTrue(((List<?>) property.metadata().get("modifiers")).contains("public"));
    }

    private static SyntaxNode syntaxTree(String type, String text, SyntaxNode... children) {
        return new SyntaxNode(type, true, 0, text.length(), 0, 0, 0, text.length(), false, false, text, List.of(children));
    }

    private static SyntaxNode child(String type, String text) {
        return new SyntaxNode(type, true, 0, text.length(), 0, 0, 0, text.length(), false, false, text, List.of());
    }
}
