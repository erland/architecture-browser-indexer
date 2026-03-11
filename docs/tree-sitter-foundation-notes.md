# Tree-sitter integration notes

The repository now includes the official Java Tree-sitter runtime dependency:

- `io.github.tree-sitter:jtreesitter:0.26.0`

## Current integration shape

The core parse abstraction from Step 4 is still preserved:

- `SourceParser`
- `ParserRegistry`
- `SourceParseRequest`
- `SourceParseResult`
- `SyntaxTree`
- `SyntaxNode`

On top of that abstraction, the default parser registry now tries to create real parsers for:

- Java
- TypeScript

using the official Java Tree-sitter runtime.

## Runtime requirements

The official runtime is present on the classpath through Maven, but language parsing also requires the
corresponding Tree-sitter language shared libraries to be available.

For the current MVP wiring, the expected shared library base names are:

- `tree-sitter-java`
- `tree-sitter-typescript`

and the expected language symbols are:

- `tree_sitter_java`
- `tree_sitter_typescript`

If these libraries are not available, the parser registry degrades cleanly and returns
`BACKEND_UNAVAILABLE` parse results with diagnostics instead of crashing.

## Library discovery

The default registry resolves shared libraries using either:

1. the normal OS / `java.library.path` lookup, or
2. an explicit directory configured through:
   - system property: `-Darchbrowser.treesitter.lib.dir=/path/to/libs`
   - environment variable: `ARCH_BROWSER_TREE_SITTER_LIB_DIR=/path/to/libs`

Tree-sitter parsing can also be disabled explicitly with:

- `-Darchbrowser.treesitter.enabled=false`
- or `ARCH_BROWSER_TREE_SITTER_ENABLED=false`

## Why the parse abstraction remains

Keeping the abstraction is still useful even after integrating the official runtime because it:

- isolates Tree-sitter-specific concerns
- allows controlled fallback diagnostics
- keeps structural extraction decoupled from the concrete parsing backend
- leaves room for future additional backends or language coverage


## Bundled native library layout

The current build supports bundled language libraries from the repository itself.

Search order:
1. explicit configured directory via `ARCH_BROWSER_TREE_SITTER_LIB_DIR` or `-Darchbrowser.treesitter.lib.dir=...`
2. `./lib/<detected-os-arch>/`
3. `./lib/`
4. normal OS library lookup

Expected examples:
- `lib/macos-aarch64/libtree-sitter-java.dylib`
- `lib/macos-aarch64/libtree-sitter-typescript.dylib`


## Native grammar build notes

The bundled macOS setup script now pins grammar revisions by default instead of cloning repository HEAD. Default pins are `tree-sitter-java v0.23.5` and `tree-sitter-typescript v0.23.2`, which makes local native library builds deterministic. The script can still be overridden with `TREE_SITTER_JAVA_REF` and `TREE_SITTER_TYPESCRIPT_REF`.


## Step 6: SQL and config grammars

The default Tree-sitter registry now also attempts to load grammars for JSON, YAML, SQL, Properties, and XML when the corresponding shared libraries are present under `lib/<platform>/`.
