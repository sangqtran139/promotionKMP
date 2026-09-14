package com.ttcn.promotionsdk.host

import com.ttcn.promotionsdk.config.PromotionSDKConfig
import com.ttcn.promotionsdk.di.internal.get
import com.ttcn.promotionsdk.di.internal.module
import com.ttcn.promotionsdk.di.internal.single

/**
 * Khai báo DI cho **gói năng lực do host cấp** ([PromotionHostServices]).
 *
 * Nạp **trước** mọi module khác trong `PromotionContainer.initialize` để tầng nào cũng resolve được.
 * Cùng quy ước "module nằm cùng package với lớp nó dựng" (`docs/common/DependencyInjection.md` §5).
 *
 * Module này chỉ dựng **cổng**, không dựng thứ dùng cổng: `PromotionPreferences` vẫn do `LocalModule`
 * bind — nó chỉ hỏi ở đây xem host có cấp kho thay thế không.
 *
 * `PromotionRequestContextProvider` **vẫn ở `NetworkModule`** — nó thuộc thông tin phiên
 * (`PromotionSessionConfig`), không thuộc gói năng lực; xem KDoc của [PromotionHostServices].
 */
internal object HostModule {
    val module = module {
        single { get<PromotionSDKConfig>().hostServices }

        // Host không cấp → no-op. SDK chạy y nguyên, không nhánh `if (tracker != null)` nào ở
        // call-site. Đối ứng `EmptyPromotionRequestContextProvider` của NetworkModule.
        single<PromotionTracker> {
            get<PromotionHostServices>().tracker ?: NoOpPromotionTracker()
        }
    }
}
