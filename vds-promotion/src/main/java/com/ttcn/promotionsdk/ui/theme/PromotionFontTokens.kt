package com.ttcn.promotionsdk.ui.theme

import android.graphics.Typeface

data class PromotionFontTokens(
    // "Ưu đãi của tôi" — fontSfDisplayBold22
    val fontTitle: Typeface? = null,

    // tvContent, empty state title — fontSfDisplayBold14 / Bold18
    val fontBodyBold: Typeface? = null,

    // tvStore, tvExpired, empty state desc — fontDisPlayRegular14 / Regular16
    val fontBodyRegular: Typeface? = null,

    // Tab item label — TabLayOutNew
    val fontTab: Typeface? = null,
)