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

class TypeScriptPropertyMetadataSeamTest extends AbstractTypeScriptExtractionTestSupport {

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
