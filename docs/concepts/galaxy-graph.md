# Galaxy Graph

The Galaxy Graph is a visualization of module dependency information collected by Gradle Lighthouse. It is intended to make relationships and detected cycles easier to inspect.

## Visual encoding

The report may visualize:
- Nodes as Gradle modules.
- Edges as detected module dependencies.
- Cycle-related edges with a distinct visual treatment.
- Additional module metadata or grouping when that information is available.

Exact node sizing, coloring, grouping, and edge styling are presentation choices. They should not be interpreted as independently measured module quality, complexity, or coupling unless the report explicitly defines the underlying metric.

## Cycle detection

The graph can display cycles detected in the module dependency representation built by the plugin. Detection is limited to relationships captured by that representation; absence of a displayed cycle does not prove that all forms of architectural dependency cycles are absent.

## Interactive exploration

Some report versions provide interactive graph exploration or dependency-edge simulation. A simulated edge removal is a what-if view of the captured graph, not a real refactor and not proof that the project will compile or behave correctly after a corresponding code change. Re-run the audit and build after making actual changes.

## Portability

The generated report is designed to be viewable as an HTML artifact. Check the generated report itself for its asset and network requirements in the version you use.
