package works.lysenko.util.test;

import org.junit.jupiter.api.Test;
import works.lysenko.Base;
import works.lysenko.base.TestProperties;
import works.lysenko.testing.SelectionFixture;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Unit tests for the {@link Checklist} class.
 */
class ChecklistTest {

    @Test
    void testInstantiation() {
        final Checklist checklist = new Checklist();
        assertNotNull(checklist);
    }

    @Test
    void testRun() {
        try (final SelectionFixture fixture = SelectionFixture.install()) {
            Base.properties = new TestProperties();
            final Checklist checklist = new Checklist();
            assertDoesNotThrow(checklist::run);
        } finally {
            Base.properties = null;
        }
    }
}
