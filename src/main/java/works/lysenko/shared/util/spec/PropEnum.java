package works.lysenko.shared.util.spec;

import works.lysenko.util.apis.*;

import static works.lysenko.util.data.strs.Swap.*;

/**
 * Enumeration representing property definitions implementing the {@link _PropEnum} interface.
 */
@SuppressWarnings({"OverloadedVarargsMethod", "Singleton"})
public enum PropEnum implements _PropEnum {

    /**
     * Dummy property used for placeholder or default configuration purposes.
     */
    _DUMMY(Boolean.class, "false");

    private final Class<?> type;
    private final String defaultValue;

    /**
     * Constructs a property enum constant with a specified type and default string value.
     *
     * @param type               the data type of the property
     * @param defaultStringValue the default string value of the property
     */
    PropEnum(final Class<?> type, final String defaultStringValue) {

        this.type = type;
        defaultValue = defaultStringValue;
    }

    /**
     * Constructs a property enum constant with a specified type and default character value.
     *
     * @param type             the data type of the property
     * @param defaultCharValue the default character value of the property
     */
    PropEnum(final Class<?> type, final char defaultCharValue) {

        this.type = type;
        defaultValue = s(defaultCharValue);
    }

    /**
     * Constructs a property enum constant with a specified type and default character array.
     *
     * @param type         the data type of the property
     * @param defaultChars the default characters constituting the string value
     */
    PropEnum(final Class<?> type, final char... defaultChars) {

        this.type = type;
        defaultValue = new String(defaultChars);
    }

    /**
     * Returns the default value associated with this property enum constant.
     *
     * @return the default string value
     */
    @Override
    public String defaultValue() {

        return defaultValue;
    }

    /**
     * Returns the data type associated with this property enum constant.
     *
     * @return the Class representing the type of the property
     */
    @Override
    public Class<?> type() {

        return type;
    }
}
