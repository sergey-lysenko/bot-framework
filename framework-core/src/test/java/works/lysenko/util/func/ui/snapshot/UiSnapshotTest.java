package works.lysenko.util.func.ui.snapshot;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UiSnapshotTest {

    private static final String SAMPLE_XML = """
            <?xml version='1.0' encoding='UTF-8' standalone='yes' ?>
            <hierarchy index="0" class="hierarchy" rotation="0" width="1080" height="2424">
              <android.widget.FrameLayout index="0" package="works.lysenko.rozkladach" class="android.widget.FrameLayout" text="" resource-id="android:id/content" displayed="true" bounds="[0,0][1080,2424]">
                <android.view.ViewGroup index="0" package="works.lysenko.rozkladach" class="android.view.ViewGroup" resource-id="works.lysenko.rozkladach:id/toolbar" displayed="true" bounds="[0,100][1080,240]">
                  <android.widget.TextView index="0" package="works.lysenko.rozkladach" class="android.widget.TextView" text="Work" resource-id="works.lysenko.rozkladach:id/title_main" clickable="true" enabled="true" displayed="true" bounds="[40,120][340,220]" />
                  <android.widget.ImageView index="1" package="works.lysenko.rozkladach" class="android.widget.ImageView" text="" content-desc="More options" clickable="true" enabled="true" displayed="true" bounds="[960,120][1060,220]" />
                </android.view.ViewGroup>
                <androidx.recyclerview.widget.RecyclerView index="1" package="works.lysenko.rozkladach" class="androidx.recyclerview.widget.RecyclerView" resource-id="works.lysenko.rozkladach:id/scheduleView" displayed="true" bounds="[0,240][1080,2360]" />
                <android.widget.Button index="2" package="works.lysenko.rozkladach" class="android.widget.Button" text="AI Wizard" resource-id="works.lysenko.rozkladach:id/btnAi" clickable="true" enabled="true" displayed="true" bounds="[100,2000][500,2120]" />
                <android.widget.LinearLayout index="3" package="works.lysenko.rozkladach" class="android.widget.LinearLayout" resource-id="works.lysenko.rozkladach:id/settings_root" displayed="false" bounds="[0,0][0,0]" />
              </android.widget.FrameLayout>
            </hierarchy>
            """;

    @AfterEach
    void tearDown() {

        Snapshots.resetDefaults();
    }

    @Test
    void parsesBoundsAndComputesCoordinates() {

        final UiBounds bounds = UiBounds.parse("[960,120][1060,220]");
        assertFalse(bounds.isEmpty());
        assertEquals(100, bounds.width());
        assertEquals(100, bounds.height());
        assertEquals(1010, bounds.centerX());
        assertEquals(170, bounds.centerY());
        assertEquals(980, bounds.relativeX(0.2));
        assertEquals(200, bounds.relativeY(0.8));

        final UiBounds negative = UiBounds.parse("[-20,10][80,110]");
        assertEquals(100, negative.width());
        assertEquals(30, negative.centerX());

        assertTrue(UiBounds.parse(null).isEmpty());
        assertTrue(UiBounds.parse("").isEmpty());
        assertTrue(UiBounds.parse("invalid").isEmpty());
        assertTrue(UiBounds.parse("[10,10][10,10]").isEmpty());
    }

    @Test
    void indexesDisplayedNodesAndFiltersHiddenNodes() {

        final UiSnapshot snap = UiSnapshot.parse(SAMPLE_XML);
        assertFalse(snap.isEmpty());
        assertEquals(7, snap.allNodes().size());
        assertEquals(6, snap.nodes().size());

        assertTrue(snap.hasId("scheduleView"));
        assertTrue(snap.hasId("works.lysenko.rozkladach:id/scheduleView"));
        assertTrue(snap.hasAnyId("missing_id", "title_main"));
        assertTrue(snap.hasAllIds("toolbar", "scheduleView", "btnAi"));
        assertFalse(snap.hasAllIds("toolbar", "missing_id"));

        // Hidden node (displayed="false") must not be treated as displayed
        assertFalse(snap.hasId("settings_root"));

        assertTrue(snap.hasIdWithText("title_main", "Work", "Робота"));
        assertFalse(snap.hasIdWithText("title_main", "Tasks", "Завдання"));

        assertTrue(snap.hasText("Work"));
        assertTrue(snap.hasTextContaining("Wiz"));
        assertTrue(snap.hasButton("AI Wizard"));
        assertFalse(snap.hasButton("Work"));

        assertTrue(snap.hasDesc("More options"));
        assertTrue(snap.hasDescContaining("More", "Більше"));
        assertFalse(snap.hasDescContaining("Help"));
    }

    @Test
    void evaluatesSimpleAndComplexXpathLocally() {

        final UiSnapshot snap = UiSnapshot.parse(SAMPLE_XML);

        // Simple fast-path XPaths
        assertTrue(snap.hasLocator("//androidx.recyclerview.widget.RecyclerView[@resource-id='works.lysenko.rozkladach:id/scheduleView']"));
        assertTrue(snap.hasLocator("//android.widget.TextView[@resource-id='works.lysenko.rozkladach:id/title_main']"));
        assertFalse(snap.hasLocator("//*[@resource-id='works.lysenko.rozkladach:id/settings_root']"));

        // Complex XPath (contains / or / hierarchy) via local DOM evaluation
        final String moreXpath = "//android.widget.ImageView[contains(@content-desc, 'More') or contains(@content-desc, 'Більше')]";
        final String menuXpath = "//android.widget.TextView[@resource-id='works.lysenko.rozkladach:id/title_list' or @resource-id='works.lysenko.rozkladach:id/title_main']";
        assertTrue(snap.hasLocator(moreXpath));
        assertTrue(snap.hasLocator(menuXpath));
        assertTrue(snap.hasLocator("//android.view.ViewGroup[@resource-id='works.lysenko.rozkladach:id/toolbar']/android.widget.TextView"));

        final UiNode more = snap.findByLocator(moreXpath).orElseThrow();
        assertEquals(1010, more.bounds().centerX());
        assertEquals(170, more.bounds().centerY());

        // Hidden node is excluded by default, included when displayedOnly=false
        final String settingsXpath = "//*[@resource-id='works.lysenko.rozkladach:id/settings_root']";
        assertTrue(snap.findAllByLocator(settingsXpath, true).isEmpty());
        assertEquals(1, snap.findAllByLocator(settingsXpath, false).size());
    }

    @Test
    void isolatesCachedSnapshotsPerThread() throws Exception {

        final AtomicInteger captures = new AtomicInteger();
        Snapshots.setCacheTtl(Duration.ofSeconds(10));
        Snapshots.setSourceSupplier(() -> {
            final int id = captures.incrementAndGet();
            return "<hierarchy><android.widget.TextView text=\"call-" + id + "\" displayed=\"true\" bounds=\"[0,0][100,100]\"/></hierarchy>";
        });

        final UiSnapshot mainSnap = Snapshots.snapshot();
        assertTrue(mainSnap.hasText("call-1"));

        final java.util.concurrent.atomic.AtomicReference<UiSnapshot> workerSnap = new java.util.concurrent.atomic.AtomicReference<>();
        final Thread worker = new Thread(() -> workerSnap.set(Snapshots.snapshot()));
        worker.start();
        worker.join();

        assertTrue(workerSnap.get().hasText("call-2"));
        // Main thread still sees its own cached snapshot
        assertSame(mainSnap, Snapshots.snapshot());
    }

    @Test
    void cachesWithinTtlAndInvalidatesOnTapOrExplicitReset() {

        final AtomicInteger captures = new AtomicInteger();
        final AtomicLong fakeClock = new AtomicLong(1_000_000_000L);
        final List<String> taps = new ArrayList<>();

        Snapshots.setCacheTtl(Duration.ofMillis(250));
        Snapshots.setClockNanos(fakeClock::get);
        Snapshots.setSourceSupplier(() -> {
            captures.incrementAndGet();
            return SAMPLE_XML;
        });
        Snapshots.setTapHandler((x, y) -> taps.add(x + "," + y));

        final UiSnapshot first = Snapshots.snapshot();
        final UiSnapshot second = Snapshots.snapshot();
        assertSame(first, second);
        assertEquals(1, captures.get());

        // Advance past TTL -> recaptures
        fakeClock.addAndGet(Duration.ofMillis(300).toNanos());
        Snapshots.snapshot();
        assertEquals(2, captures.get());

        // Tapping a node dispatches center coordinates AND invalidates cache
        assertTrue(first.tapByDescContaining("More"));
        assertEquals(List.of("1010,170"), taps);

        Snapshots.snapshot();
        assertEquals(3, captures.get());

        // Relative tap works too
        assertTrue(first.tapLocator("//android.widget.ImageView[@content-desc='More options']", 0.2, 0.8));
        assertEquals(List.of("1010,170", "980,200"), taps);

        // freshSnapshot always bypasses cache
        Snapshots.freshSnapshot();
        assertEquals(4, captures.get());

        // Manual invalidation forces next snapshot() to recapture
        Snapshots.invalidateSnapshot();
        Snapshots.snapshot();
        assertEquals(5, captures.get());
    }

    @Test
    void waitUntilSnapshotPollsFreshSnapshotsUntilConditionMet() {

        final AtomicInteger calls = new AtomicInteger();
        Snapshots.setSourceSupplier(() -> (calls.incrementAndGet() >= 3)
                ? SAMPLE_XML
                : "<hierarchy><android.widget.FrameLayout displayed=\"true\" bounds=\"[0,0][100,100]\"/></hierarchy>");

        final boolean matched = Snapshots.waitUntilSnapshot(
                snap -> snap.hasId("scheduleView"),
                Duration.ofSeconds(2)
        );
        assertTrue(matched);
        assertTrue(calls.get() >= 3);
    }

    @Test
    void screenClassifierAppliesRulesInPriorityOrder() {

        final ScreenClassifier<String> classifier = ScreenClassifier.<String>builder("UNKNOWN")
                .when("OVERLAY", snap -> snap.hasId("overlay"))
                .when("BASE", snap -> snap.hasId("base"))
                .build();

        final UiSnapshot both = UiSnapshot.parse("""
                <hierarchy>
                  <android.view.View resource-id="pkg:id/base" displayed="true" bounds="[0,0][100,100]"/>
                  <android.view.View resource-id="pkg:id/overlay" displayed="true" bounds="[0,0][100,100]"/>
                </hierarchy>
                """);
        assertEquals("OVERLAY", classifier.classify(both));
        assertEquals("UNKNOWN", classifier.classify(UiSnapshot.empty()));
    }
}
