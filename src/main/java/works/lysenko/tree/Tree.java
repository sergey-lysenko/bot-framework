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

    /**
     * The name of the root package representing the root of the scenario tree.
     */
    private final String rootPackage;

    /**
     * The set of first-level scenario branches discovered in the root package.
     */
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
     * Retrieves the root package name of this tree.
     *
     * @return the root package name
     */
    public String getRootPackage() {
        return rootPackage;
    }

    /**
     * Retrieves the first-level scenario branches discovered in the root package.
     *
     * @return the set of first-level scenario branches
     */
    public Set<_Scenario> getBranches() {
        return firstLevelBranches;
    }
}
