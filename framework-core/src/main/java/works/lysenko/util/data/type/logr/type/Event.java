package works.lysenko.util.data.type.logr.type;

import works.lysenko.util.apis.data._Event;
import works.lysenko.util.data.enums.EventType;
import works.lysenko.util.data.enums.Severity;
import works.lysenko.util.data.type.logr.AbstractLogData;

import static works.lysenko.util.data.strs.Bind.b;

/**
 * Represents an event log record.
 * Extends {@link AbstractLogData} and implements {@link _Event}.
 */
@SuppressWarnings("ProtectedField")
public class Event extends AbstractLogData implements _Event {

    /** The type of the event. */
    @SuppressWarnings("WeakerAccess")
    protected final EventType type;

    /**
     * Constructs an Event object with the specified parameters.
     *
     * @param id             the unique identifier for this Event instance
     * @param type           the event type, represented by an instance of EventType
     * @param depth          the depth or indentation level of the log event
     * @param message        the message or description of the event
     * @param shortCallstack the shortened call stack associated with the event
     */
    public Event(final long id, final EventType type, final int depth, final String message, final String shortCallstack) {

        super(id, depth, message, shortCallstack);
        this.type = type;
    }

    @Override
    public final String message() {

        return b(type.getString(), super.message());
    }

    /**
     * Returns the severity of the event determined by its type.
     *
     * @return the Severity level
     */
    public final Severity severity() {

        return Severity.byType(type);
    }

    /**
     * Returns the string tag representation of the event type.
     *
     * @return the event type tag string
     */
    public final String tag() {

        return type.getString();
    }

    /**
     * Returns the event type.
     *
     * @return the EventType
     */
    public final EventType type() {

        return type;
    }

    /**
     * Returns the compact call stack.
     *
     * @return the call stack
     */
    @Override
    public final String callstack() {

        return callstack;
    }

    /**
     * Legacy accessor preserved for binary backwards-compatibility.
     *
     * @return the call stack
     * @deprecated Use {@link #callstack()} instead.
     */
    @Deprecated
    @Override
    public final String stacktrace() {

        return callstack();
    }
}
