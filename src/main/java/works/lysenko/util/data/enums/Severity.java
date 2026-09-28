package works.lysenko.util.data.enums;

import works.lysenko.util.apis.util._Severity;

import static java.util.Objects.isNull;
import static org.apache.commons.math3.util.FastMath.min;
import static works.lysenko.util.data.enums.EventType.*;

/**
 * Severity of indicated events
 *
 * @author Sergii Lysenko
 */
@SuppressWarnings("EnumClass")
public enum Severity implements _Severity {

    /**
     * FAILURE
     */
    S0(FAILURE, Ansi.MAGENTA),

    /**
     * Severe
     */
    S1(SEVERE, Ansi.RED),

    /**
     * Warning
     */
    S2(WARNING, Ansi.YELLOW),

    /**
     * Notice
     */
    S3(NOTICE, Ansi.CYAN),

    /**
     * Known Issue
     */
    SK(KNOWN_ISSUE, Ansi.BLUE);

    private final EventType type;
    private final Ansi color;

    Severity(final EventType type, final Ansi color) {

        this.type = type;
        this.color = color;
    }

    /**
     * Retrieves the Severity based on the given code.
     *
     * @param code The code representing the severity level.
     * @return The Severity corresponding to the given code.
     */
    public static Severity byCode(final int code) {

        return values()[code];
    }

    /**
     * @param ansi Color of Severity
     * @return Severity for this Color
     */
    @SuppressWarnings({"unused", "MethodWithMultipleReturnPoints"})
    public static Severity byColor(final Ansi ansi) {

        for (final Severity e : values())
            if (e.color() == ansi)
                return e;
        return null;
    }

    /**
     * @param type of Event
     * @return Severity for this Code
     */
    @SuppressWarnings("MethodWithMultipleReturnPoints")
    public static Severity byType(final EventType type) {

        for (final Severity e : values())
            if (e.type() == type)
                return e;
        return null;
    }

    /**
     * @param a Severity
     * @param b Severity
     * @return Severity of bigger magnitude
     */
    @SuppressWarnings({"MethodWithMultipleReturnPoints", "StandardVariableNames"})
    public static Severity greaterSeverity(final Severity a, final Severity b) {

        if (isNull(a)) return b;
        if (isNull(b)) return a;
        return values()[min(a.ordinal(), b.ordinal())];
    }

    /**
     * @return Color of this Severity
     */
    public Ansi color() {

        return color;
    }

    /**
     * @return Tag of this Severity
     */
    public EventType type() {

        return type;
    }
}