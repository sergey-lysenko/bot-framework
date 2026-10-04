package works.lysenko.base.output;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public final class RunLocks {

    private static final String RUN_DIRECTORY = "run";
    private static final String RUN_LOCK = ".run.lock";
    private static final String POSTPROCESS_LOCK = ".postprocess.lock";

    private RunLocks() {
    }

    public static RunLock acquireRunLock() {
        final Path directory = Path.of(RUN_DIRECTORY).toAbsolutePath().normalize();
        return acquireRunLock(directory);
    }

    static RunLock acquireRunLock(final Path directory) {
        try {
            Files.createDirectories(directory);
            final LockHandle run = tryAcquire(directory.resolve(RUN_LOCK));
            if (null == run) {
                throw new IllegalStateException("A test run is already active (run/.run.lock)");
            }
            try {
                final LockHandle postprocess = tryAcquire(directory.resolve(POSTPROCESS_LOCK));
                if (null == postprocess) {
                    throw new IllegalStateException(
                            "The previous test run is still being post-processed (run/.postprocess.lock)");
                }
                postprocess.close();
                return new RunLock(run);
            } catch (final Exception e) {
                run.close();
                throw e;
            }
        } catch (final IOException e) {
            throw new IllegalStateException("Unable to acquire run lock in " + directory, e);
        }
    }

    static LockHandle acquirePostprocessLock() throws IOException {
        return acquirePostprocessLock(Path.of(RUN_DIRECTORY).toAbsolutePath().normalize());
    }

    static LockHandle acquirePostprocessLock(final Path directory) throws IOException {
        Files.createDirectories(directory);
        final LockHandle lock = tryAcquire(directory.resolve(POSTPROCESS_LOCK));
        if (null == lock) throw new IOException("Another post-processor is already active in " + directory);
        return lock;
    }

    static boolean isPostprocessLockHeld() throws IOException {
        return isPostprocessLockHeld(Path.of(RUN_DIRECTORY).toAbsolutePath().normalize());
    }

    static boolean isPostprocessLockHeld(final Path directory) throws IOException {
        Files.createDirectories(directory);
        final LockHandle lock = tryAcquire(directory.resolve(POSTPROCESS_LOCK));
        if (null == lock) return true;
        lock.close();
        return false;
    }

    private static LockHandle tryAcquire(final Path path) throws IOException {
        final FileChannel channel = FileChannel.open(
                path,
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE);
        try {
            final FileLock lock;
            try {
                lock = channel.tryLock();
            } catch (final OverlappingFileLockException e) {
                channel.close();
                return null;
            }
            if (null == lock) {
                channel.close();
                return null;
            }
            return new LockHandle(channel, lock);
        } catch (final IOException | RuntimeException e) {
            channel.close();
            throw e;
        }
    }

    public static final class RunLock implements AutoCloseable {
        private final LockHandle handle;

        private RunLock(final LockHandle handle) {
            this.handle = handle;
        }

        @Override
        public void close() {
            handle.close();
        }
    }

    static final class LockHandle implements AutoCloseable {
        private final FileChannel channel;
        private final FileLock lock;
        private boolean closed;

        private LockHandle(final FileChannel channel, final FileLock lock) {
            this.channel = channel;
            this.lock = lock;
        }

        @Override
        public synchronized void close() {
            if (closed) return;
            closed = true;
            try {
                lock.release();
            } catch (final IOException e) {
                System.err.println("Unable to release lock: " + e.getMessage());
            }
            try {
                channel.close();
            } catch (final IOException e) {
                System.err.println("Unable to close lock file: " + e.getMessage());
            }
        }
    }
}
