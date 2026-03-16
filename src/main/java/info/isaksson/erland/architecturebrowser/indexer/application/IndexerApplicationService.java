package info.isaksson.erland.architecturebrowser.indexer.application;

import info.isaksson.erland.architecturebrowser.indexer.acquisition.AcquisitionRequest;
import info.isaksson.erland.architecturebrowser.indexer.acquisition.AcquisitionResult;
import info.isaksson.erland.architecturebrowser.indexer.acquisition.SourceAcquisitionService;
import info.isaksson.erland.architecturebrowser.indexer.extract.StructuralExtractionService;
import info.isaksson.erland.architecturebrowser.indexer.extract.StructuralExtractorRegistry;
import info.isaksson.erland.architecturebrowser.indexer.extract.model.StructuralExtractionResult;
import info.isaksson.erland.architecturebrowser.indexer.incremental.FileFingerprintService;
import info.isaksson.erland.architecturebrowser.indexer.incremental.IncrementalDiffService;
import info.isaksson.erland.architecturebrowser.indexer.incremental.IncrementalPlanner;
import info.isaksson.erland.architecturebrowser.indexer.incremental.IncrementalSnapshotJson;
import info.isaksson.erland.architecturebrowser.indexer.incremental.model.IncrementalDiff;
import info.isaksson.erland.architecturebrowser.indexer.incremental.model.IncrementalPlan;
import info.isaksson.erland.architecturebrowser.indexer.incremental.model.IncrementalSnapshot;
import info.isaksson.erland.architecturebrowser.indexer.interpret.InterpretationRegistry;
import info.isaksson.erland.architecturebrowser.indexer.interpret.InterpretationService;
import info.isaksson.erland.architecturebrowser.indexer.interpret.model.InterpretationResult;
import info.isaksson.erland.architecturebrowser.indexer.ir.ArchitectureIrFactory;
import info.isaksson.erland.architecturebrowser.indexer.ir.ArchitectureIrValidator;
import info.isaksson.erland.architecturebrowser.indexer.ir.json.ArchitectureIrJson;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureIndexDocument;
import info.isaksson.erland.architecturebrowser.indexer.parse.ParseBatchResult;
import info.isaksson.erland.architecturebrowser.indexer.parse.TreeSitterConfiguration;
import info.isaksson.erland.architecturebrowser.indexer.parse.TreeSitterParserRegistryFactory;
import info.isaksson.erland.architecturebrowser.indexer.parse.TreeSitterParsingService;
import info.isaksson.erland.architecturebrowser.indexer.parse.TreeSitterRuntimeDetector;
import info.isaksson.erland.architecturebrowser.indexer.publish.ExportBundleWriter;
import info.isaksson.erland.architecturebrowser.indexer.publish.model.ExportBundle;
import info.isaksson.erland.architecturebrowser.indexer.scan.FileInventory;
import info.isaksson.erland.architecturebrowser.indexer.scan.FileInventoryScanner;
import info.isaksson.erland.architecturebrowser.indexer.topology.TopologyService;
import info.isaksson.erland.architecturebrowser.indexer.topology.model.TopologyResult;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public class IndexerApplicationService {
    private final SourceAcquisitionService acquisitionService;
    private final FileInventoryScanner scanner;
    private final FileFingerprintService fingerprintService;
    private final IncrementalDiffService incrementalDiffService;
    private final IncrementalPlanner incrementalPlanner;
    private final StructuralExtractionService extractionService;
    private final InterpretationService interpretationService;
    private final TopologyService topologyService;
    private final ExportBundleWriter exportBundleWriter;

    public IndexerApplicationService() {
        this(
            new SourceAcquisitionService(),
            new FileInventoryScanner(),
            new FileFingerprintService(),
            new IncrementalDiffService(),
            new IncrementalPlanner(),
            new StructuralExtractionService(StructuralExtractorRegistry.defaultRegistry()),
            new InterpretationService(InterpretationRegistry.defaultRegistry()),
            new TopologyService(),
            new ExportBundleWriter()
        );
    }

    IndexerApplicationService(
        SourceAcquisitionService acquisitionService,
        FileInventoryScanner scanner,
        FileFingerprintService fingerprintService,
        IncrementalDiffService incrementalDiffService,
        IncrementalPlanner incrementalPlanner,
        StructuralExtractionService extractionService,
        InterpretationService interpretationService,
        TopologyService topologyService,
        ExportBundleWriter exportBundleWriter
    ) {
        this.acquisitionService = acquisitionService;
        this.scanner = scanner;
        this.fingerprintService = fingerprintService;
        this.incrementalDiffService = incrementalDiffService;
        this.incrementalPlanner = incrementalPlanner;
        this.extractionService = extractionService;
        this.interpretationService = interpretationService;
        this.topologyService = topologyService;
        this.exportBundleWriter = exportBundleWriter;
    }

    public IndexRunResult run(IndexRunRequest request) throws Exception {
        TreeSitterConfiguration treeSitterConfiguration = TreeSitterConfiguration.fromEnvironment();
        var treeSitterRuntimeStatus = TreeSitterRuntimeDetector.detect(treeSitterConfiguration);
        TreeSitterParsingService parsingService = new TreeSitterParsingService(
            TreeSitterParserRegistryFactory.createDefaultRegistry(treeSitterConfiguration));

        AcquisitionRequest acquisitionRequest = new AcquisitionRequest(
            request.repositoryId(),
            request.sourcePath(),
            request.gitUrl(),
            request.gitRef(),
            request.workingDirectory()
        );
        AcquisitionResult acquisitionResult = acquisitionService.acquire(acquisitionRequest);
        FileInventory inventory = scanner.scan(acquisitionResult.acquiredRoot());
        IncrementalSnapshot currentSnapshot = fingerprintService.createSnapshot(inventory);
        IncrementalSnapshot previousSnapshot = readPreviousSnapshot(request.snapshotIn());
        IncrementalDiff incrementalDiff = incrementalDiffService.diff(previousSnapshot, currentSnapshot);
        IncrementalPlan incrementalPlan = incrementalPlanner.plan(incrementalDiff);
        ParseBatchResult parseBatchResult = parsingService.parseInventory(acquisitionResult.acquiredRoot(), inventory);
        StructuralExtractionResult extractionResult = extractionService.extract(parseBatchResult);
        InterpretationResult interpretationResult = interpretationService.interpret(extractionResult);
        TopologyResult topologyResult = topologyService.infer(inventory, extractionResult, interpretationResult);

        ArchitectureIndexDocument document = ArchitectureIrFactory.createInventoryDocument(
            acquisitionResult.repositorySource(),
            request.applicationVersion(),
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

        Path output = request.outputPath().toAbsolutePath().normalize();
        Files.createDirectories(output.getParent() == null ? Path.of(".") : output.getParent());
        ArchitectureIrJson.write(document, output);

        ExportBundle exportBundle = exportBundleWriter.createBundle(document, request.applicationVersion(), output.getFileName().toString());
        exportBundleWriter.writeBundle(output, exportBundle);

        if (request.snapshotOut() != null && !request.snapshotOut().isBlank()) {
            IncrementalSnapshotJson.write(Path.of(request.snapshotOut()), currentSnapshot);
        }

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

        return new IndexRunResult(
            document,
            acquisitionResult.acquiredRoot(),
            output,
            summary,
            acquisitionResult.temporaryWorkspace()
        );
    }

    private static IncrementalSnapshot readPreviousSnapshot(String snapshotIn) {
        if (snapshotIn == null || snapshotIn.isBlank()) {
            return null;
        }
        try {
            return IncrementalSnapshotJson.read(Path.of(snapshotIn));
        } catch (Exception ex) {
            System.err.println("Warning: failed to read incremental snapshot from " + snapshotIn + ": " + ex.getMessage());
            return null;
        }
    }
}
