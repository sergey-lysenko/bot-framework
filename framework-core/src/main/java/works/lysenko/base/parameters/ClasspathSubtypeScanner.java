package works.lysenko.base.parameters;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.classfile.constantpool.ClassEntry;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

import static works.lysenko.util.data.strs.Swap.s;

/// Bytecode-only classpath subtype scanner built on the Java 24/25 Class-File API (`java.lang.classfile`).
///
/// Discovers direct and transitive subtypes of one or more target classes/interfaces across
/// classpath directories and JAR archives without loading or initializing non-matching classes,
/// eliminating the need for third-party reflection scanners (`org.reflections` / `javassist`).
final class ClasspathSubtypeScanner {

    private static final String CLASS_SUFFIX = ".class";
    private static final String OBJECT_INTERNAL_NAME = "java/lang/Object";

    /// Third-party JAR package prefixes skipped when scanning unrestricted classpath archives.
    private static final List<String> SKIPPED_JAR_PREFIXES = List.of(
            "java/", "javax/", "jdk/", "sun/", "com/sun/",
            "org/openqa/", "io/appium/", "io/netty/", "io/opentelemetry/",
            "com/google/", "org/apache/", "org/bouncycastle/", "com/fasterxml/",
            "org/junit/", "org/opentest4j/", "org/yaml/", "okhttp3/", "okio/",
            "kotlin/", "kotlinx/", "guru/nidi/", "com/github/", "net/bytebuddy/",
            "org/slf4j/", "org/aspectj/", "org/jgrapht/", "org/jheaps/",
            "org/apfloat/", "net/arnx/", "com/sun/jna/", "META-INF/");

    private ClasspathSubtypeScanner() {
    }

    /// Finds all subtypes of `targetType` on the classpath under `packagePrefixes`.
    ///
    /// When `scanAllDirectoryClasses` is `true`, every `.class` file inside classpath directories
    /// (and `target/classes`) is inspected regardless of package prefix, while JAR archives are
    /// filtered to `packagePrefixes` (or non-third-party packages if `packagePrefixes` is empty).
    @SuppressWarnings("unchecked")
    static <T> Set<Class<? extends T>> findSubTypesOf(
            final Class<T> targetType,
            final Collection<String> packagePrefixes,
            final boolean scanAllDirectoryClasses) {

        final Map<Class<?>, Set<Class<?>>> results = findSubTypesOf(
                List.of(targetType), packagePrefixes, scanAllDirectoryClasses);
        final Set<Class<?>> raw = results.getOrDefault(targetType, Set.of());
        final Set<Class<? extends T>> typed = new LinkedHashSet<>(raw.size());
        for (final Class<?> cls : raw) {
            if (targetType.isAssignableFrom(cls) && cls != targetType) {
                typed.add((Class<? extends T>) cls);
            }
        }
        return typed;
    }

    /// Finds all subtypes of each target type in a single classpath pass.
    static Map<Class<?>, Set<Class<?>>> findSubTypesOf(
            final Collection<Class<?>> targetTypes,
            final Collection<String> packagePrefixes,
            final boolean scanAllDirectoryClasses) {

        final ClassLoader classLoader = resolveClassLoader();
        final List<String> internalPrefixes = normalizePrefixes(packagePrefixes);
        final Set<Path> roots = collectClasspathRoots(classLoader, internalPrefixes, scanAllDirectoryClasses);

        final Map<String, List<String>> parentsByClass = new HashMap<>();
        final List<String> candidates = new ArrayList<>();
        final ClassFile classFile = ClassFile.of();

        for (final Path root : roots) {
            if (Files.isDirectory(root)) {
                scanDirectory(root, internalPrefixes, scanAllDirectoryClasses, classFile, parentsByClass, candidates);
            } else if (Files.isRegularFile(root) && root.getFileName().toString().endsWith(".jar")) {
                scanJar(root, internalPrefixes, classFile, parentsByClass, candidates);
            }
        }

        final Map<Class<?>, Set<Class<?>>> results = new HashMap<>();
        for (final Class<?> target : targetTypes) {
            final String targetInternal = target.getName().replace('.', '/');
            final Map<String, Boolean> memo = new HashMap<>();
            memo.put(targetInternal, Boolean.TRUE);
            memo.put(OBJECT_INTERNAL_NAME, Boolean.FALSE);

            final Set<Class<?>> matched = new LinkedHashSet<>();
            for (final String candidate : candidates) {
                if (candidate.equals(targetInternal)) continue;
                if (!internalPrefixes.isEmpty() && !scanAllDirectoryClasses && !matchesAnyPrefix(candidate, internalPrefixes)) {
                    continue;
                }
                if (isSubtype(candidate, targetInternal, classFile, classLoader, parentsByClass, memo)) {
                    final Class<?> loaded = loadUnchecked(candidate, classLoader);
                    if (null != loaded && target.isAssignableFrom(loaded) && loaded != target) {
                        matched.add(loaded);
                    }
                }
            }
            results.put(target, matched);
        }
        return results;
    }

    private static ClassLoader resolveClassLoader() {
        final ClassLoader ctx = Thread.currentThread().getContextClassLoader();
        return (null != ctx) ? ctx : ClasspathSubtypeScanner.class.getClassLoader();
    }

    private static List<String> normalizePrefixes(final Collection<String> packagePrefixes) {
        if (null == packagePrefixes || packagePrefixes.isEmpty()) return List.of();
        final Set<String> normalized = new LinkedHashSet<>();
        for (final String pkg : packagePrefixes) {
            if (null == pkg || pkg.isBlank() || ".".equals(pkg)) continue;
            final String slash = pkg.trim().replace('.', '/');
            normalized.add(slash.endsWith("/") ? slash : s(slash, "/"));
        }
        return List.copyOf(normalized);
    }

    private static Set<Path> collectClasspathRoots(
            final ClassLoader classLoader,
            final List<String> internalPrefixes,
            final boolean includeWorkingDirClasses) {

        final Set<Path> roots = new LinkedHashSet<>();

        // 1. Package resource roots visible to the active ClassLoader
        for (final String prefix : internalPrefixes) {
            final String resourcePath = prefix.endsWith("/") ? prefix.substring(0, prefix.length() - 1) : prefix;
            try {
                final Enumeration<URL> urls = classLoader.getResources(resourcePath);
                while (urls.hasMoreElements()) {
                    final Path root = rootFromResourceUrl(urls.nextElement(), resourcePath);
                    if (null != root) roots.add(root);
                }
            } catch (final IOException _) {
            }
        }

        // 2. URLClassLoader entries (if any)
        for (ClassLoader cl = classLoader; null != cl; cl = cl.getParent()) {
            if (cl instanceof URLClassLoader ucl) {
                for (final URL url : ucl.getURLs()) {
                    final Path path = toFilePath(url);
                    if (null != path) roots.add(path);
                }
            }
        }

        // 3. Standard java.class.path entries
        final String classpath = System.getProperty("java.class.path", "");
        if (!classpath.isBlank()) {
            for (final String entry : classpath.split(File.pathSeparator)) {
                if (entry.isBlank()) continue;
                try {
                    final Path p = Path.of(entry).toAbsolutePath().normalize();
                    if (Files.exists(p)) roots.add(p);
                } catch (final Exception _) {
                }
            }
        }

        // 4. Current working directory target/classes (for IDE / Maven runs)
        if (includeWorkingDirClasses) {
            try {
                final Path targetClasses = Path.of(System.getProperty("user.dir", "."), "target", "classes")
                        .toAbsolutePath().normalize();
                if (Files.isDirectory(targetClasses)) roots.add(targetClasses);
            } catch (final Exception _) {
            }
        }

        return roots;
    }

    private static Path rootFromResourceUrl(final URL url, final String resourcePath) {
        try {
            final String protocol = url.getProtocol();
            if ("file".equals(protocol)) {
                Path dir = Path.of(url.toURI()).toAbsolutePath().normalize();
                final String normalizedRes = resourcePath.replace('\\', '/');
                final int segments = normalizedRes.isEmpty() ? 0 : normalizedRes.split("/").length;
                for (int i = 0; i < segments && null != dir.getParent(); i++) {
                    dir = dir.getParent();
                }
                return dir;
            }
            if ("jar".equals(protocol)) {
                final JarURLConnection conn = (JarURLConnection) url.openConnection();
                return toFilePath(conn.getJarFileURL());
            }
        } catch (final Exception _) {
        }
        return null;
    }

    private static Path toFilePath(final URL url) {
        if (null == url || !"file".equals(url.getProtocol())) return null;
        try {
            final URI uri = url.toURI();
            final Path path = Path.of(uri).toAbsolutePath().normalize();
            return Files.exists(path) ? path : null;
        } catch (final URISyntaxException | IllegalArgumentException _) {
            return null;
        }
    }

    private static void scanDirectory(
            final Path root,
            final List<String> internalPrefixes,
            final boolean scanAllDirectoryClasses,
            final ClassFile classFile,
            final Map<String, List<String>> parentsByClass,
            final List<String> candidates) {

        final List<Path> walkRoots = new ArrayList<>();
        if (scanAllDirectoryClasses || internalPrefixes.isEmpty()) {
            walkRoots.add(root);
        } else {
            for (final String prefix : internalPrefixes) {
                final Path sub = root.resolve(prefix);
                if (Files.isDirectory(sub)) walkRoots.add(sub);
            }
        }

        for (final Path walkRoot : walkRoots) {
            try (Stream<Path> stream = Files.walk(walkRoot)) {
                stream.filter(Files::isRegularFile)
                        .filter(p -> p.getFileName().toString().endsWith(CLASS_SUFFIX))
                        .forEach(p -> {
                            final String rel = root.relativize(p).toString().replace('\\', '/');
                            if (rel.equals("module-info.class") || rel.endsWith("/module-info.class")
                                    || rel.endsWith("/package-info.class")) {
                                return;
                            }
                            try {
                                parseClassBytes(Files.readAllBytes(p), classFile, parentsByClass, candidates);
                            } catch (final Exception _) {
                            }
                        });
            } catch (final IOException _) {
            }
        }
    }

    private static void scanJar(
            final Path jarPath,
            final List<String> internalPrefixes,
            final ClassFile classFile,
            final Map<String, List<String>> parentsByClass,
            final List<String> candidates) {

        try (JarFile jar = new JarFile(jarPath.toFile())) {
            final Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                final JarEntry entry = entries.nextElement();
                if (entry.isDirectory()) continue;
                final String name = entry.getName();
                if (!name.endsWith(CLASS_SUFFIX)) continue;
                if (name.equals("module-info.class") || name.endsWith("/module-info.class")
                        || name.endsWith("/package-info.class")) {
                    continue;
                }
                if (!internalPrefixes.isEmpty()) {
                    if (!matchesAnyPrefix(name, internalPrefixes)) continue;
                } else if (isSkippedThirdParty(name)) {
                    continue;
                }
                try (InputStream in = jar.getInputStream(entry)) {
                    parseClassBytes(in.readAllBytes(), classFile, parentsByClass, candidates);
                } catch (final Exception _) {
                }
            }
        } catch (final IOException _) {
        }
    }

    private static void parseClassBytes(
            final byte[] bytes,
            final ClassFile classFile,
            final Map<String, List<String>> parentsByClass,
            final List<String> candidates) {

        final ClassModel model = classFile.parse(bytes);
        final String internalName = model.thisClass().asInternalName();
        if (parentsByClass.containsKey(internalName)) return;

        final List<String> parents = extractParents(model);
        parentsByClass.put(internalName, parents);
        candidates.add(internalName);
    }

    private static List<String> extractParents(final ClassModel model) {
        final List<ClassEntry> ifaces = model.interfaces();
        final List<String> parents = new ArrayList<>(1 + ifaces.size());
        model.superclass().ifPresent(sc -> {
            final String name = sc.asInternalName();
            if (!OBJECT_INTERNAL_NAME.equals(name)) parents.add(name);
        });
        for (final ClassEntry iface : ifaces) {
            parents.add(iface.asInternalName());
        }
        return parents;
    }

    private static boolean isSubtype(
            final String internalName,
            final String targetInternal,
            final ClassFile classFile,
            final ClassLoader classLoader,
            final Map<String, List<String>> parentsByClass,
            final Map<String, Boolean> memo) {

        final Boolean cached = memo.get(internalName);
        if (null != cached) return cached;

        // Guard against any malformed circular hierarchy
        memo.put(internalName, Boolean.FALSE);

        List<String> parents = parentsByClass.get(internalName);
        if (null == parents) {
            parents = resolveParentsFromClasspath(internalName, classFile, classLoader);
            parentsByClass.put(internalName, parents);
        }

        for (final String parent : parents) {
            if (targetInternal.equals(parent)
                    || isSubtype(parent, targetInternal, classFile, classLoader, parentsByClass, memo)) {
                memo.put(internalName, Boolean.TRUE);
                return true;
            }
        }
        return false;
    }

    private static List<String> resolveParentsFromClasspath(
            final String internalName,
            final ClassFile classFile,
            final ClassLoader classLoader) {

        if (null == internalName || OBJECT_INTERNAL_NAME.equals(internalName)) return List.of();
        try (InputStream in = classLoader.getResourceAsStream(s(internalName, CLASS_SUFFIX))) {
            if (null == in) return List.of();
            return extractParents(classFile.parse(in.readAllBytes()));
        } catch (final Exception _) {
            return List.of();
        }
    }

    private static Class<?> loadUnchecked(final String internalName, final ClassLoader classLoader) {
        try {
            return Class.forName(internalName.replace('/', '.'), false, classLoader);
        } catch (final Throwable _) {
            return null;
        }
    }

    private static boolean matchesAnyPrefix(final String internalName, final List<String> prefixes) {
        for (final String prefix : prefixes) {
            if (internalName.startsWith(prefix)) return true;
        }
        return false;
    }

    private static boolean isSkippedThirdParty(final String internalName) {
        for (final String prefix : SKIPPED_JAR_PREFIXES) {
            if (internalName.startsWith(prefix)) return true;
        }
        return false;
    }
}
