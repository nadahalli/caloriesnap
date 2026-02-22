package com.tejaswin.caloriesnap.data

import org.junit.Assert.assertEquals
import org.junit.Test

class EstimateParserTest {

    @Test
    fun `parseResponse with valid JSON`() {
        val json = """{"food": "Chicken Salad", "calories": 350, "protein_g": 30, "carbs_g": 15, "fat_g": 18}"""
        val result = EstimateParser.parseResponse(json)
        assertEquals("Chicken Salad", result.foodName)
        assertEquals(350, result.calories)
        assertEquals(30f, result.proteinG, 0.01f)
        assertEquals(15f, result.carbsG, 0.01f)
        assertEquals(18f, result.fatG, 0.01f)
    }

    @Test
    fun `parseResponse strips markdown code fences`() {
        val input = """```json
{"food": "Pizza", "calories": 285, "protein_g": 12, "carbs_g": 36, "fat_g": 10}
```"""
        val result = EstimateParser.parseResponse(input)
        assertEquals("Pizza", result.foodName)
        assertEquals(285, result.calories)
    }

    @Test
    fun `parseResponse extracts JSON from surrounding text`() {
        val input = """Here is the result: {"food": "Rice Bowl", "calories": 400, "protein_g": 8, "carbs_g": 80, "fat_g": 2} hope this helps!"""
        val result = EstimateParser.parseResponse(input)
        assertEquals("Rice Bowl", result.foodName)
        assertEquals(400, result.calories)
    }

    @Test
    fun `parseResponse returns fallback for garbage input`() {
        val result = EstimateParser.parseResponse("not json at all")
        assertEquals("Could not identify", result.foodName)
        assertEquals(0, result.calories)
    }

    @Test
    fun `parseResponse returns fallback for empty string`() {
        val result = EstimateParser.parseResponse("")
        assertEquals("Could not identify", result.foodName)
    }

    @Test
    fun `parseResponse uses defaults for missing fields`() {
        val json = """{"food": "Mystery"}"""
        val result = EstimateParser.parseResponse(json)
        assertEquals("Mystery", result.foodName)
        assertEquals(0, result.calories)
        assertEquals(0f, result.proteinG, 0.01f)
    }

    @Test
    fun `parseResponse uses default food name when missing`() {
        val json = """{"calories": 100}"""
        val result = EstimateParser.parseResponse(json)
        assertEquals("Unknown food", result.foodName)
    }

    @Test
    fun `buildPrompt without extras`() {
        val prompt = EstimateParser.buildPrompt(emptyList())
        assert("Respond ONLY with JSON" in prompt)
        assert("also contains" !in prompt)
    }

    @Test
    fun `buildPrompt with extras`() {
        val prompt = EstimateParser.buildPrompt(listOf("Butter", "Cheese"))
        assert("Butter, Cheese" in prompt)
        assert("Adjust the calorie" in prompt)
    }
}
