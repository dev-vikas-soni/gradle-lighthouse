package com.gradlelighthouse.core

import java.io.File
import java.io.Serializable

/**
 * A fully serializable, Configuration-Cache-safe snapshot of the project state.
 *
 * This replaces direct [org.gradle.api.Project] access inside task actions and auditors,
 * ensuring compatibility with Gradle Configuration Cache (8.x+) and Isolated Projects (9.x+).
 *
 * All data is captured during the Gradle **configuration phase** and passed as `@Input`
 * properties to the task, which then constructs this context for each auditor.
 */
data class AuditContext(
    /** Module display name (e.g., "app", "feature:cart") */
    val projectName: String,

    /** Full Gradle project path (e.g., ":feature:cart") */
    val projectPath: String,

    /** Absolute path to this module's directory */
    val projectDir: File,

    /** Absolute path to the root project directory */
    val rootDir: File,

    /** Absolute path to this module's build file (build.gradle.kts or build.gradle) */
    val buildFile: File,

    /** Content of the build file, captured at configuration time */
    val buildFileContent: String,

    /** Gradle version string (e.g., "8.12") */
    val gradleVersion: String,

    /** Set of all applied plugin IDs (e.g., "kotlin-kapt", "com.android.application") */
    val pluginIds: Set<String>,

    /** Declared (unresolved) dependencies across all relevant configurations */
    val dependencies: List<DependencySnapshot>,

    /** Resolved dependency graph data for conflict detection */
    val resolvedDependencies: List<ResolvedDependencySnapshot>,

    /** Repository information */
    val repositories: List<RepositorySnapshot>,

    /** Root-level gradle.properties key-value pairs */
    val gradleProperties: Map<String, String>,

    /** Discovered source sets (supports KMP: commonMain, androidMain, iosMain, etc.) */
    val sourceSets: List<SourceSetSnapshot>,

    /** Whether a Version Catalog TOML file exists at the root */
    val hasVersionCatalog: Boolean
) : Serializable {

    companion object {
        private const val serialVersionUID = 1L
    }
}

/**
 * Snapshot of a declared dependency (not yet resolved).
 * Captured from [org.gradle.api.artifacts.Configuration.allDependencies].
 */
data class DependencySnapshot(
    val group: String,
    val name: String,
    val version: String?,
    val configurationName: String
) : Serializable {

    /** Returns "group:name" coordinate for matching */
    val coordinate: String get() = "$group:$name"

    /** Returns "group:name:version" full notation */
    val notation: String get() = "$group:$name:${version ?: "?"}"

    companion object {
        private const val serialVersionUID = 1L
    }
}

/**
 * Snapshot of a resolved dependency after Gradle's conflict resolution.
 * Used to detect silent major version jumps.
 */
data class ResolvedDependencySnapshot(
    /** The originally requested notation (e.g., "com.squareup.okhttp3:okhttp:4.9.0") */
    val requestedNotation: String,

    /** The actually selected group after resolution */
    val selectedGroup: String,

    /** The actually selected artifact name after resolution */
    val selectedName: String,

    /** The actually selected version after resolution */
    val selectedVersion: String
) : Serializable {

    companion object {
        private const val serialVersionUID = 1L
    }
}

/**
 * Snapshot of a repository declared in the project.
 */
data class RepositorySnapshot(
    val name: String,
    val url: String
) : Serializable {

    companion object {
        private const val serialVersionUID = 1L
    }
}

/**
 * Snapshot of a source set, supporting both Android and KMP layouts.
 *
 * For a standard Android module: one entry with name="main"
 * For KMP: entries for "commonMain", "androidMain", "iosMain", etc.
 */
data class SourceSetSnapshot(
    /** Source set name (e.g., "main", "commonMain", "androidMain") */
    val name: String,

    /** Kotlin source directories */
    val kotlinDirs: List<File>,

    /** Java source directories */
    val javaDirs: List<File>,

    /** Resource directories (Android res/) */
    val resDirs: List<File>,

    /** AndroidManifest.xml if present */
    val manifestFile: File?,

    /** Assets directory if present */
    val assetsDir: File?
) : Serializable {

    companion object {
        private const val serialVersionUID = 1L
    }
}
