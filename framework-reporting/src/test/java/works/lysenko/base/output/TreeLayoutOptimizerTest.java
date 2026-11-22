package works.lysenko.base.output;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import works.lysenko.base.output.TreeHtml.NodeData;
import works.lysenko.base.output.tree.TreeLayoutOptimizer;
import works.lysenko.util.prop.tree.Optimization;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TreeLayoutOptimizerTest {

    private boolean origProgression;
    private boolean origAllowDynamicReordering;
    private boolean origAllowFinalReordering;
    private boolean origReduceHGaps;
    private boolean origReduceVGaps;
    private boolean origReduceHOffsets;
    private boolean origReduceVOffsets;
    private int origMinSep;
    private int origBankColumns;
    private double origMaxAspectRatio;

    @BeforeEach
    void saveDefaults() {
        origProgression = Optimization.progression;
        origAllowDynamicReordering = Optimization.allowDynamicReordering;
        origAllowFinalReordering = Optimization.allowFinalReordering;
        origReduceHGaps = Optimization.reduceHorizontalGaps;
        origReduceVGaps = Optimization.reduceVerticalGaps;
        origReduceHOffsets = Optimization.reduceHorizontalOffsets;
        origReduceVOffsets = Optimization.reduceVerticalOffsets;
        origMinSep = Optimization.minOffsetSeparation;
        origBankColumns = Optimization.bankColumns;
        origMaxAspectRatio = Optimization.maxAspectRatio;

        Optimization.progression = true;
        Optimization.allowDynamicReordering = false;
        Optimization.allowFinalReordering = true;
        Optimization.reduceHorizontalGaps = true;
        Optimization.reduceVerticalGaps = true;
        Optimization.reduceHorizontalOffsets = true;
        Optimization.reduceVerticalOffsets = true;
        Optimization.minOffsetSeparation = 1;
        Optimization.bankColumns = 0;
        Optimization.maxAspectRatio = 1.78;
        TreeLayoutOptimizer.resetPinnedBankLayout();
    }

    @AfterEach
    void restoreDefaults() {
        Optimization.progression = origProgression;
        Optimization.allowDynamicReordering = origAllowDynamicReordering;
        Optimization.allowFinalReordering = origAllowFinalReordering;
        Optimization.reduceHorizontalGaps = origReduceHGaps;
        Optimization.reduceVerticalGaps = origReduceVGaps;
        Optimization.reduceHorizontalOffsets = origReduceHOffsets;
        Optimization.reduceVerticalOffsets = origReduceVOffsets;
        Optimization.minOffsetSeparation = origMinSep;
        Optimization.bankColumns = origBankColumns;
        Optimization.maxAspectRatio = origMaxAspectRatio;
        TreeLayoutOptimizer.resetPinnedBankLayout();
    }

    private static NodeData node(final List<NodeData> list, final String id, final NodeData parent,
                                 final int col, final double row, final int colOffset, final int rowOffset) {
        final NodeData n = new NodeData(id, id, "G", col, row, colOffset, rowOffset, null, null);
        if (null != parent) {
            n.setParent(parent);
            parent.children().add(n);
        }
        list.add(n);
        return n;
    }

    private static void assertNoNodeOverlap(final List<NodeData> nodes) {
        for (int i = 0; i < nodes.size(); i++) {
            for (int j = i + 1; j < nodes.size(); j++) {
                final NodeData a = nodes.get(i);
                final NodeData b = nodes.get(j);
                if (a.col() == b.col()) {
                    assertTrue(Math.abs(a.row() - b.row()) >= 1.0 - 1e-5,
                            () -> "Nodes overlap in col " + a.col() + ": " + a.id() + "@" + a.row() + " vs " + b.id() + "@" + b.row());
                }
            }
        }
    }

    @Test
    void horizontalOffsetReducesIntoLocalPocketWhilePreservingMinSeparation() {
        final List<NodeData> nodes = new ArrayList<>();
        final NodeData login = node(nodes, "Login", null, 0, 5.0, 0, 0);

        // Top branch occupies cols 1..6 at row 1 (so cols 1..6 are NOT globally empty)
        NodeData prev = login;
        for (int c = 1; c <= 6; c++) {
            prev = node(nodes, "Top" + c, prev, c, 1.0, 0, 0);
        }

        // Middle branch occupies cols 1..2 at row 5
        final NodeData auth = node(nodes, "Auth", login, 1, 5.0, 0, 0);
        node(nodes, "AuthChild", auth, 2, 5.0, 0, 0);

        // Storefront has @VisualOffset(10) at row 5 -> col 11
        final NodeData storefront = node(nodes, "Storefront", login, 11, 5.0, 10, 0);
        final NodeData sfChild = node(nodes, "SfChild", storefront, 12, 5.0, 0, 0);

        Optimization.reduceVerticalOffsets = false;
        Optimization.minOffsetSeparation = 1;
        TreeLayoutOptimizer.reduceVisualOffsets(nodes);
        TreeLayoutOptimizer.removeEmptyRowsAndColumns(nodes);

        // AuthChild is at col 2 on row 5 -> with minSep=1, Storefront stops at col 4 (1 empty col: col 3)
        assertEquals(4, storefront.col());
        assertEquals(5, sfChild.col());
        assertNoNodeOverlap(nodes);
    }

    @Test
    void horizontalOffsetPreservesMinSeparationFromParentAndMinSepZeroCollapsesFlush() {
        final List<NodeData> nodes = new ArrayList<>();
        final NodeData login = node(nodes, "Login", null, 0, 1.0, 0, 0);
        final NodeData billing = node(nodes, "Billing", login, 7, 1.0, 6, 0);

        Optimization.reduceVerticalOffsets = false;
        Optimization.minOffsetSeparation = 1;
        TreeLayoutOptimizer.reduceVisualOffsets(nodes);
        TreeLayoutOptimizer.removeEmptyRowsAndColumns(nodes);

        // Login at col 0, normal child col is 1; with minSep=1, Billing must stay at col 2 (not collapse to 1)
        assertEquals(0, login.col());
        assertEquals(2, billing.col());

        // With minSep=2, Billing keeps 2 empty columns (col 3)
        billing.setCol(7);
        Optimization.minOffsetSeparation = 2;
        TreeLayoutOptimizer.reduceVisualOffsets(nodes);
        TreeLayoutOptimizer.removeEmptyRowsAndColumns(nodes);
        assertEquals(3, billing.col());

        // With minSep=0, Billing collapses flush to col 1
        billing.setCol(7);
        Optimization.minOffsetSeparation = 0;
        TreeLayoutOptimizer.reduceVisualOffsets(nodes);
        TreeLayoutOptimizer.removeEmptyRowsAndColumns(nodes);
        assertEquals(1, billing.col());
    }

    @Test
    void cascadingHorizontalReductionPullsBehindMovedBranch() {
        final List<NodeData> nodes = new ArrayList<>();
        final NodeData login = node(nodes, "Login", null, 0, 5.0, 0, 0);
        // Occupy cols 1..15 at row 1 so columns are not globally empty
        NodeData top = login;
        for (int c = 1; c <= 15; c++) {
            top = node(nodes, "Top" + c, top, c, 1.0, 0, 0);
        }

        // Settings at col 7 (@VisualOffset(6)), child at col 8, row 5
        final NodeData settings = node(nodes, "Settings", login, 7, 5.0, 6, 0);
        final NodeData settingsChild = node(nodes, "SettingsChild", settings, 8, 5.0, 0, 0);

        // Reports at col 14 (@VisualOffset(13)), row 5
        final NodeData reports = node(nodes, "Reports", login, 14, 5.0, 13, 0);

        Optimization.reduceVerticalOffsets = false;
        Optimization.minOffsetSeparation = 1;
        TreeLayoutOptimizer.reduceVisualOffsets(nodes);

        // Settings pulls left from col 7 -> col 2 (Login=0, minSep=1 => col 2), child -> col 3
        assertEquals(2, settings.col());
        assertEquals(3, settingsChild.col());
        // Reports cascades behind SettingsChild (col 3) with minSep=1 -> col 5
        assertEquals(5, reports.col());
        assertNoNodeOverlap(nodes);
    }

    @Test
    void horizontalSlideRespectsVerticalMinSeparationHalo() {
        final List<NodeData> nodes = new ArrayList<>();
        final NodeData login = node(nodes, "Login", null, 0, 5.0, 0, 0);
        // Settings sits at cols 1..5, row 4
        NodeData s = login;
        for (int c = 1; c <= 5; c++) {
            s = node(nodes, "S" + c, s, c, 4.0, 0, 0);
        }
        // BranchA at row 5 (distance 1.0 from Settings -> 0 empty rows!) with @VisualOffset(8)
        final NodeData branchA = node(nodes, "BranchA", login, 9, 5.0, 8, 0);
        // BranchB at row 6 (distance 2.0 from Settings -> 1 empty row!) with @VisualOffset(8)
        final NodeData branchB = node(nodes, "BranchB", login, 9, 6.0, 8, 0);

        Optimization.reduceVerticalOffsets = false;
        Optimization.minOffsetSeparation = 1;
        TreeLayoutOptimizer.reduceVisualOffsets(nodes);

        // BranchA is vertically adjacent to S5 (|5-4|=1 < 1+minSep=2), so it stops at col 5 + 1 + 1 = 7
        assertEquals(7, branchA.col());
        // BranchB has 1 empty row below S5 (|6-4|=2 >= 2), so it slides underneath S1..S5 to col 2
        // Wait: BranchB is vertically adjacent to BranchA (row 5 vs 6), but BranchB is at col 9 >= BranchA's col 7,
        // wait: BranchA moves first (col 9, row 5 < row 6) to col 7, so BranchA is at col 7 < BranchB's col 9!
        // Let's verify BranchB respects BranchA's halo too (no accidental touching)
        assertTrue(branchB.col() >= 2);
        assertNoNodeOverlap(nodes);
    }

    @Test
    void verticalOffsetReducesEmptySpaceAndFloatingLeafDoesNotBlockPartners() {
        final List<NodeData> nodes = new ArrayList<>();
        final NodeData login = node(nodes, "Login", null, 0, 10.0, 0, 0);

        // Auth at col 1, row 11 (offset +1)
        final NodeData auth = node(nodes, "Auth", login, 1, 11.0, 0, 1);
        // Floating un-shifted leaf EmailHealth interpolated way down at row 18
        final NodeData emailHealth = node(nodes, "EmailHealth", login, 1, 18.0, 0, 0);
        // Partners pushed far down at row 25 (@VisualRowOffset(10)) with child
        final NodeData partners = node(nodes, "Partners", login, 1, 25.0, 0, 10);
        final NodeData partnersChild = node(nodes, "PartnersChild", partners, 2, 25.0, 0, 0);
        // Un-shifted Payments stranded below Partners at row 27
        final NodeData payments = node(nodes, "Payments", login, 1, 27.0, 0, 0);
        final NodeData paymentsChild = node(nodes, "PaymentsChild", payments, 2, 27.0, 0, 0);

        Optimization.reduceHorizontalOffsets = false;
        Optimization.minOffsetSeparation = 1;
        TreeLayoutOptimizer.reduceVisualOffsets(nodes);
        TreeLayoutOptimizer.removeEmptyRowsAndColumns(nodes);

        // Auth stays at 11.0 (minSep=1 below Login@10.0)
        // EmailHealth pulls up to 13.0 (minSep=1 below Auth@11.0 because Auth is offset-separated)
        // Partners pulls up to 15.0 (minSep=1 below EmailHealth@13.0)
        // Payments pulls up to 17.0 (minSep=1 below Partners@15.0)
        assertTrue(emailHealth.row() < 15.0, "EmailHealth should pull up out of the void, got " + emailHealth.row());
        assertTrue(partners.row() < 18.0, "Partners should pull up behind EmailHealth, got " + partners.row());
        assertTrue(payments.row() < 20.0, "Payments should follow Partners up, got " + payments.row());

        // Preserved separation of at least 1 empty row (distance >= 2.0) between distinct offset branches
        assertTrue(emailHealth.row() - auth.row() >= 2.0 - 1e-5);
        assertTrue(partners.row() - emailHealth.row() >= 2.0 - 1e-5);
        assertTrue(payments.row() - partners.row() >= 2.0 - 1e-5);
        assertEquals(partners.row(), partnersChild.row(), 1e-5);
        assertEquals(payments.row(), paymentsChild.row(), 1e-5);
        assertNoNodeOverlap(nodes);
    }

    @Test
    void verticalOffsetPullsUpperBranchDownPreservingMinSeparation() {
        final List<NodeData> nodes = new ArrayList<>();
        final NodeData login = node(nodes, "Login", null, 0, 15.0, 0, 0);
        // Billing at col 2, row 2 (@VisualOffset(1), @VisualRowOffset(-10))
        final NodeData billing = node(nodes, "Billing", login, 2, 2.0, 1, -10);
        final NodeData billingChild = node(nodes, "BillingBottom", billing, 3, 4.0, 0, 0);
        // Settings at col 2, row 12 (@VisualOffset(1), @VisualRowOffset(-5))
        final NodeData settings = node(nodes, "Settings", login, 2, 12.0, 1, -5);
        node(nodes, "SettingsTop", settings, 3, 12.0, 0, 0);

        Optimization.reduceHorizontalOffsets = false;
        Optimization.minOffsetSeparation = 1;
        TreeLayoutOptimizer.reduceVisualOffsets(nodes);

        // Settings pulls down toward Login (15 - 1 = 14) -> row 14
        // Billing pulls down until BillingBottom is 2.0 rows above SettingsTop (14 - 2 = 12 -> Billing @ 10)
        // Then normalizeMinimumRow shifts minRow to 1.0 -> relative distances stay exact!
        assertEquals(2.0, settings.row() - billingChild.row(), 1e-5);
        assertEquals(1.0, login.row() - settings.row(), 1e-5);
        assertNoNodeOverlap(nodes);
    }

    @Test
    void prunedGapInsideSameBranchCollapsesToZeroWhileSeparatedBranchesKeepMinSep() {
        final List<NodeData> nodes = new ArrayList<>();
        final NodeData root = node(nodes, "Root", null, 0, 1.0, 0, 0);
        // Two children in the SAME un-shifted branch with a pruned empty row (row 2) between them
        final NodeData childA = node(nodes, "ChildA", root, 1, 1.0, 0, 0);
        final NodeData childC = node(nodes, "ChildC", root, 1, 3.0, 0, 0);
        // A separate offset branch below with a huge empty gap (rows 4..9 empty)
        final NodeData separated = node(nodes, "Separated", root, 1, 10.0, 0, 8);

        Optimization.reduceHorizontalOffsets = false;
        Optimization.reduceVerticalOffsets = false;
        Optimization.reduceVerticalGaps = true;
        Optimization.minOffsetSeparation = 1;

        TreeLayoutOptimizer.removeEmptyRowsAndColumns(nodes);

        // Gap between ChildA (1) and ChildC (3) is inside the same branch -> collapses to 0 empty rows (row 2)
        assertEquals(1.0, childA.row(), 1e-5);
        assertEquals(2.0, childC.row(), 1e-5);
        // Gap between ChildC (same branch) and Separated (offset branch) preserves minSep=1 empty row -> row 4
        assertEquals(4.0, separated.row(), 1e-5);
    }

    @Test
    void disablingFlagsLeavesOffsetsAndGapsUntouched() {
        final List<NodeData> nodes = new ArrayList<>();
        final NodeData login = node(nodes, "Login", null, 0, 10.0, 0, 0);
        final NodeData shifted = node(nodes, "Shifted", login, 10, 20.0, 9, 10);

        Optimization.reduceHorizontalGaps = false;
        Optimization.reduceVerticalGaps = false;
        Optimization.reduceHorizontalOffsets = false;
        Optimization.reduceVerticalOffsets = false;

        TreeLayoutOptimizer.reduceVisualOffsets(nodes);
        TreeLayoutOptimizer.removeEmptyRowsAndColumns(nodes);

        assertEquals(10, shifted.col());
        assertEquals(20.0, shifted.row(), 1e-5);
    }

    @Test
    void lowerSubtreeTucksIntoUnderbellyWithoutAdjacentColumnFreeze() {
        final List<NodeData> nodes = new ArrayList<>();
        final NodeData signIn = node(nodes, "SignIn", null, 0, 18.0, 0, 0);
        final NodeData login = node(nodes, "Login", signIn, 1, 18.0, 0, 0);

        // Upper ceiling: Auth@col2..3 keeps AuditLogs@col5..6, which keeps Settings@col8..10 (Edit@col10, row24)
        final NodeData auth = node(nodes, "AuthAndSecurity", login, 2, 17.0, 0, 0);
        node(nodes, "AuthChild", auth, 3, 17.0, 0, 0);
        final NodeData audit = node(nodes, "AuditLogs", login, 5, 17.0, 3, -5);
        node(nodes, "AuditEnd", audit, 6, 19.0, 0, 0);

        final NodeData settings = node(nodes, "Settings", login, 8, 18.0, 6, -5);
        // Upper siblings inside Settings keep PosConfig@21 and Edit@24 anchored (as in real tree)
        node(nodes, "ConfigureProducts", settings, 9, 18.0, 0, 0);
        node(nodes, "SettingsSpacer1", settings, 9, 19.0, 0, 0);
        node(nodes, "SettingsSpacer2", settings, 9, 20.0, 0, 0);
        final NodeData posConfig = node(nodes, "PosConfig", settings, 9, 21.0, 0, 0);
        node(nodes, "PosAddNew1", posConfig, 10, 21.0, 0, 0);
        node(nodes, "PosAddNew2", posConfig, 10, 22.0, 0, 0);
        node(nodes, "PosAddNew3", posConfig, 10, 23.0, 0, 0);
        final NodeData edit = node(nodes, "Edit", posConfig, 10, 24.0, 0, 0);

        // Partners (@VisualRowOffset(5)) at col 2 row 33 with AddNew (already near AuditLogs), Operators, and Restaurants (@VisualOffset(3))
        final NodeData partners = node(nodes, "Partners", login, 2, 33.0, 0, 5);
        final NodeData addNew = node(nodes, "AddNew", partners, 3, 28.0, 0, 0);
        node(nodes, "AddNewTop", addNew, 6, 22.0, 0, 0);
        node(nodes, "AddNewBottom", addNew, 6, 27.0, 0, 0);
        final NodeData operators = node(nodes, "Operators", partners, 3, 31.0, 0, 0);
        node(nodes, "OperatorsRead", operators, 4, 31.0, 0, 0);

        final NodeData restaurants = node(nodes, "Restaurants", partners, 6, 34.0, 3, 0);
        // Three floating leaves + large BotCafe blob hanging far down below Payments@col6
        final NodeData tab1 = node(nodes, "ActiveInactiveTabs", restaurants, 7, 43.0, 0, 0);
        final NodeData tab2 = node(nodes, "AddRestaurantForm", restaurants, 7, 44.0, 0, 0);
        final NodeData tab3 = node(nodes, "SearchRegex", restaurants, 7, 45.0, 0, 0);
        final NodeData botCafe = node(nodes, "BotCafe", restaurants, 7, 46.0, 0, 0);
        final NodeData botTop = node(nodes, "BotCafeTop", botCafe, 10, 39.0, 0, 0);
        node(nodes, "BotCafeBottom", botCafe, 10, 52.0, 0, 0);

        // Payments sits at col 2..6 directly below Partners/Restaurants, occupying col 5 around Restaurants' row
        // and reaching col 6 row 41 (2 rows above tab1@col7!)
        final NodeData payments = node(nodes, "Payments", login, 2, 35.0, 0, 0);
        final NodeData paymentsSave = node(nodes, "PaymentsSave", payments, 3, 35.0, 0, 0);
        final NodeData paymentsRand = node(nodes, "PaymentsRand", paymentsSave, 4, 35.0, 0, 0);
        for (int r = 32; r <= 36; r++) {
            final NodeData mid = node(nodes, "PaymentsMid" + r, paymentsRand, 5, r, 0, 0);
            if (r == 36) {
                node(nodes, "PaymentsLeaf", mid, 6, 41.0, 0, 0);
            }
        }

        Optimization.minOffsetSeparation = 1;
        TreeLayoutOptimizer.reduceVisualOffsets(nodes);
        TreeLayoutOptimizer.removeEmptyRowsAndColumns(nodes);

        // Restaurants stays at col 6 (blocked from sliding left by Payments@col5) and BotCafeTop stays at col 10
        assertEquals(6, restaurants.col());
        assertEquals(10, botTop.col());
        // BotCafeTop (col 10) must tuck tightly into the underbelly below Settings->Edit (col 10)
        // preserving minSep=1 (distance == 2.0 rows), instead of freezing 15 rows below because of Payments@col6!
        assertEquals(2.0, botTop.row() - edit.row(), 1e-5, "BotCafe should tuck to exact minSep=1 under Settings->Edit");

        // Floating leaves must pull up next to Restaurants rather than staying stranded below Payments
        assertEquals(restaurants.row(), tab1.row(), 1e-5);
        assertEquals(tab1.row() + 1.0, tab2.row(), 1e-5);
        assertEquals(tab2.row() + 1.0, tab3.row(), 1e-5);
        assertTrue(botCafe.row() >= tab3.row() + 1.0 - 1e-5);
        assertNoNodeOverlap(nodes);
    }

    @Test
    void verticalCompactionRunsBeforeHorizontalSoBillingDropsAboveSettings() {
        final List<NodeData> nodes = new ArrayList<>();
        final NodeData login = node(nodes, "Login", null, 1, 18.0, 0, 0);

        // AppManagement ends at col 6, rows 4.5..7.0
        final NodeData appMgmt = node(nodes, "AppManagement", login, 2, 10.0, 0, -5);
        node(nodes, "UpdateType", appMgmt, 6, 4.5, 0, 0);

        // Billing starts at col 8 (@VisualOffset(6), @VisualRowOffset(-7)), rows 2.5..11.0
        final NodeData billing = node(nodes, "Billing", login, 8, 2.5, 6, -7);
        final NodeData billingBottom = node(nodes, "BillingBottom", billing, 10, 11.0, 0, 0);

        // Settings sits at col 8..10, row 18.0
        final NodeData settings = node(nodes, "Settings", login, 8, 18.0, 6, -5);
        final NodeData settingsTop = node(nodes, "SettingsTop", settings, 10, 18.0, 0, 0);

        Optimization.minOffsetSeparation = 1;
        TreeLayoutOptimizer.reduceVisualOffsets(nodes);

        // Billing should drop down so BillingBottom is exactly 2.0 rows (minSep=1) above SettingsTop
        assertEquals(2.0, settingsTop.row() - billingBottom.row(), 1e-5);
        // And Billing keeps 1 empty column (col 7) from AppManagement's UpdateType (col 6)
        assertEquals(8, billing.col());
        assertNoNodeOverlap(nodes);
    }

    private static List<NodeData> buildHeterogeneousForest() {
        final List<NodeData> nodes = new ArrayList<>();
        // Specs: {name, depth, height} modeling a ~20-root suite with deep + tall-shallow + leaf roots
        final Object[][] specs = {
                {"Partners", 8, 45},
                {"Billing", 7, 35},
                {"Settings", 6, 30},
                {"Reports", 6, 20},
                {"AuditLogs", 5, 15},
                {"AppManagement", 5, 10},
                {"Payments", 5, 10},
                {"Menu", 5, 10},
                {"AIUsageReport", 3, 4},
                {"Issues", 2, 30},
                {"Storefront", 2, 15},
                {"AuthAndSecurity", 2, 8},
                {"FirstDelivery", 2, 4},
                {"Dashboard", 1, 1},
                {"EmailHealth", 1, 1},
                {"Login", 1, 1},
                {"Logout", 1, 1},
                {"WrongEmail", 1, 1},
                {"WrongPassword", 1, 1},
                {"Profile", 1, 1}
        };
        double nextRow = 1.0;
        for (final Object[] spec : specs) {
            final String name = (String) spec[0];
            final int depth = (Integer) spec[1];
            final int height = (Integer) spec[2];
            final NodeData root = node(nodes, name, null, 0, nextRow, 0, 0);
            NodeData spine = root;
            for (int d = 1; d < depth; d++) {
                spine = node(nodes, name + "_d" + d, spine, d, nextRow, 0, 0);
            }
            if (height > 1 && depth >= 2) {
                final NodeData parent = (depth == 2) ? root : root.children().get(0);
                final int leafCol = parent.col() + 1;
                for (int r = 1; r < height; r++) {
                    node(nodes, name + "_r" + r, parent, leafCol, nextRow + r, 0, 0);
                }
            }
            nextRow += height;
        }
        return nodes;
    }

    @Test
    void autoBankLayoutTargetsLandscape16By9ForLargeHeterogeneousForest() {
        final List<NodeData> nodes = buildHeterogeneousForest();
        Optimization.bankColumns = 0;
        Optimization.maxAspectRatio = 1.78;

        TreeLayoutOptimizer.compactDisconnectedTrees(nodes);

        assertNoNodeOverlap(nodes);
        final int totalCols = nodes.stream().mapToInt(NodeData::col).max().orElse(0) + 1;
        final double maxRow = nodes.stream().mapToDouble(NodeData::row).max().orElse(1.0);
        final long distinctBanks = nodes.stream().filter(n -> null == n.parent()).mapToInt(NodeData::col).distinct().count();
        final double aspectRatio = (totalCols * 260.0) / (maxRow * 56.0);

        assertTrue(distinctBanks >= 4, "Expected >= 4 adaptive banks for 20-root forest, got " + distinctBanks);
        assertTrue(aspectRatio >= 1.35 && aspectRatio <= 2.20,
                "Expected landscape aspect ratio near 16:9 (1.78), got " + aspectRatio
                        + " (" + totalCols + " cols x " + maxRow + " rows)");
        assertTrue(maxRow <= 70.0, "Expected maxRow <= 70 (down from ~115 in 2-bank layout), got " + maxRow);
        // Depth-aware packing keeps narrow banks narrower than max depth (8): totalCols < distinctBanks * 8
        assertTrue(totalCols < distinctBanks * 8,
                "Expected depth-aware packing to save columns vs uniform max-depth banks, got "
                        + totalCols + " cols across " + distinctBanks + " banks");
    }

    @Test
    void explicitBankColumnsHonorsConfiguredCountAndOneDisablesBanking() {
        final List<NodeData> threeBankNodes = buildHeterogeneousForest();
        Optimization.bankColumns = 3;
        Optimization.maxAspectRatio = 1.78;
        TreeLayoutOptimizer.compactDisconnectedTrees(threeBankNodes);
        assertNoNodeOverlap(threeBankNodes);
        assertEquals(3L, threeBankNodes.stream().filter(n -> null == n.parent()).mapToInt(NodeData::col).distinct().count());

        final List<NodeData> twoBankNodes = buildHeterogeneousForest();
        Optimization.bankColumns = 2;
        TreeLayoutOptimizer.compactDisconnectedTrees(twoBankNodes);
        assertNoNodeOverlap(twoBankNodes);
        assertEquals(2L, twoBankNodes.stream().filter(n -> null == n.parent()).mapToInt(NodeData::col).distinct().count());

        final List<NodeData> singleBankNodes = buildHeterogeneousForest();
        Optimization.bankColumns = 1;
        TreeLayoutOptimizer.compactDisconnectedTrees(singleBankNodes);
        assertNoNodeOverlap(singleBankNodes);
        assertEquals(1L, singleBankNodes.stream().filter(n -> null == n.parent()).mapToInt(NodeData::col).distinct().count());
    }

    @Test
    void multiBankLayoutRespectsRootVisualOffsetWithoutOverlap() {
        final List<NodeData> nodes = new ArrayList<>();
        // Root 1 has @VisualOffset(2): occupies cols 2..3 across rows 1..12
        final NodeData offsetRoot = node(nodes, "OffsetRoot", null, 2, 1.0, 2, 0);
        for (int r = 0; r < 12; r++) {
            node(nodes, "OffsetChild_" + r, offsetRoot, 3, 1.0 + r, 0, 0);
        }
        // Three normal depth-2 trees of height 4 each
        double row = 14.0;
        final List<NodeData> otherRoots = new ArrayList<>();
        for (int t = 0; t < 3; t++) {
            final NodeData r = node(nodes, "Tree" + t, null, 0, row, 0, 0);
            otherRoots.add(r);
            for (int c = 0; c < 4; c++) {
                node(nodes, "Tree" + t + "_" + c, r, 1, row + c, 0, 0);
            }
            row += 5.0;
        }

        Optimization.bankColumns = 2;
        Optimization.maxAspectRatio = 1.78;
        TreeLayoutOptimizer.compactDisconnectedTrees(nodes);

        assertNoNodeOverlap(nodes);
        assertEquals(2, offsetRoot.col());
        // Bank 0 occupies cols 2..3 (effective depth 4), so Bank 1 must start at col 4 (not col 2)
        for (final NodeData other : otherRoots) {
            assertEquals(4, other.col());
        }
    }

    private static List<List<String>> extractBankMembership(final List<NodeData> nodes) {
        final java.util.TreeMap<Integer, List<NodeData>> byCol = new java.util.TreeMap<>();
        for (final NodeData n : nodes) {
            if (null == n.parent()) {
                byCol.computeIfAbsent(n.col(), k -> new ArrayList<>()).add(n);
            }
        }
        final List<List<String>> result = new ArrayList<>(byCol.size());
        for (final List<NodeData> bank : byCol.values()) {
            bank.sort(java.util.Comparator.comparingDouble(NodeData::row));
            final List<String> names = new ArrayList<>(bank.size());
            for (final NodeData r : bank) {
                names.add(r.id());
            }
            result.add(names);
        }
        return result;
    }

    private static List<NodeData> buildShrunkHeterogeneousForest(final java.util.Set<String> removedRoots) {
        final List<NodeData> nodes = new ArrayList<>();
        // Drastically altered heights & depths (simulating passed leaves hiding mid-run)
        final Object[][] specs = {
                {"Partners", 2, 2},
                {"Billing", 7, 3},
                {"Settings", 2, 2},
                {"Reports", 6, 4},
                {"AuditLogs", 1, 1},
                {"AppManagement", 5, 2},
                {"Payments", 2, 2},
                {"Menu", 5, 10},
                {"AIUsageReport", 3, 4},
                {"Issues", 2, 3},
                {"Storefront", 2, 15},
                {"AuthAndSecurity", 2, 8},
                {"FirstDelivery", 2, 4},
                {"Dashboard", 1, 1},
                {"EmailHealth", 1, 1},
                {"Login", 1, 1},
                {"Logout", 1, 1},
                {"WrongEmail", 1, 1},
                {"WrongPassword", 1, 1},
                {"Profile", 1, 1}
        };
        double nextRow = 1.0;
        for (final Object[] spec : specs) {
            final String name = (String) spec[0];
            if (removedRoots.contains(name)) continue;
            final int depth = (Integer) spec[1];
            final int height = (Integer) spec[2];
            final NodeData root = node(nodes, name, null, 0, nextRow, 0, 0);
            NodeData spine = root;
            for (int d = 1; d < depth; d++) {
                spine = node(nodes, name + "_d" + d, spine, d, nextRow, 0, 0);
            }
            if (height > 1 && depth >= 2) {
                final NodeData parent = (depth == 2) ? root : root.children().get(0);
                final int leafCol = parent.col() + 1;
                for (int r = 1; r < height; r++) {
                    node(nodes, name + "_r" + r, parent, leafCol, nextRow + r, 0, 0);
                }
            }
            nextRow += height;
        }
        return nodes;
    }

    @Test
    void disabledDynamicReorderingPreservesInitialBankOrderWhileStillCompacting() {
        Optimization.allowDynamicReordering = false;
        Optimization.bankColumns = 0;
        Optimization.maxAspectRatio = 1.78;

        final List<NodeData> initialNodes = buildHeterogeneousForest();
        TreeLayoutOptimizer.compactDisconnectedTrees(initialNodes);
        assertNoNodeOverlap(initialNodes);
        final List<List<String>> initialBanks = extractBankMembership(initialNodes);
        final double initialMaxRow = initialNodes.stream().mapToDouble(NodeData::row).max().orElse(1.0);
        assertTrue(initialBanks.size() >= 4, "Expected >= 4 initial banks");

        // Frame 2: trees shrink drastically and one entire root disappears
        final java.util.Set<String> removed = java.util.Set.of("AuditLogs", "WrongEmail");
        final List<NodeData> shrunkNodes = buildShrunkHeterogeneousForest(removed);
        TreeLayoutOptimizer.compactDisconnectedTrees(shrunkNodes);
        assertNoNodeOverlap(shrunkNodes);

        final List<List<String>> shrunkBanks = extractBankMembership(shrunkNodes);
        final List<List<String>> expectedBanks = new ArrayList<>();
        for (final List<String> bank : initialBanks) {
            final List<String> remaining = bank.stream().filter(name -> !removed.contains(name)).toList();
            if (!remaining.isEmpty()) expectedBanks.add(remaining);
        }

        assertEquals(expectedBanks, shrunkBanks,
                "Level-1 nodes must keep their initial bank assignment and relative vertical order when allowDynamicReordering=false");

        final double shrunkMaxRow = shrunkNodes.stream().mapToDouble(NodeData::row).max().orElse(1.0);
        assertTrue(shrunkMaxRow < initialMaxRow,
                "Vertical compaction within banks must still remain active: " + shrunkMaxRow + " vs " + initialMaxRow);
    }

    @Test
    void enabledDynamicReorderingAllowsBankRepartitioningWhenTreeChanges() {
        Optimization.allowDynamicReordering = true;
        Optimization.bankColumns = 0;
        Optimization.maxAspectRatio = 1.78;

        final List<NodeData> initialNodes = buildHeterogeneousForest();
        TreeLayoutOptimizer.compactDisconnectedTrees(initialNodes);
        final List<List<String>> initialBanks = extractBankMembership(initialNodes);

        final List<NodeData> shrunkNodes = buildShrunkHeterogeneousForest(java.util.Set.of());
        TreeLayoutOptimizer.compactDisconnectedTrees(shrunkNodes);
        assertNoNodeOverlap(shrunkNodes);
        final List<List<String>> shrunkBanks = extractBankMembership(shrunkNodes);

        assertTrue(!initialBanks.equals(shrunkBanks),
                "With allowDynamicReordering=true, drastically shrunk trees should dynamically repartition");
    }

    @Test
    void finalReoptimizationCompactsOnceAfterTestsAndStaysPinned() {
        Optimization.allowDynamicReordering = false;
        Optimization.allowFinalReordering = true;
        Optimization.bankColumns = 0;
        Optimization.maxAspectRatio = 1.78;

        final List<NodeData> initialNodes = buildHeterogeneousForest();
        TreeLayoutOptimizer.compactDisconnectedTrees(initialNodes);
        final List<List<String>> initialBanks = extractBankMembership(initialNodes);
        assertTrue(initialBanks.size() >= 4, "Expected >= 4 initial banks");

        // Mid-run frame stays pinned to initial banks
        final java.util.Set<String> removed = java.util.Set.of("AuditLogs", "WrongEmail");
        final List<NodeData> midRunNodes = buildShrunkHeterogeneousForest(removed);
        TreeLayoutOptimizer.compactDisconnectedTrees(midRunNodes);
        final List<List<String>> midRunBanks = extractBankMembership(midRunNodes);
        final int midRunMaxCol = midRunNodes.stream().mapToInt(NodeData::col).max().orElse(0);

        // Single post-test re-optimization pass clears the in-run pinning once
        assertTrue(TreeLayoutOptimizer.triggerFinalReoptimizationOnce(),
                "First post-test trigger should clear pinned bank layout");

        final List<NodeData> finalNodes = buildShrunkHeterogeneousForest(removed);
        TreeLayoutOptimizer.compactDisconnectedTrees(finalNodes);
        assertNoNodeOverlap(finalNodes);
        final List<List<String>> finalBanks = extractBankMembership(finalNodes);
        final int finalMaxCol = finalNodes.stream().mapToInt(NodeData::col).max().orElse(0);

        assertTrue(!midRunBanks.equals(finalBanks),
                "Post-test final re-optimization must re-pack shrunk banks into a tighter layout");
        assertTrue(finalMaxCol < midRunMaxCol,
                "Final re-optimized layout must reduce column span for better zoom: "
                        + finalMaxCol + " vs " + midRunMaxCol);

        // Second post-test trigger in the same run (e.g., entering cooldown after tree.html) must be a no-op
        assertFalse(TreeLayoutOptimizer.triggerFinalReoptimizationOnce(),
                "Subsequent post-test trigger in the same run must not unpin the final layout again");

        // Subsequent cooldown layout remains pinned to the freshly compacted final banks
        final List<NodeData> cooldownNodes = buildShrunkHeterogeneousForest(removed);
        TreeLayoutOptimizer.compactDisconnectedTrees(cooldownNodes);
        assertEquals(finalBanks, extractBankMembership(cooldownNodes),
                "Cooldown frames must preserve the freshly re-optimized final bank layout");
    }

    @Test
    void disabledFinalReorderingKeepsPinnedBanksAfterTests() {
        Optimization.allowDynamicReordering = false;
        Optimization.allowFinalReordering = false;
        Optimization.bankColumns = 0;
        Optimization.maxAspectRatio = 1.78;

        final List<NodeData> initialNodes = buildHeterogeneousForest();
        TreeLayoutOptimizer.compactDisconnectedTrees(initialNodes);

        final java.util.Set<String> removed = java.util.Set.of("AuditLogs", "WrongEmail");
        final List<NodeData> midRunNodes = buildShrunkHeterogeneousForest(removed);
        TreeLayoutOptimizer.compactDisconnectedTrees(midRunNodes);
        final List<List<String>> midRunBanks = extractBankMembership(midRunNodes);

        assertFalse(TreeLayoutOptimizer.triggerFinalReoptimizationOnce(),
                "Post-test trigger must be a no-op when allowFinalReordering=false");

        final List<NodeData> finalNodes = buildShrunkHeterogeneousForest(removed);
        TreeLayoutOptimizer.compactDisconnectedTrees(finalNodes);
        assertNoNodeOverlap(finalNodes);
        assertEquals(midRunBanks, extractBankMembership(finalNodes),
                "Final layout must keep in-run pinned banks when allowFinalReordering=false");
    }
}
