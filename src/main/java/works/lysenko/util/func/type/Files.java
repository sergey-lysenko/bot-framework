package works.lysenko.util.func.type;

import works.lysenko.util.lang.U;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.writeString;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static works.lysenko.Base.logEvent;
import static works.lysenko.util.data.enums.Severity.S0;
import static works.lysenko.util.data.enums.Severity.S1;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.data.strs.Wrap.q;
import static works.lysenko.util.func.type.Objects.isNotNull;
import static works.lysenko.util.lang.N.NOT_FOUND;
import static works.lysenko.util.lang.U.UNABLE_TO_WRITE_INTO_FILE;
import static works.lysenko.util.spec.Symbols._NUMBR_;

/**
 * Various files related routines
 */
@SuppressWarnings({"StaticMethodOnlyUsedInOneClass", "CallToPrintStackTrace", "ResultOfMethodCallIgnored",
        "ThrowInsideCatchBlockWhichIgnoresCaughtException", "CallToSuspiciousStringMethod"})
public record Files() {

    /**
     * Deletes a file with the given name.
     *
     * @param name the name of the file to be deleted
     */
    public static void deleteFile(final String name) {

        try {
            java.nio.file.Files.delete(Paths.get(name));
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Retrieves a list of file paths in a given directory that match the specified file extension.
     *
     * @param path      the directory path to search for files
     * @param extension the file extension to filter by (e.g., ".txt", ".java")
     * @return a list of file paths as strings that match the specified extension, or null if an error occurs
     */
    public static List<String> getFilesList(final String path, final String extension) {

        List<String> result = null;
        try (final Stream<Path> walk = java.nio.file.Files.walk(Paths.get(path))) {
            result =
                    walk.filter(java.nio.file.Files::isRegularFile).map(Path::toString).filter(fi -> fi.endsWith(extension)).toList();
        } catch (final IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * Retrieves an input stream for the specified file.
     *
     * @param file The name of the file to retrieve the input stream for.
     * @return The FileInputStream for the specified file, or null if the file is not found.
     */
    @SuppressWarnings("WeakerAccess")
    public static FileInputStream getIn(final String file) {

        FileInputStream in = null;
        try {
            in = new FileInputStream(file);
        } catch (final FileNotFoundException e) {
            logEvent(S0, b(q(file), NOT_FOUND));
        }
        return in;
    }

    /**
     * Checks if a file with the given name exists.
     *
     * @param name the name of the file
     * @return true if the file exists, false otherwise
     */
    public static boolean isFilePresent(final String name) {

        return java.nio.file.Files.exists(Paths.get(name));
    }

    /**
     * Read contents of the text file into List of Strings
     *
     * @param name of a file
     * @return list of lines from a file
     */
    public static List<String> loadLinesFromFile(final Path name) {

        final List<String> out = new ArrayList<>(0);
        try (final Stream<String> in = java.nio.file.Files.lines(name)) {
            in.forEach(r -> {
                final String clean = r.split(s(_NUMBR_))[0].trim();
                if (!clean.isEmpty()) out.add(clean);
            });
        } catch (final IOException e) {
            logEvent(S1, b(U.UNABLE_TO_READ_LINES_FROM, q(name.toString())));
        }
        return out;
    }

    /**
     * Return first line of file as String
     *
     * @param name of a file
     * @return list of lines from a file
     */
    @SuppressWarnings("unused")
    public static String loadStringFromFile(final Path name) {

        return loadLinesFromFile(name).get(0);
    }

    /**
     * Create a file with the specified name and write the content to it.
     * If the parent directory of the file does not exist, it will be created.
     *
     * @param first the first line to be written into the file (optional, can be null)
     * @param lines an iterable collection of lines to be written into the file
     * @param name  the name of the file to be created
     * @throws IllegalArgumentException if there is an error writing the content into the file
     */
    public static void writeToFile(final String first, final Iterable<String> lines, final String name) {

        if (isNotNull((new File(name).getParentFile()))) new File(name).getParentFile().mkdirs();
        try (final BufferedWriter writer = new BufferedWriter(new FileWriter(name, UTF_8))) {
            if (isNotNull(first)) writer.write(s(first, System.lineSeparator()));
            for (final String s : lines)
                writer.write(s(s, System.lineSeparator()));
        } catch (final IOException e) {
            throw new IllegalArgumentException(b(UNABLE_TO_WRITE_INTO_FILE, q(name)));
        }
    }

    /**
     * Creates a file and writes the given line into it.
     * If the parent directory of the file does not exist, it will be created.
     * If the file already exists, its contents will be truncated.
     *
     * @param line the line to be written into the file
     * @param name the name of the file to be created
     * @throws IllegalArgumentException if there is an error writing the line into the file
     */
    public static void writeToFile(final CharSequence line, final String name) {

        if (isNotNull((new File(name).getParentFile()))) new File(name).getParentFile().mkdirs();
        try {
            writeString(Paths.get(name), line, UTF_8, CREATE, TRUNCATE_EXISTING);
        } catch (final IOException e) {
            throw new IllegalArgumentException(b(UNABLE_TO_WRITE_INTO_FILE, q(name)));
        }
    }
}
