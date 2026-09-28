package works.lysenko.util.apis.data;

import works.lysenko.util.data.enums.Severity;

/**
 * Represents a class that provides information about an event.
 */
@SuppressWarnings("InterfaceWithOnlyOneDirectInheritor")
public interface _Event {

    /**
     * @return severity of this event
     */
    Severity severity();

    /**
     * @return tag
     */
    String tag();
}
