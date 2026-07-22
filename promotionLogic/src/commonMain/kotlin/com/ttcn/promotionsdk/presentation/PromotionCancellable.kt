package com.ttcn.promotionsdk.presentation

/**
 * Handle huỷ một subscription callback (vd [com.ttcn.promotionsdk.presentation.mypromotion.MyPromotionStore.watchState]).
 * Trả về cho iOS/Swift để dừng quan sát khi rời màn. Thuần Kotlin — bridge ObjC, không thư viện.
 */
class PromotionCancellable(private val onCancel: () -> Unit) {
    fun cancel() {
        onCancel()
    }
}
