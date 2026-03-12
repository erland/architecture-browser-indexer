#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
LIB_DIR="${ROOT_DIR}/lib/linux-aarch64"
WORK_DIR="${TREE_SITTER_ROOT:-${ROOT_DIR}/.tmp/tree-sitter-build-linux-aarch64}"

TREE_SITTER_REF="${TREE_SITTER_REF:-v0.25.3}"
JAVA_REF="${TREE_SITTER_JAVA_REF:-v0.23.5}"
TYPESCRIPT_REF="${TREE_SITTER_TYPESCRIPT_REF:-v0.23.2}"
JSON_REF="${TREE_SITTER_JSON_REF:-v0.24.8}"
YAML_REF="${TREE_SITTER_YAML_REF:-master}"
SQL_REF="${TREE_SITTER_SQL_REF:-gh-pages}"
PROPERTIES_REF="${TREE_SITTER_PROPERTIES_REF:-master}"
XML_REF="${TREE_SITTER_XML_REF:-master}"

mkdir -p "${LIB_DIR}"
mkdir -p "${WORK_DIR}"

docker run --rm \
  --platform linux/arm64 \
  -v "${ROOT_DIR}:/workspace" \
  -w /workspace \
  ubuntu:24.04 \
  bash -lc '
    set -euo pipefail

    export DEBIAN_FRONTEND=noninteractive
    apt-get update
    apt-get install -y \
      build-essential \
      git \
      ca-certificates \
      curl

    ROOT_DIR=/workspace
    LIB_DIR="${ROOT_DIR}/lib/linux-aarch64"
    WORK_DIR="${ROOT_DIR}/.tmp/tree-sitter-build-linux-aarch64"

    TREE_SITTER_REF="'"${TREE_SITTER_REF}"'"
    JAVA_REF="'"${JAVA_REF}"'"
    TYPESCRIPT_REF="'"${TYPESCRIPT_REF}"'"
    JSON_REF="'"${JSON_REF}"'"
    YAML_REF="'"${YAML_REF}"'"
    SQL_REF="'"${SQL_REF}"'"
    PROPERTIES_REF="'"${PROPERTIES_REF}"'"
    XML_REF="'"${XML_REF}"'"

    mkdir -p "${LIB_DIR}" "${WORK_DIR}"

    clone_or_update() {
      local repo_url="$1"
      local dir_name="$2"
      local ref="$3"

      if [ -d "${WORK_DIR}/${dir_name}/.git" ]; then
        git -C "${WORK_DIR}/${dir_name}" fetch --tags --force origin
      else
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

      if [ ! -f "${parser_c}" ]; then
        echo "Expected parser source not found: ${parser_c}" >&2
        exit 1
      fi

      local compiler="gcc"
      local sources=("${parser_c}")

      if [ -f "${scanner_cc}" ]; then
        compiler="g++"
        sources+=("${scanner_cc}")
      elif [ -f "${scanner_c}" ]; then
        sources+=("${scanner_c}")
      fi

      "${compiler}" \
        -shared \
        -O2 \
        -fPIC \
        -I "${src_dir}" \
        -I "${WORK_DIR}/tree-sitter/lib/include" \
        "${sources[@]}" \
        -o "${out}"
    }

    clone_or_update "https://github.com/tree-sitter/tree-sitter.git" "tree-sitter" "${TREE_SITTER_REF}"
    clone_or_update "https://github.com/tree-sitter/tree-sitter-java.git" "tree-sitter-java" "${JAVA_REF}"
    clone_or_update "https://github.com/tree-sitter/tree-sitter-typescript.git" "tree-sitter-typescript" "${TYPESCRIPT_REF}"
    clone_or_update "https://github.com/tree-sitter/tree-sitter-json.git" "tree-sitter-json" "${JSON_REF}"
    clone_or_update "https://github.com/tree-sitter-grammars/tree-sitter-yaml.git" "tree-sitter-yaml" "${YAML_REF}"
    clone_or_update "https://github.com/DerekStride/tree-sitter-sql.git" "tree-sitter-sql" "${SQL_REF}"
    clone_or_update "https://github.com/tree-sitter-grammars/tree-sitter-properties.git" "tree-sitter-properties" "${PROPERTIES_REF}"
    clone_or_update "https://github.com/tree-sitter-grammars/tree-sitter-xml.git" "tree-sitter-xml" "${XML_REF}"

    gcc -shared -O2 -fPIC \
      -I "${WORK_DIR}/tree-sitter/lib/include" \
      -I "${WORK_DIR}/tree-sitter/lib/src" \
      "${WORK_DIR}/tree-sitter/lib/src/lib.c" \
      -o "${LIB_DIR}/libtree-sitter.so"

    build_grammar "tree-sitter-java" "src" "libtree-sitter-java.so"
    build_grammar "tree-sitter-typescript" "typescript/src" "libtree-sitter-typescript.so"
    build_grammar "tree-sitter-json" "src" "libtree-sitter-json.so"
    build_grammar "tree-sitter-yaml" "src" "libtree-sitter-yaml.so"
    build_grammar "tree-sitter-sql" "src" "libtree-sitter-sql.so"
    build_grammar "tree-sitter-properties" "src" "libtree-sitter-properties.so"
    build_grammar "tree-sitter-xml" "xml/src" "libtree-sitter-xml.so"

    ls -lah "${LIB_DIR}"
  '
