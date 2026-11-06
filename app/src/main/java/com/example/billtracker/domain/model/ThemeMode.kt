package com.example.billtracker.domain.model

enum class ThemeMode(val displayName: String, val isDark: Boolean) {
    // Dark Themes
    MIDNIGHT("Midnight", true),
    GRAPHITE("Graphite", true),

    // Light Themes
    CLEAN("Clean", false),
    WARM("Warm", false);

    companion object {
        fun fromString(name: String?): ThemeMode {
            return when (name?.uppercase()) {
                "MIDNIGHT" -> MIDNIGHT
                "GRAPHITE" -> GRAPHITE
                "CLEAN", "LIGHT" -> CLEAN
                "WARM" -> WARM
                "DARK" -> MIDNIGHT
                else -> CLEAN
            }
        }
    }
}
