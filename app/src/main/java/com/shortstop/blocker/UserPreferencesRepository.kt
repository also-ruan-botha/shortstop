package com.shortstop.blocker

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.shortStopDataStore by preferencesDataStore(name = "shortstop_preferences")

internal data class UserPreferences(
    val onboardingAcknowledged: Boolean,
    val paused: Boolean,
)

internal class UserPreferencesRepository(private val context: Context) {
    val preferences: Flow<UserPreferences> =
        context.shortStopDataStore.data
            .catch { error ->
                if (error is IOException) emit(emptyPreferences()) else throw error
            }
            .map { values ->
                UserPreferences(
                    onboardingAcknowledged = values[ONBOARDING_ACKNOWLEDGED] ?: false,
                    paused = values[PAUSED] ?: false,
                )
            }

    suspend fun acknowledgeOnboarding() {
        context.shortStopDataStore.edit { it[ONBOARDING_ACKNOWLEDGED] = true }
    }

    suspend fun setPaused(paused: Boolean) {
        context.shortStopDataStore.edit { it[PAUSED] = paused }
    }

    private companion object {
        val ONBOARDING_ACKNOWLEDGED = booleanPreferencesKey("onboarding_acknowledged")
        val PAUSED = booleanPreferencesKey("paused")
    }
}
