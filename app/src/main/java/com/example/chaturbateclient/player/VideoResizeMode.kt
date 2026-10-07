package com.example.chaturbateclient.player

/**
 * How the video frame is fitted into the available surface.
 *
 * Original keeps the source aspect ratio and letterboxes; Zoom fills the surface
 * and crops the overflow; Stretch fills the surface and distorts the picture.
 */
enum class VideoResizeMode(val label: String) {
    Original("Original"),
    Zoom("Zoom"),
    Stretch("Stretch");

    companion object {
        fun fromLabel(label: String?): VideoResizeMode =
            entries.firstOrNull { it.label == label } ?: Original
    }
}
