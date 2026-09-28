package works.lysenko.base.exec;

import org.apache.commons.lang3.StringUtils;
import works.lysenko.util.apis.data._DataStorage;
import works.lysenko.util.func.type.Collector;
import works.lysenko.util.prop.data.Persist;
import works.lysenko.util.prop.data.Snapshot;
import works.lysenko.util.spec.Level;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static works.lysenko.Base.exec;
import static works.lysenko.Base.log;
import static works.lysenko.Base.parameters;
import static works.lysenko.util.chrs.__.OF;
import static works.lysenko.util.chrs.__.TO;
import static works.lysenko.util.chrs.____.*;
import static works.lysenko.util.data.enums.Ansi.gray;
import static works.lysenko.util.data.enums.Brackets.ROUND;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Null.n;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.data.strs.Swap.sn;
import static works.lysenko.util.data.strs.Wrap.e;
import static works.lysenko.util.data.strs.Wrap.q;
import static works.lysenko.util.func.imgs.Screenshot.getSnapshotPath;
import static works.lysenko.util.func.type.Objects.isNotNull;
import static works.lysenko.util.lang.C.COMMA_SPACE;
import static works.lysenko.util.lang.D.DUE_TO;
import static works.lysenko.util.lang.U.UNABLE_TO_LOAD;
import static works.lysenko.util.lang.U.UNABLE_TO_WRITE;
import static works.lysenko.util.lang.word.P.PROPERTIES;
import static works.lysenko.util.lang.word.S.SNAPSHOT;
import static works.lysenko.util.lang.word.U.UNNAMED;
import static works.lysenko.util.spec.Layout.Directories.VAR_;
import static works.lysenko.util.spec.Layout.Parts._DATA_PROPERTIES;
import static works.lysenko.util.spec.Layout.Templates.DATA_SNAPSHOT_;
import static works.lysenko.util.spec.Symbols.*;

/**
 * @author Sergii Lysenko
 * <p>
 * Wrapper for Properties class. Adds search, sorting of elements,
 * and persisting of data to a file
 */
@SuppressWarnings({"ClassExtendsConcreteCollection", "CloneableClassInSecureContext",
        "SerializableDeserializableClassInSecureContext"})
public final class DataStorage extends Properties implements _DataStorage {

    private final String path;

    /**
     * The DataStorage class represents a data storage object. It extends the Properties class and provides methods for
     * loading and
     * storing data.
     */
    @SuppressWarnings({"OverlyBroadCatchBlock", "ThrowInsideCatchBlockWhichIgnoresCaughtException"})
    public DataStorage() {

        path = s(VAR_, parameters.getPool(), _DATA_PROPERTIES);
        if (Persist.data && new File(path).exists())
            try (final FileInputStream stream = new FileInputStream(path); final InputStreamReader reader =
                    new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                load(reader); // Load properties using InputStreamReader with UTF-8
            } catch (final IOException e) {
                throw new IllegalArgumentException(s(b(UNABLE_TO_LOAD, DATA, FROM), q(path)));
            }
    }

    private static String getChangeDescriptor(final String name, final String comments, final Object... values) {

        final StringBuilder descriptor = new StringBuilder(name);
        final Collection<String> list = new ArrayList<>(0);
        for (final Object o : values)
            list.add(q(sn(o)));
        if (0 < values.length) descriptor.append(e(ROUND, StringUtils.join(list, COMMA_SPACE)));
        descriptor.append(COMMA_SPACE);
        descriptor.append(comments);
        return s(descriptor);
    }

    /**
     * Writes a data snapshot to a file.
     *
     * @param name     the name of the snapshot (can be null)
     * @param comments any comments to include in the snapshot
     * @param fields   the data fields to include in the snapshot
     * @throws IllegalArgumentException if unable to write the snapshot to file
     */
    @SuppressWarnings("ThrowInsideCatchBlockWhichIgnoresCaughtException")
    public static void writeDataSnapshot(final String name, final String comments, final Object... fields) {

        if (Snapshot.allowed) {
            final String descriptor = getChangeDescriptor(name, comments, fields);
            final String contents = (Snapshot.full) ? exec.getDataSnapshot(descriptor) : StringUtils.EMPTY;
            final String finalName = n(s(_DASH_), name);
            final Path path = Path.of(getSnapshotPath(DATA_SNAPSHOT_, false, finalName, PROPERTIES));
            try {
                Files.writeString(path, contents);
                log(Level.none, gray(b(s(_UP_ARR), c(MADE), q(n(UNNAMED, name)), SNAPSHOT, OF, TEST, DATA, s(_BULLT_),
                        descriptor)), true);
            } catch (final IOException e) {
                throw new IllegalArgumentException(b(UNABLE_TO_WRITE, TEST, DATA, SNAPSHOT, TO, q(name)));
            }
        }
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public Set<Map.Entry<Object, Object>> entrySet() {

        final Set<Map.Entry<Object, Object>> set1 = super.entrySet();
        final Set<Map.Entry<Object, Object>> set2 = new LinkedHashSet<>(set1.size());
        final Iterator<Map.Entry<Object, Object>> iterator =
                set1.stream().sorted(Comparator.comparing(o -> o.getKey().toString())).iterator();
        while (iterator.hasNext()) set2.add(iterator.next());
        return set2;
    }

    public Set<Object> filterKeys(final Set<Object> keys, final Object value) {

        final Set<Object> keySubSet = new TreeSet<>();
        for (final Object key : keys)
            if (get(key).equals(value)) keySubSet.add(key);
        return keySubSet;
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public Set<Object> keySet() {

        return java.util.Collections.unmodifiableSet(new TreeSet<>(super.keySet()));
    }

    @SuppressWarnings({"SynchronizeOnThis", "MethodMayBeSynchronized"})
    @Override
    public Enumeration<Object> keys() {

        synchronized (this) {
            return Collections.enumeration(new TreeSet<>(super.keySet()));
        }
    }

    @SuppressWarnings("UseOfPropertiesAsHashtable") // setProrerty in inapplicable
    @Override
    public synchronized Object put(final Object key, final Object value) {

        final Object o = super.put(s(key), sn(value));
        store();
        return o;
    }

    @Override
    public synchronized Object remove(final Object key) {

        final Object o = super.remove(s(key));
        store();
        return o;
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    public String searchKeyByValue(final String value) {

        return (String) Collector.keys(this, value).findFirst().get();
    }

    @SuppressWarnings("MethodWithMultipleLoops")
    public Set<String> searchKeys(final String... substrings) {

        final Set<String> keySubSet = new TreeSet<>();
        for (final Object key : keySet()) {
            boolean pass = true;
            for (final String substring : substrings)
                pass = pass && ((String) key).contains(substring);
            if (pass) keySubSet.add((String) key);
        }
        return keySubSet;
    }

    public Set<String> searchKeysByValue(final String value) {

        final Set<String> set = new HashSet<>(1);
        final Stream<Object> stream = Collector.keys(this, value);
        final Set<Object> objectSet = stream.collect(Collectors.toSet());
        for (final Object o : objectSet)
            set.add((String) o);
        return set;
    }

    @Override
    public void store(final StringWriter stringWriter, final String comments) throws IOException {

        super.store(stringWriter, comments);
    }

    /**
     * Stores the data in a file if the 'PERSIST' property is set to true.
     * The file is created if it doesn't exist and parent directories are created if necessary.
     */
    @SuppressWarnings({"ThrowInsideCatchBlockWhichIgnoresCaughtException", "OverlyBroadCatchBlock",
            "ResultOfMethodCallIgnored"})
    private void store() {

        if (isNotNull(exec)) {
            if (Persist.data) {
                new File(path).getParentFile().mkdirs(); // Create parent directory
                try (final FileOutputStream stream = new FileOutputStream(path)) {
                    store(stream, path);
                } catch (final IOException e) {
                    throw new IllegalArgumentException(b(UNABLE_TO_WRITE, DATA, TO, q(path), DUE_TO, q(e.getMessage())));
                }
            }
        }
    }

    @Override
    public void store(final OutputStream out, final String comments) throws IOException {

        try (final BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8))) {
            for (final Map.Entry<Object, Object> entry : entrySet()) {
                writer.write(s(entry.getKey(), _EQUAL_, entry.getValue()));
                writer.newLine();
            }
        }
    }

}
