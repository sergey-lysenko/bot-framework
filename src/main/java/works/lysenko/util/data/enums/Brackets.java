package works.lysenko.util.data.enums;

import works.lysenko.util.apis.util._Brackets;

import static works.lysenko.util.spec.Symbols.*;

/**
 * The Brackets enum represents different types of brackets along with their opening and closing characters.
 */
@SuppressWarnings("EnumClass")
public enum Brackets implements _Brackets {

    /**
     * The `ROUND` variable represents the `ROUND` bracket type along with its opening and closing characters.
     * <p>
     * Opening character: '('
     * Closing character: ')'
     */
    ROUND(OPN_BRK, CLS_BRK),
    /**
     * The SQUARE variable represents the SQUARE bracket type along with its opening and closing characters.
     * <p>
     * Opening character: '['
     * Closing character: ']'
     */
    SQUARE(OPN_SBR, CLS_SBR),
    /**
     * The CURLY variable represents the CURLY bracket type along with its opening and closing characters.
     * <p>
     * Opening character: '{'
     * Closing character: '}'
     */
    CURLY(OPN_CUB, CLS_CUB);

    private final char opening;
    private final char closing;

    @SuppressWarnings("ParameterHidesMemberVariable")
    Brackets(final char opening, final char closing) {

        this.opening = opening;
        this.closing = closing;
    }

    /**
     * Returns the closing character of a bracket type.
     *
     * @return The closing character of the bracket type.
     */
    public char closing() {

        return closing;
    }

    /**
     * Returns the opening character of a bracket type.
     *
     * @return The opening character of the bracket type.
     */
    public char opening() {

        return opening;
    }
}
