package com.driveflow.driver.ui.screens.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.driveflow.driver.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class ReportCategory(val code: String, val labelRes: Int) {
    READING("reading", R.string.report_cat_reading),
    VERDICT("verdict", R.string.report_cat_verdict),
    OVERLAY("overlay", R.string.report_cat_overlay),
    SUBSCRIPTION("subscription", R.string.report_cat_subscription),
    IDEA("idea", R.string.report_cat_idea),
    OTHER("other", R.string.report_cat_other)
}

data class ReportFormState(
    val category: ReportCategory = ReportCategory.READING,
    val message: String = "",
    val attachedImageUri: String? = null,
    val isSending: Boolean = false,
    val submittedId: Int? = null,
    val errorMessage: String? = null
)

class ReportViewModel : ViewModel() {
    private val _formState = MutableStateFlow(ReportFormState())
    val formState: StateFlow<ReportFormState> = _formState.asStateFlow()

    fun setCategory(category: ReportCategory) {
        _formState.value = _formState.value.copy(category = category)
    }

    fun setMessage(msg: String) {
        _formState.value = _formState.value.copy(message = msg, errorMessage = null)
    }

    fun setAttachedImage(uri: String?) {
        _formState.value = _formState.value.copy(attachedImageUri = uri)
    }

    fun submitReport() {
        val current = _formState.value
        if (current.message.trim().length < 5) {
            _formState.value = current.copy(errorMessage = "يرجى كتابة تفاصيل البلاغ (5 حروف على الأقل)")
            return
        }

        viewModelScope.launch {
            _formState.value = current.copy(isSending = true, errorMessage = null)
            kotlinx.coroutines.delay(1000)
            val randomId = (1000..9999).random()
            _formState.value = current.copy(
                isSending = false,
                submittedId = randomId,
                message = "",
                attachedImageUri = null
            )
        }
    }
}
