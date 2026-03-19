package com.elementaldevelopment.diagnostics

import com.elementaldevelopment.diagnostics.internal.InMemoryDiagnosticsStore
import com.elementaldevelopment.diagnostics.model.DiagnosticEntry
import com.elementaldevelopment.diagnostics.model.DiagnosticLevel
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors

class InMemoryStoreTest {

    private fun entry(id: Long, message: String = "msg $id") = DiagnosticEntry(
        id = id,
        timestamp = System.currentTimeMillis(),
        level = DiagnosticLevel.INFO,
        tag = "test",
        message = message,
    )

    @Test
    fun `append and retrieve entries`() {
        val store = InMemoryDiagnosticsStore(10)
        store.append(entry(1))
        store.append(entry(2))

        val entries = store.getRecent()
        assertThat(entries).hasSize(2)
        assertThat(entries[0].id).isEqualTo(1)
        assertThat(entries[1].id).isEqualTo(2)
    }

    @Test
    fun `evicts oldest entries when at capacity`() {
        val store = InMemoryDiagnosticsStore(3)
        store.append(entry(1))
        store.append(entry(2))
        store.append(entry(3))
        store.append(entry(4))

        val entries = store.getRecent()
        assertThat(entries).hasSize(3)
        assertThat(entries.map { it.id }).containsExactly(2L, 3L, 4L).inOrder()
    }

    @Test
    fun `getRecent with limit returns newest entries`() {
        val store = InMemoryDiagnosticsStore(10)
        (1L..5L).forEach { store.append(entry(it)) }

        val entries = store.getRecent(2)
        assertThat(entries).hasSize(2)
        assertThat(entries.map { it.id }).containsExactly(4L, 5L).inOrder()
    }

    @Test
    fun `clear removes all entries`() {
        val store = InMemoryDiagnosticsStore(10)
        store.append(entry(1))
        store.append(entry(2))
        store.clear()

        assertThat(store.getRecent()).isEmpty()
    }

    @Test
    fun `empty store returns empty list`() {
        val store = InMemoryDiagnosticsStore(10)
        assertThat(store.getRecent()).isEmpty()
    }

    @Test
    fun `buffer never exceeds max capacity`() {
        val store = InMemoryDiagnosticsStore(5)
        (1L..100L).forEach { store.append(entry(it)) }

        assertThat(store.getRecent()).hasSize(5)
    }

    @Test
    fun `thread safety - concurrent appends do not lose entries or corrupt state`() {
        val store = InMemoryDiagnosticsStore(1000)
        val threadCount = 10
        val entriesPerThread = 100
        val latch = CountDownLatch(threadCount)
        val executor = Executors.newFixedThreadPool(threadCount)

        repeat(threadCount) { threadIndex ->
            executor.submit {
                repeat(entriesPerThread) { i ->
                    val id = (threadIndex * entriesPerThread + i).toLong()
                    store.append(entry(id))
                }
                latch.countDown()
            }
        }

        latch.await()
        executor.shutdown()

        val entries = store.getRecent()
        assertThat(entries).hasSize(1000)
    }

    @Test
    fun `thread safety - concurrent reads return consistent snapshots`() {
        val store = InMemoryDiagnosticsStore(100)
        (1L..50L).forEach { store.append(entry(it)) }

        val results = mutableListOf<List<DiagnosticEntry>>()
        val latch = CountDownLatch(5)
        val executor = Executors.newFixedThreadPool(5)

        repeat(5) {
            executor.submit {
                synchronized(results) {
                    results.add(store.getRecent())
                }
                latch.countDown()
            }
        }

        latch.await()
        executor.shutdown()

        // All reads should return the same snapshot
        results.forEach { snapshot ->
            assertThat(snapshot).hasSize(50)
        }
    }
}
