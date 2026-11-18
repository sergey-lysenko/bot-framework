package works.lysenko.base.output;

import works.lysenko.util.apis.core._Results;
import works.lysenko.util.apis.data._Result;
import works.lysenko.util.apis.scenario._Scenario;
import works.lysenko.util.data.enums.ExecutionStatus;
import works.lysenko.util.spec.Layout;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static works.lysenko.Base.core;
import static works.lysenko.util.spec.Layout.Templates.RUN_LOG_MD_TEST_;

public class SummaryGenerator {

    @SuppressWarnings({"NestedMethodCall", "UseOfSystemOutOrSystemErr", "DefaultCharset"})
    public static void run() {

        if (core == null || core.getResults() == null) return;
        
        final _Results results = core.getResults();
        final Map<_Scenario, _Result> sorted = results.getSorted();
        
        final File summaryFile = new File(Layout.Files.name(RUN_LOG_MD_TEST_, "summary"));
        if (summaryFile.getParentFile() != null) {
            summaryFile.getParentFile().mkdirs();
        }
        
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(summaryFile, StandardCharsets.UTF_8))) {
            writer.write("# Test Execution Log - Summary" + System.lineSeparator() + System.lineSeparator());
            
            writer.write(String.format("> **Total Scenarios**: %d | **Mode**: %s%n%n", sorted.size(), (works.lysenko.util.prop.PropEnum._TEST_ALL_LEAFS.get() != null && Boolean.TRUE.equals(works.lysenko.util.prop.PropEnum._TEST_ALL_LEAFS.get())) ? "ALL_LEAFS" : "DEFAULT"));
            
            writer.write("| # | Scenario Path | Result | Executions |" + System.lineSeparator());
            writer.write("| :--- | :--- | :--- | :--- |" + System.lineSeparator());
            
            int index = 1;
            for (final Map.Entry<_Scenario, _Result> entry : sorted.entrySet()) {
                final _Scenario scenario = entry.getKey();
                final _Result result = entry.getValue();
                
                final String statusMark = getStatusMarker(result.status());
                
                writer.write(String.format("| %d | %s | %s | %d |%n", 
                        index++, 
                        scenario.getName(), 
                        statusMark,
                        result.getExecutions()));
            }
            
            writer.write(System.lineSeparator() + "### Run Statistics" + System.lineSeparator());
            writer.write(String.format("* **Total Executed Scenarios:** %d%n", sorted.size()));
            
        } catch (final IOException ignored) {
        }
    }
    
    private static String getStatusMarker(final ExecutionStatus status) {
        if (status == null) return "⚪ UNKNOWN";
        return switch (status) {
            case PASSED -> "✅ PASS";
            case FAILED -> "❌ FAIL";
            case WARNING -> "⚠️ WARN";
            case CHILD_FAILED -> "⚠️ CHILD_FAIL";
            case UNVISITED -> "⏭ UNVISITED";
        };
    }
}
