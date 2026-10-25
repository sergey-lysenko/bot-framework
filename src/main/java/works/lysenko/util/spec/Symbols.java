package works.lysenko.util.spec;

import static works.lysenko.util.func.type.Chars.booleansToChar;

/**
 * Utility record defining character constants constructed bitwise from boolean representations
 * across various bit widths (4-bit, 6-bit, 7-bit, 8-bit, 10-bit, 14-bit, and 16-bit).
 */
@SuppressWarnings({"ClassWithoutLogger", "StaticMethodOnlyUsedInOneClass", "unused", "WeakerAccess"})
public record Symbols() {

    // 4 bits
    /** Line feed character ('\n'). */
    public static final char _LFD_ = booleansToChar(false, true, false, true); // '\n' // TODO: rename to _LF_

    // 6 bits
    /** Space character (' '). */
    public static final char _SPACE_ = booleansToChar(false, false, false, false, false, true); // ' '
    /** Exclamation mark character ('!'). */
    public static final char _EXCL_ = booleansToChar(true, false, false, false, false, true); // '!'
    /** Double quote character ('"'). */
    public static final char DBL_QUO = booleansToChar(false, true, false, false, false, true); // '"'
    /** Number sign / hash character ('#'). */
    public static final char _NUMBR_ = booleansToChar(true, true, false, false, false, true); // '#'
    /** Dollar sign character ('$'). */
    public static final char _DOLLAR = booleansToChar(false, false, true, false, false, true); // '$'
    /** Percent sign character ('%'). */
    public static final char _PRCNT_ = booleansToChar(true, false, true, false, false, true); // '%'
    /** Ampersand character ('&amp;'). */
    public static final char _AMPRS_ = booleansToChar(false, true, true, false, false, true); // '&'
    /** Single quote / apostrophe character ('\''). */
    public static final char _QUOTE_ = booleansToChar(true, true, true, false, false, true); // "'"
    /** Opening parenthesis character ('('). */
    public static final char OPN_BRK = booleansToChar(false, false, false, true, false, true); // '('
    /** Closing parenthesis character (')'). */
    public static final char CLS_BRK = booleansToChar(true, false, false, true, false, true); // ')'
    /** Asterisk character ('*'). */
    public static final char _ASTRS_ = booleansToChar(false, true, false, true, false, true); // '*'
    /** Plus sign character ('+'). */
    public static final char _PLUS_ = booleansToChar(true, true, false, true, false, true); // '+'
    /** Comma character (','). */
    public static final char _COMMA_ = booleansToChar(false, false, true, true, false, true); // ','
    /** Dash / hyphen-minus character ('-'). */
    public static final char _DASH_ = booleansToChar(true, false, true, true, false, true); // '-'
    /** Dot / period character ('.'). */
    public static final char _DOT_ = booleansToChar(false, true, true, true, false, true); // '.'
    /** Slash / solidus character ('/'). */
    public static final char _SLASH_ = booleansToChar(true, true, true, true, false, true); // '/'
    /** Digit '0'. */
    public static final char _0_ = booleansToChar(false, false, false, false, true, true);
    /** Digit '1'. */
    public static final char _1_ = booleansToChar(true, false, false, false, true, true);
    /** Digit '2'. */
    public static final char _2_ = booleansToChar(false, true, false, false, true, true);
    /** Digit '3'. */
    public static final char _3_ = booleansToChar(true, true, false, false, true, true);
    /** Digit '4'. */
    public static final char _4_ = booleansToChar(false, false, true, false, true, true);
    /** Digit '5'. */
    public static final char _5_ = booleansToChar(true, false, true, false, true, true);
    /** Digit '6'. */
    public static final char _6_ = booleansToChar(false, true, true, false, true, true);
    /** Digit '7'. */
    public static final char _7_ = booleansToChar(true, true, true, false, true, true);
    /** Digit '8'. */
    public static final char _8_ = booleansToChar(false, false, false, true, true, true);
    /** Digit '9'. */
    public static final char _9_ = booleansToChar(true, false, false, true, true, true);
    /** Colon character (':'). */
    public static final char _COLON_ = booleansToChar(false, true, false, true, true, true); // ':'
    /** Semicolon character (';'). */
    public static final char SEM_CLN = booleansToChar(true, true, false, true, true, true); // ';'
    /** Less-than sign character ('&lt;'). */
    public static final char LSS_THN = booleansToChar(false, false, true, true, true, true); // '<'
    /** Equals sign character ('='). */
    public static final char _EQUAL_ = booleansToChar(true, false, true, true, true, true); // '='
    /** Greater-than sign character ('&gt;'). */
    public static final char GRT_THN = booleansToChar(false, true, true, true, true, true); // '>'
    /** Question mark character ('?'). */
    public static final char QUS_MRK = booleansToChar(true, true, true, true, true, true); // '?'

    // 7 bits
    /** At sign character ('@'). */
    public static final char _AT_SGN = booleansToChar(false, false, false, false, false, false, true); // '@'
    /** Opening square bracket character ('['). */
    public static final char OPN_SBR = booleansToChar(true, true, false, true, true, false, true); // '['
    /** Rewritten solidus / backslash character ('\'). */
    public static final char REW_SOL = booleansToChar(false, false, true, true, true, false, true); // '\'
    /** Closing square bracket character (']'). */
    public static final char CLS_SBR = booleansToChar(true, false, true, true, true, false, true); // ']'
    /** Circumflex accent character ('^'). */
    public static final char CRCMFLX = booleansToChar(false, true, true, true, true, false, true); // '^'
    /** Underscore character ('_'). */
    public static final char UND_SCR = booleansToChar(true, true, true, true, true, false, true); // '_'
    /** Uppercase letter 'A'. */
    public static final char A = booleansToChar(true, false, false, false, false, true, true);
    /** Uppercase letter 'B'. */
    public static final char B = booleansToChar(false, true, false, false, false, true, true);
    /** Uppercase letter 'C'. */
    public static final char C = booleansToChar(true, true, false, false, false, true, true);
    /** Uppercase letter 'D'. */
    public static final char D = booleansToChar(false, false, true, false, false, true, true);
    /** Uppercase letter 'E'. */
    public static final char E = booleansToChar(true, false, true, false, false, true, true);
    /** Uppercase letter 'F'. */
    public static final char F = booleansToChar(false, true, true, false, false, true, true);
    /** Uppercase letter 'G'. */
    public static final char G = booleansToChar(true, true, true, false, false, true, true);
    /** Uppercase letter 'H'. */
    public static final char H = booleansToChar(false, false, false, true, false, true, true);
    /** Uppercase letter 'I'. */
    public static final char I = booleansToChar(true, false, false, true, false, true, true);
    /** Uppercase letter 'J'. */
    public static final char J = booleansToChar(false, true, false, true, false, true, true);
    /** Uppercase letter 'K'. */
    public static final char K = booleansToChar(true, true, false, true, false, true, true);
    /** Uppercase letter 'L'. */
    public static final char L = booleansToChar(false, false, true, true, false, true, true);
    /** Uppercase letter 'M'. */
    public static final char M = booleansToChar(true, false, true, true, false, true, true);
    /** Uppercase letter 'N'. */
    public static final char N = booleansToChar(false, true, true, true, false, true, true);
    /** Uppercase letter 'O'. */
    public static final char O = booleansToChar(true, true, true, true, false, true, true);
    /** Uppercase letter 'P'. */
    public static final char P = booleansToChar(false, false, false, false, true, true, true);
    /** Uppercase letter 'Q'. */
    public static final char Q = booleansToChar(true, false, false, false, true, true, true);
    /** Uppercase letter 'R'. */
    public static final char R = booleansToChar(false, true, false, false, true, true, true);
    /** Uppercase letter 'S'. */
    public static final char S = booleansToChar(true, true, false, false, true, true, true);
    /** Uppercase letter 'T'. */
    public static final char T = booleansToChar(false, false, true, false, true, true, true);
    /** Uppercase letter 'U'. */
    public static final char U = booleansToChar(true, false, true, false, true, true, true);
    /** Uppercase letter 'V'. */
    public static final char V = booleansToChar(false, true, true, false, true, true, true);
    /** Uppercase letter 'W'. */
    public static final char W = booleansToChar(true, true, true, false, true, true, true);
    /** Uppercase letter 'X'. */
    public static final char X = booleansToChar(false, false, false, true, true, true, true);
    /** Uppercase letter 'Y'. */
    public static final char Y = booleansToChar(true, false, false, true, true, true, true);
    /** Uppercase letter 'Z'. */
    public static final char Z = booleansToChar(false, true, false, true, true, true, true);
    /** Closing curly brace character ('}'). */
    public static final char CLS_CUB = booleansToChar(true, false, true, true, true, true, true); // '}'
    /** Vertical bar character ('|'). */
    public static final char VRT_BAR = booleansToChar(false, false, true, true, true, true, true); // '|'
    /** Opening curly brace character ('{'). */
    public static final char OPN_CUB = booleansToChar(true, true, false, true, true, true, true); // '{'

    // 8 bits
    /** Ring above / turned letter o / empty set symbol ('Ø'). */
    public static final char Ø = booleansToChar(false, false, false, true, true, false, true, true);
    /** Registered sign character ('®'). */
    public static final char CPY_RHT = booleansToChar(false, true, true, true, false, true, false, true); // '®'

    // 10 bits
    /** Combining long stroke overlay character ('̶'). */
    public static final char _CLS_ = booleansToChar(false, true, true, false, true, true, false, false, true, true); // '̶'

    // 14 bits
    /** Bullet character ('•'). */
    public static final char _BULLT_ = booleansToChar(false, true, false, false, false, true, false, false, false, false,
            false, false, false, true); // '•'
    /** Downwards arrow character ('↓'). */
    public static final char DWN_ARR = booleansToChar(true, true, false, false, true, false, false, true, true, false, false
            , false, false, true); // '↓'
    /** Leftwards arrow character ('←'). */
    public static final char LFT_ARR = booleansToChar(false, false, false, false, true, false, false, true, true, false,
            false, false, false, true); // '←'
    /** Rightwards arrow character ('→'). */
    public static final char RHT_ARR = booleansToChar(false, true, false, false, true, false, false, true, true, false,
            false, false, false, true); // '→'
    /** Upwards arrow character ('↑'). */
    public static final char _UP_ARR = booleansToChar(true, false, false, false, true, false, false, true, true, false,
            false, false, false, true); // '↑'
    /** Leftwards arrow with double stroke character ('⇐'). */
    public static final char LFT_DAR = booleansToChar(false, false, false, false, true, false, true, true, true, false,
            false, false, false, true); // '⇐'
    /** Rightwards arrow with double stroke character ('⇒'). */
    public static final char RGT_DAR = booleansToChar(false, true, false, false, true, false, true, true, true, false, false
            , false, false, true); // '⇒'

    // 16 bits
    /** Section sign character ('§'). */
    public static final char _ABSATZ = booleansToChar(true, true, true, false, false, true, false, true, false, false, false
            , false, false, false, false, false); // '§'
    /** En dash character ('–'). */
    public static final char EN_DASH = booleansToChar(true, true, false, false, true, false, false, false, false, false,
            false, false, false, true, false, false); // '–'
    /** Black circle character ('●'). */
    public static final char FAT_BUL = booleansToChar(true, true, true, true, false, false, true, true, true, false, true,
            false, false, true, false, false); // '●'
    /** Empty set character ('∅'). */
    public static final char EMP_SET = booleansToChar(true, false, true, false, false, false, false, false, false, true,
            false, false, false, true, false, false); // '∅'
    /** Fraction slash character ('⁄'). */
    public static final char FRC_SLS = booleansToChar(false, false, true, false, false, false, true, false, false, false,
            false, false, false, true, false, false); // '⁄'
    /** For all quantifier character ('∀'). */
    public static final char FOR_ALL = booleansToChar(false, false, false, false, false, false, false, false, false, true,
            false, false, false, true, false, false); // '∀'
    /** Sparkles character ('✨'). */
    public static final char SPARKLES = booleansToChar(false, false, false, true, false, true, false, false, true, true,
            true, false, false, true, false, false); // '✨'
    /** Horizontal ellipsis character ('…'). */
    public static final char ELLIPSIS = booleansToChar(false, true, true, false, false, true, false, false, false, false,
            false, false, false, true); // '…'
    /** Ballot box with check character ('☑'). */
    public static final char CHECKED_BALLOT = booleansToChar(true, false, false, false, true, false, false, false, false, true,
            true, false, false, true); // '☑'

    /** Alias for black circle character used as data point marker. */
    public static final char DATA_POINT = FAT_BUL;
}
