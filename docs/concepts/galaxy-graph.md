# Galaxy Graph Visualization

The Galaxy Graph is an interactive, force-directed module dependency map. It is the heart of the Lighthouse aggregation experience.

## Visual Encoding

* **Nodes (Planets)**: Represent Gradle modules.
  * **Size**: Corresponds to the module's size and complexity (LOC and public API surface).
  * **Color**: Matches the module's health score (Green = Elite, Red = Legacy).
  * **Rings**: Modules are grouped into orbital rings based on their architectural layer (e.g., App modules in the outer ring, Core in the center).
* **Links (Orbits)**: Represent dependencies between modules.
  * **Thickness**: Indicates coupling density (number of configurations sharing this dependency).
  * **Red Glow**: Indicates a dependency that is part of a circular dependency cycle.

## Core Features

### 1. Cycle Detection
The graph engine automatically runs an iterative DFS algorithm on the full module graph. Cycles are instantly visible as glowing red edges, making them easy to identify and triage.

### 2. Refactoring Sandbox
This is the graph's most powerful feature. You can click any link and select "Cut Link" to simulate its removal. Lighthouse will instantly:
* Recalculate global and per-module health scores.
* Update the cycle count.
* Show the predicted architectural impact of the refactor.

### 3. Layer Violation Highlighting
The graph enforces your `lighthouse-rules.yaml` rules. If a module in the `Core` layer depends on one in the `Feature` layer, the link will be visually flagged as a "Layer Leak."

### 4. Fullscreen Exploration
For projects with 100+ modules, the Galaxy Graph supports a fullscreen mode with zooming, panning, and module search, allowing build engineers to navigate the complexity of their module graph effortlessly.

## Portability

The Galaxy Graph engine is built using a custom, zero-dependency canvas renderer. The entire graph logic and data are inlined into the HTML output. This ensures the dashboard works:
* In air-gapped CI environments.
* Without any external CDNs.
* As a persistent artifact in your CI pipeline.
