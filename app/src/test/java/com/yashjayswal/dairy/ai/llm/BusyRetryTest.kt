package com.yashjayswal.dairy.ai.llm

import com.google.mlkit.genai.common.GenAiException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class BusyRetryTest {

    private class BusyException : Exception("busy")

    private val isBusy: (Throwable) -> Boolean = { it is BusyException }

    @Test
    fun `busy then success retries and returns the result`() = runTest {
        var calls = 0
        val sleeps = mutableListOf<Long>()

        val result = retryWhileBusy(isBusy = isBusy, sleep = { sleeps += it }) {
            calls++
            if (calls < 3) throw BusyException()
            "ok"
        }

        assertEquals("ok", result)
        assertEquals(3, calls)
        assertEquals(listOf(2_000L, 4_000L), sleeps)
    }

    @Test
    fun `busy every time gives up after the attempt cap with a clear error`() = runTest {
        var calls = 0
        val sleeps = mutableListOf<Long>()

        try {
            retryWhileBusy(maxAttempts = 4, isBusy = isBusy, sleep = { sleeps += it }) {
                calls++
                throw BusyException()
            }
            fail("expected BusyRetriesExhaustedException")
        } catch (e: BusyRetriesExhaustedException) {
            assertEquals(4, e.attempts)
            assertTrue(e.cause is BusyException)
            assertTrue(e.message!!.contains("busy"))
        }

        assertEquals(4, calls)
        assertEquals(listOf(2_000L, 4_000L, 8_000L), sleeps)
    }

    @Test
    fun `non-busy error is thrown immediately without retrying`() = runTest {
        var calls = 0
        val boom = IllegalStateException("boom")

        try {
            retryWhileBusy(isBusy = isBusy, sleep = { fail("must not sleep") }) {
                calls++
                throw boom
            }
            fail("expected the original exception")
        } catch (e: IllegalStateException) {
            assertSame(boom, e)
        }

        assertEquals(1, calls)
    }

    @Test
    fun `total wait cap stops retrying before the attempt cap`() = runTest {
        var calls = 0
        val sleeps = mutableListOf<Long>()

        try {
            retryWhileBusy(
                maxAttempts = 10,
                initialDelayMs = 2_000,
                maxTotalWaitMs = 5_000,
                isBusy = isBusy,
                sleep = { sleeps += it }
            ) {
                calls++
                throw BusyException()
            }
            fail("expected BusyRetriesExhaustedException")
        } catch (e: BusyRetriesExhaustedException) {
            assertEquals(2, e.attempts)
        }

        assertEquals(2, calls)
        assertEquals(listOf(2_000L), sleeps)
    }

    @Test
    fun `success on first try never sleeps`() = runTest {
        val result = retryWhileBusy(isBusy = isBusy, sleep = { fail("must not sleep") }) { 42 }

        assertEquals(42, result)
    }

    @Test
    fun `isBusyError matches only the BUSY error code`() {
        val busy = GenAiException("busy", RuntimeException(), GenAiException.ErrorCode.BUSY)
        val background = GenAiException("bg", RuntimeException(), GenAiException.ErrorCode.BACKGROUND_USE_BLOCKED)

        assertTrue(AiCoreGemmaInferenceEngine.isBusyError(busy))
        assertFalse(AiCoreGemmaInferenceEngine.isBusyError(background))
        assertFalse(AiCoreGemmaInferenceEngine.isBusyError(IllegalStateException("other")))
    }

    @Test
    fun `isBusyError also matches a BUSY error wrapped in another exception`() {
        val busy = GenAiException("busy", RuntimeException(), GenAiException.ErrorCode.BUSY)

        assertTrue(AiCoreGemmaInferenceEngine.isBusyError(RuntimeException("wrapper", busy)))
    }
}
