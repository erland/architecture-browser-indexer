package info.isaksson.erland.architecturebrowser.indexer.worker.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import info.isaksson.erland.architecturebrowser.indexer.cli.IndexerCli;
import info.isaksson.erland.architecturebrowser.indexer.worker.model.WorkerJobRequest;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class IndexerWorkerHttpServer {
    private static final Logger LOG = Logger.getLogger(IndexerWorkerHttpServer.class.getName());

    private final HttpWorkerService workerService;

    public IndexerWorkerHttpServer(Path workspaceDirectory) {
        this.workerService = new HttpWorkerService(workspaceDirectory);
    }

    public void start(String host, int port) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(host, port), 0);
        server.createContext("/health", exchange -> {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJson(exchange, 405, new HttpWorkerErrorResponse(
                    "method_not_allowed",
                    "Only GET is supported for /health",
                    Instant.now(),
                    null,
                    Map.of("allowedMethod", "GET")
                ));
                return;
            }
            sendJson(exchange, 200, Map.of(
                "status", "UP",
                "service", "architecture-browser-indexer-worker",
                "version", IndexerCli.APPLICATION_VERSION,
                "generatedAt", Instant.now()
            ));
        });
        server.createContext("/api/index-jobs/run", this::handleRun);
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
        LOG.info(() -> "Indexer worker HTTP server listening on http://" + host + ":" + port);
    }

    private void handleRun(HttpExchange exchange) throws IOException {
        Instant startedAt = Instant.now();
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            LOG.warning(() -> "Rejected request to /api/index-jobs/run with method=" + exchange.getRequestMethod());
            sendJson(exchange, 405, new HttpWorkerErrorResponse(
                "method_not_allowed",
                "Only POST is supported for /api/index-jobs/run",
                Instant.now(),
                null,
                Map.of("allowedMethod", "POST")
            ));
            return;
        }
        WorkerJobRequest request = null;
        try {
            request = HttpWorkerJson.readWorkerRequest(exchange.getRequestBody());
            final WorkerJobRequest requestForLog = request;
            LOG.info(() -> "Received index job request: repositoryId=" + safe(requestForLog.repositoryId())
                + ", gitUrl=" + safe(requestForLog.gitUrl())
                + ", gitRef=" + safe(requestForLog.gitRef())
                + ", sourcePath=" + safe(requestForLog.sourcePath())
                + ", outputPath=" + safe(requestForLog.outputPath())
                + ", snapshotIn=" + safe(requestForLog.snapshotIn())
                + ", snapshotOut=" + safe(requestForLog.snapshotOut()));
            HttpWorkerRunResponse response = workerService.runJob(request);
            Duration elapsed = Duration.between(startedAt, Instant.now());
            LOG.info(() -> "Completed index job request: jobId=" + safe(response.jobId())
                + ", status=" + safe(response.status())
                + ", outputPath=" + safe(response.outputPath())
                + ", elapsedMs=" + elapsed.toMillis());
            sendJson(exchange, 200, response);
        } catch (IllegalArgumentException ex) {
            LOG.log(Level.WARNING, "Invalid index job request", ex);
            sendJson(exchange, 400, new HttpWorkerErrorResponse(
                "invalid_request",
                ex.getMessage(),
                Instant.now(),
                request == null ? null : request.jobId(),
                errorDetails(ex, request, startedAt)
            ));
        } catch (Throwable ex) {
            final WorkerJobRequest requestForLog = request;
            LOG.log(Level.SEVERE,
                "Indexer worker job failed for repositoryId=" + safe(requestForLog == null ? null : requestForLog.repositoryId())
                    + ", gitUrl=" + safe(requestForLog == null ? null : requestForLog.gitUrl())
                    + ", gitRef=" + safe(requestForLog == null ? null : requestForLog.gitRef())
                    + ", sourcePath=" + safe(requestForLog == null ? null : requestForLog.sourcePath()),
                ex);
            try {
                sendJson(exchange, 500, new HttpWorkerErrorResponse(
                    "job_failed",
                    "Indexer job failed",
                    Instant.now(),
                    request == null ? null : request.jobId(),
                    errorDetails(ex, request, startedAt)
                ));
            } catch (Throwable sendFailure) {
                LOG.log(Level.SEVERE, "Failed to write error response after worker failure", sendFailure);
                if (sendFailure instanceof IOException io) {
                    throw io;
                }
                throw new IOException("Failed to write error response", sendFailure);
            }
        }
    }

    private static void sendJson(HttpExchange exchange, int status, Object body) throws IOException {
        byte[] bytes = HttpWorkerJson.writeBytes(body);
        exchange.getResponseHeaders().set("Content-Type", "application/json;charset=UTF-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(bytes);
        }
    }

    private static Map<String, Object> errorDetails(Throwable throwable, WorkerJobRequest request, Instant startedAt) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("exceptionType", throwable.getClass().getName());
        details.put("message", throwable.getMessage());
        Throwable rootCause = rootCauseOf(throwable);
        if (rootCause != throwable) {
            details.put("rootCauseType", rootCause.getClass().getName());
            details.put("rootCauseMessage", rootCause.getMessage());
        }
        if (request != null) {
            details.put("repositoryId", request.repositoryId());
            details.put("gitUrl", request.gitUrl());
            details.put("gitRef", request.gitRef());
            details.put("sourcePath", request.sourcePath());
            details.put("outputPath", request.outputPath());
            details.put("snapshotIn", request.snapshotIn());
            details.put("snapshotOut", request.snapshotOut());
        }
        details.put("elapsedMs", Duration.between(startedAt, Instant.now()).toMillis());
        return details;
    }

    private static Throwable rootCauseOf(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current;
    }

    private static String safe(String value) {
        return value == null || value.isBlank() ? "<none>" : value;
    }

    public static void main(String[] args) throws Exception {
        IndexerCli.CliArguments cliArguments = IndexerCli.CliArguments.parse(args);
        String host = cliArguments.httpHost() == null || cliArguments.httpHost().isBlank() ? "0.0.0.0" : cliArguments.httpHost();
        int port = cliArguments.httpPort() == null ? 8080 : cliArguments.httpPort();
        Path workspaceDir = cliArguments.httpWorkspaceDir() == null
            ? Path.of("./build/http-worker")
            : cliArguments.httpWorkspaceDir();
        LOG.info(() -> "Starting indexer worker with workspaceDir=" + workspaceDir.toAbsolutePath().normalize()
            + ", treeSitterLibDir=" + System.getenv().getOrDefault("ARCH_BROWSER_TREE_SITTER_LIB_DIR", "<not-set>")
            + ", os.name=" + System.getProperty("os.name")
            + ", os.arch=" + System.getProperty("os.arch")
            + ", java.version=" + System.getProperty("java.version"));
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) ->
            LOG.log(Level.SEVERE, "Uncaught exception on thread " + thread.getName(), throwable));
        new IndexerWorkerHttpServer(workspaceDir).start(host, port);
    }
}
