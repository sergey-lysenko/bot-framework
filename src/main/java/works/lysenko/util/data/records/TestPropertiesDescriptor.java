package works.lysenko.util.data.records;

/**
 * Represents a descriptor for test properties file.
 *
 * @param directory the directory containing the properties file
 * @param name      the name of the properties file
 * @param extension the extension of the properties file
 */
public record TestPropertiesDescriptor(String directory, String name, String extension) {}
