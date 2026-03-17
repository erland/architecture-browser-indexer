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

}
