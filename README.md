# bot-framework

> **Autonomous, state-driven test automation engine engineered for extreme execution resilience across Web and Android platforms.**

[![Java](https://img.shields.io/badge/Java-17%2B-blue.svg)](https://openjdk.org/)
[![Maven](https://img.shields.io/badge/Maven-Build-green.svg)](https://maven.apache.org/)
[![Selenium](https://img.shields.io/badge/Selenium-4.30.0-orange.svg)](https://www.selenium.dev/)
[![Appium](https://img.shields.io/badge/Appium-9.4.0-purple.svg)](https://appium.io/)
[![Upwork](https://img.shields.io/badge/Upwork-Verified%20Top%20Rated%20Plus-14a800.svg)](https://www.upwork.com/freelancers/alasan)

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

### 2. Autonomous Bot & State Modeling (`works.lysenko.base`)
- **State Discovery**: UI elements and interaction targets are dynamically collected and evaluated based on visibility, depth, and accessibility.
- **Exploratory & Scenario Modes**: Supports both deterministic goal-driven scenario execution and autonomous tree-exploration bots designed to stress-test complex workflows.

### 3. Fail-Safe Runtime & Self-Healing
- **Automatic Port & Resource Recovery**: Intelligently handles driver port collisions (such as recovering busy Appium instances on the fly) and restarts dead sessions cleanly.
- **Deterministic Wait Machinery**: Eliminates arbitrary `sleep` calls with adaptive expected-condition cycles (`wdw().until(...)`).
- **Telemetry & Artifact Capture**: Automated full-screen and cropped screenshot generation, bounding-box geometry logging, and color-cutoff analysis.

### 4. Zero-Compromise Purity
- **Bit-Level String Synthesis (`works.lysenko.util.spec.Symbols`)**: Internal strings, control characters, and symbols are constructed bit-by-bit from boolean matrices to avoid external encoding dependencies.
- **Surrogate Isolation (`works.lysenko.util.spec.Surrogates`)**: Clean architectural separation between single-unit 16-bit BMP characters and multi-unit UTF-16 surrogate pictograms/emojis.

---

## 📁 Repository Structure

```
src/main/java/works/lysenko/
├── Base.java               # Global execution context, property lookups, and utilities
├── base/                   # Core runtime machinery
│   ├── core/               # Engine execution loop and lifecycles
│   ├── exec/               # Driver executors (Web, Android) and session managers
│   ├── logger/             # Structured ANSI logging and reporting
│   ├── parameters/         # Execution parameters and CLI flags
│   ├── properties/         # Runtime properties loader
│   └── telemetry/          # Screenshots, diffing, and visual telemetry
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
- **Java**: OpenJDK 17 or higher
- **Maven**: 3.8+
- **Platform Drivers**:
  - *Web*: ChromeDriver / GeckoDriver (or Selenium Manager managed)
  - *Android*: Android SDK (`adb`), Appium 2.x server running locally or remotely

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
  <version>0.26.40.5</version>
</dependency>
```

---

## ⚙️ Configuration & Execution

The framework is configured via system properties, environment variables, or `test.properties`:

| Property | Default | Description |
|---|---|---|
| `platform` | `web` | Target platform (`web`, `android`) |
| `debug` | `false` | Enable verbose debug logging and telemetry |
| `retries` | `3` | Max retry attempts for state transitions |
| `colours.cutoff` | `0` | Visual comparison tolerance threshold |
| `.test.report.add.all.leaf` | `true` | Include all-leaf completion records in test reports |
| `.test.report.add.eta.debug` | `true` | Include ETA debug timeline charts and metrics in test reports |
| `.test.report.add.cpu.debug` | `true` | Include CPU telemetry measurements and charts in test reports |
| `.test.report.cpu.density` | `5000` | Maximum data points density for CPU/RAM telemetry graphs in reports |
| `.test.report.cpu.density.per.test` | `false` | When true, matches CPU/RAM telemetry data points count to the number of tests performed |
| `.test.report.progression.scenario` | `true` | Enable scenario coverage progression frames and animations |
| `.test.report.progression.scenario.gif` | `true` | Generate scenario coverage GIF animation |
| `.test.report.progression.scenario.webp` | `true` | Generate scenario coverage WebP animation |
| `.test.report.progression.scenario.mp4` | `true` | Generate scenario coverage MP4 video (requires FFmpeg) |
| `.test.report.progression.tree` | `true` | Enable scenario tree progression frames and animations |
| `.test.report.progression.tree.gif` | `true` | Generate scenario tree GIF animation |
| `.test.report.progression.tree.webp` | `true` | Generate scenario tree WebP animation |
| `.test.report.progression.tree.mp4` | `true` | Generate scenario tree MP4 video (requires FFmpeg) |
| `.test.report.progression.ffmpeg` | `ffmpeg` | FFmpeg executable name or absolute path |
| `.test.report.progression.max.frames` | `500` | Maximum progression snapshots and animation frames; later cycles replace the last snapshot (hard-capped at 10,000) |
| `.test.report.progression.max.frame.pixels` | `16777216` | Maximum pixel count for a frame; oversized tree progression frames are skipped (hard-capped at 33,554,432) |
| `.test.report.progression.max.total.pixels` | `1000000000` | Animation pixel-work budget; large frames automatically reduce the sampled frame count (hard-capped at 2,000,000,000) |

The framework uses OS-backed `run/.run.lock` and `run/.postprocess.lock` files to prevent a new test run from starting while a test or its detached post-processor is active. The lock files remain in place; the OS lock state determines whether either process is still running.

---

## 🐞 Per-Node Known Issues Tracking

The framework supports per-node Known Issues mapping. This allows test runs to track expected failures, staging defects, or known environment issues directly against specific scenarios in the interactive `TreeHtml` report.

### `known_issues.json` Schema

Known issues are configured in `src/main/resources/known_issues.json`:

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
