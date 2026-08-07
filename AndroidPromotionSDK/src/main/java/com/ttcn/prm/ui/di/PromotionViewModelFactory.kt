package com.ttcn.prm.ui.di

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ttcn.prm.ui.feature.choosepromotion.ChoosePromotionViewModel
import com.ttcn.prm.ui.feature.mypromotion.MyPromotionViewModel
import com.ttcn.prm.ui.feature.promotiondetail.PromotionDetailViewModel
import com.ttcn.prm.ui.feature.searchmypromotion.SearchMyPromotionViewModel
import com.ttcn.promotionsdk.config.PromotionSDKConfig
import com.ttcn.promotionsdk.di.PromotionContainer
import com.ttcn.promotionsdk.domain.usecase.FindEligibleCampaignsUseCase
import com.ttcn.promotionsdk.domain.usecase.GetCustomerVoucherDetailUseCase
import com.ttcn.promotionsdk.domain.usecase.SearchCustomerVouchersUseCase

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
 * từng dòng, và cách wiring dependency (`PromotionContainer.requireConfig()`, `XxxUseCase()`) bị
 * chép lại ở từng file — sửa cách lấy config là phải nhớ sửa đủ bốn chỗ. Thêm màn mới = thêm **một**
 * dòng [initializer] ở đây.
 *
 * Use case khởi tạo **mỗi lần dựng VM** chứ không giữ lại: chúng là object không state, tự lấy
 * repository từ đồ thị DI đã init — giữ tham chiếu ở factory chỉ làm nó sống dai hơn cần thiết.
 */
internal fun promotionViewModelFactory(): ViewModelProvider.Factory = viewModelFactory {
    initializer {
        MyPromotionViewModel(
            searchCustomerVouchersUseCase = SearchCustomerVouchersUseCase(),
            config = requireConfig(),
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
            config = requireConfig(),
        )
    }
    initializer {
        SearchMyPromotionViewModel(
            searchCustomerVouchersUseCase = SearchCustomerVouchersUseCase(),
            config = requireConfig(),
        )
    }
}

/** Ném nếu chưa `PromotionSDK.initialize(...)` — VM không thể chạy mà thiếu cấu hình phiên. */
private fun requireConfig(): PromotionSDKConfig = PromotionContainer.requireConfig()
