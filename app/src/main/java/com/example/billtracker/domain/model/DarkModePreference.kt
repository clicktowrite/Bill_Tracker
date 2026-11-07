package com.example.billtracker.domain.model

enum class DarkModePreference(
    val displayName: String,
    val subtitle: String
) {
    SYSTEM(
        "System Default",
        "Follow your device system theme settings"
    ),
    LIGHT(
        "Light Mode",
        "Always use light appearance styling"
    ),
    DARK(
        "Night Mode",
        "Always use dark appearance styling"
    );

    companion object {
        fun fromString(name: String?): DarkModePreference {
            return when (name?.uppercase()) {
                "LIGHT" -> LIGHT
                "DARK", "NIGHT" -> DARK
                "SYSTEM", null -> SYSTEM
                else -> SYSTEM
            }
        }
    }
}
