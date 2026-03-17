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

class TypeScriptTypeRelationshipRegressionTest extends AbstractTypeScriptExtractionTestSupport {


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

}
