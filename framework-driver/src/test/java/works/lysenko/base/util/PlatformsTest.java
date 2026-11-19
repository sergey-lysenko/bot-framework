package works.lysenko.base.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import works.lysenko.util.data.enums.Platform;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static works.lysenko.util.spec.Layout.Files.PLATFORMS_;

class PlatformsTest {

    @BeforeEach
    void setUp() {
        final File platformsFile = new File(PLATFORMS_);
        if (platformsFile.exists()) {
            platformsFile.delete();
        }
    }

    @AfterEach
    void tearDown() {
        final File platformsFile = new File(PLATFORMS_);
        if (platformsFile.exists()) {
            platformsFile.delete();
        }
        new File("var").delete();
    }

    @Test
    void testPlatformsScanAndSave() {
        final long start = System.currentTimeMillis();
        final List<Platform> platforms = Platforms.available();
        System.out.println("Platforms.available() total time: " + (System.currentTimeMillis() - start) + " ms, found: " + platforms);
        assertNotNull(platforms);
        assertFalse(platforms.isEmpty(), "Available platforms should not be empty");

        final File platformsFile = new File(PLATFORMS_);
        assertTrue(platformsFile.exists(), "Platforms file should be created after scan");

        // Subsequent call loads from file
        final List<Platform> loadedPlatforms = Platforms.available();
        assertEquals(platforms.size(), loadedPlatforms.size());
    }

    @Test
    void testPlatformsLoadFromFile() throws IOException {
        final File platformsFile = new File(PLATFORMS_);
        platformsFile.getParentFile().mkdirs();
        Files.writeString(Paths.get(PLATFORMS_), "Chrome,Firefox");

        final List<Platform> platforms = Platforms.available();
        assertEquals(2, platforms.size());
        assertTrue(platforms.contains(Platform.CHROME));
        assertTrue(platforms.contains(Platform.FIREFOX));
    }
}
