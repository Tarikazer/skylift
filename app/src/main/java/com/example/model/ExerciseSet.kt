package com.example.model

import org.json.JSONArray
import org.json.JSONObject

data class ExerciseSet(
    val setNumber: Int,
    val weightKg: Double,
    val reps: Int,
    val isCompleted: Boolean = false
) {
    fun toJsonObject(): JSONObject {
        return JSONObject().apply {
            put("setNumber", setNumber)
            put("weightKg", weightKg)
            put("reps", reps)
            put("isCompleted", isCompleted)
        }
    }

    companion object {
        fun fromJsonObject(json: JSONObject): ExerciseSet {
            return ExerciseSet(
                setNumber = json.optInt("setNumber", 1),
                weightKg = json.optDouble("weightKg", 0.0),
                reps = json.optInt("reps", 10),
                isCompleted = json.optBoolean("isCompleted", false)
            )
        }

        fun listToJsonString(sets: List<ExerciseSet>): String {
            val jsonArray = JSONArray()
            sets.forEach { jsonArray.put(it.toJsonObject()) }
            return jsonArray.toString()
        }

        fun listFromJsonString(jsonString: String?): List<ExerciseSet> {
            if (jsonString.isNullOrBlank()) return emptyList()
            return try {
                val array = JSONArray(jsonString)
                val list = mutableListOf<ExerciseSet>()
                for (i in 0 until array.length()) {
                    list.add(fromJsonObject(array.getJSONObject(i)))
                }
                list
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
}
