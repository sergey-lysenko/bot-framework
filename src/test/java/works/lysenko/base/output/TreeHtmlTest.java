package works.lysenko.base.output;

import org.junit.jupiter.api.Test;
import works.lysenko.base.output.TreeHtml.NodeData;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TreeHtmlTest {

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
}
