package works.lysenko.base.issues;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class KnownIssuesMatcherTest {

    @Test
    void testMatchesScenario() {
        assertTrue(KnownIssuesMatcher.matchesScenario("Login", "com.example.Login", "Login"));
        assertTrue(KnownIssuesMatcher.matchesScenario("com.example.Login", "com.example.Login", "Login"));
        assertTrue(KnownIssuesMatcher.matchesScenario("*.Login", "com.example.Login", "Login"));
        assertFalse(KnownIssuesMatcher.matchesScenario("Logout", "com.example.Login", "Login"));
    }

    @Test
    void testMatchesScenarioInText() {
        assertTrue(KnownIssuesMatcher.matchesScenarioInText("Login", "Error occurred in Login scenario"));
        assertTrue(KnownIssuesMatcher.matchesScenarioInText("Log*", "Error in Login"));
        assertFalse(KnownIssuesMatcher.matchesScenarioInText("Logout", "Error in Login"));
    }
}
