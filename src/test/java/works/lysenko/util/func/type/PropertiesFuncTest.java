package works.lysenko.util.func.type;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class PropertiesFuncTest {

    @Test
    void testPropertiesInstantiation() {
        Properties properties = new Properties();
        assertNotNull(properties);
    }
}
