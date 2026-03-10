package info.isaksson.erland.architecturebrowser.indexer.acquisition;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

final class GitClient {
    private GitClient() {
    }

    static boolean isGitWorkingTree(Path directory) {
        try {
            return run(directory, List.of("git", "rev-parse", "--is-inside-work-tree")).trim().equals("true");
        } catch (RuntimeException exception) {
            return false;
        }
    }

    static void appendGitMetadata(Path directory, Map<String, Object> metadata) {
        metadata.put("gitBranch", safeRun(directory, List.of("git", "rev-parse", "--abbrev-ref", "HEAD")));
        metadata.put("gitRevision", safeRun(directory, List.of("git", "rev-parse", "HEAD")));
        metadata.put("gitHasRemote", hasRemote(directory));
    }

    static void cloneRepository(String gitUrl, String gitRef, Path destinationDirectory) {
        List<String> command = gitRef == null || gitRef.isBlank()
            ? List.of("git", "clone", "--depth", "1", gitUrl, destinationDirectory.toString())
            : List.of("git", "clone", "--depth", "1", "--branch", gitRef, gitUrl, destinationDirectory.toString());
        run(destinationDirectory.getParent(), command);
    }

    private static boolean hasRemote(Path directory) {
        try {
            return !run(directory, List.of("git", "remote")).trim().isBlank();
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private static String safeRun(Path directory, List<String> command) {
        try {
            return run(directory, command).trim();
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private static String run(Path directory, List<String> command) {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        if (directory != null) {
            processBuilder.directory(directory.toFile());
        }
        processBuilder.redirectErrorStream(true);
        try {
            Process process = processBuilder.start();
            byte[] outputBytes = process.getInputStream().readAllBytes();
            int exitCode = process.waitFor();
            String output = new String(outputBytes, StandardCharsets.UTF_8);
            if (exitCode != 0) {
                throw new IllegalStateException("Git command failed: " + String.join(" ", command) + "\n" + output.trim());
            }
            return output;
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to execute git command: " + String.join(" ", command), exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Git command interrupted: " + String.join(" ", command), exception);
        }
    }
}
