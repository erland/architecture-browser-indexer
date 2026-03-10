# Tree-sitter foundation notes (Step 4)

## What Step 4 adds

This step adds the parsing foundation needed before language-specific extraction:
- parser registry keyed by detected language
- common parse request/result abstraction
- syntax tree model accessible to future extractors
- parse issue model and IR diagnostic conversion
- default Tree-sitter registry that degrades gracefully when no runtime is installed

## Why the default registry is runtime-safe

The repository baseline currently targets Java 17.
The current official `jtreesitter` artifact published by the Tree-sitter project is built with Java release 23,
so this step keeps the indexer on a Java-17-safe abstraction layer and uses runtime detection rather than
forcing an immediate JDK upgrade.

That means:
- the parsing pipeline is integrated now
- extraction code in Step 5 can depend on stable parse abstractions
- the default CLI run will emit backend-unavailable diagnostics rather than crashing if no Tree-sitter runtime is present

## Expected next evolution

When you decide to move to a compatible runtime/JDK setup, the next implementation step is to add one or more
real `SourceParser` implementations backed by Tree-sitter grammars for Java and TypeScript.
The rest of the indexer should not need large structural changes, because the registry and parse result contracts
are already in place.
