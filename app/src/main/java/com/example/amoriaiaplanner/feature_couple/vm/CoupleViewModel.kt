package com.example.amoriaiaplanner.feature_couple.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.amoriaiaplanner.feature_couple.data.CoupleRepository
import com.example.amoriaiaplanner.feature_couple.model.Couple
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class CoupleUiState(
    val myUid: String = "",
    val myEmail: String = "",
    val myCode: String = "",
    val enteredPartnerCode: String = "",
    val linkedCouple: Couple? = null,
    val partnerPreviewUid: String = "",
    val partnerPreviewEmail: String = "",
    val loading: Boolean = false,
    val message: String? = null,
    val showConfirmation: Boolean = false
)

class CoupleViewModel(
    private val repo: CoupleRepository = CoupleRepository()
) : ViewModel() {

    private val _ui = MutableStateFlow(CoupleUiState())
    val ui: StateFlow<CoupleUiState> = _ui

    private var userCoupleListener: ListenerRegistration? = null

    fun load() {
        viewModelScope.launch {
            try {
                _ui.value = _ui.value.copy(
                    loading = true,
                    message = null,
                    myUid = repo.currentUid(),
                    myEmail = repo.currentEmail()
                )

                val code = repo.ensureUserInviteCode()
                val couple = repo.getCurrentCouple()

                _ui.value = _ui.value.copy(
                    loading = false,
                    myCode = code,
                    linkedCouple = couple
                )

                startCoupleListener()
            } catch (e: Exception) {
                _ui.value = _ui.value.copy(
                    loading = false,
                    message = e.message ?: "Failed to load couple data"
                )
            }
        }
    }

    private fun startCoupleListener() {
        userCoupleListener?.remove()

        userCoupleListener = repo.observeCurrentUserCoupleId(
            onChanged = { coupleId ->
                viewModelScope.launch {
                    val couple = if (coupleId.isNullOrBlank()) null else repo.getCoupleById(coupleId)
                    _ui.value = _ui.value.copy(linkedCouple = couple)
                }
            },
            onError = { error ->
                _ui.value = _ui.value.copy(message = error.message ?: "Live sync failed")
            }
        )
    }

    fun onPartnerCodeChange(value: String) {
        val filtered = value.filter { it.isDigit() }.take(6)
        _ui.value = _ui.value.copy(
            enteredPartnerCode = filtered,
            message = null,
            showConfirmation = false,
            partnerPreviewUid = "",
            partnerPreviewEmail = ""
        )
    }

    fun refreshMyCode() {
        viewModelScope.launch {
            try {
                _ui.value = _ui.value.copy(loading = true, message = null)
                val code = repo.refreshMyCode()
                _ui.value = _ui.value.copy(
                    loading = false,
                    myCode = code,
                    message = "Code refreshed ✅"
                )
            } catch (e: Exception) {
                _ui.value = _ui.value.copy(
                    loading = false,
                    message = e.message ?: "Failed to refresh code"
                )
            }
        }
    }

    fun previewPartnerCode() {
        val code = _ui.value.enteredPartnerCode
        if (code.length != 6) {
            _ui.value = _ui.value.copy(message = "Enter a valid 6-digit code")
            return
        }

        viewModelScope.launch {
            try {
                _ui.value = _ui.value.copy(loading = true, message = null)
                val found = repo.findCodeOwner(code)

                if (found == null) {
                    _ui.value = _ui.value.copy(
                        loading = false,
                        partnerPreviewUid = "",
                        partnerPreviewEmail = "",
                        showConfirmation = false,
                        message = "Code not found"
                    )
                    return@launch
                }

                _ui.value = _ui.value.copy(
                    loading = false,
                    partnerPreviewUid = found.first,
                    partnerPreviewEmail = found.second,
                    showConfirmation = true
                )
            } catch (e: Exception) {
                _ui.value = _ui.value.copy(
                    loading = false,
                    message = e.message ?: "Failed to check code"
                )
            }
        }
    }

    fun confirmLink() {
        viewModelScope.launch {
            try {
                _ui.value = _ui.value.copy(loading = true, message = null)
                val couple = repo.linkWithPartnerCode(_ui.value.enteredPartnerCode)

                _ui.value = _ui.value.copy(
                    loading = false,
                    linkedCouple = couple,
                    showConfirmation = false,
                    enteredPartnerCode = "",
                    partnerPreviewUid = "",
                    partnerPreviewEmail = "",
                    message = "Partner linked successfully ✅"
                )
            } catch (e: Exception) {
                _ui.value = _ui.value.copy(
                    loading = false,
                    showConfirmation = false,
                    message = e.message ?: "Failed to link partner"
                )
            }
        }
    }

    fun unlinkCouple() {
        viewModelScope.launch {
            try {
                _ui.value = _ui.value.copy(loading = true, message = null)
                repo.unlinkCurrentCouple()
                _ui.value = _ui.value.copy(
                    loading = false,
                    linkedCouple = null,
                    enteredPartnerCode = "",
                    partnerPreviewUid = "",
                    partnerPreviewEmail = "",
                    showConfirmation = false,
                    message = "Couple unlinked ✅"
                )
            } catch (e: Exception) {
                _ui.value = _ui.value.copy(
                    loading = false,
                    message = e.message ?: "Failed to unlink"
                )
            }
        }
    }

    fun dismissConfirmation() {
        _ui.value = _ui.value.copy(showConfirmation = false)
    }

    override fun onCleared() {
        userCoupleListener?.remove()
        super.onCleared()
    }
}