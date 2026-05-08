// vds-promotion/src/main/java/com/ttcn/promotionsdk/ui/theme/PromotionIconToken.kt
package com.ttcn.promotionsdk.ui.theme

import androidx.annotation.DrawableRes

data class PromotionIconToken(

    // Nút back màn list (ic_back_in_onboarding)
    @DrawableRes val iconBack: Int? = null,

    // Nút back màn detail (ic_back_with_bg — có nền)
    @DrawableRes val iconBackDetail: Int? = null,

    // Icon tìm kiếm (ic_search_endow)
    @DrawableRes val iconSearch: Int? = null,

    // Icon trang trí cạnh tiêu đề (ic_my_endow_new)
    @DrawableRes val iconTitleDecor: Int? = null,

    // Ảnh empty state (il_chua_co_uu_dai_new_vers)
    @DrawableRes val iconEmptyState: Int? = null,

    // FAB scroll to top (ic_arrow_up_vector)
    @DrawableRes val iconScrollToTop: Int? = null,
)
