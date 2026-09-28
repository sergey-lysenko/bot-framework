package works.lysenko.util.apis.util;

import works.lysenko.util.data.enums.Ansi;
import works.lysenko.util.data.enums.EventType;

/**
 * The ProvidesSeverity interface defines the behavior of an object that provides severity information.
 */
@SuppressWarnings("InterfaceWithOnlyOneDirectInheritor")
public interface _Severity {

    /**
     * @return Color of this Severity
     */
    Ansi color();

    /**
     * @return Tag of this Severity
     */
    EventType type();
}
