package com.yashjayswal.dairy.ui.common

import com.yashjayswal.dairy.domain.model.Emotion

fun Emotion.label(): String = name.lowercase().replaceFirstChar(Char::uppercase)

fun Emotion.emoji(): String = when (this) {
    Emotion.JOY -> "😊"
    Emotion.SADNESS -> "😢"
    Emotion.ANGER -> "😠"
    Emotion.FEAR -> "😨"
    Emotion.SURPRISE -> "😲"
    Emotion.CALM -> "😌"
    Emotion.NEUTRAL -> "😐"
}
