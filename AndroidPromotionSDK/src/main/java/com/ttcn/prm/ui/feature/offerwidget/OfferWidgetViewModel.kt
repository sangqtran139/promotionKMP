package com.ttcn.prm.ui.feature.offerwidget

import com.ttcn.prm.ui.base.PRMStoreViewModel
import com.ttcn.promotionsdk.domain.model.eligible.EligibleOffer
import com.ttcn.promotionsdk.domain.usecase.CreateRedemptionSessionUseCase
import com.ttcn.promotionsdk.domain.usecase.FindEligibleCampaignsUseCase
import com.ttcn.promotionsdk.domain.usecase.ValidateStackableDiscountsUseCase
import com.ttcn.promotionsdk.presentation.offerwidget.OfferWidgetApplyOutcome
import com.ttcn.promotionsdk.presentation.offerwidget.OfferWidgetConfirmResult
import com.ttcn.promotionsdk.presentation.offerwidget.OfferWidgetIntent
import com.ttcn.promotionsdk.presentation.offerwidget.OfferWidgetState
import com.ttcn.promotionsdk.presentation.offerwidget.OfferWidgetStore

/**
 * ViewModel của widget [PRMOfferWidget] — cùng khuôn với mọi màn khác: [PRMStoreViewModel] bọc store
 * dùng chung, không thêm tầng kiến trúc nào.
 *
 * **Đổi so với bản trước, ba điểm:**
 *
 * 1. **Là `ViewModel` thật** (qua [PRMStoreViewModel]), không còn là object thường tạo bằng
 *    `create(scope)` với scope gắn vào View. Trước đây store chết theo `onDetachedFromWindow`, nên
 *    host mở màn "Chọn ưu đãi" bằng `replace()` là widget detach → `viewModel = null` → bấm "Áp
 *    dụng"/"Thanh toán" trả `PRM_ERROR_GENERAL` mà **không hề gọi mạng**. iOS không dính vì
 *    `offerWidgetVM` là property của `PromotionSDKImpl`. Nay store sống theo `ViewModelStore` của host.
 *
 * 2. **Bỏ `create()`**, lấy store qua DI nội bộ như các màn khác (`promotionViewModelFactory()`).
 *    Use case tự resolve repository từ đồ thị đã init, không nơi nào phải khai tay.
 *
 * 3. **Bỏ máy trạng thái `settleCompletion`/`sawValidating`/`handleSettle`/`consumeErrorUnlessSettling`**.
 *    Bốn thứ đó tồn tại chỉ vì `OfferWidgetStore.validateAndApply` là fire-and-forget: muốn biết kết quả
 *    phải rình `isValidating` true→false trên dòng state chung — mà `StateFlow` là **conflated** nên
 *    còn phải chặn widget xoá lỗi trước khi nơi gọi đọc kịp. Nay store `suspend` và **trả thẳng kết
 *    cục** ([OfferWidgetApplyOutcome]) của lượt gọi, nên cả bốn biến mất. iOS bỏ y hệt.
 *
 * Bề mặt state là **chính [OfferWidgetState] của store**, không bọc lại — xem [PRMStoreViewModel].
 */
internal class OfferWidgetViewModel(
    findEligibleCampaignsUseCase: FindEligibleCampaignsUseCase,
    validateStackableDiscountsUseCase: ValidateStackableDiscountsUseCase,
    createRedemptionSessionUseCase: CreateRedemptionSessionUseCase,
) : PRMStoreViewModel<OfferWidgetState, OfferWidgetIntent>(
    { scope ->
        OfferWidgetStore(
            findEligibleCampaignsUseCase = findEligibleCampaignsUseCase,
            validateStackableDiscountsUseCase = validateStackableDiscountsUseCase,
            createRedemptionSessionUseCase = createRedemptionSessionUseCase,
            scope = scope,
        )
    },
) {

    /** [PRMStoreViewModel.store] khai kiểu `PRMStore`; hai hàm `suspend` dưới đây là của riêng store này. */
    private val offerWidgetStore: OfferWidgetStore get() = store as OfferWidgetStore

    fun loadInitial() = dispatch(OfferWidgetIntent.LoadInitial)

    /** Cờ hiển thị widget, đọc cache (fail-open). Rule + tên cờ nằm ở store, dùng chung iOS. */
    fun availabilityFromCache(): Boolean = offerWidgetStore.availabilityFromCache()

    /** Làm mới cờ từ server rồi trả giá trị thật. */
    suspend fun refreshAvailability(): Boolean = offerWidgetStore.refreshAvailability()

    /**
     * Validate + áp ưu đãi user chọn ở màn "Chọn ưu đãi"; **trả kết cục của đúng lượt này**
     * ([OfferWidgetApplyOutcome]). Đối ứng `OfferWidgetViewModel.validateAndApply(_:completion:)` bên iOS.
     */
    suspend fun validateAndApply(offers: List<EligibleOffer>): OfferWidgetApplyOutcome =
        offerWidgetStore.validateAndApply(offers)

    /** Host tự validate rồi đưa kết quả vào (giữ public API `PRMOfferWidget.setDiscountDetails`). */
    fun setApplied(details: List<AppliedDiscount>, unavailable: Boolean = false) =
        dispatch(OfferWidgetIntent.SetApplied(details, unavailable))

    fun markUnavailable() = dispatch(OfferWidgetIntent.MarkUnavailable)

    fun clearApplied() = dispatch(OfferWidgetIntent.ClearApplied)

    fun consumeError() = dispatch(OfferWidgetIntent.ConsumeError)

    /** Bấm "Thanh toán" — nghiệp vụ nằm trọn ở [OfferWidgetStore.confirmRedemption] (dùng chung với iOS). */
    suspend fun confirmRedemption(): OfferWidgetConfirmResult = offerWidgetStore.confirmRedemption()
}
