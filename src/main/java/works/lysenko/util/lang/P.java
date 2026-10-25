package works.lysenko.util.lang;

import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.lang.word.E.EXCEPTIONAL;
import static works.lysenko.util.lang.word.L.LOCATOR;
import static works.lysenko.util.lang.word.P.PROCESSING;
import static works.lysenko.util.lang.word.S.STATE;

/**
 * Language utility constants starting with P.
 */
@SuppressWarnings({"ClassWithoutLogger", "StaticMethodOnlyUsedInOneClass", "AutoBoxing"})
public record P() {

    /** Constant representing processing exceptional state. */
    public static final String PROCESSING_EXCEPTIONAL_STATE = b(c(PROCESSING), EXCEPTIONAL, STATE);
    /** Constant representing provided locator. */
    public static final String PROVIDED_LOCATOR = b(c(works.lysenko.util.lang.word.P.PROVIDED), LOCATOR);
    /** Constant representing platform-tools directory name. */
    public static final String DIR_PLATFORM_TOOLS = "platform-tools";
    /** Constant representing platforms directory name. */
    public static final String DIR_PLATFORMS = "platforms";
    /** Constant representing port conflict 4723 message. */
    public static final String PORT_CONFLICT_4723 = "Port conflict: port 4723 is already in use (EADDRINUSE)";
    /** Constant representing port conflict generic message. */
    public static final String PORT_CONFLICT_GENERIC = "Port conflict (EADDRINUSE): another Appium instance or process is using this port";
    /** Constant representing macOS SDK path. */
    public static final String PATH_MACOS_SDK = "/Library/Android/sdk";
    /** Constant representing Android SDK path. */
    public static final String PATH_ANDROID_SDK = "/Android/Sdk";
    /** Constant representing SDK path. */
    public static final String PATH_SDK = "/sdk";
    /** Constant representing opt SDK path. */
    public static final String PATH_OPT_SDK = "/opt/android-sdk";
    /** Constant representing usr local SDK path. */
    public static final String PATH_USR_LOCAL_SDK = "/usr/local/share/android-sdk";
    public static final String MSG_PORT_CONFLICT_4723 = PORT_CONFLICT_4723;
    public static final String MSG_PORT_CONFLICT_GENERIC = PORT_CONFLICT_GENERIC;
}



