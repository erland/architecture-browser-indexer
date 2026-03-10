package info.isaksson.erland.architecturebrowser.indexer.cli;

import info.isaksson.erland.architecturebrowser.indexer.ArchitectureBrowserIndexerVersion;

import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public final class ArchitectureBrowserIndexerCli {

    private final PrintStream out;
    private final PrintStream err;

    public ArchitectureBrowserIndexerCli(PrintStream out, PrintStream err) {
        this.out = Objects.requireNonNull(out, "out");
        this.err = Objects.requireNonNull(err, "err");
    }

    public static void main(String[] args) {
        ArchitectureBrowserIndexerCli cli = new ArchitectureBrowserIndexerCli(System.out, System.err);
        int exitCode = cli.run(args);
        System.exit(exitCode);
    }

    public int run(String[] args) {
        CliArguments parsed;
        try {
            parsed = CliArguments.parse(args);
        } catch (IllegalArgumentException ex) {
            err.println(ex.getMessage());
            err.println();
            printHelp(err);
            return 2;
        }

        if (parsed.showHelp()) {
            printHelp(out);
            return 0;
        }
        if (parsed.showVersion()) {
            out.println(ArchitectureBrowserIndexerVersion.VERSION);
            return 0;
        }
        if (parsed.source() == null && parsed.output() == null) {
            out.println("Architecture Browser Indexer CLI shell");
            out.println("Version: " + ArchitectureBrowserIndexerVersion.VERSION);
            out.println("No source/output supplied yet. Use --help for CLI options.");
            return 0;
        }
        if (parsed.source() == null || parsed.output() == null) {
            err.println("Both --source and --output must be provided together.");
            return 2;
        }

        Path normalizedSource = parsed.source().toAbsolutePath().normalize();
        Path normalizedOutput = parsed.output().toAbsolutePath().normalize();
        if (!Files.exists(normalizedSource)) {
            err.println("Source path does not exist: " + normalizedSource);
            return 2;
        }

        out.println("Architecture Browser Indexer CLI shell");
        out.println("Version: " + ArchitectureBrowserIndexerVersion.VERSION);
        out.println("Source: " + normalizedSource);
        out.println("Output: " + normalizedOutput);
        out.println("Status: baseline-only shell; indexing pipeline not implemented yet.");
        return 0;
    }

    private static void printHelp(PrintStream stream) {
        stream.println("Usage: architecture-browser-indexer [--help] [--version] [--source <path> --output <path>]");
        stream.println();
        stream.println("Options:");
        stream.println("  --help            Show this help text.");
        stream.println("  --version         Show the indexer version.");
        stream.println("  --source <path>   Local source path to index.");
        stream.println("  --output <path>   Target output file for emitted IR.");
    }
}
