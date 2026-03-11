#!/usr/bin/env bash
set -euo pipefail

# Builds bundled Tree-sitter native libraries for macOS Apple Silicon and copies them into:
#   lib/macos-aarch64/
#
# Default pinned grammar revisions:
#   tree-sitter-java:        v0.23.5
#   tree-sitter-typescript:  v0.23.2
#
# These can be overridden via:
#   TREE_SITTER_JAVA_REF=...
#   TREE_SITTER_TYPESCRIPT_REF=...
#
# Requirements:
#   - macOS arm64
#   - Apple clang / Xcode Command Line Tools
#   - Homebrew
#   - git
#
# Usage:
#   ./scripts/setup-treesitter-macos-aarch64.sh

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
LIB_DIR="${ROOT_DIR}/lib/macos-aarch64"
WORK_DIR="${TREE_SITTER_ROOT:-${ROOT_DIR}/.tmp/tree-sitter-build}"
JAVA_REF="${TREE_SITTER_JAVA_REF:-v0.23.5}"
TYPESCRIPT_REF="${TREE_SITTER_TYPESCRIPT_REF:-v0.23.2}"

mkdir -p "${LIB_DIR}"
mkdir -p "${WORK_DIR}"

echo "==> Checking platform"
if [[ "$(uname -s)" != "Darwin" ]]; then
  echo "This script currently targets macOS only." >&2
  exit 1
fi

ARCH="$(uname -m)"
if [[ "${ARCH}" != "arm64" ]]; then
  echo "This script is intended for Apple Silicon macOS (arm64)." >&2
  echo "Detected architecture: ${ARCH}" >&2
  exit 1
fi

echo "==> Ensuring Homebrew tree-sitter runtime is installed"
if ! command -v brew >/dev/null 2>&1; then
  echo "Homebrew is required but was not found." >&2
  exit 1
fi

brew list tree-sitter >/dev/null 2>&1 || brew install tree-sitter

BREW_PREFIX="$(brew --prefix)"
RUNTIME_LIB="${BREW_PREFIX}/lib/libtree-sitter.dylib"
if [[ ! -f "${RUNTIME_LIB}" ]]; then
  echo "Could not find ${RUNTIME_LIB}" >&2
  exit 1
fi

echo "==> Copying Tree-sitter runtime library"
cp -f "${RUNTIME_LIB}" "${LIB_DIR}/libtree-sitter.dylib"

clone_or_update() {
  local repo_url="$1"
  local dir_name="$2"
  local ref="$3"

  if [[ -d "${WORK_DIR}/${dir_name}/.git" ]]; then
    echo "==> Updating ${dir_name}"
    git -C "${WORK_DIR}/${dir_name}" fetch --tags --force origin
  else
    echo "==> Cloning ${dir_name}"
    git clone --filter=blob:none "${repo_url}" "${WORK_DIR}/${dir_name}"
  fi

  git -C "${WORK_DIR}/${dir_name}" checkout -f "${ref}"
  git -C "${WORK_DIR}/${dir_name}" reset --hard "${ref}"
}

build_java_grammar() {
  local repo_dir="${WORK_DIR}/tree-sitter-java"
  local out="${LIB_DIR}/libtree-sitter-java.dylib"

  echo "==> Building Java grammar from ${JAVA_REF}"
  clang     -dynamiclib     -O2     -fPIC     -I "${repo_dir}/src"     "${repo_dir}/src/parser.c"     -o "${out}"

  echo "Built ${out}"
}

build_typescript_grammar() {
  local repo_dir="${WORK_DIR}/tree-sitter-typescript"
  local out="${LIB_DIR}/libtree-sitter-typescript.dylib"

  echo "==> Building TypeScript grammar from ${TYPESCRIPT_REF}"
  clang     -dynamiclib     -O2     -fPIC     -I "${repo_dir}/typescript/src"     "${repo_dir}/typescript/src/parser.c"     "${repo_dir}/typescript/src/scanner.c"     -o "${out}"

  echo "Built ${out}"
}

clone_or_update "https://github.com/tree-sitter/tree-sitter-java.git" "tree-sitter-java" "${JAVA_REF}"
clone_or_update "https://github.com/tree-sitter/tree-sitter-typescript.git" "tree-sitter-typescript" "${TYPESCRIPT_REF}"

build_java_grammar
build_typescript_grammar

echo
echo "Done. Files available in:"
echo "  ${LIB_DIR}"
echo
ls -lah "${LIB_DIR}"
