package works.lysenko.util.data.records.test;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@DisplayName("test.Triplet Record Tests")
class TripletTest {

    @Test
    @DisplayName("Should store and retrieve ultimate, penultimate, and antepenultimate throwables")
    void testRecordFields() {
        final Throwable u = new RuntimeException("ultimate");
        final Throwable p = new IllegalStateException("penultimate");
        final Throwable a = new IllegalArgumentException("antepenultimate");

        final Triplet triplet = new Triplet(u, p, a);

        assertEquals(u, triplet.ultimate());
        assertEquals(p, triplet.penultimate());
        assertEquals(a, triplet.antepenultimate());
    }

    @Test
    @DisplayName("Should handle null throwables")
    void testNullFields() {
        final Triplet triplet = new Triplet(null, null, null);

        assertNull(triplet.ultimate());
        assertNull(triplet.penultimate());
        assertNull(triplet.antepenultimate());
    }

    @Test
    @DisplayName("Should satisfy equals and hashCode contract")
    void testEqualsAndHashCode() {
        final Throwable u = new RuntimeException("err");
        final Triplet t1 = new Triplet(u, null, null);
        final Triplet t2 = new Triplet(u, null, null);
        final Triplet t3 = new Triplet(null, null, null);

        assertEquals(t1, t2);
        assertEquals(t1.hashCode(), t2.hashCode());
        assertNotEquals(t1, t3);
    }

    @Test
    @DisplayName("Should produce non-null toString")
    void testToString() {
        final Triplet triplet = new Triplet(null, null, null);
        assertNotNull(triplet.toString());
    }
}
