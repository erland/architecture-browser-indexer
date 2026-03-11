package info.isaksson.erland.architecturebrowser.indexer.parse;

import io.github.treesitter.jtreesitter.Language;

import java.lang.foreign.Arena;
import java.lang.foreign.SymbolLookup;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

final class JTreeSitterLanguageLoader {
    private static final Arena ARENA = Arena.ofShared();
    private static final Set<String> LOADED_FILESYSTEM_LIBRARIES = ConcurrentHashMap.newKeySet();

    private final TreeSitterNativeLibraryLocator nativeLibraryLocator;

    JTreeSitterLanguageLoader(TreeSitterConfiguration configuration) {
        this.nativeLibraryLocator = new TreeSitterNativeLibraryLocator(configuration);
    }

    LoadedLanguage load(TreeSitterLanguageDescriptor descriptor) {
        TreeSitterNativeLibraryLocator.ResolvedLibrary runtimeLibrary = nativeLibraryLocator.tryResolveRuntimeLibrary().orElse(null);
        TreeSitterNativeLibraryLocator.ResolvedLibrary resolvedLibrary = nativeLibraryLocator.resolve(descriptor);

        String libraryFileName = System.mapLibraryName(descriptor.sharedLibraryBaseName());
        String lookupTarget = resolvedLibrary.lookupPath().toString();

        try {
            if (runtimeLibrary != null) {
                preloadIfFilesystem(runtimeLibrary.lookupPath());
            }
            preloadIfFilesystem(resolvedLibrary.lookupPath());

            SymbolLookup lookup = SymbolLookup.libraryLookup(lookupTarget, ARENA);
            Language language = Language.load(lookup, descriptor.languageSymbol());
            return new LoadedLanguage(language, lookupTarget, libraryFileName, resolvedLibrary.resolutionMode());
        } catch (Throwable t) {
            throw buildLoadException(descriptor, runtimeLibrary, resolvedLibrary, libraryFileName, lookupTarget, t);
        }
    }

    private TreeSitterLibraryLoadException buildLoadException(
        TreeSitterLanguageDescriptor descriptor,
        TreeSitterNativeLibraryLocator.ResolvedLibrary runtimeLibrary,
        TreeSitterNativeLibraryLocator.ResolvedLibrary languageLibrary,
        String libraryFileName,
        String lookupTarget,
        Throwable throwable
    ) {
        Throwable root = rootCause(throwable);
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("language", descriptor.language().inventoryKey());
        metadata.put("grammarName", descriptor.grammarName());
        metadata.put("sharedLibraryBaseName", descriptor.sharedLibraryBaseName());
        metadata.put("sharedLibraryFileName", libraryFileName);
        metadata.put("languageSymbol", descriptor.languageSymbol());
        metadata.put("lookupTarget", lookupTarget);
        metadata.put("languageLibraryResolutionMode", languageLibrary.resolutionMode());
        metadata.put("languageLibraryResolvedPath", languageLibrary.lookupPath().toAbsolutePath().normalize().toString());
        if (runtimeLibrary != null) {
            metadata.put("runtimeLibraryResolutionMode", runtimeLibrary.resolutionMode());
            metadata.put("runtimeLibraryResolvedPath", runtimeLibrary.lookupPath().toAbsolutePath().normalize().toString());
        }
        metadata.put("exceptionClass", throwable.getClass().getName());
        metadata.put("exceptionMessage", nullableMessage(throwable));
        metadata.put("rootCauseClass", root.getClass().getName());
        metadata.put("rootCauseMessage", nullableMessage(root));

        String message = "Failed to load Tree-sitter language library for " + descriptor.language().inventoryKey()
            + " [symbol=" + descriptor.languageSymbol()
            + ", library=" + libraryFileName
            + ", lookupTarget=" + lookupTarget
            + ", rootCause=" + root.getClass().getSimpleName() + ": " + nullableMessage(root) + "]";

        return new TreeSitterLibraryLoadException(message, throwable, metadata);
    }

    private static Throwable rootCause(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current;
    }

    private static String nullableMessage(Throwable throwable) {
        return throwable.getMessage() == null ? "<no message>" : throwable.getMessage();
    }

    private void preloadIfFilesystem(Path path) {
        if (path == null || !path.isAbsolute()) {
            return;
        }
        String normalized = path.toAbsolutePath().normalize().toString();
        if (LOADED_FILESYSTEM_LIBRARIES.add(normalized)) {
            System.load(normalized);
        }
    }

    record LoadedLanguage(Language language, String lookupTarget, String libraryFileName, String resolutionMode) {
        Optional<String> languageName() {
            return Optional.ofNullable(language.getName());
        }
    }
}
