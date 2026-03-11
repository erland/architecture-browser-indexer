#!/usr/bin/env bash
set -euo pipefail

# Builds bundled Tree-sitter native libraries for macOS Apple Silicon and copies them into:
#   lib/macos-aarch64/
#
# Languages built by default:
#   - Java
#   - TypeScript
#   - JSON
#   - YAML
#   - SQL
#   - Properties
#   - XML
#
# Notes:
# - Homebrew `tree-sitter` provides the shared runtime `libtree-sitter.dylib`.
# - `tree-sitter-sql` keeps generated parser files on the `gh-pages` branch.
#
# Ref overrides:
#   TREE_SITTER_JAVA_REF
#   TREE_SITTER_TYPESCRIPT_REF
#   TREE_SITTER_JSON_REF
#   TREE_SITTER_YAML_REF
#   TREE_SITTER_SQL_REF
#   TREE_SITTER_PROPERTIES_REF
#   TREE_SITTER_XML_REF

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
LIB_DIR="${ROOT_DIR}/lib/macos-aarch64"
WORK_DIR="${TREE_SITTER_ROOT:-${ROOT_DIR}/.tmp/tree-sitter-build}"

JAVA_REF="${TREE_SITTER_JAVA_REF:-v0.23.5}"
TYPESCRIPT_REF="${TREE_SITTER_TYPESCRIPT_REF:-v0.23.2}"
JSON_REF="${TREE_SITTER_JSON_REF:-v0.24.8}"
YAML_REF="${TREE_SITTER_YAML_REF:-master}"
SQL_REF="${TREE_SITTER_SQL_REF:-gh-pages}"
PROPERTIES_REF="${TREE_SITTER_PROPERTIES_REF:-master}"
XML_REF="${TREE_SITTER_XML_REF:-master}"

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

build_grammar() {
  local dir_name="$1"
  local src_subdir="$2"
  local output_name="$3"

  local repo_dir="${WORK_DIR}/${dir_name}"
  local src_dir="${repo_dir}/${src_subdir}"
  local parser_c="${src_dir}/parser.c"
  local scanner_c="${src_dir}/scanner.c"
  local scanner_cc="${src_dir}/scanner.cc"
  local out="${LIB_DIR}/${output_name}"

  if [[ ! -f "${parser_c}" ]]; then
    echo "Expected parser source not found: ${parser_c}" >&2
    exit 1
  fi

  local compiler="clang"
  local -a sources=("${parser_c}")
  if [[ -f "${scanner_cc}" ]]; then
    compiler="clang++"
    sources+=("${scanner_cc}")
  elif [[ -f "${scanner_c}" ]]; then
    sources+=("${scanner_c}")
  fi

  echo "==> Building ${output_name} from ${dir_name}/${src_subdir}"
  "${compiler}" \
    -dynamiclib \
    -O2 \
    -fPIC \
    -I "${src_dir}" \
    "${sources[@]}" \
    -o "${out}"

  echo "Built ${out}"
}

clone_or_update "https://github.com/tree-sitter/tree-sitter-java.git" "tree-sitter-java" "${JAVA_REF}"
clone_or_update "https://github.com/tree-sitter/tree-sitter-typescript.git" "tree-sitter-typescript" "${TYPESCRIPT_REF}"
clone_or_update "https://github.com/tree-sitter/tree-sitter-json.git" "tree-sitter-json" "${JSON_REF}"
clone_or_update "https://github.com/tree-sitter-grammars/tree-sitter-yaml.git" "tree-sitter-yaml" "${YAML_REF}"
clone_or_update "https://github.com/DerekStride/tree-sitter-sql.git" "tree-sitter-sql" "${SQL_REF}"
clone_or_update "https://github.com/tree-sitter-grammars/tree-sitter-properties.git" "tree-sitter-properties" "${PROPERTIES_REF}"
clone_or_update "https://github.com/tree-sitter-grammars/tree-sitter-xml.git" "tree-sitter-xml" "${XML_REF}"

build_grammar "tree-sitter-java" "src" "libtree-sitter-java.dylib"
build_grammar "tree-sitter-typescript" "typescript/src" "libtree-sitter-typescript.dylib"
build_grammar "tree-sitter-json" "src" "libtree-sitter-json.dylib"
build_grammar "tree-sitter-yaml" "src" "libtree-sitter-yaml.dylib"
build_grammar "tree-sitter-sql" "src" "libtree-sitter-sql.dylib"
build_grammar "tree-sitter-properties" "src" "libtree-sitter-properties.dylib"
build_grammar "tree-sitter-xml" "xml/src" "libtree-sitter-xml.dylib"

echo
echo "Done. Files available in:"
echo "  ${LIB_DIR}"
echo
ls -lah "${LIB_DIR}"
