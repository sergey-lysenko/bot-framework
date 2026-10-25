package works.lysenko.util.test;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import works.lysenko.Base;
import works.lysenko.base.TestProperties;
import works.lysenko.testing.SelectionFixture;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for the {@link Amounts} record.
 */
class AmountsTest {

    @Test
    void testRecordInstantiation() {
        final Amounts amounts = new Amounts();
        assertNotNull(amounts);
    }

    @Test
    void testVerifyRedefinedAmounts() {
        try (final SelectionFixture fixture = SelectionFixture.install()) {
            Base.properties = new TestProperties();
            assertDoesNotThrow(Amounts::verifyRedefinedAmounts);
        } finally {
            Base.properties = null;
        }
    }

    @Test
    void testVerifyRedefinedColoursAmountsWithValidFile(@TempDir final Path tempDir) throws Exception {
        final Path propFile = tempDir.resolve("test.grid");
        final Properties props = new Properties();
        props.setProperty("cda", "some_value");
        props.setProperty("cdi", "some_order_value");
        try (final var writer = Files.newBufferedWriter(propFile)) {
            props.store(writer, null);
        }

        try (final SelectionFixture fixture = SelectionFixture.install()) {
            Base.properties = new TestProperties();
            final Method method = Amounts.class.getDeclaredMethod("verifyRedefinedColoursAmounts", String.class);
            method.setAccessible(true);
            assertDoesNotThrow(() -> method.invoke(null, propFile.toString()));
        } finally {
            Base.properties = null;
        }
    }

    @Test
    void testVerifyRedefinedColoursAmountsWithoutOrder(@TempDir final Path tempDir) throws Exception {
        final Path propFile = tempDir.resolve("test.grid");
        final Properties props = new Properties();
        props.setProperty("cda", "some_value");
        try (final var writer = Files.newBufferedWriter(propFile)) {
            props.store(writer, null);
        }

        try (final SelectionFixture fixture = SelectionFixture.install()) {
            Base.properties = new TestProperties();
            final Method method = Amounts.class.getDeclaredMethod("verifyRedefinedColoursAmounts", String.class);
            method.setAccessible(true);
            assertDoesNotThrow(() -> method.invoke(null, propFile.toString()));
        } finally {
            Base.properties = null;
        }
    }

    @Test
    void testVerifyRedefinedColoursAmountsWithNonExistentFile() {
        try (final SelectionFixture fixture = SelectionFixture.install()) {
            Base.properties = new TestProperties();
            final Method method = Amounts.class.getDeclaredMethod("verifyRedefinedColoursAmounts", String.class);
            method.setAccessible(true);
            assertThrows(Exception.class, () -> method.invoke(null, "non_existent_file.grid"));
        } catch (final Exception e) {
            assertNotNull(e);
        } finally {
            Base.properties = null;
        }
    }
}
