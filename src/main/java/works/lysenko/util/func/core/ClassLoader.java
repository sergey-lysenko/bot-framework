package works.lysenko.util.func.core;

import works.lysenko.util.apis.scenario._Scenario;
import works.lysenko.util.data.records.Descriptor;
import works.lysenko.util.func.data.Regexes;
import works.lysenko.util.spec.Symbols;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.JarURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Objects.isNull;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static works.lysenko.Base.core;
import static works.lysenko.Base.isDebug;
import static works.lysenko.Base.logEvent;
import static works.lysenko.util.chrs.__.*;
import static works.lysenko.util.chrs.___.JAR;
import static works.lysenko.util.chrs.___.NOT;
import static works.lysenko.util.chrs._____.THERE;
import static works.lysenko.util.data.enums.Brackets.ROUND;
import static works.lysenko.util.data.enums.Brackets.SQUARE;
import static works.lysenko.util.data.enums.Severity.S1;
import static works.lysenko.util.data.enums.Severity.S2;
import static works.lysenko.util.data.enums.Severity.S3;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.data.strs.Wrap.e;
import static works.lysenko.util.data.strs.Wrap.q;
import static works.lysenko.util.func.type.Objects.isNotNull;
import static works.lysenko.util.lang.D.DOT_CLASS;
import static works.lysenko.util.lang.word.C.CAUGHT;
import static works.lysenko.util.lang.word.C.CLASS;
import static works.lysenko.util.lang.word.C.CONSTRUCTOR;
import static works.lysenko.util.lang.word.F.FOUND;
import static works.lysenko.util.lang.word.I.INNER;
import static works.lysenko.util.lang.word.P.PACKAGE;
import static works.lysenko.util.lang.word.P.PUBLIC;
import static works.lysenko.util.lang.word.R.REDEFINED;
import static works.lysenko.util.lang.word.S.SCENARIOS;
import static works.lysenko.util.lang.word.S.SKIPPED;
import static works.lysenko.util.prop.tree.Notify.emptyPackage;
import static works.lysenko.util.spec.Numbers.FIVE;
import static works.lysenko.util.spec.Numbers.FOUR;
import static works.lysenko.util.spec.Numbers.THREE;
import static works.lysenko.util.spec.Symbols._COLON_;
import static works.lysenko.util.spec.Symbols._DOT_;
import static works.lysenko.util.spec.Symbols._SLASH_;

/**
 * The ClassLoader class provides methods for loading classes from packages and instantiating objects of a given class.
 */
@SuppressWarnings({"MethodWithMultipleReturnPoints", "CallToPrintStackTrace", "IOResourceOpenedButNotSafelyClosed"})
public record ClassLoader() {

    private static final Class<?>[] ZERO_LENGTH_ARRAY = {};

    /**
     * Find all classes within a specified package.
     *
     * @param inPackage the package name to search for classes
     * @param silent    a boolean flag indicating whether to suppress error messages
     * @return a set of Class objects representing the found classes
     */
    public static Set<Class<?>> findAll(final String inPackage, final boolean silent) {

        if (core.isInJar()) return getClassesSetFromJar(inPackage, silent);
        final InputStream inputStream;
        try {
            inputStream = getInputStreamFromPack(inPackage);
            if (isNotNull(inputStream)) return getClassesSetFromInputStream(inputStream, inPackage);
            else notFound(inPackage, 1, silent);
        } catch (final RuntimeException e) {
            e.printStackTrace();
            notFound(inPackage, 2, silent);
        }
        return null;
    }

    /**
     * Gets the class based on the given class name and package name.
     *
     * @param className   the name of the class
     * @param packageName the name of the package that contains the class
     * @return the Class object representing the class with the specified name and package, or null if the class is not found
     */
    private static Class<?> getClassFromNameAndPackage(final String className, final String packageName) {

        try {
            return Class.forName(s(packageName, s(_DOT_), className.substring(0, className.lastIndexOf(_DOT_))));
        } catch (final ClassNotFoundException e) {
            //
        }
        return null;
    }

    /**
     * Retrieves a set of scenarios represented by classes from an input stream.
     *
     * @param inputStream The input stream containing the scenario information.
     * @param pack        The package name of the scenarios.
     * @return A1 set of Class objects representing the scenarios from the input stream.
     */
    @SuppressWarnings("StandardVariableNames")
    private static Set<Class<?>> getClassesSetFromInputStream(final InputStream inputStream, final String pack) {

        final InputStreamReader inputReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
        final Stream<String> bufferedReader = new BufferedReader(inputReader).lines();
        final Stream<String> classStream = bufferedReader.filter(l -> l.endsWith(DOT_CLASS));
        final Stream<? extends Class<?>> mapStream = classStream.map(l -> getClassFromNameAndPackage(l, pack));
        return mapStream.collect(Collectors.toSet());
    }

    /**
     * Retrieves a set of scenarios represented by classes from a JAR file.
     *
     * @param fromPackage The package name of the scenarios.
     * @param silent      A1 boolean flag indicating whether to suppress error messages.
     * @return A1 set of Class objects representing the scenarios from the JAR file.
     */
    private static Set<Class<?>> getClassesSetFromJar(final String fromPackage, final boolean silent) {

        final Set<Class<?>> resources = tryToRead(ClassLoader.class, getDescriptor(fromPackage), silent);
        if (resources.isEmpty()) notFound(fromPackage, FIVE, silent);
        return resources;
    }

    /**
     * Retrieves the constructor of a given class with the specified parameter types.
     *
     * @param z The class to retrieve the constructor from.
     * @param t The parameter types of the constructor.
     * @return The constructor of the class with the specified parameter types, or {@code null} if no such constructor is
     * found.
     */
    @SuppressWarnings("SameParameterValue")
    private static Constructor<?> getConstructor(final Class<?> z, final Class<?>[] t) {

        try {
            return z.getConstructor(t);
        } catch (final NoSuchMethodException e) {
            final String[] tokens = z.getName().split(Regexes.dot);
            final String trueName = tokens[tokens.length - 1];
            if (trueName.contains(s(Symbols._DOLLAR)))
                logEvent(S3, b(c(INNER), CLASS, q(z.getName()), SKIPPED));
            else logEvent(S2, b(c(THERE), IS, NO, REDEFINED, CONSTRUCTOR, IN, q(z.getName()), OR, IT, IS, NOT, PUBLIC));
        }
        return null;
    }

    private static Descriptor getDescriptor(final String pack) {

        return new Descriptor(pack,
                s(pack.replaceAll(e(SQUARE, s(_DOT_)), s(_SLASH_)), _SLASH_),
                pack.split(e(SQUARE, s(_DOT_))).length);
    }

    /**
     * Retrieves an InputStream from a given package.
     *
     * @param pack the package to retrieve the InputStream from
     * @return an InputStream obtained from the specified package
     */
    private static InputStream getInputStreamFromPack(final String pack) {

        final String s0 = s(pack.replaceAll(e(SQUARE, s(_DOT_)), s(_SLASH_)), _SLASH_);
        final Class<ClassLoader> clazz = ClassLoader.class;
        final java.lang.ClassLoader classLoader = clazz.getClassLoader();
        return classLoader.getResourceAsStream(s0);
    }

    /**
     * Handles the instantiation exception by logging an event and printing the stack trace (if debug mode is enabled).
     *
     * @param ex     the exception that occurred during instantiation
     * @param silent a flag indicating whether to suppress error messages
     */
    private static void handleInstantiationException(final Exception ex, final boolean silent) {

        if (!silent) {
            logEvent(S1, b(ex.getClass().getName(), s(CAUGHT, _COLON_), ex.getMessage()));
            if (isDebug()) ex.printStackTrace();
        }
    }

    /**
     * Instantiates an object of the given class.
     *
     * @param <T>   the type of the object to be instantiated
     * @param clazz the class of the object to be instantiated
     * @return the instantiated object of type T
     * @throws RuntimeException if the class is null, or if there is no accessible default constructor for the class,
     *                          or if an error occurs while instantiating the object
     */
    @SuppressWarnings({"StaticMethodOnlyUsedInOneClass", "unchecked"})
    public static <T> T instantiate(final Class<T> clazz) {

        if (isNull(clazz)) {
            return null;
        }
        final Constructor<?> cons;
        try {
            cons = clazz.getConstructor();
        } catch (final NoSuchMethodException e) {
            throw new IllegalCallerException(e);
        }
        try {
            return (T) cons.newInstance();
        } catch (final InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Instantiates a scenario object of the given class.
     *
     * @param classType the class of the scenario object to be instantiated
     * @return the instantiated scenario object
     * @throws IllegalAccessException    if accessing the class or its constructor is not allowed
     * @throws InvocationTargetException if the constructor throws an exception while being invoked
     * @throws InstantiationException    if the class cannot be instantiated (e.g., it is abstract or an interface)
     */
    private static _Scenario instantiateScenario(final Class<?> classType)
            throws IllegalAccessException, InvocationTargetException, InstantiationException {

        final Constructor<?> constructor = getConstructor(classType, ZERO_LENGTH_ARRAY);
        if (isNull(constructor)) return null;
        return (_Scenario) constructor.newInstance((Object[]) ZERO_LENGTH_ARRAY);
    }

    /**
     * Instantiates a set of scenario objects represented by the given classes.
     *
     * @param classes a set of classes representing the scenarios to be instantiated
     * @param silent  a flag indicating whether to suppress error messages
     * @return a set of instantiated scenario objects
     */
    private static Set<_Scenario> instantiateScenarios(final Iterable<Class<?>> classes, final boolean silent) {

        final Set<_Scenario> scenarioInstances = new HashSet<>(0);

        if (isNotNull(classes)) {
            for (final Class<?> classType : classes) {
                try {
                    final _Scenario instance = instantiateScenario(classType);
                    scenarioInstances.add(instance);
                } catch (final InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException |
                               SecurityException ex) {
                    handleInstantiationException(ex, silent);
                }
            }
        }

        return scenarioInstances;
    }

    /**
     * Logs a not found event for a scenario pack at a specific index.
     *
     * @param pack   the scenario pack name
     * @param i      the index of the scenario pack
     * @param silent specifies if the event should be logged silently
     */
    private static void notFound(final String pack, final int i, final boolean silent) {

        if ((!silent) && emptyPackage)
            logEvent(S3, b(c(NO), SCENARIOS, FOUND, IN, q(pack), PACKAGE, e(ROUND, i)));
    }

    /**
     * Processes a JarEntry by checking its name and adding the corresponding Class object to a collection of resources.
     *
     * @param entries    the enumeration of JarEntries
     * @param descriptor the descriptor for the package to read from
     * @param resources  the collection to add the found classes to
     * @throws ClassNotFoundException if the class corresponding to the JarEntry is not found
     */
    private static void processJarEntry(final Enumeration<? extends JarEntry> entries, final Descriptor descriptor,
                                        final Collection<? super Class<?>> resources) throws ClassNotFoundException {

        final JarEntry entry = entries.nextElement();
        final String entryName = entry.getName();
        final int entryDepth = entryName.split(s(_SLASH_)).length;
        if ((entryDepth == (descriptor.depth() + 1)) && !entry.isDirectory() && entry.getName().startsWith(descriptor.path())) {
            final String classSimpleName = (entryName.split(s(_SLASH_))[descriptor.depth()]).replace(DOT_CLASS, EMPTY);
            final String fullName = s(descriptor.pack(), _DOT_, classSimpleName);
            resources.add(Class.forName(fullName));
        }
    }

    /**
     * Reads and instantiates a set of scenario objects based on the specified package name.
     *
     * @param packageName the name of the package to search for scenarios
     * @param silent      a flag indicating whether to suppress error messages
     * @return a set of instantiated scenario objects
     */
    public static Set<_Scenario> readFrom(final String packageName, final boolean silent) {

        return instantiateScenarios(findAll(packageName, silent), silent);
    }

    /**
     * Reads and processes a JAR file to find and add classes to a collection of resources.
     *
     * @param jarURL     the URL of the JAR file to read
     * @param descriptor the descriptor for the package to read from
     * @param resources  the collection to add the found classes to
     * @param silent     a flag indicating whether to suppress error messages
     * @throws IOException if an I/O error occurs while reading the JAR file
     */
    private static void readJarFile(final URL jarURL, final Descriptor descriptor,
                                    final Collection<? super Class<?>> resources, final boolean silent) throws IOException {

        final JarURLConnection jarConnection = (JarURLConnection) jarURL.openConnection();
        try (final JarFile jarFile = jarConnection.getJarFile()) {
            final Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                processJarEntry(entries, descriptor, resources);
            }
        } catch (final ClassNotFoundException e) {
            notFound(descriptor.pack(), THREE, silent);
        }
    }

    /**
     * Tries to read classes from a specified package.
     *
     * @param clazz      the class loader class to use for resource retrieval
     * @param descriptor the descriptor for the package
     * @param silent     a flag indicating whether to suppress error messages
     * @return a set of Class objects representing the found classes
     */
    @SuppressWarnings({"CallToSuspiciousStringMethod", "SameParameterValue"})
    private static Set<Class<?>> tryToRead(final Class<ClassLoader> clazz, final Descriptor descriptor, final boolean silent) {

        final Set<Class<?>> resources = new HashSet<>(0);
        try {
            final URL jarURL = clazz.getClassLoader().getResource(s(clazz.getName().replace(s(_DOT_), s(_SLASH_)), DOT_CLASS));
            if (isNotNull(jarURL) && JAR.equals(jarURL.getProtocol())) readJarFile(jarURL, descriptor, resources, silent);
        } catch (final IOException e) {
            notFound(descriptor.pack(), FOUR, silent);
        }
        return resources;
    }
}
