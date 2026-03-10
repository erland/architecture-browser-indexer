package info.isaksson.erland.architecturebrowser.indexer.parse;

import info.isaksson.erland.architecturebrowser.indexer.scan.FileInventory;
import info.isaksson.erland.architecturebrowser.indexer.scan.FileInventoryEntry;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class TreeSitterParsingService {
    private final ParserRegistry parserRegistry;

    public TreeSitterParsingService(ParserRegistry parserRegistry) {
        this.parserRegistry = parserRegistry;
    }

    public ParseBatchResult parseInventory(Path repositoryRoot, FileInventory inventory) {
        List<SourceParseResult> results = new ArrayList<>();
        Map<ParseLanguage, Integer> attemptedByLanguage = new EnumMap<>(ParseLanguage.class);
        Map<ParseStatus, Integer> countsByStatus = new EnumMap<>(ParseStatus.class);

        for (FileInventoryEntry entry : inventory.entries()) {
            if (entry.ignored()) {
                continue;
            }
            ParseLanguage language = ParseLanguage.fromInventoryLanguage(entry.detectedLanguage()).orElse(null);
            if (language == null) {
                continue;
            }
            attemptedByLanguage.merge(language, 1, Integer::sum);
            SourceParseResult result = parseSingle(repositoryRoot, entry, language);
            results.add(result);
            countsByStatus.merge(result.status(), 1, Integer::sum);
        }

        return new ParseBatchResult(results, attemptedByLanguage, countsByStatus);
    }

    public SourceParseResult parseSingle(Path repositoryRoot, FileInventoryEntry entry, ParseLanguage language) {
        Path absolutePath = repositoryRoot.resolve(entry.relativePath()).normalize();
        String sourceText;
        try {
            sourceText = Files.readString(absolutePath, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return new SourceParseResult(
                new SourceParseRequest(absolutePath, entry.relativePath(), language, null),
                ParseStatus.PARSE_ERROR,
                null,
                List.of(new ParseIssue("parse.io.read-failed", e.getMessage(), null, null, true)),
                Map.of("relativePath", entry.relativePath())
            );
        }

        SourceParseRequest request = new SourceParseRequest(absolutePath, entry.relativePath(), language, sourceText);
        return parserRegistry.find(language)
            .map(parser -> parser.parse(request))
            .orElseGet(() -> new SourceParseResult(
                request,
                ParseStatus.UNSUPPORTED_LANGUAGE,
                null,
                List.of(new ParseIssue(
                    "parse.language.unsupported",
                    "No parser registered for language " + language.inventoryKey(),
                    null,
                    null,
                    false
                )),
                Map.of("relativePath", entry.relativePath(), "language", language.inventoryKey())
            ));
    }

    public static Map<String, Object> summarize(ParseBatchResult batchResult) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("attemptedByLanguage", batchResult.attemptedByLanguage());
        summary.put("countsByStatus", batchResult.countsByStatus());
        summary.put("successfulFiles", batchResult.results().stream().filter(SourceParseResult::successful).count());
        summary.put("parseableFiles", batchResult.results().size());
        return summary;
    }
}
