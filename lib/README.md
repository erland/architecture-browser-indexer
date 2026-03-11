# Bundled Tree-sitter native libraries

Place prebuilt Tree-sitter grammar shared libraries here to make the indexer work out of the box.

Expected layout:

- `lib/macos-aarch64/`
- `lib/macos-x86_64/`
- `lib/linux-x86_64/`
- `lib/windows-x86_64/`

Expected filenames are based on `System.mapLibraryName(...)` for these base names:

- `tree-sitter-java`
- `tree-sitter-typescript`

Examples:
- macOS: `libtree-sitter-java.dylib`
- Linux: `libtree-sitter-java.so`
- Windows: `tree-sitter-java.dll`

Search order:
1. `-Darchbrowser.treesitter.lib.dir=...` or `ARCH_BROWSER_TREE_SITTER_LIB_DIR`
2. `./lib/<detected-os-arch>/`
3. `./lib/`
4. system library lookup

Additional Step 6 libraries expected for SQL/config extraction:
- libtree-sitter-json.dylib
- libtree-sitter-yaml.dylib
- libtree-sitter-sql.dylib
- libtree-sitter-properties.dylib
- libtree-sitter-xml.dylib
