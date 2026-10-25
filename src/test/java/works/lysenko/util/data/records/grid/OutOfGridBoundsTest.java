package works.lysenko.util.data.records.grid;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;

import works.lysenko.util.data.records.Location;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("grid.OutOfGridBounds Tests")
class OutOfGridBoundsTest {

    @Test
    @DisplayName("Should initially be empty and report false for isNotEmpty()")
    void testInitialState() {
        final OutOfGridBounds bounds = new OutOfGridBounds();
        assertFalse(bounds.isNotEmpty());
    }

    @Test
    @DisplayName("Should add entries and report true for isNotEmpty()")
    void testAddAndIsNotEmpty() {
        final OutOfGridBounds bounds = new OutOfGridBounds();
        bounds.add(Location.point(1.0f, 2.0f), 1, 2);

        assertTrue(bounds.isNotEmpty());
    }

    @Test
    @DisplayName("Should throw NoSuchElementException on summary when empty")
    void testSummaryWhenEmpty() {
        final OutOfGridBounds bounds = new OutOfGridBounds();

        assertThrows(NoSuchElementException.class, bounds::summary);
    }

    @Test
    @DisplayName("Should log summary without exception when elements exist")
    void testSummaryWithElements() {
        final OutOfGridBounds bounds = new OutOfGridBounds();
        bounds.add(Location.point(10.0f, 20.0f), 10, 20);
        bounds.add(Location.point(5.0f, 25.0f), 5, 25);

        // Note: Base.logEvent will run during summary()
        bounds.summary();
        assertTrue(bounds.isNotEmpty());
    }
}
