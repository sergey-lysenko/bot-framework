package works.lysenko.base.output.svg;

import works.lysenko.util.data.type.Result;

import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.spec.Symbols._DOT_;

/**
 * This class represents a collection of groups.
 * Each group is identified by a key and contains a map of results associated with that group.
 * The groups are stored in a TreeMap, which provides easy access and sorting based on the key.
 */
@SuppressWarnings({"BoundedWildcard", "CloneableClassInSecureContext", "ClassWithoutLogger", "PublicMethodWithoutLogging",
        "ClassWithTooManyTransitiveDependents", "ClassExtendsConcreteCollection", "ClassWithTooManyTransitiveDependencies",
        "CyclicClassDependency", "FinalClass", "ImplicitCallToSuper", "WeakerAccess", "UseOfConcreteClass",
        "ClassUnconnectedToPackage"})
public final class Groups extends TreeMap<String, TreeMap<String, Result>> {

    /**
     * Default constructor
     */
    private Groups() {

    }

    /**
     * @param key   of new group
     * @param value of new group
     */
    @SuppressWarnings({"PublicConstructor", "UnqualifiedMethodAccess", "LocalCanBeFinal"})
    public Groups(String key, TreeMap<String, Result> value) {

        put(key, value);
    }

    /**
     * @param map of groups
     */
    @SuppressWarnings({"unused", "PublicConstructor", "LocalCanBeFinal"})
    public Groups(SortedMap<String, TreeMap<String, Result>> map) {

        super(map);
    }

    /**
     * @param source to process
     * @return Groups
     */
    @SuppressWarnings({"TypeMayBeWeakened", "ChainedMethodCall", "ObjectAllocationInLoop", "LocalCanBeFinal",
            "NestedMethodCall",
            "ForeachStatement", "AutoBoxing"})
    public static Groups process(SortedMap<String, Result> source) {

        Groups g = new Groups();
        for (Map.Entry<String, Result> e : source.entrySet()) {
            String groupId = e.getKey().split("\\.")[0];
            String oldKey = e.getKey();
            String newKey = oldKey.replaceFirst(s(groupId, _DOT_), EMPTY);
            Result newValue = e.getValue();
            TreeMap<String, Result> newMap = null == g.get(groupId) ? new TreeMap<>() : g.get(groupId);
            newMap.put(newKey, newValue);
            g.put(groupId, newMap);
        }
        return g;
    }

}
