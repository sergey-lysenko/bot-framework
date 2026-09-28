package works.lysenko.tree;

import io.appium.java_client.HidesKeyboard;
import io.appium.java_client.InteractsWithApps;
import org.apache.commons.math3.fraction.Fraction;
import org.openqa.selenium.*;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.interactions.Actions;
import works.lysenko.Base;
import works.lysenko.util.Constants;
import works.lysenko.util.apis.common.*;
import works.lysenko.util.apis.exception.unchecked.BotRuntimeException;
import works.lysenko.util.data.enums.Severity;
import works.lysenko.util.data.records.KeyValue;
import works.lysenko.util.func.core.Stacktrace;
import works.lysenko.util.func.imgs.Painter;
import works.lysenko.util.func.type.Points;
import works.lysenko.util.func.type.Strings;
import works.lysenko.util.func.ui.Locators;
import works.lysenko.util.prop.core.Exceptions;
import works.lysenko.util.prop.core.Screenshot;
import works.lysenko.util.prop.core.Sleeping;
import works.lysenko.util.prop.core.Start;
import works.lysenko.util.prop.grid.Marker;
import works.lysenko.util.spec.Level;
import works.lysenko.util.spec.Numbers;

import java.awt.image.BufferedImage;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import static io.appium.java_client.AppiumBy.androidUIAutomator;
import static java.util.Objects.isNull;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.openqa.selenium.support.ui.ExpectedConditions.*;
import static works.lysenko.Base.*;
import static works.lysenko.base.exec.DataStorage.writeDataSnapshot;
import static works.lysenko.base.core.Routines.in;
import static works.lysenko.util.chrs.__.*;
import static works.lysenko.util.chrs.___.*;
import static works.lysenko.util.chrs.____.*;
import static works.lysenko.util.chrs._____.VALUE;
import static works.lysenko.util.data.enums.Ansi.*;
import static works.lysenko.util.data.enums.Brackets.ROUND;
import static works.lysenko.util.data.enums.Brackets.SQUARE;
import static works.lysenko.util.data.enums.Platform.ANDROID;
import static works.lysenko.util.data.enums.Platform.CHROME;
import static works.lysenko.util.data.enums.Platform.FIREFOX;
import static works.lysenko.util.data.enums.Severity.*;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Bind.d;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.data.strs.Swap.s1;
import static works.lysenko.util.data.strs.Swap.sn;
import static works.lysenko.util.data.strs.Time.t;
import static works.lysenko.util.data.strs.Vars.a;
import static works.lysenko.util.data.strs.Wrap.e;
import static works.lysenko.util.data.strs.Wrap.q;
import static works.lysenko.util.func.core.Assertions.fail;
import static works.lysenko.util.func.core.Stacktrace.getShort;
import static works.lysenko.util.func.data.Percents.percentString;
import static works.lysenko.util.func.imgs.BufferedImages.getBufferedImage;
import static works.lysenko.util.func.imgs.Painter.drawArea;
import static works.lysenko.util.func.imgs.Screenshot.writeScreenshot;
import static works.lysenko.util.func.type.Booleans.isTrue;
import static works.lysenko.util.func.type.Numbers.random;
import static works.lysenko.util.func.type.Objects.isNotNull;
import static works.lysenko.util.func.ui.Locators.*;
import static works.lysenko.util.func.ui.Scroll.swipeVertically;
import static works.lysenko.util.lang.C.CHECKING_VISIBILITY_OF;
import static works.lysenko.util.lang.C.CLICKING_ON;
import static works.lysenko.util.lang.C.COMMA_SPACE;
import static works.lysenko.util.lang.E.ELEMENT_IS_NULL;
import static works.lysenko.util.lang.M.MAXIMUM_RETRIES_AMOUNT___;
import static works.lysenko.util.lang.R.READING_TEXT;
import static works.lysenko.util.lang.R.READING_VALUE;
import static works.lysenko.util.lang.T.TEXT_IN;
import static works.lysenko.util.lang.T.TO_BE;
import static works.lysenko.util.lang.U.UNABLE_TO;
import static works.lysenko.util.lang.U.UNABLE_TO_OPEN;
import static works.lysenko.util.lang.U.USER_DIR;
import static works.lysenko.util.lang.W.*;
import static works.lysenko.util.lang.word.A.*;
import static works.lysenko.util.lang.word.B.BEFORE;
import static works.lysenko.util.lang.word.B.BETTER;
import static works.lysenko.util.lang.word.C.*;
import static works.lysenko.util.lang.word.D.DISABLED;
import static works.lysenko.util.lang.word.D.DURING;
import static works.lysenko.util.lang.word.E.ELEMENT;
import static works.lysenko.util.lang.word.E.ENABLED;
import static works.lysenko.util.lang.word.E.EXCEPTION;
import static works.lysenko.util.lang.word.F.FINDING;
import static works.lysenko.util.lang.word.F.FORWARD;
import static works.lysenko.util.lang.word.F.FOUND;
import static works.lysenko.util.lang.word.I.INTERRUPTED;
import static works.lysenko.util.lang.word.K.KEYS;
import static works.lysenko.util.lang.word.L.LOCATING;
import static works.lysenko.util.lang.word.L.LOCATION;
import static works.lysenko.util.lang.word.O.OFFSET;
import static works.lysenko.util.lang.word.O.OPENING;
import static works.lysenko.util.lang.word.O.OPTIONAL;
import static works.lysenko.util.lang.word.P.PERFORMED;
import static works.lysenko.util.lang.word.P.PIXELS;
import static works.lysenko.util.lang.word.R.*;
import static works.lysenko.util.lang.word.S.*;
import static works.lysenko.util.lang.word.T.*;
import static works.lysenko.util.lang.word.U.UNKNOWN;
import static works.lysenko.util.lang.word.U.UPLOADING;
import static works.lysenko.util.lang.word.V.VERIFYING;
import static works.lysenko.util.lang.word.W.WHETHER;
import static works.lysenko.util.lang.word.W.WHILE;
import static works.lysenko.util.lang.word.W.WORKAROUND;
import static works.lysenko.util.prop.core.Waits.implicit;
import static works.lysenko.util.spec.Code.*;
import static works.lysenko.util.spec.Layout.Paths._RESOURCES_;
import static works.lysenko.util.spec.Numbers.ZERO;
import static works.lysenko.util.spec.Symbols.*;
import static works.lysenko.util.spec.Waits.*;

/**
 * The Root class represents a base class for other classes in the application.
 * It contains various utility methods for interacting with elements on the user interface.
 */
@SuppressWarnings({"AbstractClassWithoutAbstractMethods", "OverlyComplexClass", "MethodWithMultipleReturnPoints", "NegativelyNamedBooleanVariable", "WeakerAccess", "unused", "DataFlowIssue"})
public abstract class Root implements ClearsWebElements, ClicksOnWebElements, ControlsExecution, DescribesWebElements,
        OperatesOnUrl, SendsKeys, ReadsWebElements, Scrolls, Swipes, Sleeps, SearchesWebElements, TypesIntoWebElements,
        WaitsForWebElements, WritesLog, Runs {

    /**
     * An instance of the DataStorage class.
     * Represents a data object that can be used to store, manipulate, or interact with specific
     * types of information required by the application.
     */
    public Data data = new Data();

    /**
     * Generates a UUID (Universally Unique Identifier) as a string.
     *
     * @return A randomly generated UUID represented as a string.
     */
    @SuppressWarnings("unused")
    protected static String generateUUID() {

        return s(UUID.randomUUID());
    }

    /**
     * Checks if the application is currently halted.
     *
     * @return true if the application is halted, false otherwise
     */
    static boolean isHalt() {

        return isNotNull(core.getDashboard()) && core.getDashboard().isHalt();
    }

    /**
     * Checks if the application is currently paused.
     *
     * @return true if the application is paused, false otherwise
     */
    @SuppressWarnings("WeakerAccess")
    public static boolean isPause() {

        return isNotNull(core.getDashboard()) && core.getDashboard().isPause();
    }

    /**
     * @param name  of object(s)
     * @param part  fraction
     * @param total amount
     * @return {name} {fraction}/{total} xx,xx%
     */
    public static String stats(final String name, final int part, final int total) {

        return b(name, s(part, _SLASH_, total), yb(percentString(part, total)));
    }

    /**
     * Navigates the web driver back to the previous page in the browser's history.
     * This method interacts with the web driver's navigation functionality
     * to simulate the browser's back button behaviour.
     */
    public static void back() {

        exec.wd().navigate().back();
    }

    /**
     * Navigates the web driver to the next page in the browser's session history,
     * if available.
     * <p>
     * This method utilises the web driver's navigation capabilities to move
     * forward in the browsing history. If there are no forward entries
     * in the session history, this method does nothing.
     * <p>
     * Note: Behaviour depends on the state of the browser's history.
     */
    public static void forward() {

        exec.wd().navigate().forward();
    }

    public final void clear(final boolean sendKeys, final String... locator) {

        final WebElement e = find(false, true, true, locator);
        clear(sendKeys, e);
    }

    public final void clear(final boolean sendKeys, final WebElement element) {

        log(b(c(CLEARING), describe(element)));
        logDebug(b(TEXT_IN, describe(element), b(BEFORE, s(CLEARING, _COLON_)), element.getAttribute(VALUE)));
        if (sendKeys) {
            element.sendKeys(Keys.CONTROL, s(A));
            element.sendKeys(Keys.BACK_SPACE);
        } else element.clear();
        logDebug(b(TEXT_IN, describe(element), b(AFTER, s(CLEARING, _COLON_)), element.getAttribute(VALUE)));
    }

    public final void clear(final String... locator) {

        final WebElement e = find(false, true, true, locator);
        clear(e);
    }

    public final void clear(final WebElement element) {

        clear(false, element);
    }

    public final void clickOn(final WebElement element) {

        clickOn(element, true);
    }

    public final void clickOn(final WebElement element, final long waitAfter) {

        clickOn(element, true);
        sleep(waitAfter);
    }

    public final void clickOn(final WebElement element, final boolean geometry) {

        clickOn(false, element);
    }

    public final void clickOn(final Fraction x1, final Fraction y1, final String... locator) {

        clickOn(x1.doubleValue(), y1.doubleValue(), locator);
    }

    public final void clickOn(final double x1, final double y1, final String... locator) {

        log(b(CLICKING_ON, Locators.describe(locator), s(_AT_SGN), s(OPN_BRK, X, x1, _COMMA_), s(Y, y1, CLS_BRK)));
        log(Level.supplementary, b(c(LOCATING), THE, ELEMENT, Locators.describe(locator)), true);
        final WebElement element = find(true, true, true, locator);
        final Rectangle re = isNull(element) ? null : element.getRect();
        if (isNull(re)) logEvent(S0, b(c(ELEMENT), LOCATION, UNKNOWN));
        else {
            log(Level.supplementary, b(s(c(ELEMENT), _QUOTE_, S), RECTANGLE, IS, Painter.describe(re)), true);
            final Point center = Points.getCenter(re);
            final Point p = Points.getPoint(x1, y1, re);
            final int offsetX = p.x - center.x;
            final int offsetY = p.y - center.y;
            log(Level.supplementary, b(c(CALCULATED), OFFSET, IN, PIXELS, FROM, CENTER, IS, s(OPN_BRK, X, offsetX, _COMMA_),
                    s(Y, offsetY, CLS_BRK)), true);
            final Actions actions = new Actions(exec.wd());
            actions.moveToElement(element);
            actions.moveByOffset(offsetX, offsetY);
            actions.click();
            actions.perform();
        }
    }

    public final void clickOn(final String locator, final long waitAfter) {

        clickOn(false, locator);
        sleep(waitAfter);
    }

    public final void clickOn(final String locator) {

        clickOn(false, locator);
    }

    @SuppressWarnings("ValueOfIncrementOrDecrementUsed")
    public final void clickOn(final boolean thenWaitForInvisibilityOf, final String locator) {

        boolean notDone = true;
        int attempt = 0;
        final int retries = Exceptions.retries;
        do try {
            final WebElement element = find(false, true, true, locator);
            log(b(CLICKING_ON, q(bb(Locators.describe(locator)))));
            if (isNull(element)) {
                logEvent(S2, ELEMENT_IS_NULL);
                return;
            } else {
                click0(element);
                notDone = false;
            }
        } catch (final StaleElementReferenceException ex) {
            logEvent(S2, b(CAUGHT, ex.getClass().getName(), WHILE_TRYING_TO_CLICK_, s(++attempt), DOTS));
        } while (notDone && retries >= attempt);
        if (thenWaitForInvisibilityOf) waitForInvisibilityOf(locator);
    }

    @SuppressWarnings("ValueOfIncrementOrDecrementUsed")
    @Override
    public final void clickOn(final boolean thenWaitForInvisibilityOf, final WebElement element) {

        boolean notDone = true;
        int attempt = 0;
        final int retries = Exceptions.retries;
        do try {
            log(b(CLICKING_ON, q(describe(element))));
            if (isNull(element)) {
                logEvent(S2, ELEMENT_IS_NULL);
                return;
            } else {
                click0(element);
                notDone = false;
            }
        } catch (final StaleElementReferenceException ex) {
            logEvent(S2, b(CAUGHT, ex.getClass().getName(), WHILE_TRYING_TO_CLICK_, s(++attempt), DOTS));
        } while (notDone && retries >= attempt);
        if (thenWaitForInvisibilityOf) waitForInvisibilityOf(element);
    }

    public final void clickOnText(final String text) {

        clickOn(false, text(text));
    }

    public final void clickOnText(final double x1, final double y1, final String text) {

        clickOn(x1, y1, text(text));
    }

    public final void clickOnText(final Fraction x1, final Fraction y1, final String text) {

        clickOn(x1, y1, text(text));
    }

    public final void clickOnDesc(final String contentDesc) {

        clickOn(false, desc(contentDesc));
    }

    public final void clickOnText(final boolean thenWaitForInvisibilityOf, final String text) {

        clickOn(thenWaitForInvisibilityOf, text(text));
    }

    public final void clickOnText(final String text, final long waitAfter) {

        clickOn(false, text(text));
        sleep(waitAfter);
    }

    public final String describe(final WebElement element) {

        return describe(element, true);
    }


    public final String describe(final WebElement element, final boolean geometry) {

        final String an = in(CHROME) ? element.getAccessibleName() : EMPTY;
        if (isNull(element)) return EMPTY;
        final String tg = element.getTagName();
        return s(((null == tg) ? ELEMENT : tg), (an.isEmpty() ? EMPTY : e(true, q(an))), (geometry ? s(e(s(_AT_SGN)),
                Painter.describe(element.getRect())) : EMPTY));
    }

    @SuppressWarnings({"MethodWithMultipleLoops", "OverlyComplexMethod", "ArrayLengthInLoopCondition",
            "ValueOfIncrementOrDecrementUsed"})
    public final WebElement find(final boolean silent, final boolean scrollTo, final boolean mandatory,
                                 final String... locators) {

        WebElement element = null;
        boolean notDone = true;
        int attempt = 0;
        final int retries = Exceptions.retries;
        do {
            try {
                if (!silent && 0 < locators.length) log(Level.supplementary, b(c(FINDING), q(bb(locators[0]))), true);
                if (0 == locators.length) element = exec.wd().findElement(by(s(_SLASH_, _SLASH_, BODY)));
                if (0 < locators.length) element = exec.wd().findElement(by(locators[0]));
                if (1 < locators.length) for (int i = 1; i < locators.length; i++) {
                    if (!silent) log(b(Constants._AND_CHILD, q(bb(locators[i])), s(_QUOTE_)));
                    if (isNotNull(element)) element = element.findElement(by(locators[i]));
                }
                notDone = false;
            } catch (final TimeoutException | NoSuchElementException | StaleElementReferenceException ex) {
                if (mandatory) throw ex;
                logEvent(S2, b(c(CAUGHT), q(ex.getClass().getName()), WHILE_TRYING_TO, s(FIND, e(ROUND, EMPTY), _COMMA_),
                        DURING, ATTEMPT, s(++attempt), DOTS));
                sleepShort();
            }
        } while (notDone && retries > attempt);
        if (retries == attempt) {
            logEvent(S1, MAXIMUM_RETRIES_AMOUNT___);
            return null;
        }
        return scrollTo ? find(element) : element;
    }

    public final WebElement find(final WebElement element) {

        if (in(FIREFOX)) ((JavascriptExecutor) exec.wd()).executeScript(JS_FIND_CODE, element);
        else {
            final Actions actions = new Actions(exec.wd());
            actions.moveToElement(element);
            actions.perform();
        }
        return element;
    }

    public final WebElement find(final String locator) {

        return find(false, true, true, locator);
    }

    public final List<WebElement> findAll(final String locator) {

        return findAll(locator, false);
    }

    public final List<WebElement> findAll(final String locator, final boolean expose) {

        return findAll(locator, expose, exec.wd().manage().timeouts().getImplicitWaitTimeout());
    }

    public final List<WebElement> findAll(final String locator, final Duration iwait) {

        return findAll(locator, false, exec.wd().manage().timeouts().getImplicitWaitTimeout());
    }

    public final List<WebElement> findAll(final String locator, final boolean expose, final Duration iwait) {

        final Duration defIwait = exec.wd().manage().timeouts().getImplicitWaitTimeout();
        if (!defIwait.equals(iwait)) exec.wd().manage().timeouts().implicitlyWait(iwait);
        final ArrayList<WebElement> elements;
        if (isNull(locator)) elements = new ArrayList<>(1);
        else elements = (ArrayList<WebElement>) exec.wd().findElements(by(locator, expose));
        final String result = (elements.isEmpty()) ? (rb(ZERO)) : (gb(elements.size()));
        log(Level.supplementary, b(c(FINDING), ALL, q(bb(locator)), s(GRT_THN), result, s(ELEMENT, s1(elements.size()))),
                true);
        if (!defIwait.equals(iwait)) exec.wd().manage().timeouts().implicitlyWait(defIwait);
        return elements;
    }

    public final String findFirstOfTexts(final String locator, final String textRegex) {

        return Strings.firstOf(findTexts(locator, textRegex));
    }

    public final String findFirstOfTexts(final String locator, final String textRegex, final int retries) {

        return Strings.firstOf(findTexts(locator, textRegex, retries));
    }

    public final WebElement findOrNull(final String locator) {

        final WebElement result;
        try {
            result = find(locator);
        } catch (final org.openqa.selenium.NoSuchElementException e) {
            return null;
        }
        return result;
    }

    public final WebElement findScrollToTextOrNull(final String text) {

        return findScrollToTextOrNull(text, null);
    }

    public final WebElement findScrollToTextOrNull(final String text, final Runnable code) {

        try {
            scrollToText(text, true, code);
        } catch (final org.openqa.selenium.NoSuchElementException e) {
            return null;
        }
        return findText(text);
    }

    public final WebElement findText(final String text) {

        return find(text(text));
    }

    public final List<String> findTexts(final String locator, final String textsRegex) {

        return findTexts(locator, textsRegex, Numbers.ONE);
    }

    @SuppressWarnings({"ValueOfIncrementOrDecrementUsed", "ObjectAllocationInLoop"})
    public final List<String> findTexts(final String locator, final String textsRegex, final int retries) {

        int attempts = retries;
        final Pattern pattern = Pattern.compile(textsRegex);

        List<String> result;
        do {
            if (attempts != retries) sleep(implicit);
            result =
                    findAll(locator).stream().map(candidate -> candidate.getAttribute(TEXT)).filter(attribute -> pattern.matcher(attribute).matches()) // Keep only strings with digits only
                            .toList();
        } while (ZERO < --attempts && result.isEmpty());
        log(0, b(s(FAT_BUL), q(yb(s(result)))), true);
        return result;
    }

    @SuppressWarnings({"MethodWithMultipleLoops", "ValueOfIncrementOrDecrementUsed"})
    public final void findThenClick(final String locator) {

        final List<WebElement> list = findAll(locator);
        WebElement target = null;
        boolean notDone = true;
        int attempt = 0;
        final int retries = Exceptions.retries;
        do try {
            for (final WebElement element : list)
                if (element.isDisplayed()) target = element;
            notDone = false;
        } catch (final TimeoutException ex) {
            logEvent(S2, b(c(CAUGHT), q(ex.getClass().getName()), WHILE_TRYING_TO, s(FIND, c(THEN), c(CLICK), e(ROUND,
                    EMPTY), _COMMA_), DURING, ATTEMPT, s(++attempt), DOTS));
        } while (notDone || retries < attempt);
        if (isNotNull(target)) target.click();
    }

    public final void getLog(final Object field, final Object o) {

        log(Level.debug, b(sn(s(c(D), _DOT_, GET), e(ROUND, bb(field)), e(s(RGT_DAR)), yb(o))), true);
    }

    @Override
    public final void hideKeyboard() {

        ((HidesKeyboard) exec.wd()).hideKeyboard();
    }

    public final void ifExistsClickOnText(final String text) {

        if (isPresent(text(text))) clickOnText(text);
    }

    public final boolean isDisabled(final String locator) {

        log(b(c(CHECKING), WHETHER, q(locator), IS, DISABLED));
        return !exec.wd().findElement(by(locator)).isEnabled();
    }

    public final boolean isEnabled(final String locator) {

        log(b(c(CHECKING), WHETHER, q(locator), IS, ENABLED));
        return !exec.wd().findElement(by(locator)).isEnabled();
    }

    public final int amountPresent(final String locator) {

        return findAll(locator).size();
    }

    public final boolean isPresent(final String locator) {

        return isPresent(locator, false);
    }

    public final boolean isPresent(final String locator, final Duration iwait) {

        final int oc = findAll(locator, iwait).size();
        return 0 < oc;
    }

    public final boolean isPresent(final String locator, final boolean expose) {

        final int oc = findAll(locator, expose).size();
        return 0 < oc;
    }

    public final boolean isPresent(final String locator, final boolean expose, final Duration iwait) {

        final int oc = findAll(locator, expose, iwait).size();
        return 0 < oc;
    }

    public final boolean isVisible(final String locator) {

        return isVisible(locator, false);
    }

    public final boolean isVisible(final String locator, final boolean silent) {

        if (!silent) log(b(CHECKING_VISIBILITY_OF, q(locator)));
        return !exec.wd().findElements(by(locator)).isEmpty();
    }

    public final void log(final String message) {

        core.getLogger().log(message);
    }

    public final void log(final int level, final String message, final boolean addStack) {

        log(level, message, addStack, false);
    }

    public final void log(final int level, final String message, final boolean addStack, final boolean forceUnfiltered) {

        boolean stack = addStack;
        if (isNotNull(exec)) stack = addStack && isDebug();
        final String stackText = getShort(Thread.currentThread().getStackTrace(), forceUnfiltered);
        final String stackTrace = gray(stackText);
        core.getLogger().log(level, (stack) ? b(message, stackTrace) : message);
    }

    public final void logDebug(final String message) {

        logDebug(message, true);
    }

    public final void logDebug(final String message, final boolean addStack) {

        logDebug(message, addStack, false);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public final void logDebug(final KeyValue... keys) {

        logDebug(a(List.of(keys), COMMA_SPACE));
    }

    public final void logDebug(final String message, final boolean addStack, final boolean forceUnfiltered) {

        if (isNull(exec) || isDebug()) log(Level.debug, message, addStack, forceUnfiltered);
    }

    public final void logEmptyLine() {

        logEmptyLine(false);
    }

    public final void logEmptyLine(final boolean addStack) {

        logEmptyLine(addStack, false);
    }

    public final void logEmptyLine(final boolean addStack, final boolean forceUnfiltered) {

        if (addStack) core.getLogger().log(ZERO, gray(Stacktrace.getShort(Thread.currentThread().getStackTrace())));
        else core.getLogger().logEmptyLine();
    }

    public final void logEvent(final Severity severity, final String message) {

        core.getLogger().logEvent(severity, message, isDebug() ?
                Stacktrace.getShort(Thread.currentThread().getStackTrace()) : EMPTY);
    }

    public final void logTrace(final String message) {

        logTrace(message, false);
    }

    public final void logTrace(final String message, final boolean addStack) {

        logTrace(message, addStack, false);
    }

    public final void logTrace(final String message, final boolean addStack, final boolean forceUnfiltered) {

        if (isNull(exec) || isTrace()) log(Level.debug, message, addStack, forceUnfiltered);
    }

    @SuppressWarnings({"ThrowInsideCatchBlockWhichIgnoresCaughtException", "HttpUrlsUsage"})
    public final void open(final String u) {

        URL url = null;
        try {
            url = new URL(u);
        } catch (final MalformedURLException e) {
            try {
                url = new URL("https://" + u);
            } catch (final MalformedURLException f1) {
                try {
                    url = new URL("http://" + u);
                } catch (final MalformedURLException f2) {
                    @SuppressWarnings("ConstantValue") final String urlString = (null == url) ? b(UNKNOWN, s(U, R, L)) :
                            q(url.toString());
                    throw new IllegalArgumentException(s(UNABLE_TO_OPEN, urlString));
                }
            }
        }
        log(b(c(OPENING), q(s(url))));
        exec.wd().get(url.toString());
    }

    @SuppressWarnings({"ThrowInsideCatchBlockWhichIgnoresCaughtException", "HardcodedFileSeparator"})
    public final void open(final String p, final String u) {

        URL url = null;
        try {
            url = new URL(u);
        } catch (final MalformedURLException e) {
            try {
                url = new URL(p + "://" + u);
            } catch (final MalformedURLException e1) {
                @SuppressWarnings("ConstantValue") final String urlString = (null == url) ? b(UNKNOWN, s(U, R, L)) :
                        q(url.toString());
                throw new IllegalArgumentException(s(UNABLE_TO_OPEN, urlString));
            }
        }
        log(b(OPENING, q(s(url))));
        if (isNotNull(getWebDriver())) getWebDriver().get(url.toString());
    }

    public final String read(final WebElement element) {

        return read(element, true);
    }

    public final String read(final WebElement element, final boolean geometry) {

        log(b(READING_TEXT, q(bb(describe(element, geometry)))));
        final String text = element.getText();
        logText(text);
        return text;
    }

    public final String read(final String locator) {

        log(b(READING_TEXT, q(bb(locator))));
        final String text = exec.wd().findElement(by(locator)).getText();
        logText(text);
        return text;
    }

    @SuppressWarnings("ValueOfIncrementOrDecrementUsed")
    public final String read(final String locator, final int retries) {

        int i = retries;
        log(b(READING_TEXT, q(bb(locator)), WITH, UP, TO, s(i), RETRIES));
        while (ZERO < i--) try {
            final String text = exec.wd().findElement(by(locator)).getText();
            log(Level.none, q(yb(text)), true);
            return text;
        } catch (final NoSuchElementException e) {
            if (ZERO == i) throw e;
            log(Level.none, b(yb(s(i)), RETRIES, LEFT), true);
            sleepShort();
        }
        return null;
    }

    public final String readInput(final String locator) {

        log(b(READING_VALUE, q(bb(locator))));
        final String text = exec.wd().findElement(by(locator)).getAttribute(VALUE);
        log(Level.none, q(yb(text)), true);
        return text;
    }

    public final String readInput(final WebElement element) {

        log(b(READING_VALUE, q(bb(describe(element)))));
        final String text = element.getAttribute(VALUE);
        log(Level.none, q(yb(text)), true);
        return text;
    }

    public final void restartApp(final long pause) {

        if (in(ANDROID)) {
            log(b(TERMINATING, Start.bundleId));
            ((InteractsWithApps) exec.wd()).terminateApp(Start.bundleId);
            try {
                log(b(ACTIVATING, Start.bundleId));
                sleep(pause);
                ((InteractsWithApps) exec.wd()).activateApp(Start.bundleId);
                sleep(pause); // TODO: [framework] investigate the reason of needing two attempts
                ((InteractsWithApps) exec.wd()).activateApp(Start.bundleId);
            } catch (final RuntimeException e) {
                //
            }
        } else logEvent(S3, b(s(RESTART, c(APP), e(ROUND, EMPTY)), ONLY, APPLICABLE, FOR, c(ANDROID), TESTING));
    }

    public final Boolean runForBoolean(final String title, final Supplier<Boolean> booleanSupplier) {

        title(title);
        return booleanSupplier.get();
    }

    public final Integer runForInteger(final String title, final Supplier<Integer> integerSupplier) {

        title(title);
        return integerSupplier.get();
    }

    @SuppressWarnings("ImplicitNumericConversion")
    public final WebElement scrollForward(final int steps) {
        // TODO: [framework] investigate reason of unpredictable scrolling behavior (or just fall back to swipes)
        if (in(ANDROID)) {
            log(b(SCROLLING, s1(steps, STEP), FORWARD));
            return exec.wd().findElement(androidUIAutomator(s(AUTOMATOR_CODE_PART1, _DOT_, SCROLL, c(FORWARD),
                    e(ROUND, steps))));
        } else {
            logEvent(S3, b(s(SCROLL, c(FORWARD), e(ROUND, EMPTY)), ONLY, APPLICABLE, FOR, c(ANDROID), TESTING));
            return null;
        }
    }

    public final void scrollIntoView(final WebElement element) {

        scrollIntoView(element, ZERO, ZERO);
    }

    public final void scrollIntoView(final WebElement element, final int dx, final int dy) {

        log(b(c(SCROLLING), describe(element), INTO, VIEW));
        if (isNotNull(getWebDriver())) ((JavascriptExecutor) getWebDriver()).executeScript(JS_FIND_CODE, element);
    }

    public final void scrollIntoViewAndClickOn(final String locator) {

        scrollIntoViewAndClickOn(locator, ZERO, ZERO);
    }

    public final void scrollIntoViewAndClickOn(final String locator, final int dx, final int dy) {

        final WebElement element = find(locator);
        scrollIntoView(element);
        scroll(dx, dy);
        //clickOn(element); TODO: figure out exception cause
    }

    public final void scrollIntoViewAndMakeScreenshot(final String locator) {

        scrollIntoViewAndMakeScreenshot(locator, ZERO, ZERO);
    }

    public final void scrollIntoViewAndMakeScreenshot(final String locator, final int dx, final int dy) {

        final WebElement element = find(locator);
        scrollIntoView(element);
        //makeScreenshot(element, locator); TODO: figure out exception cause
    }

    public final void scrollToText(final String text) {

        scrollToText(text, true);
    }

    public final void scrollToText(final String text, final boolean lazy) {

        scrollToText(text, lazy, null);
    }

    public final void scrollToText(final String text, final boolean lazy, final Runnable afterScroll) {

        if (isNotNull(text)) {
            if (in(ANDROID)) {
                if (lazy) if (alreadyOnScreen(text)) return;
                log(b(c(SCROLLING), TO, q(bb(text)), TEXT));
                exec.wd().findElement(androidUIAutomator(s(AUTOMATOR_CODE_PART1, AUTOMATOR_CODE_PART2, text,
                        AUTOMATOR_CODE_PART3)));
                if (isNotNull(afterScroll)) afterScroll.run();
                return;
            }
            logEvent(S3, b(s(SCROLL, c(TO), c(TEXT), e(ROUND, EMPTY)), ONLY, APPLICABLE, FOR, s(ANDROID), TESTING));
        }
    }

    public final void scrollToThenClickOn(final String text) {

        scrollToText(text, true);
        clickOnText(text);
    }

    public final void section(final String title) {

        title(title);
    }

    public final void section(final String title, final Runnable runnable) {

        title(title);
        runnable.run();
    }

    @SuppressWarnings("rawtypes")
    public final void section(final String title, final Callable callable) {

        title(title);
        try {
            callable.call();
        } catch (final java.lang.Exception e) {
            throw new BotRuntimeException(b(c(EXCEPTION), IN, c(CALLABLE)), e);
        }
    }

    public final void sendKeys(final CharSequence keys) {

        sendKeys(keys, 1);
    }

    @SuppressWarnings({"ObjectAllocationInLoop", "ValueOfIncrementOrDecrementUsed", "AssignmentToMethodParameter"})
    public final void sendKeys(final CharSequence keys, int i) {

        do new Actions(exec.wd()).sendKeys(keys).build().perform(); while (0 < --i);
    }

    public final void sendKeys(final String locator, final CharSequence keysToSend) {

        find(locator).sendKeys(keysToSend);
    }

    public final void sleep(final long duration) {

        sleep(duration, null);
    }

    public final void sleep(final long duration, final String message) {

        if (Sleeping.threshold > duration)
            logEvent(S2, b(SLEEPING, FOR, LESS, THAN, s(Sleeping.threshold, MS), IS, BETTER, TO_BE, PERFORMED, IN, SILENT,
                    MODE));
        sleep(duration, message, false);
    }

    @SuppressWarnings("ThrowInsideCatchBlockWhichIgnoresCaughtException")
    public final void sleep(final long duration, final String message, final boolean silent) {

        try {
            if (!silent) core.getLogger().log(null == message ? b(c(SLEEPING), yb(t(duration))) : message);
            Thread.sleep(duration);
        } catch (final InterruptedException e) {
            throw new IllegalStateException(b(c(INTERRUPTED), s(SLEEP, e(ROUND, EMPTY))));
        }
    }

    public final void sleep(final long duration, final boolean silent) {

        sleep(duration, null, silent);
    }

    public final void sleepLong() {

        sleepLong(1);
    }

    public final void sleepLong(final int times) {

        logDebug(b(c(SLEEPING), LONG, s(times), TIMES));
        sleep(random(times * LONG_WAIT_FROM, times * LONG_WAIT_TO), null);
    }

    public final void sleepMedium() {

        sleep(random(MID_WAIT_FROM, MID_WAIT_TO), null);
    }

    public final void sleepShort() {

        sleep(random(SHORT_WAIT_FROM, SHORT_WAIT_TO), null);
    }

    public final void stopTests() {

        log(b(c(STOPPING), TEST, SESSION));
        core.getTest().stop();
    }

    public final void swipeToText(final String text, final boolean lazy, final Runnable afterScroll, final float reduction) {

        if (isNotNull(text)) {
            if (in(ANDROID)) {
                if (lazy) if (alreadyOnScreen(text)) return;
                if (isDone(b(c(SWIPING), TO, q(text)), () -> swipeVertically(nthScrollView(1), reduction),
                        () -> isPresent(text(text)))) {
                    log(b(c(SWIPED), TO, q(bb(text)), TEXT));
                    if (isNotNull(afterScroll)) afterScroll.run();
                    return;
                } else fail(b(UNABLE_TO, SWIPE, TO, q(text)));
                return;
            }
            logEvent(S3, b(s(SWIPE, c(TO), c(TEXT), e(ROUND, EMPTY)), ONLY, APPLICABLE, FOR, c(ANDROID), TESTING));
        }
    }

    public final void swipeToText(final String text, final float reduction) {

        swipeToText(text, true, null, reduction);
    }

    public final void switchTo(final String window) {

        log(b(c(SWITCHING), TO, s(window), s(_QUOTE_)));
        exec.wd().switchTo().window(window);
    }

    @SuppressWarnings("ImplicitNumericConversion")
    public final boolean typeInto(final String locator, final Object content) {

        return typeInto(locator, content, ZERO, false);
    }

    public final boolean typeInto(final String locator, final Object content, final double pasteProbability,
                                  final boolean secret) {

        final WebElement e = find(locator);
        return typeInto(e, content, pasteProbability, secret);
    }

    public final boolean typeInto(final WebElement element, final Object content, final double pasteProbability,
                                  final boolean secret) {

        final CharSequence symbols = s(content);
        log(b(c(TYPING), q(bb(s(secret ? s(_BULLT_).repeat(symbols.length()) : content))), INTO, q(describe(element))));
        try {
            if (isTrue(pasteProbability)) {
                // simulation of pasting from clipboard by instant addition of all content
                element.sendKeys(symbols);
                return true;
            }
            final char[] chars = symbols.toString().toCharArray();
            for (final char ch : chars) {
                sleep(random(0, Sleeping.maxTypingDelay).longValue(), true); // silent sleep
                element.sendKeys(s(ch));
            }
            return false;
        } catch (final RuntimeException ex) {
            logEvent(S2, b(c(EXCEPTION), q(ex.getClass().getName()), b(CAUGHT, WHILE, TRYING, TO, TYPE), s(secret ?
                    s(_BULLT_).repeat(symbols.length()) : content), INTO, describe(element), b(s(_COMMA_), ATTEMPTING,
                    WORKAROUND, DOTS)));
            final Actions action = new Actions(exec.wd());
            action.moveToElement(element).click().sendKeys(s(content)).build().perform();
            return true;
        }
    }

    public final boolean typeInto(final String locator, final Object content, final boolean secret) {

        return typeInto(locator, content, 0.0, secret);
    }

    public final boolean typeInto(final String locator, final Object content, final double pasteProbability) {

        return typeInto(locator, content, pasteProbability, false);
    }

    public final boolean typeInto(final WebElement element, final Object content) {

        return typeInto(element, content, 0.0, false);
    }

    public final boolean typeInto(final WebElement element, final Object content, final boolean secret) {

        return typeInto(element, content, 0.0, secret);
    }

    public final boolean typeInto(final WebElement element, final Object content, final double pasteProbability) {

        return typeInto(element, content, pasteProbability, false);
    }

    @SuppressWarnings({"AccessOfSystemProperties", "DataFlowIssue"})
    public final void upload(final String uploader, final String name) {

        log(b(UPLOADING, q(name), THROUGH, q(uploader)));
        find(false, false, true, uploader).sendKeys(s(System.getProperty(USER_DIR), _SLASH_, _RESOURCES_, name));
    }

    public final void wait(final String locator) {

        waitForVisibilityOf(locator);
    }

    public final void wait(final String[] locator) {

        waitForVisibilityOf(locator);
    }

    public final void waitClickable(final String locator) {

        log(b(WAITING_FOR, CLICKABILITY, OF, q(locator)));
        exec.wdw().until(elementToBeClickable(by(locator)));
    }

    public final void waitFor(final String locator) {

        wait(locator);
    }

    public final void waitForInvisibilityOf(final String locator) {

        waitForInvisibilityOf(locator, exec.wd().manage().timeouts().getImplicitWaitTimeout());
    }

    public final boolean waitForInvisibilityOfOptional(final String locator) {

        try {
            waitForInvisibilityOf(locator, exec.wd().manage().timeouts().getImplicitWaitTimeout());
            return true;
        } catch (final org.openqa.selenium.TimeoutException e) {
            Base.logEvent(S1, e.toString());
            return false;
        }
    }

    public final void waitForInvisibilityOf(final WebElement element) {

        waitForInvisibilityOf(element, exec.wd().manage().timeouts().getImplicitWaitTimeout());
    }

    public final void waitForInvisibilityOf(final String locator, final Duration iwait) {

        final Duration defIwait = exec.wd().manage().timeouts().getImplicitWaitTimeout();
        if (!defIwait.equals(iwait)) exec.wd().manage().timeouts().implicitlyWait(iwait);
        log(b(WAITING_FOR_INVISIBILITY_OF, q(locator)));
        exec.wdw().until(invisibilityOfElementLocated(by(locator)));
        if (!defIwait.equals(iwait)) exec.wd().manage().timeouts().implicitlyWait(defIwait);
    }

    public final void waitForInvisibilityOf(final WebElement element, final Duration iwait) {

        final Duration defIwait = exec.wd().manage().timeouts().getImplicitWaitTimeout();
        if (!defIwait.equals(iwait)) exec.wd().manage().timeouts().implicitlyWait(iwait);
        log(b(WAITING_FOR_INVISIBILITY_OF, describe(element)));
        exec.wdw().until(invisibilityOf(element));
        if (!defIwait.equals(iwait)) exec.wd().manage().timeouts().implicitlyWait(defIwait);
    }

    public final void waitForInvisibilityOfText(final String text) {

        waitForInvisibilityOf(text(text), exec.wd().manage().timeouts().getImplicitWaitTimeout());
    }

    public final void waitForInvisibilityOfTexts(final String... texts) {

        for (final String text : texts) waitForInvisibilityOfText(text);
    }

    public final void waitForInvisibilityOfText(final String text, final Duration iwait) {

        waitForInvisibilityOf(text(text), iwait);
    }

    public final void waitForTexts(final String... texts) {

        for (final String text : texts) waitForText(text);
    }

    public final void waitForTexts(final Iterable<String> texts) {

        for (final String text : texts) waitForText(text);
    }

    public final void waitForText(final String text) {

        waitForVisibilityOfText(text);
    }

    public final void waitForEdit(final String text) {

        waitForVisibilityOfEdit(text);
    }

    public final void waitForDesc(final String contentDesc) {

        waitForVisibilityOfDesc(contentDesc);
    }

    public final void waitForVisibilityOf(final String locator) {

        log(b(WAITING_FOR_VISIBILITY_OF, q(bb(locator))));
        exec.wdw().until(visibilityOfElementLocated(by(locator)));
    }

    @SuppressWarnings("OverloadedVarargsMethod")
    public final void waitForVisibilityOf(final String... locators) {

        log(b(WAITING_FOR_VISIBILITY_OF, q(bb(Arrays.toString(locators)))));
        exec.wdw().until(visibilityOfElementLocated(by(locators)));
    }

    public final void waitForVisibilityOfText(final String text) {

        log(b(WAITING_FOR_VISIBILITY_OF, q(bb(text(text)))));
        exec.wdw().until(visibilityOfElementLocated(by(text(text))));
    }

    public final void waitForVisibilityOfEdit(final String text) {

        log(b(WAITING_FOR_VISIBILITY_OF, q(bb(edit(text)))));
        exec.wdw().until(visibilityOfElementLocated(by(edit(text))));
    }

    public final void waitForVisibilityOfDesc(final String text) {

        log(b(WAITING_FOR_VISIBILITY_OF, q(bb(desc(text)))));
        exec.wdw().until(visibilityOfElementLocated(by(desc(text))));
    }

    public final void waitSelected(final String locator) {

        log(b(WAITING_FOR, q(locator), b(TO_BE, SELECTED)));
        exec.wdw().until(visibilityOfElementLocated(by(locator)));
    }

    public final void waitThenClickOn(final double x1, final double y1, final String locator) {

        wait(locator);
        clickOn(x1, y1, locator);
    }

    public final void waitThenClickOn(final String locator) {

        wait(locator);
        clickOn(locator);
    }

    public final void waitThenClickOn(final boolean thenWaitForInvisibilityOf, final String locator) {

        wait(locator);
        clickOn(thenWaitForInvisibilityOf, locator);
    }

    public final void waitThenClickOnText(final String text) {

        waitForText(text);
        clickOnText(text);
    }

    public final WebElement waitThenFind(final String locator) {

        wait(locator);
        return find(locator);
    }

    public final void waitValue(final String locator, final String value) {

        log(s(s(WAITING_FOR, q(locator), b(TO, HAVE, VALUE), q(value))));
        exec.wdw().until(textToBe(by(locator), value));
    }

    public final String waitValueNot(final String locator, final String value) {

        log(b(WAITING_WHILE, q(locator), b(STILL, HAVE, VALUE), q(value)));
        exec.wdw().until(not(textToBe(by(locator), value)));
        return read(locator);
    }

    public final String waitValueNotEmpty(final String locator) {
        // TODO: [framework] this routine seems to be working not as expected
        log(s(WAITING_WHILE, q(locator), b(IS, STILL, EMPTY)));
        exec.wdw().until(not(textToBe(by(locator), EMPTY)));
        sleepShort();
        final String text = read(locator);
        log(b(c(THE), TEXT, OF, q(locator), IS, NOW, q(text)));
        return text;
    }

    @SuppressWarnings("BooleanMethodNameMustStartWithQuestion")
    private boolean alreadyOnScreen(final String text) {

        log(b(b(c(VERIFYING), THAT, TEXT), q(bb(text)), IS, NOT, ALREADY, s(ON, _DASH_, SCREEN)));
        if (isPresent(text(text))) {
            log(b(c(TEXT), q(bb(text)), IS, ALREADY, s(ON, _DASH_, SCREEN)));
            return true;
        }
        return false;
    }

    /**
     * Clicks on the given WebElement and takes a screenshot if the property "_screenshots.of.clicks" is set to true.
     *
     * @param element the WebElement to click on
     */
    private void click0(final WebElement element) {

        if (Screenshot.clicks) {
            final BufferedImage image = getBufferedImage();
            final BufferedImage img = drawArea(image, Marker.colour, element.getRect());
            writeScreenshot(img, false, b(CLICK, ON, describe(element)));
        }
        element.click();
    }

    /**
     * Logs the provided text with a specific formatting and logging level.
     *
     * @param text the text to be logged
     */
    private void logText(final String text) {

        log(Level.none, b(s(FAT_BUL), q(yb(text))), true);
    }

    @SuppressWarnings("HardCodedStringLiteral")
    private void scroll(final int dx, final int dy) {

        if (ZERO != dx || ZERO != dy) {
            log(b(c(SCROLLING), a(DX, dx, _COMMA_), a(DY, dy)));
            if (isNotNull(getWebDriver()))
                ((JavascriptExecutor) getWebDriver()).executeScript(s("window.scrollBy(", dx, ",", dy, ")"), "");
        }
    }

    /**
     * Logs the title.
     *
     * @param title the title
     */
    private void title(final String title) {

        exec.logEmptyLine();
        log(Level.none, bb(e(_EQUAL_, e(title))), true);
    }

    /**
     * The Data class provides methods to interact with a data storage system.
     * It supports querying, updating, and manipulating data entries such as keys, values,
     * and specific data types including integers, strings, and booleans.
     * <p>
     * This class ensures proper logging and debugging for its operations.
     * It also provides additional utility methods to validate the presence of keys
     * and search for keys or values within the data storage.
     */
    @SuppressWarnings("PublicInnerClass")
    public final class Data implements OperatesOnTestData {

        public boolean containsKey(final Object field) {

            return containsKey(field, false);
        }

        public boolean containsKey(final Object field, final boolean silent) {

            final boolean containsKey = exec.dataContainsKey(field);
            if (!silent)
                logDebug(b(s(c(D), _DOT_, CONTAINS, c(KEY), e(ROUND, bb(field)), e(s(RGT_DAR)), yb(containsKey))), true);
            return containsKey;
        }

        public boolean containsKeys(final Object... fields) {

            boolean containsKeys = true;
            for (final Object o : fields)
                if (!exec.dataContainsKey(o)) containsKeys = false;
            logDebug(b(s(CONTAINS, c(KEYS), e(ROUND, bb(Arrays.toString(fields))), e(s(RGT_DAR)), yb(containsKeys)),
                    gray(Stacktrace.getShort(Thread.currentThread().getStackTrace()))));
            return containsKeys;
        }

        public Set<Object> filterKeys(final Set<Object> keys, final Object value) {

            return exec.getDataStorage().filterKeys(keys, value);
        }

        public Object get(final Object field) {

            verifyPresence(field);
            final Object o = exec.dataGet(field);
            if (isDebug()) getLog(field, o);
            return o;
        }

        public Object get(final Object field, final Object def) {

            final Object o = exec.dataGetOrDefault(field, def);
            if (isDebug()) getLog(field, o);
            return o;
        }

        public Boolean getBoolean(final Object field, final Boolean def) {

            return (containsKey(field)) ? getBoolean(field) : def;
        }

        public Boolean getBoolean(final Object field) {

            verifyPresence(field);
            final Boolean o = Boolean.valueOf((String) exec.dataGet(field));
            if (isDebug()) getLog(field, o);
            return o;
        }

        public int getInteger(final Object field, final int def) {

            return (containsKey(field, true)) ? getInteger(field) : def;
        }

        public int getInteger(final Object field) {

            return getInteger(field, false);
        }

        public int getInteger(final Object field, final boolean silent) {

            verifyPresence(field);
            final int o = Integer.parseInt((String) exec.dataGet(field));
            if (isDebug() && !silent) getLog(field, o);
            return o;
        }

        public long getLong(final Object field, final long def) {

            return (containsKey(field, true)) ? getLong(field) : def;
        }

        public long getLong(final Object field) {

            return getLong(field, false);
        }

        public long getLong(final Object field, final boolean silent) {

            verifyPresence(field);
            final long o = Long.parseLong((String) exec.dataGet(field));
            if (isDebug() && !silent) getLog(field, o);
            return o;
        }

        public String getString(final Object field) {

            return getString(field, false);
        }

        @Override
        public String getString(final Object field, final boolean silent) {

            verifyPresence(field);
            final String o = (String) exec.dataGet(field);
            if (isDebug() && !silent) getLog(field, o);
            return o;
        }

        public String getString(final Object field, final String def) {

            return getString(field, def, false);
        }

        public String getString(final Object field, final String def, final boolean silent) {

            final String o = (String) exec.dataGetOrDefault(field, def);
            if (isDebug() && !silent) getLog(field, o);
            return o;
        }

        @SuppressWarnings("CallToSuspiciousStringMethod")
        public void put(final Object field, final Object value) {

            final Object o = exec.dataPut(field, value);
            if (isDebug()) {
                final String info = ((null == o) || (null == value) || !o.toString().equals(value.toString())) ? s(gb(value)
                        , RHT_ARR, rc(o)) : yb(o);
                log(Level.debug, b(sn(c(D), _DOT_, PUT, e(ROUND, bb(field)), e(s(RGT_DAR)), e(SQUARE, info))), true);
                writeDataSnapshot(PUT, Stacktrace.getShort(Thread.currentThread().getStackTrace()), field, value);
            }
        }

        public void remove(final Object field) {

            remove(field, false);
        }

        public void remove(final Object field, final boolean nonOptional) {

            if (nonOptional) verifyPresence(field);
            logDebug(b(s(REMOVE, e(ROUND, rc(field)))), true);
            if (exec.dataRemove(field))
                writeDataSnapshot(REMOVE, Stacktrace.getShort(Thread.currentThread().getStackTrace()), field);
        }

        public boolean removeValue(final Object value) {

            final boolean result;
            result = exec.dataRemoveValue(value);
            if (result) logDebug(b(s(REMOVE, c(VALUE), e(ROUND, rc(value)))), true);
            else logDebug(b(VALUE, q(s(value)), WAS, NOT, REMOVED));
            writeDataSnapshot(s(REMOVE, c(VALUE)), Stacktrace.getShort(Thread.currentThread().getStackTrace()), s(value));
            return result;
        }

        public String searchKeyByValue(final String value) {

            return exec.getDataStorage().searchKeyByValue(value);
        }

        public Set<String> searchKeys(final String... substrings) {

            return exec.getDataStorage().searchKeys(substrings);
        }

        public Set<String> searchKeysByValue(final String value) {

            return exec.getDataStorage().searchKeysByValue(value);
        }

        public void tick(final String user, final String name) {

            tick(name);
            tick(d(user, name));
        }

        /**
         * Increments the integer value associated with a specific key in the data storage by 1.
         * If the key does not exist, initialise it with a value of 0 before incrementing.
         *
         * @param name the name of the key to be incremented
         */
        private void tick(final String name) {

            put(s(_DASH_, name), getInteger(s(_DASH_, name), 0) + 1);
        }

        /**
         * Verifies the presence of a given field in the data storage and fails execution in case of its absence.
         *
         * @param field the field to verify the presence of
         */
        private void verifyPresence(final Object field) {

            if (!containsKey(field, true)) fail(b(d(c(s(N, ON)), OPTIONAL), KEY, q(s(field)), VALUE, NOT, FOUND));
        }
    }
}