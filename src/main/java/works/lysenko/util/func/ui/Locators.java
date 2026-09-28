package works.lysenko.util.func.ui;

import org.openqa.selenium.By;
import org.openqa.selenium.Rectangle;
import works.lysenko.util.Constants;
import works.lysenko.util.data.enums.Platform;

import java.util.Arrays;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static works.lysenko.Base.logDebug;
import static works.lysenko.base.core.Routines.in;
import static works.lysenko.util.Constants.*;
import static works.lysenko.util.chrs.__.IS;
import static works.lysenko.util.chrs.___.NTH;
import static works.lysenko.util.chrs.____.*;
import static works.lysenko.util.data.enums.Brackets.ROUND;
import static works.lysenko.util.data.enums.Brackets.SQUARE;
import static works.lysenko.util.data.enums.Platform.ANDROID;
import static works.lysenko.util.data.records.KeyValue.kv;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.data.strs.Vars.a;
import static works.lysenko.util.data.strs.Wrap.e;
import static works.lysenko.util.data.strs.Wrap.q;
import static works.lysenko.util.func.core.Assertions.еггог;
import static works.lysenko.util.func.type.Collector.selectOneOf;
import static works.lysenko.util.lang.C.COMMA_SPACE;
import static works.lysenko.util.lang.word.A.ACTUAL;
import static works.lysenko.util.lang.word.B.BUTTON;
import static works.lysenko.util.lang.word.E.EXPECTED;
import static works.lysenko.util.lang.word.I.INDEX;
import static works.lysenko.util.lang.word.P.PLATFORM;
import static works.lysenko.util.spec.Symbols.*;

/**
 * The Locators class provides methods for generating locator objects based on given strings.
 */
@SuppressWarnings("unused")
public record Locators() {

    /**
     * Generate {@link org.openqa.selenium.By} object corresponding to given string
     * locator
     *
     * @param locator string
     * @return proper locator object based on contents of source string contents
     */
    public static By by(final String locator) {

        return by(locator, false);
    }

    /**
     * Generate {@link By} object corresponding to given string
     * locator or descriptor
     *
     * @param locator string
     * @param expose  locator or not
     * @return proper locator object based on contents of source string contents
     */
    @SuppressWarnings({"OverlyComplexBooleanExpression", "MethodWithMultipleReturnPoints", "OverlyComplexMethod"})
    public static By by(final String locator, final boolean expose) {

        if (locator.startsWith(s(_SLASH_, _SLASH_)) || locator.startsWith(s(_DOT_, _SLASH_)) || locator.startsWith(s(_DOT_,
                _SLASH_, _SLASH_)) || locator.startsWith(s(OPN_BRK, _DOT_, _SLASH_, _SLASH_)) || locator.startsWith(s(OPN_BRK, _SLASH_, _SLASH_)))
            return By.xpath(locator);
        if (!locator.isEmpty() && _NUMBR_ == locator.charAt(0) || locator.contains(e(s(GRT_THN))) || locator.contains(s(_COLON_, NTH, _DASH_)))
            return By.cssSelector(locator);
        final String a = Descriptors.get(locator);
        if (expose) еггог(a);
        return by(a);
    }

    /**
     * Generate {@link By} object corresponding to given string
     * locator
     *
     * @param locators array of locator strings to randomly select from
     * @return one of the locators
     */
    @SuppressWarnings("OverloadedVarargsMethod")
    public static By by(final String... locators) {

        return by((String) selectOneOf(locators));
    }

    /**
     * Generate readable description of an object addressed by String locators {@link Rectangle}
     *
     * @param locators to object
     * @return it's description
     */
    @SuppressWarnings({"MethodWithMultipleReturnPoints", "StaticMethodOnlyUsedInOneClass"})
    public static String describe(final String... locators) {

        if (0 == locators.length) return EMPTY;
        if (1 == locators.length) return locators[0];
        return Arrays.toString(locators);
    }

    /**
     * Edit the text with the given input text.
     *
     * @param s The input text to edit
     * @return The edited text as a String
     */
    @SuppressWarnings("WeakerAccess")
    public static String editTextWithText(final String s) {

        if (in(ANDROID)) return s(__ANDROID_WIDGET, Constants.EDIT_TEXT, e(SQUARE, s(_AT_SGN, TEXT, _EQUAL_, q(s))));
        throw new IllegalStateException(getWrongPlatformMessage(ANDROID));

    }

    /**
     * Returns the element with the given index as a formatted string representation.
     *
     * @param element the element to format
     * @param i       the index of the element
     * @return the formatted string representation of the element with the given index
     */
    @SuppressWarnings("unused")
    public static String elementWithIndex(final String element, final int i) {

        if (in(ANDROID)) return s(__ANDROID_WIDGET, element, e(SQUARE, s(_AT_SGN, INDEX, _EQUAL_, q(s(i)))));
        throw new IllegalStateException(getWrongPlatformMessage(ANDROID));
    }

    @SuppressWarnings("SameParameterValue")
    private static String getWrongPlatformMessage(final Platform expected) {

        return s(c(EXPECTED), PLATFORM, IS, expected, ACTUAL, IS, in());
    }

    /**
     * Retrieves the text of an n-th EditText element with the given index as a text string.
     *
     * @param i The index of the EditText element to retrieve text from
     * @return The text of the element
     */
    public static String nthEditText(final int i) {

        if (in(ANDROID)) return nthElement(Constants.EDIT_TEXT, i);
        throw new IllegalStateException(getWrongPlatformMessage(ANDROID));
    }

    private static String nthElement(final String element, final int i) {

        if (in(ANDROID)) return s(__ANDROID_WIDGET, element, e(SQUARE, s(i)));
        throw new IllegalStateException(getWrongPlatformMessage(ANDROID));
    }

    @SuppressWarnings("SameParameterValue")
    private static String nthInternalElement(final String element, final int i) {

        if (in(ANDROID)) return s(Constants._ANDROID_INTERNAL_WIDGET, element, e(SQUARE, s(i)));
        throw new IllegalStateException(getWrongPlatformMessage(ANDROID));
    }

    /**
     * Returns the locator for the nth RecyclerView element.
     *
     * @param i The index of the RecyclerView element.
     * @return The locator for the nth RecyclerView element.
     * @throws IllegalStateException If the current platform is not Android.
     */
    @SuppressWarnings("WeakerAccess")
    public static String nthRecycler(final int i) {

        if (in(ANDROID)) return nthInternalElement(Constants.RECYCLER_VIEW, i);
        throw new IllegalStateException(getWrongPlatformMessage(ANDROID));
    }

    /**
     * Returns the locator for the nth TabHost element.
     *
     * @param i The index of the TabHost element.
     * @return The locator for the nth TabHost element.
     * @throws IllegalStateException If the current platform is not Android.
     */
    @SuppressWarnings("WeakerAccess")
    public static String nthTabHost(final int i) {

        if (in(ANDROID)) return nthInternalElement(Constants.TAB_HOST, i);
        throw new IllegalStateException(getWrongPlatformMessage(ANDROID));
    }

    /**
     * Returns the locator for the nth ScrollView element.
     *
     * @param i The index of the ScrollView element.
     * @return The locator for the nth ScrollView element.
     * @throws IllegalStateException If the current platform is not Android.
     */
    public static String nthScrollView(final int i) {

        if (in(ANDROID)) return nthElement(Constants.SCROLL_VIEW, i);
        throw new IllegalStateException(getWrongPlatformMessage(ANDROID));
    }

    /**
     * Get the locator for the nth TextView element.
     *
     * @param i The index of the TextView element.
     * @return The locator for the nth TextView element.
     * @throws IllegalStateException If the current platform is not Android.
     */
    public static String nthTextView(final int i) {

        if (in(ANDROID)) return nthElement(TEXT_VIEW, i);
        throw new IllegalStateException(getWrongPlatformMessage(ANDROID));
    }

    /**
     * Retrieves the locator for the first RecyclerView element.
     *
     * @return The locator for the first RecyclerView element
     */
    @SuppressWarnings("StaticMethodOnlyUsedInOneClass")
    public static String recycler() {

        return nthRecycler(1);
    }

    /**
     * Retrieves the locator for the first TabHost element.
     *
     * @return The locator for the first TabHost element.
     */
    public static String tabHost() {

        return nthTabHost(1);
    }

    /**
     * Retrieves the locator for the first ScrollView element.
     *
     * @return The locator for the first ScrollView element.
     */
    @SuppressWarnings("StaticMethodOnlyUsedInOneClass")
    public static String scrollView() {

        return nthScrollView(1);
    }

    /**
     * Wrapper for {@link Locators#text(String)} accepting {@code int} values
     *
     * @param i int value to search for
     * @return locator for text attribute of any element
     */
    public static String text(final int i) {

        return text(s(i));
    }

    /**
     * Generate a locator string based on the input text.
     *
     * @param text The input text
     * @return The generated locator string
     */
    @SuppressWarnings("StandardVariableNames")
    public static String text(final String text) {

        final String k = b(TEXT, q(text));
        final String v = s(textWithText(text));
        Descriptors.put(k, v);
        return k;
    }

    /**
     * Edits the given text and generates a descriptor string for it.
     * The edited string is stored in a descriptor map and returned.
     *
     * @param text The input text to be edited and used for generating the descriptor string.
     * @return The generated descriptor string corresponding to the edited text.
     */
    @SuppressWarnings("StandardVariableNames")
    public static String edit(final String text) {

        final String k = b(EDIT, q(text));
        final String v = s(editWithText(text));
        Descriptors.put(k, v);
        return k;
    }

    /**
     * Generates a descriptor string based on the given input text. The generated
     * descriptor string is created by binding certain components of the text and
     * invoking specific formatting methods. The descriptor is then stored in a
     * descriptor map and returned.
     *
     * @param text The input text used for generating the descriptor string.
     * @return The generated descriptor string corresponding to the input text.
     */
    @SuppressWarnings("StandardVariableNames")
    public static String desc(final String text) {

        final String k = b(DESC, q(text));
        final String v = s(viewWithDesc(text));
        Descriptors.put(k, v);
        return k;
    }

    /**
     * Generates a button locator string based on the given input text.
     * The generated string is stored in a descriptor map and returned.
     *
     * @param text The text to be used for creating a button locator string.
     * @return The generated button locator string.
     */
    @SuppressWarnings("StandardVariableNames")
    public static String button(final String text) {

        final String k = b(BUTTON, q(text));
        final String v = s(buttonWithText(text));
        logDebug(a(List.of(kv(K, k), kv(V, v)), COMMA_SPACE));
        Descriptors.put(k, v);
        return k;
    }

    /**
     * Generate a locator string based on the input text.
     *
     * @param s The input text
     * @return The generated locator string
     */
    private static String textWithText(final String s) {

        return (in(ANDROID)) ? s(__ANDROID_WIDGET, TEXT_VIEW, e(SQUARE, s(_AT_SGN, TEXT, _EQUAL_, q(s)))) : s(_SLASH_,
                _SLASH_, _ASTRS_, e(SQUARE, s(TEXT, e(ROUND, EMPTY), _EQUAL_, q(s))));
    }

    /**
     * Edits the given text and generates a formatted locator string with the text.
     * The generated string is conditioned based on the platform.
     *
     * @param s The input text to be edited and used for generating the locator string.
     * @return The generated locator string, formatted according to the platform.
     */
    private static String editWithText(final String s) {

        return (in(ANDROID)) ? s(__ANDROID_WIDGET, EDIT_TEXT, e(SQUARE, s(_AT_SGN, TEXT, _EQUAL_, q(s)))) : s(_SLASH_,
                _SLASH_, _ASTRS_, e(SQUARE, s(TEXT, e(ROUND, EMPTY), _EQUAL_, q(s))));
    }

    /**
     * Generates a view locator string with a description, formatted for the appropriate platform.
     *
     * @param s The description text to use for the view locator.
     * @return The generated view locator string, formatted based on the platform.
     */
    private static String viewWithDesc(final String s) {

        return (in(ANDROID)) ? s(__ANDROID_VIEW, c(VIEW), e(SQUARE, s(_AT_SGN, CONTENT_DESC, _EQUAL_, q(s)))) : s(_SLASH_,
                _SLASH_, _ASTRS_, e(SQUARE, s(TEXT, e(ROUND, EMPTY), _EQUAL_, q(s))));
    }

    /**
     * Generates a button locator string with the given text, formatted for the appropriate platform.
     *
     * @param s The text to use for the button locator.
     * @return The generated button locator string, formatted based on the platform.
     */
    private static String buttonWithText(final String s) {

        return (in(ANDROID)) ? s(__ANDROID_WIDGET, c(BUTTON), e(SQUARE, s(_AT_SGN, TEXT, _EQUAL_, q(s)))) : s(_SLASH_,
                _SLASH_, _ASTRS_, e(SQUARE, s(TEXT, e(ROUND, EMPTY), _EQUAL_, q(s))));
    }
}
