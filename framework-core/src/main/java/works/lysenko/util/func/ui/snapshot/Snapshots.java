package works.lysenko.util.func.ui.snapshot;

import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.FluentWait;
import works.lysenko.Base;
import works.lysenko.util.func.ui.Click;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.LongSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Static DSL entry point for capturing, caching, querying, waiting on, and tapping elements
 * from a single UI hierarchy XML snapshot ({@link UiSnapshot}).
 * <p>
 * Designed as a reusable framework DSL extension (can be statically imported alongside
 * {@link works.lysenko.Base} methods).
 */
public final class Snapshots {

    /**
     * Default short TTL used to coalesce back-to-back read-only probes (such as root {@code fits()}
     * evaluation across all candidate scenarios) into a single {@code getPageSource()} call.
     */
    public static final Duration DEFAULT_CACHE_TTL = Duration.ofMillis(250);

    private static final Duration DEFAULT_POLL_INTERVAL = Duration.ofMillis(100);

    private static volatile Duration cacheTtl = DEFAULT_CACHE_TTL;
    private static final ThreadLocal<UiSnapshot> CACHED_SNAPSHOT = new ThreadLocal<>();
    private static volatile Supplier<String> sourceSupplier = Snapshots::defaultPageSource;
    private static volatile BiConsumer<Integer, Integer> tapHandler = Click::clickAtCoordinates;
    private static volatile LongSupplier clockNanos = System::nanoTime;

    private Snapshots() {}

    /**
     * Returns a recent {@link UiSnapshot} if still within the cache TTL, or captures a fresh one.
     * Ideal for read-only probes such as {@code fits()} checks.
     *
     * @return current or recently cached snapshot
     */
    public static UiSnapshot snapshot() {

        final long ttlNanos = cacheTtl.toNanos();
        final UiSnapshot current = CACHED_SNAPSHOT.get();
        if (ttlNanos > 0L && null != current && !current.isEmpty()) {
            final long ageNanos = clockNanos.getAsLong() - current.capturedAtNanos();
            if (ageNanos >= 0L && ageNanos <= ttlNanos) {
                return current;
            }
        }
        return freshSnapshot();
    }

    /**
     * Always captures a brand-new {@link UiSnapshot} from the active driver and updates the cache.
     * Preferred after UI actions or inside navigation/wait loops.
     *
     * @return freshly captured snapshot
     */
    public static UiSnapshot freshSnapshot() {

        final long now = clockNanos.getAsLong();
        final String xml = safeCaptureXml();
        final UiSnapshot parsed = UiSnapshot.parse(xml, now);
        if (parsed.isEmpty()) {
            CACHED_SNAPSHOT.remove();
        } else {
            CACHED_SNAPSHOT.set(parsed);
        }
        return parsed;
    }

    /**
     * Clears the calling thread's cached snapshot so the next {@link #snapshot()} call captures a fresh UI tree.
     * Framework UI mutations in {@link works.lysenko.tree.Root}, {@link Click}, and {@link Scroll}
     * invoke this automatically; call manually only after raw WebDriver interactions.
     */
    public static void invalidateSnapshot() {

        CACHED_SNAPSHOT.remove();
    }

    /**
     * Configures the cache TTL for {@link #snapshot()}. Pass {@link Duration#ZERO} to disable caching.
     *
     * @param ttl non-null, non-negative duration
     */
    public static void setCacheTtl(final Duration ttl) {

        Objects.requireNonNull(ttl, "ttl must not be null");
        cacheTtl = ttl.isNegative() ? Duration.ZERO : ttl;
        if (cacheTtl.isZero()) {
            invalidateSnapshot();
        }
    }

    /**
     * @return currently configured cache TTL
     */
    public static Duration getCacheTtl() {

        return cacheTtl;
    }

    /**
     * Convenience DSL check evaluating a framework locator or XPath against the cached {@link #snapshot()}.
     *
     * @param locatorOrXpath framework locator key or raw XPath
     * @return {@code true} if displayed in the snapshot
     */
    public static boolean isPresentInSnapshot(final String locatorOrXpath) {

        return snapshot().hasLocator(locatorOrXpath);
    }

    /**
     * Polls using {@link #freshSnapshot()} until {@code condition} matches or {@code timeout} expires.
     * Uses Selenium's {@link FluentWait} without explicit {@code sleep()} calls.
     *
     * @param condition predicate evaluated against each freshly captured snapshot
     * @param timeout   maximum wait duration
     * @return {@code true} if the condition was met before timeout
     */
    public static boolean waitUntilSnapshot(final Predicate<UiSnapshot> condition, final Duration timeout) {

        return awaitSnapshot(condition, timeout).isPresent();
    }

    /**
     * Polls using {@link #freshSnapshot()} until {@code condition} matches or {@code timeout} expires,
     * returning the matching {@link UiSnapshot} directly so callers avoid capturing a duplicate snapshot.
     *
     * @param condition predicate evaluated against each freshly captured snapshot
     * @param timeout   maximum wait duration
     * @return matching snapshot, or {@link Optional#empty()} if timeout elapsed
     */
    public static Optional<UiSnapshot> awaitSnapshot(final Predicate<UiSnapshot> condition, final Duration timeout) {

        Objects.requireNonNull(condition, "condition must not be null");
        Objects.requireNonNull(timeout, "timeout must not be null");
        if (timeout.isZero() || timeout.isNegative()) {
            final UiSnapshot snap = freshSnapshot();
            return condition.test(snap) ? Optional.of(snap) : Optional.empty();
        }
        try {
            final UiSnapshot matched = new FluentWait<>(Boolean.TRUE)
                    .withTimeout(timeout)
                    .pollingEvery(DEFAULT_POLL_INTERVAL)
                    .until(ignored -> {
                        final UiSnapshot snap = freshSnapshot();
                        return condition.test(snap) ? snap : null;
                    });
            return Optional.ofNullable(matched);
        } catch (final TimeoutException ignored) {
            return Optional.empty();
        }
    }

    /**
     * Taps the center of the given {@link UiNode}'s bounds using the framework's coordinate tap
     * ({@link Click#clickAtCoordinates(int, int)}) and invalidates the snapshot cache.
     *
     * @param node target node from a snapshot
     * @return {@code true} if {@code node} had valid bounds and the tap was dispatched
     */
    public static boolean tap(final UiNode node) {

        if (null == node || !node.hasBounds()) {
            return false;
        }
        return tapAt(node.bounds().centerX(), node.bounds().centerY(), node.describe());
    }

    /**
     * Taps at relative coordinates inside the given {@link UiNode}'s bounds and invalidates the cache.
     *
     * @param node   target node from a snapshot
     * @param xRatio horizontal ratio in {@code [0.0, 1.0]}
     * @param yRatio vertical ratio in {@code [0.0, 1.0]}
     * @return {@code true} if {@code node} had valid bounds and the tap was dispatched
     */
    public static boolean tap(final UiNode node, final double xRatio, final double yRatio) {

        if (null == node || !node.hasBounds()) {
            return false;
        }
        return tapAt(node.bounds().relativeX(xRatio), node.bounds().relativeY(yRatio), node.describe());
    }

    private static boolean tapAt(final int x, final int y, final String label) {

        if (null != Base.exec) {
            Base.log(String.format("Tapping snapshot %s at (%d, %d)", label, x, y));
        }
        invalidateSnapshot();
        tapHandler.accept(x, y);
        invalidateSnapshot();
        return true;
    }

    private static String safeCaptureXml() {

        try {
            final String xml = sourceSupplier.get();
            return (null != xml) ? xml : "";
        } catch (final Exception ignored) {
            return "";
        }
    }

    private static String defaultPageSource() {

        if (null == Base.exec) {
            return "";
        }
        final WebDriver wd = Base.exec.wd();
        return (null != wd) ? wd.getPageSource() : "";
    }

    // -------------------------------------------------------------------------
    // Test / extension hooks
    // -------------------------------------------------------------------------

    static void setSourceSupplier(final Supplier<String> supplier) {

        sourceSupplier = Objects.requireNonNull(supplier, "supplier must not be null");
        invalidateSnapshot();
    }

    static void setTapHandler(final BiConsumer<Integer, Integer> handler) {

        tapHandler = Objects.requireNonNull(handler, "handler must not be null");
    }

    static void setClockNanos(final LongSupplier clock) {

        clockNanos = Objects.requireNonNull(clock, "clock must not be null");
    }

    static void resetDefaults() {

        cacheTtl = DEFAULT_CACHE_TTL;
        sourceSupplier = Snapshots::defaultPageSource;
        tapHandler = Click::clickAtCoordinates;
        clockNanos = System::nanoTime;
        invalidateSnapshot();
    }
}
