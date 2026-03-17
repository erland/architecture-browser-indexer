package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact;
import info.isaksson.erland.architecturebrowser.indexer.extract.model.StructuralExtractionResult;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RelationshipKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ScopeKind;
import info.isaksson.erland.architecturebrowser.indexer.parse.ParseBatchResult;
import info.isaksson.erland.architecturebrowser.indexer.parse.ParseLanguage;
import info.isaksson.erland.architecturebrowser.indexer.parse.ParseStatus;
import info.isaksson.erland.architecturebrowser.indexer.parse.SourceParseRequest;
import info.isaksson.erland.architecturebrowser.indexer.parse.SourceParseResult;
import info.isaksson.erland.architecturebrowser.indexer.parse.SyntaxNode;
import info.isaksson.erland.architecturebrowser.indexer.parse.SyntaxTree;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TypeScriptMemberOwnershipSeamTest extends AbstractTypeScriptExtractionTestSupport {

    @Test
    void classOwnsMethodsAndProperties() {
        String source = """
            export class UserService {
              constructor(private readonly api: ApiClient) {}
              getUser(): User { return this.api.get(); }
              currentUser: User;
            }
            """;
        SyntaxNode classNode = new SyntaxNode("class_declaration", true, 0, source.length(), 0, 0, 3, 1, false, false,
            source.strip(), List.of(
                new SyntaxNode("type_identifier", true, 13, 24, 0, 13, 0, 24, false, false, "UserService", List.of()),
                new SyntaxNode("method_definition", true, 77, 116, 2, 2, 2, 41, false, false,
                    "getUser(): User { return this.api.get(); }", List.of(
                        new SyntaxNode("property_identifier", true, 77, 84, 2, 2, 2, 9, false, false, "getUser", List.of())
                    )),
                new SyntaxNode("public_field_definition", true, 119, 137, 3, 2, 3, 20, false, false,
                    "currentUser: User;", List.of(
                        new SyntaxNode("property_identifier", true, 119, 130, 3, 2, 3, 13, false, false, "currentUser", List.of())
                    ))
            ));

        StructuralExtractionResult result = extract("src/app/user.service.ts", source, program(source, classNode));

        var userService = entity(result, EntityKind.CLASS, "UserService");
        var getUser = entity(result, EntityKind.FUNCTION, "getUser");
        var currentUser = entity(result, EntityKind.FIELD, "currentUser");

        assertNotNull(userService);
        assertEquals("UserService", getUser.metadata().get("ownerQualifiedName"));
        assertEquals("class", getUser.metadata().get("ownerDeclarationKind"));
        assertEquals("UserService", currentUser.metadata().get("ownerQualifiedName"));
        assertEquals("class", currentUser.metadata().get("ownerDeclarationKind"));
        assertEquals("User", currentUser.metadata().get("declaredType"));
        assertEquals(false, currentUser.metadata().get("optional"));
        assertEquals(false, currentUser.metadata().get("readonly"));
        assertEquals("", currentUser.metadata().get("accessibility"));
        assertEquals(List.of(), currentUser.metadata().get("modifiers"));
        assertTrue(result.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.CONTAINS
            && userService.id().equals(rel.fromEntityId())
            && getUser.id().equals(rel.toEntityId())));
        assertTrue(result.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.CONTAINS
            && userService.id().equals(rel.fromEntityId())
            && currentUser.id().equals(rel.toEntityId())));
    }

    @Test
    void interfaceOwnsMethodSignaturesAndPropertySignatures() {
        String source = """
            export interface UserContract {
              getUser(id: string): User;
              currentUser: User;
            }
            """;
        SyntaxNode interfaceNode = new SyntaxNode("interface_declaration", true, 0, source.length(), 0, 0, 3, 1, false, false,
            source.strip(), List.of(
                new SyntaxNode("type_identifier", true, 17, 29, 0, 17, 0, 29, false, false, "UserContract", List.of()),
                new SyntaxNode("method_signature", true, 34, 60, 1, 2, 1, 28, false, false,
                    "getUser(id: string): User;", List.of(
                        new SyntaxNode("property_identifier", true, 34, 41, 1, 2, 1, 9, false, false, "getUser", List.of()),
                        new SyntaxNode("formal_parameters", true, 41, 53, 1, 9, 1, 21, false, false, "(id: string)", List.of())
                    )),
                new SyntaxNode("property_signature", true, 63, 81, 2, 2, 2, 20, false, false,
                    "currentUser: User;", List.of(
                        new SyntaxNode("property_identifier", true, 63, 74, 2, 2, 2, 13, false, false, "currentUser", List.of())
                    ))
            ));

        StructuralExtractionResult result = extract("src/app/user-contract.ts", source, program(source, interfaceNode));

        var userContract = entity(result, EntityKind.INTERFACE, "UserContract");
        var getUser = entity(result, EntityKind.FUNCTION, "getUser");
        var currentUser = entity(result, EntityKind.FIELD, "currentUser");

        assertEquals("UserContract", getUser.metadata().get("ownerQualifiedName"));
        assertEquals("interface", getUser.metadata().get("ownerDeclarationKind"));
        assertEquals("(id: string)", getUser.metadata().get("parameters"));
        assertEquals("UserContract", currentUser.metadata().get("ownerQualifiedName"));
        assertEquals("interface", currentUser.metadata().get("ownerDeclarationKind"));
        assertEquals("User", currentUser.metadata().get("declaredType"));
        assertEquals(false, currentUser.metadata().get("optional"));
        assertEquals(false, currentUser.metadata().get("readonly"));
        assertEquals("", currentUser.metadata().get("accessibility"));
        assertEquals(List.of(), currentUser.metadata().get("modifiers"));
        assertTrue(result.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.CONTAINS
            && userContract.id().equals(rel.fromEntityId())
            && getUser.id().equals(rel.toEntityId())));
        assertTrue(result.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.CONTAINS
            && userContract.id().equals(rel.fromEntityId())
            && currentUser.id().equals(rel.toEntityId())));
    }

}
