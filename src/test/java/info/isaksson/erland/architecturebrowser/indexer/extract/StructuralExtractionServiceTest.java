package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.StructuralExtractionResult;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RelationshipKind;
import info.isaksson.erland.architecturebrowser.indexer.parse.ParseBatchResult;
import info.isaksson.erland.architecturebrowser.indexer.parse.ParseLanguage;
import info.isaksson.erland.architecturebrowser.indexer.parse.ParseStatus;
import info.isaksson.erland.architecturebrowser.indexer.parse.SourceParseRequest;
import info.isaksson.erland.architecturebrowser.indexer.parse.SourceParseResult;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StructuralExtractionServiceTest {
    @Test
    void extractsJavaStructuralFactsFromSourceTextEvenWithoutBackendSyntaxTree() {
        String source = """
            package com.example.demo;
            import org.springframework.web.bind.annotation.GetMapping;
            import java.util.List;

            @RestController
            public class DemoController {
                @GetMapping("/demo")
                public String getDemo(List<String> values) {
                    return "ok";
                }
            }
            """;
        SourceParseResult parseResult = new SourceParseResult(
            new SourceParseRequest(Path.of("src/main/java/com/example/demo/DemoController.java"), "src/main/java/com/example/demo/DemoController.java", ParseLanguage.JAVA, source),
            ParseStatus.BACKEND_UNAVAILABLE,
            null,
            List.of(),
            Map.of()
        );

        StructuralExtractionResult result = new StructuralExtractionService(StructuralExtractorRegistry.defaultRegistry())
            .extract(new ParseBatchResult(List.of(parseResult), Map.of(ParseLanguage.JAVA, 1), Map.of(ParseStatus.BACKEND_UNAVAILABLE, 1)));

        assertTrue(result.scopes().stream().anyMatch(scope -> "com.example.demo".equals(scope.name())));
        assertTrue(result.entities().stream().anyMatch(entity -> entity.kind() == EntityKind.CLASS && "DemoController".equals(entity.name())));
        assertTrue(result.entities().stream().anyMatch(entity -> entity.kind() == EntityKind.FUNCTION && "getDemo".equals(entity.name())));
        assertTrue(result.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.DEPENDS_ON && "org.springframework.web.bind.annotation.GetMapping".equals(rel.label())));
        assertEquals(1, result.summary().extractedByLanguage().get("java"));
    }

    @Test
    void extractsTypescriptClassesFunctionsImportsAndDecorators() {
        String source = """
            import { Injectable } from '@nestjs/common';
            import { HttpClient } from './http-client';

            @Injectable()
            export class ApiService {}

            export function loadData() {
                return 1;
            }

            @Component
            export const App = () => null;
            """;
        SourceParseResult parseResult = new SourceParseResult(
            new SourceParseRequest(Path.of("src/app/api-service.ts"), "src/app/api-service.ts", ParseLanguage.TYPESCRIPT, source),
            ParseStatus.BACKEND_UNAVAILABLE,
            null,
            List.of(),
            Map.of()
        );

        StructuralExtractionResult result = new StructuralExtractionService(StructuralExtractorRegistry.defaultRegistry())
            .extract(new ParseBatchResult(List.of(parseResult), Map.of(ParseLanguage.TYPESCRIPT, 1), Map.of(ParseStatus.BACKEND_UNAVAILABLE, 1)));

        assertTrue(result.entities().stream().anyMatch(entity -> entity.kind() == EntityKind.CLASS && "ApiService".equals(entity.name())));
        assertTrue(result.entities().stream().anyMatch(entity -> entity.kind() == EntityKind.FUNCTION && "loadData".equals(entity.name())));
        assertTrue(result.entities().stream().anyMatch(entity -> entity.kind() == EntityKind.FUNCTION && "App".equals(entity.name())
            && ((List<?>) entity.metadata().get("decorators")).contains("Component")));
        assertTrue(result.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.DEPENDS_ON && "@nestjs/common".equals(rel.label())));
        assertEquals(1, result.summary().extractedByLanguage().get("typescript"));
    }
}
