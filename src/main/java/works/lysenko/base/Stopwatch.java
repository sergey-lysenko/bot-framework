package works.lysenko.base;

import works.lysenko.util.apis.core._Timer;

/**
 * @author Sergii Lysenko
 */
@SuppressWarnings({"ClassHasNoToStringMethod", "ClassWithoutLogger", "PublicMethodWithoutLogging",
        "ClassWithTooManyTransitiveDependents",
        "UnqualifiedFieldAccess", "ClassUnconnectedToPackage", "DesignForExtension", "ImplicitCallToSuper",
        "ClassWithTooManyTransitiveDependencies", "CyclicClassDependency"})
public class Stopwatch implements _Timer {

    private final long start;

    /**
     * Constructs a new Stopwatch instance and initialises the start time to the
     * current system time in milliseconds.
     */

    Stopwatch() {

        start = System.currentTimeMillis();
    }

    public long msSinceStart() {

        return System.currentTimeMillis() - start;
    }

    public long startedAt() {

        return start;
    }

    public long now() {

        return System.currentTimeMillis();
    }
}
