# Property Management Architecture in `bot-framework`

This document details the exact architecture, lifecycle, and design patterns for property handling in `bot-framework`. Use this guide when adding, modifying, or refactoring properties across the codebase.

---

## 1. System Overview

Property management in `bot-framework` uses a two-layer architecture separating **definition & resolution** from **domain-scoped static caching**:

```
 ┌─────────────────────────────────────────────────────────┐
 │                   Configuration Files                   │
 │   (user overrides, *.test, *.part, common.properties)   │
 └──────────────────────────┬──────────────────────────────┘
                            │
                            ▼
 ┌─────────────────────────────────────────────────────────┐
 │                  TestProperties Engine                  │
 │   Layered resolution, key fallback, StringParser        │
 └──────────────────────────┬──────────────────────────────┘
                            │
                            ▼
 ┌─────────────────────────────────────────────────────────┐
 │        Layer 1: Master Registry (PropEnum)              │
 │   Implements _PropEnum interface; declares metadata,    │
 │   default values, types, and silent/exec flags          │
 └──────────────────────────┬──────────────────────────────┘
                            │
                            ▼
 ┌─────────────────────────────────────────────────────────┐
 │     Layer 2: Domain Record Micro-Caches                 │
 │   (works.lysenko.util.prop.{core,data,grid,logs,...})   │
 │   Empty Java records providing static typed accessors   │
 └─────────────────────────────────────────────────────────┘
```

---

## 2. Layer 1: The Master Registry (`PropEnum` & `_PropEnum`)

All configuration parameters in the application must be declared in [PropEnum](file:///Users/sergii/Repos/bot-framework/src/main/java/works/lysenko/util/prop/PropEnum.java), which implements the [_PropEnum](file:///Users/sergii/Repos/bot-framework/src/main/java/works/lysenko/util/apis/_PropEnum.java) interface.

### Enum Constant Structure & Metadata

Each `PropEnum` entry encapsulates four attributes:
1. **Target Class Type (`Class<?>`)**: Parsed at runtime via `StringParser` (e.g. `Boolean.class`, `Integer.class`, `Double.class`, `String.class`, `Severity.class`, `Fraction.class`, `Color.class`, `Float.class`).
2. **Default Value (`String`)**: Stored as a string (frequently constructed using concise framework constants like `TRUE`, `FALSE`, `_0_`, `b(...)`, `s(...)`).
3. **Silent Flag (`boolean silent`)**: Optional flag (defaults to `false`). When `true`, suppresses verbose debug logging during property lookup.
4. **Execution Parameter Flag (`boolean executionParameter`)**: Optional flag (defaults to `false`). When `true`, indicates the property is exposed in GUI / CLI parameters.

### Constructor Overloads

```java
// Full declaration: type, defaultValue, silent, executionParameter
_TEST_ALL_LEAFS(Boolean.class, FALSE, true, true),

// Silent flag declaration
_LOGS_DEBUG(Boolean.class, FALSE, true),

// Standard declaration (type + default value)
_TEST_ACTION_RETRIES(Integer.class, _3_),

// Varargs character constructor for string assembly
_GRID_DEFAULT_RESOLUTION(String.class, _3_, _ASTRS_, _3_)
```

### Property Naming Convention

By default, `_PropEnum.getPropertyName()` converts enum names to key strings by lowercasing and replacing underscores (`_`) with dots (`.`):
* `_TEST_RESILIENT_MODE` $\rightarrow$ `.test.resilient.mode`
* `_LOGS_DEBUG` $\rightarrow$ `.logs.debug`

When a custom key structure is required, `PropEnum` overrides `getPropertyName()`:

```java
@Override
public String getPropertyName() {
    return switch (this) {
        case _SWIPE_LINE_MARKER_COLOUR -> ".swipes.marker.line.colour";
        case _SWIPE_START_MARKER_COLOUR -> ".swipes.marker.start.colour";
        case _SWIPE_STOP_MARKER_COLOUR -> ".swipes.marker.stop.colour";
        default -> _PropEnum.super.getPropertyName();
    };
}
```

### Retrieval Mechanics

Calling `_TEST_RESILIENT_MODE.get()` delegates to `Base.properties.getEnum(this)`. If `Base.properties` is uninitialized or null, it safely falls back to parsing `defaultValue` via `StringParser.create(defaultValue, type).result()`.

---

## 3. Layer 2: Record-Based Micro-Caches (`works.lysenko.util.prop.*`)

To prevent repeated enum lookups, string formatting, and map querying during execution, properties are organized into **zero-boilerplate Java records** grouped by domain package under `works.lysenko.util.prop.*`:

| Domain Package | Class Examples | Description |
| :--- | :--- | :--- |
| `works.lysenko.util.prop.core` | `Mode`, `Assertions`, `Screenshot`, `Similarity`, `Waits`, `Swipe`, `StackTrace` | Core test execution and system parameters |
| `works.lysenko.util.prop.data` | `Delimeters`, `Fractions`, `Persist`, `Snapshot` | Data parsing and serialization settings |
| `works.lysenko.util.prop.grid` | `Allowed`, `Colours`, `Defaults`, `Logs`, `Marker`, `Ranges`, `Validation` | Grid image comparison and visual analysis settings |
| `works.lysenko.util.prop.issues` | `KnownIssues` | Known issue matcher configurations |
| `works.lysenko.util.prop.logs` | `Debug`, `LongOperation`, `Rate`, `Table`, `Time`, `To` | Logging, verbosity, and reporting thresholds |
| `works.lysenko.util.prop.tree` | `HideOff`, `Include`, `Notify`, `Optimization`, `Retries`, `Scenario`, `Traverse` | Execution tree and scenario selector settings |

### Anatomy of a Property Record

Property records use **empty record parameter lists** (`public record Name()`) and declare **public static fields** bound directly to `PropEnum._ENUM.get()`:

```java
package works.lysenko.util.prop.core;

import static works.lysenko.util.prop.PropEnum._TEST_RESILIENT_MODE;

/**
 * Execution mode configuration.
 */
public record Mode() {

    /**
     * Determines whether resilient mode is enabled.
     */
    public static final boolean resilient = _TEST_RESILIENT_MODE.get();
}
```

### Static Initialization & Lifecycles

1. **JVM Class Loading**: When a record field is first accessed (e.g., `if (Mode.resilient)`), the JVM loads the record class, executing static initializers and binding the resolved `PropEnum` value.
2. **Type Generalization**: Property fields are typed appropriately—supporting primitives (`boolean`, `int`), domain records (`RangedMargin`, `Fraction`, `Color`), enumerated domain types (`Severity`), or collections (`List<String>`).
3. **Dynamic Refresh Pattern**: For properties that may be updated mid-execution or between test runs, records expose an explicit static `refresh()` method:

```java
public record Scenario() {

    public static Fraction completionWeight = _TREE_COMPLETION_WEIGHT.get();
    public static String defaultWeight = _TREE_DEFAULT_WEIGHT.get();

    public static void refresh() {
        completionWeight = _TREE_COMPLETION_WEIGHT.get();
        defaultWeight = _TREE_DEFAULT_WEIGHT.get();
    }
}
```

---

## 4. Property Resolution Pipeline (`TestProperties`)

When `_PropEnum.get()` or `properties.getEnum(p)` is called, [TestProperties](file:///Users/sergii/Repos/bot-framework/src/main/java/works/lysenko/base/TestProperties.java) evaluates the property key using the following order of precedence:

```
1. User Overrides (runtime CLI parameters, GUI inputs)
   ↓
2. Test Configuration File (*.test, *.part)
   ↓
3. Common Configuration (common.properties)
   ↓
4. Enum Default Value (PropEnum.defaultValue())
```

Key lookup features:
* **Dot Tolerance**: Checks key both with and without leading dot (`.test.resilient.mode` vs `test.resilient.mode`).
* **Type Conversion**: Converts raw string source to target type via `StringParser.create(source, p.type())`.

---

## 5. Critical Guidelines & LLM Anti-Patterns

When reading, updating, or generating code in `works.lysenko.util.prop`, strictly follow these rules to avoid regressions:

> [!CAUTION]
> **DO NOT instantiate property records.** They are static namespace containers. Use `Mode.resilient`, `Debug.debug`, `Assertions.exception`, etc.

> [!WARNING]
> **DO NOT add instance getter methods to property records.** Do NOT replace `public static final boolean resilient` with `public boolean getResilient()`. The framework uses direct public static field access across all domain records.

> [!WARNING]
> **DO NOT hardcode property keys as literal strings in application logic.** All property keys must originate from `PropEnum` constants.

> [!IMPORTANT]
> **DO NOT perform manual type conversions or system property calls.** Always let `PropEnum._ENUM.get()` delegate parsing to `StringParser`.

> [!NOTE]
> **Keep record fields static final unless dynamic updates are explicitly required.** If dynamic updates are required, provide a static `refresh()` method that re-reads `PropEnum._ENUM.get()`.

---

## 6. How to Add a New Property

To add a new property to the framework:

1. **Declare in `PropEnum`**:
   ```java
   _MY_NEW_FEATURE_ENABLED(Boolean.class, TRUE),
   ```
2. **Expose in the Appropriate Domain Record** (or create a new record under `works.lysenko.util.prop.<domain>` if a new group is needed):
   ```java
   package works.lysenko.util.prop.core;

   import static works.lysenko.util.prop.PropEnum._MY_NEW_FEATURE_ENABLED;

   public record Feature() {
       public static final boolean enabled = _MY_NEW_FEATURE_ENABLED.get();
   }
   ```
3. **Consume in Application Code**:
   ```java
   if (Feature.enabled) {
       // execute feature logic
   }
   ```
