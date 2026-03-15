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

class TypeScriptStructuralExtractorSafetyNetTest {
    @Test
    void createsFileScopeAndModuleEntityForTypeScriptSource() {
        String source = "export class AppComponent {}\n";
        SyntaxNode root = program(source,
            classDeclaration(0, source.length(), 0, "AppComponent", List.of())
        );

        StructuralExtractionResult result = extract("src/app/app.component.ts", source, root);

        var fileScope = result.scopes().stream()
            .filter(scope -> scope.kind() == ScopeKind.FILE && "src/app/app.component.ts".equals(scope.name()))
            .findFirst().orElseThrow();
        var moduleEntity = result.entities().stream()
            .filter(entity -> entity.kind() == EntityKind.MODULE && "src/app/app.component.ts".equals(entity.name()))
            .findFirst().orElseThrow();

        assertEquals("app.component.ts", fileScope.displayName());
        assertTrue(fileScope.parentScopeId().startsWith("scope:directory:"));
        assertEquals("app.component.ts", moduleEntity.displayName());
        assertEquals(fileScope.id(), moduleEntity.scopeId());
        assertEquals("typescript", moduleEntity.metadata().get("language"));
        assertEquals("src/app/app.component.ts", moduleEntity.metadata().get("relativePath"));
    }

    @Test
    void extractsCurrentTopLevelTypeScriptEntitiesImportsArrowFunctionsAndDecorators() {
        String source = """
            import { Injectable } from '@angular/core';
            import { ApiClient } from './api-client';

            @Injectable()
            export class ApiService {}

            export interface ApiResponse {}

            export function bootstrapApplication() {}

            export const loadData = () => ApiClient.get();
            """;

        SyntaxNode importAngular = new SyntaxNode("import_statement", true, 0, 42, 0, 0, 0, 42, false, false,
            "import { Injectable } from '@angular/core';", List.of());
        SyntaxNode importApiClient = new SyntaxNode("import_statement", true, 43, 84, 1, 0, 1, 41, false, false,
            "import { ApiClient } from './api-client';", List.of());
        SyntaxNode apiService = new SyntaxNode("class_declaration", true, 86, 124, 3, 0, 4, 26, false, false,
            "@Injectable()\nexport class ApiService {}", List.of(
                new SyntaxNode("decorator", true, 86, 99, 3, 0, 3, 13, false, false, "@Injectable()", List.of()),
                new SyntaxNode("type_identifier", true, 113, 123, 4, 13, 4, 23, false, false, "ApiService", List.of())
            ));
        SyntaxNode apiResponse = new SyntaxNode("interface_declaration", true, 126, 157, 6, 0, 6, 31, false, false,
            "export interface ApiResponse {}", List.of(
                new SyntaxNode("type_identifier", true, 143, 154, 6, 17, 6, 28, false, false, "ApiResponse", List.of())
            ));
        SyntaxNode bootstrapApplication = new SyntaxNode("function_declaration", true, 159, 200, 8, 0, 8, 41, false, false,
            "export function bootstrapApplication() {}", List.of(
                new SyntaxNode("identifier", true, 175, 195, 8, 16, 8, 36, false, false, "bootstrapApplication", List.of())
            ));
        SyntaxNode loadData = new SyntaxNode("variable_declarator", true, 202, 241, 10, 13, 10, 52, false, false,
            "loadData = () => ApiClient.get()", List.of(
                new SyntaxNode("identifier", true, 202, 210, 10, 13, 10, 21, false, false, "loadData", List.of()),
                new SyntaxNode("arrow_function", true, 213, 241, 10, 24, 10, 52, false, false, "() => ApiClient.get()", List.of())
            ));

        StructuralExtractionResult result = extract("src/app/api.service.ts", source, program(source,
            importAngular,
            importApiClient,
            apiService,
            apiResponse,
            bootstrapApplication,
            loadData
        ));

        var apiServiceEntity = entity(result, EntityKind.CLASS, "ApiService");
        var apiResponseEntity = entity(result, EntityKind.INTERFACE, "ApiResponse");
        var bootstrapEntity = entity(result, EntityKind.FUNCTION, "bootstrapApplication");
        var loadDataEntity = entity(result, EntityKind.FUNCTION, "loadData");
        var moduleEntity = entity(result, EntityKind.MODULE, "src/app/api.service.ts");

        assertEquals(List.of("Injectable"), apiServiceEntity.metadata().get("decorators"));
        assertEquals(List.of(), apiResponseEntity.metadata().get("decorators"));
        assertEquals(List.of(), bootstrapEntity.metadata().get("decorators"));
        assertEquals(List.of(), loadDataEntity.metadata().get("decorators"));

        assertTrue(result.entities().stream().anyMatch(entity -> entity.origin().name().equals("INFERRED")
            && "@angular/core".equals(entity.name())
            && Boolean.TRUE.equals(entity.metadata().get("external"))));
        assertTrue(result.entities().stream().anyMatch(entity -> entity.origin().name().equals("INFERRED")
            && "./api-client".equals(entity.name())
            && Boolean.TRUE.equals(entity.metadata().get("external"))));

        assertTrue(result.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.CONTAINS
            && moduleEntity.id().equals(rel.fromEntityId())
            && apiServiceEntity.id().equals(rel.toEntityId())));
        assertTrue(result.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.CONTAINS
            && moduleEntity.id().equals(rel.fromEntityId())
            && apiResponseEntity.id().equals(rel.toEntityId())));
        assertTrue(result.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.CONTAINS
            && moduleEntity.id().equals(rel.fromEntityId())
            && bootstrapEntity.id().equals(rel.toEntityId())));
        assertTrue(result.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.CONTAINS
            && moduleEntity.id().equals(rel.fromEntityId())
            && loadDataEntity.id().equals(rel.toEntityId())));
        assertTrue(result.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.DEPENDS_ON
            && moduleEntity.id().equals(rel.fromEntityId())
            && "@angular/core".equals(rel.label())));
        assertTrue(result.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.DEPENDS_ON
            && moduleEntity.id().equals(rel.fromEntityId())
            && "./api-client".equals(rel.label())));
    }

    @Test
    void keepsAnonymousDefaultExportAsGapByOnlyCreatingFileModule() {
        String source = "export default class {}\nexport default () => 42;\n";
        SyntaxNode anonymousClass = new SyntaxNode("class_declaration", true, 0, 23, 0, 0, 0, 23, false, false,
            "export default class {}", List.of());
        SyntaxNode anonymousArrow = new SyntaxNode("arrow_function", true, 39, 47, 1, 15, 1, 23, false, false,
            "() => 42", List.of());

        StructuralExtractionResult result = extract("src/app/defaults.ts", source, program(source, anonymousClass, anonymousArrow));

        assertTrue(result.entities().stream().anyMatch(entity -> entity.kind() == EntityKind.MODULE && "src/app/defaults.ts".equals(entity.name())));
        assertFalse(result.entities().stream().anyMatch(entity -> entity.kind() == EntityKind.CLASS));
        assertFalse(result.entities().stream().anyMatch(entity -> entity.kind() == EntityKind.FUNCTION));
    }

    @Test
    void documentsCurrentGapThatTypeMembersAndPropertiesAreNotYetExtracted() {
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
        assertNotNull(userService);
        assertFalse(result.entities().stream().anyMatch(entity -> entity.kind() == EntityKind.FUNCTION && "getUser".equals(entity.name())));
        assertFalse(result.entities().stream().anyMatch(entity -> entity.kind() == EntityKind.FIELD && "currentUser".equals(entity.name())));
    }

    @Test
    void documentsCurrentGapThatHierarchyAndDeclarationTypeDependenciesAreStillMissing() {
        String source = """
            import { BaseService } from './base-service';
            import type { User } from './contracts';

            export class UserService extends BaseService implements User {
              currentUser: User;
            }
            """;
        SyntaxNode importBase = new SyntaxNode("import_statement", true, 0, 43, 0, 0, 0, 43, false, false,
            "import { BaseService } from './base-service';", List.of());
        SyntaxNode importUser = new SyntaxNode("import_statement", true, 44, 84, 1, 0, 1, 40, false, false,
            "import type { User } from './contracts';", List.of());
        SyntaxNode userService = new SyntaxNode("class_declaration", true, 86, source.length(), 3, 0, 5, 1, false, false,
            "export class UserService extends BaseService implements User {\n  currentUser: User;\n}", List.of(
                new SyntaxNode("type_identifier", true, 99, 110, 3, 13, 3, 24, false, false, "UserService", List.of()),
                new SyntaxNode("extends_clause", true, 111, 130, 3, 25, 3, 44, false, false, "extends BaseService", List.of(
                    new SyntaxNode("type_identifier", true, 119, 130, 3, 33, 3, 44, false, false, "BaseService", List.of())
                )),
                new SyntaxNode("implements_clause", true, 131, 146, 3, 45, 3, 60, false, false, "implements User", List.of(
                    new SyntaxNode("type_identifier", true, 142, 146, 3, 56, 3, 60, false, false, "User", List.of())
                )),
                new SyntaxNode("public_field_definition", true, 151, 169, 4, 2, 4, 20, false, false,
                    "currentUser: User;", List.of(
                        new SyntaxNode("property_identifier", true, 151, 162, 4, 2, 4, 13, false, false, "currentUser", List.of()),
                        new SyntaxNode("type_identifier", true, 164, 168, 4, 15, 4, 19, false, false, "User", List.of())
                    ))
            ));

        StructuralExtractionResult result = extract("src/app/user.service.ts", source, program(source, importBase, importUser, userService));

        var moduleEntity = entity(result, EntityKind.MODULE, "src/app/user.service.ts");
        var userServiceEntity = entity(result, EntityKind.CLASS, "UserService");

        assertTrue(result.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.DEPENDS_ON
            && moduleEntity.id().equals(rel.fromEntityId())
            && "./base-service".equals(rel.label())));
        assertTrue(result.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.DEPENDS_ON
            && moduleEntity.id().equals(rel.fromEntityId())
            && "./contracts".equals(rel.label())));

        assertFalse(result.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.EXTENDS
            && userServiceEntity.id().equals(rel.fromEntityId())));
        assertFalse(result.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.IMPLEMENTS
            && userServiceEntity.id().equals(rel.fromEntityId())));
        assertFalse(result.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.DEPENDS_ON
            && userServiceEntity.id().equals(rel.fromEntityId())));
    }

    private static StructuralExtractionResult extract(String relativePath, String source, SyntaxNode root) {
        SourceParseResult parseResult = new SourceParseResult(
            new SourceParseRequest(Path.of(relativePath), relativePath, ParseLanguage.TYPESCRIPT, source),
            ParseStatus.SUCCESS,
            new SyntaxTree(ParseLanguage.TYPESCRIPT, "tree-sitter-jtreesitter", root, false, root.nodeCount()),
            List.of(),
            Map.of("parserBackend", "tree-sitter-jtreesitter")
        );
        return new StructuralExtractionService(StructuralExtractorRegistry.defaultRegistry())
            .extract(new ParseBatchResult(List.of(parseResult), Map.of(ParseLanguage.TYPESCRIPT, 1), Map.of(ParseStatus.SUCCESS, 1)));
    }

    private static SyntaxNode program(String source, SyntaxNode... children) {
        int endLine = Math.max(0, source.split("\\R", -1).length - 1);
        int endColumn = source.isEmpty() ? 0 : source.length() - source.lastIndexOf('\n') - 1;
        return new SyntaxNode("program", true, 0, source.length(), 0, 0, endLine, endColumn, false, false, source, List.of(children));
    }

    private static SyntaxNode classDeclaration(int startIndex, int endIndex, int startLine, String name, List<SyntaxNode> extraChildren) {
        int startColumn = 0;
        int endColumn = Math.max(0, endIndex - startIndex);
        java.util.ArrayList<SyntaxNode> children = new java.util.ArrayList<>();
        children.add(new SyntaxNode("type_identifier", true, startIndex, startIndex + name.length(), startLine, startColumn, startLine, startColumn + name.length(), false, false, name, List.of()));
        children.addAll(extraChildren);
        return new SyntaxNode("class_declaration", true, startIndex, endIndex, startLine, startColumn, startLine, endColumn, false, false,
            "export class " + name + " {}", List.copyOf(children));
    }

    private static ExtractedEntityFact entity(StructuralExtractionResult result, EntityKind kind, String name) {
        return result.entities().stream()
            .filter(entity -> entity.kind() == kind && name.equals(entity.name()))
            .findFirst()
            .orElseThrow();
    }
}
