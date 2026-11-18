package works.lysenko.util.spec;

/**
 * Container for reusable code snippets and script fragments used in automation and web interaction.
 */
@SuppressWarnings({"ClassWithoutLogger", "StaticMethodOnlyUsedInOneClass"})
public record Code() {

    /** JavaScript snippet to scroll an element into view. */
    public static final String JS_FIND_CODE = "arguments[0].scrollIntoView(true);"; //NON-NLS

    /** First part of UiAutomator scrollable selector code. */
    public static final String AUTOMATOR_CODE_PART1 = "new UiScrollable(new UiSelector().scrollable(true))"; //NON-NLS

    /** Second part of UiAutomator scrollable selector code with text contains predicate. */
    public static final String AUTOMATOR_CODE_PART2 = ".scrollIntoView(new UiSelector().textContains(\""; //NON-NLS

    /** Third part of UiAutomator scrollable selector code closing parenthesis. */
    public static final String AUTOMATOR_CODE_PART3 = "\"))";
}
