package works.lysenko.base.output;

import java.util.Map;
import java.util.Set;

/**
 * Immutable snapshot of the "Known Issues summary" and fixed-issue regression verification produced at the end of a run.
 *
 * @param confirmed         active known issues reproduced during the run, keyed by reference
 * @param notReproduced     active known issues whose scenario executed but which did not reproduce
 * @param notChecked        active known issues whose scenario was not executed at all
 * @param regressed         fixed issues that regressed at failure severity (S0/S1), keyed by reference
 * @param regressedWarnings fixed issues that regressed at warning/notice severity (S2/S3/untyped), keyed by reference
 * @param verifiedFixed     fixed issues whose scenario executed and did not reproduce
 * @param fixedNotChecked   fixed issues whose scenario was not executed at all
 */
record KnownIssuesSummary(
        Map<String, Entry> confirmed,
        Map<String, Entry> notReproduced,
        Map<String, Entry> notChecked,
        Map<String, Entry> regressed,
        Map<String, Entry> regressedWarnings,
        Map<String, Entry> verifiedFixed,
        Map<String, Entry> fixedNotChecked) {

    KnownIssuesSummary(
            final Map<String, Entry> confirmed,
            final Map<String, Entry> notReproduced,
            final Map<String, Entry> notChecked) {
        this(confirmed, notReproduced, notChecked, Map.of(), Map.of(), Map.of(), Map.of());
    }

    boolean hasFixedIssues() {
        return !regressed.isEmpty() || !regressedWarnings.isEmpty()
                || !verifiedFixed.isEmpty() || !fixedNotChecked.isEmpty();
    }

    /**
     * A single issue reference with optional metadata and the tests in which it was reproduced.
     *
     * @param ref           display reference (link, title, scenario or pattern, whichever is first available)
     * @param link          tracker link, possibly empty
     * @param title         issue title, possibly empty
     * @param description   issue description, possibly empty
     * @param tests         ordered test numbers that reproduced the issue (empty unless confirmed/regressed)
     * @param resolvedAt    optional ISO-8601 resolution timestamp (for fixed issues)
     * @param verifiedInRun optional run ID where fix was verified (for fixed issues)
     */
    record Entry(
            String ref,
            String link,
            String title,
            String description,
            Set<Integer> tests,
            String resolvedAt,
            String verifiedInRun) {

        Entry(
                final String ref,
                final String link,
                final String title,
                final String description,
                final Set<Integer> tests) {
            this(ref, link, title, description, tests, "", "");
        }
    }
}
