package works.lysenko.util.prop.issues;

import static works.lysenko.util.prop.PropEnum._TEST_KNOWN_ISSUES_FAILED_ONLY;

/**
 * Known issues configuration properties.
 */
@SuppressWarnings("NonFinalStaticVariableUsedInClassInitialization")
public record KnownIssues() {

    /**
     * Indicates whether only failed known issues should be processed or reported.
     */
    public static boolean failedOnly = _TEST_KNOWN_ISSUES_FAILED_ONLY.get();

    /**
     * Refreshes static fields from current test properties.
     */
    public static void refresh() {

        failedOnly = _TEST_KNOWN_ISSUES_FAILED_ONLY.get();
    }
}
