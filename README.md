# architecture-browser-indexer

Deterministic architecture indexer for the Architecture Browser MVP.

## Step 1 scope

This repository baseline provides:
- Maven build and test setup
- Java package baseline rooted at `info.isaksson.erland`
- CLI shell with help and version support
- placeholder package structure for later pipeline steps
- initial README and copied planning documents under `docs/`

This step does **not** implement indexing yet. The CLI currently validates and echoes invocation parameters so later steps can add the real acquisition and indexing pipeline behind a stable entry point.

## Requirements

- Java 21+
- Maven 3.9+

## Build

```bash
mvn clean test
```

## Run help

```bash
mvn -q -DskipTests exec:java -Dexec.mainClass=info.isaksson.erland.architecturebrowser.indexer.cli.ArchitectureBrowserIndexerCli -- --help
```

## Run version

```bash
mvn -q -DskipTests exec:java -Dexec.mainClass=info.isaksson.erland.architecturebrowser.indexer.cli.ArchitectureBrowserIndexerCli -- --version
```

## Run placeholder index command

```bash
mvn -q -DskipTests exec:java -Dexec.mainClass=info.isaksson.erland.architecturebrowser.indexer.cli.ArchitectureBrowserIndexerCli -- \
  --source /path/to/repository \
  --output /tmp/index-result.json
```

## Current CLI behavior

The current command:
- accepts `--source` and `--output`
- resolves paths to absolute normalized paths
- validates that the source path exists
- prints a placeholder run summary
- exits successfully without producing an IR yet

## Planned next step

Step 2 should define the versioned architecture IR, diagnostics model, and serialization contract so the CLI can emit a real placeholder payload instead of only console output.
