package com.driveflow.driver.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.driveflow.driver.core.model.Policy
import com.driveflow.driver.core.model.RideApp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException
import java.util.UUID

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "driveflow_settings")

@Serializable
enum class MarkerSize {
    SMALL,
    NORMAL,
    LARGE
}

@Serializable
enum class AlertMode {
    TONE,
    TONE_VIBRATE
}

@Serializable
data class BarPosition(
    val isAuto: Boolean = true,
    val x: Int = 0,
    val y: Int = 0
)

@Serializable
data class HomeWay(
    val enabled: Boolean = false,
    val lat: Double? = null,
    val lon: Double? = null,
    val label: String? = null,
    val placeMode: String? = null
)

@Serializable
data class LicenseCache(
    val signedResponseJson: String = "",
    val receivedAt: Long = 0L
)

@Serializable
data class FreeQuota(
    val date: String = "",
    val listCount: Int = 0,
    val detailCount: Int = 0
)

class SettingsStore(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private companion object Keys {
        val LANGUAGE = stringPreferencesKey("language")
        val POLICY_JSON = stringPreferencesKey("policy_json")
        val MARKER_SIZE = stringPreferencesKey("marker_size")
        val BAR_OPACITY_PERCENT = intPreferencesKey("bar_opacity_percent")
        val BAR_POSITION_AUTO = booleanPreferencesKey("bar_pos_auto")
        val BAR_POSITION_X = intPreferencesKey("bar_pos_x")
        val BAR_POSITION_Y = intPreferencesKey("bar_pos_y")
        val AUTO_OFF_HOURS = intPreferencesKey("auto_off_hours")
        val ALERT_ENABLED = booleanPreferencesKey("alert_enabled")
        val ALERT_MODE = stringPreferencesKey("alert_mode")
        val HOME_WAY_ENABLED = booleanPreferencesKey("home_way_enabled")
        val HOME_WAY_LAT = doublePreferencesKey("home_way_lat")
        val HOME_WAY_LON = doublePreferencesKey("home_way_lon")
        val HOME_WAY_LABEL = stringPreferencesKey("home_way_label")
        val HOME_WAY_PLACE_MODE = stringPreferencesKey("home_way_place_mode")
        val CONSENT_VERSION = intPreferencesKey("consent_version")
        val WELCOME_SEEN = booleanPreferencesKey("welcome_seen")
        val SETUP_DONE = booleanPreferencesKey("setup_done")
        val CONTACT_NAME = stringPreferencesKey("contact_name")
        val CONTACT_PHONE = stringPreferencesKey("contact_phone")
        val LICENSE_CACHE_JSON = stringPreferencesKey("license_cache_json")
        val LICENSE_CACHE_RECEIVED_AT = longPreferencesKey("license_cache_received_at")
        val CLOCK_HIGH_WATER = longPreferencesKey("clock_high_water")
        val INSTALL_ID = stringPreferencesKey("install_id")
        val FREE_QUOTA_DATE = stringPreferencesKey("free_quota_date")
        val FREE_QUOTA_LIST_COUNT = intPreferencesKey("free_quota_list_count")
        val FREE_QUOTA_DETAIL_COUNT = intPreferencesKey("free_quota_detail_count")
        val LAST_RIDE_APP_OPENED_AT = longPreferencesKey("last_ride_app_opened_at")
        val NEWS_LAST_SEEN_ID = intPreferencesKey("news_last_seen_id")

        fun readerConfigKey(app: RideApp) = stringPreferencesKey("reader_cfg_${app.code}")
        fun ocrCalibrationKey(app: RideApp) = stringPreferencesKey("ocr_calib_${app.code}")
    }

    private val prefsFlow: Flow<Preferences> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }

    // 1. Language ("ar" default, or "en")
    val languageFlow: Flow<String> = prefsFlow.map { prefs ->
        prefs[LANGUAGE] ?: "ar"
    }

    suspend fun setLanguage(lang: String) {
        context.dataStore.edit { it[LANGUAGE] = lang }
    }

    // 2. Policy (JSON)
    val policyFlow: Flow<Policy> = prefsFlow.map { prefs ->
        val raw = prefs[POLICY_JSON]
        if (!raw.isNullOrBlank()) {
            try {
                json.decodeFromString<Policy>(raw)
            } catch (_: Exception) {
                Policy()
            }
        } else {
            Policy()
        }
    }

    suspend fun setPolicy(policy: Policy) {
        val serialized = json.encodeToString(policy)
        context.dataStore.edit { it[POLICY_JSON] = serialized }
    }

    // 3. Marker size (SMALL | NORMAL | LARGE, default NORMAL)
    val markerSizeFlow: Flow<MarkerSize> = prefsFlow.map { prefs ->
        val name = prefs[MARKER_SIZE]
        try {
            if (name != null) MarkerSize.valueOf(name) else MarkerSize.NORMAL
        } catch (_: Exception) {
            MarkerSize.NORMAL
        }
    }

    suspend fun setMarkerSize(size: MarkerSize) {
        context.dataStore.edit { it[MARKER_SIZE] = size.name }
    }

    // 4. Bar opacity percent (default 67)
    val barOpacityPercentFlow: Flow<Int> = prefsFlow.map { prefs ->
        prefs[BAR_OPACITY_PERCENT] ?: 67
    }

    suspend fun setBarOpacityPercent(opacity: Int) {
        context.dataStore.edit { it[BAR_OPACITY_PERCENT] = opacity.coerceIn(20, 100) }
    }

    // 5. Bar position (AUTO or custom x,y)
    val barPositionFlow: Flow<BarPosition> = prefsFlow.map { prefs ->
        BarPosition(
            isAuto = prefs[BAR_POSITION_AUTO] ?: true,
            x = prefs[BAR_POSITION_X] ?: 0,
            y = prefs[BAR_POSITION_Y] ?: 0
        )
    }

    suspend fun setBarPosition(position: BarPosition) {
        context.dataStore.edit {
            it[BAR_POSITION_AUTO] = position.isAuto
            it[BAR_POSITION_X] = position.x
            it[BAR_POSITION_Y] = position.y
        }
    }

    // 6. Auto-off hours (1 | 3 | 6 | 0 = never, default 3)
    val autoOffHoursFlow: Flow<Int> = prefsFlow.map { prefs ->
        prefs[AUTO_OFF_HOURS] ?: 3
    }

    suspend fun setAutoOffHours(hours: Int) {
        context.dataStore.edit { it[AUTO_OFF_HOURS] = hours }
    }

    // 7. Alert enabled (default true)
    val alertEnabledFlow: Flow<Boolean> = prefsFlow.map { prefs ->
        prefs[ALERT_ENABLED] ?: true
    }

    suspend fun setAlertEnabled(enabled: Boolean) {
        context.dataStore.edit { it[ALERT_ENABLED] = enabled }
    }

    // 8. Alert mode (TONE | TONE_VIBRATE, default TONE)
    val alertModeFlow: Flow<AlertMode> = prefsFlow.map { prefs ->
        val name = prefs[ALERT_MODE]
        try {
            if (name != null) AlertMode.valueOf(name) else AlertMode.TONE
        } catch (_: Exception) {
            AlertMode.TONE
        }
    }

    suspend fun setAlertMode(mode: AlertMode) {
        context.dataStore.edit { it[ALERT_MODE] = mode.name }
    }

    // 9. Home way (enabled, lat, lon, label, placeMode)
    val homeWayFlow: Flow<HomeWay> = prefsFlow.map { prefs ->
        HomeWay(
            enabled = prefs[HOME_WAY_ENABLED] ?: false,
            lat = prefs[HOME_WAY_LAT],
            lon = prefs[HOME_WAY_LON],
            label = prefs[HOME_WAY_LABEL],
            placeMode = prefs[HOME_WAY_PLACE_MODE]
        )
    }

    suspend fun setHomeWay(homeWay: HomeWay) {
        context.dataStore.edit {
            it[HOME_WAY_ENABLED] = homeWay.enabled
            if (homeWay.lat != null) it[HOME_WAY_LAT] = homeWay.lat else it.remove(HOME_WAY_LAT)
            if (homeWay.lon != null) it[HOME_WAY_LON] = homeWay.lon else it.remove(HOME_WAY_LON)
            if (homeWay.label != null) it[HOME_WAY_LABEL] = homeWay.label else it.remove(HOME_WAY_LABEL)
            if (homeWay.placeMode != null) it[HOME_WAY_PLACE_MODE] = homeWay.placeMode else it.remove(HOME_WAY_PLACE_MODE)
        }
    }

    // 10. Consent version (int)
    val consentVersionFlow: Flow<Int> = prefsFlow.map { prefs ->
        prefs[CONSENT_VERSION] ?: 0
    }

    suspend fun setConsentVersion(version: Int) {
        context.dataStore.edit { it[CONSENT_VERSION] = version }
    }

    // 11. Welcome seen
    val welcomeSeenFlow: Flow<Boolean> = prefsFlow.map { prefs ->
        prefs[WELCOME_SEEN] ?: false
    }

    suspend fun setWelcomeSeen(seen: Boolean) {
        context.dataStore.edit { it[WELCOME_SEEN] = seen }
    }

    // 12. Setup done
    val setupDoneFlow: Flow<Boolean> = prefsFlow.map { prefs ->
        prefs[SETUP_DONE] ?: false
    }

    suspend fun setSetupDone(done: Boolean) {
        context.dataStore.edit { it[SETUP_DONE] = done }
    }

    // 13. Contact name
    val contactNameFlow: Flow<String?> = prefsFlow.map { prefs ->
        prefs[CONTACT_NAME]
    }

    suspend fun setContactName(name: String?) {
        context.dataStore.edit {
            if (name != null) it[CONTACT_NAME] = name else it.remove(CONTACT_NAME)
        }
    }

    // 14. Contact phone
    val contactPhoneFlow: Flow<String?> = prefsFlow.map { prefs ->
        prefs[CONTACT_PHONE]
    }

    suspend fun setContactPhone(phone: String?) {
        context.dataStore.edit {
            if (phone != null) it[CONTACT_PHONE] = phone else it.remove(CONTACT_PHONE)
        }
    }

    // 15. License cache (last signed response JSON + receivedAt)
    val licenseCacheFlow: Flow<LicenseCache?> = prefsFlow.map { prefs ->
        val jsonStr = prefs[LICENSE_CACHE_JSON]
        val receivedAt = prefs[LICENSE_CACHE_RECEIVED_AT] ?: 0L
        if (!jsonStr.isNullOrBlank()) {
            LicenseCache(jsonStr, receivedAt)
        } else {
            null
        }
    }

    suspend fun setLicenseCache(cache: LicenseCache?) {
        context.dataStore.edit {
            if (cache != null) {
                it[LICENSE_CACHE_JSON] = cache.signedResponseJson
                it[LICENSE_CACHE_RECEIVED_AT] = cache.receivedAt
            } else {
                it.remove(LICENSE_CACHE_JSON)
                it.remove(LICENSE_CACHE_RECEIVED_AT)
            }
        }
    }

    // 16. Clock high water (Long, max serverTime seen)
    val clockHighWaterFlow: Flow<Long> = prefsFlow.map { prefs ->
        prefs[CLOCK_HIGH_WATER] ?: 0L
    }

    suspend fun updateClockHighWater(serverTimeMillis: Long) {
        context.dataStore.edit { prefs ->
            val current = prefs[CLOCK_HIGH_WATER] ?: 0L
            if (serverTimeMillis > current) {
                prefs[CLOCK_HIGH_WATER] = serverTimeMillis
            }
        }
    }

    // 17. Install ID (UUID string generated once)
    val installIdFlow: Flow<String> = prefsFlow.map { prefs ->
        prefs[INSTALL_ID] ?: ""
    }

    suspend fun getOrCreateInstallId(): String {
        val current = prefsFlow.first()[INSTALL_ID]
        if (!current.isNullOrBlank()) return current
        val newId = UUID.randomUUID().toString()
        context.dataStore.edit { it[INSTALL_ID] = newId }
        return newId
    }

    // 18. Reader config cache per app
    fun readerConfigFlow(app: RideApp): Flow<String?> = prefsFlow.map { prefs ->
        prefs[readerConfigKey(app)]
    }

    suspend fun setReaderConfig(app: RideApp, configJson: String) {
        context.dataStore.edit { it[readerConfigKey(app)] = configJson }
    }

    // 19. OCR calibration per app
    fun ocrCalibrationFlow(app: RideApp): Flow<String?> = prefsFlow.map { prefs ->
        prefs[ocrCalibrationKey(app)]
    }

    suspend fun setOcrCalibration(app: RideApp, calibrationJson: String) {
        context.dataStore.edit { it[ocrCalibrationKey(app)] = calibrationJson }
    }

    // 20. Free quota (date, listCount, detailCount)
    val freeQuotaFlow: Flow<FreeQuota> = prefsFlow.map { prefs ->
        FreeQuota(
            date = prefs[FREE_QUOTA_DATE] ?: "",
            listCount = prefs[FREE_QUOTA_LIST_COUNT] ?: 0,
            detailCount = prefs[FREE_QUOTA_DETAIL_COUNT] ?: 0
        )
    }

    suspend fun setFreeQuota(quota: FreeQuota) {
        context.dataStore.edit {
            it[FREE_QUOTA_DATE] = quota.date
            it[FREE_QUOTA_LIST_COUNT] = quota.listCount
            it[FREE_QUOTA_DETAIL_COUNT] = quota.detailCount
        }
    }

    // 21. Last ride app opened at (timestamp in millis)
    val lastRideAppOpenedAtFlow: Flow<Long> = prefsFlow.map { prefs ->
        prefs[LAST_RIDE_APP_OPENED_AT] ?: 0L
    }

    suspend fun setLastRideAppOpenedAt(timestamp: Long) {
        context.dataStore.edit { it[LAST_RIDE_APP_OPENED_AT] = timestamp }
    }

    // 22. News last seen ID
    val newsLastSeenIdFlow: Flow<Int> = prefsFlow.map { prefs ->
        prefs[NEWS_LAST_SEEN_ID] ?: 0
    }

    suspend fun setNewsLastSeenId(id: Int) {
        context.dataStore.edit { it[NEWS_LAST_SEEN_ID] = id }
    }
}
