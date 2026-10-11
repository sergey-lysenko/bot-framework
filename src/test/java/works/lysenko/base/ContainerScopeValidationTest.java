package works.lysenko.base;

import org.junit.jupiter.api.Test;
import works.lysenko.util.data.records.TestPropertiesDescriptor;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContainerScopeValidationTest {

    @Test
    void commonConfigurationAcceptsOnlyDottedKeysAndIncludes() {
        final TestProperties testProperties = new TestProperties();
        assertDoesNotThrow(testProperties::readCommonConfiguration);
    }

    @Test
    void throwsExceptionWhenPartFileContainsBareScenarioReference() throws IOException {
        final Path tempDir = java.nio.file.Files.createTempDirectory("part_test");
        final File includeFile = new File(tempDir.toFile(), "invalid.part");
        try (final FileWriter writer = new FileWriter(includeFile)) {
            writer.write(".test.headless=true\n");
            writer.write("SignIn=1\n"); // Invalid bare scenario reference in part file
        }

        final File parentFile = new File(tempDir.toFile(), "parent.properties");
        try (final FileWriter writer = new FileWriter(parentFile)) {
            writer.write(".include=invalid\n");
        }

        final TestPropertiesDescriptor descriptor = new TestPropertiesDescriptor(
                tempDir.toAbsolutePath() + File.separator, "parent", ".properties"
        );

        final IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                works.lysenko.util.func.core.TestProperties.readTestPropertiesFromFile(descriptor)
        );

        assertTrue(exception.getMessage().contains("Invalid key"));
        assertTrue(exception.getMessage().contains("SignIn"));
        assertTrue(exception.getMessage().contains("Part files must only contain configuration keys"));
    }
}
