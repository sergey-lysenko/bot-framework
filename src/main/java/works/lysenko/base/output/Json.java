package works.lysenko.base.output;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import works.lysenko.base.core.Routines;
import works.lysenko.util.apis.data._Result;
import works.lysenko.util.apis.log._LogRecord;
import works.lysenko.util.apis.scenario._Scenario;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static works.lysenko.Base.core;
import static works.lysenko.Base.exec;
import static works.lysenko.util.func.type.Objects.isNotNull;
import static works.lysenko.util.spec.Layout.Files.name;
import static works.lysenko.util.spec.Layout.Templates.RUN_JSON_;

/**
 * Standard-compliant JSON report serialization for test runs.
 */
@SuppressWarnings({"ClassWithoutLogger", "PublicMethodWithoutLogging", "LawOfDemeter", "ForeachStatement", "FeatureEnvy"})
public final class Json {

    private static final Pattern ANSI_PATTERN = Pattern.compile("\\x1b\\[[0-9;]*m");
    private static final ObjectMapper MAPPER = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    private Json() {
    }

    /**
     * Serializes test execution statistics and issues into standard-compliant indented JSON.
     */
    public static void jsonStats() {
        final Map<String, Object> root = new LinkedHashMap<>();
        root.put("startAt", Routines.startedAt());

        // Issues block
        final Map<String, Object> issues = new LinkedHashMap<>();
        final List<String> newIssues = new ArrayList<>();
        for (final _LogRecord lr : exec.issues().latestCopy()) {
            final String rendered = lr.render(core.getTotalTests(), core.getLogger().getSpanLength(), null);
            newIssues.add(stripAnsi(rendered).replace("\n", " ").trim());
        }
        issues.put("newIssues", newIssues);

        final List<String> knownIssues = new ArrayList<>();
        for (final String str : exec.issues().known()) {
            knownIssues.add(stripAnsi(str).replace("\n", " ").trim());
        }
        issues.put("knownIssues", knownIssues);

        if (isNotNull(exec.issues().notReproduced())) {
            final List<String> notRep = new ArrayList<>();
            for (final String str : exec.issues().notReproduced()) {
                notRep.add(stripAnsi(str).replace("\n", " ").trim());
            }
            issues.put("notReproduced", notRep);
        }
        root.put("issues", issues);

        // Run / scenarios block
        final Map<_Scenario, _Result> sorted = core.getResults().getSorted();
        final List<Map<String, Object>> runList = new ArrayList<>(sorted.size());

        for (final Map.Entry<_Scenario, _Result> entry : sorted.entrySet()) {
            final Map<String, Object> scenMap = new LinkedHashMap<>();
            scenMap.put("scenario", String.valueOf(entry.getKey()));
            scenMap.put("cWeight", jsonify(entry.getValue().getConfiguredWeight().doubleValue()));
            scenMap.put("dWeight", jsonify(entry.getValue().getDownstreamWeight().doubleValue()));
            scenMap.put("uWeight", jsonify(entry.getValue().getUpstreamWeight().doubleValue()));
            scenMap.put("executions", entry.getValue().getExecutions());
            scenMap.put("status", entry.getValue().status().code());

            if (!entry.getValue().getEvents().isEmpty()) {
                final List<Map<String, Object>> eventList = new ArrayList<>(entry.getValue().getEvents().size());
                for (final _LogRecord logRecord : entry.getValue().getEvents()) {
                    final Map<String, Object> eventMap = new LinkedHashMap<>();
                    final String rawText = stripAnsi(logRecord.text()).replace("\n", " ").trim();
                    final String[] parts = rawText.split(" ", 2);
                    String p0 = parts.length > 0 ? parts[0] : "";
                    String rest = parts.length > 1 ? parts[1] : "";

                    String shift = EMPTY;
                    String type;
                    String text;

                    if (p0.matches("\\[\\d+\\]")) {
                        shift = p0.substring(1, p0.length() - 1);
                        final String[] pSub = rest.split(" ", 2);
                        type = pSub.length > 0 ? pSub[0] : "";
                        text = pSub.length > 1 ? pSub[1] : "";
                    } else {
                        type = p0;
                        text = rest;
                    }

                    eventMap.put("timestamp", logRecord.time());
                    if (!shift.isEmpty()) {
                        try {
                            eventMap.put("shift", Long.parseLong(shift));
                        } catch (final NumberFormatException e) {
                            eventMap.put("shift", shift);
                        }
                    }
                    eventMap.put("type", type);
                    eventMap.put("text", text);
                    eventList.add(eventMap);
                }
                scenMap.put("events", eventList);
            }
            runList.add(scenMap);
        }
        root.put("run", runList);

        try {
            MAPPER.writeValue(new File(name(RUN_JSON_)), root);
        } catch (final IOException e) {
            throw new RuntimeException("JSON stats writing issue: " + e.getMessage(), e);
        }
    }

    private static String stripAnsi(final String text) {
        if (null == text) return "";
        return ANSI_PATTERN.matcher(text).replaceAll("");
    }

    private static double jsonify(final Double d) {
        return Double.POSITIVE_INFINITY == d ? Double.MAX_VALUE : d;
    }
}
