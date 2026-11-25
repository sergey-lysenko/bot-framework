package works.lysenko.util.func.ui.snapshot;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.DefaultHandler;
import works.lysenko.util.func.ui.Descriptors;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Immutable, locally indexed snapshot of the current UI hierarchy XML (captured in one
 * {@code getPageSource()} call and analyzed locally in Java without additional Selenium round-trips).
 * <p>
 * Standard queries ({@code hasId}, {@code hasText}, {@code hasDesc}, {@code hasButton}, etc.)
 * operate in O(1) over pre-indexed displayed nodes. Framework locator descriptors and arbitrary
 * XPath expressions are also supported via {@link #hasLocator(String)} / {@link #findByLocator(String)}
 * with a fast-path index matcher and a lazy local DOM/XPath fallback.
 */
public final class UiSnapshot {

    private static final UiSnapshot EMPTY = new UiSnapshot(
            "",
            0L,
            List.of(),
            List.of(),
            Map.of(),
            Map.of(),
            Map.of(),
            Map.of(),
            Map.of()
    );

    private static final Pattern SIMPLE_BY_ID =
            Pattern.compile("^//([\\w.*$-]+)\\[@resource-id='([^']+)'\\]$");
    private static final Pattern SIMPLE_BY_TEXT =
            Pattern.compile("^//([\\w.*$-]+)\\[@text='([^']+)'\\]$");
    private static final Pattern SIMPLE_BY_DESC =
            Pattern.compile("^//([\\w.*$-]+)\\[@content-desc='([^']+)'\\]$");

    private static final int MAX_CACHED_XPATHS = 256;

    private static final ThreadLocal<SAXParser> SAX_PARSER = ThreadLocal.withInitial(() -> {
        try {
            final SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setNamespaceAware(false);
            return factory.newSAXParser();
        } catch (final Exception e) {
            throw new IllegalStateException("Failed to initialize SAXParser for UiSnapshot", e);
        }
    });

    private static final ThreadLocal<DocumentBuilder> DOM_BUILDER = ThreadLocal.withInitial(() -> {
        try {
            final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setNamespaceAware(false);
            return factory.newDocumentBuilder();
        } catch (final Exception e) {
            throw new IllegalStateException("Failed to initialize DocumentBuilder for UiSnapshot", e);
        }
    });

    private static final ThreadLocal<XPath> XPATH = ThreadLocal.withInitial(() ->
            XPathFactory.newInstance().newXPath());

    private static final ThreadLocal<Map<String, XPathExpression>> XPATH_CACHE = ThreadLocal.withInitial(() ->
            new LinkedHashMap<>(64, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(final Map.Entry<String, XPathExpression> eldest) {

                    return size() > MAX_CACHED_XPATHS;
                }
            });

    private final String rawXml;
    private final long capturedAtNanos;
    private final List<UiNode> allNodes;
    private final List<UiNode> displayedNodes;
    private final Map<String, List<UiNode>> byResourceId;
    private final Map<String, List<UiNode>> byShortId;
    private final Map<String, List<UiNode>> byText;
    private final Map<String, List<UiNode>> byContentDesc;
    private final Map<String, List<UiNode>> byTag;

    private volatile Document lazyDocument;

    private UiSnapshot(
            final String rawXml,
            final long capturedAtNanos,
            final List<UiNode> allNodes,
            final List<UiNode> displayedNodes,
            final Map<String, List<UiNode>> byResourceId,
            final Map<String, List<UiNode>> byShortId,
            final Map<String, List<UiNode>> byText,
            final Map<String, List<UiNode>> byContentDesc,
            final Map<String, List<UiNode>> byTag
    ) {

        this.rawXml = rawXml;
        this.capturedAtNanos = capturedAtNanos;
        this.allNodes = allNodes;
        this.displayedNodes = displayedNodes;
        this.byResourceId = byResourceId;
        this.byShortId = byShortId;
        this.byText = byText;
        this.byContentDesc = byContentDesc;
        this.byTag = byTag;
    }

    /**
     * @return an empty {@link UiSnapshot}
     */
    public static UiSnapshot empty() {

        return EMPTY;
    }

    /**
     * Parses a raw page-source XML string into an indexed {@link UiSnapshot}.
     *
     * @param xml raw XML hierarchy (from {@code WebDriver.getPageSource()} or test fixture)
     * @return parsed snapshot, or {@link #empty()} if input is null/blank/unparseable
     */
    public static UiSnapshot parse(final String xml) {

        return parse(xml, System.nanoTime());
    }

    /**
     * Parses a raw page-source XML string with an explicit capture timestamp.
     *
     * @param xml             raw XML hierarchy
     * @param capturedAtNanos monotonic capture timestamp ({@link System#nanoTime()})
     * @return parsed snapshot, or {@link #empty()} if input is null/blank/unparseable
     */
    public static UiSnapshot parse(final String xml, final long capturedAtNanos) {

        if (null == xml || xml.isBlank()) {
            return EMPTY;
        }
        final List<UiNode> all = new ArrayList<>(64);
        final List<UiNode> displayed = new ArrayList<>(64);
        final Map<String, List<UiNode>> byResourceId = new HashMap<>(64);
        final Map<String, List<UiNode>> byShortId = new HashMap<>(64);
        final Map<String, List<UiNode>> byText = new HashMap<>(64);
        final Map<String, List<UiNode>> byContentDesc = new HashMap<>(16);
        final Map<String, List<UiNode>> byTag = new HashMap<>(32);

        try {
            final SAXParser parser = SAX_PARSER.get();
            parser.reset();
            parser.parse(new InputSource(new StringReader(xml)), new DefaultHandler() {
                @Override
                public void startElement(
                        final String uri,
                        final String localName,
                        final String qName,
                        final Attributes attributes
                ) {

                    if ("hierarchy".equals(qName)) {
                        return;
                    }
                    final UiNode node = UiNode.fromSaxAttributes(qName, attributes);
                    all.add(node);
                    if (node.displayed()) {
                        displayed.add(node);
                        if (!node.resourceId().isEmpty()) {
                            index(byResourceId, node.resourceId(), node);
                            index(byShortId, node.shortId(), node);
                        }
                        if (!node.text().isEmpty()) {
                            index(byText, node.text(), node);
                        }
                        if (!node.contentDesc().isEmpty()) {
                            index(byContentDesc, node.contentDesc(), node);
                        }
                        if (!node.tag().isEmpty()) {
                            index(byTag, node.tag(), node);
                        }
                    }
                }
            });
        } catch (final Exception ignored) {
            return EMPTY;
        }

        return new UiSnapshot(
                xml,
                capturedAtNanos,
                Collections.unmodifiableList(all),
                Collections.unmodifiableList(displayed),
                freezeIndex(byResourceId),
                freezeIndex(byShortId),
                freezeIndex(byText),
                freezeIndex(byContentDesc),
                freezeIndex(byTag)
        );
    }

    private static void index(final Map<String, List<UiNode>> map, final String key, final UiNode node) {

        map.computeIfAbsent(key, ignored -> new ArrayList<>(2)).add(node);
    }

    private static Map<String, List<UiNode>> freezeIndex(final Map<String, List<UiNode>> map) {

        if (map.isEmpty()) {
            return Map.of();
        }
        map.replaceAll((ignored, list) -> Collections.unmodifiableList(list));
        return Collections.unmodifiableMap(map);
    }

    /**
     * @return {@code true} if no nodes were parsed
     */
    public boolean isEmpty() {

        return allNodes.isEmpty();
    }

    /**
     * @return raw XML source string
     */
    public String rawXml() {

        return rawXml;
    }

    /**
     * @return monotonic capture timestamp in nanoseconds
     */
    public long capturedAtNanos() {

        return capturedAtNanos;
    }

    /**
     * @return unmodifiable list of displayed nodes in document order
     */
    public List<UiNode> nodes() {

        return displayedNodes;
    }

    /**
     * @return unmodifiable list of all parsed nodes (including {@code displayed="false"})
     */
    public List<UiNode> allNodes() {

        return allNodes;
    }

    // -------------------------------------------------------------------------
    // O(1) indexed queries on displayed nodes
    // -------------------------------------------------------------------------

    /**
     * Checks if a displayed node exists with the given resource ID (short ID or full {@code pkg:id/name}).
     *
     * @param id short ID or full resource-id
     * @return {@code true} if present and displayed
     */
    public boolean hasId(final String id) {

        return !findAllById(id).isEmpty();
    }

    /**
     * @param ids resource IDs (short or full)
     * @return {@code true} if at least one matching displayed node exists
     */
    public boolean hasAnyId(final String... ids) {

        for (final String id : ids) {
            if (hasId(id)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param ids resource IDs (short or full)
     * @return {@code true} if every given ID has a displayed node
     */
    public boolean hasAllIds(final String... ids) {

        for (final String id : ids) {
            if (!hasId(id)) {
                return false;
            }
        }
        return ids.length > 0;
    }

    /**
     * @param id short ID or full resource-id
     * @return first matching displayed node
     */
    public Optional<UiNode> findById(final String id) {

        final List<UiNode> list = findAllById(id);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    /**
     * @param id short ID or full resource-id
     * @return all matching displayed nodes in document order
     */
    public List<UiNode> findAllById(final String id) {

        if (null == id || id.isEmpty()) {
            return List.of();
        }
        final List<UiNode> exact = byResourceId.get(id);
        if (null != exact) {
            return exact;
        }
        final List<UiNode> shortMatch = byShortId.get(id);
        return (null != shortMatch) ? shortMatch : List.of();
    }

    /**
     * Checks if a displayed node has the given ID and one of the specified exact texts.
     *
     * @param id    short ID or full resource-id
     * @param texts candidate exact texts
     * @return {@code true} if matched
     */
    public boolean hasIdWithText(final String id, final String... texts) {

        return findByIdAndText(id, texts).isPresent();
    }

    /**
     * Finds the first displayed node matching the given ID and any of the specified exact texts.
     *
     * @param id    short ID or full resource-id
     * @param texts candidate exact texts
     * @return matching node, if present
     */
    public Optional<UiNode> findByIdAndText(final String id, final String... texts) {

        for (final UiNode node : findAllById(id)) {
            for (final String candidate : texts) {
                if (null != candidate && candidate.equals(node.text())) {
                    return Optional.of(node);
                }
            }
        }
        return Optional.empty();
    }

    /**
     * @param text exact text attribute
     * @return {@code true} if a displayed node has this exact text
     */
    public boolean hasText(final String text) {

        return !findAllByText(text).isEmpty();
    }

    /**
     * @param texts candidate exact texts
     * @return {@code true} if any displayed node matches one of the texts
     */
    public boolean hasAnyText(final String... texts) {

        for (final String text : texts) {
            if (hasText(text)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param substring text substring
     * @return {@code true} if any displayed node contains the substring in its text
     */
    public boolean hasTextContaining(final String substring) {

        return findByTextContaining(substring).isPresent();
    }

    /**
     * @param text exact text attribute
     * @return first matching displayed node
     */
    public Optional<UiNode> findByText(final String text) {

        final List<UiNode> list = findAllByText(text);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    /**
     * @param text exact text attribute
     * @return all matching displayed nodes
     */
    public List<UiNode> findAllByText(final String text) {

        if (null == text || text.isEmpty()) {
            return List.of();
        }
        final List<UiNode> list = byText.get(text);
        return (null != list) ? list : List.of();
    }

    /**
     * @param substring text substring
     * @return first displayed node whose text contains {@code substring}
     */
    public Optional<UiNode> findByTextContaining(final String substring) {

        if (null == substring || substring.isEmpty()) {
            return Optional.empty();
        }
        for (final UiNode node : displayedNodes) {
            if (node.text().contains(substring)) {
                return Optional.of(node);
            }
        }
        return Optional.empty();
    }

    /**
     * @param contentDesc exact {@code content-desc} attribute
     * @return {@code true} if a displayed node matches
     */
    public boolean hasDesc(final String contentDesc) {

        return findByDesc(contentDesc).isPresent();
    }

    /**
     * @param descs candidate exact {@code content-desc} values
     * @return {@code true} if any matches
     */
    public boolean hasAnyDesc(final String... descs) {

        for (final String desc : descs) {
            if (hasDesc(desc)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param substrings candidate substrings to match against {@code content-desc}
     * @return {@code true} if any displayed node's {@code content-desc} contains one of the substrings
     */
    public boolean hasDescContaining(final String... substrings) {

        return findByDescContaining(substrings).isPresent();
    }

    /**
     * @param contentDesc exact {@code content-desc} attribute
     * @return first matching displayed node
     */
    public Optional<UiNode> findByDesc(final String contentDesc) {

        final List<UiNode> list = findAllByDesc(contentDesc);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    /**
     * @param contentDesc exact {@code content-desc} attribute
     * @return all matching displayed nodes
     */
    public List<UiNode> findAllByDesc(final String contentDesc) {

        if (null == contentDesc || contentDesc.isEmpty()) {
            return List.of();
        }
        final List<UiNode> list = byContentDesc.get(contentDesc);
        return (null != list) ? list : List.of();
    }

    /**
     * @param substrings candidate substrings to match against {@code content-desc}
     * @return first displayed node whose {@code content-desc} contains one of the substrings
     */
    public Optional<UiNode> findByDescContaining(final String... substrings) {

        final List<UiNode> matches = findAllByDescContaining(substrings);
        return matches.isEmpty() ? Optional.empty() : Optional.of(matches.get(0));
    }

    /**
     * @param substrings candidate substrings to match against {@code content-desc}
     * @return all displayed nodes whose {@code content-desc} contains one of the substrings
     */
    public List<UiNode> findAllByDescContaining(final String... substrings) {

        final List<UiNode> matches = new ArrayList<>();
        for (final UiNode node : displayedNodes) {
            final String desc = node.contentDesc();
            if (desc.isEmpty()) {
                continue;
            }
            for (final String sub : substrings) {
                if (null != sub && !sub.isEmpty() && desc.contains(sub)) {
                    matches.add(node);
                    break;
                }
            }
        }
        return matches.isEmpty() ? List.of() : Collections.unmodifiableList(matches);
    }

    /**
     * @param text exact button text
     * @return {@code true} if a displayed Button widget has the given text
     */
    public boolean hasButton(final String text) {

        return findButton(text).isPresent();
    }

    /**
     * @param texts candidate button texts
     * @return {@code true} if any displayed Button widget matches
     */
    public boolean hasAnyButton(final String... texts) {

        for (final String text : texts) {
            if (hasButton(text)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param text exact button text
     * @return first displayed Button widget with the given text
     */
    public Optional<UiNode> findButton(final String text) {

        for (final UiNode node : findAllByText(text)) {
            if (node.isButton()) {
                return Optional.of(node);
            }
        }
        return Optional.empty();
    }

    /**
     * @param text exact button text
     * @return all displayed Button widgets with the given text
     */
    public List<UiNode> findAllButtons(final String text) {

        final List<UiNode> candidates = findAllByText(text);
        if (candidates.isEmpty()) {
            return List.of();
        }
        final List<UiNode> buttons = new ArrayList<>(candidates.size());
        for (final UiNode node : candidates) {
            if (node.isButton()) {
                buttons.add(node);
            }
        }
        return buttons.isEmpty() ? List.of() : Collections.unmodifiableList(buttons);
    }

    /**
     * @param predicate custom predicate evaluated over displayed nodes
     * @return {@code true} if any displayed node satisfies {@code predicate}
     */
    public boolean has(final Predicate<UiNode> predicate) {

        return find(predicate).isPresent();
    }

    /**
     * @param predicate custom predicate evaluated over displayed nodes
     * @return first matching displayed node
     */
    public Optional<UiNode> find(final Predicate<UiNode> predicate) {

        for (final UiNode node : displayedNodes) {
            if (predicate.test(node)) {
                return Optional.of(node);
            }
        }
        return Optional.empty();
    }

    /**
     * @param predicate custom predicate evaluated over displayed nodes
     * @return all matching displayed nodes
     */
    public List<UiNode> findAll(final Predicate<UiNode> predicate) {

        final List<UiNode> matches = new ArrayList<>();
        for (final UiNode node : displayedNodes) {
            if (predicate.test(node)) {
                matches.add(node);
            }
        }
        return Collections.unmodifiableList(matches);
    }

    // -------------------------------------------------------------------------
    // Framework locator & local XPath bridge
    // -------------------------------------------------------------------------

    /**
     * Checks whether a framework locator key (from {@code locators}) or raw XPath expression
     * matches at least one displayed node in this snapshot — evaluated completely locally.
     *
     * @param locatorOrXpath framework descriptor key or raw XPath
     * @return {@code true} if matched
     */
    public boolean hasLocator(final String locatorOrXpath) {

        return !findAllByLocator(locatorOrXpath).isEmpty();
    }

    /**
     * Finds the first displayed node matching a framework locator key or raw XPath expression.
     *
     * @param locatorOrXpath framework descriptor key or raw XPath
     * @return first matching displayed node
     */
    public Optional<UiNode> findByLocator(final String locatorOrXpath) {

        final List<UiNode> list = findAllByLocator(locatorOrXpath);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    /**
     * Finds all displayed nodes matching a framework locator key or raw XPath expression.
     *
     * @param locatorOrXpath framework descriptor key or raw XPath
     * @return matching displayed nodes
     */
    public List<UiNode> findAllByLocator(final String locatorOrXpath) {

        return findAllByLocator(locatorOrXpath, true);
    }

    /**
     * Finds nodes matching a framework locator key or raw XPath expression.
     *
     * @param locatorOrXpath framework descriptor key or raw XPath
     * @param displayedOnly  whether to filter results to {@code displayed="true"}
     * @return matching nodes
     */
    public List<UiNode> findAllByLocator(final String locatorOrXpath, final boolean displayedOnly) {

        if (isEmpty() || null == locatorOrXpath || locatorOrXpath.isBlank()) {
            return List.of();
        }
        final String xpath = resolveXpath(locatorOrXpath);
        if (null == xpath || xpath.isBlank()) {
            return List.of();
        }
        if (displayedOnly) {
            final List<UiNode> fast = tryFastPath(xpath);
            if (null != fast) {
                return fast;
            }
        }
        return evaluateDomXpath(xpath, displayedOnly);
    }

    private List<UiNode> tryFastPath(final String xpath) {

        Matcher m = SIMPLE_BY_ID.matcher(xpath);
        if (m.matches()) {
            final List<UiNode> exact = byResourceId.get(m.group(2));
            return (null == exact) ? List.of() : filterByTag(exact, m.group(1));
        }
        m = SIMPLE_BY_TEXT.matcher(xpath);
        if (m.matches()) {
            return filterByTag(findAllByText(m.group(2)), m.group(1));
        }
        m = SIMPLE_BY_DESC.matcher(xpath);
        if (m.matches()) {
            final List<UiNode> byDesc = byContentDesc.get(m.group(2));
            return (null == byDesc) ? List.of() : filterByTag(byDesc, m.group(1));
        }
        return null;
    }

    private static List<UiNode> filterByTag(final List<UiNode> candidates, final String tag) {

        if (candidates.isEmpty() || "*".equals(tag)) {
            return candidates;
        }
        final List<UiNode> filtered = new ArrayList<>(candidates.size());
        for (final UiNode node : candidates) {
            if (tag.equals(node.tag())) {
                filtered.add(node);
            }
        }
        return Collections.unmodifiableList(filtered);
    }

    private static String resolveXpath(final String locatorOrXpath) {

        final String trimmed = locatorOrXpath.trim();
        if (trimmed.startsWith("/") || trimmed.startsWith("(") || trimmed.startsWith(".")) {
            return trimmed;
        }
        return Descriptors.get(trimmed);
    }

    private List<UiNode> evaluateDomXpath(final String xpathExpr, final boolean displayedOnly) {

        final Document doc = getOrParseDocument();
        if (null == doc) {
            return List.of();
        }
        try {
            final Map<String, XPathExpression> cache = XPATH_CACHE.get();
            XPathExpression compiled = cache.get(xpathExpr);
            if (null == compiled) {
                compiled = XPATH.get().compile(xpathExpr);
                cache.put(xpathExpr, compiled);
            }
            final NodeList nodes = (NodeList) compiled.evaluate(doc, XPathConstants.NODESET);
            if (null == nodes || 0 == nodes.getLength()) {
                return List.of();
            }
            final List<UiNode> result = new ArrayList<>(nodes.getLength());
            for (int i = 0; i < nodes.getLength(); i++) {
                final Node domNode = nodes.item(i);
                if (domNode instanceof Element element) {
                    final UiNode uiNode = UiNode.fromDomElement(element);
                    if (!displayedOnly || uiNode.displayed()) {
                        result.add(uiNode);
                    }
                }
            }
            return Collections.unmodifiableList(result);
        } catch (final Exception ignored) {
            return List.of();
        }
    }

    private Document getOrParseDocument() {

        Document doc = lazyDocument;
        if (null != doc) {
            return doc;
        }
        synchronized (this) {
            doc = lazyDocument;
            if (null == doc && !rawXml.isEmpty()) {
                try {
                    final DocumentBuilder builder = DOM_BUILDER.get();
                    builder.reset();
                    doc = builder.parse(new InputSource(new StringReader(rawXml)));
                    lazyDocument = doc;
                } catch (final Exception ignored) {
                    return null;
                }
            }
        }
        return doc;
    }

    // -------------------------------------------------------------------------
    // Direct tap conveniences (delegating to Snapshots.tap + cache invalidation)
    // -------------------------------------------------------------------------

    private static boolean tapFirst(final List<UiNode> candidates) {

        for (final UiNode node : candidates) {
            if (node.hasBounds()) {
                return Snapshots.tap(node);
            }
        }
        return false;
    }

    private static boolean tapFirst(final List<UiNode> candidates, final double xRatio, final double yRatio) {

        for (final UiNode node : candidates) {
            if (node.hasBounds()) {
                return Snapshots.tap(node, xRatio, yRatio);
            }
        }
        return false;
    }

    /**
     * Taps the first displayed node with valid bounds matching the given ID (short or full).
     *
     * @param id short ID or full resource-id
     * @return {@code true} if found and tapped
     */
    public boolean tapById(final String id) {

        return tapFirst(findAllById(id));
    }

    /**
     * Taps the first displayed node with valid bounds matching the given ID at relative coordinates.
     *
     * @param id     short ID or full resource-id
     * @param xRatio horizontal ratio in {@code [0.0, 1.0]}
     * @param yRatio vertical ratio in {@code [0.0, 1.0]}
     * @return {@code true} if found and tapped
     */
    public boolean tapById(final String id, final double xRatio, final double yRatio) {

        return tapFirst(findAllById(id), xRatio, yRatio);
    }

    /**
     * Taps the first displayed node with valid bounds matching the given exact text.
     *
     * @param text exact text
     * @return {@code true} if found and tapped
     */
    public boolean tapByText(final String text) {

        return tapFirst(findAllByText(text));
    }

    /**
     * Taps the first displayed Button with valid bounds matching the given exact text.
     *
     * @param text exact button text
     * @return {@code true} if found and tapped
     */
    public boolean tapButton(final String text) {

        return tapFirst(findAllButtons(text));
    }

    /**
     * Taps the first displayed node with valid bounds matching the given exact {@code content-desc}.
     *
     * @param contentDesc exact {@code content-desc}
     * @return {@code true} if found and tapped
     */
    public boolean tapByDesc(final String contentDesc) {

        return tapFirst(findAllByDesc(contentDesc));
    }

    /**
     * Taps the first displayed node with valid bounds whose {@code content-desc} contains any of the given substrings.
     *
     * @param substrings candidate substrings
     * @return {@code true} if found and tapped
     */
    public boolean tapByDescContaining(final String... substrings) {

        return tapFirst(findAllByDescContaining(substrings));
    }

    /**
     * Taps the first displayed node with valid bounds matching the given framework locator key or raw XPath.
     *
     * @param locatorOrXpath framework descriptor key or raw XPath
     * @return {@code true} if found and tapped
     */
    public boolean tapLocator(final String locatorOrXpath) {

        return tapFirst(findAllByLocator(locatorOrXpath));
    }

    /**
     * Taps the first displayed node with valid bounds matching the given framework locator key or raw XPath
     * at relative coordinates.
     *
     * @param locatorOrXpath framework descriptor key or raw XPath
     * @param xRatio         horizontal ratio in {@code [0.0, 1.0]}
     * @param yRatio         vertical ratio in {@code [0.0, 1.0]}
     * @return {@code true} if found and tapped
     */
    public boolean tapLocator(final String locatorOrXpath, final double xRatio, final double yRatio) {

        return tapFirst(findAllByLocator(locatorOrXpath), xRatio, yRatio);
    }

    /**
     * Classifies this snapshot using the provided {@link ScreenClassifier}.
     *
     * @param classifier screen classifier
     * @param <S>        screen type
     * @return classified screen
     */
    public <S> S classify(final ScreenClassifier<S> classifier) {

        return classifier.classify(this);
    }

    /**
     * @param tag XML element tag name
     * @return all displayed nodes with this exact tag
     */
    public List<UiNode> findAllByTag(final String tag) {

        if (null == tag || tag.isEmpty()) {
            return List.of();
        }
        final List<UiNode> list = byTag.get(tag);
        return (null != list) ? list : List.of();
    }
}
