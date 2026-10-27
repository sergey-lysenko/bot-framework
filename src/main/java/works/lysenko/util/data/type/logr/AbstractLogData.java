package works.lysenko.util.data.type.logr;

import works.lysenko.util.apis.log._LogData;

/**
 * AbstractLogData is an abstract class that provides common functionalities for log data objects.
 * Implements the {@link _LogData} interface.
 */
@SuppressWarnings("ProtectedField")
public abstract class AbstractLogData implements _LogData { // TODO: port to Record

    /** The log message string. */
    protected final String message;
    /** The indentation depth level. */
    protected final int depth;
    /** The unique log data identifier. */
    protected final long id;
    /** The stacktrace or callsite associated with this log data. */
    protected final String stacktrace;

    /**
     * Constructs an AbstractLogData object with the specified ID, depth, message, and stacktrace.
     *
     * @param id         the unique identifier
     * @param depth      the indentation depth
     * @param message    the log message text
     * @param stacktrace the stacktrace string
     */
    protected AbstractLogData(final long id, final int depth, final String message, final String stacktrace) {

        this.id = id;
        this.depth = depth;
        this.message = message;
        this.stacktrace = stacktrace;
    }

    /**
     * Constructs an AbstractLogData object with the specified ID, depth, and message.
     *
     * @param id      the unique identifier
     * @param depth   the indentation depth
     * @param message the log message text
     */
    protected AbstractLogData(final long id, final int depth, final String message) {

        this(id, depth, message, null);
    }

    @Override
    public final int depth() {

        return depth;
    }

    @Override
    public final long id() {

        return id;
    }

    @SuppressWarnings("DesignForExtension")
    @Override
    public String message() {

        return message;
    }

    @Override
    public String stacktrace() {

        return stacktrace;
    }
}
