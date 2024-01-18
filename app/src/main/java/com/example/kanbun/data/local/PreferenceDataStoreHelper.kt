package com.example.kanbun.data.local

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOError
import java.io.IOException
import javax.inject.Inject

private val Context.dataStore by preferencesDataStore(name = "KanbunPreferenceDataStore")

class PreferenceDataStoreHelper @Inject constructor(val context: Application) {

    fun <T> getPreference(key: Preferences.Key<T>, defaultValue: T): Flow<T> =
        context.dataStore.data.map { preferences ->
            preferences[key] ?: defaultValue
        }

    suspend fun <T> getPreferenceFirst(key: Preferences.Key<T>, defaultValue: T): T =
        context.dataStore.data.map { preferences ->
            preferences[key] ?: defaultValue
        }.first()

    suspend fun <T> setPreference(key: Preferences.Key<T>, value: T) {
        context.dataStore.edit { preferences ->
            preferences[key] = value
        }
    }
}