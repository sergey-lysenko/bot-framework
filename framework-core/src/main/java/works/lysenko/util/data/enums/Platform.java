package works.lysenko.util.data.enums;

import works.lysenko.util.apis.data._String;
import works.lysenko.util.lang.word.A;

import static works.lysenko.util.chrs.__.*;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.spec.Symbols.X;

/**
 * @author Sergii Lysenko
 */
@SuppressWarnings("EnumClass")
public enum Platform implements _String {
    /**
     * Chrome
     */
    CHROME(s(c(CH), RO, ME)),

    /**
     * Firefox
     */
    FIREFOX(s(c(FI), RE, FO, X)),

    /**
     * Edge
     */
    EDGE(s(c(ED), GE)),

    /**
     * Android
     */
    ANDROID(s(c(A.ANDROID))),

    /**
     * Safari
     */
    SAFARI(s(c(SA), FA, RI));

    private final String title;

    Platform(final String title) {

        this.title = title;
    }

    /**
     * Retrieves the value based on the provided string.
     *
     * @param value the string representation of the platform
     * @return the corresponding value
     */
    public static Platform get(final String value) {

        return switch (value) {
            case "Firefox" -> // NON-NLS
                    FIREFOX;
            case "Edge" -> // NON-NLS
                    EDGE;
            case "Safari" -> // NON-NLS
                    SAFARI;
            case "Android" -> // NON-NLS
                    ANDROID;
            default -> CHROME;
        };
    }

    /**
     * @return title of this Platform
     */
    @SuppressWarnings("SuspiciousGetterSetter")
    public String getString() {

        return title;
    }
}
