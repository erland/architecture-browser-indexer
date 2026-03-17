package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;
import info.isaksson.erland.architecturebrowser.indexer.parse.SyntaxNode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TypeScriptDeclarationDiscoverySupportTest {

    @Test
    void discoversTopLevelDeclarationCandidatesBeforeFactEmission() {
        String source = """
            export type UserId = string;
            export interface UserContract extends BaseContract {}
            export class UserService extends BaseService implements UserContract {
              loadData(): UserContract { return this.profile; }
            }
            export function fetchUsers() { return []; }
            export const loadData = () => 42;
            """;

        SyntaxNode typeAlias = new SyntaxNode("type_alias_declaration", true, 0, 28, 0, 0, 0, 28, false, false,
            "export type UserId = string;", List.of(
                new SyntaxNode("type_identifier", true, 12, 18, 0, 12, 0, 18, false, false, "UserId", List.of())
            ));
        SyntaxNode userContract = new SyntaxNode("interface_declaration", true, 29, 80, 1, 0, 1, 51, false, false,
            "export interface UserContract extends BaseContract {}", List.of(
                new SyntaxNode("type_identifier", true, 46, 58, 1, 17, 1, 29, false, false, "UserContract", List.of())
            ));
        SyntaxNode userService = new SyntaxNode("class_declaration", true, 81, 199, 2, 0, 4, 1, false, false,
            "export class UserService extends BaseService implements UserContract {\n  loadData(): UserContract { return this.profile; }\n}", List.of(
                new SyntaxNode("type_identifier", true, 94, 105, 2, 13, 2, 24, false, false, "UserService", List.of())
            ));
        SyntaxNode fetchUsers = new SyntaxNode("function_declaration", true, 200, 242, 5, 0, 5, 42, false, false,
            "export function fetchUsers() { return []; }", List.of(
                new SyntaxNode("identifier", true, 216, 226, 5, 16, 5, 26, false, false, "fetchUsers", List.of())
            ));
        SyntaxNode loadDataArrow = new SyntaxNode("variable_declarator", true, 256, 274, 6, 13, 6, 31, false, false,
            "loadData = () => 42", List.of(
                new SyntaxNode("identifier", true, 256, 264, 6, 13, 6, 21, false, false, "loadData", List.of()),
                new SyntaxNode("arrow_function", true, 267, 274, 6, 24, 6, 31, false, false, "() => 42", List.of())
            ));

        TypeScriptDeclarationDiscoverySupport.TypeScriptDiscoveredDeclarations discovered =
            TypeScriptDeclarationDiscoverySupport.discover(program(source, typeAlias, userContract, userService, fetchUsers, loadDataArrow));

        assertEquals(List.of("UserContract", "UserId", "UserService"),
            discovered.namedTypeDeclarations().stream()
                .map(TypeScriptDeclarationDiscoverySupport.DiscoveredTypeDeclaration::node)
                .map(SyntaxTreeExtractionSupport::declarationName)
                .sorted().toList());
        assertEquals(1,
            discovered.namedTypeDeclarations().stream()
                .map(TypeScriptDeclarationDiscoverySupport.DiscoveredTypeDeclaration::entityKind)
                .filter(kind -> kind == EntityKind.CLASS)
                .count());
        assertEquals(2,
            discovered.namedTypeDeclarations().stream()
                .map(TypeScriptDeclarationDiscoverySupport.DiscoveredTypeDeclaration::entityKind)
                .filter(kind -> kind == EntityKind.INTERFACE)
                .count());
        assertEquals(1, discovered.classDeclarations().size());
        assertEquals(1, discovered.interfaceDeclarations().size());
        assertEquals(1, discovered.functionDeclarations().size());
        assertEquals(1, discovered.arrowFunctionDeclarators().size());
    }

    private static SyntaxNode program(String source, SyntaxNode... children) {
        int endLine = Math.max(0, source.split("\\R", -1).length - 1);
        int endColumn = source.isEmpty() ? 0 : source.length() - source.lastIndexOf('\n') - 1;
        return new SyntaxNode("program", true, 0, source.length(), 0, 0, endLine, endColumn, false, false, source, List.of(children));
    }
}
