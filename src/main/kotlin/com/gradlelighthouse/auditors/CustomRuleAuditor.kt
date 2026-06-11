package com.gradlelighthouse.auditors

import com.gradlelighthouse.core.AuditContext
import com.gradlelighthouse.core.Auditor
import com.gradlelighthouse.core.AuditIssue
import com.gradlelighthouse.core.LighthouseCategory
import com.gradlelighthouse.core.Severity
import java.io.File

/**
 * CustomRuleAuditor: Allows teams to define their own architectural constraints in YAML.
 *
 * Example lighthouse-rules.yaml:
 * rules:
 *   - name: "No Data in Feature"
 *     forbidden_pattern: ":data:"
 *     in_module: ":feature:"
 *     severity: "ERROR"
 *     message: "Feature modules should not depend on data modules directly. Use domain contracts."
 */
class CustomRuleAuditor : Auditor {
    override val name: String = "CustomRules"

    override fun audit(context: AuditContext): List<AuditIssue> {
        val issues = mutableListOf<AuditIssue>()
        val rulesFile = File(context.rootDir, "lighthouse-rules.yaml")

        if (!rulesFile.exists()) return issues

        try {
            val content = rulesFile.readText()
            // Simple manual parser to avoid adding heavy YAML dependency to the plugin classpath
            val rules = parseRules(content)

            rules.forEach { rule ->
                val currentModule = context.projectPath
                if (rule.inModule.isEmpty() || currentModule.contains(rule.inModule)) {
                    val deps = context.moduleDependencyGraph[currentModule] ?: emptySet()
                    deps.forEach { dep ->
                        if (dep.contains(rule.forbiddenPattern)) {
                            issues.add(AuditIssue(
                                category = LighthouseCategory.ARCHITECTURE,
                                severity = rule.severity,
                                title = rule.name,
                                reasoning = rule.message,
                                impactAnalysis = "Violation of team-defined architectural boundaries.",
                                resolution = "Refactor the dependency to follow project standards.",
                                roiAfterFix = "Maintains clean architecture as defined by the team."
                            ))
                        }
                    }
                }
            }
        } catch (_: Exception) { }

        return issues
    }

    private data class Rule(
        val name: String,
        val forbiddenPattern: String,
        val inModule: String,
        val severity: Severity,
        val message: String
    )

    private fun parseRules(yaml: String): List<Rule> {
        val rules = mutableListOf<Rule>()
        // Very basic YAML block parser for "rules:" sections
        val lines = yaml.lines()
        var currentRule: MutableMap<String, String>? = null

        lines.forEach { line ->
            val trimmed = line.trim()
            if (trimmed == "- name:" || trimmed.startsWith("- name:")) {
                if (currentRule != null) rules.add(mapToRule(currentRule!!))
                currentRule = mutableMapOf()
                currentRule!!["name"] = trimmed.substringAfter("- name:").trim().removeSurrounding("\"")
            } else if (currentRule != null && trimmed.contains(":")) {
                val key = trimmed.substringBefore(":").trim()
                val value = trimmed.substringAfter(":").trim().removeSurrounding("\"")
                currentRule!![key] = value
            }
        }
        if (currentRule != null) rules.add(mapToRule(currentRule!!))
        return rules
    }

    private fun mapToRule(map: Map<String, String>): Rule {
        return Rule(
            name = map["name"] ?: "Custom Rule",
            forbiddenPattern = map["forbidden_pattern"] ?: "",
            inModule = map["in_module"] ?: "",
            severity = try { Severity.valueOf(map["severity"] ?: "WARNING") } catch (_: Exception) { Severity.WARNING },
            message = map["message"] ?: "Architectural boundary violation."
        )
    }
}
