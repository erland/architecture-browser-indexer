# architecture-browser-indexer

Deterministic architecture indexer for the Architecture Browser product.

## Current status

This repository currently contains:
- Step 1 baseline CLI shell
- Step 2 versioned architecture IR model
- diagnostics and completeness metadata model
- JSON serializer/deserializer
- golden IR fixtures and round-trip tests

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

Write a placeholder IR payload:

```bash
mvn -q exec:java -Dexec.mainClass=info.isaksson.erland.architecturebrowser.indexer.cli.IndexerCli --   --source /path/to/repository   --output /tmp/index-result.json
```

At this stage the CLI still emits a placeholder payload built from the Step 2 IR contract. Real acquisition and scanning start in Step 3.
