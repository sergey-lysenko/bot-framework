package works.lysenko.util.apis.core;

/**
 * The _Timer interface provides methods to manage and retrieve timing information.
 * It defines basic functionality for tracking the starting moment and elapsed time.
 */
@SuppressWarnings("InterfaceWithOnlyOneDirectInheritor")
public interface _Timer {

    /**
     * Retrieves elapsed milliseconds since stopwatch start.
     *
     * @return milliseconds since Stopwatch instantiation
     */
    @SuppressWarnings("unused")
    long msSinceStart();

    /**
     * Retrieves timestamp when stopwatch started.
     *
     * @return moment of Stopwatch instantiation
     */
    long startedAt();

    /**
     * Retrieves the current moment in time.
     *
     * @return the current time in milliseconds since the epoch.
     */
    long now();
}
