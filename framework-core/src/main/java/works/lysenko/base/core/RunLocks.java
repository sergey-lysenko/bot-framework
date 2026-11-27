package works.lysenko.base.core;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.FileLockInterruptionException;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.data.strs.Swap.sn;

/**
 * Coordinates filesystem locks across active test runs and detached post-processors.
 */
public final class RunLocks {

    private static final String RUN_DIRECTORY = "run";
    private static final String RUN_LOCK = ".run.lock";
    private static final String POSTPROCESS_LOCK = ".postprocess.lock";

    private RunLocks() {
    }

    /**
     * Acquires the active test run lock in the default {@code run} directory.
     *
     * @return the acquired run lock handle
     */
    public static RunLock acquireRunLock() {
        final Path directory = Path.of(RUN_DIRECTORY).toAbsolutePath().normalize();
        return acquireRunLock(directory);
    }

    /**
     * Acquires the active test run lock in the given directory.
     *
     * @param directory the lock directory
     * @return the acquired run lock handle
     */
    public static RunLock acquireRunLock(final Path directory) {
        try {
            Files.createDirectories(directory);
            final LockHandle run = tryAcquire(directory.resolve(RUN_LOCK));
            if (null == run) {
                throw new IllegalStateException("A test run is already active (run/.run.lock)");
            }
            return new RunLock(run);
        } catch (final IOException e) {
            throw new IllegalStateException(s("Unable to acquire run lock in ", directory), e);
        }
    }

    /**
     * Acquires the post-process lock in the default {@code run} directory.
     *
     * @return the acquired post-process lock handle
     * @throws IOException if another post-processor holds the lock or I/O fails
     */
    public static LockHandle acquirePostprocessLock() throws IOException {
        return acquirePostprocessLock(Path.of(RUN_DIRECTORY).toAbsolutePath().normalize());
    }

    /**
     * Acquires the post-process lock in the given directory.
     *
     * @param directory the lock directory
     * @return the acquired post-process lock handle
     * @throws IOException if another post-processor holds the lock or I/O fails
     */
    public static LockHandle acquirePostprocessLock(final Path directory) throws IOException {
        Files.createDirectories(directory);
        final LockHandle lock = tryAcquire(directory.resolve(POSTPROCESS_LOCK));
        if (null == lock) throw new IOException(s("Another post-processor is already active in ", directory));
        return lock;
    }

    /**
     * Acquires the post-process lock in the default {@code run} directory,
     * waiting until any previous post-processor releases it.
     *
     * @return the acquired post-process lock handle
     * @throws IOException          if I/O fails
     * @throws InterruptedException if interrupted while waiting for the lock
     */
    public static LockHandle acquirePostprocessLockWaiting() throws IOException, InterruptedException {
        return acquirePostprocessLockWaiting(Path.of(RUN_DIRECTORY).toAbsolutePath().normalize());
    }

    /**
     * Acquires the post-process lock in the given directory,
     * waiting until any previous post-processor releases it.
     *
     * @param directory the lock directory
     * @return the acquired post-process lock handle
     * @throws IOException          if I/O fails
     * @throws InterruptedException if interrupted while waiting for the lock
     */
    public static LockHandle acquirePostprocessLockWaiting(final Path directory) throws IOException, InterruptedException {
        Files.createDirectories(directory);
        final Path lockPath = directory.resolve(POSTPROCESS_LOCK);
        final LockHandle immediate = tryAcquire(lockPath);
        if (null != immediate) return immediate;
        return acquireBlocking(lockPath);
    }

    /**
     * Checks whether the post-process lock is currently held in the default {@code run} directory.
     *
     * @return true if held
     * @throws IOException on I/O error
     */
    public static boolean isPostprocessLockHeld() throws IOException {
        return isPostprocessLockHeld(Path.of(RUN_DIRECTORY).toAbsolutePath().normalize());
    }

    /**
     * Checks whether the post-process lock is currently held in the given directory.
     *
     * @param directory the lock directory
     * @return true if held
     * @throws IOException on I/O error
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
     * Waits up to {@code timeoutSeconds} for the post-process lock to be released.
     *
     * @param timeoutSeconds maximum seconds to wait
     */
    public static void waitForPostprocess(final long timeoutSeconds) {
        final long deadline = System.currentTimeMillis() + (timeoutSeconds * 1000);
        try {
            while (System.currentTimeMillis() < deadline) {
                if (!isPostprocessLockHeld()) return;
                Thread.sleep(250);
            }
        } catch (final Exception _) {
        }
    }

    /**
     * CLI entrypoint waiting for post-processing lock release.
     *
     * @param args optional timeout seconds in {@code args[0]}
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

    private static LockHandle acquireBlocking(final Path path) throws IOException, InterruptedException {
        while (true) {
            if (Thread.interrupted()) {
                throw new InterruptedException(s("Interrupted while waiting for lock ", path));
            }
            final FileChannel channel = FileChannel.open(
                    path,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE);
            try {
                final FileLock lock = channel.lock();
                return new LockHandle(channel, lock);
            } catch (final OverlappingFileLockException e) {
                channel.close();
                Thread.sleep(50);
            } catch (final FileLockInterruptionException e) {
                channel.close();
                Thread.interrupted();
                final InterruptedException ie = new InterruptedException(s("Interrupted while waiting for lock ", path));
                ie.initCause(e);
                throw ie;
            } catch (final IOException | RuntimeException e) {
                channel.close();
                throw e;
            }
        }
    }

    /**
     * Auto-closeable wrapper for an active run lock.
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

    /**
     * Auto-closeable underlying file lock handle.
     */
    public static final class LockHandle implements AutoCloseable {
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
                System.err.println(sn("Unable to release lock: ", e.getMessage()));
            }
            try {
                channel.close();
            } catch (final IOException e) {
                System.err.println(sn("Unable to close lock file: ", e.getMessage()));
            }
        }
    }
}
