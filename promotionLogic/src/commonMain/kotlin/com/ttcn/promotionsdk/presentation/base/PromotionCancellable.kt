package com.ttcn.promotionsdk.presentation.base

/**
 * Handle huỷ một subscription callback (vd [com.ttcn.promotionsdk.presentation.mypromotion.MyPromotionStore.watchState]).
 * Trả về cho iOS/Swift để dừng quan sát khi rời màn. Thuần Kotlin — bridge ObjC, không thư viện.
 */
public class PromotionCancellable(private val onCancel: () -> Unit) {
    public fun cancel() {
        onCancel()
    }
}
