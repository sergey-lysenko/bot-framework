package works.lysenko.util.grid;

import works.lysenko.util.func.type.Files;
import works.lysenko.util.func.type.Properties;

import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Objects.isNull;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.func.type.Objects.isNotNull;
import static works.lysenko.util.spec.Layout.Files.knownColours;
import static works.lysenko.util.spec.Layout.Parts.THEME_EXTENSION;
import static works.lysenko.util.spec.Layout.Paths._THEMES_;

/**
 * Immutable named colour palette for theme-aware visual grid validation.
 * <p>
 * A {@code Theme} maps semantic colour token names (for example {@code SURFACE_BG}, {@code SURFACE_CARD})
 * to ARGB integer values. Grid expectation files can reference these semantic tokens instead of hard-coding
 * raw ARGB integers, allowing a single {@code .grid} file to validate any number of UI themes.
 * <p>
 * Theme files are stored as standard {@code .properties} files under {@code src/main/resources/themes/<name>.colours}
 * (with automatic classpath fallback for packaged JARs). Values may be signed ARGB integers ({@code -16184042}),
 * hex colours ({@code #090d16} / {@code #ff090d16}), or names defined in {@code known/colours} (e.g. {@code WHITE}).
 *
 * @param name    theme identifier (matches filename without extension)
 * @param colours immutable mapping from semantic colour token name to ARGB integer
 */
@SuppressWarnings({"MethodWithMultipleReturnPoints", "StaticMethodOnlyUsedInOneClass", "unused"})
public record Theme(String name, Map<String, Integer> colours) {

    private static final String CLASSPATH_THEMES_PREFIX = "themes/";
    private static final AtomicReference<Theme> ACTIVE = new AtomicReference<>();

    /**
     * Compact constructor enforcing an immutable copy of the palette map.
     */
    public Theme {

        final Map<String, Integer> copy = new LinkedHashMap<>();
        if (isNotNull(colours)) copy.putAll(colours);
        colours = Collections.unmodifiableMap(copy);
    }

    /**
     * Returns the globally active theme, or {@code null} when no theme is active.
     *
     * @return active theme or {@code null}
     */
    public static Theme active() {

        return ACTIVE.get();
    }

    /**
     * Clears the globally active theme.
     */
    public static void clearActive() {

        ACTIVE.set(null);
    }

    /**
     * Checks whether a theme file exists on disk or on the classpath.
     *
     * @param name theme identifier
     * @return {@code true} if the theme file is available
     */
    public static boolean isDefined(final String name) {

        if (isNull(name) || name.isBlank()) return false;
        final Path fsPath = Paths.get(path(name));
        if (java.nio.file.Files.exists(fsPath)) return true;
        final String resource = CLASSPATH_THEMES_PREFIX + name + THEME_EXTENSION;
        final ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try (InputStream in = isNotNull(cl)
                ? cl.getResourceAsStream(resource)
                : Theme.class.getClassLoader().getResourceAsStream(resource)) {
            if (isNotNull(in)) return true;
        } catch (final Exception ignored) {
            return false;
        }
        try (InputStream in = Theme.class.getClassLoader().getResourceAsStream(resource)) {
            return isNotNull(in);
        } catch (final Exception ignored) {
            return false;
        }
    }

    /**
     * Returns {@code true} if the given string parses directly as a signed base-10 integer.
     *
     * @param raw candidate token string
     * @return {@code true} if {@code raw} is a literal integer
     */
    public static boolean isLiteralInteger(final String raw) {

        if (isNull(raw)) return false;
        final String trimmed = raw.trim();
        if (trimmed.isEmpty()) return false;
        try {
            Integer.parseInt(trimmed);
            return true;
        } catch (final NumberFormatException ignored) {
            return false;
        }
    }

    /**
     * Discovers and loads all themes defined under {@code src/main/resources/themes/}.
     *
     * @return sorted list of defined themes (empty if no themes directory exists)
     */
    public static List<Theme> listDefined() {

        final Path dir = Paths.get(_THEMES_);
        if (!java.nio.file.Files.isDirectory(dir)) return List.of();
        final List<String> files = Files.getFilesList(_THEMES_, THEME_EXTENSION);
        if (isNull(files) || files.isEmpty()) return List.of();
        final List<String> names = new ArrayList<>(files.size());
        final String ext = s(THEME_EXTENSION);
        for (final String filePath : files) {
            final Path fileName = Paths.get(filePath).getFileName();
            if (isNotNull(fileName)) {
                final String fn = fileName.toString();
                if (fn.endsWith(ext) && fn.length() > ext.length()) {
                    names.add(fn.substring(0, fn.length() - ext.length()));
                }
            }
        }
        Collections.sort(names);
        final List<Theme> themes = new ArrayList<>(names.size());
        for (final String name : names) {
            final Theme loaded = of(name);
            if (isNotNull(loaded)) themes.add(loaded);
        }
        return Collections.unmodifiableList(themes);
    }

    /**
     * Loads a theme by name from {@code src/main/resources/themes/<name>.colours} (or classpath).
     *
     * @param name theme identifier
     * @return loaded {@link Theme}, or {@code null} if not defined
     */
    public static Theme of(final String name) {

        if (!isDefined(name)) return null;
        final java.util.Properties props = Properties.getPropertiesFromFile(path(name), true);
        if (isNull(props)) return null;
        final Map<String, Integer> parsed = new LinkedHashMap<>();
        final List<String> keys = new ArrayList<>(props.stringPropertyNames());
        Collections.sort(keys);
        for (final String key : keys) {
            final String rawVal = props.getProperty(key);
            final Integer rgb = parseColour(rawVal);
            if (isNull(rgb)) {
                throw new IllegalArgumentException(
                        "Invalid colour value '" + rawVal + "' for token '" + key + "' in theme '" + name + "'");
            }
            parsed.put(key.trim(), rgb);
        }
        return new Theme(name, parsed);
    }

    /**
     * Creates a theme programmatically from an explicit token-to-ARGB map.
     *
     * @param name    theme identifier
     * @param colours token-to-ARGB mapping
     * @return new immutable {@link Theme}
     */
    public static Theme of(final String name, final Map<String, Integer> colours) {

        return new Theme(name, colours);
    }

    /**
     * Parses a colour literal or {@code known/colours} reference into a signed ARGB integer.
     * Supports:
     * <ul>
     *   <li>Signed decimal ARGB integer (e.g. {@code -16184042})</li>
     *   <li>6-digit hex {@code #RRGGBB} or {@code 0xRRGGBB} (opaque alpha {@code 0xFF})</li>
     *   <li>8-digit hex {@code #AARRGGBB} or {@code 0xAARRGGBB}</li>
     *   <li>Symbolic name registered in {@code known/colours} (e.g. {@code WHITE}, {@code BLACK})</li>
     * </ul>
     *
     * @param raw raw colour specification
     * @return parsed ARGB integer, or {@code null} if unparseable / unknown
     */
    public static Integer parseColour(final String raw) {

        if (isNull(raw)) return null;
        final String trimmed = raw.trim();
        if (trimmed.isEmpty()) return null;

        if (trimmed.startsWith("#") || trimmed.startsWith("0x") || trimmed.startsWith("0X")) {
            final String hex = trimmed.startsWith("#") ? trimmed.substring(1) : trimmed.substring(2);
            try {
                if (hex.length() == 6) {
                    return 0xFF000000 | Integer.parseInt(hex, 16);
                }
                if (hex.length() == 8) {
                    return (int) Long.parseLong(hex, 16);
                }
                return null;
            } catch (final NumberFormatException ignored) {
                return null;
            }
        }

        if (isLiteralInteger(trimmed)) {
            return Integer.parseInt(trimmed);
        }

        final String known = knownColours.getProperty(trimmed);
        if (isNotNull(known) && !known.trim().equals(trimmed)) {
            return parseColour(known);
        }
        return null;
    }

    /**
     * Returns the relative resource path for a theme file.
     *
     * @param name theme identifier
     * @return path string {@code src/main/resources/themes/<name>.colours}
     */
    public static String path(final String name) {

        return s(_THEMES_, name, THEME_EXTENSION);
    }

    /**
     * Resolves a colour token using the given theme (when present), falling back to literals and {@code known/colours}.
     *
     * @param token colour token, hex literal, or integer literal
     * @param theme active theme (may be {@code null})
     * @return resolved ARGB integer, or {@code null} if unresolved
     */
    public static Integer resolveToken(final String token, final Theme theme) {

        if (isNull(token)) return null;
        if (isNotNull(theme)) {
            final Integer themed = theme.resolve(token);
            if (isNotNull(themed)) return themed;
        }
        return parseColour(token);
    }

    /**
     * Sets the globally active theme.
     *
     * @param theme theme instance (or {@code null} to clear)
     */
    public static void setActive(final Theme theme) {

        ACTIVE.set(theme);
    }

    /**
     * Loads the named theme and sets it as the globally active theme.
     *
     * @param themeName theme identifier (or {@code null} to clear)
     * @return loaded theme, or {@code null} if not defined
     */
    public static Theme setActive(final String themeName) {

        final Theme loaded = isNull(themeName) ? null : of(themeName);
        ACTIVE.set(loaded);
        return loaded;
    }

    /**
     * Returns {@code true} if this theme explicitly defines the given token.
     *
     * @param token semantic colour token name
     * @return {@code true} if defined in this theme
     */
    public boolean contains(final String token) {

        return isNotNull(token) && colours.containsKey(token.trim());
    }

    /**
     * Resolves a semantic colour token against this theme's palette, falling back to {@link #parseColour(String)}.
     *
     * @param token semantic token name, hex literal, or integer literal
     * @return resolved ARGB integer, or {@code null} if unknown
     */
    public Integer resolve(final String token) {

        if (isNull(token)) return null;
        final String trimmed = token.trim();
        final Integer mapped = colours.get(trimmed);
        if (isNotNull(mapped)) return mapped;
        return parseColour(trimmed);
    }
}
