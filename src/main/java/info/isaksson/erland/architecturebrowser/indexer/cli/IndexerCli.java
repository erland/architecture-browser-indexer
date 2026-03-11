package info.isaksson.erland.architecturebrowser.indexer.cli;

import info.isaksson.erland.architecturebrowser.indexer.acquisition.AcquisitionRequest;
import info.isaksson.erland.architecturebrowser.indexer.acquisition.AcquisitionResult;
import info.isaksson.erland.architecturebrowser.indexer.acquisition.SourceAcquisitionService;
import info.isaksson.erland.architecturebrowser.indexer.extract.StructuralExtractionService;
import info.isaksson.erland.architecturebrowser.indexer.extract.StructuralExtractorRegistry;
import info.isaksson.erland.architecturebrowser.indexer.extract.model.StructuralExtractionResult;
import info.isaksson.erland.architecturebrowser.indexer.ir.ArchitectureIrFactory;
import info.isaksson.erland.architecturebrowser.indexer.ir.ArchitectureIrValidator;
import info.isaksson.erland.architecturebrowser.indexer.ir.json.ArchitectureIrJson;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureIndexDocument;
import info.isaksson.erland.architecturebrowser.indexer.parse.ParseBatchResult;
import info.isaksson.erland.architecturebrowser.indexer.parse.TreeSitterParserRegistryFactory;
import info.isaksson.erland.architecturebrowser.indexer.parse.TreeSitterParsingService;
import info.isaksson.erland.architecturebrowser.indexer.parse.TreeSitterRuntimeDetector;
import info.isaksson.erland.architecturebrowser.indexer.scan.FileInventory;
import info.isaksson.erland.architecturebrowser.indexer.scan.FileInventoryScanner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public final class IndexerCli {
    public static final String APPLICATION_VERSION = "0.1.0-SNAPSHOT";

    private IndexerCli() {
    }

    public static void main(String[] args) throws Exception {
        CliArguments arguments = CliArguments.parse(args);
        if (arguments.showHelp()) {
            printHelp();
            return;
        }
        if (arguments.showVersion()) {
            System.out.println(APPLICATION_VERSION);
            return;
        }
        if (!arguments.hasInput() || arguments.outputPath() == null) {
            System.err.println("Missing required arguments: provide exactly one of --source <path> or --git-url <url>, and --output <path>");
            printHelp();
            System.exit(2);
            return;
        }

        SourceAcquisitionService acquisitionService = new SourceAcquisitionService();
        FileInventoryScanner scanner = new FileInventoryScanner();
        TreeSitterParsingService parsingService = new TreeSitterParsingService(TreeSitterParserRegistryFactory.createDefaultRegistry());

        AcquisitionRequest request = new AcquisitionRequest(
            arguments.repositoryId(),
            arguments.sourcePath(),
            arguments.gitUrl(),
            arguments.gitRef(),
            arguments.workingDirectory()
        );
        AcquisitionResult acquisitionResult = acquisitionService.acquire(request);
        FileInventory inventory = scanner.scan(acquisitionResult.acquiredRoot());
        ParseBatchResult parseBatchResult = parsingService.parseInventory(acquisitionResult.acquiredRoot(), inventory);
        StructuralExtractionResult extractionResult = new StructuralExtractionService(StructuralExtractorRegistry.defaultRegistry())
            .extract(parseBatchResult);

        ArchitectureIndexDocument document = ArchitectureIrFactory.createInventoryDocument(
            acquisitionResult.repositorySource(),
            APPLICATION_VERSION,
            inventory,
            acquisitionResult.diagnostics(),
            parseBatchResult,
            extractionResult
        );

        ArchitectureIrValidator.ValidationResult validation = ArchitectureIrValidator.validate(document);
        if (!validation.isValid()) {
            throw new IllegalStateException("Invalid IR document: " + String.join("; ", validation.messages()));
        }

        Path output = arguments.outputPath().toAbsolutePath().normalize();
        Files.createDirectories(output.getParent() == null ? Path.of(".") : output.getParent());
        ArchitectureIrJson.write(document, output);

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("status", "ok");
        summary.put("repositoryId", document.source().repositoryId());
        summary.put("acquisitionType", document.source().acquisitionType());
        summary.put("sourcePath", acquisitionResult.acquiredRoot().toString());
        summary.put("output", output.toString());
        summary.put("schemaVersion", document.schemaVersion());
        summary.put("indexedFiles", inventory.indexedFiles());
        summary.put("ignoredFiles", inventory.ignoredFiles());
        summary.put("detectedLanguages", inventory.detectedLanguages());
        summary.put("detectedTechnologyMarkers", inventory.detectedTechnologyMarkers());
        summary.put("treeSitterRuntime", TreeSitterRuntimeDetector.detect().detail());
        summary.put("parseSummary", TreeSitterParsingService.summarize(parseBatchResult));
        summary.put("extractionSummary", document.metadata().get("extractionSummary"));
        System.out.println(ArchitectureIrJson.toPrettyJson(summary));

        if (acquisitionResult.temporaryWorkspace()) {
            deleteRecursively(acquisitionResult.acquiredRoot().getParent());
        }
    }

    private static void deleteRecursively(Path path) {
        if (path == null || !Files.exists(path)) {
            return;
        }
        try (var stream = Files.walk(path)) {
            stream.sorted(java.util.Comparator.reverseOrder()).forEach(candidate -> {
                try {
                    Files.deleteIfExists(candidate);
                } catch (IOException ignored) {
                    // Best-effort cleanup for temp Git workspaces.
                }
            });
        } catch (IOException ignored) {
            // Best-effort cleanup for temp Git workspaces.
        }
    }

    private static void printHelp() {
        System.out.println("""
            architecture-browser-indexer

            Usage:
              --help                           Show help
              --version                        Show version
              --source <path>                  Source repository path
              --git-url <url-or-path>          Git repository URL or local Git path
              --git-ref <branch-or-tag>        Optional Git branch/reference
              --repository-id <id>             Optional repository identity override
              --working-dir <path>             Optional workspace parent for Git acquisition
              --output <path>                  Output JSON file
            """);
    }

    public record CliArguments(
        boolean showHelp,
        boolean showVersion,
        Path sourcePath,
        String gitUrl,
        String gitRef,
        String repositoryId,
        Path workingDirectory,
        Path outputPath
    ) {
        boolean hasInput() {
            return (sourcePath != null) ^ (gitUrl != null && !gitUrl.isBlank());
        }

        static CliArguments parse(String[] args) {
            boolean help = false;
            boolean version = false;
            Path source = null;
            String gitUrl = null;
            String gitRef = null;
            String repositoryId = null;
            Path workingDirectory = null;
            Path output = null;

            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                switch (arg) {
                    case "--help", "-h" -> help = true;
                    case "--version", "-v" -> version = true;
                    case "--source" -> {
                        i = requireValue(args, i, arg);
                        source = Path.of(args[i]);
                    }
                    case "--git-url" -> {
                        i = requireValue(args, i, arg);
                        gitUrl = args[i];
                    }
                    case "--git-ref" -> {
                        i = requireValue(args, i, arg);
                        gitRef = args[i];
                    }
                    case "--repository-id" -> {
                        i = requireValue(args, i, arg);
                        repositoryId = args[i];
                    }
                    case "--working-dir" -> {
                        i = requireValue(args, i, arg);
                        workingDirectory = Path.of(args[i]);
                    }
                    case "--output" -> {
                        i = requireValue(args, i, arg);
                        output = Path.of(args[i]);
                    }
                    default -> throw new IllegalArgumentException("Unknown argument: " + arg);
                }
            }
            return new CliArguments(help, version, source, gitUrl, gitRef, repositoryId, workingDirectory, output);
        }

        private static int requireValue(String[] args, int index, String option) {
            int valueIndex = index + 1;
            if (valueIndex >= args.length) {
                throw new IllegalArgumentException("Missing value for " + option);
            }
            return valueIndex;
        }
    }
}
