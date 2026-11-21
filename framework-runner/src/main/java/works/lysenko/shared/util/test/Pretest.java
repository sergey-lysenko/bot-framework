package works.lysenko.shared.util.test;

import works.lysenko.util.spec.*;

import static works.lysenko.Base.*;
import static works.lysenko.util.chrs.____.*;
import static works.lysenko.util.data.enums.Ansi.*;
import static works.lysenko.util.data.strs.Bind.*;

/**
 * Pretest lifecycle hook responsible for resetting application state before each individual test iteration.
 */
public class Pretest implements Runnable {

    /**
     * Executes the per-test setup routine.
     */
    @Override
    public final void run() {

        section(getClass().getSimpleName());
        log(Level.none, bb(b(getClass().getSimpleName(), DONE)), false);
    }
}
