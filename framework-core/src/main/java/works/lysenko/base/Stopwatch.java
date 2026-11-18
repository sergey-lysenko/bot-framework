package works.lysenko.base;

import works.lysenko.util.apis.core._Timer;

/**
 * The Stopwatch class provides utility methods for measuring elapsed time.
 * Implements the _Timer interface.
 *
 * @author Sergii Lysenko
 */
@SuppressWarnings({"ClassHasNoToStringMethod", "ClassWithoutLogger", "PublicMethodWithoutLogging",
        "ClassWithTooManyTransitiveDependents",
        "UnqualifiedFieldAccess", "ClassUnconnectedToPackage", "DesignForExtension", "ImplicitCallToSuper",
        "ClassWithTooManyTransitiveDependencies", "CyclicClassDependency"})
/**
 * The Stopwatch class.
 */
public class Stopwatch implements _Timer {

    private final long start;

    /**
     * Constructs a new Stopwatch instance and initialises the start time to the
     * current system time in milliseconds.
     */
    public Stopwatch() {

        start = System.currentTimeMillis();
    }

    /**
     * Returns the number of milliseconds elapsed since the stopwatch was started.
     *
     * @return elapsed milliseconds since start
     */
    public long msSinceStart() {

        return System.currentTimeMillis() - start;
    }

    /**
     * Returns the timestamp (in milliseconds) when the stopwatch was started.
     *
     * @return start timestamp in milliseconds
     */
    public long startedAt() {

        return start;
    }

    /**
     * Returns the current system time in milliseconds.
     *
     * @return current system time in milliseconds
     */
    public long now() {

        return System.currentTimeMillis();
    }
}
