package works.lysenko.base.core;

import works.lysenko.util.apis.data._String;
import works.lysenko.util.chrs.___;
import works.lysenko.util.data.enums.ExecutionParameter;
import works.lysenko.util.data.enums.Platform;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;

import static works.lysenko.Base.parameters;
import static works.lysenko.Base.timer;
import static works.lysenko.util.chrs.___.BOT;
import static works.lysenko.util.chrs.____.CORE;
import static works.lysenko.util.chrs.____.PROC;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.data.strs.Wrap.e;
import static works.lysenko.util.lang.D.DUE_TO;
import static works.lysenko.util.lang.U.UNABLE_TO;
import static works.lysenko.util.lang.word.C.CGROUP;
import static works.lysenko.util.lang.word.D.DETERMINE;
import static works.lysenko.util.lang.word.D.DOCKER;
import static works.lysenko.util.lang.word.P.PRESENCE;
import static works.lysenko.util.lang.word.S.STARTING;
import static works.lysenko.util.spec.Symbols._1_;
import static works.lysenko.util.spec.Symbols._SLASH_;

/**
 * A utility class providing several methods related to operational routines.
 * This class consists of static methods and constants for managing platform-specific
 * operations, resource utilities, and environment checks.
 */
@SuppressWarnings("MethodWithMultipleReturnPoints")
public record Routines() {

    /**
     * A constant representing an optionally spaced "DOTS" value, enveloped using the `e` method.
     * This value is formatted with no leading space and a trailing space.
     * It can be used for constructing messages or formatted output that includes the "DOTS" string constant.
     */
    public static final String DOTS = e(false, ___.DOTS, true);

    /**
     * Closes a Closeable resource quietly.
     *
     * @param closeable      the Closeable resource to close
     * @param failureMessage the failure message to throw as an IllegalStateException if an IOException occurs during the
     *                       close operation
     * @throws IllegalStateException if an IOException occurs during the close operation
     */
    @SuppressWarnings({"ThrowInsideCatchBlockWhichIgnoresCaughtException", "StaticMethodOnlyUsedInOneClass"})
    public static void closeQuietly(final Closeable closeable, final String failureMessage) {

        try {
            closeable.close();
        } catch (final IOException e) {
            throw new IllegalStateException(failureMessage);
        }
    }

    /**
     * @return the platform value as a String
     */
    @SuppressWarnings("StaticMethodOnlyUsedInOneClass")
    public static String get_Platform() {

        return parameters.getValue(ExecutionParameter.PLATFORM);
    }

    /**
     * Determines if the provided {@link Platform} value matches the current execution's platform.
     *
     * @param platform The platform value to compare.
     * @return {@code true} if the provided platform matches the current execution's platform, {@code false} otherwise.
     */
    @SuppressWarnings({"BooleanMethodNameMustStartWithQuestion", "CallToSuspiciousStringMethod"})
    public static boolean in(final _String platform) {

        return platform.getString().equals(parameters.getValue(ExecutionParameter.PLATFORM));
    }

    /**
     * Retrieves the {@link Platform} value from the execution parameters and returns it.
     *
     * @return the {@link Platform} value
     */
    @SuppressWarnings("StaticMethodOnlyUsedInOneClass")
    public static Platform in() {

        return Platform.get(parameters.getValue(ExecutionParameter.PLATFORM));
    }

    /**
     * @return true if current execution is performed in an environment with CI
     * variable set to any value. This is a common way of determining
     * execution inside GitHUB actions and other pipelines
     */
    public static boolean isInsideCI() {

        return System.getenv().containsKey("CI");
    }

    /**
     * @return true if current execution is performed in a Docker container
     */
    @SuppressWarnings("ThrowInsideCatchBlockWhichIgnoresCaughtException")
    public static boolean isInsideDocker() {

        try {
            return Files.readString(Paths.get(s(_SLASH_, PROC, _SLASH_, _1_, _SLASH_, CGROUP))).contains(s(_SLASH_, DOCKER));
        } catch (final NoSuchFileException e) {
            // most probably that is caused by being executed on Windows;
            return false;
        } catch (final IOException e) {
            throw new IllegalStateException(b(UNABLE_TO, DETERMINE, c(DOCKER), PRESENCE, DUE_TO, e.toString()));
        }
    }

    /**
     * Checks if the test properties are empty.
     *
     * @return true if the test properties are empty, false otherwise
     */
    @SuppressWarnings("StaticMethodOnlyUsedInOneClass")
    public static boolean isTestPropertiesEmpty() {

        return parameters.isEmpty();
    }

    /**
     * @return The number of milliseconds since the program start.
     */
    public static long msSinceStart() {

        return timer.msSinceStart();
    }

    /**
     * @return The moment of Stopwatch instantiation.
     */
    public static long startedAt() {

        return timer.startedAt();
    }

    /**
     * @return The moment of Stopwatch instantiation.
     */
    @SuppressWarnings("unused")
    public static long now() {

        return timer.now();
    }

    /**
     * Constructs and outputs a formatted message by combining specific string constants and transformations.
     * <p>
     * This method utilizes helper methods and constants to generate the output message:
     * - The `c()` method is used to capitalize string representations of predefined constants.
     * - The `b()` method binds the resulting strings with a space (`_SPACE_`) character.
     * - The `DOTS` constant from the `___` class is appended to the constructed string.
     * <p>
     * The message combines the capitalized versions of the `STARTING` and `BOT` constants,
     * a `CORE` string, and the `DOTS` constant, and prints the result to the standard output.
     */
    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    public static void starting() {
        BootCapture.start();
        System.out.println(b(c(STARTING), c(BOT), CORE, ___.DOTS));
    }
}
