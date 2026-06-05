package com.namesupport.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "namesupport_prefs")

class AppPreferences(private val context: Context) {

    companion object {
        private val KEY_FIRST_SCAN_DONE = booleanPreferencesKey("has_completed_first_scan")
        private val KEY_MONITORING_ENABLED = booleanPreferencesKey("is_monitoring_enabled")
    }

    val hasCompletedFirstScan: Flow<Boolean> =
        context.dataStore.data.map { it[KEY_FIRST_SCAN_DONE] ?: false }

    val isMonitoringEnabled: Flow<Boolean> =
        context.dataStore.data.map { it[KEY_MONITORING_ENABLED] ?: true }

    suspend fun setHasCompletedFirstScan(value: Boolean) {
        context.dataStore.edit { it[KEY_FIRST_SCAN_DONE] = value }
    }

    suspend fun setMonitoringEnabled(value: Boolean) {
        context.dataStore.edit { it[KEY_MONITORING_ENABLED] = value }
    }
}
