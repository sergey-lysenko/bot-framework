package works.lysenko.util.data.enums;

import works.lysenko.util.apis.data._String;

import static works.lysenko.util.data.enums.Brackets.SQUARE;
import static works.lysenko.util.data.strs.Wrap.e;

/**
 * Represents a type of event.
 */
@SuppressWarnings({"NestedMethodCall", "EnumClass"})
public enum EventType implements _String {

    /**
     * Failure event type.
     */
    FAILURE,

    /**
     * Severe event type.
     */
    SEVERE,

    /**
     * Warning event type.
     */
    WARNING,

    /**
     * Notice event type.
     */
    NOTICE,

    /**
     * Undefined event type.
     */
    UNDEFINED;

    /**
     * Returns the name of the event type wrapped in square brackets.
     *
     * @return formatted string representation of the event type
     */
    public String getString() {

        return e(SQUARE, name());
    }
}
