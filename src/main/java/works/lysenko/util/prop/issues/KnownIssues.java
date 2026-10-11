package works.lysenko.util.prop.issues;

import static works.lysenko.util.spec.PropEnum._TEST_KNOWN_ISSUES_FAILED_ONLY;

/**
 * Known issues configuration properties.
 */
@SuppressWarnings({"MissingJavadoc", "NonFinalStaticVariableUsedInClassInitialization"})
public record KnownIssues() {

    public static boolean failedOnly = Boolean.TRUE.equals(_TEST_KNOWN_ISSUES_FAILED_ONLY.get());

    /**
     * Refreshes static fields from current test properties.
     */
    public static void refresh() {

        failedOnly = Boolean.TRUE.equals(_TEST_KNOWN_ISSUES_FAILED_ONLY.get());
    }
}
