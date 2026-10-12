package works.lysenko.base.output.loghtml.parser;

import works.lysenko.base.output.ProcessingProgress;
import works.lysenko.base.output.loghtml.LogModels;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static works.lysenko.base.output.loghtml.LogFormatting.SCEN_TOKEN_RE;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.lang.word.B.BOOTING;
import static works.lysenko.util.lang.word.C.CONFIGURING;
import static works.lysenko.util.lang.word.L.LIMBO;
import static works.lysenko.util.lang.word.N.NEUTRAL;
import static works.lysenko.util.lang.word.P.PASSED;
import static works.lysenko.util.lang.word.P.POSTFLIGHT;
import static works.lysenko.util.lang.word.P.PREFLIGHT;
import static works.lysenko.util.lang.word.R.REPORT;
import static works.lysenko.util.lang.word.R.RUN;
import static works.lysenko.util.lang.word.S.SPECIFICS;
import static works.lysenko.util.lang.word.T.TIMELINE;
import static works.lysenko.util.chrs.____.TEST;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static works.lysenko.util.spec.Symbols._DOT_;
import static works.lysenko.util.chrs.__.BE;
import static works.lysenko.util.chrs.__.NO;
import static works.lysenko.util.chrs.__.OF;
import static works.lysenko.util.chrs.__.TO;
import static works.lysenko.util.chrs.___.HAD;
import static works.lysenko.util.chrs.___.SET;
import static works.lysenko.util.chrs.____.WERE;
import static works.lysenko.util.chrs.____.WITH;
import static works.lysenko.util.chrs.____.PATH;
import static works.lysenko.util.data.strs.Case.u;
import static works.lysenko.util.lang.word.A.ACTUALLY;
import static works.lysenko.util.lang.word.A.APPLIED;
import static works.lysenko.util.lang.word.C.CHANCE;
import static works.lysenko.util.lang.word.C.CLOSING;
import static works.lysenko.util.lang.word.C.COMMON;
import static works.lysenko.util.lang.word.C.COMPLETED;
import static works.lysenko.util.lang.word.C.CONFIGURATION;
import static works.lysenko.util.lang.word.C.CURRENT;

import static works.lysenko.util.lang.word.E.EVENT;
import static works.lysenko.util.lang.word.E.EVENTS;
import static works.lysenko.util.lang.word.E.ERROR;
import static works.lysenko.util.lang.word.E.EXECUTED;
import static works.lysenko.util.lang.word.E.EXECUTING;
import static works.lysenko.util.lang.word.E.EXECUTION;
import static works.lysenko.util.lang.word.F.FAILURE;
import static works.lysenko.util.lang.word.F.FAILED;
import static works.lysenko.util.lang.word.P.POSSIBLE;
import static works.lysenko.util.lang.word.R.RESULTS;
import static works.lysenko.util.lang.word.S.SCENARIO;
import static works.lysenko.util.lang.word.S.SCENARIOS;
import static works.lysenko.util.lang.word.S.SERVICE;
import static works.lysenko.util.lang.word.S.SESSION;
import static works.lysenko.util.lang.word.S.SUCCESSFULLY;
import static works.lysenko.util.lang.word.S.SUMMARY;
import static works.lysenko.util.lang.word.S.SEVERE;
import static works.lysenko.util.lang.word.W.WARNING;
import static works.lysenko.util.spec.Symbols._SPACE_;
import static works.lysenko.util.spec.Symbols._EQUAL_;
import static works.lysenko.util.spec.Symbols._COLON_;
import static works.lysenko.util.spec.Symbols._NUMBR_;
import static works.lysenko.util.spec.Symbols.OPN_SBR;
import static works.lysenko.util.spec.Symbols.CLS_SBR;
import static works.lysenko.util.spec.Symbols.RHT_ARR;
import static works.lysenko.util.spec.Symbols._COMMA_;

import static works.lysenko.util.spec.Symbols.A;


/**
 * Parses execution log files to extract structured information about test execution.
 */
@SuppressWarnings({"UtilityClassCanBeEnum", "PublicInnerClass", "StringTrim", "StringEquals", "StringEqualsIgnoreCase", "StringToLowerCase", "ObjectAllocationInLoop", "HardcodedStringLiteral", "NonInternationalizedStringLiteral", "DynamicRegexReplaceableByCompiledPattern", "RegExpRedundantEscape", "RegExpUnexpectedAnchor"})
public final class LogFileParser {

    private static final Pattern ANSI_PATTERN = Pattern.compile("\\x1b\\[[0-9;]*m");
    private static final Pattern LINE_WITH_TEST_RE = Pattern.compile("^\\[\\s*(\\d+)\\s*\\]\\[\\s*(\\d+)\\s*\\]\\[([^\\]]+)\\](?:\\[([^\\]]*)\\])?(.*)$");
    private static final Pattern LINE_NO_TEST_RE = Pattern.compile("^(?:\\[\\s*\\])?\\[\\s*(\\d+)\\s*\\]\\[([^\\]]+)\\](?:\\[([^\\]]*)\\])?(.*)$");
    @SuppressWarnings("HardcodedFileSeparator")
    private static final Pattern PATH_LINE_RE = Pattern.compile(s("\\[\\s*(\\d+)\\s*\\]\\s*\\[\\s*([\\d,]+)\\s*\\]\\s*", RHT_ARR, "\\s*(.*)"));
    private static final Pattern SCEN_LINE_RE = Pattern.compile("([▷◼◆●])\\s+([a-zA-Z0-9_]+(?:\\.[a-zA-Z0-9_]+)*)[.\\s]+(.+?)\\s+(\\d+)(?:\\s*\\(([^)]+)\\))?$");
    private static final Pattern TEST_RUN_SUMMARY_RE = Pattern.compile(".*\\b\\d+\\s+tests?\\s+of\\s+.+\\s+done\\s+in\\s+.*");
    private static final Pattern DIGITS_RE = Pattern.compile("\\d+");
    private static final Pattern DOT_END_RE = Pattern.compile("\\.+$");


    /**
     * Extracts bracketed string from a cleaned line.
     *
     * @param clean the cleaned line
     * @return the extracted string
     */
    private static String extractBracketStr(final String clean) {
        return clean.contains(s(CLS_SBR)) ? clean.substring(clean.lastIndexOf(CLS_SBR) + 1).trim() : clean;
    }


    private static boolean isPostflightEvent(final String clean) {
        return clean.contains(b(c(CLOSING), TEST, SERVICE)) || clean.contains(b(c(EVENT), SUMMARY)) || clean.contains(b(c(EVENTS), SUMMARY)) || clean.contains(c(POSTFLIGHT)) || clean.contains(b(c(TEST), SESSION, COMPLETED));
    }

    private static boolean isValidScenarioToken(final String sc, final LogModels.LogSection currentSec) {
        return !sc.isEmpty() && !SCENARIO.equalsIgnoreCase(sc) && !sc.equalsIgnoreCase(c(SPECIFICS)) && !sc.toLowerCase(Locale.ROOT).endsWith(s(_DOT_, SPECIFICS)) && !currentSec.scenarios.contains(sc);
    }
    
    private LogFileParser() {
    }

    /**
     * Represents the result of parsing a log file.
     */
    public static class ParseResult {
        /** List of log sections. */
        public final List<LogModels.LogSection> sections = new ArrayList<>(16);
        /** List of test paths. */
        public final List<LogModels.PathEntry> testPaths = new ArrayList<>(16);
        /** List of scenario statistics. */
        public final List<LogModels.ScenEntry> scenStats = new ArrayList<>(16);
        /** Execution configuration string. */
        public String execConfigStr = EMPTY;
        /** Paths possible string. */
        public String pathsPossibleStr = EMPTY;
        /** Paths chance string. */
        public String pathsChanceStr = EMPTY;
        /** Paths executed string. */
        public String pathsExecutedStr = EMPTY;
        /** Common path steps. */
        public final List<String> commonPathSteps = new ArrayList<>(16);
        /** Plaque status. */
        public String plaqueStatus = null;
        /** Plaque message. */
        public String plaqueMessage = null;
    }

    /**
     * Parses the log file from the given reader.
     *
     * @param reader    the reader for the log file
     * @param logFile   the original log file
     * @param lineStore the line store for lines
     * @param progress  the processing progress tracker
     * @return the parsed result
     * @throws IOException if an I/O error occurs
     */
    public static ParseResult parse(final BufferedReader reader, final File logFile, final LogModels.LineStore lineStore, final ProcessingProgress progress) throws IOException {
        final ParseResult result = new ParseResult();
        final String task = b(c(RUN), TIMELINE, REPORT);
        final ProcessingProgress prg = null != progress ? progress : ProcessingProgress.NONE;
        final long totalBytes = (null != logFile && logFile.exists()) ? logFile.length() : 0;
        long bytesRead = 0;
        int lastPct = -1;

        LogModels.LogSection currentSec = new LogModels.LogSection(BOOTING, c(BOOTING), NEUTRAL, lineStore);

        int prevTestNum = 0;
        boolean inPostflight = false;
        boolean inScenStats = false;


        final Matcher mAnsi = ANSI_PATTERN.matcher("");
        final Matcher mPath = PATH_LINE_RE.matcher("");
        final Matcher mDigits = DIGITS_RE.matcher("");
        final Matcher mScen = SCEN_LINE_RE.matcher("");
        final Matcher mDotEnd = DOT_END_RE.matcher("");
        final Matcher mTest = LINE_WITH_TEST_RE.matcher("");
        final Matcher mNoTest = LINE_NO_TEST_RE.matcher("");
        final Matcher mScenToken = SCEN_TOKEN_RE.matcher("");
        final Matcher mTestRunSummary = TEST_RUN_SUMMARY_RE.matcher("");

        String line = reader.readLine();
        while (null != line) {
            bytesRead += line.getBytes(StandardCharsets.UTF_8).length + 1;
            if (0 < totalBytes) {
                final int pct = Math.toIntExact(Math.min(75L, (bytesRead * 75L) / totalBytes));
                if (pct != lastPct) {
                    lastPct = pct;
                    prg.update(task, pct);
                }
            }
            final String clean = mAnsi.reset(line).replaceAll(EMPTY).trim();
            if (clean.isEmpty()) {
                currentSec.lines.add(line);
                line = reader.readLine();
                continue;
            }

            mPath.reset(clean);
            if (mPath.find()) {
                final String tNum = mPath.group(1);
                final String tDur = mPath.group(2).replace(s(_COMMA_), EMPTY);
                final double durSec = mDigits.reset(tDur).matches() ? Double.parseDouble(tDur) / 1000.0 : 0.0;
                final List<String> steps = new ArrayList<>(16);
                for (final String s : mPath.group(3).split(s(RHT_ARR))) {
                    final String stepTrim = s.trim();
                    if (!stepTrim.isEmpty() && !stepTrim.equalsIgnoreCase(c(SPECIFICS)) && !stepTrim.toLowerCase(Locale.ROOT).endsWith(s(_DOT_, SPECIFICS))) {
                        steps.add(stepTrim);
                    }
                }
                result.testPaths.add(new LogModels.PathEntry(tNum, String.format(Locale.ROOT, "%.3fs", durSec), steps));
            }

            if (clean.contains("Scenarios statistics") || clean.contains("Scenario statistics")) {
                inScenStats = true;
            }

            if (inScenStats) {
                mScen.reset(clean);
                if (mScen.find()) {
                    final String sym = mScen.group(1);
                    final String name = mDotEnd.reset(mScen.group(2)).replaceAll("").trim();
                    final String weight = mScen.group(3).trim();
                    final String count = mScen.group(4);
                    final String events = null != mScen.group(5) ? mScen.group(5).trim() : EMPTY;
                    result.scenStats.add(new LogModels.ScenEntry(sym, name, weight, count, events));
                } else if ((clean.startsWith("=") || clean.contains("screenshot") || isPostflightEvent(clean)) && !clean.contains("Scenarios statistics")) {
                    inScenStats = false;
                }
            }

            if (clean.contains(s(c(EXECUTING), _SPACE_)) && clean.contains(s(_SPACE_, OF, _SPACE_))) {
                final int idx = clean.indexOf(s(c(EXECUTING), _SPACE_));
                result.execConfigStr = clean.substring(idx).trim();
            }

            if (clean.contains(b(POSSIBLE, WITH, CURRENT, SET, OF, c(SCENARIOS))) || clean.contains(b(POSSIBLE, WITH, CURRENT, SET, OF, SCENARIOS))) {
                result.pathsPossibleStr = extractBracketStr(clean);
            }

            if (clean.contains(b(HAD, s(A), CHANCE, TO, BE, EXECUTED))) {
                result.pathsChanceStr = extractBracketStr(clean);
            }

            if (clean.contains(b(WERE, ACTUALLY, EXECUTED)) || clean.contains(b(WERE, EXECUTED))) {
                result.pathsExecutedStr = extractBracketStr(clean);
            }

            if (clean.contains(b(c(COMMON), s(PATH, _COLON_)))) {
                final String raw = clean.substring(clean.indexOf(b(c(COMMON), s(PATH, _COLON_))) + b(c(COMMON), s(PATH, _COLON_)).length()).trim();
                result.commonPathSteps.clear();
                for (final String s : raw.split(s(RHT_ARR))) {
                    final String stepTrim = s.trim();
                    if (!stepTrim.isEmpty()) {
                        result.commonPathSteps.add(stepTrim);
                    }
                }
            }

            final int eqIdx = clean.indexOf(s(_EQUAL_, _SPACE_));
            if (0 <= eqIdx) {
                final String candidate = clean.substring(eqIdx).trim();
                if (candidate.startsWith(s(_EQUAL_, _SPACE_, OPN_SBR, u(FAILURE), CLS_SBR)) || candidate.startsWith(s(_EQUAL_, _SPACE_, OPN_SBR, u(ERROR), CLS_SBR)) || candidate.startsWith(s(_EQUAL_, _SPACE_, OPN_SBR, u(WARNING), CLS_SBR))) {
                    result.plaqueStatus = FAILED;
                    result.plaqueMessage = candidate;
                } else if (candidate.startsWith(s(_EQUAL_, _SPACE_, b(c(EXECUTION), PASSED, SUCCESSFULLY)))) {
                    result.plaqueStatus = PASSED;
                    result.plaqueMessage = candidate;
                } else if (candidate.startsWith(s(_EQUAL_, _SPACE_, b(c(NO), TEST, RESULTS)))) {
                    result.plaqueStatus = NEUTRAL;
                    result.plaqueMessage = candidate;
                }
            }

            final boolean isConfigEvent = clean.contains("Reading Common Configuration") || clean.contains("Reading Parameters") || clean.startsWith("# Applied test configuration");

            if (isConfigEvent && BOOTING.equals(currentSec.type)) {
                currentSec.lines.add(line);
                if (!currentSec.lines.isEmpty()) result.sections.add(currentSec);
                currentSec = new LogModels.LogSection(CONFIGURING, c(CONFIGURING), NEUTRAL, lineStore);
                line = reader.readLine();
                continue;
            }

            if (!inPostflight && isPostflightEvent(clean)) {
                inPostflight = true;
                if (!currentSec.lines.isEmpty()) result.sections.add(currentSec);
                currentSec = new LogModels.LogSection(POSTFLIGHT, c(POSTFLIGHT), NEUTRAL, lineStore);
            }

            mTest.reset(clean);
            mNoTest.reset(clean);
            final boolean testMatches = mTest.matches();
            final boolean noTestMatches = !testMatches && mNoTest.matches();

            if (inPostflight) {
                if (noTestMatches) {
                    final int opNum = Integer.parseInt(mNoTest.group(1));
                    final String ts = mNoTest.group(2);
                    if (null == currentSec.startOp) {
                        currentSec.startOp = opNum;
                        currentSec.startTime = ts;
                    }
                    currentSec.endOp = opNum;
                    currentSec.endTime = ts;
                }
            } else {
                if (testMatches) {
                    final int tNum = Integer.parseInt(mTest.group(1));
                    final int opNum = Integer.parseInt(mTest.group(2));
                    final String ts = mTest.group(3);

                    if (!TEST.equals(currentSec.type) || currentSec.testNum != tNum) {
                        if (!currentSec.lines.isEmpty()) result.sections.add(currentSec);
                        currentSec = new LogModels.LogSection(TEST, s(c(TEST), _SPACE_, _NUMBR_, _SPACE_, tNum), PASSED, lineStore);
                        currentSec.testNum = tNum;
                        currentSec.startOp = opNum;
                        currentSec.startTime = ts;
                        prevTestNum = tNum;
                    }
                    currentSec.endOp = opNum;
                    currentSec.endTime = ts;
                } else if (noTestMatches) {
                    final int opNum = Integer.parseInt(mNoTest.group(1));
                    final String ts = mNoTest.group(2);

                    if (BOOTING.equals(currentSec.type) || CONFIGURING.equals(currentSec.type)) {
                        if (!currentSec.lines.isEmpty()) result.sections.add(currentSec);
                        currentSec = new LogModels.LogSection(PREFLIGHT, c(PREFLIGHT), NEUTRAL, lineStore);
                        currentSec.startOp = opNum;
                        currentSec.startTime = ts;
                    } else if (0 < prevTestNum && !LIMBO.equals(currentSec.type) && !POSTFLIGHT.equals(currentSec.type)) {
                        if (!currentSec.lines.isEmpty()) result.sections.add(currentSec);
                        currentSec = new LogModels.LogSection(LIMBO, c(LIMBO), NEUTRAL, lineStore);
                        currentSec.prevTestNum = prevTestNum;
                        currentSec.startOp = opNum;
                        currentSec.startTime = ts;
                    } else {
                        if (null == currentSec.startOp) {
                            currentSec.startOp = opNum;
                            currentSec.startTime = ts;
                        }
                    }
                    currentSec.endOp = opNum;
                    currentSec.endTime = ts;
                }
            }

            if (clean.contains(s(OPN_SBR, u(WARNING), CLS_SBR)) && !FAILED.equals(currentSec.status)) {
                currentSec.status = WARNING;
            } else if (clean.contains(s(OPN_SBR, u(SEVERE), CLS_SBR)) || clean.contains(s(OPN_SBR, u(FAILURE), CLS_SBR))) {
                currentSec.status = FAILED;
            }

            if (TEST.equals(currentSec.type)) { // suppressed by logic
                mScenToken.reset(clean);
                if (mScenToken.find()) {
                    final String sc = mScenToken.group(2).trim();
                    if (isValidScenarioToken(sc, currentSec)) {
                        currentSec.scenarios.add(sc);
                    }
                }
            }

            currentSec.lines.add(line);

            mTestRunSummary.reset(clean);
            if (!inPostflight && mTestRunSummary.matches()) {
                inPostflight = true;
                result.sections.add(currentSec);
                currentSec = new LogModels.LogSection(POSTFLIGHT, c(POSTFLIGHT), NEUTRAL, lineStore);
            }
            line = reader.readLine();
        }
        if (!currentSec.lines.isEmpty()) result.sections.add(currentSec);
        
        return result;
    }
}
