package works.lysenko.base;

import io.appium.java_client.service.local.AppiumDriverLocalService;
import org.apache.commons.math3.fraction.Fraction;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.support.ui.WebDriverWait;
import works.lysenko.base.exec.*;
import works.lysenko.tree.Root;
import works.lysenko.util.apis._PropEnum;
import works.lysenko.util.apis.data._DataStorage;
import works.lysenko.util.apis.execution.*;
import works.lysenko.util.apis.test._Test;
import works.lysenko.util.prop.logs.Debug;
import works.lysenko.util.spec.Level;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Map;

import static java.util.Objects.isNull;
import static works.lysenko.Base.core;
import static works.lysenko.util.chrs.__.UI;
import static works.lysenko.util.chrs.___.DOTS;
import static works.lysenko.util.lang.word.S.SESSION;
import static works.lysenko.util.chrs.___.JAR;
import static works.lysenko.util.chrs.____.*;
import static works.lysenko.util.data.enums.Ansi.MAGENTA_BOLD_BRIGHT;
import static works.lysenko.util.data.enums.Ansi.ansi;
import static works.lysenko.util.data.enums.Severity.S0;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Bind.d;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Null.n;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.data.strs.Wrap.q;
import static works.lysenko.util.func.core.Assertions.еггог;
import static works.lysenko.util.func.type.Objects.isNotNull;
import static works.lysenko.util.grid.validation.BackgroundValidator.bv;
import static works.lysenko.util.lang.D.DUE_TO;
import static works.lysenko.util.lang.W.WEB_DRIVER;
import static works.lysenko.util.lang.word.C.CREATING;
import static works.lysenko.util.lang.word.E.EXECUTION;
import static works.lysenko.util.lang.word.I.INACCESSIBLE;
import static works.lysenko.util.lang.word.R.READING;
import static works.lysenko.util.lang.word.R.RUNNING;
import static works.lysenko.util.lang.word.S.*;

/**
 * This class represents single bot execution information
 *
 * @author Sergii Lysenko
 */
@SuppressWarnings({"AssignmentToStaticFieldFromInstanceMethod", "MethodWithMultipleReturnPoints",
        "CallToPrintStackTrace"})
public final class Exec extends Root implements _TestData, _Executes {

    private Boolean isDebug = null;

    private _DataStorage dataStorage = null;
    private _DataStorage sessionStorage = null;
    private _Scaler scaler = null;
    private _Driver driver = null;
    private _Issues issues = null;
    private _Scenarios scenarios = null;


    /**
     * Constructs a new instance of the Exec class.
     * Initialises the core object with the specified tests, additional parameters,
     * and parameters list. Logs the start of the execution process and initialises a bot.
     *
     * @param test           The _Test object used for execution.
     * @param additional     A1 collection of additional property enums extending _PropEnum.
     * @param parametersList A1 string containing the list of parameters for execution.
     */
    @SuppressWarnings("WeakerAccess")
    public Exec(final _Test test, final Collection<Class<? extends _PropEnum>> additional, final String parametersList) {

        core = createCore(test, additional, parametersList);
        logEmptyLine(true, true);
        createBot();
        log(Level.none, (b(c(TEST), c(EXECUTION), STARTED)), true);
    }

    /**
     * Creates a new instance of the Core class with the specified Test and parameters list.
     *
     * @param test           The Test object.
     * @param parametersList The additional parameters list.
     * @return A1 new instance of the Core class.
     */
    private static Core createCore(final _Test test, final Collection<Class<? extends _PropEnum>> additional,
                                   final String parametersList) {

        return new Core(test, additional, parametersList);
    }

    public boolean createDriver() {

        driver = new Driver();
        return driver.isOk();
    }

    /**
     * Creates a Scaler instance and performs the necessary initialisations.
     * This method logs the process of creating the Scaler, initialises a Scaler object,
     * and verifies the background with the created Scaler.
     */
    public boolean createScaler() {

        log(Level.none, b(c(CREATING), c(UI), c(SCALER), DOTS), true);
        scaler = new Scaler();
        if (scaler.isOk()) {
            final String name = d(s(scaler.getCurrent().getNumerator()), s(scaler.getCurrent().getDenominator()));
            bv("ROOT", name, null, scaler.getImage(), S0);
        }
        return scaler.isOk();
    }

    public boolean dataContainsKey(final Object field) {

        return dataStorage.containsKey(field);
    }

    public Object dataGet(final Object field) {

        return dataStorage.get(field);
    }

    public Object dataGetOrDefault(final Object field, final Object def) {

        return dataStorage.getOrDefault(field, def);
    }

    public Object dataPut(final Object field, final Object value) {

        return dataStorage.put(field, value);
    }

    public boolean dataRemove(final Object field) {

        final Object before = dataStorage.remove(field);
        return (isNotNull(before) && null == dataStorage.get(field));
    }

    public boolean dataRemoveValue(final Object value) {

        final Object keyToDelete;
        for (final Map.Entry<Object, Object> entry : dataStorage.entrySet())
            if (entry.getValue().equals(value)) {
                keyToDelete = entry.getKey();
                dataStorage.remove(keyToDelete);
                return true;
            }
        return false;
    }

    public Fraction getCurrentScale() {

        return null != scaler.getCurrent() ? scaler.getCurrent() : Fraction.ZERO;
    }

    public String getDataSnapshot(final String comments) {

        String s = null;
        try (final StringWriter stringWriter = new StringWriter()) {
            dataStorage.store(stringWriter, comments);
            s = stringWriter.toString();
        } catch (final IOException e) {
            e.printStackTrace();
        }
        return s;
    }

    public _DataStorage getDataStorage() {

        return dataStorage;
    }

    public _DataStorage getSessionStorage() {

        return sessionStorage;
    }

    /**
     * Retrieves the Scaler instance.
     *
     * @return The Scaler object.
     */
    @SuppressWarnings("PublicMethodNotExposedInInterface")
    public _Scaler getScaler() {

        return scaler;
    }

    public String getStatus() {

        return core.getStatus();
    }

    /**
     * Retrieves the WebDriver instance associated with the current execution context.
     * Attempts to access and manage logs of the WebDriver if it is not null.
     * In case of an exception, logs an error message indicating the inaccessibility of the WebDriver.
     *
     * @return The current WebDriver instance if it is accessible; otherwise, null.
     */
    public WebDriver getWebDriver() {
        return wd();
    }

    public WebDriverWait getWebDriverWait() {
        return wdw();
    }

    public void clearDriver() {

        if (isNotNull(driver)) {
            driver.wd(null);
            driver.wdw((WebDriverWait) null);
        }
    }

    public WebDriver wd() {

        try {
            if (isNotNull(driver))
                if (isNotNull(driver.wd())) {
                    driver.wd().manage().logs().getAvailableLogTypes();
                    return driver.wd();
                }
        } catch (final WebDriverException e) {
            еггог(b(WEB_DRIVER, INACCESSIBLE, DUE_TO, q(e.getMessage())));
            clearDriver();
        }
        return null;
    }

    /**
     * Retrieves the WebDriverWait instance associated with the driver
     * or returns null if the driver is not initialised.
     *
     * @return The WebDriverWait instance if the driver is not null; otherwise, null.
     */
    public WebDriverWait wdw() {

        return isNotNull(driver) ? driver.wdw() : null;
    }

    public WebDriverWait wdw(final java.time.Duration timeout) {

        return isNotNull(driver) ? driver.wdw(timeout) : null;
    }

    /**
     * @return debug state
     */
    public boolean isDebug() {

        if (isNotNull(core.getDashboard())) isDebug = core.getDashboard().isDebug();
        if (isNull(isDebug)) isDebug = Debug.debug;
        return n(false, isDebug);
    }

    /**
     * @return trace state
     */
    public boolean isTrace() {

        return Debug.trace;
    }

    /**
     * Retrieves the AppiumDriverLocalService instance associated with the current driver
     * or returns null if the driver is not initialised.
     *
     * @return The AppiumDriverLocalService instance if the driver is not null; otherwise, null.
     */
    public AppiumDriverLocalService service() {

        return isNotNull(driver) ? driver.service() : null;
    }

    /**
     * Creates a bot.
     */
    private void createBot() {

        if (core.isInJar()) log(Level.none, ansi(b(c(RUNNING), FROM, JAR, FILE), MAGENTA_BOLD_BRIGHT), false);

        issues = new Issues();

        log(Level.none, b(c(CREATING), c(TEST), c(DATA), c(STORAGE), DOTS), true);
        dataStorage = new DataStorage();

        log(Level.none, b(c(CREATING), c(SESSION), c(DATA), c(STORAGE), DOTS), true);
        sessionStorage = new DataStorage(false);

        log(Level.none, b(c(CREATING), c(TEST), c(SCENARIOS), c(STORAGE), DOTS), true);
        scenarios = new Scenarios();
    }

    public _Issues issues() {

        return issues;
    }

    public _Scenarios scenarios() {

        return scenarios;
    }
}
