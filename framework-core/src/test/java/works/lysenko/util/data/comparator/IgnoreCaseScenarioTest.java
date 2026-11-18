package works.lysenko.util.data.comparator;

import org.junit.jupiter.api.Test;
import works.lysenko.util.apis.scenario._Scenario;

import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IgnoreCaseScenarioTest {

    private _Scenario mockScenario(final String name) {
        return (_Scenario) Proxy.newProxyInstance(
                _Scenario.class.getClassLoader(),
                new Class<?>[]{_Scenario.class},
                (proxy, method, args) -> {
                    if ("getName".equals(method.getName())) return name;
                    return null;
                }
        );
    }

    @Test
    void testCompareSameObject() {
        final IgnoreCaseScenario comparator = new IgnoreCaseScenario();
        final _Scenario s1 = mockScenario("Test");

        assertEquals(0, comparator.compare(s1, s1));
    }

    @Test
    void testCompareDifferentObjects() {
        final IgnoreCaseScenario comparator = new IgnoreCaseScenario();
        final _Scenario s1 = mockScenario("alpha");
        final _Scenario s2 = mockScenario("BETA");

        final int result1 = comparator.compare(s1, s2);
        final int result2 = comparator.compare(s2, s1);

        assertTrue(result1 != 0);
        assertEquals(-Integer.signum(result1), Integer.signum(result2));
    }
}

