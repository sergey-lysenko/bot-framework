package works.lysenko.util.apis.parameters;

/**
 * This interface defines methods for retrieving additional parameters associated with a method.
 * Classes implementing this interface must provide implementations for the methods {@link #getAdditionalTypes()}
 * and {@link #getAdditionalValues()}.
 *
 * <p>
 * The additional types represent the types of the additional parameters, and the additional values represent the
 * values of the additional parameters.
 * </p>
 *
 * <p>
 * Usage example:
 * </p>
 * <pre>{@code
 * class Parameters implements ProvidesAdditionalParameters {
 *     // ...
 *
 *     public String[] getAdditionalTypes() {
 *         // implementation
 *     }
 *
 *     public String[] getAdditionalValues() {
 *         // implementation
 *     }
 * }
 * }</pre>
 */
@SuppressWarnings("InterfaceWithOnlyOneDirectInheritor")
public interface _AdditionalParameters {

    /**
     * Retrieves the additional types associated with a method.
     *
     * @return an array of additional types
     */
    String[] getAdditionalTypes();

    /**
     * Retrieves additional values associated with a method.
     *
     * @return an array of additional values
     */
    String[] getAdditionalValues();

}
