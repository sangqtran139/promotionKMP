package com.ttcn.prm.ui.di

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ttcn.prm.ui.feature.choosepromotion.ChoosePromotionViewModel
import com.ttcn.prm.ui.feature.mypromotion.MyPromotionViewModel
import com.ttcn.prm.ui.feature.promotiondetail.PromotionDetailViewModel
import com.ttcn.prm.ui.feature.searchmypromotion.SearchMyPromotionViewModel
import com.ttcn.promotionsdk.domain.usecase.FindEligibleCampaignsUseCase
import com.ttcn.promotionsdk.domain.usecase.GetCustomerVoucherDetailUseCase
import com.ttcn.promotionsdk.domain.usecase.SearchCustomerVouchersUseCase
import com.ttcn.promotionsdk.domain.usecase.ValidateStackableDiscountsUseCase
import com.ttcn.promotionsdk.domain.usecase.CreateRedemptionSessionUseCase
import com.ttcn.prm.ui.feature.endowview.EndowViewModel

/**
 * **Một** factory cho toàn bộ ViewModel của SDK — mọi Fragment dùng chung:
 *
 * ```kotlin
 * private val viewModel: MyPromotionViewModel by viewModels { promotionViewModelFactory() }
 * ```
 *
 * `viewModelFactory { }` đánh khoá initializer theo **kiểu reified**, nên gọi ở Fragment nào thì nó
 * tự dựng đúng VM của Fragment đó — không cần `when (modelClass)`, không `UNCHECKED_CAST`, không
 * nhánh `else throw` như factory viết tay.
 *
 * Gom về đây thay vì mỗi VM một `companion object { fun factory() }`: bốn bản đó giống hệt nhau tới
 * từng dòng, và cách wiring dependency (`XxxUseCase()`) bị chép lại ở từng file. Thêm màn mới =
 * thêm **một** dòng [initializer] ở đây.
 *
 * Use case khởi tạo **mỗi lần dựng VM** chứ không giữ lại: chúng là object không state, tự lấy
 * repository từ đồ thị DI đã init — giữ tham chiếu ở factory chỉ làm nó sống dai hơn cần thiết.
 */
internal fun promotionViewModelFactory(): ViewModelProvider.Factory = viewModelFactory {
    initializer {
        MyPromotionViewModel(
            searchCustomerVouchersUseCase = SearchCustomerVouchersUseCase(),
        )
    }
    initializer {
        ChoosePromotionViewModel(
            findEligibleCampaignsUseCase = FindEligibleCampaignsUseCase(),
        )
    }
    initializer {
        PromotionDetailViewModel(
            getCustomerVoucherDetailUseCase = GetCustomerVoucherDetailUseCase(),
        )
    }
    initializer {
        SearchMyPromotionViewModel(
            searchCustomerVouchersUseCase = SearchCustomerVouchersUseCase(),
        )
    }
    // Widget `PRMEndowView` — VM duy nhất không thuộc về một Fragment. Nó lấy `ViewModelStoreOwner`
    // từ cây view (`findViewTreeViewModelStoreOwner`), nên store sống theo màn của host thay vì chết
    // theo `onDetachedFromWindow` như bản `create(scope)` cũ.
    initializer {
        EndowViewModel(
            findEligibleCampaignsUseCase = FindEligibleCampaignsUseCase(),
            validateStackableDiscountsUseCase = ValidateStackableDiscountsUseCase(),
            createRedemptionSessionUseCase = CreateRedemptionSessionUseCase(),
        )
    }
}
