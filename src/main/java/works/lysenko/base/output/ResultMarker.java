package works.lysenko.base.output;

import works.lysenko.util.data.enums.Ansi;
import works.lysenko.util.spec.Level;

import static org.apache.commons.lang3.StringUtils.SPACE;
import static works.lysenko.Base.core;
import static works.lysenko.Base.exec;
import static works.lysenko.Base.log;
import static works.lysenko.util.chrs.__.NO;
import static works.lysenko.util.chrs.____.EXIT;
import static works.lysenko.util.chrs.____.TEST;
import static works.lysenko.util.data.enums.Ansi.*;
import static works.lysenko.util.data.enums.Brackets.SQUARE;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Case.u;
import static works.lysenko.util.data.strs.Null.sn;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.data.strs.Wrap.e;
import static works.lysenko.util.func.imgs.Screenshot.makeScreenAndCodeSnapshot;
import static works.lysenko.util.lang.word.E.EXECUTION;
import static works.lysenko.util.lang.word.P.PASSED;
import static works.lysenko.util.lang.word.R.RESULTS;
import static works.lysenko.util.lang.word.S.SUCCESSFULLY;
import static works.lysenko.util.spec.Symbols.*;

@SuppressWarnings({"NestedMethodCall", "FeatureEnvy", "AutoBoxing"})
record ResultMarker() {

    /**
     * Generates a failure message using the core's failure message.
     *
     * @return The generated failure message.
     */
    @SuppressWarnings({"MethodWithMultipleReturnPoints", "CallToSuspiciousStringMethod"})
    private static String generateFailureMessage() {

        if (core.getResults().getFailures().isEmpty()) return null;
        final String s = core.getResults().getFailures().get(0);
        return generateMessage(s.trim().replace(_LFD_, _BULLT_));
    }

    /**
     * Generates a message by enclosing the given message with symbols.
     *
     * @param message The message to be enclosed.
     * @return The generated message.
     */
    private static String generateMessage(final String message) {

        return s(e(s(_EQUAL_)), message, e(s(_EQUAL_)));
    }

    /**
     * Generates a plaque with the given message using the specified foreground and background colours.
     *
     * @param message    The message to be displayed on the plaque.
     * @param foreground The foreground colour of the plaque.
     * @param background The background colour of the plaque.
     */
    private static void plaque(final String message, final Ansi foreground, final Ansi background) {

        log(Level.none, ansi(ansi(SPACE.repeat(message.length()), background), foreground), false);
        log(Level.none, ansi(ansi(message, background), foreground), false);
        log(Level.none, ansi(ansi(SPACE.repeat(message.length()), background), foreground), false);
    }

    /**
     * Runs the specified method and generates a result message based on success or failure.
     * It displays the result message on a plaque with specified foreground and background colours.
     * It also logs an empty line before displaying the result message.
     */
    static void run() {

        exec.logEmptyLine();

        final String resultMessage;
        final Ansi foreground;
        final Ansi background;

        if (core.getResults().getSorted().isEmpty()) {
            resultMessage = generateMessage(b(c(NO), TEST, RESULTS));
            foreground = WHITE_BOLD_BRIGHT;
            background = BLACK_BACKGROUND_BRIGHT;
        } else if (core.getResults().getFailures().isEmpty()) {
            resultMessage = generateMessage(b(c(EXECUTION), PASSED, SUCCESSFULLY));
            foreground = BLACK;
            background = GREEN_BACKGROUND;
        } else {
            resultMessage = generateFailureMessage();
            foreground = WHITE_BOLD_BRIGHT;
            background = RED_BACKGROUND;
            makeScreenAndCodeSnapshot(true, e(UND_SCR, e(SQUARE, u(EXIT))));
        }
        plaque(sn(resultMessage), foreground, background);
    }
}
