package works.lysenko.util.apis.grid.t;

/**
 * Represents a generic value of type T that can be rendered to a string representation.
 *
 * @param <T> type of value
 */
@SuppressWarnings({"InterfaceWithOnlyOneDirectInheritor", "ClassNamePrefixedWithPackageName"})
public interface _Value<T> {

    /**
     * Renders the object to a string representation.
     *
     * @return A1 string representation of the object.
     */
    String render();

    /**
     * Retrieves the value of type T.
     *
     * @return The value of type T.
     */
    T value();
}
