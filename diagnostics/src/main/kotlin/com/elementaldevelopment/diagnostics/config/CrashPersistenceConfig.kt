package com.elementaldevelopment.diagnostics.config

/**
 * Controls whether diagnostics are persisted locally to survive process death.
 *
 * Persistence remains device-local and redacted-before-write.
 */
sealed interface CrashPersistenceConfig {
    data object Disabled : CrashPersistenceConfig

    data class Enabled(
        val maxPersistedEntries: Int,
        val retentionAfterRecoveryMillis: Long = DEFAULT_RETENTION_AFTER_RECOVERY_MILLIS,
        val includeRecoveredEntriesByDefault: Boolean = true,
    ) : CrashPersistenceConfig {
        init {
            require(maxPersistedEntries > 0) { "maxPersistedEntries must be > 0" }
            require(retentionAfterRecoveryMillis >= 0L) {
                "retentionAfterRecoveryMillis must be >= 0"
            }
        }
    }

    companion object {
        const val DEFAULT_RETENTION_AFTER_RECOVERY_MILLIS: Long = 7L * 24 * 60 * 60 * 1000
    }
}
