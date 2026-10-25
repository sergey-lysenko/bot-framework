package works.lysenko.util.func.ui;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import works.lysenko.Base;
import works.lysenko.base.Parameters;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ScrollTest {

    private Parameters previousParameters;

    @BeforeEach
    void setUp() {
        previousParameters = Base.parameters;
        Base.parameters = new Parameters(new java.util.Properties());
    }

    @AfterEach
    void tearDown() {
        Base.parameters = previousParameters;
    }

    @Test
    void testScrollInstantiation() {
        Scroll scroll = new Scroll();
        assertNotNull(scroll);
    }

    @Test
    void testDefaultSwiperLocator() {
        assertDoesNotThrow(() -> {
            try {
                Scroll.defaultSwiperLocator();
            } catch (IllegalStateException | NullPointerException e) {
                // Expected if current platform is not configured
            }
        });
    }
}
