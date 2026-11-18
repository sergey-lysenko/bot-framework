package works.lysenko.util.apis.scenario;

import works.lysenko.util.apis.data._Result;

/**
 * Read-only view of a scenario tree node used by response and defect matchers without coupling to HTML/SVG renderers.
 */
public interface _TreeNodeView {

    /**
     * Node display label.
     *
     * @return node label
     */
    String label();

    /**
     * Node package/group identifier.
     *
     * @return node group
     */
    String group();

    /**
     * Associated scenario execution result, if any.
     *
     * @return execution result or null
     */
    _Result result();

    /**
     * Associated scenario instance, if any.
     *
     * @return scenario or null
     */
    _Scenario scenario();
}
