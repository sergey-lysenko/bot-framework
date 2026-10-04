package works.lysenko.base.output;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RunLocksTest {

    @Test
    void preventsConcurrentRunsAndWaitsForPostProcessing(@TempDir final Path runDirectory) throws IOException {
        final RunLocks.RunLock runLock = RunLocks.acquireRunLock(runDirectory);
        assertThrows(IllegalStateException.class, () -> RunLocks.acquireRunLock(runDirectory));

        final RunLocks.LockHandle postprocessLock = RunLocks.acquirePostprocessLock(runDirectory);
        runLock.close();
        assertThrows(IllegalStateException.class, () -> RunLocks.acquireRunLock(runDirectory));

        postprocessLock.close();
        try (RunLocks.RunLock nextRun = RunLocks.acquireRunLock(runDirectory)) {
            assertTrue(Files.exists(runDirectory.resolve(".run.lock")));
            assertTrue(Files.exists(runDirectory.resolve(".postprocess.lock")));
        }
    }
}
