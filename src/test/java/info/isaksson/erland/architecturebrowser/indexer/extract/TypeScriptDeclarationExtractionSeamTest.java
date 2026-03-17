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

class TypeScriptDeclarationExtractionSeamTest extends AbstractTypeScriptExtractionTestSupport {



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

}
