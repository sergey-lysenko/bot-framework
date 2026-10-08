package works.lysenko.base.output;

import org.junit.jupiter.api.Test;
import works.lysenko.base.output.TreeHtml.NodeData;
import works.lysenko.base.output.TreeHtml.TreeLayout;
import works.lysenko.tree.inheritance.Outer;
import works.lysenko.tree.inheritance.outer.Alias;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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
    void rendersSharedPackageChildrenBelowTheirAliasParent() {

        final Outer outer = new Outer();
        final Alias alias = (Alias) outer.getPool().getSortedSet().iterator().next();
        final TreeLayout layout = TreeHtml.computeLayout(TreeHtml.rootsOf(outer.list()), new TreeMap<>());
        final NodeData aliasNode = layout.nodes().stream()
                .filter(node -> node.label().startsWith(alias.getSimpleName()))
                .findFirst()
                .orElseThrow();
        final NodeData sharedChild = layout.nodes().stream()
                .filter(node -> node.parent() == aliasNode)
                .findFirst()
                .orElseThrow();

        assertEquals(aliasNode.col() + 1, sharedChild.col());
        assertTrue(layout.edges().stream().anyMatch(edge -> edge.from() == aliasNode && edge.to() == sharedChild));
        assertEquals(3, layout.nodes().size());
    }

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
            final List<NodeData> inCol = nodes.stream().filter(n -> n.col() == col).sorted(java.util.Comparator.comparingDouble(NodeData::row)).toList();
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
    void compactsDisconnectedTreesWithoutChangingTheirInternalLayout() {

        final NodeData firstRoot = new NodeData("first", "First", "Root", 0, 1.0, null);
        final NodeData firstChild = new NodeData("first-child", "FirstChild", "First", 1, 2.0, null);
        firstChild.setParent(firstRoot);
        firstRoot.children().add(firstChild);

        final NodeData secondRoot = new NodeData("second", "Second", "Root", 0, 12.0, null);
        final NodeData secondChild = new NodeData("second-child", "SecondChild", "Second", 1, 13.0, null);
        secondChild.setParent(secondRoot);
        secondRoot.children().add(secondChild);

        TreeHtml.compactDisconnectedTrees(List.of(firstRoot, firstChild, secondRoot, secondChild));

        assertEquals(1.0, firstRoot.row());
        assertEquals(2.0, firstChild.row());
        assertEquals(3.0, secondRoot.row());
        assertEquals(4.0, secondChild.row());
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

    @Test
    void testLayoutDeterminismAndNoEdgeCrossings() {
        final List<NodeData> run1Nodes = createTestGraph();
        final List<NodeData> run2Nodes = createTestGraph();

        // Permute/shuffle run2Nodes order to simulate non-deterministic initial list order
        java.util.Collections.shuffle(run2Nodes, new java.util.Random(42));

        TreeHtml.optimizeLayout(run1Nodes);
        TreeHtml.optimizeLayout(run2Nodes);

        // 1. Check 100% deterministic positioning across runs
        final Map<String, Double> run1Map = run1Nodes.stream().collect(java.util.stream.Collectors.toMap(NodeData::id, NodeData::row));
        final Map<String, Double> run2Map = run2Nodes.stream().collect(java.util.stream.Collectors.toMap(NodeData::id, NodeData::row));

        assertEquals(run1Map.size(), run2Map.size());
        for (final String id : run1Map.keySet()) {
            assertEquals(run1Map.get(id), run2Map.get(id), 1e-6, "Node " + id + " row must be identical across runs");
        }

        // 2. Check no edge crossings between adjacent columns
        for (final NodeData n1 : run1Nodes) {
            for (final NodeData n2 : run1Nodes) {
                if (n1.col() == n2.col() && n1.row() < n2.row()) {
                    for (final NodeData child1 : n1.children()) {
                        for (final NodeData child2 : n2.children()) {
                            if (child1.col() == child2.col()) {
                                assertTrue(child1.row() <= child2.row(),
                                        "Edge (" + n1.label() + " -> " + child1.label() + ") and ("
                                                + n2.label() + " -> " + child2.label() + ") must not cross");
                            }
                        }
                    }
                }
            }
        }
    }

    private static List<NodeData> createTestGraph() {
        final List<NodeData> nodes = new ArrayList<>();
        final NodeData root = new NodeData("col_0_0", "SignIn", "Root", 0, 1.0, null);
        nodes.add(root);

        final NodeData loginA = new NodeData("col_1_0", "CorrectLogin", "Login", 1, 1.0, null);
        final NodeData loginB = new NodeData("col_1_1", "WrongLogin", "Login", 1, 2.0, null);
        loginA.setParent(root); root.children().add(loginA);
        loginB.setParent(root); root.children().add(loginB);
        nodes.add(loginA); nodes.add(loginB);

        final NodeData modA1 = new NodeData("col_2_0", "Dashboard", "Main", 2, 1.0, null);
        final NodeData modA2 = new NodeData("col_2_1", "Reports", "Main", 2, 2.0, null);
        final NodeData modB1 = new NodeData("col_2_2", "AuditLogs", "Main", 2, 3.0, null);

        modA1.setParent(loginA); loginA.children().add(modA1);
        modA2.setParent(loginA); loginA.children().add(modA2);
        modB1.setParent(loginB); loginB.children().add(modB1);
        nodes.add(modA1); nodes.add(modA2); nodes.add(modB1);

        return nodes;
    }

    @Test
    void testComplex37LeafTreeHasNoEdgeCrossings() {
        final List<NodeData> nodes = createComplex37LeafGraph();
        TreeHtml.optimizeLayout(nodes);

        // Verify zero edge crossings between any adjacent columns
        for (final NodeData n1 : nodes) {
            for (final NodeData n2 : nodes) {
                if (n1.col() == n2.col() && n1.row() < n2.row()) {
                    for (final NodeData child1 : n1.children()) {
                        for (final NodeData child2 : n2.children()) {
                            if (child1.col() == child2.col()) {
                                assertTrue(child1.row() <= child2.row(),
                                        "Edge (" + n1.label() + " -> " + child1.label() + ") at row " + child1.row()
                                                + " and (" + n2.label() + " -> " + child2.label() + ") at row " + child2.row()
                                                + " must not cross");
                            }
                        }
                    }
                }
            }
        }
    }

    private static List<NodeData> createComplex37LeafGraph() {
        final List<NodeData> nodes = new ArrayList<>();
        final NodeData root = new NodeData("signIn", "SignIn", "Root", 0, 1.0, null);
        nodes.add(root);

        final NodeData correctLogin = new NodeData("correctLogin", "CorrectLogin", "Login", 1, 1.0, null);
        final NodeData wrongLogin = new NodeData("wrongLogin", "WrongLogin", "Login", 1, 2.0, null);
        final NodeData wrongPassword = new NodeData("wrongPassword", "WrongPassword", "Login", 1, 3.0, null);
        correctLogin.setParent(root); root.children().add(correctLogin);
        wrongLogin.setParent(root); root.children().add(wrongLogin);
        wrongPassword.setParent(root); root.children().add(wrongPassword);
        nodes.add(correctLogin); nodes.add(wrongLogin); nodes.add(wrongPassword);

        // Col 2 under CorrectLogin
        final NodeData aiUsage = new NodeData("aiUsage", "AiUsageReport", "Main", 2, 1.0, null);
        final NodeData appMgmt = new NodeData("appMgmt", "AppManagement", "Main", 2, 2.0, null);
        final NodeData auditLogs = new NodeData("auditLogs", "AuditLogs", "Main", 2, 3.0, null);
        final NodeData billing = new NodeData("billing", "Billing", "Main", 2, 4.0, null);
        final NodeData reports = new NodeData("reports", "Reports", "Main", 2, 5.0, null);
        final NodeData settings = new NodeData("settings", "Settings", "Main", 2, 6.0, null);

        for (final NodeData nd : List.of(aiUsage, appMgmt, auditLogs, billing, reports, settings)) {
            nd.setParent(correctLogin);
            correctLogin.children().add(nd);
            nodes.add(nd);
        }

        // Col 3
        final NodeData aiChild = new NodeData("aiChild", "Export", "Ai", 3, 1.0, null);
        aiChild.setParent(aiUsage); aiUsage.children().add(aiChild); nodes.add(aiChild);

        final NodeData details = new NodeData("details", "Details", "Audit", 3, 1.0, null);
        details.setParent(auditLogs); auditLogs.children().add(details); nodes.add(details);

        final NodeData inv = new NodeData("inv", "Invoices", "Bill", 3, 1.0, null);
        final NodeData mod = new NodeData("mod", "Modules", "Bill", 3, 2.0, null);
        inv.setParent(billing); billing.children().add(inv); nodes.add(inv);
        mod.setParent(billing); billing.children().add(mod); nodes.add(mod);

        final NodeData repBilling = new NodeData("repBilling", "Billing", "Rep", 3, 1.0, null);
        repBilling.setParent(reports); reports.children().add(repBilling); nodes.add(repBilling);

        final NodeData configProd = new NodeData("configProd", "ConfigureProducts", "Set", 3, 1.0, null);
        configProd.setParent(settings); settings.children().add(configProd); nodes.add(configProd);

        // Col 4
        final NodeData filter = new NodeData("filter", "Filter", "RepBill", 4, 1.0, null);
        filter.setParent(repBilling); repBilling.children().add(filter); nodes.add(filter);

        return nodes;
    }

    @Test
    void testNonExecutableNodeAndConnectorRendering() {
        final List<NodeData> nodes = new ArrayList<>();
        final List<Edge> edges = new ArrayList<>();

        final _Result rootResult = new Result(ScenarioType.NODE, Fraction.ONE, Fraction.ONE, Fraction.ONE, List.of(), 1, 1);
        final Result nonExecResult = new Result(ScenarioType.LEAF, null, null, null, new ArrayList<>(), 0, 0);

        final NodeData root = new NodeData("col_0_0", "RootApp", "Root", 0, 1.0, rootResult);
        final NodeData childNonExec = new NodeData("col_1_0", "DisabledFeature", "Feature", 1, 1.0, nonExecResult);
        childNonExec.setParent(root);
        root.children().add(childNonExec);

        nodes.add(root);
        nodes.add(childNonExec);
        edges.add(new Edge(root, childNonExec));

        final String html = TreeHtml.renderHtml(nodes, edges);

        // Node card must have non_executable class and title
        assertTrue(html.contains("class=\"node-card non_executable\""));
        assertTrue(html.contains("<title>DisabledFeature (Non-executable)</title>"));
        // Connector must have non_executable class
        assertTrue(html.contains("class=\"connector non_executable\" data-from=\"col_0_0\" data-to=\"col_1_0\""));
        // CSS rules must be present
        assertTrue(html.contains(".node-card.non_executable rect { fill: #131a2a; stroke: #283548; stroke-width: 1.2; stroke-dasharray: 4 4; }"));
        assertTrue(html.contains(".connector.non_executable { stroke: #243044; stroke-width: 1.2; stroke-dasharray: 4 4; opacity: 0.6; }"));
    }

    @Test
    void testChildFailedNodeAndConnectorRendering() {
        final List<NodeData> nodes = new ArrayList<>();
        final List<Edge> edges = new ArrayList<>();

        final Result rootResult = new Result(ScenarioType.NODE, Fraction.ONE, Fraction.ONE, Fraction.ONE, List.of(), 1, 1);
        rootResult.updateStatus(works.lysenko.util.data.enums.ExecutionStatus.CHILD_FAILED);

        final NodeData root = new NodeData("col_0_0", "RootApp", "Root", 0, 1.0, rootResult);
        final NodeData child = new NodeData("col_1_0", "ChildFeature", "Feature", 1, 1.0, rootResult);
        child.setParent(root);
        root.children().add(child);

        nodes.add(root);
        nodes.add(child);
        edges.add(new Edge(root, child));

        final String html = TreeHtml.renderHtml(nodes, edges);

        assertTrue(html.contains("class=\"node-card child_failed\""));
        assertTrue(html.contains("(Upset)"));
        assertTrue(html.contains("class=\"connector child_failed\" data-from=\"col_0_0\" data-to=\"col_1_0\""));
        assertTrue(html.contains(".node-card.child_failed rect { fill: #1e1b4b; stroke: #818cf8; stroke-width: 1.2; stroke-dasharray: 3 3; }"));
        assertTrue(html.contains(".connector.child_failed { stroke: #818cf8; stroke-width: 1.5; stroke-dasharray: 3 3; }"));
    }
}
