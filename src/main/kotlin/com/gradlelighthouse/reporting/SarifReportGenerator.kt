package com.gradlelighthouse.reporting

import com.gradlelighthouse.core.AuditIssue
import com.gradlelighthouse.core.Severity
import java.util.Locale

/**
 * SARIF v2.1.0 Report Generator — Static Analysis Results Interchange Format.
 */
object SarifReportGenerator {

    private const val SARIF_VERSION = "2.1.0"
    private const val SCHEMA_URI = "https://raw.githubusercontent.com/oasis-tcs/sarif-spec/main/sarif-2.1/schema/sarif-schema-2.1.0.json"
    private const val TOOL_NAME = "Gradle Lighthouse"

    fun generate(moduleName: String, pluginVersion: String, issues: List<AuditIssue>): String {
        val uniqueIssues = issues.distinctBy { it.title }
        val rules = uniqueIssues.mapIndexed { index, issue ->
            buildRule(index, issue)
        }

        val results = issues.map { issue ->
            val ruleIndex = uniqueIssues.indexOfFirst { it.title == issue.title }
            buildResult(ruleIndex, issue)
        }

        return buildString {
            appendLine("{")
            appendLine("""  "${"$"}schema": "${escapeJson(SCHEMA_URI)}",""")
            appendLine("""  "version": "$SARIF_VERSION",""")
            appendLine("""  "runs": [{""")
            appendLine("""    "tool": {""")
            appendLine("""      "driver": {""")
            appendLine("""        "name": "$TOOL_NAME",""")
            appendLine("""        "version": "${escapeJson(pluginVersion)}",""")
            appendLine("""        "informationUri": "https://github.com/dev-vikas-soni/gradle-lighthouse",""")
            appendLine("""        "rules": [""")
            appendLine(rules.joinToString(",\n"))
            appendLine("""        ]""")
            appendLine("""      }""")
            appendLine("""    },""")
            appendLine("""    "results": [""")
            appendLine(results.joinToString(",\n"))
            appendLine("""    ],""")
            appendLine("""    "automationDetails": {""")
            appendLine("""      "id": "gradlelighthouse/$moduleName"  """)
            appendLine("""    }""")
            appendLine("""  }]""")
            appendLine("}")
        }
    }

    private fun buildRule(index: Int, issue: AuditIssue): String {
        val level = sarifLevel(issue.severity)
        return buildString {
            appendLine("          {")
            appendLine("""            "id": "DU${String.format("%04d", index)}",""")
            appendLine("""            "name": "${escapeJson(issue.title)}",""")
            appendLine("""            "shortDescription": { "text": "${escapeJson(issue.title)}" },""")
            appendLine("""            "fullDescription": { "text": "${escapeJson(issue.reasoning)}" },""")
            appendLine("""            "helpUri": "https://github.com/dev-vikas-soni/gradle-lighthouse#readme",""")
            appendLine("""            "defaultConfiguration": { "level": "$level" },""")
            appendLine("""            "properties": {""")
            appendLine("""              "category": "${escapeJson(issue.category)}",""")
            appendLine("""              "tags": ["android", "${escapeJson(issue.category.toLowerCase(Locale.ROOT))}"]""")
            appendLine("""            }""")
            append("          }")
        }
    }

    private fun buildResult(ruleIndex: Int, issue: AuditIssue): String {
        val level = sarifLevel(issue.severity)
        return buildString {
            appendLine("      {")
            appendLine("""        "ruleId": "DU${String.format("%04d", ruleIndex)}",""")
            appendLine("""        "ruleIndex": $ruleIndex,""")
            appendLine("""        "level": "$level",""")
            appendLine("""        "message": {""")
            appendLine("""          "text": "${escapeJson(issue.reasoning)}\n\nImpact: ${escapeJson(issue.impactAnalysis)}\n\nResolution: ${escapeJson(issue.resolution)}\n\nROI: ${escapeJson(issue.roiAfterFix)}" """)
            appendLine("""        },""")

            if (issue.sourceFile != null) {
                appendLine("""        "locations": [{""")
                appendLine("""          "physicalLocation": {""")
                appendLine("""            "artifactLocation": {""")
                appendLine("""              "uri": "${escapeJson(issue.sourceFile)}",""")
                appendLine("""              "uriBaseId": "%SRCROOT%"  """)
                appendLine("""            }""")
                if (issue.sourceLine != null) {
                    appendLine("""            ,"region": { "startLine": ${issue.sourceLine} }""")
                }
                appendLine("""          }""")
                appendLine("""        }],""")
            }

            appendLine("""        "properties": {""")
            appendLine("""          "category": "${escapeJson(issue.category)}",""")
            appendLine("""          "roiAfterFix": "${escapeJson(issue.roiAfterFix)}" """)
            appendLine("""        }""")
            append("      }")
        }
    }

    private fun sarifLevel(severity: Severity): String = when (severity) {
        Severity.FATAL -> "error"
        Severity.ERROR -> "error"
        Severity.WARNING -> "warning"
        Severity.INFO -> "note"
    }

    private fun escapeJson(text: String): String = text
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t")
}
