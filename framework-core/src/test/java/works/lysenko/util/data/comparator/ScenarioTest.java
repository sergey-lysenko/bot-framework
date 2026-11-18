package works.lysenko.util.data.comparator;

import org.junit.jupiter.api.Test;
import works.lysenko.util.apis.scenario._Scenario;

import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScenarioTest {

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
    void testCompare() {
        final Scenario comparator = new Scenario();

        final _Scenario s1 = mockScenario("Alpha");
        final _Scenario s2 = mockScenario("Beta");

        assertTrue(comparator.compare(s1, s2) < 0);
        assertTrue(comparator.compare(s2, s1) > 0);

        final _Scenario s3 = mockScenario("Alpha");
        assertEquals(0, comparator.compare(s1, s3));
    }
}
