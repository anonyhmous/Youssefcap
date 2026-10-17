package com.driveflow.driver.ui.screens.rules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.driveflow.driver.core.di.AppGraph
import com.driveflow.driver.core.model.DistanceUnit
import com.driveflow.driver.core.model.LongTripMode
import com.driveflow.driver.core.model.Policy
import com.driveflow.driver.core.model.RideApp
import com.driveflow.driver.core.model.StopsMode
import com.driveflow.driver.ui.components.normalizeNumericInput
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class RulesFormState(
    val minPricePerKm: String = "8.0",
    val currency: String = "EGP",
    val distanceUnit: DistanceUnit = DistanceUnit.KM,
    val governorate: String = "cairo",
    val minTotalKm: String = "",
    val maxTotalKm: String = "",
    val maxTripKm: String = "",
    val maxPickupKm: String = "",
    val maxPickupMinutes: String = "",
    val freePickupKm: String = "",
    val minRiderRating: String = "",
    val minRiderTrips: String = "",
    val maxRequestAgeMinutes: String = "",
    val paymentMode: String = "ALL", // "ALL", "CASH", "CUSTOM"
    val allowedPayments: Set<String> = emptySet(),
    val bannedNoteWords: List<String> = emptyList(),
    val stopsMode: StopsMode = StopsMode.IGNORE,
    val stopFee: String = "",
    val longTripMode: LongTripMode = LongTripMode.IGNORE,
    val longTripKm: String = "",
    val warnOnUberSaver: Boolean = false,
    val fuelCostPerKm: String = "",
    val upkeepCostPerKm: String = "",
    val basisNet: Boolean = false,
    val commissionPct: Map<RideApp, String> = mapOf(
        RideApp.INDRIVE to "9.5",
        RideApp.UBER to "22.0",
        RideApp.DIDI to "15.0"
    ),
    val commissionEnabled: Map<RideApp, Boolean> = mapOf(
        RideApp.INDRIVE to true,
        RideApp.UBER to true,
        RideApp.DIDI to true
    ),
    val negotiationStep: String = "5",
    val maxOffer: String = "",
    val appEnabled: Map<RideApp, Boolean> = mapOf(
        RideApp.INDRIVE to true,
        RideApp.UBER to true,
        RideApp.DIDI to true
    ),
    val errorMessage: String? = null,
    val isSavedSuccess: Boolean = false
)

class RulesViewModel : ViewModel() {
    private val settingsStore = AppGraph.settingsStore

    private val _formState = MutableStateFlow(RulesFormState())
    val formState: StateFlow<RulesFormState> = _formState.asStateFlow()

    init {
        loadPolicy()
    }

    private fun loadPolicy() {
        viewModelScope.launch {
            val policy = settingsStore.policyFlow.first()
            _formState.value = RulesFormState(
                minPricePerKm = if (policy.minPricePerKm > 0.0) policy.minPricePerKm.toString() else "8.0",
                currency = policy.currency,
                distanceUnit = policy.distanceUnit,
                governorate = policy.governorate ?: "cairo",
                minTotalKm = policy.minTotalKm?.toString() ?: "",
                maxTotalKm = policy.maxTotalKm?.toString() ?: "",
                maxTripKm = policy.maxTripKm?.toString() ?: "",
                maxPickupKm = policy.maxPickupKm?.toString() ?: "",
                maxPickupMinutes = policy.maxPickupMinutes?.toString() ?: "",
                freePickupKm = policy.freePickupKm?.toString() ?: "",
                minRiderRating = policy.minRiderRating?.toString() ?: "",
                minRiderTrips = policy.minRiderTrips?.toString() ?: "",
                maxRequestAgeMinutes = policy.maxRequestAgeMinutes?.toString() ?: "",
                paymentMode = if (policy.allowedPayments.isEmpty()) "ALL" else if (policy.allowedPayments == setOf("CASH")) "CASH" else "CUSTOM",
                allowedPayments = policy.allowedPayments,
                bannedNoteWords = policy.bannedNoteWords,
                stopsMode = policy.stopsMode,
                stopFee = policy.stopFee?.toString() ?: "",
                longTripMode = policy.longTripMode,
                longTripKm = policy.longTripKm?.toString() ?: "",
                warnOnUberSaver = policy.warnOnUberSaver,
                fuelCostPerKm = policy.fuelCostPerKm?.toString() ?: "",
                upkeepCostPerKm = policy.upkeepCostPerKm?.toString() ?: "",
                basisNet = policy.basisNet,
                commissionPct = policy.commissionPct.mapValues { it.value.toString() },
                commissionEnabled = policy.commissionEnabled,
                negotiationStep = policy.negotiationStep.toString(),
                maxOffer = policy.maxOffer?.toString() ?: "",
                appEnabled = policy.appEnabled
            )
        }
    }

    fun updateForm(transform: (RulesFormState) -> RulesFormState) {
        _formState.value = transform(_formState.value).copy(isSavedSuccess = false, errorMessage = null)
    }

    fun addBannedWord(word: String) {
        val trimmed = word.trim()
        if (trimmed.isNotEmpty() && !_formState.value.bannedNoteWords.contains(trimmed)) {
            _formState.value = _formState.value.copy(
                bannedNoteWords = _formState.value.bannedNoteWords + trimmed
            )
        }
    }

    fun removeBannedWord(word: String) {
        _formState.value = _formState.value.copy(
            bannedNoteWords = _formState.value.bannedNoteWords - word
        )
    }

    fun savePolicy(onSuccess: () -> Unit) {
        val state = _formState.value
        val minPrice = normalizeNumericInput(state.minPricePerKm).toDoubleOrNull()
        if (minPrice == null || minPrice <= 0.0) {
            _formState.value = state.copy(errorMessage = "يرجى تحديد أقل سعر للكيلو بشكل صحيح")
            return
        }

        val minTotal = normalizeNumericInput(state.minTotalKm).toDoubleOrNull()
        val maxTotal = normalizeNumericInput(state.maxTotalKm).toDoubleOrNull()
        if (minTotal != null && maxTotal != null && minTotal > maxTotal) {
            _formState.value = state.copy(errorMessage = "أقل مسافة لا يمكن أن تكون أكبر من أقصى مسافة")
            return
        }

        val rating = normalizeNumericInput(state.minRiderRating).toDoubleOrNull()
        if (rating != null && (rating < 0.0 || rating > 5.0)) {
            _formState.value = state.copy(errorMessage = "تقييم الراكب يجب أن يكون بين 0 و 5")
            return
        }

        val commPctMap = state.commissionPct.mapNotNull { (app, str) ->
            val num = normalizeNumericInput(str).toDoubleOrNull()
            if (num != null) app to num else null
        }.toMap()

        val payments = when (state.paymentMode) {
            "CASH" -> setOf("CASH")
            "CUSTOM" -> state.allowedPayments
            else -> emptySet()
        }

        val policy = Policy(
            minPricePerKm = minPrice,
            minTotalKm = minTotal,
            maxTotalKm = maxTotal,
            maxTripKm = normalizeNumericInput(state.maxTripKm).toDoubleOrNull(),
            maxPickupKm = normalizeNumericInput(state.maxPickupKm).toDoubleOrNull(),
            maxPickupMinutes = normalizeNumericInput(state.maxPickupMinutes).toIntOrNull(),
            freePickupKm = normalizeNumericInput(state.freePickupKm).toDoubleOrNull(),
            minRiderRating = rating,
            minRiderTrips = normalizeNumericInput(state.minRiderTrips).toIntOrNull(),
            maxRequestAgeMinutes = normalizeNumericInput(state.maxRequestAgeMinutes).toIntOrNull(),
            negotiationStep = normalizeNumericInput(state.negotiationStep).toIntOrNull() ?: 5,
            maxOffer = normalizeNumericInput(state.maxOffer).toDoubleOrNull(),
            currency = state.currency,
            governorate = state.governorate,
            allowedPayments = payments,
            bannedNoteWords = state.bannedNoteWords,
            stopsMode = state.stopsMode,
            stopFee = normalizeNumericInput(state.stopFee).toDoubleOrNull(),
            longTripMode = state.longTripMode,
            longTripKm = normalizeNumericInput(state.longTripKm).toDoubleOrNull(),
            warnOnUberSaver = state.warnOnUberSaver,
            fuelCostPerKm = normalizeNumericInput(state.fuelCostPerKm).toDoubleOrNull(),
            upkeepCostPerKm = normalizeNumericInput(state.upkeepCostPerKm).toDoubleOrNull(),
            commissionPct = commPctMap,
            commissionEnabled = state.commissionEnabled,
            appEnabled = state.appEnabled,
            distanceUnit = state.distanceUnit,
            basisNet = state.basisNet
        )

        viewModelScope.launch {
            settingsStore.setPolicy(policy)
            _formState.value = state.copy(isSavedSuccess = true, errorMessage = null)
            onSuccess()
        }
    }
}
