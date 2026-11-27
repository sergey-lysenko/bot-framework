package works.lysenko.base.output.tree;

import works.lysenko.base.output.ProgressionSettings;
import works.lysenko.base.output.TreeHtml.NodeData;
import works.lysenko.base.output.TreeHtml.TreeLayout;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import static works.lysenko.util.data.strs.Swap.s;

/**
 * Smoothly interpolates scenario tree node positions ({@code visualCol}, {@code row}) across
 * progression frames using exponential easing matching {@link TreeCameraTransform}.
 * <p>
 * Only a lightweight map of {@code stableNodeKey -> [visualCol, row]} is persisted between frames;
 * target coordinates are computed fresh each frame by {@link works.lysenko.base.output.TreeHtml#computeLayout}.
 */
public final class TreeNodePositionAnimator {

    static final double ALPHA = 0.20;
    private static final double SNAP_COL = 0.002; // ~0.5 px at 270 px/col
    private static final double SNAP_ROW = 0.008; // ~0.5 px at 60 px/row

    private static final Map<String, double[]> currentPositions = new HashMap<>();
    private static boolean initialized = false;

    private TreeNodePositionAnimator() {
    }

    /**
     * Computes the effective per-frame exponential easing factor by scaling {@link #ALPHA}
     * with the configured {@code .tree.progression.movement.speed} multiplier, clamped to {@code (0, 1]}.
     */
    static double effectiveAlpha() {
        final double speed = ProgressionSettings.current().treeMovementSpeed();
        if (!(speed > 0.0) || Double.isInfinite(speed)) {
            return ALPHA;
        }
        return Math.min(1.0, ALPHA * speed);
    }

    /**
     * Advances persisted node positions one step toward the targets in {@code layout} and writes
     * the smoothed coordinates onto each node's {@link NodeData#setVisualCol(double)} and
     * {@link NodeData#setRow(double)}.
     * <p>
     * On the first frame after {@link #reset()}, all nodes snap directly to their targets.
     * Nodes that appear mid-run seed from their parent's current position so branches bloom
     * smoothly out of their parent.
     */
    public static synchronized void applySmoothPositions(final TreeLayout layout) {
        if (null == layout || layout.nodes().isEmpty()) {
            return;
        }

        final List<NodeData> ordered = topDownOrder(layout.nodes());
        final Map<NodeData, String> keys = computeStableKeys(ordered);
        final Map<String, double[]> nextPositions = new HashMap<>(ordered.size() * 2);

        if (!initialized) {
            for (final NodeData n : ordered) {
                final double targetCol = n.col();
                final double targetRow = n.row();
                nextPositions.put(keys.get(n), new double[]{targetCol, targetRow});
                n.setVisualCol(targetCol);
                n.setRow(targetRow);
            }
            currentPositions.clear();
            currentPositions.putAll(nextPositions);
            initialized = true;
            return;
        }

        final double alpha = effectiveAlpha();

        for (final NodeData n : ordered) {
            final String key = keys.get(n);
            final double targetCol = n.col();
            final double targetRow = n.row();

            double[] cur = currentPositions.get(key);
            if (null == cur) {
                // Mid-run newly appeared node: bloom out of parent's current position if available
                final NodeData parent = n.parent();
                final double[] parentPos = (null != parent) ? nextPositions.get(keys.get(parent)) : null;
                if (null != parentPos) {
                    cur = new double[]{parentPos[0], parentPos[1]};
                } else {
                    cur = new double[]{targetCol, targetRow};
                }
            }

            double nextCol = cur[0] + alpha * (targetCol - cur[0]);
            double nextRow = cur[1] + alpha * (targetRow - cur[1]);

            if (Math.abs(targetCol - nextCol) < SNAP_COL) {
                nextCol = targetCol;
            }
            if (Math.abs(targetRow - nextRow) < SNAP_ROW) {
                nextRow = targetRow;
            }

            nextPositions.put(key, new double[]{nextCol, nextRow});
            n.setVisualCol(nextCol);
            n.setRow(nextRow);
        }

        currentPositions.clear();
        currentPositions.putAll(nextPositions);
    }

    /**
     * Checks whether every node in {@code targetLayout} has converged to its target coordinates
     * without advancing the animation state.
     */
    public static synchronized boolean isSettled(final TreeLayout targetLayout) {
        if (!initialized || null == targetLayout || targetLayout.nodes().isEmpty()) {
            return true;
        }

        final List<NodeData> ordered = topDownOrder(targetLayout.nodes());
        final Map<NodeData, String> keys = computeStableKeys(ordered);

        for (final NodeData n : ordered) {
            final double[] cur = currentPositions.get(keys.get(n));
            if (null == cur) {
                // A brand-new node with a parent still needs to glide from parent -> target
                if (null != n.parent()) {
                    return false;
                }
                continue;
            }
            if (Math.abs(n.col() - cur[0]) >= SNAP_COL || Math.abs(n.row() - cur[1]) >= SNAP_ROW) {
                return false;
            }
        }
        return true;
    }

    /**
     * Clears persisted node positions so the next frame starts fresh without carry-over.
     */
    public static synchronized void reset() {
        currentPositions.clear();
        initialized = false;
    }

    static synchronized boolean isInitialized() {
        return initialized;
    }

    private static List<NodeData> topDownOrder(final List<NodeData> nodes) {
        final List<NodeData> ordered = new ArrayList<>(nodes.size());
        final Map<NodeData, Boolean> visited = new IdentityHashMap<>(nodes.size() * 2);
        final Deque<NodeData> queue = new ArrayDeque<>(nodes.size());

        for (final NodeData n : nodes) {
            if (null == n.parent() && null == visited.putIfAbsent(n, Boolean.TRUE)) {
                queue.add(n);
            }
        }
        while (!queue.isEmpty()) {
            final NodeData curr = queue.poll();
            ordered.add(curr);
            for (final NodeData child : curr.children()) {
                if (null == visited.putIfAbsent(child, Boolean.TRUE)) {
                    queue.add(child);
                }
            }
        }
        // Append any disconnected/orphan nodes that were not reachable from a null-parent root
        if (ordered.size() < nodes.size()) {
            for (final NodeData n : nodes) {
                if (null == visited.putIfAbsent(n, Boolean.TRUE)) {
                    ordered.add(n);
                }
            }
        }
        return ordered;
    }

    /**
     * Computes a stable root-to-node lineage key for every node so identity does not shift when
     * sibling nodes are hidden, pruned, or added (unlike {@link NodeData#id()}).
     */
    static Map<NodeData, String> computeStableKeys(final List<NodeData> topDownNodes) {
        final Map<NodeData, String> keys = new IdentityHashMap<>(topDownNodes.size() * 2);
        final Map<String, Integer> siblingCounts = new HashMap<>(topDownNodes.size() * 2);

        for (final NodeData n : topDownNodes) {
            final String parentKey = (null != n.parent()) ? keys.get(n.parent()) : null;
            final String segment = (null != n.scenario())
                    ? n.scenario().getClass().getName()
                    : ((null != n.label()) ? n.label() : n.id());
            final String baseKey = (null != parentKey) ? s(parentKey, "/", segment) : segment;
            final int count = siblingCounts.getOrDefault(baseKey, 0);
            siblingCounts.put(baseKey, count + 1);
            keys.put(n, (0 == count) ? baseKey : s(baseKey, "#", count));
        }
        return keys;
    }
}
