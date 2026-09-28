package works.lysenko.base.output;

import works.lysenko.base.core.Routines;
import works.lysenko.util.apis.data._Result;
import works.lysenko.util.apis.log._LogRecord;
import works.lysenko.util.apis.scenario._Scenario;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static works.lysenko.Base.core;
import static works.lysenko.Base.exec;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.func.type.Objects.isNotNull;
import static works.lysenko.util.spec.Layout.Files.name;
import static works.lysenko.util.spec.Layout.Templates.RUN_JSON_;

/**
 * Various JSON-related code
 */
@SuppressWarnings({"UtilityClass", "UtilityClassCanBeEnum", "FinalClass", "ClassWithTooManyTransitiveDependents",
        "ClassWithTooManyTransitiveDependencies", "ClassWithoutLogger", "PublicMethodWithoutLogging",
        "ClassUnconnectedToPackage",
        "ImplicitCallToSuper", "CyclicClassDependency", "ClassWithTooManyDependencies"})
public final class Json {

    private Json() {

    }

    /**
     * This routine writes test execution in JSON format. Potentially will be
     * replaced by some JSON libraries if the amount of data to write is more
     * than manageable. File location defined by DEFAULT_RUNS_LOCATION, file name
     * template defined by RUN_JSON_FILENAME
     */
    @SuppressWarnings({"ValueOfIncrementOrDecrementUsed", "HardcodedLineSeparator", "HardcodedFileSeparator",
            "DynamicRegexReplaceableByCompiledPattern", "AutoBoxing", "OverlyLongMethod", "OverlyComplexMethod",
            "MagicCharacter",
            "ProhibitedExceptionThrown", "ThrowInsideCatchBlockWhichIgnoresCaughtException", "LawOfDemeter",
            "FeatureEnvy", "ChainedMethodCall", "LocalCanBeFinal", "NestedMethodCall", "UnqualifiedStaticUsage",
            "MethodWithMultipleLoops",
            "ForeachStatement", "RegExpRedundantEscape"})
    public static void jsonStats() {

        Map<_Scenario, _Result> sorted = core.getResults().getSorted();
        try (BufferedWriter w = new BufferedWriter(new FileWriter(name(RUN_JSON_), StandardCharsets.UTF_8))) {
            int i;
            w.write(s("{\"startAt\":", Routines.startedAt(), ",\"issues\":{")); //NON-NLS
            w.write("\"newIssues\":["); //NON-NLS

            List<_LogRecord> newIssues = exec.issues().latestCopy();
            i = newIssues.size();
            for (_LogRecord lr : newIssues) {
                w.write(s('"', lr.render(core.getTotalTests(), core.getLogger().getSpanLength(), null).replaceAll("\n",
                        " ").replace("\"", "\\\""), '"'));
                if (1 < i--) w.write(",");
            }

            i = exec.issues().known().size();
            w.write("],\"knownIssues\":["); //NON-NLS
            for (String str : exec.issues().known()) {
                w.write(s("\"", str.replaceAll("\n", " ").replace("\"",
                        "\\\""), "\""));
                if (1 < i--) w.write(",");
            }

            if (isNotNull(exec.issues().notReproduced())) {
                i = exec.issues().notReproduced().size();
                w.write("],\"notReproduced\":["); //NON-NLS
                for (String str : exec.issues().notReproduced()) {
                    w.write(s("\"", str.replaceAll("\n", " ").replace("\"",
                            "\\\""), "\""));
                    if (1 < i--) w.write(",");
                }
            }

            w.write("]},\"run\":["); //NON-NLS
            i = sorted.size();
            for (Map.Entry<_Scenario, _Result> entry : sorted.entrySet()) {
                w.write(s("{\"scenario\":\"", entry.getKey(), "\"", ",\"cWeight\": ", //NON-NLS
                        jsonify(entry.getValue().getConfiguredWeight().doubleValue()), ",\"dWeight\": ",//NON-NLS
                        jsonify(entry.getValue().getDownstreamWeight().doubleValue()), ",\"uWeight\": ",//NON-NLS
                        jsonify(entry.getValue().getUpstreamWeight().doubleValue()), ",\"executions\":",//NON-NLS
                        entry.getValue().getExecutions())); //NON-NLS
                if (!entry.getValue().getEvents().isEmpty()) {
                    w.write(",\"events\":["); //NON-NLS
                    int j = entry.getValue().getEvents().size();
                    for (_LogRecord logRecord : entry.getValue().getEvents()) {
                        String shift;
                        String type;
                        String text;
                        String p0 = logRecord.text().split(" ")[0];
                        String p1 = logRecord.text().split(" ")[1];
                        if (p0.matches("\\[[\\d]+\\]")) {
                            shift = p0.substring(1, p0.length() - 1);
                            type = p1;
                        } else {
                            shift = EMPTY;
                            type = p0;
                        }
                        w.write(s("{\"timestamp\":", logRecord.time(), ",")); //NON-NLS
                        if (shift.isEmpty()) text = logRecord.text().substring(type.length() + 1);
                        else {
                            text = logRecord.text().substring(shift.length() + type.length() + 4);
                            w.write(s("\"shift\":", shift, ",")); //NON-NLS
                        }
                        w.write(s("\"type\":\"", type, "\",")); //NON-NLS
                        w.write("\"text\":\""); //NON-NLS
                        w.write(text.replaceAll("\n", " ").replace("\"", "\\\""));
                        w.write("\"}");
                        if (1 < j--) w.write(",");
                    }
                    w.write("]");
                }
                w.write("}");
                if (1 < i--) w.write(",");
            }
            w.write("]}");
        } catch (IOException e) {
            throw new RuntimeException("JSON stats writing issue");
        }
    }

    @SuppressWarnings({"AutoUnboxing", "LocalCanBeFinal"})
    private static double jsonify(Double d) {

        return Double.POSITIVE_INFINITY == d ? Double.MAX_VALUE : d;
    }
}
