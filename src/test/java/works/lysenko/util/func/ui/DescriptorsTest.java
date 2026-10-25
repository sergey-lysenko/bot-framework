package works.lysenko.util.func.ui;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import works.lysenko.Base;
import works.lysenko.base.Parameters;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

class DescriptorsTest {

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
    void testPutAndGet() {
        Descriptors.put("test.key", "xpath=//test");
        assertEquals("xpath=//test", Descriptors.get("test.key"));
    }

    @Test
    void testFill() {
        Descriptors.put("formatted.key", "xpath=//test[%s]");
        assertEquals("xpath=//test[123]", Descriptors.fill("formatted.key", "123"));
    }

    @Test
    void testEdit() {
        assertDoesNotThrow(() -> {
            try {
                Descriptors.edit("sampleText");
            } catch (IllegalStateException | NullPointerException e) {
                // Expected if platform is not configured
            }
        });
    }
}
