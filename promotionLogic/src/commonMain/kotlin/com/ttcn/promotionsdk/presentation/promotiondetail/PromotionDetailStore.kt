package com.ttcn.promotionsdk.presentation.promotiondetail

import com.ttcn.promotionsdk.core.di.PromotionContainer
import com.ttcn.promotionsdk.core.domain.exception.toErrorCode
import com.ttcn.promotionsdk.core.domain.model.voucher.VoucherDetail
import com.ttcn.promotionsdk.core.domain.model.voucher.VoucherStatus
import com.ttcn.promotionsdk.core.domain.model.voucher.displayState
import com.ttcn.promotionsdk.core.domain.usecase.GetCustomerVoucherDetailUseCase
import com.ttcn.promotionsdk.presentation.PromotionCancellable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * **Tầng UI-logic dùng chung** cho màn "Chi tiết ưu đãi" — chạy trên cả Android & iOS.
 *
 * Gom: fetch chi tiết theo voucherId (service tự đọc từ [PromotionContainer.requestContextProvider]),
 * quyết định nút "Dùng ngay" (visible/enabled/label) từ trạng thái, xử lý lỗi. Cùng khuôn với
 * `MyPromotionStore`: `state` / `dispatch` / `watchState` / `currentState` / `clear`.
 */
class PromotionDetailStore(
    private val getCustomerVoucherDetailUseCase: GetCustomerVoucherDetailUseCase,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {
    /** iOS/Swift: khởi tạo không cần truyền scope (xem `MyPromotionStore`). */
    constructor(getCustomerVoucherDetailUseCase: GetCustomerVoucherDetailUseCase) :
        this(getCustomerVoucherDetailUseCase, CoroutineScope(SupervisorJob() + Dispatchers.Default))

    private val _state = MutableStateFlow(PromotionDetailState())
    val state: StateFlow<PromotionDetailState> = _state.asStateFlow()

    fun currentState(): PromotionDetailState = _state.value

    fun watchState(onEach: (PromotionDetailState) -> Unit): PromotionCancellable {
        val job = scope.launch { state.collect { onEach(it) } }
        return PromotionCancellable { job.cancel() }
    }

    fun clear() {
        scope.cancel()
    }

    fun dispatch(intent: PromotionDetailIntent) {
        when (intent) {
            is PromotionDetailIntent.Seed -> seed(intent.status)
            is PromotionDetailIntent.LoadDetail -> loadDetail(intent.voucherId)
            PromotionDetailIntent.ConsumeError -> _state.update { it.copy(errorCode = null) }
        }
    }

    /**
     * Seed quyết định nút "Dùng ngay" từ trạng thái cơ bản (từ card/promotion) **trước khi** fetch
     * detail — để hai nền tảng hiện nút ngay, không chờ mạng. Cùng rule với [loadDetail]
     * (`displayState().isUsable`); KHÔNG đè khi đã có detail thật.
     */
    private fun seed(status: String) {
        val s = VoucherStatus.from(status)
        val usable = s.displayState().isUsable
        _state.update {
            if (it.detail != null) it else it.copy(
                status = s,
                actionVisible = usable,
                actionEnabled = usable,
                actionLabel = "",
            )
        }
    }

    private fun loadDetail(voucherId: String) {
        scope.launch {
            _state.update { it.copy(isLoading = true) }
            runCatching {
                getCustomerVoucherDetailUseCase(
                    voucherId = voucherId,
                    service = PromotionContainer.requestContextProvider.getService(),
                )
            }.onSuccess { detail ->
                val status = VoucherStatus.from(detail?.status)
                // Nút "Dùng ngay" hiện khi còn dùng được; quy tắc trạng thái ở domain (dùng chung iOS).
                val usable = status.displayState().isUsable
                _state.update {
                    it.copy(
                        isLoading = false,
                        detail = detail,
                        status = status,
                        actionVisible = usable,
                        actionEnabled = usable,
                        actionLabel = if (usable) "" else detail?.displayStatusLabel.orEmpty(),
                        errorCode = if (detail == null) "error_detail_unavailable" else it.errorCode,
                    )
                }
            }.onFailure { throwable ->
                _state.update { it.copy(isLoading = false, errorCode = throwable.toErrorCode()) }
            }
        }
    }
}

// ─── State / Intent (cấu trúc; label là dữ liệu server, không phải chuỗi ta dựng) ──

data class PromotionDetailState(
    val isLoading: Boolean = false,
    val detail: VoucherDetail? = null,
    val status: VoucherStatus = VoucherStatus.UNKNOWN,
    val actionVisible: Boolean = true,
    val actionEnabled: Boolean = false,
    val actionLabel: String = "",
    val errorCode: String? = null,
)

sealed interface PromotionDetailIntent {
    /** Seed nút "Dùng ngay" từ trạng thái cơ bản (card/promotion) trước khi fetch detail. */
    data class Seed(val status: String) : PromotionDetailIntent
    data class LoadDetail(val voucherId: String) : PromotionDetailIntent
    data object ConsumeError : PromotionDetailIntent
}
