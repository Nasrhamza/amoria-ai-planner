package com.example.amoriaiaplanner.core.util

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "amoria_prefs")

object UserPrefs {
    private val KEY_LAST_EMAIL = stringPreferencesKey("last_email")

    fun lastEmailFlow(context: Context): Flow<String> =
        context.dataStore.data.map { prefs -> prefs[KEY_LAST_EMAIL] ?: "" }

    suspend fun saveLastEmail(context: Context, email: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_LAST_EMAIL] = email
        }
    }
}