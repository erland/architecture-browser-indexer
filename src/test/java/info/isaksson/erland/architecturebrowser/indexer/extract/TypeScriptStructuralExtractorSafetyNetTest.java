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

class TypeScriptStructuralExtractorSafetyNetTest extends AbstractTypeScriptExtractionTestSupport {

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

}
