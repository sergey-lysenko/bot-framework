package works.lysenko.base.output;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * The RunLocks class.
 */
public final class RunLocks {

    private static final String RUN_DIRECTORY = "run";
    private static final String RUN_LOCK = ".run.lock";
    private static final String POSTPROCESS_LOCK = ".postprocess.lock";

    private RunLocks() {
    }

    /**
     * Performs acquire run lock.
     * @return the acquire run lock result
     */
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

    /**
     * Performs is postprocess lock held.
     * @return the is postprocess lock held result
     */
    public static boolean isPostprocessLockHeld() throws IOException {
        return isPostprocessLockHeld(Path.of(RUN_DIRECTORY).toAbsolutePath().normalize());
    }

    /**
     * Performs is postprocess lock held.
     * @param directory the directory
     * @return the is postprocess lock held result
     */
    public static boolean isPostprocessLockHeld(final Path directory) throws IOException {
        Files.createDirectories(directory);
        final Path lockPath = directory.resolve(POSTPROCESS_LOCK);
        if (!Files.exists(lockPath)) return false;
        final LockHandle lock = tryAcquire(lockPath);
        if (null == lock) return true;
        lock.close();
        return false;
    }

    /**
     * Performs wait for postprocess.
     * @param timeoutSeconds the timeoutSeconds
     */
    public static void waitForPostprocess(final long timeoutSeconds) {
        final long deadline = System.currentTimeMillis() + (timeoutSeconds * 1000);
        try {
            while (System.currentTimeMillis() < deadline) {
                if (!isPostprocessLockHeld()) return;
                Thread.sleep(250);
            }
        } catch (final Exception ignored) {
        }
    }

    /**
     * Performs main.
     * @param args the args
     */
    public static void main(final String[] args) {
        final long timeout = (args.length > 0) ? Long.parseLong(args[0]) : 300;
        System.out.println("⏳ Waiting for postprocessing lock (run/.postprocess.lock)...");
        waitForPostprocess(timeout);
        System.out.println("✅ Postprocessing lock released!");
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

    /**
     * The RunLock class.
     */
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
