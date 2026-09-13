package com.yashjayswal.dairy.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign

/**
 * A fully borderless text field -- no box, no underline, no reserved
 * label/helper-text chrome -- just a cursor over plain text on the
 * background, matching the reference "My Diary" write-screen look
 * (analyzed 2026-09-13, not stored in this repo -- see docs/TODO.md).
 * Material3's [androidx.compose.material3.TextField] reserves visible
 * padding/decoration space even when empty, which read as "screen
 * wastage" against that reference.
 */
@Composable
fun PlainTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = LocalTextStyle.current,
    singleLine: Boolean = false,
    minLines: Int = 1
) {
    val mergedStyle = textStyle.copy(color = LocalContentColor.current)
    Box(modifier = modifier) {
        if (value.isEmpty()) {
            Text(
                placeholder,
                style = mergedStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Start
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = mergedStyle,
            singleLine = singleLine,
            minLines = minLines,
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
        )
    }
}
