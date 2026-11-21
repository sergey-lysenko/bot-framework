package works.lysenko.base.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RunLocksTest {

    @Test
    void preventsConcurrentRunsWhileAllowingNewRunDuringPostProcessing(@TempDir final Path runDirectory) throws IOException {
        final RunLocks.RunLock runLock = RunLocks.acquireRunLock(runDirectory);
        assertThrows(IllegalStateException.class, () -> RunLocks.acquireRunLock(runDirectory));

        final RunLocks.LockHandle postprocessLock = RunLocks.acquirePostprocessLock(runDirectory);
        runLock.close();

        try (RunLocks.RunLock nextRun = RunLocks.acquireRunLock(runDirectory)) {
            assertTrue(Files.exists(runDirectory.resolve(".run.lock")));
            assertTrue(Files.exists(runDirectory.resolve(".postprocess.lock")));
            assertTrue(RunLocks.isPostprocessLockHeld(runDirectory));
            assertThrows(IllegalStateException.class, () -> RunLocks.acquireRunLock(runDirectory));
        } finally {
            postprocessLock.close();
        }
        assertFalse(RunLocks.isPostprocessLockHeld(runDirectory));
    }

    @Test
    void postprocessLockWaitingQueuesUntilPreviousPostProcessorFinishes(@TempDir final Path runDirectory) throws Exception {
        final RunLocks.LockHandle first = RunLocks.acquirePostprocessLock(runDirectory);
        assertThrows(IOException.class, () -> RunLocks.acquirePostprocessLock(runDirectory));

        final CountDownLatch acquired = new CountDownLatch(1);
        final AtomicReference<Throwable> error = new AtomicReference<>();
        final Thread waiter = new Thread(() -> {
            try (RunLocks.LockHandle second = RunLocks.acquirePostprocessLockWaiting(runDirectory)) {
                acquired.countDown();
            } catch (final Throwable t) {
                error.set(t);
            }
        });
        waiter.start();

        assertFalse(acquired.await(150, TimeUnit.MILLISECONDS));
        first.close();

        assertTrue(acquired.await(5, TimeUnit.SECONDS));
        waiter.join(5000);
        assertNull(error.get());
        assertFalse(RunLocks.isPostprocessLockHeld(runDirectory));
    }
}
