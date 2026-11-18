package works.lysenko.util.apis.util;

/**
 * ProvidesBrackets is an interface that represents a bracket type along with its opening and closing characters.
 * Implementing classes should override the methods to provide the specific opening and closing characters for the bracket
 * type.
 */
@SuppressWarnings("InterfaceWithOnlyOneDirectInheritor")
public interface _Brackets {

    /**
     * @return closing bracket
     */
    char closing();

    /**
     * @return opening bracket
     */
    char opening();
}
