package works.lysenko.util.func.core;

import works.lysenko.util.data.records.TestPropertiesDescriptor;

import java.util.List;
import java.util.Properties;

import static works.lysenko.util.chrs.____.NAME;
import static works.lysenko.util.data.records.KeyValue.kv;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.data.strs.Vars.a;
import static works.lysenko.util.func.type.Objects.isNotNull;
import static works.lysenko.util.func.type.Properties.getPropertiesFromFile;
import static works.lysenko.util.lang.C.COMMA_SPACE;
import static works.lysenko.util.lang.word.D.DIRECTORY;
import static works.lysenko.util.lang.word.E.EXTENSION;
import static works.lysenko.util.lang.word.I.INCLUDE;
import static works.lysenko.util.prop.data.Delimeters.L1;
import static works.lysenko.util.spec.Layout.Parts.TEST_PROPERTIES_PARTS_EXTENSION;
import static works.lysenko.util.spec.Symbols._DOT_;
import static works.lysenko.util.spec.Symbols._LFD_;

/**
 * Represents a class for reading test properties from files.
 */
public record TestProperties() {

    private static final String include = s(_DOT_, INCLUDE);

    @SuppressWarnings({"AssignmentToMethodParameter", "ObjectAllocationInLoop", "CallToSuspiciousStringMethod",
            "CollectionDeclaredAsConcreteClass"})
    private static String loadIncluded(final String include, final String directory, String debug, final Properties target) {

        final String[] files = include.split(s(L1));
        for (final String file : files) {
            final Result result = readTestPropertiesFromFile(new TestPropertiesDescriptor(directory, file.trim(),
                    TEST_PROPERTIES_PARTS_EXTENSION));
            debug = s(debug, _LFD_, a(INCLUDE, result.debug()));
            target.putAll(result.properties());
        }
        return debug;
    }

    /**
     * Reads test properties from a file based on the provided location.
     *
     * @param location The descriptor of the test properties file
     * @return The combined properties from the file and its included files
     */
    @SuppressWarnings("MethodWithMultipleReturnPoints")
    public static Result readTestPropertiesFromFile(final TestPropertiesDescriptor location) {

        final String directory = location.directory();
        final String name = location.name();
        final String extension = location.extension();
        String debug = b(a(List.of(kv(DIRECTORY, directory), kv(NAME, name), kv(EXTENSION, extension)), COMMA_SPACE));
        final Properties source = getPropertiesFromFile(s(location.directory(), location.name(), location.extension()));
        if (!source.containsKey(include)) return new Result(source, debug);
        final String include = source.getProperty(TestProperties.include);
        source.remove(TestProperties.include);
        final Properties target = new Properties();
        if (isNotNull(include)) debug = loadIncluded(include, directory, debug, target);
        target.putAll(source); // Defined values have bigger priority then included ones
        return new Result(target, debug);
    }

    /**
     * Represents the result of reading test properties from a file.
     */
    @SuppressWarnings({"CollectionDeclaredAsConcreteClass", "PublicInnerClass"})
    public record Result(Properties properties, String debug) {

    }
}
