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
    void exposesStableTypeScriptDeclarationKindMetadata() {
        String source = """
            export class ApiService {}
            export interface ApiContract {}
            export type ApiResult = ApiContract | null;
            export enum Status { OPEN, CLOSED }
            export function bootstrapApplication() {}
            export const loadData = () => 42;
            """;

        SyntaxNode apiService = new SyntaxNode("class_declaration", true, 0, 26, 0, 0, 0, 26, false, false,
            "export class ApiService {}", List.of(
                new SyntaxNode("type_identifier", true, 13, 23, 0, 13, 0, 23, false, false, "ApiService", List.of())
            ));
        SyntaxNode apiContract = new SyntaxNode("interface_declaration", true, 27, 58, 1, 0, 1, 31, false, false,
            "export interface ApiContract {}", List.of(
                new SyntaxNode("type_identifier", true, 44, 55, 1, 17, 1, 28, false, false, "ApiContract", List.of())
            ));
        SyntaxNode apiResult = new SyntaxNode("type_alias_declaration", true, 59, 98, 2, 0, 2, 39, false, false,
            "export type ApiResult = ApiContract | null;", List.of(
                new SyntaxNode("type_identifier", true, 71, 80, 2, 12, 2, 21, false, false, "ApiResult", List.of()),
                new SyntaxNode("type_identifier", true, 83, 94, 2, 24, 2, 35, false, false, "ApiContract", List.of())
            ));
        SyntaxNode status = new SyntaxNode("enum_declaration", true, 99, 135, 3, 0, 3, 36, false, false,
            "export enum Status { OPEN, CLOSED }", List.of(
                new SyntaxNode("identifier", true, 111, 117, 3, 12, 3, 18, false, false, "Status", List.of())
            ));
        SyntaxNode bootstrapApplication = new SyntaxNode("function_declaration", true, 136, 177, 4, 0, 4, 41, false, false,
            "export function bootstrapApplication() {}", List.of(
                new SyntaxNode("identifier", true, 152, 172, 4, 16, 4, 36, false, false, "bootstrapApplication", List.of())
            ));
        SyntaxNode loadData = new SyntaxNode("variable_declarator", true, 191, 210, 5, 13, 5, 32, false, false,
            "loadData = () => 42", List.of(
                new SyntaxNode("identifier", true, 191, 199, 5, 13, 5, 21, false, false, "loadData", List.of()),
                new SyntaxNode("arrow_function", true, 202, 210, 5, 24, 5, 32, false, false, "() => 42", List.of())
            ));

        StructuralExtractionResult result = extract("src/app/declarations.ts", source, program(source,
            apiService,
            apiContract,
            apiResult,
            status,
            bootstrapApplication,
            loadData
        ));

        assertEquals("class", entity(result, EntityKind.CLASS, "ApiService").metadata().get("declarationKind"));
        assertEquals("interface", entity(result, EntityKind.INTERFACE, "ApiContract").metadata().get("declarationKind"));

        var apiResultEntity = entity(result, EntityKind.INTERFACE, "ApiResult");
        assertEquals("typeAlias", apiResultEntity.metadata().get("declarationKind"));
        assertEquals("ApiResult", apiResultEntity.metadata().get("qualifiedName"));

        var statusEntity = entity(result, EntityKind.CLASS, "Status");
        assertEquals("enum", statusEntity.metadata().get("declarationKind"));
        assertEquals("Status", statusEntity.metadata().get("qualifiedName"));

        assertEquals("function", entity(result, EntityKind.FUNCTION, "bootstrapApplication").metadata().get("declarationKind"));
        assertEquals("function", entity(result, EntityKind.FUNCTION, "loadData").metadata().get("declarationKind"));
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


    @Test
    void extractsFirstClassTypeScriptPropertyMetadata() {
        String source = """
            export class UserService {
              @Input() readonly profile?: UserProfile;
            }
            """;
        SyntaxNode property = new SyntaxNode("public_field_definition", true, 29, 68, 1, 2, 1, 41, false, false,
            "@Input() readonly profile?: UserProfile;", List.of(
                new SyntaxNode("decorator", true, 29, 37, 1, 2, 1, 10, false, false, "@Input()", List.of()),
                new SyntaxNode("property_identifier", true, 47, 54, 1, 20, 1, 27, false, false, "profile", List.of()),
                new SyntaxNode("type_identifier", true, 57, 68, 1, 30, 1, 41, false, false, "UserProfile", List.of())
            ));
        SyntaxNode classNode = new SyntaxNode("class_declaration", true, 0, source.length(), 0, 0, 2, 1, false, false,
            source.strip(), List.of(
                new SyntaxNode("type_identifier", true, 13, 24, 0, 13, 0, 24, false, false, "UserService", List.of()),
                property
            ));

        StructuralExtractionResult result = extract("src/app/user.service.ts", source, program(source, classNode));

        var profile = entity(result, EntityKind.FIELD, "profile");
        assertEquals("UserProfile", profile.metadata().get("declaredType"));
        assertEquals(true, profile.metadata().get("optional"));
        assertEquals(true, profile.metadata().get("readonly"));
        assertEquals("", profile.metadata().get("accessibility"));
        assertEquals(List.of("readonly"), profile.metadata().get("modifiers"));
        assertEquals(List.of("Input"), profile.metadata().get("decorators"));
        assertEquals("UserService", profile.metadata().get("ownerQualifiedName"));
    }

    @Test
    void extractsTypeScriptPropertyAccessibilityAndModifiers() {
        String source = """
            export class UserService {
              @Inject() private readonly apiClient: ApiClient;
            }
            """;
        SyntaxNode property = new SyntaxNode("public_field_definition", true, 29, 78, 1, 2, 1, 51, false, false,
            "@Inject() private readonly apiClient: ApiClient;", List.of(
                new SyntaxNode("decorator", true, 29, 38, 1, 2, 1, 11, false, false, "@Inject()", List.of()),
                new SyntaxNode("property_identifier", true, 56, 65, 1, 29, 1, 38, false, false, "apiClient", List.of()),
                new SyntaxNode("type_identifier", true, 67, 76, 1, 40, 1, 49, false, false, "ApiClient", List.of())
            ));
        SyntaxNode classNode = new SyntaxNode("class_declaration", true, 0, source.length(), 0, 0, 2, 1, false, false,
            source.strip(), List.of(
                new SyntaxNode("type_identifier", true, 13, 24, 0, 13, 0, 24, false, false, "UserService", List.of()),
                property
            ));

        StructuralExtractionResult result = extract("src/app/user.service.ts", source, program(source, classNode));

        var apiClient = entity(result, EntityKind.FIELD, "apiClient");
        assertEquals("ApiClient", apiClient.metadata().get("declaredType"));
        assertEquals(false, apiClient.metadata().get("optional"));
        assertEquals(true, apiClient.metadata().get("readonly"));
        assertEquals("private", apiClient.metadata().get("accessibility"));
        assertEquals(List.of("private", "readonly"), apiClient.metadata().get("modifiers"));
        assertEquals(List.of("Inject"), apiClient.metadata().get("decorators"));
    }

    @Test
    void extractsTypeScriptHierarchyRelationshipsAndHierarchyDependencies() {
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

        assertTrue(result.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.EXTENDS
            && userServiceEntity.id().equals(rel.fromEntityId())
            && "BaseService".equals(rel.label())
            && "extends".equals(rel.metadata().get("dependencySource"))
            && "hierarchy".equals(rel.metadata().get("dependencyCategory"))));
        assertTrue(result.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.IMPLEMENTS
            && userServiceEntity.id().equals(rel.fromEntityId())
            && "User".equals(rel.label())
            && "implements".equals(rel.metadata().get("dependencySource"))
            && "hierarchy".equals(rel.metadata().get("dependencyCategory"))));
        assertTrue(result.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.DEPENDS_ON
            && userServiceEntity.id().equals(rel.fromEntityId())
            && "BaseService".equals(rel.label())
            && "extends".equals(rel.metadata().get("dependencySource"))
            && "hierarchy".equals(rel.metadata().get("dependencyCategory"))));
        assertTrue(result.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.DEPENDS_ON
            && userServiceEntity.id().equals(rel.fromEntityId())
            && "User".equals(rel.label())
            && "implements".equals(rel.metadata().get("dependencySource"))
            && "hierarchy".equals(rel.metadata().get("dependencyCategory"))));
    }

    @Test
    void addsDeclarationBasedDependenciesForPropertiesAndMethodSignatures() {
        String source = """
            import { ApiClient } from './api-client';
            import type { User } from './contracts';

            export class UserService {
              currentUser: User;
              constructor(api: ApiClient) {}
              getUser(client: ApiClient): User { return this.currentUser; }
            }
            """;
        SyntaxNode importApi = new SyntaxNode("import_statement", true, 0, 40, 0, 0, 0, 40, false, false,
            "import { ApiClient } from './api-client';", List.of());
        SyntaxNode importUser = new SyntaxNode("import_statement", true, 41, 81, 1, 0, 1, 40, false, false,
            "import type { User } from './contracts';", List.of());
        SyntaxNode userService = new SyntaxNode("class_declaration", true, 83, source.length(), 3, 0, 7, 1, false, false,
            """
            export class UserService {
              currentUser: User;
              constructor(api: ApiClient) {}
              getUser(client: ApiClient): User { return this.currentUser; }
            }
            """.strip(), List.of(
                new SyntaxNode("type_identifier", true, 96, 107, 3, 13, 3, 24, false, false, "UserService", List.of()),
                new SyntaxNode("public_field_definition", true, 112, 130, 4, 2, 4, 20, false, false,
                    "currentUser: User;", List.of(
                        new SyntaxNode("property_identifier", true, 112, 123, 4, 2, 4, 13, false, false, "currentUser", List.of()),
                        new SyntaxNode("type_identifier", true, 125, 129, 4, 15, 4, 19, false, false, "User", List.of())
                    )),
                new SyntaxNode("method_definition", true, 133, 163, 5, 2, 5, 32, false, false,
                    "constructor(api: ApiClient) {}", List.of(
                        new SyntaxNode("property_identifier", true, 133, 144, 5, 2, 5, 13, false, false, "constructor", List.of()),
                        new SyntaxNode("formal_parameters", true, 144, 161, 5, 13, 5, 30, false, false, "(api: ApiClient)", List.of())
                    )),
                new SyntaxNode("method_definition", true, 166, 223, 6, 2, 6, 59, false, false,
                    "getUser(client: ApiClient): User { return this.currentUser; }", List.of(
                        new SyntaxNode("property_identifier", true, 166, 173, 6, 2, 6, 9, false, false, "getUser", List.of()),
                        new SyntaxNode("formal_parameters", true, 173, 192, 6, 9, 6, 28, false, false, "(client: ApiClient)", List.of()),
                        new SyntaxNode("type_identifier", true, 195, 199, 6, 31, 6, 35, false, false, "User", List.of())
                    ))
            ));

        StructuralExtractionResult result = extract("src/app/user.service.ts", source, program(source, importApi, importUser, userService));

        var userServiceEntity = entity(result, EntityKind.CLASS, "UserService");

        assertTrue(result.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.DEPENDS_ON
            && userServiceEntity.id().equals(rel.fromEntityId())
            && "User".equals(rel.label())
            && "field".equals(rel.metadata().get("dependencySource"))
            && "composition".equals(rel.metadata().get("dependencyCategory"))));
        assertTrue(result.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.DEPENDS_ON
            && userServiceEntity.id().equals(rel.fromEntityId())
            && "ApiClient".equals(rel.label())
            && "constructorParameter".equals(rel.metadata().get("dependencySource"))
            && "api".equals(rel.metadata().get("dependencyCategory"))));
        assertTrue(result.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.DEPENDS_ON
            && userServiceEntity.id().equals(rel.fromEntityId())
            && "ApiClient".equals(rel.label())
            && "parameterType".equals(rel.metadata().get("dependencySource"))
            && "api".equals(rel.metadata().get("dependencyCategory"))));
        assertTrue(result.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.DEPENDS_ON
            && userServiceEntity.id().equals(rel.fromEntityId())
            && "User".equals(rel.label())
            && "returnType".equals(rel.metadata().get("dependencySource"))
            && "api".equals(rel.metadata().get("dependencyCategory"))));
    }

    @Test
    void resolvesLocalDeclaredTypesForTypeScriptHierarchyRelationships() {
        String source = """
            export interface BaseContract {}
            export interface UserContract extends BaseContract {}
            export class BaseService {}
            export class UserService extends BaseService implements UserContract {}
            """;
        SyntaxNode baseContract = new SyntaxNode("interface_declaration", true, 0, 32, 0, 0, 0, 32, false, false,
            "export interface BaseContract {}", List.of(
                new SyntaxNode("type_identifier", true, 17, 29, 0, 17, 0, 29, false, false, "BaseContract", List.of())
            ));
        SyntaxNode userContract = new SyntaxNode("interface_declaration", true, 33, 87, 1, 0, 1, 54, false, false,
            "export interface UserContract extends BaseContract {}", List.of(
                new SyntaxNode("type_identifier", true, 50, 62, 1, 17, 1, 29, false, false, "UserContract", List.of()),
                new SyntaxNode("extends_clause", true, 63, 83, 1, 30, 1, 50, false, false, "extends BaseContract", List.of(
                    new SyntaxNode("type_identifier", true, 71, 83, 1, 38, 1, 50, false, false, "BaseContract", List.of())
                ))
            ));
        SyntaxNode baseService = new SyntaxNode("class_declaration", true, 88, 115, 2, 0, 2, 27, false, false,
            "export class BaseService {}", List.of(
                new SyntaxNode("type_identifier", true, 101, 112, 2, 13, 2, 24, false, false, "BaseService", List.of())
            ));
        SyntaxNode userService = new SyntaxNode("class_declaration", true, 116, source.length(), 3, 0, 3, 68, false, false,
            "export class UserService extends BaseService implements UserContract {}", List.of(
                new SyntaxNode("type_identifier", true, 129, 140, 3, 13, 3, 24, false, false, "UserService", List.of()),
                new SyntaxNode("extends_clause", true, 141, 160, 3, 25, 3, 44, false, false, "extends BaseService", List.of(
                    new SyntaxNode("type_identifier", true, 149, 160, 3, 33, 3, 44, false, false, "BaseService", List.of())
                )),
                new SyntaxNode("implements_clause", true, 161, 184, 3, 45, 3, 68, false, false, "implements UserContract", List.of(
                    new SyntaxNode("type_identifier", true, 172, 184, 3, 56, 3, 68, false, false, "UserContract", List.of())
                ))
            ));

        StructuralExtractionResult result = extract("src/app/types.ts", source, program(source, baseContract, userContract, baseService, userService));

        var userContractEntity = entity(result, EntityKind.INTERFACE, "UserContract");
        var baseContractEntity = entity(result, EntityKind.INTERFACE, "BaseContract");
        var userServiceEntity = entity(result, EntityKind.CLASS, "UserService");
        var baseServiceEntity = entity(result, EntityKind.CLASS, "BaseService");

        assertTrue(result.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.EXTENDS
            && userContractEntity.id().equals(rel.fromEntityId())
            && baseContractEntity.id().equals(rel.toEntityId())
            && "BaseContract".equals(rel.label())));
        assertTrue(result.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.EXTENDS
            && userServiceEntity.id().equals(rel.fromEntityId())
            && baseServiceEntity.id().equals(rel.toEntityId())
            && "BaseService".equals(rel.label())));
        assertTrue(result.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.IMPLEMENTS
            && userServiceEntity.id().equals(rel.fromEntityId())
            && userContractEntity.id().equals(rel.toEntityId())
            && "UserContract".equals(rel.label())));
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
