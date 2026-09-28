package works.lysenko.util.spec;

import static works.lysenko.util.func.type.Chars.booleansToChar;

@SuppressWarnings({"ClassWithoutLogger", "MissingJavadoc", "StaticMethodOnlyUsedInOneClass", "unused", "WeakerAccess"})
public record Symbols() {

    // 4 bits
    public static final char _LFD_ = booleansToChar(false, true, false, true); // '\n' // TODO: rename to _LF_

    // 6 bits
    public static final char _SPACE_ = booleansToChar(false, false, false, false, false, true); // ' '
    public static final char _EXCL_ = booleansToChar(true, false, false, false, false, true); // '!'
    public static final char DBL_QUO = booleansToChar(false, true, false, false, false, true); // '"'
    public static final char _NUMBR_ = booleansToChar(true, true, false, false, false, true); // '#'
    public static final char _DOLLAR = booleansToChar(false, false, true, false, false, true); // '$'
    public static final char _PRCNT_ = booleansToChar(true, false, true, false, false, true); // '%'
    public static final char _AMPRS_ = booleansToChar(false, true, true, false, false, true); // '&'
    public static final char _QUOTE_ = booleansToChar(true, true, true, false, false, true); // "'"
    public static final char OPN_BRK = booleansToChar(false, false, false, true, false, true); // '('
    public static final char CLS_BRK = booleansToChar(true, false, false, true, false, true); // ')'
    public static final char _ASTRS_ = booleansToChar(false, true, false, true, false, true); // '*'
    public static final char _PLUS_ = booleansToChar(true, true, false, true, false, true); // '+'
    public static final char _COMMA_ = booleansToChar(false, false, true, true, false, true); // ','
    public static final char _DASH_ = booleansToChar(true, false, true, true, false, true); // '-'
    public static final char _DOT_ = booleansToChar(false, true, true, true, false, true); // '.'
    public static final char _SLASH_ = booleansToChar(true, true, true, true, false, true); // '/'
    public static final char _0_ = booleansToChar(false, false, false, false, true, true);
    public static final char _1_ = booleansToChar(true, false, false, false, true, true);
    public static final char _2_ = booleansToChar(false, true, false, false, true, true);
    public static final char _3_ = booleansToChar(true, true, false, false, true, true);
    public static final char _4_ = booleansToChar(false, false, true, false, true, true);
    public static final char _5_ = booleansToChar(true, false, true, false, true, true);
    public static final char _6_ = booleansToChar(false, true, true, false, true, true);
    public static final char _7_ = booleansToChar(true, true, true, false, true, true);
    public static final char _8_ = booleansToChar(false, false, false, true, true, true);
    public static final char _9_ = booleansToChar(true, false, false, true, true, true);
    public static final char _COLON_ = booleansToChar(false, true, false, true, true, true); // ':'
    public static final char SEM_CLN = booleansToChar(true, true, false, true, true, true); // ';'
    public static final char LSS_THN = booleansToChar(false, false, true, true, true, true); // '<'
    public static final char _EQUAL_ = booleansToChar(true, false, true, true, true, true); // '='
    public static final char GRT_THN = booleansToChar(false, true, true, true, true, true); // '>'
    public static final char QUS_MRK = booleansToChar(true, true, true, true, true, true); // '?'

    // 7 bits
    public static final char _AT_SGN = booleansToChar(false, false, false, false, false, false, true); // '@'
    public static final char OPN_SBR = booleansToChar(true, true, false, true, true, false, true); // '['
    public static final char REW_SOL = booleansToChar(false, false, true, true, true, false, true); // '\'
    public static final char CLS_SBR = booleansToChar(true, false, true, true, true, false, true); // ']'
    public static final char CRCMFLX = booleansToChar(false, true, true, true, true, false, true); // '^'
    public static final char UND_SCR = booleansToChar(true, true, true, true, true, false, true); // '_'
    public static final char A = booleansToChar(true, false, false, false, false, true, true);
    public static final char B = booleansToChar(false, true, false, false, false, true, true);
    public static final char C = booleansToChar(true, true, false, false, false, true, true);
    public static final char D = booleansToChar(false, false, true, false, false, true, true);
    public static final char E = booleansToChar(true, false, true, false, false, true, true);
    public static final char F = booleansToChar(false, true, true, false, false, true, true);
    public static final char G = booleansToChar(true, true, true, false, false, true, true);
    public static final char H = booleansToChar(false, false, false, true, false, true, true);
    public static final char I = booleansToChar(true, false, false, true, false, true, true);
    public static final char J = booleansToChar(false, true, false, true, false, true, true);
    public static final char K = booleansToChar(true, true, false, true, false, true, true);
    public static final char L = booleansToChar(false, false, true, true, false, true, true);
    public static final char M = booleansToChar(true, false, true, true, false, true, true);
    public static final char N = booleansToChar(false, true, true, true, false, true, true);
    public static final char O = booleansToChar(true, true, true, true, false, true, true);
    public static final char P = booleansToChar(false, false, false, false, true, true, true);
    public static final char Q = booleansToChar(true, false, false, false, true, true, true);
    public static final char R = booleansToChar(false, true, false, false, true, true, true);
    public static final char S = booleansToChar(true, true, false, false, true, true, true);
    public static final char T = booleansToChar(false, false, true, false, true, true, true);
    public static final char U = booleansToChar(true, false, true, false, true, true, true);
    public static final char V = booleansToChar(false, true, true, false, true, true, true);
    public static final char W = booleansToChar(true, true, true, false, true, true, true);
    public static final char X = booleansToChar(false, false, false, true, true, true, true);
    public static final char Y = booleansToChar(true, false, false, true, true, true, true);
    public static final char Z = booleansToChar(false, true, false, true, true, true, true);
    public static final char CLS_CUB = booleansToChar(true, false, true, true, true, true, true); // '}'
    public static final char VRT_BAR = booleansToChar(false, false, true, true, true, true, true); // '|'
    public static final char OPN_CUB = booleansToChar(true, true, false, true, true, true, true); // '{'

    // 8 bits
    public static final char Ø = booleansToChar(false, false, false, true, true, false, true, true);
    public static final char CPY_RHT = booleansToChar(false, true, true, true, false, true, false, true); // '®'

    // 10 bits
    public static final char _CLS_ = booleansToChar(false, true, true, false, true, true, false, false, true, true); // '̶'

    // 14 bits
    public static final char _BULLT_ = booleansToChar(false, true, false, false, false, true, false, false, false, false,
            false, false, false, true); // '•'
    public static final char DWN_ARR = booleansToChar(true, true, false, false, true, false, false, true, true, false, false
            , false, false, true); // '↓'
    public static final char LFT_ARR = booleansToChar(false, false, false, false, true, false, false, true, true, false,
            false, false, false, true); // '←'
    public static final char RHT_ARR = booleansToChar(false, true, false, false, true, false, false, true, true, false,
            false, false, false, true); // '→'
    public static final char _UP_ARR = booleansToChar(true, false, false, false, true, false, false, true, true, false,
            false, false, false, true); // '↑'
    public static final char LFT_DAR = booleansToChar(false, false, false, false, true, false, true, true, true, false,
            false, false, false, true); // '⇐'
    public static final char RGT_DAR = booleansToChar(false, true, false, false, true, false, true, true, true, false, false
            , false, false, true); // '⇒'

    // 16 bits
    public static final char _ABSATZ = booleansToChar(true, true, true, false, false, true, false, true, false, false, false
            , false, false, false, false, false); // '§'
    public static final char EN_DASH = booleansToChar(true, true, false, false, true, false, false, false, false, false,
            false, false, false, true, false, false); // '–'
    public static final char FAT_BUL = booleansToChar(true, true, true, true, false, false, true, true, true, false, true,
            false, false, true, false, false); // '●'
    public static final char EMP_SET = booleansToChar(true, false, true, false, false, false, false, false, false, true,
            false, false, false, true, false, false); // '∅'
    public static final char FRC_SLS = booleansToChar(false, false, true, false, false, false, true, false, false, false,
            false, false, false, true, false, false); // '⁄'
    public static final char FOR_ALL = booleansToChar(false, false, false, false, false, false, false, false, false, true,
            false, false, false, true, false, false); // '∀'

    public static final char DATA_POINT = FAT_BUL;
}
