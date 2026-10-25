package works.lysenko.util.spec;

import java.io.File;
import java.nio.file.Path;
import java.util.*;

import static java.util.Objects.isNull;
import static works.lysenko.Base.*;
import static works.lysenko.util.chrs.__.$0;
import static works.lysenko.util.chrs.__.DA;
import static works.lysenko.util.chrs.__.SH;
import static works.lysenko.util.chrs.___.*;
import static works.lysenko.util.chrs.____.*;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Bind.d;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Null.n;
import static works.lysenko.util.data.strs.Swap.f;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.func.type.Objects.isNotNull;
import static works.lysenko.util.func.type.Properties.getStore;
import static works.lysenko.util.lang.word.A.ANFANG;
import static works.lysenko.util.lang.word.A.APPIUM;
import static works.lysenko.util.lang.word.C.*;
import static works.lysenko.util.lang.word.D.*;
import static works.lysenko.util.lang.word.G.GRIDS;
import static works.lysenko.util.lang.word.I.ISSUES;
import static works.lysenko.util.lang.word.K.KNOWN;
import static works.lysenko.util.lang.word.L.LOCATORS;
import static works.lysenko.util.lang.word.N.NOTICE;
import static works.lysenko.util.lang.word.P.PARAMETERS;
import static works.lysenko.util.lang.word.P.PLATFORMS;
import static works.lysenko.util.lang.word.P.PROPERTIES;
import static works.lysenko.util.lang.word.R.RESOURCES;
import static works.lysenko.util.lang.word.R.RESULT;
import static works.lysenko.util.lang.word.S.*;
import static works.lysenko.util.lang.word.T.TARGET;
import static works.lysenko.util.lang.word.T.TELEMETRY;
import static works.lysenko.util.lang.word.T.TESTS;
import static works.lysenko.util.lang.word.U.USERS;
import static works.lysenko.util.lang.word.V.VERSION;
import static works.lysenko.util.lang.word.W.WARNING;
import static works.lysenko.util.spec.Layout.Directories.*;
import static works.lysenko.util.spec.Layout.Paths._RESOURCES_;
import static works.lysenko.util.spec.Layout.Paths._RUNS_;
import static works.lysenko.util.spec.Symbols.*;


/**
 * The Layout class represents the file paths and names used within the application.
 * It defines constants for various file paths and names that are commonly used.
 * These constants are specifically designed for a specific project structure and naming conventions.
 */
@SuppressWarnings({"ClassWithoutLogger", "StaticMethodOnlyUsedInOneClass", "AutoBoxing", "HardCodedStringLiteral", "HardcodedFileSeparator", "unused"})
public record Layout() {

    /** File separator string. */
    public static final String s = File.separator;
    /** Path to test PNG resource. */
    public static final String SRC_MAIN_RESOURCES_TEST_PNG = "src/main/resources/test.png";
    /** Default test path identifier. */
    public static final String TEST_TEST = "test/test";

    /**
     * The Directories class represents a set of predefined directory names used in a file system.
     */
    @SuppressWarnings({"PublicInnerClass"})
    public record Directories() {

        /** Var directory path with separator. */
        public static final String VAR_ = s(VAR, s);
        /** Etc directory path with separator. */
        public static final String ETC_ = s(ETC, s);
        /** Bin directory path with separator. */
        static final String BIN_ = s(BIN, s);
        /** Dash directory path with separator. */
        public static final String DASH_ = s(s(DA, SH), s);
        /** Main directory path with separator. */
        static final String MAIN_ = s(MAIN, s);
        /** Resources directory path with separator. */
        static final String RESOURCES_ = s(RESOURCES, s);
        /** Result directory path with separator. */
        static final String RESULT_ = s(RESULT, s);
        /** Runs directory path with separator. */
        static final String RUNS_ = s(RUNS, s);
        /** Src directory path with separator. */
        static final String SRC_ = s(SRC, s);
        /** Target directory path with separator. */
        static final String TARGET_ = s(TARGET, s);
        /** Suites directory path with separator. */
        static final String SUITES_ = s(SUITES, s);
        /** Grids directory path with separator. */
        static final String GRIDS_ = s(GRIDS, s);
        /** Config directory path with separator. */
        static final String CONFIG_ = s(CONFIG, s);
    }

    /**
     * The Paths class provides constants for file paths used in the application.
     * These paths are constructed by combining other path segments using the '_DASH_' constant.
     * The class also provides static variables for commonly used directories.
     * All variables in this class are final and should not be modified.
     */
    @SuppressWarnings({"PublicInnerClass"})
    public record Paths() {

        /** Dash path separator constant. */
        public static final String _DASH_ = DASH_;
        /** Resources path. */
        public static final String _RESOURCES_ = s(SRC_, MAIN_, RESOURCES_);
        /** Config path. */
        public static final String _CONFIG_ = s(_RESOURCES_, CONFIG_);
        /** Suites path. */
        public static final String _SUITES_ = s(_RESOURCES_, SUITES_);
        /** Grids path. */
        public static final String _GRIDS_ = s(_RESOURCES_, GRIDS_);
        /** Runs path. */
        public static final String _RUNS_ = s(RESULT_, RUNS_);
    }

    /**
     * This class represents a collection of constants related to parts of a software system.
     * It contains various file extensions and name prefixes used in the system.
     */
    @SuppressWarnings({"PublicInnerClass"})
    public record Parts() {

        /** Suite properties file extension. */
        public static final String SUITE_PROPERTIES_EXTENSION = s(_DOT_, SUITE);
        /** Grid file extension. */
        public static final String GRID_EXTENSION = s(_DOT_, GRID);
        /** Test properties parts file extension. */
        public static final String TEST_PROPERTIES_PARTS_EXTENSION = s(_DOT_, PART);
        /** Users pool file extension. */
        public static final String USERS_POOL_EXTENSION = s(_DOT_, USERS);
        /** Standard properties file extension. */
        public static final String _DOT_PROPERTIES = s(_DOT_, PROPERTIES);
        /** Data properties file extension. */
        public static final String _DATA_PROPERTIES = s(_DOT_, DATA, _DOT_PROPERTIES);
    }

    /**
     * Files class contains static methods for generating file names.
     */
    @SuppressWarnings({"PublicInnerClass", "FeatureEnvy", "NestedMethodCall", "ChainedMethodCall", "AutoUnboxing",
            "StaticCollection"})
    public record Files() {

        /** Appium log file path. */
        public static final String APPIUM_LOG_ = s(RESULT_, APPIUM, _DOT_, LOG);
        /** Common configuration file name. */
        public static final String COMMON_CONFIGURATION = b(_DOT_, COMMON, CONFIGURATION);
        /** Default Android application APK path. */
        public static final String DEFAULT_ANDROID_APPLICATION_ = s(BIN_, ANFANG, _DOT_, APK);
        /** Locators path. */
        public static final String LOCATORS_ = s(_RESOURCES_, LOCATORS);
        /** Parameters path. */
        public static final String PARAMETERS_ = s(VAR_, PARAMETERS);
        /** Platforms path. */
        public static final String PLATFORMS_ = s(VAR_, PLATFORMS);
        /** Known issues file name. */
        public static final String KNOWN_ISSUES = s(KNOWN, _SLASH_, ISSUES, _DOT_, JSON);
        /** Known issues path. */
        public static final String KNOWN_ISSUES_ = s(_RESOURCES_, KNOWN_ISSUES);
        static final String KNOWN_COLOURS_ = s(_RESOURCES_, KNOWN, _SLASH_, COLOURS);
        // Containers
        /** Properties container for known colours. */
        public static final Properties knownColours = getStore(s(KNOWN_COLOURS_));

        /**
         * Generates a file name based on a template and a list of values.
         *
         * @param template the template for the file name
         * @param values   the values to be inserted into the template
         * @return the generated file name
         */
        public static String name(final String template, final String... values) {

            final String rootPath = s(_RUNS_, (isNull(parameters)) ? "null" : n("null", parameters.getTest()), _SLASH_);
            final String testsFormat = s($0, s((isNull(core)) ? 1 : n(1, core.getTotalTests())).length(), s(D));
            final int currentTest = (isNull(core)) ? 0 : n(0, core.getCurrentTestNumber());
            final List<String> rawList =
                    (null == values) ? (new ArrayList<>(0)) : (new ArrayList<>(Arrays.asList(values)));
            final long startTs = (isNull(timer)) ? System.currentTimeMillis() : timer.startedAt();
            final long elapsedMs = (isNull(timer)) ? 0L : timer.msSinceStart();
            rawList.add(0, s(startTs));
            rawList.add(1, String.format(testsFormat, currentTest));
            rawList.add(2, String.format(s($0, _9_, D), elapsedMs));

            final Collection<String> list = new ArrayList<>(rawList.size());
            for (final String raw : rawList) {
                list.add(unTaint(raw));
            }

            final String name = s(rootPath, f(template, list.toArray()));
            final Path path = java.nio.file.Paths.get(name);
            if (isNotNull(path.getParent()))
                if (path.getParent().toFile().mkdirs()) logDebug(b(c(CREATED), DIRECTORIES, FOR, name));
            return name;
        }

        private static String unTaint(final String s) {

            return s.replace(File.separator, s(_DASH_));
        }
    }

    /**
     * The Templates class holds a set of string constants representing various file paths or names.
     * Each constant follows a specific naming convention and is declared as a public static final String field.
     */
    @SuppressWarnings({"PublicInnerClass"})
    public record Templates() {

        /** Tests history template. */
        public static final String TESTS_HISTORY_ = s($1_S, s, $1_S, _DOT_, TESTS, _DOT_, HTML);
        /** Data snapshot template. */
        public static final String DATA_SNAPSHOT_ = s($1_S, s, DATA, s, $1_S, _DOT_, $2_S, _DOT_, $3_S, _DOT_, $4_S, _DOT_,
                $5_S);
        /** New issues notice template. */
        public static final String NEW_ISSUES_NOTICE_ = s($1_S, s, $1_S, _DOT_, ISSUES, _DOT_, NOTICE, _DOT_, HTML);
        /** New issues warning template. */
        public static final String NEW_ISSUES_WARNING_ = s($1_S, s, $1_S, _DOT_, ISSUES, _DOT_, WARNING, _DOT_, HTML);
        /** Run JSON template. */
        public static final String RUN_JSON_ = s($1_S, s, $1_S, _DOT_, RUN, _DOT_, JSON);
        /** Run all leaf completions template. */
        public static final String RUN_ALL_LEAF_COMPLETIONS_ = s($1_S, s, $1_S, _DOT_, ALL, _DASH_, LEAF, _DASH_,
                "completions", _DOT_, LOG);
        /** Run ETA debug template. */
        public static final String RUN_ETA_DEBUG_ = s($1_S, s, $1_S, _DOT_, ETA, _DASH_, DEBUG, _DOT_, LOG);
        /** Run log template. */
        public static final String RUN_LOG_ = s($1_S, s, $1_S, _DOT_, RUN, _DOT_, LOG);
        /** Run root template. */
        public static final String RUN_ROOT_ = s($1_S, s, $1_S, _DOT_, $2_S, _DOT_, $3_S, _DOT_, $4_S, _DOT_, $5_S);
        /** Run tree HTML template. */
        public static final String RUN_TREE_HTML_ = s($1_S, s, $1_S, _DOT_, works.lysenko.util.lang.word.T.TREE, _DOT_, HTML);
        /** Run log HTML template. */
        public static final String RUN_LOG_HTML_ = s($1_S, s, $1_S, _DOT_, RUN, _DOT_, LOG, _DOT_, HTML);
        /** Run sequence template. */
        public static final String RUN_SEQUENCE_ = s($1_S, s, $1_S, _DOT_, SCENARIOS, _DOT_, "sequence");
        /** Scenarios statistics template. */
        public static final String SCENARIOS_STATISTICS_ = s($1_S, s, $1_S, _DOT_, SCENARIOS, _DOT_, HTML);
        /** Screenshot template. */
        public static final String SCREENSHOT_ = s($1_S, s, SNAPSHOTS, s, $1_S, _DOT_, $2_S, _DOT_, $3_S, _DOT_, $4_S, _DOT_
                , $5_S);
        /** Telemetry template. */
        public static final String TELEMETRY_ = s($1_S, s, $1_S, _DOT_, TELEMETRY, _DOT_, LOG);
        /** Run stacktraces template. */
        public static final String RUN_STACKTRACES_ = s($1_S, s, $1_S, _DOT_, STACKTRACES, _DOT_, PROPERTIES);
        /** Users pool template. */
        public static final String USERS_POOL_ = s(ETC, s, d(USERS, POOL));
        /** Version template. */
        public static final String VERSION_ = s($1_S, _DOT_, VERSION);
        /** Warp template. */
        public static final String WARP_ = s($1_S, _DOT_, $2_S);
    }
}
