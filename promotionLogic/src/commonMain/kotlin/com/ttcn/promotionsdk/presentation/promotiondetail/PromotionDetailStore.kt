package com.ttcn.promotionsdk.presentation.promotiondetail

import com.ttcn.promotionsdk.di.PromotionContainer
import com.ttcn.promotionsdk.domain.exception.toErrorCode
import com.ttcn.promotionsdk.domain.model.voucher.VoucherStatus
import com.ttcn.promotionsdk.domain.usecase.GetCustomerVoucherDetailUseCase
import com.ttcn.promotionsdk.presentation.base.PRMStore
import com.ttcn.promotionsdk.presentation.base.PromotionCancellable
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
public class PromotionDetailStore(
    private val getCustomerVoucherDetailUseCase: GetCustomerVoucherDetailUseCase,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) : PRMStore<PromotionDetailState, PromotionDetailIntent> {
    /** iOS/Swift: khởi tạo không cần truyền scope (xem `MyPromotionStore`). */
    public constructor(getCustomerVoucherDetailUseCase: GetCustomerVoucherDetailUseCase) :
        this(getCustomerVoucherDetailUseCase, CoroutineScope(SupervisorJob() + Dispatchers.Default))

    private val _state = MutableStateFlow(PromotionDetailState())
    override val state: StateFlow<PromotionDetailState> = _state.asStateFlow()

    public fun currentState(): PromotionDetailState = _state.value

    public fun watchState(onEach: (PromotionDetailState) -> Unit): PromotionCancellable {
        val job = scope.launch { state.collect { onEach(it) } }
        return PromotionCancellable { job.cancel() }
    }

    public fun clear() {
        scope.cancel()
    }

    override fun errorOf(state: PromotionDetailState): String? = state.errorCode

    override val consumeErrorIntent: PromotionDetailIntent = PromotionDetailIntent.ConsumeError

    override fun dispatch(intent: PromotionDetailIntent) {
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
