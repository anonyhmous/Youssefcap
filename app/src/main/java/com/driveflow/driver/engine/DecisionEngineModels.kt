package com.driveflow.driver.engine

import com.driveflow.driver.core.model.LicenseStatus

/**
 * Operation mode for Heading Home feature.
 */
enum class HomeWayMode {
    HOME,
    PLACE
}

/**
 * State of Heading Home (On My Way) passed into DecisionEngine.
 */
data class HomeWayState(
    val enabled: Boolean = false,
    val lat: Double? = null,
    val lon: Double? = null,
    val label: String? = null,
    val mode: HomeWayMode = HomeWayMode.HOME
)

/**
 * Execution context for request evaluation.
 */
data class DecisionContext(
    val nowMillis: Long = System.currentTimeMillis(),
    val licenseStatus: LicenseStatus = LicenseStatus.TRIAL,
    val detailAvailable: Boolean = false
)

/**
 * Geographic coordinate representation (latitude, longitude).
 */
data class LatLon(
    val lat: Double,
    val lon: Double
)

/**
 * Interface to resolve text addresses to coordinates for heading-home bearing checks.
 */
interface GeoResolver {
    fun resolve(address: String): LatLon?
}

/**
 * Default stub resolver returning null (real geocoder out of scope).
 */
object StubGeoResolver : GeoResolver {
    override fun resolve(address: String): LatLon? = null
}

/**
 * Stable reason codes for all decision outcomes.
 */
object ReasonCodes {
    // Gray
    const val APP_DISABLED = "APP_DISABLED"
    const val DATA_MISSING = "DATA_MISSING"

    // Hard Red
    const val PICKUP_TOO_FAR = "PICKUP_TOO_FAR"
    const val PICKUP_TOO_LONG = "PICKUP_TOO_LONG"
    const val TRIP_TOO_LONG = "TRIP_TOO_LONG"
    const val TOTAL_TOO_SHORT = "TOTAL_TOO_SHORT"
    const val TOTAL_TOO_LONG = "TOTAL_TOO_LONG"
    const val RATING_LOW = "RATING_LOW"
    const val TRIPS_FEW = "TRIPS_FEW"
    const val REQUEST_OLD = "REQUEST_OLD"
    const val PAYMENT_NOT_ALLOWED = "PAYMENT_NOT_ALLOWED"
    const val NOTE_BANNED_WORD = "NOTE_BANNED_WORD"
    const val AREA_AVOIDED = "AREA_AVOIDED"
    const val STOPS_AVOID = "STOPS_AVOID"
    const val LONG_TRIP_AVOID = "LONG_TRIP_AVOID"
    const val PRICE_LOW = "PRICE_LOW"

    // Orange
    const val STOPS_WARN = "STOPS_WARN"
    const val LONG_TRIP_WARN = "LONG_TRIP_WARN"
    const val UBER_SAVER = "UBER_SAVER"
    const val PRICE_BORDERLINE = "PRICE_BORDERLINE"
    const val LOW_CONFIDENCE = "LOW_CONFIDENCE"
    const val AREA_UNKNOWN = "AREA_UNKNOWN"

    // Green
    const val ACCEPTABLE = "ACCEPTABLE"
}

/**
 * Stable flags attached to Verdict.
 */
object DecisionFlags {
    const val PREFERRED_AREA = "PREFERRED_AREA"
    const val STOPS_PREFERRED = "STOPS_PREFERRED"
    const val HOME_DIRECTION = "HOME_DIRECTION"
    const val FREE_PICKUP_USED = "FREE_PICKUP_USED"
    const val NET_BASIS = "NET_BASIS"
    const val OPEN_FOR_ROUTE = "OPEN_FOR_ROUTE"
    const val FREE_PLAN = "FREE_PLAN"
}
