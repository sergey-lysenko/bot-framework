import java.util.Iterator;
import java.util.List;
import works.lysenko.base.output.TreeHtml.NodeData;
import works.lysenko.base.output.TreeHtml.Edge;

static void pruneOffBranches(List<NodeData> nodes, List<Edge> edges) {
    if (!works.lysenko.util.prop.tree.HideOffBranches.b()) return;
    
    // identify nodes to remove
    java.util.Set<NodeData> toRemove = new java.util.HashSet<>();
    for (NodeData n : nodes) {
        if (!isTheoreticallyExecutable(n)) {
            toRemove.add(n);
        }
    }
    
    if (toRemove.isEmpty()) return;
    
    nodes.removeAll(toRemove);
    
    Iterator<Edge> edgeIt = edges.iterator();
    while (edgeIt.hasNext()) {
        Edge e = edgeIt.next();
        if (toRemove.contains(e.from()) || toRemove.contains(e.to())) {
            edgeIt.remove();
        }
    }
    
    for (NodeData n : nodes) {
        n.children().removeAll(toRemove);
    }
}
