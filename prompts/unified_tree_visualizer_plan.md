# Unified Tree Visualizer Architecture Plan

## Context & Problem Statement
Currently, the scenario execution tree is rendered via two parallel engines in `bot-framework`:
1. **`TreeHtml.java`**: Generates interactive HTML/SVG files.
2. **`TreeTracker.java`**: Generates bitmaps (`BufferedImage`) for `tree.progression.mp4` / `webp` animations.

While they share the same base topological layout logic (`computeLayout`), their visual definitions are entirely duplicated. `TreeHtml` hardcodes CSS classes and hex codes, while `TreeTracker` hardcodes `java.awt.Color` instances and `Graphics2D` draw calls. This duplication has led to visual drift, layout discrepancies, and a "non-enterprise" feel where the HTML tree and the video tree look slightly different.

## Goal
Establish a **Single Source of Truth** for the visual appearance of the Scenario Execution Tree. Separate the logic, theming, and layout from the actual rendering mechanisms (HTML vs. Bitmap).

## Proposed Architecture (MVC Approach)

### 1. The Model & Theme (`TreeTheme.java`)
A central configuration record acting as the single source of truth for all visual constants.
- Defines dimensions: `cardWidth`, `cardHeight`, `padding`, `cornerRadius`.
- Defines color palettes using `java.awt.Color`: `passedBg`, `failedBg`, `upsetBg`, `excludedBg`, etc.
- Provides utility methods to seamlessly translate AWT colors into SVG-compatible hex strings (e.g., `theme.hex(theme.passedBg())`).

### 2. The Logic Controller (`VisualNodeResolver.java`)
A pure logic layer that consumes raw `NodeData` and execution stats, and outputs a highly abstracted `VisualNode` object.
- Replaces the duplicated `if/else` chains checking for `isTheoreticallyExecutable`, `hasFailure`, and `isAllLeafs`.
- Outputs a ready-to-render object containing explicit properties: `badgeText`, `backgroundColor`, `borderColor`, `strokeWidth`, `x`, `y`.

### 3. The "Dumb" Renderers (The Views)
With all logic and theming centralized, the actual renderers will contain zero business logic.
- **`HtmlTreeRenderer`**: Iterates over `VisualNode`s and concatenates `<rect fill="${node.backgroundColorHex}" ... />` strings.
- **`GraphicsTreeRenderer`**: Iterates over `VisualNode`s and executes `g2d.setColor(node.backgroundColor)` followed by `g2d.fillRoundRect(...)`.

## Action Items for the Next Session
1. **Palette Extraction**: Rip all hardcoded hex strings out of `TreeHtml` and `Color` constants out of `TreeTracker`, unifying them into `TreeTheme`.
2. **State Abstraction**: Move the badge text logic (e.g., `getExecutionBadgeText`, `[0/1]`, `OFF`) and color logic (`getScenarioProgressColor`) into `VisualNodeResolver`.
3. **Renderer Refactoring**: Gut the drawing routines in `TreeHtml` and `TreeTracker` and wire them up to consume the new `VisualNode` pipeline.
4. **Pixel Perfection**: Run test suite and compare the SVG output against the PNG output to ensure 100% visual parity.
