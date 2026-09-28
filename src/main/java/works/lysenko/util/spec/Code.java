package works.lysenko.util.spec;

/**
 * Code snippets
 */
@SuppressWarnings({"ClassWithoutLogger", "MissingJavadoc", "StaticMethodOnlyUsedInOneClass"})
public record Code() {

    public static final String JS_FIND_CODE = "arguments[0].scrollIntoView(true);"; //NON-NLS
    public static final String AUTOMATOR_CODE_PART1 = "new UiScrollable(new UiSelector().scrollable(true))"; //NON-NLS
    public static final String AUTOMATOR_CODE_PART2 = ".scrollIntoView(new UiSelector().textContains(\""; //NON-NLS
    public static final String AUTOMATOR_CODE_PART3 = "\"))";
}
