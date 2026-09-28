package works.lysenko.util.apis.parameters;

import works.lysenko.util.data.enums.ExecutionParameter;

/**
 * Represents a functional interface for providing execution parameter values.
 * Implementing classes must define the method {@link #getValue(ExecutionParameter)}
 * to retrieve the value of a given execution parameter.
 *
 * <p>
 * Usage example:
 * </p>
 * <pre>{@code
 * class MyClass implements ProvidesExecutionParameterValue {
 *     // ...
 *
 *     public String getValue(ExecutionParameter parameter) {
 *         // implementation
 *     }
 * }
 * }</pre>
 */
@SuppressWarnings("InterfaceWithOnlyOneDirectInheritor")
@FunctionalInterface
public interface _ExecutionParameterValue {

    /**
     * Retrieves the value of the specified execution parameter.
     *
     * @param parameter the execution parameter for which to retrieve the value
     * @return the value associated with the provided execution parameter
     */
    String getValue(ExecutionParameter parameter);
}
