# Source language detection for viewer friendliness

This step adds a lightweight language detection layer for the retained-source read flow so the platform source viewer can choose an appropriate syntax-highlighting mode without duplicating extension mapping logic.

## What this step adds

- `SourceLanguageDetectionService`
- viewer-oriented language detection from repository-relative file paths
- integration into `HttpWorkerService.readSourceFile(...)`
- tests covering common source and config file extensions

## Current output contract

The source-read response now populates the `language` field when the file path matches a known viewer language.

Examples:

- `.java` -> `java`
- `.js` -> `javascript`
- `.jsx` -> `jsx`
- `.ts` -> `typescript`
- `.tsx` -> `tsx`
- `.json` -> `json`
- `.yml` / `.yaml` -> `yaml`
- `.properties` -> `properties`
- `.xml` / `pom.xml` -> `xml`
- `.sql` -> `sql`
- `.md` -> `markdown`
- `.txt` -> `plaintext`

If no known mapping exists, the response leaves `language` as `null`.

## Why this is intentionally simple

The goal here is viewer friendliness, not full parser/runtime language identity. The returned value is meant for read-only syntax highlighting in the platform UI.

This step does not attempt to:

- infer framework-specific modes
- detect languages from file contents
- guarantee parity with parse-time language inventory keys
- support every text file type the indexer may scan

## Follow-up compatibility note

The platform should treat `language` as advisory metadata and fall back to path-based highlighting or plaintext when it is absent.
