package works.lysenko.util.data.type.logr.type;

import works.lysenko.util.apis.data._Event;
import works.lysenko.util.data.enums.Ansi;
import works.lysenko.util.data.enums.EventType;
import works.lysenko.util.data.enums.Severity;
import works.lysenko.util.data.type.logr.AbstractLogData;

import static org.apache.commons.lang3.StringUtils.SPACE;
import static works.lysenko.util.data.enums.Ansi.gray;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.func.type.Objects.isNotNull;

/**
 * Represents an event log record.
 * Extends {@link AbstractLogData} and implements {@link _Event}.
 */
@SuppressWarnings("ProtectedField")
public class Event extends AbstractLogData implements _Event {

    /** The type of the event. */
    @SuppressWarnings("WeakerAccess")
    protected final EventType type;
    /** The shortened stack trace associated with the event, if any. */
    private final String stacktrace;

    /**
     * Constructs an Event object with the specified parameters.
     *
     * @param id              the unique identifier for this Event instance
     * @param type            the event type, represented by an instance of EventType
     * @param depth           the depth or indentation level of the log event
     * @param message         the message or description of the event
     * @param shortStackTrace the shortened stack trace associated with the event
     */
    public Event(final long id, final EventType type, final int depth, final String message, final String shortStackTrace) {

        super(id, depth, message);
        this.type = type;
        stacktrace = shortStackTrace;
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
     * Returns the stacktrace.
     *
     * @return the stacktrace
     */
    public final String stacktrace() {

        return stacktrace;
    }
}
