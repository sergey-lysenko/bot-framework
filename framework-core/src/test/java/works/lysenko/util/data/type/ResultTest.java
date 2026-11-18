package works.lysenko.util.data.type;

import org.apache.commons.math3.fraction.Fraction;
import org.junit.jupiter.api.Test;
import works.lysenko.util.apis.scenario._Scenario;
import works.lysenko.util.data.enums.ExecutionStatus;
import works.lysenko.util.data.enums.ScenarioType;

import java.lang.reflect.Proxy;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ResultTest {

    private static _Scenario mockScenario(final ScenarioType type, final Fraction weight, final boolean failed) {
        return (_Scenario) Proxy.newProxyInstance(
                _Scenario.class.getClassLoader(),
                new Class<?>[]{_Scenario.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "type" -> type;
                    case "weightConfigured", "weightUpstream", "weightDownstream" -> weight;
                    case "hasFailed" -> failed;
                    default -> null;
                }
        );
    }

    @Test
    void testConstructorWithScenario() {
        final _Scenario scenario = mockScenario(ScenarioType.LEAF, Fraction.ONE, false);

        final Result result = new Result(scenario);
        assertEquals(ExecutionStatus.UNVISITED, result.status());

        result.tick();
        assertEquals(ExecutionStatus.PASSED, result.status());
        assertEquals(1, result.getExecutions());

        result.updateStatus(ExecutionStatus.FAILED);
        assertEquals(ExecutionStatus.FAILED, result.status());

        assertNotNull(result.toString());
    }

    @Test
    void testFullConstructor() {
        final Result result = new Result(
                ScenarioType.NODE,
                Fraction.ONE_HALF,
                Fraction.ONE_HALF,
                Fraction.ONE_HALF,
                Collections.emptyList(),
                2,
                1
        );

        assertEquals(ExecutionStatus.PASSED, result.status());
        assertEquals(ScenarioType.NODE, result.getScenarioType());
        assertEquals(Fraction.ONE_HALF, result.getConfiguredWeight());
        assertEquals(Fraction.ONE_HALF, result.getUpstreamWeight());
        assertEquals(Fraction.ONE_HALF, result.getDownstreamWeight());
        assertEquals(2, result.getExecutions());
        assertEquals(1, result.getInstances());
    }
}
