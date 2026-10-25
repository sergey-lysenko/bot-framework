package works.lysenko.util.test;

import org.junit.jupiter.api.Test;
import works.lysenko.Base;
import works.lysenko.base.TestProperties;
import works.lysenko.testing.SelectionFixture;

import java.lang.reflect.Method;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for the {@link Colour} record.
 */
class ColourTest {

    @Test
    void testRecordInstantiation() {
        final Colour colour = new Colour();
        assertNotNull(colour);
    }

    @Test
    void testVerifyUndefinedColours() {
        try (final SelectionFixture fixture = SelectionFixture.install()) {
            Base.properties = new TestProperties();
            assertDoesNotThrow(Colour::verifyUndefinedColours);
        } finally {
            Base.properties = null;
        }
    }

    @Test
    void testVerifyGridUndefinedColoursWithInvalidPath() {
        try (final SelectionFixture fixture = SelectionFixture.install()) {
            Base.properties = new TestProperties();
            final Method method = Colour.class.getDeclaredMethod("verifyGridUndefinedColours", String.class, Collection.class);
            method.setAccessible(true);
            assertThrows(Exception.class, () -> method.invoke(null, "non_existent_file.properties", null));
        } catch (final Exception e) {
            assertNotNull(e);
        } finally {
            Base.properties = null;
        }
    }
}
