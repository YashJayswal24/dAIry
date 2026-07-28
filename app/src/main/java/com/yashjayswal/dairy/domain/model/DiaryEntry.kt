package com.yashjayswal.dairy.domain.model

data class DiaryEntry(
    val id: Long = 0,
    val text: String,
    val emotion: Emotion,
    val emotionIntensity: Int, // 1..5
    val createdAt: Long,
    val embedding: FloatArray? = null
)

enum class Emotion {
    JOY, SADNESS, ANGER, FEAR, SURPRISE, CALM, NEUTRAL
}
