package works.lysenko.base.output;

import works.lysenko.util.spec.Level;

import static works.lysenko.Base.exec;
import static works.lysenko.Base.log;
import static works.lysenko.util.chrs.__.NO;
import static works.lysenko.util.chrs.___.ALL;
import static works.lysenko.util.chrs.___.NOT;
import static works.lysenko.util.data.enums.Ansi.*;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.lang.C.COMMA_SPACE;
import static works.lysenko.util.lang.K.KNOWN_ISSUES;
import static works.lysenko.util.lang.W.WERE_REPRODUCED;
import static works.lysenko.util.lang.word.C.CONFIRMED;
import static works.lysenko.util.lang.word.R.REPRODUCED;
import static works.lysenko.util.spec.Symbols._COLON_;

/**
 * The `KnownIssues` class provides methods for logging and displaying known issues.
 */
@SuppressWarnings({"FeatureEnvy", "ChainedMethodCall", "NestedMethodCall", "AutoBoxing"})
record KnownIssues() {

    /**
     * This method prints the not reproduced issues to the console.
     */
    private static void notReproduced() {

        if (!exec.issues().known().isEmpty()) {
            exec.logEmptyLine();
            if (exec.issues().notReproduced().isEmpty())
                log(Level.none, ansi(b(c(ALL), KNOWN_ISSUES, WERE_REPRODUCED), GREEN), true);
            else {
                log(Level.none, ansi(b(c(NOT), REPRODUCED, s(KNOWN_ISSUES, _COLON_)), YELLOW), true);
                log(Level.none, ansi(String.join(COMMA_SPACE, exec.issues().notReproduced()), YELLOW), false);
            }
        }
    }

    /**
     * Performs specific actions related to logging known issues.
     */
    private static void reproduced() {

        exec.logEmptyLine();
        if (exec.issues().known().isEmpty())
            log(Level.none, ansi(b(c(NO), KNOWN_ISSUES, WERE_REPRODUCED), RED), true);
        else {
            log(Level.none, ansi(b(c(CONFIRMED), s(KNOWN_ISSUES, _COLON_)), MAGENTA), true);
            log(Level.none, ansi(String.join(COMMA_SPACE, exec.issues().known()), MAGENTA), false);
        }
    }

    /**
     * Runs the main process.
     * <p>
     * This method is responsible for executing the main process by invoking the `reproduced()` and `notReproduced()`
     * methods. No parameters are required for this method.
     */
    static void run() {

        reproduced();
        notReproduced();
    }
}
