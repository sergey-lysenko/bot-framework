package works.lysenko.util.func.ui;

import org.apache.commons.math3.fraction.Fraction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import works.lysenko.Base;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ClickTest {

    @BeforeEach
    void setUp() {
        Base.exec = null;
    }

    @AfterEach
    void tearDown() {
        Base.exec = null;
    }

    @Test
    void testClickInstantiation() {
        Click click = new Click();
        assertNotNull(click);
    }

    @Test
    void testClickAtCoordinatesNullDriver() {
        assertDoesNotThrow(() -> Click.clickAtCoordinates(10, 20));
    }

    @Test
    void testLongClickAtCoordinatesNullDriver() {
        assertDoesNotThrow(() -> Click.longClickAtCoordinates(100L, 10, 20));
    }
}
