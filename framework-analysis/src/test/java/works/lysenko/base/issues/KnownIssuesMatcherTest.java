package works.lysenko.base.issues;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import works.lysenko.util.apis.log._LogRecord;
import works.lysenko.util.data.enums.EventType;
import works.lysenko.util.data.type.LogRecord;
import works.lysenko.util.data.type.logr.type.Event;

import static org.junit.jupiter.api.Assertions.*;

class KnownIssuesMatcherTest {

    @Test
    @DisplayName("Enforces dot-delimited package boundaries and exact/wildcard scenario matching")
    void testMatchesScenario() {
        assertTrue(KnownIssuesMatcher.matchesScenario("Login", "com.example.Login", "Login"));
        assertTrue(KnownIssuesMatcher.matchesScenario("com.example.Login", "com.example.Login", "Login"));
        assertTrue(KnownIssuesMatcher.matchesScenario("example.Login", "com.example.Login", "Login"));
        assertTrue(KnownIssuesMatcher.matchesScenario("*.Login", "com.example.Login", "Login"));
        assertTrue(KnownIssuesMatcher.matchesScenario("*", "com.example.Login", "Login"));
        assertTrue(KnownIssuesMatcher.matchesScenario("Pretest", "Pretest", "Pretest"));

        assertFalse(KnownIssuesMatcher.matchesScenario("Logout", "com.example.Login", "Login"));
        assertFalse(KnownIssuesMatcher.matchesScenario("", "com.example.Login", "Login"));
        assertFalse(KnownIssuesMatcher.matchesScenario(null, "com.example.Login", "Login"));

        // Must NOT match across identifier boundaries without dot delimiter
        assertFalse(KnownIssuesMatcher.matchesScenario("Save", "biteheist.tree.partners.SpamSave", "SpamSave"));
        assertFalse(KnownIssuesMatcher.matchesScenario("*.Save", "biteheist.tree.partners.SpamSave", "SpamSave"));
        assertFalse(KnownIssuesMatcher.matchesScenario("com.example.*", "notcom.example.Login", "Login"));
        // Must NOT reverse-match a shorter FQN against a more specific target path
        assertFalse(KnownIssuesMatcher.matchesScenario("partners.addNew.Save", "Save", "Save"));
        assertFalse(KnownIssuesMatcher.matchesScenario("partners.addNew.Save", "biteheist.tree.settings.Save", "Save"));
    }

    @Test
    @DisplayName("Rejects blank patterns and global wildcard catch-all patterns")
    void testCatchAllAndBlankPatternRejection() {
        final _LogRecord lr = new LogRecord(1, 1000L, new Event(1, EventType.FAILURE, 0, "Any error text", null));

        assertFalse(KnownIssuesMatcher.matchesPatternForRecord(null, lr));
        assertFalse(KnownIssuesMatcher.matchesPatternForRecord("", lr));
        assertFalse(KnownIssuesMatcher.matchesPatternForRecord("   ", lr));

        assertTrue(KnownIssuesMatcher.isCatchAllRule("*", ""));
        assertTrue(KnownIssuesMatcher.isCatchAllRule("*", ".*"));
        assertTrue(KnownIssuesMatcher.isCatchAllRule("*", ".+"));
        assertTrue(KnownIssuesMatcher.isCatchAllRule("*", "^.*$"));
        assertTrue(KnownIssuesMatcher.isCatchAllRule("*", "foo|"));
        assertTrue(KnownIssuesMatcher.isCatchAllRule("Login", ""));
        assertTrue(KnownIssuesMatcher.isCatchAllRule("", "TimeoutException"));

        assertFalse(KnownIssuesMatcher.isCatchAllRule("*", "loading=async"));
        assertFalse(KnownIssuesMatcher.isCatchAllRule("*", "SensorNotRequired"));
        assertFalse(KnownIssuesMatcher.isCatchAllRule("Login", "TimeoutException"));
    }

    @Test
    @DisplayName("Matches via regex or case-insensitive substring without loose alphanumeric stripping")
    void testPatternMatchingPrecision() {
        final _LogRecord lr = new LogRecord(1, 1000L, new Event(1, EventType.FAILURE, 0, "e_r_r_o_r 1 2 3 happened", null));
        assertFalse(KnownIssuesMatcher.matchesPatternForRecord("error-123", lr),
                "Loose alphanumeric stripping must not cause false matches");

        final _LogRecord lrExact = new LogRecord(1, 1000L, new Event(1, EventType.FAILURE, 0, "API failed with ERROR-123", null));
        assertTrue(KnownIssuesMatcher.matchesPatternForRecord("error-123", lrExact));
        assertTrue(KnownIssuesMatcher.matchesPatternForRecord("API.*ERROR-\\d+", lrExact));
    }

    @Test
    void testMatchesScenarioInText() {
        assertTrue(KnownIssuesMatcher.matchesScenarioInText("Login", "Error occurred in Login scenario"));
        assertTrue(KnownIssuesMatcher.matchesScenarioInText("Log*", "Error in Login"));
        assertFalse(KnownIssuesMatcher.matchesScenarioInText("Logout", "Error in Login"));
    }
}
