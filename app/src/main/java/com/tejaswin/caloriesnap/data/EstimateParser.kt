package com.tejaswin.caloriesnap.data

import org.json.JSONObject

object EstimateParser {

    fun buildPrompt(extras: List<String>): String = buildString {
        append("Look at this food photo. Identify the food items and estimate total calories. ")
        append("Respond ONLY with JSON, no other text: ")
        append("{\"food\": \"name\", \"calories\": N, \"protein_g\": N, \"carbs_g\": N, \"fat_g\": N}")
        if (extras.isNotEmpty()) {
            append(" The food also contains: ${extras.joinToString(", ")}. Adjust the calorie and macro estimates accordingly.")
        }
    }

    fun parseResponse(text: String): FoodEstimate {
        val jsonStr = text
            .replace("```json", "")
            .replace("```", "")
            .trim()
            .let { raw ->
                val start = raw.indexOf('{')
                val end = raw.lastIndexOf('}')
                if (start >= 0 && end > start) raw.substring(start, end + 1) else raw
            }

        return try {
            val json = JSONObject(jsonStr)
            FoodEstimate(
                foodName = json.optString("food", "Unknown food"),
                calories = json.optInt("calories", 0),
                proteinG = json.optDouble("protein_g", 0.0).toFloat(),
                carbsG = json.optDouble("carbs_g", 0.0).toFloat(),
                fatG = json.optDouble("fat_g", 0.0).toFloat(),
            )
        } catch (e: Exception) {
            FoodEstimate(
                foodName = "Could not identify",
                calories = 0,
                proteinG = 0f,
                carbsG = 0f,
                fatG = 0f,
            )
        }
    }
}
