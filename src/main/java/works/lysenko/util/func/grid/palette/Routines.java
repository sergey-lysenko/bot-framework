package works.lysenko.util.func.grid.palette;

import works.lysenko.util.apis.grid.g._Grid;
import works.lysenko.util.apis.grid.r._Palette;
import works.lysenko.util.grid.record.Request;

import static java.util.Objects.isNull;
import static works.lysenko.Base.log;
import static works.lysenko.Base.logEvent;
import static works.lysenko.util.chrs.__.IN;
import static works.lysenko.util.chrs.__.IS;
import static works.lysenko.util.chrs.__.OF;
import static works.lysenko.util.chrs.___.BUT;
import static works.lysenko.util.chrs.___.WAS;
import static works.lysenko.util.data.enums.Ansi.yb;
import static works.lysenko.util.data.enums.Severity.S0;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Swap.s1;
import static works.lysenko.util.data.strs.Wrap.q;
import static works.lysenko.util.func.type.Objects.isAnyNull;
import static works.lysenko.util.lang.U.UNABLE_TO;
import static works.lysenko.util.lang.word.C.COLOUR;
import static works.lysenko.util.lang.word.C.CONFIGURED;
import static works.lysenko.util.lang.word.C.CREATED;
import static works.lysenko.util.lang.word.E.EMPTY;
import static works.lysenko.util.lang.word.I.INCORRECTLY;
import static works.lysenko.util.lang.word.P.PALETTE;
import static works.lysenko.util.lang.word.R.REQUIREMENT;
import static works.lysenko.util.lang.word.V.VALIDATE;
import static works.lysenko.util.lang.word.V.VALIDATING;

/**
 * The Routines class provides static methods for validating various color components in a given grid.
 */
@SuppressWarnings({"MethodWithMultipleReturnPoints", "StaticMethodOnlyUsedInOneClass"})
public record Routines() {

    /**
     * Checks if the palette of colours is valid, according to the expected range of colours.
     *
     * @param vr   the validation request object
     * @param grid the grid object containing colour data
     * @return true if the palette is valid, false otherwise
     */
    @SuppressWarnings("DataFlowIssue")
    public static boolean isPaletteOk(final Request vr, final _Grid grid) {

        final String name = vr.name();
        final int actualSize = grid.calculator().countPixelsByColor().size();
        final _Palette expected = vr.irq().palette();
        if (isNull(expected.get())) {
            throw new IllegalStateException(b(c(PALETTE), REQUIREMENT, WAS, CREATED, BUT, IS, EMPTY, IN, q(name)));
        }
        if (isAnyNull(expected.get(), expected.get().min(), expected.get().max())) {
            logEvent(S0, b(UNABLE_TO, VALIDATE, INCORRECTLY, CONFIGURED, q(name), PALETTE));
            return false;
        }
        log(b(c(VALIDATING), q(name), PALETTE, OF, yb(s1(actualSize, COLOUR))));
        return actualSize >= expected.get().min() && actualSize <= expected.get().max();
    }

}
