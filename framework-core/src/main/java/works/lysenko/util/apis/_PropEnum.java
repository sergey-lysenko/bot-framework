package works.lysenko.util.apis;

import works.lysenko.base.util.StringParser;

import java.util.Locale;

import static java.util.Objects.isNull;
import static works.lysenko.Base.properties;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.spec.Symbols.UND_SCR;

/**
 * Represents a generic property enumerator providing methods to retrieve
 * property-specific information and values with customizable behavior.
 */
public interface _PropEnum {

    /**
     * Retrieves the name of this enum constant, as declared in the enum declaration.
     *
     * @return the name of this enum constant
     */
    String name();

    /**
     * Retrieves the default value associated with the property.
     *
     * @return the default value of the property as a string
     */
    String defaultValue();

    /**
     * Retrieves the value associated with the property.
     * If the property is not set, it provides a default value based on the property type.
     *
     * @param <T> the type of the property value
     * @return the value of the property, or the default value if the property is not set
     */
    @SuppressWarnings("unchecked")
    default <T> T get() {

        if (isNull(properties)) {
            properties = new works.lysenko.base.TestProperties();
            properties.readCommonConfiguration();
        }
        return (T) properties.getEnum(this);
    }

    /**
     * Retrieves the name of the property represented by the enumerator.
     *
     * @return the name of the property as a string
     */
    default String getPropertyName() {

        return s(name().toLowerCase(Locale.ROOT).replace(UND_SCR, '.'));
    }

    /**
     * Determines whether the property interactions or retrieves should operate silently.
     * When set to true, operations related to the property may suppress logs or exceptions,
     * depending on the implementation.
     *
     * @return true if the property should operate silently, false otherwise
     */
    @SuppressWarnings("BooleanMethodNameMustStartWithQuestion")
    default boolean silent() {

        return false;
    }

    /**
     * Determines whether this property is also exposed as an execution parameter.
     *
     * @return true if the property is also an execution parameter
     */
    default boolean executionParameter() {

        return false;
    }

    /**
     * Retrieves the type of the property associated with this property enumerator.
     *
     * @return the class type of the property
     */
    Class<?> type();
}
