// vds-promotion/src/main/java/com/ttcn/promotionsdk/ui/presentation/promotion/PromotionViewModel.kt
package com.ttcn.promotionsdk.ui.feature.promotion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ttcn.promotionsdk.core.config.PromotionRequestContextProvider
import com.ttcn.promotionsdk.core.data.dto.redemption.RedemptionSessionRequest
import com.ttcn.promotionsdk.core.data.dto.redemption.RedemptionSessionResponse
import com.ttcn.promotionsdk.core.data.dto.stackablediscount.StackableDiscountsRequest
import com.ttcn.promotionsdk.core.data.dto.stackablediscount.StackableDiscountsResponse
import com.ttcn.promotionsdk.core.domain.usecase.CreateRedemptionSessionUseCase
import com.ttcn.promotionsdk.core.domain.usecase.ValidateStackableDiscountsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RedemptionUiState(
    val isValidating: Boolean = false,
    val isCreatingSession: Boolean = false,
    val stackableDiscountsResult: StackableDiscountsResponse? = null,
    val redemptionSessionResult: RedemptionSessionResponse? = null,
    val error: String? = null,
)

class RedemptionViewModel(
    private val validateStackableDiscountsUseCase: ValidateStackableDiscountsUseCase,
    private val createRedemptionSessionUseCase: CreateRedemptionSessionUseCase,
    private val requestContextProvider: PromotionRequestContextProvider,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RedemptionUiState())
    val uiState: StateFlow<RedemptionUiState> = _uiState.asStateFlow()

    fun validateStackableDiscounts(request: StackableDiscountsRequest) {
        viewModelScope.launch {
            _uiState.update { it.copy(isValidating = true, error = null) }
            runCatching {
                validateStackableDiscountsUseCase(request)
            }.onSuccess { response ->
                _uiState.update {
                    it.copy(
                        isValidating = false,
                        stackableDiscountsResult = response,
                    )
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isValidating = false,
                        error = throwable.message ?: "Failed to validate stackable discounts",
                    )
                }
            }
        }
    }

    fun createRedemptionSession(request: RedemptionSessionRequest) {
        viewModelScope.launch {
            _uiState.update { it.copy(isCreatingSession = true, error = null) }
            runCatching {
                createRedemptionSessionUseCase(request)
            }.onSuccess { response ->
                _uiState.update {
                    it.copy(
                        isCreatingSession = false,
                        redemptionSessionResult = response,
                    )
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isCreatingSession = false,
                        error = throwable.message ?: "Failed to create redemption session",
                    )
                }
            }
        }
    }

    /**
     * Validate discounts trước, nếu hợp lệ thì tự động tạo redemption session.
     */
    fun validateThenCreateSession(
        stackableRequest: StackableDiscountsRequest,
        sessionRequest: RedemptionSessionRequest,
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isValidating = true, error = null) }
            runCatching {
                validateStackableDiscountsUseCase(stackableRequest)
            }.onSuccess { discountsResponse ->
                _uiState.update {
                    it.copy(
                        isValidating = false,
                        stackableDiscountsResult = discountsResponse,
                        isCreatingSession = true,
                    )
                }
                runCatching {
                    createRedemptionSessionUseCase(sessionRequest)
                }.onSuccess { sessionResponse ->
                    _uiState.update {
                        it.copy(
                            isCreatingSession = false,
                            redemptionSessionResult = sessionResponse,
                        )
                    }
                }.onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isCreatingSession = false,
                            error = throwable.message ?: "Failed to create redemption session",
                        )
                    }
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isValidating = false,
                        error = throwable.message ?: "Failed to validate stackable discounts",
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}