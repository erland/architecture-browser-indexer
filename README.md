# architecture-browser-indexer

Deterministic architecture indexer for the Architecture Browser product.

## Current status

This repository currently contains:
- Step 1 baseline CLI shell
- Step 2 versioned architecture IR model
- Step 3 acquisition and file inventory
- Step 4 Tree-sitter parsing foundation (registry, parse result abstraction, graceful diagnostics)
- diagnostics and completeness metadata model
- JSON serializer/deserializer
- golden IR fixtures and regression tests

## Package root

`info.isaksson.erland.architecturebrowser.indexer`

## Build

```bash
mvn test
```

## CLI usage

Show help:

```bash
mvn -q exec:java -Dexec.mainClass=info.isaksson.erland.architecturebrowser.indexer.cli.IndexerCli -- --help
```

Show version:

```bash
mvn -q exec:java -Dexec.mainClass=info.isaksson.erland.architecturebrowser.indexer.cli.IndexerCli -- --version
```

Index a local repository path and write an inventory IR payload:

```bash
mvn -q exec:java -Dexec.mainClass=info.isaksson.erland.architecturebrowser.indexer.cli.IndexerCli -- \
  --source /path/to/repository \
  --output /tmp/index-result.json
```

Acquire from Git and write an inventory IR payload:

```bash
mvn -q exec:java -Dexec.mainClass=info.isaksson.erland.architecturebrowser.indexer.cli.IndexerCli -- \
  --git-url /path/to/local-or-remote-git-repo \
  --git-ref main \
  --output /tmp/index-result.json
```

At this stage the CLI performs acquisition, deterministic file inventory, and a Tree-sitter parsing pass through a Java-17-safe abstraction layer. The default registry reports backend-unavailable diagnostics unless a compatible Tree-sitter runtime is added later. Structural extraction starts in Step 5.
