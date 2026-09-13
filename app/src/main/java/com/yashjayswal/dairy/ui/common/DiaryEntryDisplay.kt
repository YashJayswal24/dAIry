package com.yashjayswal.dairy.ui.common

import com.yashjayswal.dairy.domain.model.DiaryEntry

private const val DERIVED_TITLE_MAX_LENGTH = 50

// Entries created before the title field existed (and any saved with a
// blank title) fall back to a title derived from the body text, rather
// than showing nothing.
fun DiaryEntry.displayTitle(): String =
    title.ifBlank {
        val firstLine = text.lineSequence().firstOrNull { it.isNotBlank() }?.trim().orEmpty()
        if (firstLine.length > DERIVED_TITLE_MAX_LENGTH) {
            firstLine.take(DERIVED_TITLE_MAX_LENGTH).trimEnd() + "…"
        } else {
            firstLine
        }
    }
