# architecture-browser-indexer

Deterministic architecture indexer for the Architecture Browser product.

## Current status

This repository currently contains:
- Step 1 baseline CLI shell
- Step 2 versioned architecture IR model
- Step 3 acquisition and file inventory
- Step 4 Tree-sitter parsing foundation
- Step 5 initial structural extraction for Java and TypeScript
- diagnostics and completeness metadata model
- JSON serializer/deserializer
- golden IR fixtures and regression tests

## Package root

`info.isaksson.erland.architecturebrowser.indexer`

## Build

This repository now targets Java 25 so it can use the official Java Tree-sitter runtime.

```bash
mvn test
```

## Tree-sitter setup

The Maven build now includes the official runtime dependency:

- `io.github.tree-sitter:jtreesitter:0.26.0`

The default registry attempts to create real Tree-sitter parsers for:
- Java
- TypeScript

That runtime still needs the corresponding Tree-sitter language shared libraries to be available.

The indexer now looks for bundled libraries automatically in this order:

1. explicit override via `ARCH_BROWSER_TREE_SITTER_LIB_DIR` or `-Darchbrowser.treesitter.lib.dir=...`
2. `./lib/<detected-os-arch>/`
3. `./lib/`
4. normal system library lookup

A recommended repository layout is:

```text
lib/
  macos-aarch64/
    libtree-sitter-java.dylib
    libtree-sitter-typescript.dylib
  linux-x86_64/
    libtree-sitter-java.so
    libtree-sitter-typescript.so
```

For your machine, the primary target is likely:

```text
lib/macos-aarch64/
```

If the libraries are not available, the indexer still degrades gracefully and emits
`BACKEND_UNAVAILABLE` diagnostics instead of crashing.

## CLI usage

Show help:

```bash
mvn -q exec:java -Dexec.mainClass=info.isaksson.erland.architecturebrowser.indexer.cli.IndexerCli -- --help
```

Show version:

```bash
mvn -q exec:java -Dexec.mainClass=info.isaksson.erland.architecturebrowser.indexer.cli.IndexerCli -- --version
```

Index a local repository path and write an IR payload:

```bash
mvn -q exec:java -Dexec.mainClass=info.isaksson.erland.architecturebrowser.indexer.cli.IndexerCli -- \
  --source /path/to/repository \
  --output /tmp/index-result.json
```

Acquire from Git and write an IR payload:

```bash
mvn -q exec:java -Dexec.mainClass=info.isaksson.erland.architecturebrowser.indexer.cli.IndexerCli -- \
  --git-url /path/to/local-or-remote-git-repo \
  --git-ref main \
  --output /tmp/index-result.json
```


## Tree-sitter cleanup notes

- The parser registry now takes `TreeSitterConfiguration` explicitly for deterministic tests and CLI wiring.
- Only Java and TypeScript are registered as active default Tree-sitter parsers right now.
- Other languages remain documented as future parser targets, but are not registered by default yet.


## Bundled Tree-sitter native libraries (macOS Apple Silicon)

For macOS arm64 development, build the pinned Tree-sitter runtime and grammar libraries with:

```bash
./scripts/setup-treesitter-macos-aarch64.sh
```

The script pins the grammar repos to `tree-sitter-java v0.23.5` and `tree-sitter-typescript v0.23.2` by default, and places the resulting libraries under `lib/macos-aarch64/`.


## Tree-sitter native library path during tests

The Maven Surefire configuration sets `java.library.path` to `lib/macos-aarch64` so `jtreesitter` can find `libtree-sitter.dylib` during test runs on Apple Silicon macOS. The official Java Tree-sitter docs state that the libraries can be installed in the OS-specific library search path or in `java.library.path`.
