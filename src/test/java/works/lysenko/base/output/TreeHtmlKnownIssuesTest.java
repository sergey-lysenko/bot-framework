package works.lysenko.base.output;

import org.apache.commons.math3.fraction.Fraction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import works.lysenko.base.issues.KnownIssue;
import works.lysenko.base.issues.KnownIssuesStore;
import works.lysenko.tree.base.Leaf;
import works.lysenko.util.apis.scenario._Scenario;
import works.lysenko.util.data.enums.ScenarioType;
import works.lysenko.util.data.type.Result;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TreeHtmlKnownIssuesTest {

    @Test
    @DisplayName("Renders bug icon badge and details panel HTML for nodes with known issues")
    void testTreeHtmlKnownIssuesRendering() {
        final KnownIssue issue = new KnownIssue(
                "TestScenario",
                "Known Timeout",
                "Server is slow during peak hours",
                "https://jira.example.com/BUG-123",
                ""
        );

        final KnownIssuesStore store = new KnownIssuesStore(List.of(issue));
        TreeHtml.setKnownIssuesStore(store);

        final _Scenario scenario = new Leaf(Fraction.ONE) {
            @Override
            public String getName() {
                return "TestScenario";
            }

            @Override
            public String getSimpleName() {
                return "TestScenario";
            }

            @Override
            public ScenarioType type() {
                return ScenarioType.LEAF;
            }
        };

        final Result res = new Result(scenario);
        final TreeHtml.NodeData node = new TreeHtml.NodeData("id1", "TestScenario [T]", "Tests", 0, 1.0, res, scenario);
        node.knownIssues().add(issue);

        final String html = TreeHtml.renderHtml(List.of(node), List.of());

        assertNotNull(html);
        assertTrue(html.contains("🐞"));
        assertTrue(html.contains("Known Timeout"));
        assertTrue(html.contains("Server is slow during peak hours"));
        assertTrue(html.contains("https://jira.example.com/BUG-123"));
        assertTrue(html.contains("Known Issues:"));
    }
}
