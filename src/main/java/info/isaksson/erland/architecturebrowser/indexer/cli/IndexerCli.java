package info.isaksson.erland.architecturebrowser.indexer.cli;

import info.isaksson.erland.architecturebrowser.indexer.application.IndexRunRequest;
import info.isaksson.erland.architecturebrowser.indexer.application.IndexRunResult;
import info.isaksson.erland.architecturebrowser.indexer.application.IndexerApplicationService;
import info.isaksson.erland.architecturebrowser.indexer.worker.WorkerModeService;
import info.isaksson.erland.architecturebrowser.indexer.worker.http.IndexerWorkerHttpServer;

import java.io.IOException;
import info.isaksson.erland.architecturebrowser.indexer.ir.json.ArchitectureIrJson;

import java.nio.file.Files;
import java.nio.file.Path;

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
        if (arguments.serveHttp()) {
            String host = arguments.httpHost() == null || arguments.httpHost().isBlank() ? "0.0.0.0" : arguments.httpHost();
            int port = arguments.httpPort() == null ? 8080 : arguments.httpPort();
            Path workspaceDir = arguments.httpWorkspaceDir() == null
                ? Path.of("./build/http-worker")
                : arguments.httpWorkspaceDir();
            new IndexerWorkerHttpServer(workspaceDir).start(host, port);
            return;
        }
        if (arguments.workerRequestPath() != null) {
            if (arguments.workerResultPath() == null) {
                throw new IllegalArgumentException("--worker-result is required when --worker-request is used");
            }
            new WorkerModeService().runJob(arguments.workerRequestPath(), arguments.workerResultPath());
            return;
        }
        if (!arguments.hasInput() || arguments.outputPath() == null) {
            System.err.println("Missing required arguments: provide exactly one of --source <path> or --git-url <url>, and --output <path>");
            printHelp();
            System.exit(2);
            return;
        }

        IndexerApplicationService applicationService = new IndexerApplicationService();
        IndexRunResult result = applicationService.run(new IndexRunRequest(
            APPLICATION_VERSION,
            arguments.repositoryId(),
            arguments.sourcePath(),
            arguments.gitUrl(),
            arguments.gitRef(),
            arguments.workingDirectory(),
            arguments.outputPath(),
            arguments.snapshotIn(),
            arguments.snapshotOut()
        ));

        System.out.println(ArchitectureIrJson.toPrettyJson(result.summary()));

        if (result.temporaryWorkspace()) {
            deleteRecursively(result.acquiredRoot().getParent());
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
              --worker-request <path>          Run as worker using request JSON
              --worker-result <path>           Write worker result JSON
              --serve-http                     Run long-lived HTTP worker server
              --http-host <host>               HTTP bind host (default 0.0.0.0)
              --http-port <port>               HTTP bind port (default 8080)
              --http-workspace-dir <path>      Workspace for temporary HTTP job files
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
        String snapshotOut,
        Path workerRequestPath,
        Path workerResultPath,
        boolean serveHttp,
        String httpHost,
        Integer httpPort,
        Path httpWorkspaceDir
    ) {
        boolean hasInput() {
            return (sourcePath != null) ^ (gitUrl != null && !gitUrl.isBlank());
        }

        public static CliArguments parse(String[] args) {
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
            Path workerRequest = null;
            Path workerResult = null;
            boolean serveHttp = false;
            String httpHost = null;
            Integer httpPort = null;
            Path httpWorkspaceDir = null;

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
                    case "--worker-request" -> {
                        i = requireValue(args, i, arg);
                        workerRequest = Path.of(args[i]);
                    }
                    case "--worker-result" -> {
                        i = requireValue(args, i, arg);
                        workerResult = Path.of(args[i]);
                    }
                    case "--serve-http" -> serveHttp = true;
                    case "--http-host" -> {
                        i = requireValue(args, i, arg);
                        httpHost = args[i];
                    }
                    case "--http-port" -> {
                        i = requireValue(args, i, arg);
                        httpPort = Integer.parseInt(args[i]);
                    }
                    case "--http-workspace-dir" -> {
                        i = requireValue(args, i, arg);
                        httpWorkspaceDir = Path.of(args[i]);
                    }
                    default -> throw new IllegalArgumentException("Unknown argument: " + arg);
                }
            }
            return new CliArguments(help, version, source, gitUrl, gitRef, repositoryId, workingDirectory, output, snapshotIn, snapshotOut, workerRequest, workerResult, serveHttp, httpHost, httpPort, httpWorkspaceDir);
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
