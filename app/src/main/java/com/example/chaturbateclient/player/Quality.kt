package com.example.chaturbateclient.player

enum class VideoQuality(val label: String, val maxHeight: Int?) {
    Auto("Auto", null),
    P240("240p", 240),
    P360("360p", 360),
    P480("480p", 480),
    P720("720p", 720),
    P1080("1080p", 1080),
    P1440("1440p", 1440),
    P2160("4K", 2160)
}
