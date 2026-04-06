package com.elementaldevelopment.diagnostics.internal

import android.content.Context
import android.util.AtomicFile
import com.elementaldevelopment.diagnostics.model.DiagnosticEntry
import com.elementaldevelopment.diagnostics.model.DiagnosticLevel
import com.elementaldevelopment.diagnostics.model.PreviousSessionOutcome
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.nio.charset.StandardCharsets

internal class FileBackedCrashStore(
    context: Context,
    private val appId: String,
    private val maxPersistedEntries: Int,
    private val retentionAfterRecoveryMillis: Long,
) : RecoveredDiagnosticsRepository {

    init {
        require(maxPersistedEntries > 0) { "maxPersistedEntries must be > 0" }
        require(retentionAfterRecoveryMillis >= 0L) { "retentionAfterRecoveryMillis must be >= 0" }
    }

    private val lock = Any()
    private val file = AtomicFile(
        File(File(context.applicationContext.filesDir, "ember"), "${appId.sanitizeForFilename()}_diagnostics_snapshot.json")
    )

    override fun recoverAndStartNewSession(sessionId: String, startedAt: Long): RecoveryState = synchronized(lock) {
        val snapshot = readSnapshot()
        val retainedRecovered = snapshot.recoveredSession?.takeUnless { it.isExpired(startedAt) }
        val recoveredSession = when {
            snapshot.activeSession == null -> retainedRecovered
            snapshot.activeSession.outcome == PersistedSessionOutcome.CLEAN_EXIT -> retainedRecovered
            snapshot.activeSession.outcome == PersistedSessionOutcome.UNCUGHT_EXCEPTION -> {
                snapshot.activeSession.withOutcome(PersistedSessionOutcome.UNCUGHT_EXCEPTION, startedAt)
            }
            else -> snapshot.activeSession.withOutcome(PersistedSessionOutcome.UNEXPECTED_TERMINATION, startedAt)
        }

        writeSnapshot(
            PersistedDiagnosticsSnapshot(
                activeSession = PersistedSession(
                    sessionId = sessionId,
                    startedAt = startedAt,
                    endedAt = null,
                    outcome = PersistedSessionOutcome.OPEN,
                    entries = emptyList(),
                ),
                recoveredSession = recoveredSession,
            )
        )

        recoveredSession.toRecoveryState()
    }

    override fun appendToActiveSession(entry: DiagnosticEntry) = synchronized(lock) {
        val snapshot = readSnapshot()
        val activeSession = snapshot.activeSession ?: return
        val updatedEntries = (activeSession.entries + entry).takeLast(maxPersistedEntries)
        writeSnapshot(
            snapshot.copy(
                activeSession = activeSession.copy(entries = updatedEntries)
            )
        )
    }

    override fun markSessionOpen() = synchronized(lock) {
        val snapshot = readSnapshot()
        val activeSession = snapshot.activeSession ?: return
        if (activeSession.outcome == PersistedSessionOutcome.OPEN && activeSession.endedAt == null) {
            return
        }
        writeSnapshot(
            snapshot.copy(
                activeSession = activeSession.copy(
                    outcome = PersistedSessionOutcome.OPEN,
                    endedAt = null,
                )
            )
        )
    }

    override fun markCleanExit(endedAt: Long) = synchronized(lock) {
        val snapshot = readSnapshot()
        val activeSession = snapshot.activeSession ?: return
        writeSnapshot(
            snapshot.copy(
                activeSession = activeSession.copy(
                    outcome = PersistedSessionOutcome.CLEAN_EXIT,
                    endedAt = endedAt,
                )
            )
        )
    }

    override fun markUncaughtException(endedAt: Long) = synchronized(lock) {
        val snapshot = readSnapshot()
        val activeSession = snapshot.activeSession ?: return
        writeSnapshot(
            snapshot.copy(
                activeSession = activeSession.copy(
                    outcome = PersistedSessionOutcome.UNCUGHT_EXCEPTION,
                    endedAt = endedAt,
                )
            )
        )
    }

    override fun getRecoveredEntries(limit: Int?): List<DiagnosticEntry> = synchronized(lock) {
        val snapshot = readSnapshot()
        val now = System.currentTimeMillis()
        val recoveredSession = snapshot.recoveredSession?.takeUnless { it.isExpired(now) } ?: return emptyList()
        val entries = recoveredSession.entries
        if (limit != null) entries.takeLast(limit) else entries
    }

    override fun clearAll() = synchronized(lock) {
        val snapshot = readSnapshot()
        val activeSession = snapshot.activeSession
        writeSnapshot(
            PersistedDiagnosticsSnapshot(
                activeSession = activeSession?.copy(entries = emptyList()),
                recoveredSession = null,
            )
        )
    }

    private fun PersistedSession.isExpired(now: Long): Boolean {
        if (retentionAfterRecoveryMillis == 0L) return true
        val referenceTime = endedAt ?: startedAt
        return now - referenceTime > retentionAfterRecoveryMillis
    }

    private fun PersistedSession.withOutcome(
        outcome: PersistedSessionOutcome,
        endedAtFallback: Long,
    ): PersistedSession {
        return copy(
            outcome = outcome,
            endedAt = endedAt ?: endedAtFallback,
        )
    }

    private fun PersistedSession?.toRecoveryState(): RecoveryState {
        if (this == null) return RecoveryState()
        return RecoveryState(
            recoveredEntries = entries,
            previousSessionOutcome = when (outcome) {
                PersistedSessionOutcome.UNCUGHT_EXCEPTION -> PreviousSessionOutcome.UNCUGHT_EXCEPTION
                PersistedSessionOutcome.OPEN,
                PersistedSessionOutcome.UNEXPECTED_TERMINATION,
                PersistedSessionOutcome.CLEAN_EXIT,
                -> PreviousSessionOutcome.UNEXPECTED_TERMINATION
            },
            previousSessionId = sessionId,
            previousSessionTimestamp = endedAt ?: startedAt,
        )
    }

    private fun readSnapshot(): PersistedDiagnosticsSnapshot {
        if (!file.baseFile.exists()) return PersistedDiagnosticsSnapshot()
        return runCatching {
            val bytes = file.openRead().use { it.readBytes() }
            val json = JSONObject(String(bytes, StandardCharsets.UTF_8))
            PersistedDiagnosticsSnapshot(
                activeSession = json.optJSONObject("activeSession")?.toPersistedSession(),
                recoveredSession = json.optJSONObject("recoveredSession")?.toPersistedSession(),
            )
        }.getOrElse {
            PersistedDiagnosticsSnapshot()
        }
    }

    private fun writeSnapshot(snapshot: PersistedDiagnosticsSnapshot) {
        val parent = file.baseFile.parentFile
        if (parent != null && !parent.exists()) {
            parent.mkdirs()
        }

        val output = file.startWrite()
        try {
            output.write(snapshot.toJson().toString().toByteArray(StandardCharsets.UTF_8))
            file.finishWrite(output)
        } catch (t: Throwable) {
            file.failWrite(output)
            throw t
        }
    }

    private fun PersistedDiagnosticsSnapshot.toJson(): JSONObject {
        return JSONObject().apply {
            put("schemaVersion", 1)
            put("activeSession", activeSession?.toJson())
            put("recoveredSession", recoveredSession?.toJson())
        }
    }

    private fun PersistedSession.toJson(): JSONObject {
        return JSONObject().apply {
            put("sessionId", sessionId)
            put("startedAt", startedAt)
            put("endedAt", endedAt)
            put("outcome", outcome.name)
            put("entries", JSONArray().apply {
                entries.forEach { put(it.toJson()) }
            })
        }
    }

    private fun JSONObject.toPersistedSession(): PersistedSession {
        val entriesJson = optJSONArray("entries") ?: JSONArray()
        val entries = buildList(entriesJson.length()) {
            for (index in 0 until entriesJson.length()) {
                add(entriesJson.getJSONObject(index).toDiagnosticEntry())
            }
        }

        return PersistedSession(
            sessionId = getString("sessionId"),
            startedAt = getLong("startedAt"),
            endedAt = if (isNull("endedAt")) null else getLong("endedAt"),
            outcome = PersistedSessionOutcome.valueOf(getString("outcome")),
            entries = entries,
        )
    }

    private fun DiagnosticEntry.toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("timestamp", timestamp)
            put("level", level.name)
            put("tag", tag)
            put("message", message)
            put("throwableSummary", throwableSummary)
            put("attributes", JSONObject(attributes))
        }
    }

    private fun JSONObject.toDiagnosticEntry(): DiagnosticEntry {
        val attributesJson = optJSONObject("attributes") ?: JSONObject()
        val attributes = buildMap {
            val keys = attributesJson.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                put(key, attributesJson.getString(key))
            }
        }

        return DiagnosticEntry(
            id = getLong("id"),
            timestamp = getLong("timestamp"),
            level = DiagnosticLevel.valueOf(getString("level")),
            tag = getString("tag"),
            message = getString("message"),
            throwableSummary = if (isNull("throwableSummary")) null else getString("throwableSummary"),
            attributes = attributes,
        )
    }

    private fun String.sanitizeForFilename(): String {
        return replace(Regex("[^A-Za-z0-9._-]"), "_")
    }
}

internal data class PersistedDiagnosticsSnapshot(
    val activeSession: PersistedSession? = null,
    val recoveredSession: PersistedSession? = null,
)

internal data class PersistedSession(
    val sessionId: String,
    val startedAt: Long,
    val endedAt: Long?,
    val outcome: PersistedSessionOutcome,
    val entries: List<DiagnosticEntry>,
)

internal enum class PersistedSessionOutcome {
    OPEN,
    CLEAN_EXIT,
    UNCUGHT_EXCEPTION,
    UNEXPECTED_TERMINATION,
}
