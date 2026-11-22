package works.lysenko.base.output.tree;

import works.lysenko.base.output.TreeHtml.NodeData;
import works.lysenko.util.prop.tree.Optimization;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Optimizes scenario tree coordinates, balancing multi-column banks, compacting
 * disconnected trees, and solving 1D non-overlapping row constraints.
 */
public final class TreeLayoutOptimizer {

    private static final int COL_WIDTH = 260;
    private static final int ROW_HEIGHT = 56;
    public static final double GOLDEN_RATIO = (Math.sqrt(5.0) - 1.0) / 2.0;

    private TreeLayoutOptimizer() {
    }

    private static double columnWeight(final int col) {
        return Math.pow(2.0, Math.max(0, col));
    }

    private static boolean isShallowWideTree(final List<NodeData> nodes) {
        if (null == nodes || nodes.size() < 10) return false;
        int maxCol = 0;
        for (final NodeData n : nodes) {
            if (n.col() > maxCol) {
                maxCol = n.col();
            }
        }
        final int depth = maxCol + 1;
        return depth <= Optimization.shallowMaxDepth && (nodes.size() / (double) depth) >= Optimization.shallowMinRatio;
    }

    private static void packLeafChildren(final List<NodeData> nodes) {
        if (null == nodes || nodes.isEmpty()) return;

        final boolean shallowWide = isShallowWideTree(nodes);
        final int threshold = shallowWide ? Optimization.shallowThreshold : Optimization.standardThreshold;
        final int numCols = Math.max(1, Optimization.packColumns);

        for (final NodeData parent : nodes) {
            final List<NodeData> allChildren = parent.children();
            if (allChildren.size() >= threshold) {
                final boolean allAreLeaves = allChildren.stream().allMatch(c -> c.children().isEmpty());
                if (allAreLeaves) {
                    final int perCol = (int) Math.ceil(allChildren.size() / (double) numCols);
                    for (int i = 0; i < allChildren.size(); i++) {
                        final NodeData leaf = allChildren.get(i);
                        final int extraCol = i / perCol;
                        final int offset = leaf.col() - (parent.col() + 1);
                        leaf.setCol(parent.col() + 1 + extraCol + offset);
                    }
                }
            }
        }
    }

    public static void optimizeLayout(final List<NodeData> nodes) {
        if (nodes.isEmpty()) return;

        if (Optimization.progression) {
            packLeafChildren(nodes);
        }

        final Map<Integer, List<NodeData>> colMap = new TreeMap<>();
        int maxCol = 0;
        for (final NodeData n : nodes) {
            colMap.computeIfAbsent(n.col(), k -> new ArrayList<>()).add(n);
            if (n.col() > maxCol) maxCol = n.col();
        }

        for (int c = 0; c <= maxCol; c++) {
            final List<NodeData> colNodes = colMap.get(c);
            if (null == colNodes || colNodes.isEmpty()) continue;
            colNodes.sort((n1, n2) -> {
                final double p1 = (null != n1.parent()) ? n1.parent().row() : n1.row();
                final double p2 = (null != n2.parent()) ? n2.parent().row() : n2.row();
                final int pCmp = Double.compare(p1, p2);
                if (pCmp != 0) return pCmp;
                final int lCmp = n1.label().compareTo(n2.label());
                if (lCmp != 0) return lCmp;
                return n1.id().compareTo(n2.id());
            });

            final double[] targets = new double[colNodes.size()];
            for (int i = 0; i < colNodes.size(); i++) {
                final NodeData n = colNodes.get(i);
                double t = (null != n.parent()) ? n.parent().row() : (i + 1.0);
                if (null != n.parent() && n.children().isEmpty()) {
                    final int extraCol = n.col() - (n.parent().col() + 1);
                    if (extraCol > 0) {
                        t += extraCol * GOLDEN_RATIO * 0.382;
                    }
                }
                t += n.rowOffset();
                targets[i] = t;
            }
            final double[] resolved = solveColumn1D(targets);
            for (int i = 0; i < colNodes.size(); i++) {
                colNodes.get(i).setRow(resolved[i]);
            }
        }

        final int iterations = 32;
        for (int iter = 0; iter < iterations; iter++) {
            TreeLayoutComputer.logProgressIfNeeded("optimizing tree layout", iter + 1, iterations);
            final boolean backward = (1 == iter % 2);
            final int startCol = backward ? maxCol : 0;
            final int endCol = backward ? 0 : maxCol;
            final int step = backward ? -1 : 1;

            for (int c = startCol; backward ? c >= endCol : c <= endCol; c += step) {
                final List<NodeData> colNodes = colMap.get(c);
                if (null == colNodes || colNodes.isEmpty()) continue;

                final Map<NodeData, Double> parentRowMap = new HashMap<>();
                final Map<NodeData, Double> childrenRowMap = new HashMap<>();
                final Map<NodeData, Double> targetMap = new HashMap<>();

                for (final NodeData n : colNodes) {
                    double parentSum = 0.0;
                    double parentWeight = 0.0;
                    if (null != n.parent()) {
                        final double wP = columnWeight(c - 1);
                        parentSum += n.parent().row() * wP;
                        parentWeight += wP;
                    }

                    double childSum = 0.0;
                    double childWeight = 0.0;
                    final double wC = columnWeight(c);
                    for (final NodeData child : n.children()) {
                        if (!child.children().isEmpty()) {
                            childSum += child.row() * wC;
                            childWeight += wC;
                        }
                    }

                    if (parentWeight > 0.0) {
                        parentRowMap.put(n, parentSum / parentWeight);
                    }
                    if (childWeight > 0.0) {
                        childrenRowMap.put(n, childSum / childWeight);
                    }

                    double target;
                    if (parentWeight > 0.0 && childWeight > 0.0) {
                        target = (parentSum + childSum) / (parentWeight + childWeight);
                    } else if (parentWeight > 0.0) {
                        target = parentSum / parentWeight;
                    } else if (childWeight > 0.0) {
                        target = childSum / childWeight;
                    } else {
                        target = n.row();
                    }
                    if (null != n.parent() && n.children().isEmpty()) {
                        final int extraCol = n.col() - (n.parent().col() + 1);
                        if (extraCol > 0) {
                            target += extraCol * GOLDEN_RATIO * 0.382;
                        }
                    }
                    target += n.rowOffset();
                    targetMap.put(n, target);
                }

                interpolateSiblingTargets(colNodes, parentRowMap, childrenRowMap, targetMap);

                colNodes.sort((n1, n2) -> {
                    final double p1 = parentRowMap.containsKey(n1) ? parentRowMap.get(n1) : n1.row();
                    final double p2 = parentRowMap.containsKey(n2) ? parentRowMap.get(n2) : n2.row();
                    final int pCmp = Double.compare(p1, p2);
                    if (pCmp != 0) return pCmp;

                    final double c1 = childrenRowMap.containsKey(n1) ? childrenRowMap.get(n1) : n1.row();
                    final double c2 = childrenRowMap.containsKey(n2) ? childrenRowMap.get(n2) : n2.row();
                    final int cCmp = Double.compare(c1, c2);
                    if (cCmp != 0) return cCmp;

                    final int tCmp = Double.compare(targetMap.get(n1), targetMap.get(n2));
                    if (tCmp != 0) return tCmp;

                    final int lCmp = n1.label().compareTo(n2.label());
                    if (lCmp != 0) return lCmp;

                    return n1.id().compareTo(n2.id());
                });

                final double[] targets = new double[colNodes.size()];
                for (int i = 0; i < colNodes.size(); i++) {
                    targets[i] = targetMap.get(colNodes.get(i));
                }

                final double[] resolved = solveColumn1D(targets);
                for (int i = 0; i < colNodes.size(); i++) {
                    colNodes.get(i).setRow(resolved[i]);
                }
            }
        }

        double minRow = Double.MAX_VALUE;
        for (final NodeData n : nodes) {
            if (n.row() < minRow) minRow = n.row();
        }
        if (minRow < Double.MAX_VALUE && minRow > 1.0) {
            final double shift = minRow - 1.0;
            for (final NodeData n : nodes) {
                n.setRow(n.row() - shift);
            }
        }
        compactDisconnectedTrees(nodes);
    }

    private static void interpolateSiblingTargets(
            final List<NodeData> colNodes,
            final Map<NodeData, Double> parentRowMap,
            final Map<NodeData, Double> childrenRowMap,
            final Map<NodeData, Double> targetMap) {

        final Map<NodeData, List<NodeData>> parentGroups = new LinkedHashMap<>();
        for (final NodeData n : colNodes) {
            parentGroups.computeIfAbsent(n.parent(), k -> new ArrayList<>()).add(n);
        }

        for (final List<NodeData> siblings : parentGroups.values()) {
            if (siblings.size() <= 1) continue;

            final List<Integer> fixedIndices = new ArrayList<>();
            for (int i = 0; i < siblings.size(); i++) {
                if (childrenRowMap.containsKey(siblings.get(i))) {
                    fixedIndices.add(i);
                }
            }

            if (fixedIndices.isEmpty()) continue;

            for (int k = 0; k < siblings.size(); k++) {
                final NodeData s = siblings.get(k);
                if (childrenRowMap.containsKey(s)) continue;

                int leftIdx = -1;
                for (final int f : fixedIndices) {
                    if (f < k) leftIdx = f;
                    else break;
                }

                int rightIdx = -1;
                for (final int f : fixedIndices) {
                    if (f > k) { rightIdx = f; break; }
                }

                final double parentRow = parentRowMap.getOrDefault(s, s.row());

                if (leftIdx != -1 && rightIdx != -1) {
                    final double leftTarget = targetMap.get(siblings.get(leftIdx));
                    final double rightTarget = targetMap.get(siblings.get(rightIdx));
                    final double ratio = (double) (k - leftIdx) / (rightIdx - leftIdx);
                    targetMap.put(s, leftTarget + ratio * (rightTarget - leftTarget));
                } else if (leftIdx != -1) {
                    final double leftTarget = targetMap.get(siblings.get(leftIdx));
                    targetMap.put(s, leftTarget + (k - leftIdx) * 1.0);
                } else if (rightIdx != -1) {
                    final double rightTarget = targetMap.get(siblings.get(rightIdx));
                    targetMap.put(s, Math.max(parentRow, rightTarget - (rightIdx - k) * 1.0));
                }
            }
        }
    }

    private record ComponentInfo(
            int index,
            String rootKey,
            List<NodeData> nodes,
            double height,
            int minCol,
            int rootOffset,
            int effectiveDepth) {
    }

    private static final class PinnedBankState {
        final int bankColumns;
        final double maxAspectRatio;
        final List<List<String>> banks;
        final Set<String> knownRoots;

        PinnedBankState(final int bankColumns, final double maxAspectRatio, final List<List<String>> banks) {
            this.bankColumns = bankColumns;
            this.maxAspectRatio = maxAspectRatio;
            this.banks = banks;
            this.knownRoots = new HashSet<>();
            for (final List<String> bank : banks) {
                this.knownRoots.addAll(bank);
            }
        }

        boolean matches(final int bankCols, final double targetAspect, final List<ComponentInfo> infos) {
            if (this.bankColumns != bankCols || Double.compare(this.maxAspectRatio, targetAspect) != 0) {
                return false;
            }
            for (final ComponentInfo info : infos) {
                if (knownRoots.contains(info.rootKey)) {
                    return true;
                }
            }
            return false;
        }
    }

    private static final Object PINNED_BANK_LOCK = new Object();
    private static PinnedBankState pinnedBankState = null;
    private static boolean finalReoptimizationConsumed = false;

    /**
     * Clears any pinned level-1 multi-bank layout state and resets the one-shot final re-optimization
     * gate so the next layout computes fresh bank assignments.
     */
    public static void resetPinnedBankLayout() {
        synchronized (PINNED_BANK_LOCK) {
            pinnedBankState = null;
            finalReoptimizationConsumed = false;
        }
    }

    /**
     * Triggers the single post-test re-optimization pass by unpinning the in-run level-1 column bank
     * layout once when {@link Optimization#allowFinalReordering} is enabled and
     * {@link Optimization#allowDynamicReordering} is disabled.
     * <p>
     * Subsequent invocations in the same run are no-ops so the freshly compacted final bank partition
     * stays stable across both the final HTML report and all cooldown progression frames.
     *
     * @return {@code true} if an active pinned bank layout was cleared for the final re-optimization pass
     */
    public static boolean triggerFinalReoptimizationOnce() {
        if (!Optimization.allowFinalReordering || Optimization.allowDynamicReordering) {
            return false;
        }
        synchronized (PINNED_BANK_LOCK) {
            if (finalReoptimizationConsumed) {
                return false;
            }
            finalReoptimizationConsumed = true;
            if (null == pinnedBankState) {
                return false;
            }
            pinnedBankState = null;
            return true;
        }
    }

    public static void compactDisconnectedTrees(final List<NodeData> nodes) {
        final Map<NodeData, List<NodeData>> components = new IdentityHashMap<>();
        for (final NodeData node : nodes)
            components.computeIfAbsent(rootOf(node), ignored -> new ArrayList<>()).add(node);

        final List<List<NodeData>> ordered = new ArrayList<>(components.values());
        ordered.sort((c1, c2) -> {
            final int mCmp = Double.compare(minimumRow(c1), minimumRow(c2));
            if (mCmp != 0) return mCmp;
            final String l1 = c1.isEmpty() ? "" : c1.get(0).label();
            final String l2 = c2.isEmpty() ? "" : c2.get(0).label();
            final int lCmp = l1.compareTo(l2);
            if (lCmp != 0) return lCmp;
            final String id1 = c1.isEmpty() ? "" : c1.get(0).id();
            final String id2 = c2.isEmpty() ? "" : c2.get(0).id();
            return id1.compareTo(id2);
        });

        if (ordered.isEmpty()) return;

        if (Optimization.progression && ordered.size() >= 2 && Optimization.bankColumns != 1 && Optimization.maxAspectRatio > 0.0) {
            final List<ComponentInfo> infos = buildComponentInfos(ordered);
            if (!Optimization.allowDynamicReordering) {
                layoutPinnedOrInitialBanks(infos, Optimization.maxAspectRatio);
                return;
            }
            if (needsMultiBankLayout(infos, Optimization.maxAspectRatio)) {
                final List<List<ComponentInfo>> chosenBanks = computeOptimalBankPartition(infos, Optimization.maxAspectRatio);
                applyBankPlacement(infos, chosenBanks);
                return;
            }
        }

        stackComponentsVertically(ordered);
    }

    private static boolean needsMultiBankLayout(final List<ComponentInfo> infos, final double targetAspect) {
        int maxDepth = 0;
        double totalStackedRows = 0.0;
        for (final ComponentInfo info : infos) {
            if (info.effectiveDepth > maxDepth) maxDepth = info.effectiveDepth;
            totalStackedRows += info.height;
        }
        final double naturalWidth = maxDepth * (double) COL_WIDTH;
        final double naturalHeight = totalStackedRows * (double) ROW_HEIGHT;
        final double naturalAspectRatio = (naturalHeight > 0.0) ? (naturalWidth / naturalHeight) : targetAspect;
        return naturalAspectRatio < targetAspect;
    }

    private static String baseRootKey(final NodeData root) {
        if (null == root) return "";
        if (null != root.scenario()) {
            final String name = root.scenario().getName();
            if (null != name && !name.isBlank()) return name;
            return root.scenario().getClass().getName();
        }
        if (null != root.label() && !root.label().isBlank()) return root.label();
        return (null != root.id()) ? root.id() : "";
    }

    private static List<ComponentInfo> buildComponentInfos(final List<List<NodeData>> ordered) {
        final List<ComponentInfo> infos = new ArrayList<>(ordered.size());
        final Map<String, Integer> keyCounts = new HashMap<>(ordered.size() * 2);
        for (int i = 0; i < ordered.size(); i++) {
            final List<NodeData> comp = ordered.get(i);
            final NodeData root = comp.isEmpty() ? null : rootOf(comp.get(0));
            final String baseKey = baseRootKey(root);
            final int count = keyCounts.getOrDefault(baseKey, 0);
            keyCounts.put(baseKey, count + 1);
            final String rootKey = (0 == count) ? baseKey : (baseKey + "#" + count);
            final int minC = minimumCol(comp);
            final int maxC = maximumCol(comp);
            final int rootOffset = (null == root) ? 0 : Math.max(0, root.col());
            final int effectiveDepth = Math.max(1, rootOffset + (maxC - minC) + 1);
            final double height = maximumRow(comp) - minimumRow(comp) + 1.0;
            infos.add(new ComponentInfo(i, rootKey, comp, height, minC, rootOffset, effectiveDepth));
        }
        return infos;
    }

    private static void stackComponentsVertically(final List<List<NodeData>> ordered) {
        double nextRow = 1.0;
        for (final List<NodeData> component : ordered) {
            final double minimum = minimumRow(component);
            final double shift = nextRow - minimum;
            if (0.0 != shift)
                for (final NodeData node : component)
                    node.setRow(node.row() + shift);
            nextRow = maximumRow(component) + 1.0;
        }
    }

    private static double bankLayoutScore(final int totalCols, final double maxRows, final double targetAspect) {
        final double pixelW = Math.max(1, totalCols) * (double) COL_WIDTH;
        final double pixelH = Math.max(1.0, maxRows) * (double) ROW_HEIGHT;
        final double enclosingH = Math.max(pixelW / targetAspect, pixelH);
        final double sqrtEnclosingArea = enclosingH * Math.sqrt(targetAspect);
        final double sqrtActualArea = Math.sqrt(pixelW * pixelH);
        return 0.78 * sqrtEnclosingArea + 0.22 * sqrtActualArea;
    }

    private static double evaluatePartition(final List<List<ComponentInfo>> banks, final double targetAspect) {
        int totalCols = 0;
        double maxRows = 0.0;
        for (final List<ComponentInfo> bank : banks) {
            if (bank.isEmpty()) continue;
            int bankDepth = 0;
            double bankHeight = 0.0;
            for (final ComponentInfo info : bank) {
                if (info.effectiveDepth > bankDepth) bankDepth = info.effectiveDepth;
                bankHeight += info.height;
            }
            totalCols += bankDepth;
            if (bankHeight > maxRows) maxRows = bankHeight;
        }
        return bankLayoutScore(totalCols, maxRows, targetAspect);
    }

    private static void layoutPinnedOrInitialBanks(final List<ComponentInfo> infos, final double targetAspect) {
        final List<List<ComponentInfo>> chosenBanks;
        synchronized (PINNED_BANK_LOCK) {
            if (null != pinnedBankState && pinnedBankState.matches(Optimization.bankColumns, targetAspect, infos)) {
                chosenBanks = resolvePinnedBanks(pinnedBankState, infos);
            } else {
                if (needsMultiBankLayout(infos, targetAspect)) {
                    chosenBanks = computeOptimalBankPartition(infos, targetAspect);
                } else {
                    chosenBanks = new ArrayList<>(1);
                    chosenBanks.add(new ArrayList<>(infos));
                }
                final List<List<String>> pinnedKeys = new ArrayList<>(chosenBanks.size());
                for (final List<ComponentInfo> bank : chosenBanks) {
                    if (bank.isEmpty()) continue;
                    final List<String> keys = new ArrayList<>(bank.size());
                    for (final ComponentInfo info : bank) {
                        keys.add(info.rootKey);
                    }
                    pinnedKeys.add(keys);
                }
                pinnedBankState = new PinnedBankState(Optimization.bankColumns, targetAspect, pinnedKeys);
            }
        }
        applyBankPlacement(infos, chosenBanks);
    }

    private static List<List<ComponentInfo>> resolvePinnedBanks(
            final PinnedBankState state,
            final List<ComponentInfo> infos) {
        final Map<String, ComponentInfo> byKey = new LinkedHashMap<>(infos.size() * 2);
        for (final ComponentInfo info : infos) {
            byKey.put(info.rootKey, info);
        }
        final List<List<ComponentInfo>> banks = new ArrayList<>(state.banks.size());
        final double[] bankHeights = new double[state.banks.size()];
        final int[] bankDepths = new int[state.banks.size()];
        for (int b = 0; b < state.banks.size(); b++) {
            final List<String> pinnedKeys = state.banks.get(b);
            final List<ComponentInfo> bank = new ArrayList<>(pinnedKeys.size());
            for (final String key : pinnedKeys) {
                final ComponentInfo match = byKey.remove(key);
                if (null != match) {
                    bank.add(match);
                    bankHeights[b] += match.height;
                    if (match.effectiveDepth > bankDepths[b]) {
                        bankDepths[b] = match.effectiveDepth;
                    }
                }
            }
            banks.add(bank);
        }
        if (!byKey.isEmpty() && !banks.isEmpty()) {
            for (final ComponentInfo extra : byKey.values()) {
                int bestBank = 0;
                double minHeight = bankHeights[0];
                int bestDepthInc = Math.max(0, extra.effectiveDepth - bankDepths[0]);
                for (int b = 1; b < banks.size(); b++) {
                    final double h = bankHeights[b];
                    final int depthInc = Math.max(0, extra.effectiveDepth - bankDepths[b]);
                    if (h < minHeight - EPS || (Math.abs(h - minHeight) <= EPS && depthInc < bestDepthInc)) {
                        minHeight = h;
                        bestDepthInc = depthInc;
                        bestBank = b;
                    }
                }
                banks.get(bestBank).add(extra);
                bankHeights[bestBank] += extra.height;
                if (extra.effectiveDepth > bankDepths[bestBank]) {
                    bankDepths[bestBank] = extra.effectiveDepth;
                }
                state.banks.get(bestBank).add(extra.rootKey);
                state.knownRoots.add(extra.rootKey);
            }
        }
        return banks;
    }

    private static List<List<ComponentInfo>> computeOptimalBankPartition(
            final List<ComponentInfo> infos,
            final double targetAspect) {
        final List<ComponentInfo> byHeightDesc = new ArrayList<>(infos);
        byHeightDesc.sort((a, b) -> {
            final int hCmp = Double.compare(b.height, a.height);
            if (hCmp != 0) return hCmp;
            final int dCmp = Integer.compare(b.effectiveDepth, a.effectiveDepth);
            if (dCmp != 0) return dCmp;
            return Integer.compare(a.index, b.index);
        });

        final List<ComponentInfo> byDepthDesc = new ArrayList<>(infos);
        byDepthDesc.sort((a, b) -> {
            final int dCmp = Integer.compare(b.effectiveDepth, a.effectiveDepth);
            if (dCmp != 0) return dCmp;
            final int hCmp = Double.compare(b.height, a.height);
            if (hCmp != 0) return hCmp;
            return Integer.compare(a.index, b.index);
        });

        final List<List<ComponentInfo>> chosenBanks;
        if (Optimization.bankColumns >= 2) {
            final int fixedK = Math.min(Optimization.bankColumns, infos.size());
            chosenBanks = selectFixedBankPartition(byHeightDesc, byDepthDesc, fixedK, targetAspect);
        } else {
            chosenBanks = selectAutoBankPartition(infos, byHeightDesc, byDepthDesc, targetAspect);
        }

        chosenBanks.removeIf(List::isEmpty);
        chosenBanks.sort(Comparator.comparingInt(bank -> bank.stream().mapToInt(ComponentInfo::index).min().orElse(0)));
        for (final List<ComponentInfo> bank : chosenBanks) {
            bank.sort(Comparator.comparingInt(ComponentInfo::index));
        }
        return chosenBanks;
    }

    private static void applyBankPlacement(final List<ComponentInfo> infos, final List<List<ComponentInfo>> chosenBanks) {
        final List<List<ComponentInfo>> nonEmpty = new ArrayList<>(chosenBanks.size());
        for (final List<ComponentInfo> bank : chosenBanks) {
            if (!bank.isEmpty()) {
                nonEmpty.add(bank);
            }
        }
        if (nonEmpty.isEmpty()) {
            final List<List<NodeData>> single = new ArrayList<>(infos.size());
            for (final ComponentInfo info : infos) single.add(info.nodes);
            stackComponentsVertically(single);
            return;
        }
        if (nonEmpty.size() == 1) {
            final List<ComponentInfo> onlyBank = nonEmpty.get(0);
            final List<List<NodeData>> single = new ArrayList<>(onlyBank.size());
            for (final ComponentInfo info : onlyBank) single.add(info.nodes);
            stackComponentsVertically(single);
            return;
        }

        int currentColOffset = 0;
        for (final List<ComponentInfo> bank : nonEmpty) {
            double nextRow = 1.0;
            int bankMaxDepth = 0;
            for (final ComponentInfo info : bank) {
                final List<NodeData> component = info.nodes;
                final double minR = minimumRow(component);
                final double shiftR = nextRow - minR;
                final int shiftC = (currentColOffset + info.rootOffset) - info.minCol;

                if (info.effectiveDepth > bankMaxDepth) bankMaxDepth = info.effectiveDepth;

                for (final NodeData node : component) {
                    if (0.0 != shiftR) node.setRow(node.row() + shiftR);
                    if (0 != shiftC) node.setCol(node.col() + shiftC);
                }
                nextRow = maximumRow(component) + 1.0;
            }
            currentColOffset += bankMaxDepth;
        }
    }

    private static List<List<ComponentInfo>> selectFixedBankPartition(
            final List<ComponentInfo> byHeightDesc,
            final List<ComponentInfo> byDepthDesc,
            final int fixedK,
            final double targetAspect) {
        final List<List<List<ComponentInfo>>> candidates = new ArrayList<>(3);
        candidates.add(packFixedKHeightBalanced(byHeightDesc, fixedK));
        candidates.add(packFixedKScoreGuided(byHeightDesc, fixedK, targetAspect));
        candidates.add(packFixedKScoreGuided(byDepthDesc, fixedK, targetAspect));

        List<List<ComponentInfo>> best = null;
        double bestScore = Double.POSITIVE_INFINITY;
        for (final List<List<ComponentInfo>> candidate : candidates) {
            final List<List<ComponentInfo>> refined = localImprovePartition(candidate, targetAspect, false);
            final double score = evaluatePartition(refined, targetAspect);
            if (null == best || score < bestScore - EPS) {
                best = refined;
                bestScore = score;
            }
        }
        return best;
    }

    private static List<List<ComponentInfo>> selectAutoBankPartition(
            final List<ComponentInfo> infos,
            final List<ComponentInfo> byHeightDesc,
            final List<ComponentInfo> byDepthDesc,
            final double targetAspect) {
        final List<List<ComponentInfo>> singleBank = new ArrayList<>(1);
        singleBank.add(new ArrayList<>(infos));
        List<List<ComponentInfo>> bestOverall = singleBank;
        double bestOverallScore = evaluatePartition(singleBank, targetAspect);

        final List<Double> heightCaps = buildCandidateHeightCaps(infos);

        List<List<ComponentInfo>> bestDepthCap = null;
        double bestDepthCapScore = Double.POSITIVE_INFINITY;
        List<List<ComponentInfo>> bestHeightCap = null;
        double bestHeightCapScore = Double.POSITIVE_INFINITY;

        for (final double capH : heightCaps) {
            final List<List<ComponentInfo>> depthCandidate = packBestFitWithHeightCap(byDepthDesc, capH);
            final double depthScore = evaluatePartition(depthCandidate, targetAspect);
            if (depthScore < bestDepthCapScore - EPS) {
                bestDepthCapScore = depthScore;
                bestDepthCap = depthCandidate;
            }

            final List<List<ComponentInfo>> heightCandidate = packBestFitWithHeightCap(byHeightDesc, capH);
            final double heightScore = evaluatePartition(heightCandidate, targetAspect);
            if (heightScore < bestHeightCapScore - EPS) {
                bestHeightCapScore = heightScore;
                bestHeightCap = heightCandidate;
            }
        }

        List<List<ComponentInfo>> bestScoreGuided = null;
        double bestScoreGuidedScore = Double.POSITIVE_INFINITY;
        final int maxSweepK = Math.min(infos.size(), 16);
        for (int k = 2; k <= maxSweepK; k++) {
            final List<List<ComponentInfo>> hCand = packAutoScoreGuided(byHeightDesc, k, targetAspect);
            final double hScore = evaluatePartition(hCand, targetAspect);
            if (hScore < bestScoreGuidedScore - EPS) {
                bestScoreGuidedScore = hScore;
                bestScoreGuided = hCand;
            }

            final List<List<ComponentInfo>> dCand = packAutoScoreGuided(byDepthDesc, k, targetAspect);
            final double dScore = evaluatePartition(dCand, targetAspect);
            if (dScore < bestScoreGuidedScore - EPS) {
                bestScoreGuidedScore = dScore;
                bestScoreGuided = dCand;
            }
        }

        final List<List<List<ComponentInfo>>> finalists = new ArrayList<>(3);
        if (null != bestDepthCap) finalists.add(bestDepthCap);
        if (null != bestHeightCap) finalists.add(bestHeightCap);
        if (null != bestScoreGuided) finalists.add(bestScoreGuided);

        for (final List<List<ComponentInfo>> finalist : finalists) {
            final List<List<ComponentInfo>> refined = localImprovePartition(finalist, targetAspect, true);
            final double score = evaluatePartition(refined, targetAspect);
            if (score < bestOverallScore - EPS) {
                bestOverallScore = score;
                bestOverall = refined;
            }
        }
        return bestOverall;
    }

    private static List<Double> buildCandidateHeightCaps(final List<ComponentInfo> infos) {
        double minH = 1.0;
        double totalH = 0.0;
        for (final ComponentInfo info : infos) {
            if (info.height > minH) minH = info.height;
            totalH += info.height;
        }
        final java.util.TreeSet<Double> caps = new java.util.TreeSet<>();
        caps.add(minH);
        if (totalH > minH + EPS) {
            final int maxDiv = Math.min(infos.size(), 24);
            for (int k = 2; k <= maxDiv; k++) {
                final double avg = totalH / k;
                caps.add(Math.max(minH, Math.ceil(avg)));
                caps.add(Math.max(minH, avg * 1.05));
            }
            for (int step = 1; step <= 32; step++) {
                caps.add(minH + (totalH - minH) * step / 33.0);
            }
        }
        return new ArrayList<>(caps);
    }

    private static List<List<ComponentInfo>> packBestFitWithHeightCap(
            final List<ComponentInfo> orderedComps,
            final double capH) {
        final List<List<ComponentInfo>> banks = new ArrayList<>();
        final List<Double> bankHeights = new ArrayList<>();
        final List<Integer> bankDepths = new ArrayList<>();

        for (final ComponentInfo comp : orderedComps) {
            int bestBank = -1;
            int bestDepthIncrease = Integer.MAX_VALUE;
            double bestResultHeight = -1.0;
            int bestBankDepth = Integer.MAX_VALUE;

            for (int b = 0; b < banks.size(); b++) {
                final double nextH = bankHeights.get(b) + comp.height;
                if (nextH > capH + EPS) continue;
                final int curD = bankDepths.get(b);
                final int depthInc = Math.max(0, comp.effectiveDepth - curD);
                final boolean better;
                if (bestBank == -1) {
                    better = true;
                } else if (depthInc != bestDepthIncrease) {
                    better = depthInc < bestDepthIncrease;
                } else if (Math.abs(nextH - bestResultHeight) > EPS) {
                    better = nextH > bestResultHeight;
                } else {
                    better = curD < bestBankDepth;
                }
                if (better) {
                    bestBank = b;
                    bestDepthIncrease = depthInc;
                    bestResultHeight = nextH;
                    bestBankDepth = curD;
                }
            }

            if (bestBank == -1) {
                final List<ComponentInfo> newBank = new ArrayList<>();
                newBank.add(comp);
                banks.add(newBank);
                bankHeights.add(comp.height);
                bankDepths.add(comp.effectiveDepth);
            } else {
                banks.get(bestBank).add(comp);
                bankHeights.set(bestBank, bankHeights.get(bestBank) + comp.height);
                if (comp.effectiveDepth > bankDepths.get(bestBank)) {
                    bankDepths.set(bestBank, comp.effectiveDepth);
                }
            }
        }
        return banks;
    }

    private static List<List<ComponentInfo>> packFixedKHeightBalanced(
            final List<ComponentInfo> orderedComps,
            final int numBanks) {
        final List<List<ComponentInfo>> banks = new ArrayList<>(numBanks);
        for (int i = 0; i < numBanks; i++) banks.add(new ArrayList<>());
        final double[] bankHeights = new double[numBanks];
        final int[] bankDepths = new int[numBanks];

        for (final ComponentInfo comp : orderedComps) {
            int bestBank = 0;
            double minHeight = bankHeights[0];
            int bestDepthInc = Math.max(0, comp.effectiveDepth - bankDepths[0]);
            for (int b = 1; b < numBanks; b++) {
                final double h = bankHeights[b];
                final int depthInc = Math.max(0, comp.effectiveDepth - bankDepths[b]);
                if (h < minHeight - EPS || (Math.abs(h - minHeight) <= EPS && depthInc < bestDepthInc)) {
                    minHeight = h;
                    bestDepthInc = depthInc;
                    bestBank = b;
                }
            }
            banks.get(bestBank).add(comp);
            bankHeights[bestBank] += comp.height;
            if (comp.effectiveDepth > bankDepths[bestBank]) bankDepths[bestBank] = comp.effectiveDepth;
        }
        return banks;
    }

    private static List<List<ComponentInfo>> packFixedKScoreGuided(
            final List<ComponentInfo> orderedComps,
            final int numBanks,
            final double targetAspect) {
        final List<List<ComponentInfo>> banks = new ArrayList<>(numBanks);
        for (int i = 0; i < numBanks; i++) banks.add(new ArrayList<>());
        final double[] bankHeights = new double[numBanks];
        final int[] bankDepths = new int[numBanks];
        int totalCols = 0;
        double maxRows = 0.0;

        for (int idx = 0; idx < orderedComps.size(); idx++) {
            final ComponentInfo comp = orderedComps.get(idx);
            final int chosen;
            if (idx < numBanks) {
                chosen = idx;
            } else {
                int bestB = 0;
                double bestScore = Double.POSITIVE_INFINITY;
                double bestBankH = Double.POSITIVE_INFINITY;
                int bestDepthInc = Integer.MAX_VALUE;
                for (int b = 0; b < numBanks; b++) {
                    final int depthInc = Math.max(0, comp.effectiveDepth - bankDepths[b]);
                    final int nextCols = totalCols + depthInc;
                    final double nextBankH = bankHeights[b] + comp.height;
                    final double nextMaxRows = Math.max(maxRows, nextBankH);
                    final double score = bankLayoutScore(nextCols, nextMaxRows, targetAspect);
                    if (score < bestScore - EPS
                            || (Math.abs(score - bestScore) <= EPS && nextBankH < bestBankH - EPS)
                            || (Math.abs(score - bestScore) <= EPS && Math.abs(nextBankH - bestBankH) <= EPS && depthInc < bestDepthInc)) {
                        bestScore = score;
                        bestBankH = nextBankH;
                        bestDepthInc = depthInc;
                        bestB = b;
                    }
                }
                chosen = bestB;
            }
            banks.get(chosen).add(comp);
            final int prevD = bankDepths[chosen];
            if (comp.effectiveDepth > prevD) {
                totalCols += (comp.effectiveDepth - prevD);
                bankDepths[chosen] = comp.effectiveDepth;
            }
            bankHeights[chosen] += comp.height;
            if (bankHeights[chosen] > maxRows) maxRows = bankHeights[chosen];
        }
        return banks;
    }

    private static List<List<ComponentInfo>> packAutoScoreGuided(
            final List<ComponentInfo> orderedComps,
            final int maxBanks,
            final double targetAspect) {
        final List<List<ComponentInfo>> banks = new ArrayList<>();
        final List<Double> bankHeights = new ArrayList<>();
        final List<Integer> bankDepths = new ArrayList<>();
        int totalCols = 0;
        double maxRows = 0.0;

        for (final ComponentInfo comp : orderedComps) {
            int bestB = -1;
            double bestScore = Double.POSITIVE_INFINITY;
            double bestBankH = Double.POSITIVE_INFINITY;
            int bestDepthInc = Integer.MAX_VALUE;

            for (int b = 0; b < banks.size(); b++) {
                final int depthInc = Math.max(0, comp.effectiveDepth - bankDepths.get(b));
                final int nextCols = totalCols + depthInc;
                final double nextBankH = bankHeights.get(b) + comp.height;
                final double nextMaxRows = Math.max(maxRows, nextBankH);
                final double score = bankLayoutScore(nextCols, nextMaxRows, targetAspect);
                if (score < bestScore - EPS
                        || (Math.abs(score - bestScore) <= EPS && nextBankH < bestBankH - EPS)
                        || (Math.abs(score - bestScore) <= EPS && Math.abs(nextBankH - bestBankH) <= EPS && depthInc < bestDepthInc)) {
                    bestScore = score;
                    bestBankH = nextBankH;
                    bestDepthInc = depthInc;
                    bestB = b;
                }
            }

            if (banks.size() < maxBanks) {
                final int nextCols = totalCols + comp.effectiveDepth;
                final double nextMaxRows = Math.max(maxRows, comp.height);
                final double score = bankLayoutScore(nextCols, nextMaxRows, targetAspect);
                if (bestB == -1 || score < bestScore - EPS) {
                    bestB = banks.size();
                }
            }

            if (bestB == banks.size()) {
                final List<ComponentInfo> newBank = new ArrayList<>();
                newBank.add(comp);
                banks.add(newBank);
                bankHeights.add(comp.height);
                bankDepths.add(comp.effectiveDepth);
                totalCols += comp.effectiveDepth;
                if (comp.height > maxRows) maxRows = comp.height;
            } else {
                banks.get(bestB).add(comp);
                final int prevD = bankDepths.get(bestB);
                if (comp.effectiveDepth > prevD) {
                    totalCols += (comp.effectiveDepth - prevD);
                    bankDepths.set(bestB, comp.effectiveDepth);
                }
                final double newH = bankHeights.get(bestB) + comp.height;
                bankHeights.set(bestB, newH);
                if (newH > maxRows) maxRows = newH;
            }
        }
        return banks;
    }

    private static List<List<ComponentInfo>> localImprovePartition(
            final List<List<ComponentInfo>> initial,
            final double targetAspect,
            final boolean allowEmptyingBank) {
        final List<List<ComponentInfo>> current = new ArrayList<>(initial.size());
        int totalComps = 0;
        for (final List<ComponentInfo> bank : initial) {
            if (!bank.isEmpty()) {
                current.add(new ArrayList<>(bank));
                totalComps += bank.size();
            }
        }
        if (current.isEmpty()) return current;

        final int maxSteps = 64;
        for (int step = 0; step < maxSteps; step++) {
            final int numBanks = current.size();
            final double[] bankH = new double[numBanks];
            final int[] bankD = new int[numBanks];
            int totalCols = 0;
            double maxRows = 0.0;
            for (int b = 0; b < numBanks; b++) {
                for (final ComponentInfo info : current.get(b)) {
                    bankH[b] += info.height;
                    if (info.effectiveDepth > bankD[b]) bankD[b] = info.effectiveDepth;
                }
                totalCols += bankD[b];
                if (bankH[b] > maxRows) maxRows = bankH[b];
            }
            final double curScore = bankLayoutScore(totalCols, maxRows, targetAspect);

            // 1. Try single-component move
            int bestFromBank = -1;
            int bestCompIdx = -1;
            int bestToBank = -2; // -1 means new bank
            double bestMoveScore = curScore;

            for (int a = 0; a < numBanks; a++) {
                final List<ComponentInfo> bankA = current.get(a);
                if (!allowEmptyingBank && bankA.size() <= 1) continue;
                for (int ci = 0; ci < bankA.size(); ci++) {
                    final ComponentInfo c = bankA.get(ci);
                    final double newAH = bankH[a] - c.height;
                    final int newAD = maxDepthWithout(bankA, ci);

                    for (int b = 0; b < numBanks; b++) {
                        if (b == a) continue;
                        final double newBH = bankH[b] + c.height;
                        final int newBD = Math.max(bankD[b], c.effectiveDepth);
                        final int newCols = totalCols - bankD[a] + newAD - bankD[b] + newBD;
                        final double newMaxRows = maxRowsAfterChange(bankH, a, newAH, b, newBH, 0.0);
                        final double score = bankLayoutScore(newCols, newMaxRows, targetAspect);
                        if (score < bestMoveScore - EPS) {
                            bestMoveScore = score;
                            bestFromBank = a;
                            bestCompIdx = ci;
                            bestToBank = b;
                        }
                    }

                    if (allowEmptyingBank && bankA.size() > 1) {
                        final int newCols = totalCols - bankD[a] + newAD + c.effectiveDepth;
                        final double newMaxRows = maxRowsAfterChange(bankH, a, newAH, -1, 0.0, c.height);
                        final double score = bankLayoutScore(newCols, newMaxRows, targetAspect);
                        if (score < bestMoveScore - EPS) {
                            bestMoveScore = score;
                            bestFromBank = a;
                            bestCompIdx = ci;
                            bestToBank = -1;
                        }
                    }
                }
            }

            if (bestFromBank != -1) {
                final ComponentInfo moved = current.get(bestFromBank).remove(bestCompIdx);
                if (bestToBank == -1) {
                    final List<ComponentInfo> newBank = new ArrayList<>();
                    newBank.add(moved);
                    current.add(newBank);
                } else {
                    current.get(bestToBank).add(moved);
                }
                if (current.get(bestFromBank).isEmpty()) {
                    current.remove(bestFromBank);
                }
                continue;
            }

            // 2. Try pairwise swap between two banks
            if (totalComps > 60) break;
            int swapA = -1, swapIdxA = -1, swapB = -1, swapIdxB = -1;
            double bestSwapScore = curScore;

            for (int a = 0; a < numBanks; a++) {
                final List<ComponentInfo> bankA = current.get(a);
                for (int b = a + 1; b < numBanks; b++) {
                    final List<ComponentInfo> bankB = current.get(b);
                    for (int ia = 0; ia < bankA.size(); ia++) {
                        final ComponentInfo c1 = bankA.get(ia);
                        final int baseAD = maxDepthWithout(bankA, ia);
                        for (int ib = 0; ib < bankB.size(); ib++) {
                            final ComponentInfo c2 = bankB.get(ib);
                            final int baseBD = maxDepthWithout(bankB, ib);
                            final double newAH = bankH[a] - c1.height + c2.height;
                            final double newBH = bankH[b] - c2.height + c1.height;
                            final int newAD = Math.max(baseAD, c2.effectiveDepth);
                            final int newBD = Math.max(baseBD, c1.effectiveDepth);
                            final int newCols = totalCols - bankD[a] - bankD[b] + newAD + newBD;
                            final double newMaxRows = maxRowsAfterChange(bankH, a, newAH, b, newBH, 0.0);
                            final double score = bankLayoutScore(newCols, newMaxRows, targetAspect);
                            if (score < bestSwapScore - EPS) {
                                bestSwapScore = score;
                                swapA = a;
                                swapIdxA = ia;
                                swapB = b;
                                swapIdxB = ib;
                            }
                        }
                    }
                }
            }

            if (swapA != -1) {
                final ComponentInfo c1 = current.get(swapA).get(swapIdxA);
                final ComponentInfo c2 = current.get(swapB).get(swapIdxB);
                current.get(swapA).set(swapIdxA, c2);
                current.get(swapB).set(swapIdxB, c1);
            } else {
                break;
            }
        }
        return current;
    }

    private static int maxDepthWithout(final List<ComponentInfo> bank, final int skipIndex) {
        int maxD = 0;
        for (int i = 0; i < bank.size(); i++) {
            if (i != skipIndex && bank.get(i).effectiveDepth > maxD) {
                maxD = bank.get(i).effectiveDepth;
            }
        }
        return maxD;
    }

    private static double maxRowsAfterChange(
            final double[] bankH,
            final int idxA,
            final double newAH,
            final int idxB,
            final double newBH,
            final double extraH) {
        double maxR = extraH;
        for (int i = 0; i < bankH.length; i++) {
            final double h = (i == idxA) ? newAH : (i == idxB ? newBH : bankH[i]);
            if (h > maxR) maxR = h;
        }
        return maxR;
    }

    private static int minimumCol(final List<NodeData> nodes) {
        return nodes.stream().mapToInt(NodeData::col).min().orElse(0);
    }

    private static int maximumCol(final List<NodeData> nodes) {
        return nodes.stream().mapToInt(NodeData::col).max().orElse(0);
    }

    private static NodeData rootOf(final NodeData node) {
        NodeData root = node;
        while (null != root.parent()) root = root.parent();
        return root;
    }

    private static double minimumRow(final List<NodeData> nodes) {
        return nodes.stream().mapToDouble(NodeData::row).min().orElse(1.0);
    }

    private static double maximumRow(final List<NodeData> nodes) {
        return nodes.stream().mapToDouble(NodeData::row).max().orElse(1.0);
    }

    public static double[] solveColumn1D(final double[] targets) {
        final int m = targets.length;
        if (0 == m) return new double[0];

        final class Block {
            double sum;
            int count;
            Block(final double sum, final int count) {
                this.sum = sum;
                this.count = count;
            }
            double avg() {
                return sum / count;
            }
        }

        final List<Block> blocks = new ArrayList<>(m);
        for (int i = 0; i < m; i++) {
            final double z = targets[i] - i;
            blocks.add(new Block(z, 1));
            while (blocks.size() > 1) {
                final Block b2 = blocks.get(blocks.size() - 1);
                final Block b1 = blocks.get(blocks.size() - 2);
                if (b1.avg() > b2.avg()) {
                    b1.sum += b2.sum;
                    b1.count += b2.count;
                    blocks.remove(blocks.size() - 1);
                } else {
                    break;
                }
            }
        }

        final double[] w = new double[m];
        int idx = 0;
        for (final Block b : blocks) {
            final double avg = b.avg();
            for (int i = 0; i < b.count; i++) {
                w[idx++] = avg;
            }
        }

        if (w[0] < 1.0) {
            final double shift = 1.0 - w[0];
            for (int i = 0; i < m; i++) {
                w[i] += shift;
            }
        }

        final double[] y = new double[m];
        for (int i = 0; i < m; i++) {
            y[i] = w[i] + i;
        }
        return y;
    }

    private static final double EPS = 1.0e-6;
    private static final int MAX_OFFSET_REDUCTION_PASSES = 16;

    /**
     * Dynamically reduces horizontal ({@code @VisualOffset}) and vertical ({@code @VisualRowOffset})
     * shifts to compact local 2D empty spaces while preserving at least
     * {@link Optimization#minOffsetSeparation} grid units of separation for originally separated branches.
     *
     * @param nodes layout nodes after {@link #optimizeLayout(List)}
     */
    public static void reduceVisualOffsets(final List<NodeData> nodes) {
        if (null == nodes || nodes.size() <= 1) return;
        final boolean doHorizontal = Optimization.reduceHorizontalOffsets;
        final boolean doVertical = Optimization.reduceVerticalOffsets;
        if (!doHorizontal && !doVertical) return;

        final int minSep = Math.max(0, Optimization.minOffsetSeparation);
        boolean anyVerticalMove = false;

        for (int pass = 0; pass < MAX_OFFSET_REDUCTION_PASSES; pass++) {
            boolean moved = false;
            if (doVertical) {
                final boolean vMoved = reduceVerticalOffsetsPass(nodes, minSep);
                anyVerticalMove |= vMoved;
                moved |= vMoved;
            }
            if (doHorizontal) {
                moved |= reduceHorizontalOffsetsPass(nodes, minSep);
            }
            if (!moved) break;
        }

        if (anyVerticalMove) {
            normalizeMinimumRow(nodes);
        }
    }

    private static boolean reduceHorizontalOffsetsPass(final List<NodeData> nodes, final int minSep) {
        final List<NodeData> candidates = new ArrayList<>();
        for (final NodeData n : nodes) {
            if (n.colOffset() > 0) {
                candidates.add(n);
            }
        }
        if (candidates.isEmpty()) return false;

        candidates.sort(Comparator.comparingInt(NodeData::col)
                .thenComparingInt(TreeLayoutOptimizer::depthOf)
                .thenComparingDouble(NodeData::row)
                .thenComparing(NodeData::id));

        boolean moved = false;
        final double vertBand = 1.0 + minSep - EPS;

        for (final NodeData root : candidates) {
            final int minAllowedCol = (null != root.parent())
                    ? (root.parent().col() + 1 + Math.min(root.colOffset(), minSep))
                    : Math.min(root.colOffset(), minSep);
            int maxShift = root.col() - minAllowedCol;
            if (maxShift <= 0) continue;

            final java.util.Set<NodeData> subtree = collectSubtree(root);
            for (final NodeData u : subtree) {
                if (u.col() < maxShift) {
                    maxShift = u.col();
                }
            }
            if (maxShift <= 0) continue;

            outer:
            for (final NodeData u : subtree) {
                final int uCol = u.col();
                final double uRow = u.row();
                for (final NodeData v : nodes) {
                    if (subtree.contains(v)) continue;
                    if (v.col() < uCol && Math.abs(uRow - v.row()) < vertBand) {
                        final int allowed = uCol - v.col() - 1 - minSep;
                        if (allowed < maxShift) {
                            maxShift = allowed;
                            if (maxShift <= 0) break outer;
                        }
                    }
                }
            }

            if (maxShift > 0) {
                for (final NodeData u : subtree) {
                    u.setCol(u.col() - maxShift);
                }
                moved = true;
            }
        }
        return moved;
    }

    private static boolean reduceVerticalOffsetsPass(final List<NodeData> nodes, final int minSep) {
        boolean moved = false;

        // 1. Pull floating un-shifted leaf siblings toward their parent so they do not artificially block shifted subtrees
        moved |= pullFloatingLeaves(nodes, minSep);

        // 2. Pull explicitly row-offset subtrees toward their parent while preserving minSep separation
        final List<NodeData> belowParent = new ArrayList<>();
        final List<NodeData> aboveParent = new ArrayList<>();
        for (final NodeData n : nodes) {
            if (null == n.parent()) continue;
            if (n.rowOffset() > 0) {
                belowParent.add(n);
            } else if (n.rowOffset() < 0) {
                aboveParent.add(n);
            }
        }

        belowParent.sort(Comparator.comparingDouble(NodeData::row).thenComparing(NodeData::id));
        for (final NodeData root : belowParent) {
            final double targetRow = root.parent().row() + Math.min(root.rowOffset(), minSep);
            if (root.row() > targetRow + EPS) {
                moved |= slideSubtreeVertically(root, nodes, root.row() - targetRow, -1, minSep);
            }
        }

        aboveParent.sort(Comparator.comparingDouble(NodeData::row).reversed().thenComparing(NodeData::id));
        for (final NodeData root : aboveParent) {
            final double targetRow = root.parent().row() - Math.min(-root.rowOffset(), minSep);
            if (root.row() < targetRow - EPS) {
                moved |= slideSubtreeVertically(root, nodes, targetRow - root.row(), +1, minSep);
            }
        }

        // 3. Pull un-shifted subtrees displaced by earlier siblings into newly opened vertical space
        final List<NodeData> unshiftedBelow = new ArrayList<>();
        final List<NodeData> unshiftedAbove = new ArrayList<>();
        for (final NodeData n : nodes) {
            if (null == n.parent() || n.rowOffset() != 0 || n.children().isEmpty()) continue;
            if (n.row() > n.parent().row() + EPS) {
                unshiftedBelow.add(n);
            } else if (n.row() < n.parent().row() - EPS) {
                unshiftedAbove.add(n);
            }
        }

        unshiftedBelow.sort(Comparator.comparingDouble(NodeData::row).thenComparing(NodeData::id));
        for (final NodeData root : unshiftedBelow) {
            final double gap = root.row() - root.parent().row();
            if (gap > EPS) {
                moved |= slideSubtreeVertically(root, nodes, gap, -1, minSep);
            }
        }

        unshiftedAbove.sort(Comparator.comparingDouble(NodeData::row).reversed().thenComparing(NodeData::id));
        for (final NodeData root : unshiftedAbove) {
            final double gap = root.parent().row() - root.row();
            if (gap > EPS) {
                moved |= slideSubtreeVertically(root, nodes, gap, +1, minSep);
            }
        }

        // 4. Re-center parents toward their children's average when doing so also moves them closer to their own parent,
        // unlocking child subtrees that were pinned below/above a parent whose upper/lower sibling already hit a ceiling.
        moved |= recenterParentsTowardParent(nodes, minSep);

        return moved;
    }

    private static boolean recenterParentsTowardParent(final List<NodeData> nodes, final int minSep) {
        final List<NodeData> parents = new ArrayList<>();
        for (final NodeData n : nodes) {
            if (null != n.parent() && n.children().size() > 1) {
                parents.add(n);
            }
        }
        if (parents.isEmpty()) return false;

        parents.sort(Comparator.comparingInt(TreeLayoutOptimizer::depthOf)
                .thenComparingDouble(NodeData::row)
                .thenComparing(NodeData::id));

        boolean moved = false;
        for (final NodeData p : parents) {
            double sum = 0.0;
            for (final NodeData c : p.children()) {
                sum += c.row();
            }
            final double avgChildRow = sum / p.children().size();
            final double parentRow = p.parent().row();

            if (p.rowOffset() >= 0 && p.row() > parentRow + EPS && p.row() > avgChildRow + EPS) {
                final double desired = parentRow + (p.rowOffset() > 0 ? Math.min(p.rowOffset(), minSep) : 0.0);
                final double target = Math.max(desired, avgChildRow);
                if (p.row() > target + EPS) {
                    moved |= slideSingleNodeVertically(p, nodes, p.row() - target, -1, minSep);
                }
            } else if (p.rowOffset() <= 0 && p.row() < parentRow - EPS && p.row() < avgChildRow - EPS) {
                final double desired = parentRow - (p.rowOffset() < 0 ? Math.min(-p.rowOffset(), minSep) : 0.0);
                final double target = Math.min(desired, avgChildRow);
                if (p.row() < target - EPS) {
                    moved |= slideSingleNodeVertically(p, nodes, target - p.row(), +1, minSep);
                }
            }
        }
        return moved;
    }

    private static boolean pullFloatingLeaves(final List<NodeData> nodes, final int minSep) {
        final List<NodeData> below = new ArrayList<>();
        final List<NodeData> above = new ArrayList<>();
        for (final NodeData n : nodes) {
            if (null == n.parent() || !n.children().isEmpty()) continue;
            if (n.colOffset() != 0 || n.rowOffset() != 0) continue;
            if (n.row() > n.parent().row() + EPS) {
                below.add(n);
            } else if (n.row() < n.parent().row() - EPS) {
                above.add(n);
            }
        }

        boolean moved = false;
        below.sort(Comparator.comparingDouble(NodeData::row).thenComparing(NodeData::id));
        for (final NodeData leaf : below) {
            final double maxShift = leaf.row() - leaf.parent().row();
            if (maxShift > EPS) {
                moved |= slideSubtreeVertically(leaf, nodes, maxShift, -1, minSep);
            }
        }

        above.sort(Comparator.comparingDouble(NodeData::row).reversed().thenComparing(NodeData::id));
        for (final NodeData leaf : above) {
            final double maxShift = leaf.parent().row() - leaf.row();
            if (maxShift > EPS) {
                moved |= slideSubtreeVertically(leaf, nodes, maxShift, +1, minSep);
            }
        }
        return moved;
    }

    private static boolean slideSingleNodeVertically(
            final NodeData node,
            final List<NodeData> allNodes,
            final double requestedShift,
            final int direction,
            final int minSep) {
        if (requestedShift <= EPS) return false;
        double maxShift = requestedShift;
        final int uCol = node.col();
        final double uRow = node.row();
        final NodeData uBranch = offsetBranchRoot(node);

        for (final NodeData v : allNodes) {
            if (v == node || v.col() != uCol) continue;
            final double vRow = v.row();
            final boolean vInSlideDirection = (direction < 0) ? (vRow < uRow - EPS) : (vRow > uRow + EPS);
            if (!vInSlideDirection) continue;

            final double required = (uBranch != offsetBranchRoot(v)) ? (1.0 + minSep) : 1.0;
            final double allowed = Math.abs(uRow - vRow) - required;
            if (allowed < maxShift) {
                maxShift = allowed;
                if (maxShift <= EPS) return false;
            }
        }

        if (maxShift > EPS) {
            node.setRow(uRow + direction * maxShift);
            return true;
        }
        return false;
    }

    private static boolean slideSubtreeVertically(
            final NodeData root,
            final List<NodeData> allNodes,
            final double requestedShift,
            final int direction,
            final int minSep) {
        if (requestedShift <= EPS) return false;
        double maxShift = requestedShift;

        final java.util.Set<NodeData> subtree = collectSubtree(root);

        outer:
        for (final NodeData u : subtree) {
            final int uCol = u.col();
            final double uRow = u.row();
            final NodeData uBranch = offsetBranchRoot(u);

            for (final NodeData v : allNodes) {
                if (subtree.contains(v)) continue;
                if (uCol != v.col()) continue;

                final double vRow = v.row();
                final boolean vInSlideDirection = (direction < 0) ? (vRow < uRow - EPS) : (vRow > uRow + EPS);
                if (!vInSlideDirection) continue;

                final double required = (uBranch != offsetBranchRoot(v)) ? (1.0 + minSep) : 1.0;
                final double allowed = Math.abs(uRow - vRow) - required;
                if (allowed < maxShift) {
                    maxShift = allowed;
                    if (maxShift <= EPS) break outer;
                }
            }
        }

        if (maxShift > EPS) {
            final double delta = direction * maxShift;
            for (final NodeData u : subtree) {
                u.setRow(u.row() + delta);
            }
            return true;
        }
        return false;
    }

    private static java.util.Set<NodeData> collectSubtree(final NodeData root) {
        final java.util.Set<NodeData> visited = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
        final java.util.ArrayDeque<NodeData> stack = new java.util.ArrayDeque<>();
        stack.push(root);
        while (!stack.isEmpty()) {
            final NodeData cur = stack.pop();
            if (visited.add(cur)) {
                for (final NodeData child : cur.children()) {
                    stack.push(child);
                }
            }
        }
        return visited;
    }

    private static int depthOf(final NodeData node) {
        int d = 0;
        NodeData cur = node.parent();
        while (null != cur) {
            d++;
            cur = cur.parent();
        }
        return d;
    }

    private static NodeData offsetBranchRoot(final NodeData node) {
        NodeData cur = node;
        while (null != cur) {
            if (cur.colOffset() != 0 || cur.rowOffset() != 0) {
                return cur;
            }
            cur = cur.parent();
        }
        return null;
    }

    private static void normalizeMinimumRow(final List<NodeData> nodes) {
        double minRow = Double.MAX_VALUE;
        for (final NodeData n : nodes) {
            if (n.row() < minRow) minRow = n.row();
        }
        if (minRow < Double.MAX_VALUE && Math.abs(minRow - 1.0) > EPS) {
            final double shift = minRow - 1.0;
            for (final NodeData n : nodes) {
                n.setRow(n.row() - shift);
            }
        }
    }

    public static void removeEmptyRowsAndColumns(final List<NodeData> nodes) {
        if (Optimization.reduceHorizontalGaps) {
            removeEmptyColumns(nodes);
        }
        if (Optimization.reduceVerticalGaps) {
            removeEmptyRows(nodes);
        }
    }

    private static void removeEmptyColumns(final List<NodeData> nodes) {
        if (nodes.isEmpty()) return;

        final int minSep = Math.max(0, Optimization.minOffsetSeparation);
        final Map<Integer, List<NodeData>> colNodesMap = new TreeMap<>();
        for (final NodeData n : nodes) {
            colNodesMap.computeIfAbsent(n.col(), k -> new ArrayList<>()).add(n);
        }

        final Map<Integer, Integer> colMapping = new HashMap<>();
        int prevOldCol = -1;
        int prevNewCol = -1;

        for (final Map.Entry<Integer, List<NodeData>> entry : colNodesMap.entrySet()) {
            final int oldCol = entry.getKey();
            final int newCol;
            if (prevOldCol == -1) {
                int initialRequired = 0;
                if (minSep > 0 && oldCol > 0) {
                    for (final NodeData n : entry.getValue()) {
                        if (n.colOffset() > 0 && null == n.parent()) {
                            initialRequired = Math.max(initialRequired, Math.min(n.colOffset(), minSep));
                        }
                    }
                }
                newCol = Math.min(oldCol, initialRequired);
            } else {
                final int emptyBetween = oldCol - prevOldCol - 1;
                int requiredGap = 0;
                if (emptyBetween > 0 && minSep > 0) {
                    for (final NodeData n : entry.getValue()) {
                        if (n.colOffset() > 0 && (null == n.parent() || n.parent().col() <= prevOldCol)) {
                            requiredGap = Math.max(requiredGap, Math.min(n.colOffset(), minSep));
                        }
                    }
                }
                final int keptEmpty = Math.min(emptyBetween, requiredGap);
                newCol = prevNewCol + 1 + keptEmpty;
            }
            colMapping.put(oldCol, newCol);
            prevOldCol = oldCol;
            prevNewCol = newCol;
        }

        for (final NodeData n : nodes) {
            n.setCol(colMapping.get(n.col()));
        }
    }

    private static void removeEmptyRows(final List<NodeData> nodes) {
        if (nodes.isEmpty()) return;

        final int minSep = Math.max(0, Optimization.minOffsetSeparation);
        final Map<Integer, List<NodeData>> rowNodesMap = new TreeMap<>();
        for (final NodeData n : nodes) {
            final int roundedRow = (int) Math.round(n.row());
            rowNodesMap.computeIfAbsent(roundedRow, k -> new ArrayList<>()).add(n);
        }

        double shift = 0.0;
        int previousRow = -1;
        List<NodeData> previousNodes = null;

        for (final Map.Entry<Integer, List<NodeData>> entry : rowNodesMap.entrySet()) {
            final int currentRow = entry.getKey();
            final List<NodeData> currentNodes = entry.getValue();
            if (previousRow != -1) {
                final int emptyBetween = currentRow - previousRow - 1;
                if (emptyBetween > 0) {
                    int requiredGap = 0;
                    if (minSep > 0 && hasOffsetSeparationBetween(previousNodes, currentNodes)) {
                        requiredGap = minSep;
                    }
                    final int keptEmpty = Math.min(emptyBetween, requiredGap);
                    shift += (emptyBetween - keptEmpty);
                }
            }
            if (shift > 0.0) {
                for (final NodeData n : currentNodes) {
                    n.setRow(n.row() - shift);
                }
            }
            previousRow = currentRow;
            previousNodes = currentNodes;
        }
    }

    private static boolean hasOffsetSeparationBetween(final List<NodeData> prevRowNodes, final List<NodeData> currRowNodes) {
        if (null == prevRowNodes || null == currRowNodes) return false;
        for (final NodeData a : prevRowNodes) {
            final NodeData branchA = offsetBranchRoot(a);
            for (final NodeData b : currRowNodes) {
                final NodeData branchB = offsetBranchRoot(b);
                if (branchA != branchB) {
                    return true;
                }
            }
        }
        return false;
    }
}
