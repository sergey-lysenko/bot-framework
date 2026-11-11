# 📌 Task: Rename "Stack Trace" → "Call Stack" in `bot-framework`

## 🎯 Context & Background
In `bot-framework`, call site references (e.g., `[#78]` mapping to `[Root.log:907 Exec.createBot:368 ...]`) record which code location emitted a log message. They are **compact one-liner call stacks**, not error exception stack traces.

To align terminology across `bot-framework` and `bot-analytics`, we are standardizing the terminology from **"Stack Trace / stacktrace"** to **"Call Stack / callstack"**.

`bot-analytics` has already been updated with dual-read compatibility to parse both `.callstacks.properties` and legacy `.stacktraces.properties` sidecar files without breaking backwards compatibility.

---

## 📋 Required Changes in `bot-framework`

### 1. Rename Sidecar File Constants
- **Files:** `src/main/java/works/lysenko/util/spec/Layout.java` (and layout template definitions)
- **Change:** Rename template identifier for sidecar properties files:
  - From: `.stacktraces.properties` (`RUN_STACKTRACES_`)
  - To: `.callstacks.properties` (`RUN_CALLSTACKS_`)

### 2. Create `Callstack.java` & Alias `Stacktrace.java`
- **File:** `src/main/java/works/lysenko/util/func/core/Stacktrace.java`
- **Change:**
  - Create `Callstack.java` in package `works.lysenko.util.func.core`.
  - Refactor logic from `Stacktrace.java` into `Callstack.java`.
  - Keep `Stacktrace.java` as a `@Deprecated` delegate calling `Callstack` methods so existing test projects (`BiteHeist-test`, `rozkladach-test`) compile without breaking changes.

### 3. Update Logging Data Models & Processors
- **Files:**
  - `src/main/java/works/lysenko/util/data/type/logr/AbstractLogData.java`
  - `src/main/java/works/lysenko/base/logger/Processor.java`
  - `src/main/java/works/lysenko/base/Logs.java`
- **Change:**
  - Update internal field names and parameters from `stacktrace` → `callstack`.
  - Keep deprecated accessor methods (`public String stacktrace() { return callstack(); }`) for binary backwards-compatibility.

### 4. Update Log Writer Header Documentation
- **File:** `src/main/java/works/lysenko/base/logger/processor/Writer.java`
- **Change:**
  - Update header docstring:
    ```markdown
    > - **Call Stack**: Call site references formatted as `ID:[Class.method:line]`.
    ```

### 5. Update HTML Log Output Generators
- **Files:**
  - `src/main/java/works/lysenko/base/output/loghtml/LogParser.java`
  - `src/main/java/works/lysenko/base/logger/renderer/AnsiLogRenderer.java`
  - `src/main/java/works/lysenko/base/logger/renderer/MarkdownLogRenderer.java`
- **Change:**
  - Update template placeholders to `{{CALLSTACKS_LINK}}` (with fallback support for `{{STACKTRACES_LINK}}`).

---

## ✅ Verification & Testing
1. Run `mvn test` in `bot-framework` to verify clean compilation and passing unit tests.
2. Execute a local test run and verify that `result/runs/<suite>/<runId>/<runId>.callstacks.properties` is generated on disk.
3. Refresh or ingest the run in `bot-analytics` to confirm seamless parsing and UI visualization under the updated name.
