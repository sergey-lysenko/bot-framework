package works.lysenko.util.data.records;

/**
 * Represents a descriptor for a package.
 *
 * @param pack  the package name
 * @param path  the package path
 * @param depth the depth of the package
 */
public record Descriptor(String pack, String path, int depth) {}
