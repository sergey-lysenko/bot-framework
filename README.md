# bot-framework

> **Autonomous, state-driven test automation engine engineered for extreme execution resilience across Web and Android platforms.**

[![Java](https://img.shields.io/badge/Java-17%2B-blue.svg)](https://openjdk.org/)
[![Maven](https://img.shields.io/badge/Maven-Build-green.svg)](https://maven.apache.org/)
[![Selenium](https://img.shields.io/badge/Selenium-4.30.0-orange.svg)](https://www.selenium.dev/)
[![Appium](https://img.shields.io/badge/Appium-9.4.0-purple.svg)](https://appium.io/)
[![Upwork]([[https://img.shields.io/badge/Upwork-Verified%20Top%20Rated%20Plus-14a800.svg](https://img.shields.io/badge/Upwork-Expert%20Vetted-14a800.svg)](https://img.shields.io/badge/Upwork-Expert%20Vetted-14a800.svg))](https://www.upwork.com/freelancers/alasan)

---

## 💡 Philosophy & Motivation

Most test automation suites fail not because of product defects, but because of **execution fragility**:
- Brittle locator strategies broken by subtle UI restyling.
- Rigid, linear Page Object scripts derailed by unexpected dialogs, system popups, or layout shifts.
- Flaky network, driver, and port deadlocks in CI environments.

`bot-framework` takes a fundamentally different path. Built upon **20+ years of hands-on test automation architecture**, it models user interfaces as **dynamic state trees** rather than linear page sequences. Instead of scripted rails, autonomous execution bots traverse states, dynamically adapt to UI conditions, self-heal, and isolate failures with surgical telemetry.

---

## ⚡ Core Architectural Pillars

### 1. Unified Interaction Tree (`works.lysenko.tree.Root`)
A single, consistent interaction layer that abstracts both **Web** (Selenium) and **Mobile** (Appium / Android) drivers:
- **Polymorphic UI Operations**: Methods like `clickOnButton()`, `waitForVisibilityOf()`, and `waitThenClickOn()` transparently resolve whether the underlying target is an Android Native View (`android.widget.Button`) or a Web DOM node.
- **Atomic State Transitions**: Every user action is verified against wait predicates before and after execution to prevent race conditions.

### 2. Autonomous Bot & Scenario State Modeling (`works.lysenko.base`)
- **State Discovery**: UI elements and interaction targets are dynamically collected and evaluated based on visibility, depth, and accessibility.
- **Exploratory & Scenario Modes**: Supports both deterministic goal-driven scenario execution and autonomous tree-exploration bots designed to stress-test complex workflows.
- **`ALL_LEAFS` Coverage Engine**: Goal-driven execution algorithm that dynamically calculates completion deficits across scenario branches, applies branch completion weights (`.tree.completion.weight`), and continues execution until 100% of accessible leaf scenarios are covered.

### 3. Fail-Safe Runtime & Self-Healing
- **Automatic Port & Resource Recovery**: Intelligently handles driver port collisions (such as recovering busy Appium instances on the fly) and restarts dead sessions cleanly.
- **Deterministic Wait Machinery**: Eliminates arbitrary `sleep` calls with adaptive expected-condition cycles (`wdw().until(...)`).
- **Telemetry & Artifact Capture**: Automated full-screen and cropped screenshot generation, bounding-box geometry logging, color-cutoff analysis, and CPU/RAM telemetry sampling.

### 4. Zero-Compromise Purity
- **Bit-Level String Synthesis (`works.lysenko.util.spec.Symbols`)**: Internal strings, control characters, and symbols are constructed bit-by-bit from boolean matrices to avoid external encoding dependencies.
- **Surrogate Isolation (`works.lysenko.util.spec.Surrogates`)**: Clean architectural separation between single-unit 16-bit BMP characters and multi-unit UTF-16 surrogate pictograms/emojis.

---

## 🛠️ Control Panel & Embedded Micro-IDE

The framework features a built-in Swing-based **Control Panel** (`works.lysenko.base.ui.ControlPanel`) providing interactive management across five primary tabs:

1. **Config & Tests (Micro-IDE)**:
   - **Project Explorer**: Tree-based navigation for test suites (`*.test`), test partials (`*.part`), and global configuration files.
   - **Real-Time Property Validation**: Highlights invalid property keys (`⚠️ INVALID`), detects container scope violations, and suggests corrections for misspelled keys (e.g., suggesting `.test.tests` for `.test.test`).
   - **Unsaved Changes Tracking**: Prompts users to save modifications upon losing focus, switching files, or switching tabs.
   - **Preservation Assurance**: Invalid or custom properties are clearly flagged while guaranteeing they are preserved on disk when saved.
2. **Run Setup**:
   - Graphical controls for selecting test targets, user pools, domains, platforms (Web/Android/iOS), headless execution, and execution parameters (`TESTS`, `ALL_LEAFS`, `ALL_LEAFS_EXECUTIONS`, `RESILIENT_MODE`, `COMPLETION_WEIGHT`, `TRAVERSE_EXTENSIONS`).
   - Live status tracking (`Default`, `Configured`, `Modified`) for every property against default baselines.
3. **Execution Dashboard**:
   - Live controls (`Pause`, `Resume`, `Stop`, `Halt`, `Step`), real-time CPU/RAM meters, execution duration counters, and active scenario breadcrumb trails.
4. **Tree Progression**:
   - Interactive zoomable/pannable rendering of the live scenario execution tree with SVG card visualizers.
5. **Known Issues**:
   - GUI editor for `known/issues.json` to configure expected defects, regular expression failure patterns, and bug tracker links.

---

## 📁 Repository Structure

```
src/main/resources/
├── config/                 # Modular configuration directory
│   ├── common.configuration.properties # Master config include file
│   ├── property-help.properties       # Complete property documentation dictionary
│   └── parts/              # Categorized configuration partials (.part)
│       ├── data.part       # Data storage & serialization delimiters
│       ├── debug.part      # Appium diagnostics, debug logs, and tracing
│       ├── fractions.part  # Math precision and fraction formatting
│       ├── images.part     # Color grid, screenshot triggers, and swipe markers
│       ├── log.part        # Log formatting, similarity matching, and rates
│       ├── project.part    # Scenario root package and application paths
│       ├── scenarios.part  # Tree safeguards, sufficiency, and weights
│       ├── stacktrace.part # Stack trace shortening and exclusions
│       ├── tests.part      # Execution controls, progression, and sonification
│       ├── ui.part         # Dashboard display and mobile native resolutions
│       └── webdriver.part  # WebDriver wait timeouts and platform options
├── known/
│   └── issues.json         # Per-scenario known issues and defect mapping
└── tests/                  # Test suite definitions (*.test) and suite parts
    └── parts/

src/main/java/works/lysenko/
├── Base.java               # Global execution context, property lookups, and utilities
├── base/                   # Core runtime machinery
│   ├── core/               # Engine execution loop and lifecycles (BootCapture)
│   ├── exec/               # Driver executors (Web, Android) and session managers
│   ├── logger/             # Structured ANSI logging and reporting
│   ├── parameters/         # Execution parameters, CLI flags, and property validation
│   ├── properties/         # Runtime properties loader
│   ├── telemetry/          # Screenshots, diffing, and visual telemetry
│   └── ui/                 # Control Panel GUI & Micro-IDE components
├── tree/                   # State-tree navigation and Root action dispatch
│   ├── Root.java           # Primary interaction API (clicks, waits, scrolls, types)
│   └── base/               # Tree node abstractions
└── util/                   # Framework utilities
    ├── apis/               # Modular capability interfaces (Clicks, Waits, Clears)
    ├── func/               # Geometric, color, visual, and locator functions
    ├── lang/               # Lexicon and linguistic building blocks
    └── spec/               # Bits, symbols, and surrogate definitions
```

---

## 🚀 Getting Started

### Prerequisites
- **Java**: OpenJDK 17 or higher (Java 21 supported)
- **Maven**: 3.8+
- **Platform Drivers**:
  - *Web*: ChromeDriver / GeckoDriver (or Selenium Manager managed)
  - *Android*: Android SDK (`adb`), Appium 2.x server running locally or remotely
  - *Media Processing (Optional)*: FFmpeg (for MP4 scenario progression videos)

### Building

```bash
mvn clean install -DskipTests
```

### Dependency Usage

To consume `bot-framework` in your test suite, include it in your `pom.xml`:

```xml
<dependency>
  <groupId>works.lysenko</groupId>
  <artifactId>framework</artifactId>
  <version>0.26.43.23</version>
</dependency>
```

---

## ⚙️ Configuration & Execution

Configuration is organized into 11 specialized partial files included via `src/main/resources/config/common.configuration.properties`:

| Category | Key / Property | Default | Description |
|---|---|---|---|
| **Data** | `.data.delimiter.l0` | `\|` | Top-level entry delimiter for framework serialization |
| **Data** | `.data.persist` | `false` | Preserves test data across test runs |
| **Debug** | `.logs.adebug` | `false` | Enables verbose Appium diagnostic logging |
| **Debug** | `.logs.debug` | `false` | Enables additional framework diagnostic output |
| **Images** | `.grid.colour.similarity.threshold` | `1.0` | Color similarity comparison threshold |
| **Images** | `.screenshots.of.clicks` | `false` | Captures screenshots after every click action |
| **Images** | `.swipes.marker.line.colour` | `0,0,0,128` | RGBA color for drawing swipe paths on screenshots |
| **Log** | `.logs.lines.similarity.check` | `true` | Enables grouping and deduplication of similar log lines |
| **Log** | `.logs.silent.sleeping.threshold` | `10` | Sleep durations under this ms threshold do not log messages |
| **Project** | `.test.root` | `link.tree` | Root package for scenario tree discovery |
| **Project** | `.test.app` | `""` | Path to target Android APK file |
| **Scenarios** | `.tree.completion.weight` | `4.5` | Weight boosting unfinished scenario branches |
| **Scenarios** | `.tree.scenario.depth.safeguard` | `20` | Maximum scenario tree depth safeguard threshold |
| **Stacktrace**| `.logs.full.stacktrace` | `false` | Includes unshortened stacktraces in log output |
| **Tests** | `.test.action.retries` | `3` | Maximum retry attempts for UI state actions |
| **Tests** | `.test.all.leafs` | `false` | Enables ALL_LEAFS coverage mode |
| **Tests** | `.test.all.leafs.executions` | `1` | Target execution count for each leaf scenario in ALL_LEAFS mode |
| **Tests** | `.test.fail.by.event.of.severity` | `s1` | Event severity level that halts test execution |
| **Tests** | `.test.resilient.mode` | `false` | Enables resilient mode (executes full suite despite failures) |
| **Tests** | `.test.report.progression.tree.mp4` | `true` | Generates MP4 video of scenario tree progression (FFmpeg) |
| **Tests** | `.test.report.progression.tree.sonification` | `none` | Adds deterministic musical audio to progression MP4 (`none`, `copilot`, `claude`, `gemini`) |
| **UI** | `.screen.to.spawn.dashboard` | `0` | Screen index on which to open the dashboard |
| **UI** | `.mobi.native.x.resolution` | `1080` | Mobile native screen width in pixels |
| **WebDriver**| `.test.ewait` | `30` | Explicit wait timeout in seconds |
| **WebDriver**| `.webd.iwait` | `1000` | Implicit WebDriver element location wait in milliseconds |

The framework uses OS-backed `run/.run.lock` and `run/.postprocess.lock` files to prevent a new test run from starting while a test or its detached post-processor is active.

### Applied Test Configuration Visual Legend

When executing tests, the framework logs the active test parameters under `# Applied test configuration`. The visual indicators and color-coding convey property origin, defaults, and custom overrides:

- **Presence in the list**: Indicates properties currently active and applied for the test execution.
- **Color Coding**:
  - **Blue Property Name + Yellow Value**: Highlights properties whose values have been **redefined / modified** from standard schema defaults (`value != def`).
  - **Magenta Property Name + Magenta Value**: Highlights properties whose values were **overridden by execution parameters, JVM system properties, or CLI flags** (`value != configFileDefaults`).
  - **Gray (Plain Text)**: Indicates properties whose values **match their default values** (`value == def`).
- **Prefix Markers**:
  - **`•` Bullet Present**: Indicates the property value originates from or matches the **common configuration** (`common.configuration.properties`).
  - **`*` Asterisk Present**: Indicates the property value was **redefined by an execution parameter, JVM `-D` flag, or runtime override** (`var/parameters`).
  - **No Marker**: Indicates the property value was overridden by a **test-suite file** (`*.suite`).
- **Green `" = "` with Spaces**:
  - **Green `" = "` with spaces**: Indicates a custom, ad-hoc, or non-standard property that does **not** have a default value defined in the framework's property schema (`isNull(def)`).
  - **Plain `=` without spaces**: Indicates a standard framework property recognized by the schema (`def != null`).

#### Parameter & Property Precedence Order
When resolving configuration, `bot-framework` evaluates properties in strict precedence order (highest to lowest):
1. **JVM System Properties / CLI Flags** (e.g. `-D.test.resilient.mode=true` or `-DRESILIENT_MODE=true`)
2. **Runtime Execution Parameters** (`var/parameters`)
3. **Test Suite & Suite Part Overrides** (`*.suite`, `*.part`)
4. **Common Configuration Partials** (`src/main/resources/config/*.part`)
5. **Master Schema Defaults** (`PropEnum`)

---

## 🎬 Progression Videos & Deterministic Sonification

When `.test.report.progression.tree.mp4 = true`, the framework uses FFmpeg to compile frame-by-frame animations of the scenario tree evolution during test runs.

### Deterministic Audio Sonification Modes (`.test.report.progression.tree.sonification`)
- **`none`** *(Default)*: Silent video generation.
- **`copilot`**: Leaf-driven audio tracks where scenario tree path depth determines pitch and target execution progress drives dynamics.
- **`claude`**: Visual diff-driven audio tracks where image changes between frames control note selection and volume.
- **`gemini`**: Ambient structural cascade reflecting macro-level scenario tree layout transformations.

---

## 🐞 Per-Node Known Issues Tracking

The framework supports per-node Known Issues mapping. This allows test runs to track expected failures, staging defects, or known environment issues directly against specific scenarios in the interactive `TreeHtml` report.

### `known/issues.json` Schema

Known issues are configured in `src/main/resources/known/issues.json`:

```json
{
  "issues": [
    {
      "scenario": "com.biteheist.scenarios.menu.SelectCategory",
      "title": "Staging Auth Timeout",
      "description": "Auth service failure on staging under load.",
      "link": "https://jira.company.com/browse/BH-1234",
      "pattern": ".*TimeoutException.*"
    },
    {
      "scenario": "com.biteheist.scenarios.checkout.*",
      "title": "Payment Gateway Maintenance",
      "description": "Payment service undergoing scheduled maintenance on staging.",
      "link": "https://github.com/org/repo/issues/567"
    }
  ]
}
```

### Key Capabilities

- **Flexible Scenario Addressing**: Match scenarios by Fully Qualified Class Name (FQN), simple class name, or package wildcards (e.g. `com.biteheist.scenarios.checkout.*`).
- **Selective Pattern Matching (`pattern`)**: Optional regular expression evaluated against failure log messages or stacktraces. The issue is only attached if the failure log matches the pattern, preventing misclassification of new or unexpected defects.
- **Interactive TreeHtml Integration**:
  - Nodes with active known issues render a `🐞` bug badge indicator on SVG cards.
  - Interactive details panel displays full issue titles, descriptions, and clickable tracker links.
  - Execution summary plaque highlights known issue statistics across test runs.

---

## 👨‍💻 Author & Consulting

Architected and developed by **Sergii Lysenko** — Principal Test Automation Architect with 20+ years of experience designing resilient test engines, distributed CI infrastructures, and autonomous quality systems for desktop, mobile, and web applications.

- **Upwork Profile**: [Sergii Lysenko on Upwork](https://www.upwork.com/freelancers/alasan) *(Client reviews, contract track record, and verified project outcomes)*
- **Contact & Portfolio**: Available for select high-impact technical consulting, architecture reviews, and automation foundations.

---

## 📄 License

All rights reserved. See individual license terms for commercial engagement.
