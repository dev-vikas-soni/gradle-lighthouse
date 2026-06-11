package com.gradlelighthouse.core.benchmarking

import com.gradlelighthouse.core.ConsoleLogger
import com.gradlelighthouse.LighthousePlugin
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Registry that manages multiple benchmark sources.
 * Thread-safe and optimized for multi-module parallel execution.
 */
object BenchmarkRegistry {

    private val providers = CopyOnWriteArrayList<BenchmarkProvider>()
    private val registeredDirs = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()

    @Volatile
    private var cachedBenchmarks: List<BenchmarkSnapshot>? = null

    init {
        // Default providers
        providers.add(EmbeddedBenchmarkProvider())
    }

    fun addProvider(provider: BenchmarkProvider) {
        var added = false
        if (provider is JsonBenchmarkProvider) {
            val path = provider.getDirectoryPath()
            if (registeredDirs.add(path)) {
                providers.add(provider)
                added = true
            }
        } else if (!providers.contains(provider)) {
            providers.add(provider)
            added = true
        }

        if (added) {
            synchronized(this) {
                cachedBenchmarks = null // Invalidate cache
            }
        }
    }

    fun getAllBenchmarks(): List<BenchmarkSnapshot> {
        val cached = cachedBenchmarks
        if (cached != null) return cached

        synchronized(this) {
            val secondCheck = cachedBenchmarks
            if (secondCheck != null) return secondCheck

            val currentVersion = LighthousePlugin.VERSION
            val result = providers.flatMap { it.loadBenchmarks() }
                .distinctBy { it.projectName } // Deduplicate by name
                .map { snapshot ->
                    if (snapshot.lighthouseVersion != currentVersion) {
                        ConsoleLogger.info("⚠️", "[BENCH]",
                            "Benchmark '${snapshot.projectName}' was generated with V${snapshot.lighthouseVersion}. " +
                            "Current engine is V$currentVersion. Scores may have shifted.")
                    }
                    snapshot
                }
            cachedBenchmarks = result
            return result
        }
    }
}
