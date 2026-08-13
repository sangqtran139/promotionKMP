package com.ttcn.prm.ui.feature.endowview

import com.ttcn.prm.ui.base.PRMStoreViewModel
import com.ttcn.promotionsdk.domain.model.eligible.EligibleOffer
import com.ttcn.promotionsdk.domain.usecase.CreateRedemptionSessionUseCase
import com.ttcn.promotionsdk.domain.usecase.FindEligibleCampaignsUseCase
import com.ttcn.promotionsdk.domain.usecase.ValidateStackableDiscountsUseCase
import com.ttcn.promotionsdk.presentation.endow.EndowConfirmResult
import com.ttcn.promotionsdk.presentation.endow.EndowIntent
import com.ttcn.promotionsdk.presentation.endow.EndowState
import com.ttcn.promotionsdk.presentation.endow.EndowStore

/**
 * ViewModel của widget [PRMEndowView] — cùng khuôn với mọi màn khác: [PRMStoreViewModel] bọc store
 * dùng chung, không thêm tầng kiến trúc nào.
 *
 * **Đổi so với bản trước, ba điểm:**
 *
 * 1. **Là `ViewModel` thật** (qua [PRMStoreViewModel]), không còn là object thường tạo bằng
 *    `create(scope)` với scope gắn vào View. Trước đây store chết theo `onDetachedFromWindow`, nên
 *    host mở màn "Chọn ưu đãi" bằng `replace()` là widget detach → `viewModel = null` → bấm "Áp
 *    dụng"/"Thanh toán" trả `PRM_ERROR_GENERAL` mà **không hề gọi mạng**. iOS không dính vì
 *    `endowVM` là property của `PromotionSDKImpl`. Nay store sống theo `ViewModelStore` của host.
 *
 * 2. **Bỏ `create()`**, lấy store qua DI nội bộ như các màn khác (`promotionViewModelFactory()`).
 *    Use case tự resolve repository từ đồ thị đã init, không nơi nào phải khai tay.
 *
 * 3. **Bỏ máy trạng thái `settleCompletion`/`sawValidating`/`handleSettle`/`consumeErrorUnlessSettling`**.
 *    Bốn thứ đó tồn tại chỉ vì `EndowStore.validateAndApply` là fire-and-forget: muốn biết kết quả
 *    phải rình `isValidating` true→false trên dòng state chung — mà `StateFlow` là **conflated** nên
 *    còn phải chặn widget xoá lỗi trước khi nơi gọi đọc kịp. Nay store `suspend` và **trả thẳng
 *    state cuối**, nên cả bốn biến mất. iOS bỏ y hệt.
 *
 * Bề mặt state là **chính [EndowState] của store**, không bọc lại — xem [PRMStoreViewModel].
 */
internal class EndowViewModel(
    findEligibleCampaignsUseCase: FindEligibleCampaignsUseCase,
    validateStackableDiscountsUseCase: ValidateStackableDiscountsUseCase,
    createRedemptionSessionUseCase: CreateRedemptionSessionUseCase,
) : PRMStoreViewModel<EndowState, EndowIntent>(
    { scope ->
        EndowStore(
            findEligibleCampaignsUseCase = findEligibleCampaignsUseCase,
            validateStackableDiscountsUseCase = validateStackableDiscountsUseCase,
            createRedemptionSessionUseCase = createRedemptionSessionUseCase,
            scope = scope,
        )
    },
) {

    /** [PRMStoreViewModel.store] khai kiểu `PRMStore`; hai hàm `suspend` dưới đây là của riêng store này. */
    private val endowStore: EndowStore get() = store as EndowStore

    fun loadInitial() = dispatch(EndowIntent.LoadInitial)

    /** Cờ hiển thị widget, đọc cache (fail-open). Rule + tên cờ nằm ở store, dùng chung iOS. */
    fun availabilityFromCache(): Boolean = endowStore.availabilityFromCache()

    /** Làm mới cờ từ server rồi trả giá trị thật. */
    suspend fun refreshAvailability(): Boolean = endowStore.refreshAvailability()

    /**
     * Validate + áp ưu đãi user chọn ở màn "Chọn ưu đãi"; **trả state cuối của đúng lượt này**.
     * Đối ứng `EndowViewModel.validateAndApply(_:)` bên iOS.
     */
    suspend fun validateAndApply(offers: List<EligibleOffer>): EndowState =
        endowStore.validateAndApply(offers)

    /** Host tự validate rồi đưa kết quả vào (giữ public API `PRMEndowView.setDiscountDetails`). */
    fun setApplied(details: List<AppliedDiscount>, unavailable: Boolean = false) =
        dispatch(EndowIntent.SetApplied(details, unavailable))

    fun markUnavailable() = dispatch(EndowIntent.MarkUnavailable)

    fun clearApplied() = dispatch(EndowIntent.ClearApplied)

    fun consumeError() = dispatch(EndowIntent.ConsumeError)

    /** Bấm "Thanh toán" — nghiệp vụ nằm trọn ở [EndowStore.confirmRedemption] (dùng chung với iOS). */
    suspend fun confirmRedemption(): EndowConfirmResult = endowStore.confirmRedemption()
}
