package works.lysenko.base.ui;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static works.lysenko.util.spec.Layout.Paths._DASH_;

class DirectoryTest {

    private final Path dashDir = Path.of(_DASH_);

    @BeforeEach
    @AfterEach
    void cleanup() throws IOException {
        if (Files.exists(dashDir)) {
            try (final var stream = Files.walk(dashDir)) {
                stream.sorted((a, b) -> b.compareTo(a))
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
        }
    }

    @Test
    void testCliFilesOperationsInRootDashDirectory() throws IOException {
        final Directory directory = new Directory(true);

        assertTrue(directory.isDebug(), "Debug flag file should exist");
        assertTrue(Files.exists(dashDir.resolve("debug")), "File dash/debug should exist");

        directory.setTitle("Test Title");
        assertTrue(Files.exists(dashDir.resolve("title")));
        assertEquals("Test Title", Files.readString(dashDir.resolve("title")).trim());

        assertFalse(directory.isPause());
        directory.setPause(true);
        assertTrue(directory.isPause());
        assertTrue(Files.exists(dashDir.resolve("pause")));
        directory.setPause(false);
        assertFalse(directory.isPause());
        assertFalse(Files.exists(dashDir.resolve("pause")));

        assertFalse(directory.isStop());
        directory.setStop(true);
        assertTrue(directory.isStop());
        assertTrue(Files.exists(dashDir.resolve("stop")));
        directory.setStop(false);
        assertFalse(directory.isStop());
        assertFalse(Files.exists(dashDir.resolve("stop")));

        assertFalse(directory.isHalt());
        directory.setHalt(true);
        assertTrue(directory.isHalt());
        assertTrue(Files.exists(dashDir.resolve("halt")));
        directory.setHalt(false);
        assertFalse(directory.isHalt());
        assertFalse(Files.exists(dashDir.resolve("halt")));

        directory.setDebug(false);
        assertFalse(directory.isDebug());
        assertFalse(Files.exists(dashDir.resolve("debug")));
    }
}
