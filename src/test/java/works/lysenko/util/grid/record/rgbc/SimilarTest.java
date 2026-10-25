package works.lysenko.util.grid.record.rgbc;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SimilarTest {

    @Test
    void testSimilarInstantiationAndEqualsHashCode() {
        Similar s1 = new Similar(1, 2, 5.0);
        Similar s2 = new Similar(1, 2, 5.0);
        Similar s3 = new Similar(2, 3, 2.0);

        assertEquals(1, s1.c1());
        assertEquals(2, s1.c2());
        assertEquals(5.0, s1.distance());

        assertEquals(s1, s1);
        assertEquals(s1, s2);
        assertEquals(s1.hashCode(), s2.hashCode());
        assertNotEquals(s1, s3);
        assertNotEquals(s1, null);
        assertNotEquals(s1, "string");
    }
}
