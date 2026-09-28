package works.lysenko.util.func.ui;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Objects.isNull;
import static works.lysenko.util.chrs.___.FOR;
import static works.lysenko.util.chrs.____.EDIT;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Swap.f;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.data.strs.Wrap.q;
import static works.lysenko.util.func.type.Files.loadLinesFromFile;
import static works.lysenko.util.func.ui.Locators.editTextWithText;
import static works.lysenko.util.lang.N.NOT_FOUND;
import static works.lysenko.util.lang.word.D.DESCRIPTOR;
import static works.lysenko.util.spec.Layout.Files.LOCATORS_;
import static works.lysenko.util.spec.Symbols._EQUAL_;


/**
 * Descriptors class provides methods for generating and retrieving locators for various elements.
 * It also offers utility methods for manipulation and formatting of strings.
 */
@SuppressWarnings({"ForeachStatement", "MethodWithMultipleReturnPoints", "StaticCollection"})
public record Descriptors() {

    private static final Map<String, String> descriptors = new HashMap<>(0);

    static {
        final List<String> lines = loadLinesFromFile(Path.of(LOCATORS_));
        for (final String line : lines) {
            final String[] a = line.split(s(_EQUAL_, _EQUAL_));
            if (1 < a.length) descriptors.put(a[0], a[1]);
        }
    }

    /**
     * Generate "Any EditText element with given value of 'text' attribute" descriptor.
     *
     * @param text to search for
     * @return locator for text attribute of any element
     */
    @SuppressWarnings({"StaticMethodOnlyUsedInOneClass", "unused"})
    public static String edit(final String text) {

        final String key = b(EDIT, q(text));
        final String value = s(editTextWithText(text));
        put(key, value);
        return key;
    }

    /**
     * Alias for {@link Descriptors#get(String, Object...)} which can be statically imported for shorter test code
     *
     * @param templateKey of a Descriptor
     * @param values      to be used for placeholder replacement
     * @return correspondent Locator with provided values
     */
    @SuppressWarnings("WeakerAccess")
    public static String fill(final String templateKey, final Object... values) {

        return get(templateKey, values);
    }

    /**
     * Retrieve locator for specified Descriptor and replace placeholders with values provided as varargs
     *
     * @param key    of a Descriptor
     * @param values to be used for placeholder replacement
     * @return correspondent Locator with provided values
     */
    @SuppressWarnings("OverloadedVarargsMethod")
    private static String get(final String key, final Object... values) {

        if (0 == values.length) return get(key);
        return (f(get(key), values));
    }

    /**
     * Retrieve locator for specified Descriptor
     *
     * @param key of a Descriptor
     * @return correspondent Locator
     */
    public static String get(final String key) {

        final String result = descriptors.get(key);
        if (isNull(result)) throw new IllegalArgumentException(b(c(DESCRIPTOR), FOR, q(key), NOT_FOUND));
        return result;
    }

    /**
     * Add a Descriptor with given key. Added Descriptor is not persisted.
     * This method is mainly utilized by {@link Locators#text(String)} method for adding locators for text elements
     *
     * @param key   of added Descriptor
     * @param value of added Descriptor
     */
    public static void put(final String key, final String value) {

        descriptors.put(key, value);
    }
}
