package works.lysenko.util.test;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import works.lysenko.Base;
import works.lysenko.base.TestProperties;
import works.lysenko.testing.SelectionFixture;
import works.lysenko.util.data.enums.PolychromyMethod;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PolychromyTest {

    @Test
    void testRecordInstantiation() {
        final Polychromy polychromy = new Polychromy();
        assertNotNull(polychromy);
    }

    @Test
    void testCalculateMaxPolychromies() {
        try (final SelectionFixture fixture = SelectionFixture.install()) {
            Base.properties = new TestProperties();
            assertDoesNotThrow(Polychromy::calculateMaxPolychromies);
        } finally {
            Base.properties = null;
        }
    }

    @Test
    void testCalculateMaxPolychromyWithValidFile(@TempDir final Path tempDir) throws Exception {
        final Path propFile = tempDir.resolve("test_grid.properties");
        final Properties props = new Properties();
        props.setProperty("polychromy", "true");
        props.setProperty("pdm", String.valueOf(PolychromyMethod.EUCLIDEAN_DISTANCE.ordinal()));
        props.setProperty("resolution", "100x100");
        try (final var writer = Files.newBufferedWriter(propFile)) {
            props.store(writer, null);
        }

        try (final SelectionFixture fixture = SelectionFixture.install()) {
            Base.properties = new TestProperties();
            final Method method = Polychromy.class.getDeclaredMethod("calculateMaxPolychromy", String.class);
            method.setAccessible(true);
            assertDoesNotThrow(() -> method.invoke(null, propFile.toString()));
        } finally {
            Base.properties = null;
        }
    }

    @Test
    void testCalculateMaxPolychromyWithNonExistentFile() {
        try (final SelectionFixture fixture = SelectionFixture.install()) {
            Base.properties = new TestProperties();
            final Method method = Polychromy.class.getDeclaredMethod("calculateMaxPolychromy", String.class);
            method.setAccessible(true);
            assertThrows(Exception.class, () -> method.invoke(null, "non_existent_file.properties"));
        } catch (final Exception e) {
            assertNotNull(e);
        } finally {
            Base.properties = null;
        }
    }
}
