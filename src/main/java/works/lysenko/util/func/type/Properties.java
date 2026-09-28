package works.lysenko.util.func.type;

import works.lysenko.util.apis.core.DirectoryFile;
import works.lysenko.util.spec.Level;

import java.io.*;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.isNull;
import static works.lysenko.Base.log;
import static works.lysenko.Base.logEvent;
import static works.lysenko.util.apis.core.DirectoryFile.df;
import static works.lysenko.util.chrs.__.TO;
import static works.lysenko.util.chrs.____.FROM;
import static works.lysenko.util.data.enums.Ansi.gb;
import static works.lysenko.util.data.enums.Ansi.rb;
import static works.lysenko.util.data.enums.Severity.S1;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Wrap.q;
import static works.lysenko.util.func.type.Objects.isNotNull;
import static works.lysenko.util.lang.D.DUE_TO;
import static works.lysenko.util.lang.U.UNABLE_TO_LOAD;
import static works.lysenko.util.lang.U.UNABLE_TO_WRITE_DATA_TO;
import static works.lysenko.util.lang.word.E.ERROR;
import static works.lysenko.util.lang.word.L.LOADING;
import static works.lysenko.util.lang.word.R.READING;
import static works.lysenko.util.lang.word.R.REQUESTED;
import static works.lysenko.util.lang.word.S.STORING;

/**
 * Utility functions for managing and interacting with java.util.Properties.
 * Includes methods for storing properties to files, reading properties from files,
 * and managing property streams.
 */
@SuppressWarnings({"OverlyBroadCatchBlock", "ThrowInsideCatchBlockWhichIgnoresCaughtException"})
public record Properties() {

    /**
     * Writes the provided Properties object to a specified file.
     *
     * @param properties the Properties object to be written
     * @param path       the path of the file to write to
     * @return true if the file was successfully written, false otherwise
     * @throws IllegalArgumentException if there is an error writing the properties to the file
     */
    public static boolean arePropertiesStoredToFile(final java.util.Properties properties, final String path) {

        return arePropertiesStoredToFile(properties, path, null, false);
    }

    /**
     * Writes the provided Properties object to a specified file.
     *
     * @param properties the Properties object to be written
     * @param path       the path of the file to write to
     * @param comment    a comment to be associated with the file (optional, can be null)
     * @param silent     if true, suppress log messages; if false, log storing message
     * @return true if the file was successfully written, false otherwise
     * @throws IllegalArgumentException if there is an error writing the properties to the file
     */
    public static boolean arePropertiesStoredToFile(final java.util.Properties properties, final String path,
                                                    final String comment,
                                                    final boolean silent) {

        boolean result = false;
        if (isNotNull(path) && isNotNull(properties)) {
            if (!silent) log(Level.none, rb(getStoringMessage(df(path))), true);
            try (final Writer writer = new OutputStreamWriter(new FileOutputStream(path), UTF_8)) {
                properties.store(writer, isNull(comment) ? path : comment);
                result = true;
            } catch (final IOException e) {
                throw new IllegalArgumentException(b(UNABLE_TO_WRITE_DATA_TO, q(path), DUE_TO, q(e.getMessage())));
            }
        }
        return result;
    }

    private static String getLoadingMessage(final DirectoryFile df) {

        return b(c(LOADING), q(df.f()), FROM, q(df.d()));
    }

    /**
     * @param path of properties file to read
     * @return {@link java.util.Properties} instance
     */
    public static java.util.Properties getPropertiesFromFile(final String path) {

        return getPropertiesFromFile(path, false);
    }

    /**
     * Reads properties from a file and returns a Properties object.
     *
     * @param path   the name of the file to read properties from
     * @param silent if true, suppress log messages; if false, log loading message
     * @return a Properties instance containing the properties from the file, or an empty Properties object if the file does
     * not exist or there is an error reading it
     */
    @SuppressWarnings("TryWithIdenticalCatches")
    public static java.util.Properties getPropertiesFromFile(final String path, final boolean silent) {

        java.util.Properties prop = null;

        if (isNotNull(path)) {
            prop = new java.util.Properties();
            if (!silent)
                log(Level.none, gb(getLoadingMessage(df(path))), true);
            try (final FileInputStream fis = new FileInputStream(path);
                 final Reader reader = new InputStreamReader(fis, UTF_8)) {
                prop.load(reader);
            } catch (final FileNotFoundException e) {
                throw new UncheckedIOException(b(UNABLE_TO_LOAD, REQUESTED, q(path)), e);
            } catch (final IOException e) {
                throw new UncheckedIOException(b(UNABLE_TO_LOAD, REQUESTED, q(path)), e);
            }
        }
        return prop;
    }

    /**
     * Retrieves properties from a given FileInputStream.
     *
     * @param in           the FileInputStream to read properties from (optional, can be null)
     * @param errorMessage the error message to log if there is an IOException
     * @return a Properties instance containing the properties from the FileInputStream
     */
    @SuppressWarnings("NestedTryStatement")
    public static java.util.Properties getStore(final FileInputStream in, final String errorMessage) {

        final java.util.Properties store = new java.util.Properties();
        try {
            if (isNotNull(in)) {
                try (final Reader reader = new InputStreamReader(in, UTF_8)) {
                    store.load(reader);
                }
                in.close();
            }
        } catch (final IOException e) {
            logEvent(S1, errorMessage);
        }
        return store;
    }

    /**
     * Retrieves properties from a given FileInputStream.
     *
     * @param path the name of the file to retrieve the input stream for
     * @return a Properties instance containing the properties from the FileInputStream
     */
    public static java.util.Properties getStore(final String path) {

        log(Level.none, gb(getLoadingMessage(df(path))), true);
        return getStore(Files.getIn(path), b(q(path), READING, ERROR));
    }

    private static String getStoringMessage(final DirectoryFile df) {

        return b(c(STORING), q(df.f()), TO, q(df.d()));
    }

    /**
     * Writes the provided Properties object to a specified file.
     *
     * @param store   the Properties object to be written
     * @param path    the path of the file to write to
     * @param comment a comment to be associated with the file (optional, can be null)
     * @return true if the file was successfully written, false otherwise
     */
    @SuppressWarnings({"BooleanMethodNameMustStartWithQuestion", "StaticMethodOnlyUsedInOneClass"})
    public static boolean putStore(final java.util.Properties store, final String path, final String comment) {

        return arePropertiesStoredToFile(store, path, comment, false);
    }
}
