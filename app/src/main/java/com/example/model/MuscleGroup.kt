package com.example.model

enum class MuscleGroup(val displayName: String, val iconName: String) {
    CHEST("Chest", "fitness_center"),
    BACK("Back", "accessibility_new"),
    LEGS("Legs", "directions_run"),
    SHOULDERS("Shoulders", "sports_gymnastics"),
    ARMS("Arms", "sports_martial_arts"),
    CORE("Core", "self_improvement"),
    CARDIO("Cardio", "speed"),
    FULL_BODY("Full Body", "flash_on");

    companion object {
        fun fromString(name: String): MuscleGroup {
            return entries.firstOrNull { it.displayName.equals(name, ignoreCase = true) || it.name.equals(name, ignoreCase = true) }
                ?: FULL_BODY
        }
    }
}
