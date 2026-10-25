package works.lysenko.util.data.records.grid;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import works.lysenko.util.data.records.Location;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@DisplayName("grid.OutOfMatrixBoundsRecord Tests")
class OutOfMatrixBoundsRecordTest {

    @Test
    @DisplayName("Should store and access location and integer indices")
    void testRecordFields() {
        final Location loc = Location.point(10.5f, 20.5f);
        final OutOfMatrixBoundsRecord record = new OutOfMatrixBoundsRecord(loc, 10, 20);

        assertEquals(loc, record.location());
        assertEquals(10, record.iX());
        assertEquals(20, record.iY());
    }

    @Test
    @DisplayName("Should handle null location")
    void testNullLocation() {
        final OutOfMatrixBoundsRecord record = new OutOfMatrixBoundsRecord(null, 0, 0);

        assertNull(record.location());
        assertEquals(0, record.iX());
        assertEquals(0, record.iY());
    }

    @Test
    @DisplayName("Should satisfy equals and hashCode contract")
    void testEqualsAndHashCode() {
        final Location loc = Location.point(1.0f, 2.0f);
        final OutOfMatrixBoundsRecord r1 = new OutOfMatrixBoundsRecord(loc, 1, 2);
        final OutOfMatrixBoundsRecord r2 = new OutOfMatrixBoundsRecord(loc, 1, 2);
        final OutOfMatrixBoundsRecord r3 = new OutOfMatrixBoundsRecord(loc, 1, 3);

        assertEquals(r1, r2);
        assertEquals(r1.hashCode(), r2.hashCode());
        assertNotEquals(r1, r3);
    }

    @Test
    @DisplayName("Should produce non-null toString")
    void testToString() {
        final OutOfMatrixBoundsRecord record = new OutOfMatrixBoundsRecord(null, 1, 1);
        assertNotNull(record.toString());
    }
}
