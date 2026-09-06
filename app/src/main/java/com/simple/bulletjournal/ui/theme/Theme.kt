package com.simple.bulletjournal.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class NotebookColors(
    val paper: Color,
    val ruledLine: Color,
    val marginLine: Color,
    val text: Color,
    val subtleText: Color,
    val completed: Color,
    val inputContainer: Color,
    val bannerBackground: Color,
    val bannerText: Color,
    val priority: Color,
    val isDark: Boolean
)

val LightNotebookColors = NotebookColors(
    paper = LightPaper,
    ruledLine = LightRuledLine,
    marginLine = LightMarginLine,
    text = LightNoteText,
    subtleText = LightNoteSubtleText,
    completed = LightCompleted,
    inputContainer = LightInputContainer,
    bannerBackground = LightBannerBackground,
    bannerText = LightBannerText,
    priority = LightPriority,
    isDark = false
)

val DarkNotebookColors = NotebookColors(
    paper = DarkPaper,
    ruledLine = DarkRuledLine,
    marginLine = DarkMarginLine,
    text = DarkNoteText,
    subtleText = DarkNoteSubtleText,
    completed = DarkCompleted,
    inputContainer = DarkInputContainer,
    bannerBackground = DarkBannerBackground,
    bannerText = DarkBannerText,
    priority = DarkPriority,
    isDark = true
)

val LocalNotebookColors = staticCompositionLocalOf { LightNotebookColors }

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryContainer,
    secondary = Secondary,
    surface = LightPaper,
    onSurface = LightNoteText,
    background = LightPaper,
    onBackground = LightNoteText
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryContainer,
    onPrimary = Primary,
    primaryContainer = Secondary,
    secondary = PrimaryContainer,
    surface = DarkPaper,
    onSurface = DarkNoteText,
    background = DarkPaper,
    onBackground = DarkNoteText
)

@Composable
fun BulletJournalTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val notebookColors = if (darkTheme) DarkNotebookColors else LightNotebookColors
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(
        LocalNotebookColors provides notebookColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content
        )
    }
}
