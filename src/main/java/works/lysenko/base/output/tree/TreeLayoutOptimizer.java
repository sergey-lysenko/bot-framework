package works.lysenko.base.output.tree;

import works.lysenko.base.output.TreeHtml.NodeData;
import works.lysenko.util.prop.tree.Optimization;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
                        leaf.setCol(parent.col() + 1 + extraCol);
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

        if (Optimization.progression && ordered.size() >= 2 && Optimization.bankColumns >= 2) {
            int maxDepth = 0;
            double totalStackedRows = 0.0;
            for (int i = 0; i < ordered.size(); i++) {
                final List<NodeData> comp = ordered.get(i);
                final int minC = minimumCol(comp);
                final int maxC = maximumCol(comp);
                final int d = maxC - minC + 1;
                if (d > maxDepth) maxDepth = d;
                final double h = maximumRow(comp) - minimumRow(comp) + 1.0;
                totalStackedRows += h;
                if (i > 0) totalStackedRows += 1.0;
            }
            final double naturalWidth = maxDepth * COL_WIDTH;
            final double naturalHeight = totalStackedRows * ROW_HEIGHT;
            final double naturalAspectRatio = (naturalHeight > 0.0) ? (naturalWidth / naturalHeight) : 1.0;

            if (naturalAspectRatio < Optimization.maxAspectRatio) {
                layoutMultiColumnBanks(ordered);
                return;
            }
        }

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

    private static void layoutMultiColumnBanks(final List<List<NodeData>> ordered) {
        final int numBanks = Math.max(1, Math.min(Optimization.bankColumns, ordered.size()));
        if (numBanks <= 1) {
            double nextRow = 1.0;
            for (final List<NodeData> component : ordered) {
                final double minimum = minimumRow(component);
                final double shift = nextRow - minimum;
                if (0.0 != shift)
                    for (final NodeData node : component)
                        node.setRow(node.row() + shift);
                nextRow = maximumRow(component) + 1.0;
            }
            return;
        }

        final List<List<NodeData>> sortedByHeight = new ArrayList<>(ordered);
        sortedByHeight.sort((c1, c2) -> {
            final double h1 = maximumRow(c1) - minimumRow(c1) + 1.0;
            final double h2 = maximumRow(c2) - minimumRow(c2) + 1.0;
            final int hCmp = Double.compare(h2, h1);
            if (hCmp != 0) return hCmp;
            final String l1 = c1.isEmpty() ? "" : c1.get(0).label();
            final String l2 = c2.isEmpty() ? "" : c2.get(0).label();
            final int lCmp = l1.compareTo(l2);
            if (lCmp != 0) return lCmp;
            final String id1 = c1.isEmpty() ? "" : c1.get(0).id();
            final String id2 = c2.isEmpty() ? "" : c2.get(0).id();
            return id1.compareTo(id2);
        });

        final List<List<List<NodeData>>> banks = new ArrayList<>();
        for (int i = 0; i < numBanks; i++) {
            banks.add(new ArrayList<>());
        }
        final double[] bankHeights = new double[numBanks];

        for (final List<NodeData> component : sortedByHeight) {
            int bestBank = 0;
            double minHeight = bankHeights[0];
            for (int b = 1; b < numBanks; b++) {
                if (bankHeights[b] < minHeight) {
                    minHeight = bankHeights[b];
                    bestBank = b;
                }
            }
            banks.get(bestBank).add(component);
            final double compH = maximumRow(component) - minimumRow(component) + 1.0;
            bankHeights[bestBank] += compH + 1.0;
        }

        banks.removeIf(List::isEmpty);

        banks.sort(Comparator.comparingInt(bank -> bank.stream().mapToInt(ordered::indexOf).min().orElse(0)));

        for (final List<List<NodeData>> bank : banks) {
            bank.sort(Comparator.comparingInt(ordered::indexOf));
        }

        int currentColOffset = 0;
        for (final List<List<NodeData>> bank : banks) {
            double nextRow = 1.0;
            int bankMaxDepth = 0;
            for (final List<NodeData> component : bank) {
                final double minR = minimumRow(component);
                final double shiftR = nextRow - minR;
                final int minC = minimumCol(component);
                final int shiftC = currentColOffset - minC;

                final int depth = maximumCol(component) - minC + 1;
                if (depth > bankMaxDepth) bankMaxDepth = depth;

                for (final NodeData node : component) {
                    if (0.0 != shiftR) node.setRow(node.row() + shiftR);
                    if (0 != shiftC) node.setCol(node.col() + shiftC);
                }
                nextRow = maximumRow(component) + 1.0;
            }
            currentColOffset += bankMaxDepth;
        }
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
}
