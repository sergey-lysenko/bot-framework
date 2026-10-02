package works.lysenko.base.output;

import org.junit.jupiter.api.Test;
import works.lysenko.base.output.TreeHtml.NodeData;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import works.lysenko.base.output.TreeHtml.Edge;
import works.lysenko.util.apis.data._Result;
import works.lysenko.util.apis.log._LogRecord;
import works.lysenko.util.data.enums.ScenarioType;
import org.apache.commons.math3.fraction.Fraction;
import works.lysenko.util.data.type.Result;


class TreeHtmlTest {

    @Test
    void testSolveColumn1DGuaranteesNonOverlapAndMinimization() {
        // Test targets that violate ordering (e.g. 10.0, 2.0, 5.0)
        final double[] targets = {10.0, 2.0, 5.0};
        final double[] resolved = TreeHtml.solveColumn1D(targets);

        assertEquals(3, resolved.length);
        assertTrue(resolved[0] >= 1.0);
        assertTrue(resolved[1] >= resolved[0] + 1.0 - 1e-6);
        assertTrue(resolved[2] >= resolved[1] + 1.0 - 1e-6);

        // Verify PAVA merges violators into optimal monotonic average
        // (10 - 0) = 10, (2 - 1) = 1, (5 - 2) = 3 -> mean of z is (10+1+3)/3 = 14/3 = 4.667
        // w = [4.667, 4.667, 4.667] -> y = [4.667, 5.667, 6.667]
        assertEquals(14.0 / 3.0, resolved[0], 1e-6);
        assertEquals(14.0 / 3.0 + 1.0, resolved[1], 1e-6);
        assertEquals(14.0 / 3.0 + 2.0, resolved[2], 1e-6);
    }

    @Test
    void testOptimizeLayoutBringsChildrenCloserToParents() {
        final List<NodeData> nodes = new ArrayList<>();

        // Col 0
        final NodeData root = new NodeData("col_0_0", "SignIn", "Root", 0, 1.0, null);
        nodes.add(root);

        // Col 1
        final NodeData correctLogin = new NodeData("col_1_0", "CorrectLogin", "Login", 1, 1.0, null);
        final NodeData wrongLogin = new NodeData("col_1_1", "WrongLogin", "Login", 1, 2.0, null);
        final NodeData wrongPassword = new NodeData("col_1_2", "WrongPassword", "Login", 1, 3.0, null);
        for (final NodeData n : List.of(correctLogin, wrongLogin, wrongPassword)) {
            n.setParent(root);
            root.children().add(n);
            nodes.add(n);
        }

        // Col 2: 11 children under CorrectLogin
        final List<NodeData> col2Nodes = new ArrayList<>();
        final String[] labels = {
                "AiUsageReport", "AppManagement", "AuditLogs", "Billing",
                "Dashboard", "FirstDelivery", "Logout", "Partners",
                "Payments", "Reports", "Settings"
        };
        for (int i = 0; i < labels.length; i++) {
            final NodeData nd = new NodeData("col_2_" + i, labels[i], "Main", 2, i + 1.0, null);
            nd.setParent(correctLogin);
            correctLogin.children().add(nd);
            nodes.add(nd);
            col2Nodes.add(nd);
        }

        final NodeData billing = col2Nodes.get(3);
        final NodeData settings = col2Nodes.get(10);

        // Col 3: ConfigureProducts under Settings
        final NodeData configureProducts = new NodeData("col_3_0", "ConfigureProducts", "Settings", 3, 1.0, null);
        configureProducts.setParent(settings);
        settings.children().add(configureProducts);
        nodes.add(configureProducts);

        // Col 4: Child under ConfigureProducts
        final NodeData settingsConfigureProducts = new NodeData("col_4_0", "SettingsConfigureProducts", "Configure", 4, 1.0, null);
        settingsConfigureProducts.setParent(configureProducts);
        configureProducts.children().add(settingsConfigureProducts);
        nodes.add(settingsConfigureProducts);

        TreeHtml.optimizeLayout(nodes);

        // 1. Check non-overlapping constraint: within each column, each subsequent node row >= prevRow + 1.0
        for (int c = 0; c <= 4; c++) {
            final int col = c;
            final List<NodeData> inCol = nodes.stream().filter(n -> n.col() == col).toList();
            for (int i = 1; i < inCol.size(); i++) {
                assertTrue(inCol.get(i).row() >= inCol.get(i - 1).row() + 1.0 - 1e-6,
                        "Nodes in col " + col + " must not overlap");
            }
        }

        // 2. Settings is child 10 in col 2, so it is positioned deep (around row 10+)
        assertTrue(settings.row() >= 10.0, "Settings should be at row 10+ but was " + settings.row());

        // 3. ConfigureProducts in col 3 must be moved lower near Settings, not at row 1
        assertTrue(configureProducts.row() >= 10.0,
                "ConfigureProducts should be near Settings (>= 10.0) but was " + configureProducts.row());

        // 4. SettingsConfigureProducts in col 4 must be moved lower near ConfigureProducts, not at row 1
        assertTrue(settingsConfigureProducts.row() >= 10.0,
                "SettingsConfigureProducts should be near ConfigureProducts (>= 10.0) but was " + settingsConfigureProducts.row());
    }

    @Test
    void testVisitedAndUnvisitedEdgesRendering() {
        final List<NodeData> nodes = new ArrayList<>();
        final List<Edge> edges = new ArrayList<>();

        final _Result visitedResult = new Result(ScenarioType.NODE, Fraction.ONE, Fraction.ONE, Fraction.ONE, List.of(), 1, 1);
        final _Result unvisitedResult = new Result(ScenarioType.LEAF, Fraction.ONE, Fraction.ONE, Fraction.ONE, List.of(), 0, 1);

        final NodeData root = new NodeData("col_0_0", "SignIn", "Root", 0, 1.0, visitedResult);
        final NodeData childVisited = new NodeData("col_1_0", "CorrectLogin", "Login", 1, 1.0, visitedResult);
        final NodeData childUnvisited = new NodeData("col_1_1", "WrongLogin", "Login", 1, 2.0, unvisitedResult);

        nodes.add(root);
        nodes.add(childVisited);
        nodes.add(childUnvisited);

        edges.add(new Edge(root, childVisited));
        edges.add(new Edge(root, childUnvisited));

        final String html = TreeHtml.renderHtml(nodes, edges);

        // Edge to childVisited should have class "connector visited"
        assertTrue(html.contains("class=\"connector visited\" data-from=\"col_0_0\" data-to=\"col_1_0\""));
        // Edge to childUnvisited should have class "connector"
        assertTrue(html.contains("class=\"connector\" data-from=\"col_0_0\" data-to=\"col_1_1\""));
        // CSS must contain the green visited connector styling
        assertTrue(html.contains(".connector.visited { stroke: #22c55e; stroke-width: 2.5; }"));
    }

}
