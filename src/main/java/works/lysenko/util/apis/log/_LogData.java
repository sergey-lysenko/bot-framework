package works.lysenko.util.apis.log;

/**
 * ProvidesLogData is an interface that defines methods for retrieving log data.
 */
@SuppressWarnings("InterfaceWithOnlyOneDirectInheritor")
public interface _LogData {

    /**
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
     * @return text of log data
     */
    String message();

    /**
     * @return rendered representation of Log DataStorage for terminal output
     */
    String render();
}
