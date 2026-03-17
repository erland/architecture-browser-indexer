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

class TypeScriptImportBoundarySeamTest extends AbstractTypeScriptExtractionTestSupport {

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

}
