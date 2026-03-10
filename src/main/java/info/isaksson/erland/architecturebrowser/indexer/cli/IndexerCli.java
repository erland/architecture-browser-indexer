package info.isaksson.erland.architecturebrowser.indexer.cli;

import info.isaksson.erland.architecturebrowser.indexer.ir.ArchitectureIrFactory;
import info.isaksson.erland.architecturebrowser.indexer.ir.ArchitectureIrValidator;
import info.isaksson.erland.architecturebrowser.indexer.ir.json.ArchitectureIrJson;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureIndexDocument;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RepositorySource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
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
        if (arguments.sourcePath() == null || arguments.outputPath() == null) {
            System.err.println("Missing required arguments: --source <path> and --output <path>");
            printHelp();
            System.exit(2);
            return;
        }

        Path source = arguments.sourcePath().toAbsolutePath().normalize();
        Path output = arguments.outputPath().toAbsolutePath().normalize();

        ArchitectureIndexDocument document = ArchitectureIrFactory.createPlaceholderDocument(
            RepositorySource.localPath(
                source.getFileName() != null ? source.getFileName().toString() : source.toString(),
                source.toString(),
                Instant.now()
            ),
            APPLICATION_VERSION
        );

        ArchitectureIrValidator.ValidationResult validation = ArchitectureIrValidator.validate(document);
        if (!validation.isValid()) {
            throw new IllegalStateException("Invalid IR document: " + String.join("; ", validation.messages()));
        }

        Files.createDirectories(output.getParent() == null ? Path.of(".") : output.getParent());
        ArchitectureIrJson.write(document, output);

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("status", "ok");
        summary.put("source", source.toString());
        summary.put("output", output.toString());
        summary.put("schemaVersion", document.schemaVersion());
        summary.put("entities", document.entities().size());
        summary.put("relationships", document.relationships().size());
        summary.put("diagnostics", document.diagnostics().size());
        System.out.println(ArchitectureIrJson.toPrettyJson(summary));
    }

    private static void printHelp() {
        System.out.println("""
            architecture-browser-indexer

            Usage:
              --help                     Show help
              --version                  Show version
              --source <path>            Source repository path
              --output <path>            Output JSON file
            """);
    }

    public record CliArguments(boolean showHelp, boolean showVersion, Path sourcePath, Path outputPath) {
        static CliArguments parse(String[] args) {
            boolean help = false;
            boolean version = false;
            Path source = null;
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
                    case "--output" -> {
                        i = requireValue(args, i, arg);
                        output = Path.of(args[i]);
                    }
                    default -> throw new IllegalArgumentException("Unknown argument: " + arg);
                }
            }
            return new CliArguments(help, version, source, output);
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
