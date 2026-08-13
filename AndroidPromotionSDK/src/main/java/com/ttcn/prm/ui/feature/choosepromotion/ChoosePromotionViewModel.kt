package com.ttcn.prm.ui.feature.choosepromotion

import com.ttcn.prm.ui.base.PRMStoreViewModel
import com.ttcn.promotionsdk.domain.model.eligible.EligibleOffer
import com.ttcn.promotionsdk.domain.usecase.FindEligibleCampaignsUseCase
import com.ttcn.promotionsdk.presentation.choosepromotion.ChoosePromotionIntent
import com.ttcn.promotionsdk.presentation.choosepromotion.ChoosePromotionState
import com.ttcn.promotionsdk.presentation.choosepromotion.ChoosePromotionStore
import com.ttcn.promotionsdk.presentation.choosepromotion.selectedOffers

/**
 * Màn "Chọn ưu đãi". Nghiệp vụ nằm trọn ở [ChoosePromotionStore] (dùng chung với iOS); lớp này chỉ
 * giữ store sống qua xoay màn — xem [PRMStoreViewModel].
 *
 * Fragment đọc thẳng [ChoosePromotionState] và phát thẳng [ChoosePromotionIntent].
 */
internal class ChoosePromotionViewModel(
    findEligibleCampaignsUseCase: FindEligibleCampaignsUseCase,
) : PRMStoreViewModel<ChoosePromotionState, ChoosePromotionIntent>(
    { scope -> ChoosePromotionStore(findEligibleCampaignsUseCase, scope) },
) {

    /**
     * Ưu đãi user đang chọn, để bấm "Áp dụng" trả về widget — validate & áp do `EndowStore` lo
     * (dùng chung iOS), màn này không tự validate.
     *
     * Là **hàm gọi lúc bấm** chứ không phải effect: nó chỉ đọc selection hiện tại của store, không
     * có gì bất đồng bộ để chờ. Trước kia đi vòng qua `Effect.ApplySelectedOffers` rồi Fragment bắt
     * lại — thêm một chặng để quay về đúng chỗ vừa bấm.
     *
     * Luật lọc nằm ở store ([selectedOffers]) — iOS gọi đúng hàm này.
     */
    fun selectedOffers(): List<EligibleOffer> = state.value.selectedOffers()
}
