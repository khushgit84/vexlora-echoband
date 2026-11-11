package com.vexlora.echoband.service

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class RememberedFact(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

class MemoryManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        "echoband_facts_memory",
        Context.MODE_PRIVATE
    )
    private val gson = Gson()
    private val keyFacts = "remembered_facts_list"

    fun saveFact(fact: String): RememberedFact {
        val facts = getFacts().toMutableList()
        val newFact = RememberedFact(text = fact.trim())
        facts.add(0, newFact)
        val json = gson.toJson(facts)
        prefs.edit().putString(keyFacts, json).apply()
        return newFact
    }

    fun getFacts(): List<RememberedFact> {
        return try {
            val json = prefs.getString(keyFacts, null) ?: return emptyList()
            val type = object : TypeToken<List<RememberedFact>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun removeFact(id: String) {
        val facts = getFacts().filter { it.id != id }
        val json = gson.toJson(facts)
        prefs.edit().putString(keyFacts, json).apply()
    }

    fun clearAllFacts() {
        prefs.edit().remove(keyFacts).apply()
    }

    fun getFactsSummary(): String {
        val facts = getFacts()
        if (facts.isEmpty()) return ""
        return buildString {
            append("User Remembered Facts and Preferences:\n")
            facts.forEachIndexed { index, fact ->
                append("${index + 1}. ${fact.text}\n")
            }
        }
    }
}
