package works.lysenko.base.output;

import java.util.Map;
import java.util.Set;

/**
 * Immutable snapshot of the "Known Issues summary" produced at the end of a run.
 *
 * @param confirmed    issues reproduced during the run, keyed by reference
 * @param notReproduced issues whose scenario executed but which did not reproduce
 * @param notChecked   issues whose scenario was not executed at all
 */
record KnownIssuesSummary(
        Map<String, Entry> confirmed,
        Map<String, Entry> notReproduced,
        Map<String, Entry> notChecked) {

    /**
     * A single issue reference with optional metadata and the tests in which it was reproduced.
     *
     * @param ref         display reference (link, title, scenario or pattern, whichever is first available)
     * @param link        tracker link, possibly empty
     * @param title       issue title, possibly empty
     * @param description issue description, possibly empty
     * @param tests       ordered test numbers that reproduced the issue (empty unless confirmed)
     */
    record Entry(String ref, String link, String title, String description, Set<Integer> tests) {}
}
