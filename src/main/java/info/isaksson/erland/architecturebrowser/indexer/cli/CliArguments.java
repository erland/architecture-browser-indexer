package info.isaksson.erland.architecturebrowser.indexer.cli;

import java.nio.file.Path;

record CliArguments(boolean showHelp, boolean showVersion, Path source, Path output) {

    static CliArguments parse(String[] args) {
        boolean showHelp = false;
        boolean showVersion = false;
        Path source = null;
        Path output = null;

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            switch (arg) {
                case "--help", "-h" -> showHelp = true;
                case "--version", "-V" -> showVersion = true;
                case "--source", "-s" -> {
                    i = requireValueIndex(args, i, arg);
                    source = Path.of(args[i]);
                }
                case "--output", "-o" -> {
                    i = requireValueIndex(args, i, arg);
                    output = Path.of(args[i]);
                }
                default -> throw new IllegalArgumentException("Unknown argument: " + arg);
            }
        }

        return new CliArguments(showHelp, showVersion, source, output);
    }

    private static int requireValueIndex(String[] args, int currentIndex, String optionName) {
        int valueIndex = currentIndex + 1;
        if (valueIndex >= args.length) {
            throw new IllegalArgumentException("Missing value for option: " + optionName);
        }
        return valueIndex;
    }
}
