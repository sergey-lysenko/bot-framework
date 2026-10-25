package works.lysenko.shared.lang.word;

import static works.lysenko.util.chrs.__.*;
import static works.lysenko.util.chrs.____.*;
import static works.lysenko.util.data.strs.Swap.*;
import static works.lysenko.util.spec.Symbols.*;

/**
 * Shared language word string constants starting with 'T'.
 */
@SuppressWarnings({"ClassWithoutLogger", "StaticMethodOnlyUsedInOneClass", "AutoBoxing", "WeakerAccess"})
public record T() {

    /**
     * String constant for "timestamp".
     */
    public static final String TIMESTAMP = s(TIME, ST, A, MP);

    /**
     * String constant for "timestamps".
     */
    public static final String TIMESTAMPS = s(TIMESTAMP, S);
}
