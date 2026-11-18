package works.lysenko.util.apis.log;

/**
 * _Indentation is an interface that provides methods for retrieving the depth and level of indentation in the output log.
 */
@SuppressWarnings("InterfaceWithOnlyOneDirectInheritor")
public interface _Indentation {

    /**
     * @return depth in the output log
     */
    int depth();

    /**
     * @return level of log
     */
    int level();
}
