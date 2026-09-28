package works.lysenko.util.data.enums;

import works.lysenko.util.apis.data._String;

import static works.lysenko.util.data.enums.Brackets.SQUARE;
import static works.lysenko.util.data.strs.Wrap.e;

/**
 * Represents a type of event.
 */
@SuppressWarnings({"NestedMethodCall", "EnumClass", "MissingJavadoc"})
public enum EventType implements _String {
    FAILURE,
    SEVERE,
    WARNING,
    NOTICE,
    KNOWN_ISSUE,
    UNDEFINED;

    public String getString() {

        return e(SQUARE, name());
    }
}
