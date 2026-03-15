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
        assertTrue(result.entities().stream().anyMatch(entity -> "./api-client".equals(entity.name())
            && Boolean.FALSE.equals(entity.metadata().get("external"))
            && "inferred-internal-module".equals(entity.metadata().get("targetClassification"))));

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
    void classifiesTypeScriptImportsAndTargetBoundaries() {
        String source = """
            import { ApiClient } from './api-client';
            import type { User } from './contracts';
            import '@angular/core';
            import { map } from 'rxjs';

            export class UserService {}
            """;

        SyntaxNode importApiClient = new SyntaxNode("import_statement", true, 0, 40, 0, 0, 0, 40, false, false,
            "import { ApiClient } from './api-client';", List.of());
        SyntaxNode importUser = new SyntaxNode("import_statement", true, 41, 81, 1, 0, 1, 40, false, false,
            "import type { User } from './contracts';", List.of());
        SyntaxNode importAngularSideEffect = new SyntaxNode("import_statement", true, 82, 105, 2, 0, 2, 23, false, false,
            "import '@angular/core';", List.of());
        SyntaxNode importAngularPackage = new SyntaxNode("import_statement", true, 106, 146, 3, 0, 3, 40, false, false,
            "import { map } from 'rxjs';", List.of());
        SyntaxNode userService = new SyntaxNode("class_declaration", true, 148, source.length(), 5, 0, 5, 27, false, false,
            "export class UserService {}", List.of(
                new SyntaxNode("type_identifier", true, 161, 172, 5, 13, 5, 24, false, false, "UserService", List.of())
            ));

        StructuralExtractionResult result = extract(
            "src/app/user.service.ts",
            source,
            program(source, importApiClient, importUser, importAngularSideEffect, importAngularPackage, userService)
        );

        var relativeTarget = entity(result, EntityKind.MODULE, "./api-client");
        var typeOnlyTarget = entity(result, EntityKind.MODULE, "./contracts");
        var angularTarget = entity(result, EntityKind.MODULE, "@angular/core");
        var rxjsTarget = entity(result, EntityKind.MODULE, "rxjs");
        var moduleEntity = entity(result, EntityKind.MODULE, "src/app/user.service.ts");

        assertEquals(false, relativeTarget.metadata().get("external"));
        assertEquals("inferred-internal-module", relativeTarget.metadata().get("targetClassification"));
        assertEquals("relative", relativeTarget.metadata().get("importKind"));

        assertEquals(false, typeOnlyTarget.metadata().get("external"));
        assertEquals("typeOnly", typeOnlyTarget.metadata().get("importKind"));
        assertEquals("inferred-internal-module", typeOnlyTarget.metadata().get("targetClassification"));

        assertEquals(true, angularTarget.metadata().get("external"));
        assertEquals("sideEffect", angularTarget.metadata().get("importKind"));
        assertEquals("external-package-target", angularTarget.metadata().get("targetClassification"));

        assertEquals(true, rxjsTarget.metadata().get("external"));
        assertEquals("package", rxjsTarget.metadata().get("importKind"));
        assertEquals("external-package-target", rxjsTarget.metadata().get("targetClassification"));

        assertTrue(result.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.DEPENDS_ON
            && moduleEntity.id().equals(rel.fromEntityId())
            && "./api-client".equals(rel.label())
            && "relative".equals(rel.metadata().get("importKind"))
            && "internal".equals(rel.metadata().get("importTargetBoundary"))));
        assertTrue(result.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.DEPENDS_ON
            && moduleEntity.id().equals(rel.fromEntityId())
            && "./contracts".equals(rel.label())
            && "typeOnly".equals(rel.metadata().get("importKind"))
            && "internal".equals(rel.metadata().get("importTargetBoundary"))));
        assertTrue(result.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.DEPENDS_ON
            && moduleEntity.id().equals(rel.fromEntityId())
            && "@angular/core".equals(rel.label())
            && "sideEffect".equals(rel.metadata().get("importKind"))
            && "external".equals(rel.metadata().get("importTargetBoundary"))));
        assertTrue(result.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.DEPENDS_ON
            && moduleEntity.id().equals(rel.fromEntityId())
            && "rxjs".equals(rel.label())
            && "package".equals(rel.metadata().get("importKind"))
            && "external".equals(rel.metadata().get("importTargetBoundary"))));
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
    void extractsAngularComponentDirectiveAndPipeDecoratorPayloadMetadata() {
        String source = """
            @Component({
              selector: 'app-order-list',
              templateUrl: './order-list.component.html',
              styleUrls: ['./order-list.component.css', './shared.css'],
              standalone: true,
              imports: [CommonModule, RouterModule],
              providers: [OrderFacade],
              template: `<section>Orders</section>`
            })
            export class OrderListComponent {}

            @Directive({ selector: '[appFocus]', standalone: true, providers: [FocusService] })
            export class FocusDirective {}

            @Pipe({ name: 'money', standalone: false })
            export class MoneyPipe {}
            """;

        SyntaxNode component = new SyntaxNode("class_declaration", true, 0, 329, 0, 0, 8, 35, false, false,
            """
            @Component({
              selector: 'app-order-list',
              templateUrl: './order-list.component.html',
              styleUrls: ['./order-list.component.css', './shared.css'],
              standalone: true,
              imports: [CommonModule, RouterModule],
              providers: [OrderFacade],
              template: `<section>Orders</section>`
            })
            export class OrderListComponent {}
            """.strip(), List.of(
                new SyntaxNode("decorator", true, 0, 280, 0, 0, 7, 2, false, false,
                    """
                    @Component({
                      selector: 'app-order-list',
                      templateUrl: './order-list.component.html',
                      styleUrls: ['./order-list.component.css', './shared.css'],
                      standalone: true,
                      imports: [CommonModule, RouterModule],
                      providers: [OrderFacade],
                      template: `<section>Orders</section>`
                    })
                    """.strip(), List.of()),
                new SyntaxNode("type_identifier", true, 294, 312, 8, 13, 8, 31, false, false, "OrderListComponent", List.of())
            ));
        SyntaxNode directive = new SyntaxNode("class_declaration", true, 331, 455, 10, 0, 11, 31, false, false,
            """
            @Directive({ selector: '[appFocus]', standalone: true, providers: [FocusService] })
            export class FocusDirective {}
            """.strip(), List.of(
                new SyntaxNode("decorator", true, 331, 418, 10, 0, 10, 87, false, false,
                    "@Directive({ selector: '[appFocus]', standalone: true, providers: [FocusService] })", List.of()),
                new SyntaxNode("type_identifier", true, 432, 446, 11, 13, 11, 27, false, false, "FocusDirective", List.of())
            ));
        SyntaxNode pipe = new SyntaxNode("class_declaration", true, 457, source.length(), 13, 0, 14, 24, false, false,
            """
            @Pipe({ name: 'money', standalone: false })
            export class MoneyPipe {}
            """.strip(), List.of(
                new SyntaxNode("decorator", true, 457, 503, 13, 0, 13, 46, false, false,
                    "@Pipe({ name: 'money', standalone: false })", List.of()),
                new SyntaxNode("type_identifier", true, 517, 526, 14, 13, 14, 22, false, false, "MoneyPipe", List.of())
            ));

        StructuralExtractionResult result = extract("src/app/angular-metadata.ts", source, program(source, component, directive, pipe));

        var componentEntity = entity(result, EntityKind.CLASS, "OrderListComponent");
        assertEquals("angular", componentEntity.metadata().get("framework"));
        assertEquals("Component", componentEntity.metadata().get("angularDecorator"));
        assertEquals("component", componentEntity.metadata().get("angularKind"));
        assertEquals("app-order-list", componentEntity.metadata().get("angularSelector"));
        assertEquals("./order-list.component.html", componentEntity.metadata().get("angularTemplateUrl"));
        assertEquals(true, componentEntity.metadata().get("angularHasInlineTemplate"));
        assertEquals(true, componentEntity.metadata().get("angularStandalone"));
        assertEquals(List.of("./order-list.component.css", "./shared.css"), componentEntity.metadata().get("angularStyleUrls"));
        assertEquals(List.of("CommonModule", "RouterModule"), componentEntity.metadata().get("angularImports"));
        assertEquals(List.of("OrderFacade"), componentEntity.metadata().get("angularProviders"));

        var directiveEntity = entity(result, EntityKind.CLASS, "FocusDirective");
        assertEquals("Directive", directiveEntity.metadata().get("angularDecorator"));
        assertEquals("directive", directiveEntity.metadata().get("angularKind"));
        assertEquals("[appFocus]", directiveEntity.metadata().get("angularSelector"));
        assertEquals(true, directiveEntity.metadata().get("angularStandalone"));
        assertEquals(List.of("FocusService"), directiveEntity.metadata().get("angularProviders"));

        var pipeEntity = entity(result, EntityKind.CLASS, "MoneyPipe");
        assertEquals("Pipe", pipeEntity.metadata().get("angularDecorator"));
        assertEquals("pipe", pipeEntity.metadata().get("angularKind"));
        assertEquals("money", pipeEntity.metadata().get("angularPipeName"));
        assertEquals(false, pipeEntity.metadata().get("angularStandalone"));
    }

    @Test
    void extractsAngularNgModuleAndInjectableDecoratorPayloadMetadata() {
        String source = """
            @NgModule({
              imports: [CommonModule, RouterModule.forChild(routes)],
              declarations: [OrderListComponent, FocusDirective],
              exports: [OrderListComponent],
              providers: [OrderFacade, provideHttpClient()],
              bootstrap: [OrderListComponent]
            })
            export class OrdersModule {}

            @Injectable({ providedIn: 'root' })
            export class OrderService {}
            """;

        SyntaxNode ordersModule = new SyntaxNode("class_declaration", true, 0, 297, 0, 0, 6, 29, false, false,
            """
            @NgModule({
              imports: [CommonModule, RouterModule.forChild(routes)],
              declarations: [OrderListComponent, FocusDirective],
              exports: [OrderListComponent],
              providers: [OrderFacade, provideHttpClient()],
              bootstrap: [OrderListComponent]
            })
            export class OrdersModule {}
            """.strip(), List.of(
                new SyntaxNode("decorator", true, 0, 267, 0, 0, 5, 2, false, false,
                    """
                    @NgModule({
                      imports: [CommonModule, RouterModule.forChild(routes)],
                      declarations: [OrderListComponent, FocusDirective],
                      exports: [OrderListComponent],
                      providers: [OrderFacade, provideHttpClient()],
                      bootstrap: [OrderListComponent]
                    })
                    """.strip(), List.of()),
                new SyntaxNode("type_identifier", true, 281, 293, 6, 13, 6, 25, false, false, "OrdersModule", List.of())
            ));
        SyntaxNode orderService = new SyntaxNode("class_declaration", true, 299, source.length(), 8, 0, 9, 29, false, false,
            """
            @Injectable({ providedIn: 'root' })
            export class OrderService {}
            """.strip(), List.of(
                new SyntaxNode("decorator", true, 299, 334, 8, 0, 8, 35, false, false,
                    "@Injectable({ providedIn: 'root' })", List.of()),
                new SyntaxNode("type_identifier", true, 348, 360, 9, 13, 9, 25, false, false, "OrderService", List.of())
            ));

        StructuralExtractionResult result = extract("src/app/orders.module.ts", source, program(source, ordersModule, orderService));

        var ordersModuleEntity = entity(result, EntityKind.CLASS, "OrdersModule");
        assertEquals("angular", ordersModuleEntity.metadata().get("framework"));
        assertEquals("NgModule", ordersModuleEntity.metadata().get("angularDecorator"));
        assertEquals("module", ordersModuleEntity.metadata().get("angularKind"));
        assertEquals(List.of("CommonModule", "RouterModule.forChild(routes)"), ordersModuleEntity.metadata().get("angularImports"));
        assertEquals(List.of("OrderListComponent", "FocusDirective"), ordersModuleEntity.metadata().get("angularDeclarations"));
        assertEquals(List.of("OrderListComponent"), ordersModuleEntity.metadata().get("angularExports"));
        assertEquals(List.of("OrderFacade", "provideHttpClient()"), ordersModuleEntity.metadata().get("angularProviders"));
        assertEquals(List.of("OrderListComponent"), ordersModuleEntity.metadata().get("angularBootstrap"));

        var orderServiceEntity = entity(result, EntityKind.CLASS, "OrderService");
        assertEquals("Injectable", orderServiceEntity.metadata().get("angularDecorator"));
        assertEquals("injectable", orderServiceEntity.metadata().get("angularKind"));
        assertEquals("root", orderServiceEntity.metadata().get("angularProvidedIn"));
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

    @Test
    void extractsAngularModuleAndStandaloneFrameworkRelationshipsFromDecoratorPayloads() {
        String source = """
            @Component({ standalone: true, imports: [SharedCardComponent, CommonModule], providers: [OrderFacade] })
            export class OrdersComponent {}

            @Directive({ standalone: true })
            export class SharedCardComponent {}

            @NgModule({
              declarations: [OrdersComponent],
              imports: [SharedModule, OrdersComponent],
              exports: [OrdersComponent],
              bootstrap: [OrdersComponent],
              providers: [OrdersFacade, provideHttpClient()]
            })
            export class OrdersModule {}

            export class SharedModule {}
            export class OrderFacade {}
            """;

        SyntaxNode ordersComponent = new SyntaxNode("class_declaration", true, 0, 140, 0, 0, 1, 31, false, false,
            """
            @Component({ standalone: true, imports: [SharedCardComponent, CommonModule], providers: [OrderFacade] })
            export class OrdersComponent {}
            """.strip(), List.of(
                new SyntaxNode("decorator", true, 0, 109, 0, 0, 0, 109, false, false,
                    "@Component({ standalone: true, imports: [SharedCardComponent, CommonModule], providers: [OrderFacade] })", List.of()),
                new SyntaxNode("type_identifier", true, 123, 138, 1, 13, 1, 28, false, false, "OrdersComponent", List.of())
            ));
        SyntaxNode sharedCardComponent = new SyntaxNode("class_declaration", true, 142, 220, 3, 0, 4, 35, false, false,
            """
            @Directive({ standalone: true })
            export class SharedCardComponent {}
            """.strip(), List.of(
                new SyntaxNode("decorator", true, 142, 173, 3, 0, 3, 31, false, false,
                    "@Directive({ standalone: true })", List.of()),
                new SyntaxNode("type_identifier", true, 187, 206, 4, 13, 4, 32, false, false, "SharedCardComponent", List.of())
            ));
        SyntaxNode ordersModule = new SyntaxNode("class_declaration", true, 222, 478, 6, 0, 13, 28, false, false,
            """
            @NgModule({
              declarations: [OrdersComponent],
              imports: [SharedModule, OrdersComponent],
              exports: [OrdersComponent],
              bootstrap: [OrdersComponent],
              providers: [OrderFacade, provideHttpClient()]
            })
            export class OrdersModule {}
            """.strip(), List.of(
                new SyntaxNode("decorator", true, 222, 448, 6, 0, 12, 2, false, false,
                    """
                    @NgModule({
                      declarations: [OrdersComponent],
                      imports: [SharedModule, OrdersComponent],
                      exports: [OrdersComponent],
                      bootstrap: [OrdersComponent],
                      providers: [OrderFacade, provideHttpClient()]
                    })
                    """.strip(), List.of()),
                new SyntaxNode("type_identifier", true, 462, 474, 13, 13, 13, 25, false, false, "OrdersModule", List.of())
            ));
        SyntaxNode sharedModule = new SyntaxNode("class_declaration", true, 480, 507, 15, 0, 15, 27, false, false,
            "export class SharedModule {}", List.of(
                new SyntaxNode("type_identifier", true, 493, 505, 15, 13, 15, 25, false, false, "SharedModule", List.of())
            ));
        SyntaxNode orderFacade = new SyntaxNode("class_declaration", true, 509, source.length(), 16, 0, 16, 27, false, false,
            "export class OrderFacade {}", List.of(
                new SyntaxNode("type_identifier", true, 522, 533, 16, 13, 16, 24, false, false, "OrderFacade", List.of())
            ));

        StructuralExtractionResult result = extract("src/app/orders.angular.ts", source,
            program(source, ordersComponent, sharedCardComponent, ordersModule, sharedModule, orderFacade));

        var ordersModuleEntity = entity(result, EntityKind.CLASS, "OrdersModule");
        var ordersComponentEntity = entity(result, EntityKind.CLASS, "OrdersComponent");
        var sharedCardEntity = entity(result, EntityKind.CLASS, "SharedCardComponent");
        var sharedModuleEntity = entity(result, EntityKind.CLASS, "SharedModule");
        var orderFacadeEntity = entity(result, EntityKind.CLASS, "OrderFacade");
        var provideHttpClientEntity = entity(result, EntityKind.FUNCTION, "provideHttpClient");

        assertAngularFrameworkRelationship(result, ordersModuleEntity.id(), ordersComponentEntity.id(), "OrdersComponent", "declares");
        assertAngularFrameworkRelationship(result, ordersModuleEntity.id(), sharedModuleEntity.id(), "SharedModule", "imports");
        assertAngularFrameworkRelationship(result, ordersModuleEntity.id(), ordersComponentEntity.id(), "OrdersComponent", "exports");
        assertAngularFrameworkRelationship(result, ordersModuleEntity.id(), ordersComponentEntity.id(), "OrdersComponent", "bootstraps");
        assertAngularFrameworkRelationship(result, ordersModuleEntity.id(), orderFacadeEntity.id(), "OrderFacade", "provides");
        assertAngularFrameworkRelationship(result, ordersModuleEntity.id(), provideHttpClientEntity.id(), "provideHttpClient()", "provides");
        assertAngularFrameworkRelationship(result, ordersComponentEntity.id(), sharedCardEntity.id(), "SharedCardComponent", "imports");
        assertAngularFrameworkRelationship(result, ordersComponentEntity.id(), orderFacadeEntity.id(), "OrderFacade", "provides");
    }

    @Test
    void extractsAngularInjectableProvidedByApplicationScopeRelationship() {
        String source = """
            @Injectable({ providedIn: 'root' })
            export class OrdersService {}
            """;
        SyntaxNode service = new SyntaxNode("class_declaration", true, 0, source.length(), 0, 0, 1, 29, false, false,
            """
            @Injectable({ providedIn: 'root' })
            export class OrdersService {}
            """.strip(), List.of(
                new SyntaxNode("decorator", true, 0, 35, 0, 0, 0, 35, false, false,
                    "@Injectable({ providedIn: 'root' })", List.of()),
                new SyntaxNode("type_identifier", true, 49, 62, 1, 13, 1, 26, false, false, "OrdersService", List.of())
            ));

        StructuralExtractionResult result = extract("src/app/orders.service.ts", source, program(source, service));

        var ordersServiceEntity = entity(result, EntityKind.CLASS, "OrdersService");
        var applicationScopeEntity = entity(result, EntityKind.MODULE, "application:root");
        assertAngularFrameworkRelationship(result, ordersServiceEntity.id(), applicationScopeEntity.id(), "root", "providedBy");
    }

    @Test
    void extractsReactJsxCompositionRelationshipsFromCommonTsxPatterns() {
        String source = """
            export function OrdersPage() {
              return <PageLayout><OrdersTable /><OrderSummary /></PageLayout>;
            }

            export const PageLayout = () => <section><Toolbar /></section>;

            export function OrdersTable() { return <table />; }
            export class OrderSummary {}
            export function Toolbar() { return <header />; }
            """;

        SyntaxNode ordersPage = new SyntaxNode("function_declaration", true, 0, 101, 0, 0, 2, 1, false, false,
            """
            export function OrdersPage() {
              return <PageLayout><OrdersTable /><OrderSummary /></PageLayout>;
            }
            """.strip(), List.of(
                new SyntaxNode("identifier", true, 16, 26, 0, 16, 0, 26, false, false, "OrdersPage", List.of())
            ));
        SyntaxNode pageLayout = new SyntaxNode("variable_declarator", true, 103, 166, 4, 13, 4, 76, false, false,
            "PageLayout = () => <section><Toolbar /></section>", List.of(
                new SyntaxNode("identifier", true, 103, 113, 4, 13, 4, 23, false, false, "PageLayout", List.of()),
                new SyntaxNode("arrow_function", true, 116, 166, 4, 26, 4, 76, false, false,
                    "() => <section><Toolbar /></section>", List.of())
            ));
        SyntaxNode ordersTable = new SyntaxNode("function_declaration", true, 168, 216, 6, 0, 6, 48, false, false,
            "export function OrdersTable() { return <table />; }", List.of(
                new SyntaxNode("identifier", true, 184, 195, 6, 16, 6, 27, false, false, "OrdersTable", List.of())
            ));
        SyntaxNode orderSummary = new SyntaxNode("class_declaration", true, 217, 246, 7, 0, 7, 29, false, false,
            "export class OrderSummary {}", List.of(
                new SyntaxNode("type_identifier", true, 230, 242, 7, 13, 7, 25, false, false, "OrderSummary", List.of())
            ));
        SyntaxNode toolbar = new SyntaxNode("function_declaration", true, 247, source.length(), 8, 0, 8, 45, false, false,
            "export function Toolbar() { return <header />; }", List.of(
                new SyntaxNode("identifier", true, 263, 270, 8, 16, 8, 23, false, false, "Toolbar", List.of())
            ));

        StructuralExtractionResult result = extract("src/app/orders/OrdersPage.tsx", source,
            program(source, ordersPage, pageLayout, ordersTable, orderSummary, toolbar));

        var ordersPageEntity = entity(result, EntityKind.FUNCTION, "OrdersPage");
        var pageLayoutEntity = entity(result, EntityKind.FUNCTION, "PageLayout");
        var ordersTableEntity = entity(result, EntityKind.FUNCTION, "OrdersTable");
        var orderSummaryEntity = entity(result, EntityKind.CLASS, "OrderSummary");
        var toolbarEntity = entity(result, EntityKind.FUNCTION, "Toolbar");

        assertReactFrameworkRelationship(result, ordersPageEntity.id(), pageLayoutEntity.id(), "PageLayout", true);
        assertReactFrameworkRelationship(result, ordersPageEntity.id(), ordersTableEntity.id(), "OrdersTable", true);
        assertReactFrameworkRelationship(result, ordersPageEntity.id(), orderSummaryEntity.id(), "OrderSummary", true);
        assertReactFrameworkRelationship(result, pageLayoutEntity.id(), toolbarEntity.id(), "Toolbar", true);
        assertFalse(result.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.DEPENDS_ON
            && ordersPageEntity.id().equals(rel.fromEntityId())
            && "main".equals(rel.label())));
    }

    @Test
    void extractsAngularFrontendRoutesIncludingNestedLazyGuardsAndResolvers() {
        String source = """
            import { Routes } from '@angular/router';

            export const routes: Routes = [
              {
                path: 'orders',
                component: OrdersPageComponent,
                canActivate: [AuthGuard],
                resolve: { initial: OrdersResolver },
                children: [
                  {
                    path: 'details',
                    loadChildren: () => import('./order-details.module').then(m => m.OrderDetailsModule)
                  }
                ]
              }
            ];

            export class OrdersPageComponent {}
            export class AuthGuard {}
            export class OrdersResolver {}
            export class OrderDetailsModule {}
            """;

        SyntaxNode ordersPage = classDeclaration(0, 0, 15, "OrdersPageComponent", List.of());
        SyntaxNode authGuard = classDeclaration(0, 0, 16, "AuthGuard", List.of());
        SyntaxNode ordersResolver = classDeclaration(0, 0, 17, "OrdersResolver", List.of());
        SyntaxNode detailsModule = classDeclaration(0, 0, 18, "OrderDetailsModule", List.of());

        StructuralExtractionResult result = extract("src/app/app.routes.ts", source,
            program(source, ordersPage, authGuard, ordersResolver, detailsModule));

        var ordersRoute = entity(result, EntityKind.UI_MODULE, "angular-route:/orders");
        var detailsRoute = entity(result, EntityKind.UI_MODULE, "angular-route:/orders/details");
        var ordersPageEntity = entity(result, EntityKind.CLASS, "OrdersPageComponent");
        var authGuardEntity = entity(result, EntityKind.CLASS, "AuthGuard");
        var ordersResolverEntity = entity(result, EntityKind.CLASS, "OrdersResolver");
        var detailsModuleEntity = entity(result, EntityKind.CLASS, "OrderDetailsModule");

        assertFrontendRouteRelationship(result, ordersRoute.id(), ordersPageEntity.id(), "OrdersPageComponent", "angular", "targets", true);
        assertFrontendRouteRelationship(result, ordersRoute.id(), authGuardEntity.id(), "AuthGuard", "angular", "guards", true);
        assertFrontendRouteRelationship(result, ordersRoute.id(), ordersResolverEntity.id(), "OrdersResolver", "angular", "resolves", true);
        assertFrontendRouteRelationship(result, detailsRoute.id(), detailsModuleEntity.id(), "OrderDetailsModule", "angular", "lazyLoads", true);
        assertFrontendRouteRelationship(result, detailsRoute.id(), ordersRoute.id(), "details", "angular", "childOf", true);
        assertEquals("/orders", ordersRoute.metadata().get("routeFullPath"));
        assertEquals("/orders/details", detailsRoute.metadata().get("routeFullPath"));
    }

    @Test
    void extractsReactFrontendRoutesFromObjectAndJsxRouteDeclarations() {
        String source = """
            import { createBrowserRouter, Route, Routes } from 'react-router-dom';

            export const router = createBrowserRouter([
              {
                path: '/',
                element: <AppShell />,
                children: [
                  {
                    path: 'orders',
                    element: <OrdersPage />
                  }
                ]
              }
            ]);

            export function AppRoutes() {
              return <Routes><Route path="reports" element={<ReportsPage />} /></Routes>;
            }

            export function AppShell() { return <main />; }
            export function OrdersPage() { return <section />; }
            export function ReportsPage() { return <article />; }
            """;

        SyntaxNode appRoutes = new SyntaxNode("function_declaration", true, 0, 0, 14, 0, 16, 1, false, false,
            "export function AppRoutes() { return <Routes><Route path=\"reports\" element={<ReportsPage />} /></Routes>; }", List.of(
                new SyntaxNode("identifier", true, 0, 0, 14, 16, 14, 25, false, false, "AppRoutes", List.of())
            ));
        SyntaxNode appShell = new SyntaxNode("function_declaration", true, 0, 0, 18, 0, 18, 47, false, false,
            "export function AppShell() { return <main />; }", List.of(
                new SyntaxNode("identifier", true, 0, 0, 18, 16, 18, 24, false, false, "AppShell", List.of())
            ));
        SyntaxNode ordersPage = new SyntaxNode("function_declaration", true, 0, 0, 19, 0, 19, 50, false, false,
            "export function OrdersPage() { return <section />; }", List.of(
                new SyntaxNode("identifier", true, 0, 0, 19, 16, 19, 26, false, false, "OrdersPage", List.of())
            ));
        SyntaxNode reportsPage = new SyntaxNode("function_declaration", true, 0, 0, 20, 0, 20, 51, false, false,
            "export function ReportsPage() { return <article />; }", List.of(
                new SyntaxNode("identifier", true, 0, 0, 20, 16, 20, 27, false, false, "ReportsPage", List.of())
            ));

        StructuralExtractionResult result = extract("src/app/router.tsx", source,
            program(source, appRoutes, appShell, ordersPage, reportsPage));

        var rootRoute = entity(result, EntityKind.UI_MODULE, "react-route:/");
        var ordersRoute = entity(result, EntityKind.UI_MODULE, "react-route:/orders");
        var reportsRoute = entity(result, EntityKind.UI_MODULE, "react-route:/reports");
        var appShellEntity = entity(result, EntityKind.FUNCTION, "AppShell");
        var ordersPageEntity = entity(result, EntityKind.FUNCTION, "OrdersPage");
        var reportsPageEntity = entity(result, EntityKind.FUNCTION, "ReportsPage");

        assertFrontendRouteRelationship(result, rootRoute.id(), appShellEntity.id(), "AppShell", "react", "targets", true);
        assertFrontendRouteRelationship(result, ordersRoute.id(), ordersPageEntity.id(), "OrdersPage", "react", "targets", true);
        assertFrontendRouteRelationship(result, ordersRoute.id(), rootRoute.id(), "orders", "react", "childOf", true);
        assertFrontendRouteRelationship(result, reportsRoute.id(), reportsPageEntity.id(), "ReportsPage", "react", "targets", true);
    }

    @Test
    void infersReactJsxCompositionTargetsWhenRenderedComponentIsNotDeclaredInSameFile() {
        String source = """
            export function OrdersPage() {
              return <PageLayout><OrdersTable /></PageLayout>;
            }
            """;

        SyntaxNode ordersPage = new SyntaxNode("function_declaration", true, 0, source.length(), 0, 0, 2, 1, false, false,
            """
            export function OrdersPage() {
              return <PageLayout><OrdersTable /></PageLayout>;
            }
            """.strip(), List.of(
                new SyntaxNode("identifier", true, 16, 26, 0, 16, 0, 26, false, false, "OrdersPage", List.of())
            ));

        StructuralExtractionResult result = extract("src/app/orders/OrdersPage.tsx", source, program(source, ordersPage));

        var ordersPageEntity = entity(result, EntityKind.FUNCTION, "OrdersPage");
        var pageLayoutEntity = entity(result, EntityKind.UI_MODULE, "PageLayout");
        var ordersTableEntity = entity(result, EntityKind.UI_MODULE, "OrdersTable");

        assertReactFrameworkRelationship(result, ordersPageEntity.id(), pageLayoutEntity.id(), "PageLayout", false);
        assertReactFrameworkRelationship(result, ordersPageEntity.id(), ordersTableEntity.id(), "OrdersTable", false);
        assertEquals("react-component-target", pageLayoutEntity.metadata().get("targetClassification"));
        assertEquals(Boolean.FALSE, pageLayoutEntity.metadata().get("external"));
    }


    @Test
    void extractsReactCustomHookClassificationAndUsageRelationships() {
        String source = """
            import { useContext } from 'react';
            import { useQuery } from '@tanstack/react-query';

            export const AuthContext = createContext(null);

            export function useAuth() {
              return useContext(AuthContext);
            }

            export function useOrdersQuery() {
              return useQuery({ queryKey: ['orders'], queryFn: async () => [] });
            }

            export function useOrdersScreenState() {
              const auth = useAuth();
              const orders = useOrdersQuery();
              return { auth, orders };
            }

            export function OrdersPage() {
              const auth = useAuth();
              const orders = useOrdersQuery();
              return <section>{auth?.user}-{orders.data?.length}</section>;
            }
            """;

        SyntaxNode useAuth = new SyntaxNode("function_declaration", true, 0, 0, 5, 0, 7, 1, false, false,
            """
            export function useAuth() {
              return useContext(AuthContext);
            }
            """.strip(), List.of(
                new SyntaxNode("identifier", true, 0, 0, 5, 16, 5, 23, false, false, "useAuth", List.of())
            ));
        SyntaxNode useOrdersQuery = new SyntaxNode("function_declaration", true, 0, 0, 9, 0, 11, 1, false, false,
            """
            export function useOrdersQuery() {
              return useQuery({ queryKey: ['orders'], queryFn: async () => [] });
            }
            """.strip(), List.of(
                new SyntaxNode("identifier", true, 0, 0, 9, 16, 9, 30, false, false, "useOrdersQuery", List.of())
            ));
        SyntaxNode useOrdersScreenState = new SyntaxNode("function_declaration", true, 0, 0, 13, 0, 17, 1, false, false,
            """
            export function useOrdersScreenState() {
              const auth = useAuth();
              const orders = useOrdersQuery();
              return { auth, orders };
            }
            """.strip(), List.of(
                new SyntaxNode("identifier", true, 0, 0, 13, 16, 13, 36, false, false, "useOrdersScreenState", List.of())
            ));
        SyntaxNode ordersPage = new SyntaxNode("function_declaration", true, 0, 0, 19, 0, 23, 1, false, false,
            """
            export function OrdersPage() {
              const auth = useAuth();
              const orders = useOrdersQuery();
              return <section>{auth?.user}-{orders.data?.length}</section>;
            }
            """.strip(), List.of(
                new SyntaxNode("identifier", true, 0, 0, 19, 16, 19, 26, false, false, "OrdersPage", List.of())
            ));

        StructuralExtractionResult result = extract("src/hooks/useOrders.tsx", source,
            program(source, useAuth, useOrdersQuery, useOrdersScreenState, ordersPage));

        var useAuthEntity = entity(result, EntityKind.FUNCTION, "useAuth");
        var useOrdersQueryEntity = entity(result, EntityKind.FUNCTION, "useOrdersQuery");
        var useOrdersScreenStateEntity = entity(result, EntityKind.FUNCTION, "useOrdersScreenState");
        var ordersPageEntity = entity(result, EntityKind.FUNCTION, "OrdersPage");

        assertEquals(Boolean.TRUE, useAuthEntity.metadata().get("reactHook"));
        assertEquals(Boolean.TRUE, useAuthEntity.metadata().get("customHook"));
        assertEquals("context", useAuthEntity.metadata().get("hookClassification"));
        assertEquals(Boolean.TRUE, useAuthEntity.metadata().get("declaredReactHook"));

        assertEquals(Boolean.TRUE, useOrdersQueryEntity.metadata().get("reactHook"));
        assertEquals("data-fetch", useOrdersQueryEntity.metadata().get("hookClassification"));

        assertReactHookRelationship(result, useOrdersScreenStateEntity.id(), useAuthEntity.id(), "useAuth", "hook", "context", true);
        assertReactHookRelationship(result, useOrdersScreenStateEntity.id(), useOrdersQueryEntity.id(), "useOrdersQuery", "hook", "data-fetch", true);
        assertReactHookRelationship(result, ordersPageEntity.id(), useAuthEntity.id(), "useAuth", "component", "context", true);
        assertReactHookRelationship(result, ordersPageEntity.id(), useOrdersQueryEntity.id(), "useOrdersQuery", "component", "data-fetch", true);
    }

    @Test
    void extractsReactContextProviderAndConsumerRelationships() {
        String source = """
            import React, { createContext, useContext } from 'react';

            export const AuthContext = createContext(null);

            export function AuthProvider({ children }) {
              return <AuthContext.Provider value={{ user: 'alice' }}>{children}</AuthContext.Provider>;
            }

            export function useAuth() {
              return useContext(AuthContext);
            }

            export function OrdersPage() {
              const auth = useContext(AuthContext);
              return <section>{auth?.user}</section>;
            }
            """;

        SyntaxNode authProvider = new SyntaxNode("function_declaration", true, 0, 0, 4, 0, 6, 1, false, false,
            """
            export function AuthProvider({ children }) {
              return <AuthContext.Provider value={{ user: 'alice' }}>{children}</AuthContext.Provider>;
            }
            """.strip(), List.of(
                new SyntaxNode("identifier", true, 0, 0, 4, 16, 4, 28, false, false, "AuthProvider", List.of())
            ));
        SyntaxNode useAuth = new SyntaxNode("function_declaration", true, 0, 0, 8, 0, 10, 1, false, false,
            """
            export function useAuth() {
              return useContext(AuthContext);
            }
            """.strip(), List.of(
                new SyntaxNode("identifier", true, 0, 0, 8, 16, 8, 23, false, false, "useAuth", List.of())
            ));
        SyntaxNode ordersPage = new SyntaxNode("function_declaration", true, 0, 0, 12, 0, 15, 1, false, false,
            """
            export function OrdersPage() {
              const auth = useContext(AuthContext);
              return <section>{auth?.user}</section>;
            }
            """.strip(), List.of(
                new SyntaxNode("identifier", true, 0, 0, 12, 16, 12, 26, false, false, "OrdersPage", List.of())
            ));

        StructuralExtractionResult result = extract("src/context/AuthProvider.tsx", source,
            program(source, authProvider, useAuth, ordersPage));

        var authContext = entity(result, EntityKind.UI_MODULE, "AuthContext");
        var authProviderEntity = entity(result, EntityKind.FUNCTION, "AuthProvider");
        var useAuthEntity = entity(result, EntityKind.FUNCTION, "useAuth");
        var ordersPageEntity = entity(result, EntityKind.FUNCTION, "OrdersPage");

        assertEquals(Boolean.TRUE, authContext.metadata().get("reactContext"));
        assertEquals(Boolean.TRUE, authContext.metadata().get("declaredReactContext"));
        assertEquals(Boolean.FALSE, authContext.metadata().get("external"));
        assertReactContextRelationship(result, authProviderEntity.id(), authContext.id(), "AuthContext", "providesContext", true);
        assertReactContextRelationship(result, useAuthEntity.id(), authContext.id(), "AuthContext", "consumesContext", true);
        assertReactContextRelationship(result, ordersPageEntity.id(), authContext.id(), "AuthContext", "consumesContext", true);
    }


    @Test
    void extractsAngularDiProviderAndConstructorInjectionRelationships() {
        String source = """
            export const ORDER_API = new InjectionToken<OrderApi>('ORDER_API');

            @Injectable()
            export class OrdersApiService {}

            export function ordersConfigFactory() { return {}; }

            @Injectable()
            export class OrdersFacade {}

            @Component({
              providers: [
                { provide: ORDER_API, useClass: OrdersApiService },
                { provide: ORDERS_CONFIG, useFactory: ordersConfigFactory },
                OrdersFacade
              ]
            })
            export class OrdersComponent {
              constructor(@Inject(ORDER_API) private api: OrdersApiService, private facade: OrdersFacade) {}
            }
            """;

        SyntaxNode ordersApiService = classDeclaration(0, 0, 3, "OrdersApiService", List.of(
            new SyntaxNode("decorator", true, 0, 0, 3, 0, 3, 13, false, false, "@Injectable()", List.of())
        ));
        SyntaxNode ordersConfigFactory = new SyntaxNode("function_declaration", true, 0, 0, 5, 0, 5, 60, false, false,
            "export function ordersConfigFactory() { return {}; }", List.of(
                new SyntaxNode("identifier", true, 0, 0, 5, 16, 5, 35, false, false, "ordersConfigFactory", List.of())
            ));
        SyntaxNode ordersFacade = classDeclaration(0, 0, 8, "OrdersFacade", List.of(
            new SyntaxNode("decorator", true, 0, 0, 7, 0, 7, 13, false, false, "@Injectable()", List.of())
        ));
        SyntaxNode ordersComponent = new SyntaxNode("class_declaration", true, 0, 0, 10, 0, 18, 1, false, false,
            """
            @Component({
              providers: [
                { provide: ORDER_API, useClass: OrdersApiService },
                { provide: ORDERS_CONFIG, useFactory: ordersConfigFactory },
                OrdersFacade
              ]
            })
            export class OrdersComponent {
              constructor(@Inject(ORDER_API) private api: OrdersApiService, private facade: OrdersFacade) {}
            }
            """.strip(), List.of(
                new SyntaxNode("decorator", true, 0, 0, 10, 0, 16, 2, false, false,
                    """
                    @Component({
                      providers: [
                        { provide: ORDER_API, useClass: OrdersApiService },
                        { provide: ORDERS_CONFIG, useFactory: ordersConfigFactory },
                        OrdersFacade
                      ]
                    })
                    """.strip(), List.of()),
                new SyntaxNode("type_identifier", true, 0, 0, 17, 13, 17, 28, false, false, "OrdersComponent", List.of())
            ));

        StructuralExtractionResult result = extract("src/app/orders.component.ts", source,
            program(source, ordersApiService, ordersConfigFactory, ordersFacade, ordersComponent));

        var ordersComponentEntity = entity(result, EntityKind.CLASS, "OrdersComponent");
        var ordersApiServiceEntity = entity(result, EntityKind.CLASS, "OrdersApiService");
        var ordersFacadeEntity = entity(result, EntityKind.CLASS, "OrdersFacade");
        var orderApiTokenEntity = entity(result, EntityKind.MODULE, "ORDER_API");
        var ordersConfigTokenEntity = entity(result, EntityKind.MODULE, "ORDERS_CONFIG");
        var ordersConfigFactoryEntity = entity(result, EntityKind.FUNCTION, "ordersConfigFactory");

        assertAngularDiRelationship(result, ordersComponentEntity.id(), orderApiTokenEntity.id(), "ORDER_API", "injects", true);
        assertAngularDiRelationship(result, ordersComponentEntity.id(), ordersFacadeEntity.id(), "OrdersFacade", "injects", true);
        assertAngularDiRelationship(result, orderApiTokenEntity.id(), ordersComponentEntity.id(), "ORDER_API", "providedBy", true);
        assertAngularDiRelationship(result, orderApiTokenEntity.id(), ordersApiServiceEntity.id(), "OrdersApiService", "resolvesTo", true);
        assertAngularDiRelationship(result, ordersFacadeEntity.id(), ordersComponentEntity.id(), "OrdersFacade", "providedBy", true);
        assertAngularDiRelationship(result, ordersConfigTokenEntity.id(), ordersConfigFactoryEntity.id(), "ordersConfigFactory", "resolvesTo", true);
    }

    private static void assertReactHookRelationship(
        StructuralExtractionResult result,
        String fromId,
        String toId,
        String label,
        String consumerKind,
        String hookClassification,
        boolean resolved
    ) {
        var relationship = result.relationships().stream()
            .filter(rel -> rel.kind() == info.isaksson.erland.architecturebrowser.indexer.ir.model.RelationshipKind.DEPENDS_ON
                && fromId.equals(rel.fromEntityId())
                && toId.equals(rel.toEntityId())
                && label.equals(rel.label())
                && "react".equals(rel.metadata().get("framework"))
                && "usesHook".equals(rel.metadata().get("frameworkRelationship")))
            .findFirst()
            .orElseThrow();
        assertEquals("react:uses-hook", relationship.metadata().get("dependencySource"));
        assertEquals(consumerKind, relationship.metadata().get("hookConsumerKind"));
        assertEquals(hookClassification, relationship.metadata().get("hookClassification"));
        assertEquals(resolved, relationship.metadata().get("resolvedFromReactHookExtraction"));
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


    private static void assertAngularFrameworkRelationship(StructuralExtractionResult result, String fromId, String toId, String label, String frameworkRelationship) {
        var relationship = result.relationships().stream()
            .filter(rel -> rel.kind() == RelationshipKind.DEPENDS_ON
                && fromId.equals(rel.fromEntityId())
                && toId.equals(rel.toEntityId())
                && label.equals(rel.label())
                && "angular".equals(rel.metadata().get("framework"))
                && frameworkRelationship.equals(rel.metadata().get("frameworkRelationship")))
            .findFirst()
            .orElse(null);
        assertNotNull(relationship);
        assertEquals("angular:" + frameworkRelationship, relationship.metadata().get("dependencySource"));
    }

    private static void assertFrontendRouteRelationship(
        StructuralExtractionResult result,
        String fromId,
        String toId,
        String label,
        String framework,
        String frameworkRelationship,
        boolean resolved
    ) {
        var relationship = result.relationships().stream()
            .filter(rel -> rel.kind() == RelationshipKind.DEPENDS_ON
                && fromId.equals(rel.fromEntityId())
                && toId.equals(rel.toEntityId())
                && label.equals(rel.label())
                && framework.equals(rel.metadata().get("framework"))
                && frameworkRelationship.equals(rel.metadata().get("frameworkRelationship")))
            .findFirst()
            .orElse(null);
        assertNotNull(relationship);
        assertEquals(framework + ":route-" + frameworkRelationship, relationship.metadata().get("dependencySource"));
        assertEquals(resolved, relationship.metadata().get("resolvedFromRouteExtraction"));
    }

    private static void assertReactFrameworkRelationship(StructuralExtractionResult result, String fromId, String toId, String label, boolean resolved) {
        var relationship = result.relationships().stream()
            .filter(rel -> rel.kind() == RelationshipKind.DEPENDS_ON
                && fromId.equals(rel.fromEntityId())
                && toId.equals(rel.toEntityId())
                && label.equals(rel.label())
                && "react".equals(rel.metadata().get("framework"))
                && "renders".equals(rel.metadata().get("frameworkRelationship")))
            .findFirst()
            .orElse(null);
        assertNotNull(relationship);
        assertEquals("react:jsx-renders", relationship.metadata().get("dependencySource"));
        assertEquals(resolved, relationship.metadata().get("resolvedFromJsxComposition"));
    }


    private static void assertAngularDiRelationship(
        StructuralExtractionResult result,
        String fromId,
        String toId,
        String label,
        String frameworkRelationship,
        boolean resolved
    ) {
        var relationship = result.relationships().stream()
            .filter(rel -> rel.kind() == RelationshipKind.DEPENDS_ON
                && fromId.equals(rel.fromEntityId())
                && toId.equals(rel.toEntityId())
                && label.equals(rel.label())
                && "angular".equals(rel.metadata().get("framework"))
                && frameworkRelationship.equals(rel.metadata().get("frameworkRelationship")))
            .findFirst()
            .orElse(null);
        assertNotNull(relationship);
        assertEquals("angular:" + frameworkRelationship, relationship.metadata().get("dependencySource"));
        assertEquals(resolved, relationship.metadata().get("resolvedFromAngularDiExtraction"));
    }


    private static void assertReactContextRelationship(
        StructuralExtractionResult result,
        String fromId,
        String toId,
        String label,
        String frameworkRelationship,
        boolean resolved
    ) {
        var relationship = result.relationships().stream()
            .filter(rel -> rel.kind() == RelationshipKind.DEPENDS_ON
                && fromId.equals(rel.fromEntityId())
                && toId.equals(rel.toEntityId())
                && label.equals(rel.label())
                && "react".equals(rel.metadata().get("framework"))
                && frameworkRelationship.equals(rel.metadata().get("frameworkRelationship")))
            .findFirst()
            .orElse(null);
        assertNotNull(relationship);
        assertEquals("providesContext".equals(frameworkRelationship) ? "react:provides-context" : "react:consumes-context", relationship.metadata().get("dependencySource"));
        assertEquals(resolved, relationship.metadata().get("resolvedFromReactContextExtraction"));
    }


    private static ExtractedEntityFact entity(StructuralExtractionResult result, EntityKind kind, String name) {
        return result.entities().stream()
            .filter(entity -> entity.kind() == kind && name.equals(entity.name()))
            .sorted((left, right) -> Integer.compare(entityScore(right), entityScore(left)))
            .findFirst()
            .orElseThrow();
    }

    private static int entityScore(ExtractedEntityFact entity) {
        int score = 0;
        if (Boolean.TRUE.equals(entity.metadata().get("reactContext"))) {
            score += 10;
        }
        if (Boolean.TRUE.equals(entity.metadata().get("declaredReactContext"))) {
            score += 5;
        }
        if (Boolean.FALSE.equals(entity.metadata().get("external"))) {
            score += 2;
        }
        return score;
    }
}
