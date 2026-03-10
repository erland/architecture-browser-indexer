package info.isaksson.erland.architecturebrowser.indexer.cli;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArchitectureBrowserIndexerCliTest {

    @Test
    void versionOptionReturnsZero() {
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();

        ArchitectureBrowserIndexerCli cli = new ArchitectureBrowserIndexerCli(new PrintStream(stdout), new PrintStream(stderr));
        int exitCode = cli.run(new String[]{"--version"});

        assertEquals(0, exitCode);
        assertTrue(stdout.toString().contains("0.1.0-SNAPSHOT"));
    }

    @Test
    void helpOptionReturnsZero() {
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();

        ArchitectureBrowserIndexerCli cli = new ArchitectureBrowserIndexerCli(new PrintStream(stdout), new PrintStream(stderr));
        int exitCode = cli.run(new String[]{"--help"});

        assertEquals(0, exitCode);
        assertTrue(stdout.toString().contains("Usage: architecture-browser-indexer"));
    }

    @Test
    void baselineInvocationAcceptsSourceAndOutput() throws Exception {
        Path sourceDir = Files.createTempDirectory("architecture-browser-indexer-source");
        Path outputFile = sourceDir.resolve("result.json");

        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        ArchitectureBrowserIndexerCli cli = new ArchitectureBrowserIndexerCli(new PrintStream(stdout), new PrintStream(stderr));

        int exitCode = cli.run(new String[]{
                "--source", sourceDir.toString(),
                "--output", outputFile.toString()
        });

        assertEquals(0, exitCode);
        String text = stdout.toString();
        assertTrue(text.contains("Status: baseline-only shell"));
        assertTrue(text.contains(sourceDir.toAbsolutePath().normalize().toString()));
        assertTrue(text.contains(outputFile.toAbsolutePath().normalize().toString()));
    }
}
