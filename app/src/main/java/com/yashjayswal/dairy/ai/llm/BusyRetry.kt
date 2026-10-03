package com.yashjayswal.dairy.ai.llm

import kotlinx.coroutines.delay

/** Thrown when every retry of a busy-rate-limited call was used up. */
class BusyRetriesExhaustedException(val attempts: Int, cause: Throwable) : Exception(
    "The on-device AI is busy right now (rate-limited after $attempts attempts). " +
        "Wait a minute or two and try again.",
    cause
)

/**
 * Runs [block], retrying with exponential backoff (initialDelayMs, then
 * doubled each time) only when [isBusy] says the failure is a transient
 * rate limit. Any other exception is rethrown immediately. Gives up with
 * [BusyRetriesExhaustedException] after [maxAttempts] total attempts, or
 * sooner if the next wait would push total waiting past [maxTotalWaitMs].
 *
 * Plain function with an injectable [sleep] so it's unit-testable without
 * a device or real waiting.
 */
suspend fun <T> retryWhileBusy(
    maxAttempts: Int = 4,
    initialDelayMs: Long = 2_000,
    maxTotalWaitMs: Long = 30_000,
    isBusy: (Throwable) -> Boolean,
    sleep: suspend (Long) -> Unit = { delay(it) },
    onRetry: (attempt: Int, waitMs: Long) -> Unit = { _, _ -> },
    block: suspend () -> T
): T {
    var nextDelayMs = initialDelayMs
    var totalWaitedMs = 0L
    var attempt = 1
    while (true) {
        try {
            return block()
        } catch (e: Throwable) {
            if (!isBusy(e)) throw e
            if (attempt >= maxAttempts || totalWaitedMs + nextDelayMs > maxTotalWaitMs) {
                throw BusyRetriesExhaustedException(attempt, e)
            }
            onRetry(attempt, nextDelayMs)
            sleep(nextDelayMs)
            totalWaitedMs += nextDelayMs
            nextDelayMs *= 2
            attempt++
        }
    }
}
