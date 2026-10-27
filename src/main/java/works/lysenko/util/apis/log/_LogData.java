package works.lysenko.util.apis.log;

/**
 * ProvidesLogData is an interface that defines methods for retrieving log data.
 */
@SuppressWarnings("InterfaceWithOnlyOneDirectInheritor")
public interface _LogData {

    /**
     * Retrieves the depth of the log data.
     *
     * @return depth of log data
     */
    int depth();

    /**
     * Retrieves the unique identifier for the log data.
     *
     * @return an integer representing the unique ID of the log data
     */
    long id();

    /**
     * Retrieves the text message of the log data.
     *
     * @return text of log data
     */
    String message();

    /**
     * Retrieves the stack trace or call site of the log data, if available.
     *
     * @return stack trace string, or null if absent
     */
    default String stacktrace() {
        return null;
    }
}
