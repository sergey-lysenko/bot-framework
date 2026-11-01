package works.lysenko.util.lang;

import static works.lysenko.util.chrs.__.AT;
import static works.lysenko.util.chrs.__.IN;
import static works.lysenko.util.chrs.__.IS;
import static works.lysenko.util.chrs.__.OF;
import static works.lysenko.util.chrs.__.OR;
import static works.lysenko.util.chrs.____.THIS;
import static works.lysenko.util.data.enums.Brackets.ROUND;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Bind.d;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Case.u;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.data.strs.Wrap.e;
import static works.lysenko.util.data.strs.Wrap.q;
import static works.lysenko.util.lang.word.A.ALREADY;
import static works.lysenko.util.lang.word.A.ANDROID;
import static works.lysenko.util.lang.word.A.ANOTHER;
import static works.lysenko.util.lang.word.A.APPIUM;
import static works.lysenko.util.lang.word.C.CONFLICT;
import static works.lysenko.util.lang.word.E.EADDRINUSE;
import static works.lysenko.util.lang.word.E.EXCEPTIONAL;
import static works.lysenko.util.lang.word.I.INSTANCE;
import static works.lysenko.util.lang.word.L.LIBRARY;
import static works.lysenko.util.lang.word.L.LOCAL;
import static works.lysenko.util.lang.word.L.LOCATOR;
import static works.lysenko.util.lang.word.O.OPT;
import static works.lysenko.util.lang.word.P.*;
import static works.lysenko.util.lang.word.S.SDK;
import static works.lysenko.util.lang.word.S.SHARE;
import static works.lysenko.util.lang.word.S.STATE;
import static works.lysenko.util.lang.word.T.TOOLS;
import static works.lysenko.util.lang.word.U.USE;
import static works.lysenko.util.lang.word.U.USING;
import static works.lysenko.util.lang.word.U.USR;
import static works.lysenko.util.spec.Symbols.D;
import static works.lysenko.util.spec.Symbols.S;
import static works.lysenko.util.spec.Symbols._2_;
import static works.lysenko.util.spec.Symbols._3_;
import static works.lysenko.util.spec.Symbols._4_;
import static works.lysenko.util.spec.Symbols._7_;
import static works.lysenko.util.spec.Symbols._COLON_;
import static works.lysenko.util.spec.Symbols._PRCNT_;
import static works.lysenko.util.spec.Symbols._SLASH_;

/**
 * Language utility constants starting with P.
 */
@SuppressWarnings({"ClassWithoutLogger", "StaticMethodOnlyUsedInOneClass", "AutoBoxing", "WeakerAccess"})
public record P() {

    private static final String ANDROID_SDK = d(ANDROID, SDK);
    private static final String _EADDRINUSE_ = e(ROUND, u(EADDRINUSE));
    private static final String _S = s(_PRCNT_, S);

    /** Format template for wandering patience progress debug messages. */
    public static final String PATIENCE_AT_S_D_OF_S = b(c(PATIENCE), AT, q(s(_S, _PRCNT_, _PRCNT_)),
            e(ROUND, b(s(_PRCNT_, D), OF, _S)));
    /** Constant representing processing exceptional state. */
    public static final String PROCESSING_EXCEPTIONAL_STATE = b(c(PROCESSING), EXCEPTIONAL, STATE);
    /** Constant representing provided locator. */
    public static final String PROVIDED_LOCATOR = b(c(PROVIDED), LOCATOR);
    /** Constant representing platform-tools directory name. */
    public static final String DIR_PLATFORM_TOOLS = d(PLATFORM, TOOLS);
    /** Constant representing platforms directory name. */
    public static final String DIR_PLATFORMS = PLATFORMS;
    /** Constant representing port conflict 4723 message. */
    public static final String PORT_CONFLICT_4723 = b(c(PORT), s(CONFLICT, _COLON_), PORT, s(_4_, _7_, _2_, _3_),
            IS, ALREADY, IN, USE, _EADDRINUSE_);
    /** Constant representing port conflict generic message. */
    public static final String PORT_CONFLICT_GENERIC = b(c(PORT), CONFLICT, s(_EADDRINUSE_, _COLON_), ANOTHER,
            c(APPIUM), INSTANCE, OR, PROCESS, IS, USING, THIS, PORT);
    /** Constant representing macOS SDK path. */
    public static final String PATH_MACOS_SDK = s(_SLASH_, b(_SLASH_, c(LIBRARY), c(ANDROID), SDK));
    /** Constant representing Android SDK path. */
    public static final String PATH_ANDROID_SDK = s(_SLASH_, b(_SLASH_, c(ANDROID), c(SDK)));
    /** Constant representing SDK path. */
    public static final String PATH_SDK = s(_SLASH_, SDK);
    /** Constant representing opt SDK path. */
    public static final String PATH_OPT_SDK = s(_SLASH_, b(_SLASH_, OPT, ANDROID_SDK));
    /** Constant representing usr local SDK path. */
    public static final String PATH_USR_LOCAL_SDK = s(_SLASH_, b(_SLASH_, USR, LOCAL, SHARE, ANDROID_SDK));
    /** Constant representing port conflict 4723 diagnostic message. */
    public static final String MSG_PORT_CONFLICT_4723 = PORT_CONFLICT_4723;
    /** Constant representing port conflict generic diagnostic message. */
    public static final String MSG_PORT_CONFLICT_GENERIC = PORT_CONFLICT_GENERIC;
}
