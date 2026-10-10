package works.lysenko.tree;

import works.lysenko.util.apis.scenario._Scenario;
import works.lysenko.util.func.core.ClassLoader;

import java.util.Set;

/**
 * Formalizes the scenario execution tree structure.
 * The paradigm assumes that a package acts as the root of the tree,
 * and the classes within that package represent the 1-N first-level branches.
 */
public class Tree {

    private final String rootPackage;
    private final Set<_Scenario> firstLevelBranches;

    /**
     * Initializes the tree by scanning the specified root package for scenario branches.
     *
     * @param rootPackage the root package representing the tree
     */
    public Tree(final String rootPackage) {
        this.rootPackage = rootPackage;
        this.firstLevelBranches = ClassLoader.readFrom(rootPackage, false);
    }

    /**
     * @return the root package name
     */
    public String getRootPackage() {
        return rootPackage;
    }

    /**
     * @return the first-level scenario branches discovered in the root package
     */
    public Set<_Scenario> getBranches() {
        return firstLevelBranches;
    }
}
