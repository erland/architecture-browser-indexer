package info.isaksson.erland.architecturebrowser.indexer.cli;

import info.isaksson.erland.architecturebrowser.indexer.acquisition.AcquisitionRequest;
import info.isaksson.erland.architecturebrowser.indexer.acquisition.AcquisitionResult;
import info.isaksson.erland.architecturebrowser.indexer.acquisition.SourceAcquisitionService;
import info.isaksson.erland.architecturebrowser.indexer.extract.StructuralExtractionService;
import info.isaksson.erland.architecturebrowser.indexer.extract.StructuralExtractorRegistry;
import info.isaksson.erland.architecturebrowser.indexer.extract.model.StructuralExtractionResult;
import info.isaksson.erland.architecturebrowser.indexer.ir.ArchitectureIrFactory;
import info.isaksson.erland.architecturebrowser.indexer.interpret.InterpretationRegistry;
import info.isaksson.erland.architecturebrowser.indexer.interpret.InterpretationService;
import info.isaksson.erland.architecturebrowser.indexer.interpret.model.InterpretationResult;
import info.isaksson.erland.architecturebrowser.indexer.incremental.FileFingerprintService;
import info.isaksson.erland.architecturebrowser.indexer.incremental.IncrementalDiffService;
import info.isaksson.erland.architecturebrowser.indexer.incremental.IncrementalPlanner;
import info.isaksson.erland.architecturebrowser.indexer.incremental.IncrementalSnapshotJson;
import info.isaksson.erland.architecturebrowser.indexer.incremental.model.IncrementalDiff;
import info.isaksson.erland.architecturebrowser.indexer.incremental.model.IncrementalPlan;
import info.isaksson.erland.architecturebrowser.indexer.incremental.model.IncrementalSnapshot;
import info.isaksson.erland.architecturebrowser.indexer.topology.TopologyService;
import info.isaksson.erland.architecturebrowser.indexer.topology.model.TopologyResult;
import info.isaksson.erland.architecturebrowser.indexer.ir.ArchitectureIrValidator;
import info.isaksson.erland.architecturebrowser.indexer.ir.json.ArchitectureIrJson;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureIndexDocument;
import info.isaksson.erland.architecturebrowser.indexer.publish.ExportBundleWriter;
import info.isaksson.erland.architecturebrowser.indexer.publish.model.ExportBundle;
import info.isaksson.erland.architecturebrowser.indexer.parse.ParseBatchResult;
import info.isaksson.erland.architecturebrowser.indexer.parse.TreeSitterConfiguration;
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
        TreeSitterConfiguration treeSitterConfiguration = TreeSitterConfiguration.fromEnvironment();
        var treeSitterRuntimeStatus = TreeSitterRuntimeDetector.detect(treeSitterConfiguration);
        TreeSitterParsingService parsingService = new TreeSitterParsingService(
            TreeSitterParserRegistryFactory.createDefaultRegistry(treeSitterConfiguration));

        AcquisitionRequest request = new AcquisitionRequest(
            arguments.repositoryId(),
            arguments.sourcePath(),
            arguments.gitUrl(),
            arguments.gitRef(),
            arguments.workingDirectory()
        );
        AcquisitionResult acquisitionResult = acquisitionService.acquire(request);
        FileInventory inventory = scanner.scan(acquisitionResult.acquiredRoot());
        String snapshotInArg = arguments.snapshotIn();
        String snapshotOutArg = arguments.snapshotOut();
        FileFingerprintService fingerprintService = new FileFingerprintService();
        IncrementalSnapshot currentSnapshot = fingerprintService.createSnapshot(inventory);
        IncrementalSnapshot previousSnapshot = null;
        if (snapshotInArg != null && !snapshotInArg.isBlank()) {
            try {
                previousSnapshot = IncrementalSnapshotJson.read(Path.of(snapshotInArg));
            } catch (Exception ex) {
                System.err.println("Warning: failed to read incremental snapshot from " + snapshotInArg + ": " + ex.getMessage());
            }
        }
        IncrementalDiff incrementalDiff = new IncrementalDiffService().diff(previousSnapshot, currentSnapshot);
        IncrementalPlan incrementalPlan = new IncrementalPlanner().plan(incrementalDiff);
        ParseBatchResult parseBatchResult = parsingService.parseInventory(acquisitionResult.acquiredRoot(), inventory);
        StructuralExtractionResult extractionResult = new StructuralExtractionService(StructuralExtractorRegistry.defaultRegistry())
            .extract(parseBatchResult);
        InterpretationResult interpretationResult = new InterpretationService(InterpretationRegistry.defaultRegistry())
            .interpret(extractionResult);
        TopologyResult topologyResult = new TopologyService()
            .infer(inventory, extractionResult, interpretationResult);

        ArchitectureIndexDocument document = ArchitectureIrFactory.createInventoryDocument(
            acquisitionResult.repositorySource(),
            APPLICATION_VERSION,
            inventory,
            acquisitionResult.diagnostics(),
            parseBatchResult,
            extractionResult,
            interpretationResult,
            topologyResult
        );

        ArchitectureIrValidator.ValidationResult validation = ArchitectureIrValidator.validate(document);
        if (!validation.isValid()) {
            throw new IllegalStateException("Invalid IR document: " + String.join("; ", validation.messages()));
        }

        Path output = arguments.outputPath().toAbsolutePath().normalize();
        Files.createDirectories(output.getParent() == null ? Path.of(".") : output.getParent());
        ArchitectureIrJson.write(document, output);

        ExportBundleWriter exportBundleWriter = new ExportBundleWriter();
        ExportBundle exportBundle = exportBundleWriter.createBundle(document, APPLICATION_VERSION, output.getFileName().toString());
        exportBundleWriter.writeBundle(output, exportBundle);

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
        summary.put("treeSitterRuntime", treeSitterRuntimeStatus.detail());
        summary.put("parseSummary", TreeSitterParsingService.summarize(parseBatchResult));
        summary.put("extractionSummary", document.metadata().get("extractionSummary"));
        summary.put("interpretationSummary", document.metadata().get("interpretationSummary"));
        summary.put("topologySummary", document.metadata().get("topologySummary"));
        summary.put("diagnosticSummary", document.metadata().get("diagnosticSummary"));
        summary.put("partialResult", document.metadata().get("partialResult"));
        summary.put("incrementalPlan", incrementalPlan.metadata());
        summary.put("incrementalPaths", incrementalPlan.parsePaths());
        summary.put("exportManifestPreview", exportBundle.manifest());

        if (snapshotOutArg != null && !snapshotOutArg.isBlank()) {
            IncrementalSnapshotJson.write(Path.of(snapshotOutArg), currentSnapshot);
        }

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
              --snapshot-in <path>             Optional prior incremental snapshot JSON
              --snapshot-out <path>            Optional path to write current incremental snapshot JSON
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
        Path outputPath,
        String snapshotIn,
        String snapshotOut
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
            String snapshotIn = null;
            String snapshotOut = null;

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
                    case "--snapshot-in" -> {
                        i = requireValue(args, i, arg);
                        snapshotIn = args[i];
                    }
                    case "--snapshot-out" -> {
                        i = requireValue(args, i, arg);
                        snapshotOut = args[i];
                    }
                    default -> throw new IllegalArgumentException("Unknown argument: " + arg);
                }
            }
            return new CliArguments(help, version, source, gitUrl, gitRef, repositoryId, workingDirectory, output, snapshotIn, snapshotOut);
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
