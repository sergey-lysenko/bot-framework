package works.lysenko.base.output.svg;

import works.lysenko.util.data.type.Result;

import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * @author Sergii Lysenko
 */
@SuppressWarnings({"ClassHasNoToStringMethod", "ParameterHidesMemberVariable", "ClassWithoutLogger",
        "PublicMethodWithoutLogging",
        "ClassWithTooManyTransitiveDependents", "WeakerAccess", "ClassWithTooManyTransitiveDependencies",
        "CyclicClassDependency",
        "UnqualifiedFieldAccess", "ImplicitCallToSuper", "UseOfConcreteClass", "ClassUnconnectedToPackage"})
public class Parts {

    /**
     * Sorted map containing the head of results tree.
     */
    public final SortedMap<String, Result> head;

    /**
     * Sorted map containing the body of results tree.
     * <p>
     * The body is a part of the tree structure that contains the results of scenarios whose names start with a lowercase
     * letter.
     * The map is sorted by the keys in natural order.
     * <p>
     * The keys in the map are the names of the scenarios,
     * and the values are the corresponding Result objects that store the information about the scenarios.
     * <p>
     * The body map is declared as final, meaning its reference cannot be changed once initialized.
     */
    public final SortedMap<String, Result> body;

    /**
     * A1 class representing parts of a tree structure.
     */
    @SuppressWarnings({"unused", "PublicConstructor"})
    public Parts() {

        head = new TreeMap<>();
        body = new TreeMap<>();
    }

    /**
     * @param head of results tree
     * @param body of results tree
     */
    @SuppressWarnings({"PublicConstructor", "AssignmentOrReturnOfFieldWithMutableType", "LocalCanBeFinal"})
    public Parts(SortedMap<String, Result> head, SortedMap<String, Result> body) {

        this.head = head;
        this.body = body;
    }

    /**
     * @param source to process
     * @return Parts
     */
    @SuppressWarnings({"TypeMayBeWeakened", "ChainedMethodCall", "LocalCanBeFinal", "NestedMethodCall", "ForeachStatement"})
    public static Parts process(SortedMap<String, Result> source) {

        TreeMap<String, Result> head = new TreeMap<>();
        TreeMap<String, Result> body = new TreeMap<>();
        for (Map.Entry<String, Result> e : source.entrySet())
            if (Character.isUpperCase(e.getKey().charAt(0)))
                head.put(e.getKey(), e.getValue());
            else
                body.put(e.getKey(), e.getValue());
        return new Parts(head, body);
    }

}
