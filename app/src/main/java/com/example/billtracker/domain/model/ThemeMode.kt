package com.example.billtracker.domain.model

enum class ThemeMode(
    val displayName: String,
    val subtitle: String,
    val isDark: Boolean
) {
    FUTURISTIC(
        "Futuristic / Fictional UI",
        "Cyberpunk control dashboard with neon cyan & violet glow",
        true
    ),
    LIQUID_GLASS(
        "Liquid Glass UI",
        "Fluid deep-ocean surfaces with layered reflections & aqua accents",
        true
    ),
    CLAYMORPHISM(
        "Claymorphism UI",
        "Tactile 3D rounded surfaces with soft clay shadows & friendly contrast",
        false
    ),
    GLASSMORPHISM(
        "Glassmorphism UI",
        "Frosted translucent glass panels with clean fintech gradients",
        false
    );

    companion object {
        fun fromString(name: String?): ThemeMode {
            return when (name?.uppercase()) {
                "FUTURISTIC", "MIDNIGHT" -> FUTURISTIC
                "LIQUID_GLASS", "GRAPHITE" -> LIQUID_GLASS
                "CLAYMORPHISM", "WARM" -> CLAYMORPHISM
                "GLASSMORPHISM", "CLEAN", "LIGHT" -> GLASSMORPHISM
                "DARK" -> FUTURISTIC
                else -> GLASSMORPHISM
            }
        }
    }
}
