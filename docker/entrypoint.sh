#!/usr/bin/env sh
set -eu

exec java   --enable-native-access=ALL-UNNAMED   -Djava.library.path="${ARCH_BROWSER_TREE_SITTER_LIB_DIR:-/app/lib/macos-aarch64}"   -jar /app/architecture-browser-indexer.jar "$@"
