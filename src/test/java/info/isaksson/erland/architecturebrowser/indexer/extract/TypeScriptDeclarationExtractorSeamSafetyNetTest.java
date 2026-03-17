package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact;
import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractionMode;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RelationshipKind;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

class TypeScriptDeclarationExtractorSeamSafetyNetTest {

    @Test
    void preservesDeclarationMemberAndTypeRelationshipContractsThroughDedicatedDeclarationSeam() {
        String source = """
            export interface BaseContract {}
            export interface UserContract extends BaseContract {}
            export class BaseService {
              currentUser: UserContract;
            }
            export class UserService extends BaseService implements UserContract {
              readonly profile: UserContract;
              loadData(): UserContract { return this.profile; }
            }
            export const loadData = () => 42;
            """;

        SyntaxNode baseContract = new SyntaxNode("interface_declaration", true, 0, 32, 0, 0, 0, 32, false, false,
            "export interface BaseContract {}", List.of(
                new SyntaxNode("type_identifier", true, 17, 29, 0, 17, 0, 29, false, false, "BaseContract", List.of())
            ));
        SyntaxNode userContract = new SyntaxNode("interface_declaration", true, 33, 84, 1, 0, 1, 51, false, false,
            "export interface UserContract extends BaseContract {}", List.of(
                new SyntaxNode("type_identifier", true, 50, 62, 1, 17, 1, 29, false, false, "UserContract", List.of()),
                new SyntaxNode("extends_type_clause", true, 63, 83, 1, 30, 1, 50, false, false, "extends BaseContract", List.of(
                    new SyntaxNode("type_identifier", true, 71, 83, 1, 38, 1, 50, false, false, "BaseContract", List.of())
                ))
            ));
        SyntaxNode baseService = new SyntaxNode("class_declaration", true, 85, 155, 2, 0, 4, 1, false, false,
            "export class BaseService {\n  currentUser: UserContract;\n}", List.of(
                new SyntaxNode("type_identifier", true, 98, 109, 2, 13, 2, 24, false, false, "BaseService", List.of()),
                new SyntaxNode("public_field_definition", true, 114, 140, 3, 2, 3, 28, false, false, "currentUser: UserContract;", List.of(
                    new SyntaxNode("property_identifier", true, 114, 125, 3, 2, 3, 13, false, false, "currentUser", List.of()),
                    new SyntaxNode("type_annotation", true, 125, 139, 3, 13, 3, 27, false, false, ": UserContract", List.of(
                        new SyntaxNode("type_identifier", true, 127, 139, 3, 15, 3, 27, false, false, "UserContract", List.of())
                    ))
                ))
            ));
        SyntaxNode userService = new SyntaxNode("class_declaration", true, 156, 342, 5, 0, 8, 1, false, false,
            "export class UserService extends BaseService implements UserContract {\n  readonly profile: UserContract;\n  loadData(): UserContract { return this.profile; }\n}", List.of(
                new SyntaxNode("type_identifier", true, 169, 180, 5, 13, 5, 24, false, false, "UserService", List.of()),
                new SyntaxNode("extends_clause", true, 181, 200, 5, 25, 5, 44, false, false, "extends BaseService", List.of(
                    new SyntaxNode("type_identifier", true, 189, 200, 5, 33, 5, 44, false, false, "BaseService", List.of())
                )),
                new SyntaxNode("implements_clause", true, 201, 225, 5, 45, 5, 69, false, false, "implements UserContract", List.of(
                    new SyntaxNode("type_identifier", true, 212, 224, 5, 56, 5, 68, false, false, "UserContract", List.of())
                )),
                new SyntaxNode("public_field_definition", true, 230, 259, 6, 2, 6, 31, false, false, "readonly profile: UserContract;", List.of(
                    new SyntaxNode("accessibility_modifier", true, 230, 238, 6, 2, 6, 10, false, false, "readonly", List.of()),
                    new SyntaxNode("property_identifier", true, 239, 246, 6, 11, 6, 18, false, false, "profile", List.of()),
                    new SyntaxNode("type_annotation", true, 246, 259, 6, 18, 6, 31, false, false, ": UserContract", List.of(
                        new SyntaxNode("type_identifier", true, 248, 260, 6, 20, 6, 32, false, false, "UserContract", List.of())
                    ))
                )),
                new SyntaxNode("method_definition", true, 264, 324, 7, 2, 7, 62, false, false, "loadData(): UserContract { return this.profile; }", List.of(
                    new SyntaxNode("property_identifier", true, 264, 272, 7, 2, 7, 10, false, false, "loadData", List.of()),
                    new SyntaxNode("formal_parameters", true, 272, 274, 7, 10, 7, 12, false, false, "()", List.of()),
                    new SyntaxNode("type_annotation", true, 274, 288, 7, 12, 7, 26, false, false, ": UserContract", List.of(
                        new SyntaxNode("type_identifier", true, 276, 288, 7, 14, 7, 26, false, false, "UserContract", List.of())
                    ))
                ))
            ));
        SyntaxNode loadData = new SyntaxNode("variable_declarator", true, 343, source.length(), 9, 13, 9, 35, false, false,
            "loadData = () => 42", List.of(
                new SyntaxNode("identifier", true, 343, 351, 9, 13, 9, 21, false, false, "loadData", List.of()),
                new SyntaxNode("arrow_function", true, 354, 361, 9, 24, 9, 31, false, false, "() => 42", List.of())
            ));

        ExtractionAccumulator accumulator = new ExtractionAccumulator();
        TypeScriptDeclarationExtractor.TypeScriptDeclarationResult result = TypeScriptDeclarationExtractor.extract(new TypeScriptExtractionContext(
            parseResult("src/app/contracts.ts", source, program(source, baseContract, userContract, baseService, userService, loadData)),
            accumulator,
            "src/app/contracts.ts",
            ExtractionMode.SYNTAX_TREE,
            program(source, baseContract, userContract, baseService, userService, loadData),
            ExtractionSupport.fileModuleEntity("scope:file:contracts", "src/app/contracts.ts", "typescript")
        ));

        ExtractedEntityFact userServiceEntity = result.namedEntities().get("UserService");
        ExtractedEntityFact userContractEntity = result.namedEntities().get("UserContract");
        ExtractedEntityFact loadDataEntity = result.namedEntities().get("loadData");

        assertEquals(EntityKind.CLASS, userServiceEntity.kind());
        assertEquals(EntityKind.INTERFACE, userContractEntity.kind());
        assertEquals(EntityKind.FUNCTION, loadDataEntity.kind());
        assertEquals("class", userServiceEntity.metadata().get("declarationKind"));

        assertTrue(accumulator.entities().stream().anyMatch(entity -> entity.kind() == EntityKind.FIELD
            && "profile".equals(entity.name())
            && String.valueOf(entity.metadata().get("ownerQualifiedName")).endsWith("UserService")));
        assertTrue(accumulator.entities().stream().anyMatch(entity -> entity.kind() == EntityKind.FUNCTION
            && "loadData".equals(entity.name())
            && String.valueOf(entity.metadata().get("ownerQualifiedName")).endsWith("UserService")));
        assertTrue(accumulator.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.EXTENDS
            && rel.fromEntityId().equals(userServiceEntity.id())));
        assertTrue(accumulator.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.IMPLEMENTS
            && rel.fromEntityId().equals(userServiceEntity.id())
            && rel.toEntityId().equals(userContractEntity.id())));
    }

    private static SourceParseResult parseResult(String relativePath, String source, SyntaxNode root) {
        return new SourceParseResult(
            new SourceParseRequest(Path.of(relativePath), relativePath, ParseLanguage.TYPESCRIPT, source),
            ParseStatus.SUCCESS,
            new SyntaxTree(ParseLanguage.TYPESCRIPT, "tree-sitter-jtreesitter", root, false, root.nodeCount()),
            List.of(),
            Map.of("parserBackend", "tree-sitter-jtreesitter")
        );
    }

    private static SyntaxNode program(String source, SyntaxNode... children) {
        int endLine = Math.max(0, source.split("\\R", -1).length - 1);
        int endColumn = source.isEmpty() ? 0 : source.length() - source.lastIndexOf('\n') - 1;
        return new SyntaxNode("program", true, 0, source.length(), 0, 0, endLine, endColumn, false, false, source, List.of(children));
    }
}
