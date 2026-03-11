package info.isaksson.erland.architecturebrowser.indexer.cli;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CliArgumentsWorkerModeTest {

    @Test
    void parsesWorkerArguments() {
        IndexerCli.CliArguments arguments = IndexerCli.CliArguments.parse(new String[] {
            "--worker-request", "/jobs/request.json",
            "--worker-result", "/jobs/result.json"
        });

        assertEquals(Path.of("/jobs/request.json"), arguments.workerRequestPath());
        assertEquals(Path.of("/jobs/result.json"), arguments.workerResultPath());
    }

    @Test
    void parsesSnapshotArguments() {
        IndexerCli.CliArguments arguments = IndexerCli.CliArguments.parse(new String[] {
            "--source", "/workspace",
            "--output", "/workspace/out.json",
            "--snapshot-in", "/workspace/incremental-prev.json",
            "--snapshot-out", "/workspace/incremental-next.json"
        });

        assertEquals("/workspace/incremental-prev.json", arguments.snapshotIn());
        assertEquals("/workspace/incremental-next.json", arguments.snapshotOut());
    }
}
