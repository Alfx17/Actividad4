package com.escom.buscaminas.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Locale

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_settings")

/**
 * Repositorio para gestionar las preferencias del usuario.
 * Utiliza Jetpack DataStore para persistir los datos de forma asíncrona y segura.
 *
 * @param context El contexto de la aplicación, necesario para inicializar DataStore.
 */
class UserPreferencesRepository(private val context: Context) {

    // Objeto privado para mantener las claves de las preferencias de forma organizada y segura.
    private object PreferencesKeys {
        val IS_DARK_MODE = booleanPreferencesKey("is_dark_mode_enabled")
        val APP_LANGUAGE = stringPreferencesKey("app_language")
    }

    /**
     * Un Flow que emite el estado actual del modo oscuro.
     * Los componentes de la UI pueden observar este Flow para reaccionar a los cambios.
     * El valor por defecto es 'false' (modo claro).
     */
    val isDarkMode: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.IS_DARK_MODE] ?: false
    }

    /**
     * Un Flow que emite el código de idioma actual (ej. "en", "es").
     * El valor por defecto es el idioma del sistema.
     */
    val appLanguage: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.APP_LANGUAGE] ?: Locale.getDefault().language
    }

    /**
     * Función suspendida para actualizar el estado del modo oscuro.
     * Se guarda en DataStore de forma segura.
     *
     * @param isDarkMode El nuevo estado para el modo oscuro.
     */
    suspend fun setDarkMode(isDarkMode: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_DARK_MODE] = isDarkMode
        }
    }

    /**
     * Función suspendida para actualizar el idioma de la aplicación.
     *
     * @param languageCode El código del nuevo idioma (ej. "en" para inglés, "es" para español).
     */
    suspend fun setLanguage(languageCode: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.APP_LANGUAGE] = languageCode
        }
    }
}