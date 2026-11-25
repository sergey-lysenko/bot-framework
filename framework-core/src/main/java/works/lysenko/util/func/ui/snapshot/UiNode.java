package works.lysenko.util.func.ui.snapshot;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.Attributes;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Immutable representation of a single node captured in a {@link UiSnapshot}.
 *
 * @param tag         XML element tag name (e.g. {@code android.widget.Button})
 * @param className   {@code class} attribute, or {@code tag} when absent
 * @param packageName {@code package} attribute (empty string if absent)
 * @param resourceId  full {@code resource-id} attribute (empty string if absent)
 * @param shortId     ID suffix after {@code :id/}, or {@code resourceId} if no prefix is present
 * @param text        {@code text} attribute (empty string if absent)
 * @param contentDesc {@code content-desc} attribute (empty string if absent)
 * @param index       {@code index} attribute, or {@code -1} if absent/invalid
 * @param displayed   whether the node is displayed ({@code displayed="true"}, or {@code true} when omitted)
 * @param clickable   whether the node is clickable
 * @param enabled     whether the node is enabled
 * @param checkable   whether the node is checkable
 * @param checked     whether the node is checked
 * @param selected    whether the node is selected
 * @param scrollable  whether the node is scrollable
 * @param focusable   whether the node is focusable
 * @param focused     whether the node is focused
 * @param bounds      parsed screen bounds
 * @param attributes  unmodifiable map of all raw XML attributes on this node
 */
public record UiNode(
        String tag,
        String className,
        String packageName,
        String resourceId,
        String shortId,
        String text,
        String contentDesc,
        int index,
        boolean displayed,
        boolean clickable,
        boolean enabled,
        boolean checkable,
        boolean checked,
        boolean selected,
        boolean scrollable,
        boolean focusable,
        boolean focused,
        UiBounds bounds,
        Map<String, String> attributes
) {

    private static final String ID_SEPARATOR = ":id/";

    /**
     * Extracts the short ID after {@code :id/} (e.g. {@code "works.lysenko.rozkladach:id/scheduleView"} -> {@code "scheduleView"}).
     *
     * @param resourceId full resource-id string
     * @return short ID, or {@code ""} if null/empty
     */
    public static String extractShortId(final String resourceId) {

        if (null == resourceId || resourceId.isEmpty()) {
            return "";
        }
        final int sep = resourceId.indexOf(ID_SEPARATOR);
        return (sep >= 0) ? resourceId.substring(sep + ID_SEPARATOR.length()) : resourceId;
    }

    static UiNode fromSaxAttributes(final String qName, final Attributes attrs) {

        final int len = attrs.getLength();
        final Map<String, String> map = (len > 0) ? new LinkedHashMap<>(len * 2) : Map.of();
        for (int i = 0; i < len; i++) {
            map.put(attrs.getQName(i), attrs.getValue(i));
        }
        return fromMap(qName, (len > 0) ? Collections.unmodifiableMap(map) : map);
    }

    static UiNode fromDomElement(final Element element) {

        final NamedNodeMap named = element.getAttributes();
        final int len = named.getLength();
        final Map<String, String> map = (len > 0) ? new LinkedHashMap<>(len * 2) : Map.of();
        for (int i = 0; i < len; i++) {
            final Node attr = named.item(i);
            map.put(attr.getNodeName(), attr.getNodeValue());
        }
        return fromMap(element.getTagName(), (len > 0) ? Collections.unmodifiableMap(map) : map);
    }

    private static UiNode fromMap(final String tagName, final Map<String, String> attrs) {

        final String tag = (null != tagName) ? tagName : "";
        final String className = nonNull(attrs.get("class"), tag);
        final String packageName = nonNull(attrs.get("package"), "");
        final String resourceId = nonNull(attrs.get("resource-id"), "");
        final String shortId = extractShortId(resourceId);
        final String text = nonNull(attrs.get("text"), "");
        final String contentDesc = nonNull(attrs.get("content-desc"), "");
        final int index = parseInt(attrs.get("index"));
        final String displayedRaw = attrs.get("displayed");
        final boolean displayed = (null == displayedRaw) || "true".equalsIgnoreCase(displayedRaw);
        final boolean clickable = "true".equalsIgnoreCase(attrs.get("clickable"));
        final boolean enabled = "true".equalsIgnoreCase(attrs.get("enabled"));
        final boolean checkable = "true".equalsIgnoreCase(attrs.get("checkable"));
        final boolean checked = "true".equalsIgnoreCase(attrs.get("checked"));
        final boolean selected = "true".equalsIgnoreCase(attrs.get("selected"));
        final boolean scrollable = "true".equalsIgnoreCase(attrs.get("scrollable"));
        final boolean focusable = "true".equalsIgnoreCase(attrs.get("focusable"));
        final boolean focused = "true".equalsIgnoreCase(attrs.get("focused"));
        final UiBounds bounds = UiBounds.parse(attrs.get("bounds"));
        return new UiNode(
                tag,
                className,
                packageName,
                resourceId,
                shortId,
                text,
                contentDesc,
                index,
                displayed,
                clickable,
                enabled,
                checkable,
                checked,
                selected,
                scrollable,
                focusable,
                focused,
                bounds,
                attrs
        );
    }

    private static String nonNull(final String value, final String fallback) {

        return (null != value) ? value : fallback;
    }

    private static int parseInt(final String raw) {

        if (null == raw || raw.isEmpty()) {
            return -1;
        }
        try {
            return Integer.parseInt(raw);
        } catch (final NumberFormatException ignored) {
            return -1;
        }
    }

    /**
     * @param name XML attribute name
     * @return attribute value, or {@code ""} if absent
     */
    public String attr(final String name) {

        return attributes.getOrDefault(name, "");
    }

    /**
     * @return {@code true} if non-empty bounds are available for direct tapping
     */
    public boolean hasBounds() {

        return null != bounds && !bounds.isEmpty();
    }

    /**
     * @return {@code true} if the node is displayed and has non-empty bounds
     */
    public boolean isVisible() {

        return displayed && hasBounds();
    }

    /**
     * @param id either short ID (e.g. {@code "scheduleView"}) or full resource-id
     * @return {@code true} if this node matches the given ID
     */
    public boolean matchesId(final String id) {

        if (null == id || id.isEmpty()) {
            return false;
        }
        return id.equals(shortId) || id.equals(resourceId);
    }

    /**
     * @return {@code true} if this node represents a Button widget
     */
    public boolean isButton() {

        return "android.widget.Button".equals(tag)
                || "android.widget.Button".equals(className)
                || tag.endsWith(".Button");
    }

    /**
     * Taps the center of this node's bounds via {@link Snapshots#tap(UiNode)}.
     *
     * @return {@code true} if the tap was dispatched
     */
    public boolean tap() {

        return Snapshots.tap(this);
    }

    /**
     * Taps at relative coordinates within this node's bounds via {@link Snapshots#tap(UiNode, double, double)}.
     *
     * @param xRatio horizontal ratio in {@code [0.0, 1.0]}
     * @param yRatio vertical ratio in {@code [0.0, 1.0]}
     * @return {@code true} if the tap was dispatched
     */
    public boolean tap(final double xRatio, final double yRatio) {

        return Snapshots.tap(this, xRatio, yRatio);
    }

    /**
     * @return concise human-readable label for logs
     */
    public String describe() {

        if (!shortId.isEmpty() && !text.isEmpty()) {
            return shortId + " ('" + text + "')";
        }
        if (!shortId.isEmpty()) {
            return shortId;
        }
        if (!contentDesc.isEmpty()) {
            return "'" + contentDesc + "'";
        }
        if (!text.isEmpty()) {
            return "'" + text + "'";
        }
        return tag;
    }
}
