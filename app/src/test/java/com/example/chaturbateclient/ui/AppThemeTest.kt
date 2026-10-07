package com.example.chaturbateclient.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class AppThemeTest {
    @Test
    fun mapsEveryLabelBackToItsTheme() {
        AppTheme.entries.forEach { theme ->
            assertEquals(theme, AppTheme.fromLabel(theme.label))
        }
    }

    @Test
    fun defaultsToOledForUnknownOrMissingLabel() {
        assertEquals(AppTheme.Oled, AppTheme.fromLabel("Nonsense"))
        assertEquals(AppTheme.Oled, AppTheme.fromLabel(null))
        assertEquals(AppTheme.Oled, AppTheme.fromLabel(""))
    }
}
