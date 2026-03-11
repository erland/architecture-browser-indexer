package info.isaksson.erland.architecturebrowser.indexer.worker.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import info.isaksson.erland.architecturebrowser.indexer.cli.IndexerCli;
import info.isaksson.erland.architecturebrowser.indexer.worker.model.WorkerJobRequest;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executors;

public final class IndexerWorkerHttpServer {
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
        System.out.println("Indexer worker HTTP server listening on http://" + host + ":" + port);
    }

    private void handleRun(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJson(exchange, 405, new HttpWorkerErrorResponse(
                "method_not_allowed",
                "Only POST is supported for /api/index-jobs/run",
                Instant.now(),
                null,
                Map.of("allowedMethod", "POST")
            ));
            return;
        }
        try {
            WorkerJobRequest request = HttpWorkerJson.readWorkerRequest(exchange.getRequestBody());
            HttpWorkerRunResponse response = workerService.runJob(request);
            sendJson(exchange, 200, response);
        } catch (IllegalArgumentException ex) {
            sendJson(exchange, 400, new HttpWorkerErrorResponse(
                "invalid_request",
                ex.getMessage(),
                Instant.now(),
                null,
                Map.of()
            ));
        } catch (Exception ex) {
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("exceptionType", ex.getClass().getName());
            details.put("message", ex.getMessage());
            sendJson(exchange, 500, new HttpWorkerErrorResponse(
                "job_failed",
                "Indexer job failed",
                Instant.now(),
                null,
                details
            ));
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

    public static void main(String[] args) throws Exception {
        IndexerCli.CliArguments cliArguments = IndexerCli.CliArguments.parse(args);
        String host = cliArguments.httpHost() == null || cliArguments.httpHost().isBlank() ? "0.0.0.0" : cliArguments.httpHost();
        int port = cliArguments.httpPort() == null ? 8080 : cliArguments.httpPort();
        Path workspaceDir = cliArguments.httpWorkspaceDir() == null
            ? Path.of("./build/http-worker")
            : cliArguments.httpWorkspaceDir();
        new IndexerWorkerHttpServer(workspaceDir).start(host, port);
    }
}
