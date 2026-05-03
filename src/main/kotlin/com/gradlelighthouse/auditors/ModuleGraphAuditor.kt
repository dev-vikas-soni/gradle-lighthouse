package com.gradlelighthouse.auditors

import com.gradlelighthouse.core.AuditContext
import com.gradlelighthouse.core.Auditor
import com.gradlelighthouse.core.AuditIssue
import com.gradlelighthouse.core.ConsoleLogger
import com.gradlelighthouse.core.Severity
import java.io.File

/**
 * ModuleGraphAuditor: Detects architectural issues in the module dependency graph.
 *
 * Checks for:
 * - Circular dependencies between modules
 * - Feature modules depending on other feature modules
 * - Modules with too many dependencies (high coupling)
 * - Generates DOT graph visualization data
 */
class ModuleGraphAuditor : Auditor {
    override val name: String = "ModuleGraph"

    companion object {
        private const val HIGH_COUPLING_THRESHOLD = 10
        private const val FEATURE_MODULE_PREFIX = "feature"
    }

    override fun audit(context: AuditContext): List<AuditIssue> {
        val issues = mutableListOf<AuditIssue>()
        ConsoleLogger.auditorStart(name, "🔗", "[GRAPH]", "Analyzing module dependency graph...")

        val moduleDeps = context.moduleDependencyGraph

        if (moduleDeps.isEmpty()) {
            return issues
        }

        val currentModule = context.projectPath

        // 1. Detect cycles using DFS from this module's perspective
        val cycles = detectCycles(currentModule, moduleDeps)
        if (cycles.isNotEmpty()) {
            val cycleDesc = cycles.joinToString("; ") { cycle -> cycle.joinToString(" → ") }
            issues.add(AuditIssue(
                category = "Architecture",
                severity = Severity.FATAL,
                title = "Circular Dependency Detected",
                reasoning = "Module '$currentModule' participates in circular dependency chain(s): $cycleDesc. Circular deps prevent independent compilation and break incremental builds.",
                impactAnalysis = "Circular dependencies prevent Gradle from parallelizing module compilation, break Configuration Cache, cause unnecessary recompilations, and indicate tightly-coupled architecture that is impossible to refactor incrementally.",
                resolution = "Break the cycle by: 1) Extracting shared code into a new ':core:*' module, 2) Using dependency inversion (interfaces in a shared contract module), 3) Moving the offending dependency behind an API boundary.",
                roiAfterFix = "Enables parallel compilation, reduces incremental build times by 20-40%, and improves code maintainability.",
                sourceFile = context.buildFile.absolutePath
            ))
        }

        // 2. Detect feature-to-feature dependencies
        val directDeps = moduleDeps[currentModule] ?: emptySet()
        val isFeatureModule = currentModule.contains(FEATURE_MODULE_PREFIX, ignoreCase = true)
        if (isFeatureModule) {
            val featureDeps = directDeps.filter { it.contains(FEATURE_MODULE_PREFIX, ignoreCase = true) }
            if (featureDeps.isNotEmpty()) {
                issues.add(AuditIssue(
                    category = "Architecture",
                    severity = Severity.ERROR,
                    title = "Feature Module Depends on Other Feature Modules",
                    reasoning = "Feature module '$currentModule' depends on other feature modules: ${featureDeps.joinToString()}. This violates clean architecture — feature modules should only depend on domain/core/shared modules.",
                    impactAnalysis = "Feature-to-feature coupling creates a monolithic dependency web, prevents independent feature deployment, makes module extraction impossible, and causes cascading recompilation across unrelated features.",
                    resolution = "Extract shared logic into a ':core:*' or ':domain:*' module. Use navigation-based communication (deep links, shared contracts) instead of direct module dependencies between features.",
                    roiAfterFix = "Independent feature module builds, enabling parallel development and potential dynamic feature delivery.",
                    sourceFile = context.buildFile.absolutePath
                ))
            }
        }

        // 3. High coupling detection
        if (directDeps.size > HIGH_COUPLING_THRESHOLD) {
            issues.add(AuditIssue(
                category = "Architecture",
                severity = Severity.WARNING,
                title = "High Module Coupling: ${directDeps.size} Direct Dependencies",
                reasoning = "Module '$currentModule' depends on ${directDeps.size} other modules (threshold: $HIGH_COUPLING_THRESHOLD). High fan-out indicates the module has too many responsibilities.",
                impactAnalysis = "Any change in any of ${directDeps.size} dependencies triggers recompilation of this module. High coupling also makes the module harder to test in isolation and increases build times linearly.",
                resolution = "Consider splitting this module into smaller, focused modules. Apply the Single Responsibility Principle at the module level. Use facade modules to reduce direct coupling.",
                roiAfterFix = "Reduced recompilation surface, faster incremental builds, easier testing and maintenance."
            ))
        }

        // 4. Generate DOT graph file
        generateDotGraph(context, moduleDeps)

        return issues
    }

    private fun detectCycles(startModule: String, graph: Map<String, Set<String>>): List<List<String>> {
        val cycles = mutableListOf<List<String>>()
        val visited = mutableSetOf<String>()
        val recursionStack = mutableListOf<String>()

        fun dfs(current: String) {
            if (current in recursionStack) {
                val cycleStart = recursionStack.indexOf(current)
                cycles.add(recursionStack.subList(cycleStart, recursionStack.size) + current)
                return
            }
            if (current in visited) return

            visited.add(current)
            recursionStack.add(current)

            graph[current]?.forEach { neighbor ->
                dfs(neighbor)
            }

            recursionStack.removeAt(recursionStack.lastIndex)
        }

        dfs(startModule)
        return cycles
    }

    private fun generateDotGraph(context: AuditContext, graph: Map<String, Set<String>>) {
        val sb = StringBuilder()
        sb.appendLine("digraph ModuleDependencyGraph {")
        sb.appendLine("    rankdir=LR;")
        sb.appendLine("    node [shape=box, style=filled, fillcolor=\"#e8f4fd\", fontname=\"Helvetica\"];")
        sb.appendLine("    edge [color=\"#666666\"];")
        sb.appendLine()

        // Highlight current module
        val current = context.projectPath.replace(":", "_").trimStart('_')
        sb.appendLine("    $current [fillcolor=\"#fbbf24\", style=\"filled,bold\"];")

        graph.forEach { (module, deps) ->
            val from = module.replace(":", "_").trimStart('_')
            deps.forEach { dep ->
                val to = dep.replace(":", "_").trimStart('_')
                val isFeatureToFeature = module.contains("feature") && dep.contains("feature")
                val edgeColor = if (isFeatureToFeature) "color=\"#ef4444\", style=bold" else ""
                sb.appendLine("    $from -> $to [$edgeColor];")
            }
        }

        sb.appendLine("}")

        val outputDir = File(context.rootDir, "build/reports/lighthouse")
        if (!outputDir.exists()) outputDir.mkdirs()
        val dotFile = File(outputDir, "module-graph.dot")
        dotFile.writeText(sb.toString())
    }
}

