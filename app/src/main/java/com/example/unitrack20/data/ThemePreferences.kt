package com.example.unitrack20.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.themeDataStore by preferencesDataStore("theme_prefs")

object ThemePreferences {
    private val DARK_KEY = booleanPreferencesKey("dark_theme")
    private val DYNAMIC_KEY = booleanPreferencesKey("dynamic_color")
    private val PRIMARY_COLOR = intPreferencesKey("primary_color")

    fun isDarkFlow(context: Context): Flow<Boolean?> = context.themeDataStore.data.map { it[DARK_KEY] }
    fun dynamicColorFlow(context: Context): Flow<Boolean> = context.themeDataStore.data.map { it[DYNAMIC_KEY] ?: true }
    fun primaryColorFlow(context: Context): Flow<Int?> = context.themeDataStore.data.map { it[PRIMARY_COLOR] }

    suspend fun saveDark(context: Context, dark: Boolean?) {
        context.themeDataStore.edit { prefs ->
            if (dark == null) prefs.remove(DARK_KEY) else prefs[DARK_KEY] = dark
        }
    }

    suspend fun saveDynamic(context: Context, dynamic: Boolean) {
        context.themeDataStore.edit { prefs -> prefs[DYNAMIC_KEY] = dynamic }
    }

    suspend fun savePrimaryColor(context: Context, color: Int?) {
        context.themeDataStore.edit { prefs ->
            if (color == null) prefs.remove(PRIMARY_COLOR) else prefs[PRIMARY_COLOR] = color
        }
    }
}

