package works.lysenko.util.apis.core;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Represents a file and its directory path as two distinct components.
 * This class holds the directory and filename as immutable properties
 * and provides a utility method to create an instance by splitting
 * a full file path into these components.
 *
 * @param directory the directory path of the file
 * @param filename  the name of the file
 */
@SuppressWarnings("PublicMethodNotExposedInInterface")
public record DirectoryFile(String directory, String filename) {

    /**
     * Creates a new DirectoryFile instance by splitting the given path into its directory
     * and filename components.
     *
     * @param path the file path to be split into directory and filename
     * @return a DirectoryFile instance containing the extracted directory and filename
     */
    public static DirectoryFile df(final String path) {

        final Path pathObj = Paths.get(path);
        final String directory = pathObj.getParent().toString();
        final String filename = pathObj.getFileName().toString();
        return new DirectoryFile(directory, filename);
    }

    /**
     * Retrieves the directory component of the DirectoryFile instance.
     *
     * @return the directory path as a String.
     */
    public String d() {

        return directory;
    }

    /**
     * Retrieves the filename component of the DirectoryFile instance.
     *
     * @return the filename as a String.
     */
    public String f() {

        return filename;
    }
}
