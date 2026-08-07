package com.yashjayswal.dairy.ai.llm

import com.google.mlkit.genai.common.FeatureStatus
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Covers only the pure availability-decision logic in
 * [AiCoreGemmaInferenceEngine]. The real Generation/GenerativeModel calls
 * need Google Play services on a real device, so they're out of scope for a
 * JVM unit test — see docs/RUNNING_ON_DEVICE.md.
 */
class AiCoreGemmaInferenceEngineTest {

    @Test
    fun `available status is used immediately without downloading`() {
        val decision = AiCoreGemmaInferenceEngine.decideAvailability(FeatureStatus.AVAILABLE)

        assertEquals(AiCoreGemmaInferenceEngine.AvailabilityDecision.USE_IMMEDIATELY, decision)
    }

    @Test
    fun `downloadable status triggers a download attempt`() {
        val decision = AiCoreGemmaInferenceEngine.decideAvailability(FeatureStatus.DOWNLOADABLE)

        assertEquals(AiCoreGemmaInferenceEngine.AvailabilityDecision.ATTEMPT_DOWNLOAD, decision)
    }

    @Test
    fun `downloading status also triggers a download attempt (join the in-progress download)`() {
        val decision = AiCoreGemmaInferenceEngine.decideAvailability(FeatureStatus.DOWNLOADING)

        assertEquals(AiCoreGemmaInferenceEngine.AvailabilityDecision.ATTEMPT_DOWNLOAD, decision)
    }

    @Test
    fun `unavailable status is never used`() {
        val decision = AiCoreGemmaInferenceEngine.decideAvailability(FeatureStatus.UNAVAILABLE)

        assertEquals(AiCoreGemmaInferenceEngine.AvailabilityDecision.UNAVAILABLE, decision)
    }
}
