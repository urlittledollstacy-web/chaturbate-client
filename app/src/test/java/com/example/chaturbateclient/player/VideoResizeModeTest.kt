package com.example.chaturbateclient.player

import org.junit.Assert.assertEquals
import org.junit.Test

class VideoResizeModeTest {
    @Test
    fun mapsEveryLabelBackToItsMode() {
        VideoResizeMode.entries.forEach { mode ->
            assertEquals(mode, VideoResizeMode.fromLabel(mode.label))
        }
    }

    @Test
    fun defaultsToOriginalForUnknownOrMissingLabel() {
        assertEquals(VideoResizeMode.Original, VideoResizeMode.fromLabel("Nonsense"))
        assertEquals(VideoResizeMode.Original, VideoResizeMode.fromLabel(null))
        assertEquals(VideoResizeMode.Original, VideoResizeMode.fromLabel(""))
    }
}
