package works.lysenko.util.func.ui;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import works.lysenko.Base;
import works.lysenko.base.Parameters;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocatorsTest {

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
    void testLocatorsInstantiation() {
        Locators locators = new Locators();
        assertNotNull(locators);
    }

    @Test
    void testByXpath() {
        By byXpath = Locators.by("//div[@id='test']");
        assertNotNull(byXpath);
        assertTrue(byXpath.toString().contains("xpath"));
    }

    @Test
    void testByCss() {
        By byCss = Locators.by("#main-content");
        assertNotNull(byCss);
        assertTrue(byCss.toString().contains("cssSelector"));
    }

    @Test
    void testDescribe() {
        String desc = Locators.describe("loc1", "loc2");
        assertNotNull(desc);
        assertTrue(desc.contains("loc1"));
    }

    @Test
    void testTextDescriptor() {
        String descKey = Locators.text("hello");
        assertNotNull(descKey);
    }
}
