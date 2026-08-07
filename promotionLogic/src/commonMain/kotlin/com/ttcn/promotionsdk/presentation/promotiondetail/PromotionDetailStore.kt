package com.ttcn.promotionsdk.presentation.promotiondetail

import com.ttcn.promotionsdk.di.PromotionContainer
import com.ttcn.promotionsdk.domain.exception.toErrorCode
import com.ttcn.promotionsdk.domain.model.voucher.VoucherDetail
import com.ttcn.promotionsdk.domain.model.voucher.VoucherStatus
import com.ttcn.promotionsdk.domain.model.voucher.displayState
import com.ttcn.promotionsdk.domain.usecase.GetCustomerVoucherDetailUseCase
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
            is PromotionDetailIntent.LoadDetail -> loadDetail(intent.voucherId)
            PromotionDetailIntent.ConsumeError -> _state.update { it.copy(errorCode = null) }
        }
    }

    private fun loadDetail(voucherId: String) {
        scope.launch {
            _state.update { it.copy(isLoading = true) }
            runCatching {
                getCustomerVoucherDetailUseCase(
                    voucherId = voucherId,
                    service = null,
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
                        // Nhãn nút LUÔN lấy từ server (`displayStatusLabel`), kể cả khi usable —
                        // không tự quyết định chuỗi ở đây. Rỗng thì native mới dùng nhãn mặc định.
                        actionLabel = detail?.displayStatusLabel.orEmpty(),
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
    /**
     * Nhãn nút do server trả (`VoucherDetail.displayStatusLabel`).
     *
     * ⚠️ **Hai màn chi tiết hiện KHÔNG đọc field này** — chốt dùng chuỗi cứng "Sử dụng ngay"
     * (`prm_use_now` / `PromotionUIStrings.useNow`) vì BE trả "Sử dụng" cho mọi voucher. Giữ lại để
     * bật lại nhãn server không phải sửa store; card ở màn danh sách thì vẫn theo nhãn server.
     */
    val actionLabel: String = "",
    val errorCode: String? = null,
)

/**
 * **Không có intent seed từ ngoài**: màn chi tiết chỉ hiển thị khi `getCustomerVoucherDetail` trả về —
 * dữ liệu từ màn danh sách không được dùng để dựng card/nút (tránh hai nguồn sự thật lệch nhau).
 */
sealed interface PromotionDetailIntent {
    data class LoadDetail(val voucherId: String) : PromotionDetailIntent
    data object ConsumeError : PromotionDetailIntent
}
