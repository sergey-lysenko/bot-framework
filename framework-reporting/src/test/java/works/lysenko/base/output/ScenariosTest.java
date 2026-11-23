package works.lysenko.base.output;

import org.apache.commons.math3.fraction.Fraction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import works.lysenko.testing.SelectionFixture;
import works.lysenko.tree.base.Leaf;
import works.lysenko.util.apis.data._Result;
import works.lysenko.util.apis.scenario._Scenario;
import works.lysenko.util.data.enums.ScenarioType;
import works.lysenko.util.data.type.Result;
import works.lysenko.util.prop.tree.Notify;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScenariosTest {

    private SelectionFixture fixture;

    @BeforeEach
    void setUp() {
        fixture = SelectionFixture.install();
        Notify.muteDownstreamWeightChange = false;
    }

    @AfterEach
    void tearDown() {
        fixture.close();
    }

    @Test
    void downstreamWeightChangeEmitsS3ByDefault() {
        final Map<_Scenario, _Result> results = sharedScenarioWithDifferentDownstreamWeights();

        Scenarios.aggregateResult(results);

        assertEquals(1, fixture.events.size(), String.valueOf(fixture.events));
        assertTrue(fixture.events.get(0).startsWith("S3:"), fixture.events.get(0));
        assertTrue(fixture.events.get(0).toLowerCase().contains("downstream weight"), fixture.events.get(0));
    }

    @Test
    void muteDownstreamWeightChangeDemotesS3ToDebug() {
        Notify.muteDownstreamWeightChange = true;
        final Map<_Scenario, _Result> results = sharedScenarioWithDifferentDownstreamWeights();

        final Map<String, _Result> aggregated = Scenarios.aggregateResult(results);

        assertEquals(1, aggregated.size());
        assertEquals(2, aggregated.values().iterator().next().getInstances());
        assertTrue(fixture.events.isEmpty(), "downstream weight change notice should be demoted: " + fixture.events);
    }

    @Test
    void muteDownstreamWeightChangeStillEmitsConfiguredAndUpstreamWeightNotices() {
        Notify.muteDownstreamWeightChange = true;
        final SharedPrefab first = new SharedPrefab();
        final SharedPrefab second = new SharedPrefab();
        final Map<_Scenario, _Result> results = new LinkedHashMap<>();
        results.put(first, new Result(first, ScenarioType.LEAF, Fraction.ONE, Fraction.ZERO, Fraction.ONE, List.of(), 1, 1));
        results.put(second, new Result(second, ScenarioType.LEAF, Fraction.TWO, Fraction.ONE, Fraction.TWO, List.of(), 1, 1));

        Scenarios.aggregateResult(results);

        assertEquals(2, fixture.events.size(), String.valueOf(fixture.events));
        assertTrue(fixture.events.stream().anyMatch(e -> e.startsWith("S3:") && e.toLowerCase().contains("configured weight")),
                String.valueOf(fixture.events));
        assertTrue(fixture.events.stream().anyMatch(e -> e.startsWith("S3:") && e.toLowerCase().contains("upstream weight")),
                String.valueOf(fixture.events));
        assertTrue(fixture.events.stream().noneMatch(e -> e.toLowerCase().contains("downstream weight")),
                String.valueOf(fixture.events));
    }

    private static Map<_Scenario, _Result> sharedScenarioWithDifferentDownstreamWeights() {
        final SharedPrefab first = new SharedPrefab();
        final SharedPrefab second = new SharedPrefab();
        final Map<_Scenario, _Result> results = new LinkedHashMap<>();
        results.put(first, new Result(first, ScenarioType.LEAF, Fraction.ONE, Fraction.ZERO, Fraction.ONE, List.of(), 1, 1));
        results.put(second, new Result(second, ScenarioType.LEAF, Fraction.ONE, Fraction.ZERO, Fraction.TWO, List.of(), 1, 1));
        return results;
    }

    private static final class SharedPrefab extends Leaf {
        SharedPrefab() {
            super(Fraction.ONE);
        }

        @Override
        public String getShortName() {
            return "shared.prefab.SharedPrefab";
        }
    }
}
