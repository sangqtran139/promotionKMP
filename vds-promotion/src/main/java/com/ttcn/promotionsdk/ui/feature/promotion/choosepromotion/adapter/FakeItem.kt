package com.ttcn.promotionsdk.ui.feature.promotion.choosepromotion.adapter

import com.ttcn.promotionsdk.core.data.dto.voucher.VoucherStatus
import com.ttcn.promotionsdk.ui.feature.promotion.mypromotion.MyVoucherListItem

val allAvailableVouchers = listOf(

    PromotionItem(
        id = "1",
        name = "Highlands Coffee",
        discount = "50000",
        isApplied = false,
        isExpired = true
    ),

    PromotionItem(
        id = "2",
        name = "Shopee",
        discount = "100000",
        isApplied = false,
        isNotEnoughApplied = true
    ),

    PromotionItem(
        id = "3",
        name = "Tiki",
        discount = "30000",
        isApplied = false
    ),

    PromotionItem(
        id = "4",
        name = "Lazada",
        discount = "200000",
        isApplied = false
    ),

    PromotionItem(
        id = "5",
        name = "Grab",
        discount = "150000",
        isApplied = false
    ),

    PromotionItem(
        id = "6",
        name = "CGV",
        discount = "300000",
        isApplied = false,
        isExpired = true
    ),

    PromotionItem(
        id = "7",
        name = "Circle K",
        discount = "80000",
        isApplied = false
    ),

    PromotionItem(
        id = "8",
        name = "Phúc Long",
        discount = "150000",
        isApplied = false
    ),

    PromotionItem(
        id = "9",
        name = "KFC",
        discount = "500000",
        isApplied = false
    ),

    PromotionItem(
        id = "10",
        name = "The Coffee House",
        discount = "70000",
        isApplied = false,
        isNotEnoughApplied = true
    ),

    PromotionItem(
        id = "11",
        name = "Pizza Hut",
        discount = "120000",
        isApplied = false
    ),

    PromotionItem(
        id = "12",
        name = "Samsung",
        discount = "1000000",
        isApplied = false
    ),

    PromotionItem(
        id = "13",
        name = "WinMart",
        discount = "90000",
        isApplied = false
    ),

    PromotionItem(
        id = "14",
        name = "GO!",
        discount = "250000",
        isApplied = false,
        isExpired = true
    ),

    PromotionItem(
        id = "15",
        name = "Starbucks",
        discount = "25000",
        isApplied = false
    ),

    PromotionItem(
        id = "16",
        name = "AEON Mall",
        discount = "200000",
        isApplied = false,
        isNotEnoughApplied = true
    ),

    PromotionItem(
        id = "17",
        name = "Mixue",
        discount = "350000",
        isApplied = false
    ),

    PromotionItem(
        id = "18",
        name = "Lotteria",
        discount = "450000",
        isApplied = false
    ),

    PromotionItem(
        id = "19",
        name = "BHD Star",
        discount = "550000",
        isApplied = false,
        isExpired = true
    ),

    PromotionItem(
        id = "20",
        name = "VinFast",
        discount = "80000",
        isApplied = false
    ),
).map {
    it.copy(
        urlLogo = "https://picsum.photos/seed/prm-${it.id}/200/200",
        urlBanner = "https://picsum.photos/seed/prm-banner-${it.id}/1200/600",
    )
}

object FakeVoucherData {

    private const val TOTAL_MY_VOUCHERS = 53
    private const val TOTAL_OTHER_VOUCHERS = 27
    private const val LAST_PAGE = 5

    fun getMyVouchers(page: Int, size: Int = 10): List<MyVoucherListItem> {
        val start = page * size + 1
        val end = minOf(start + size - 1, TOTAL_MY_VOUCHERS)
        if (start > TOTAL_MY_VOUCHERS) return emptyList()
        return (start..end).map { i ->
            MyVoucherListItem(
                voucherId = "my_voucher_$i",
                merchantName = "Merchant $i",
                title = "Giảm ${i * 10}% cho đơn hàng từ ${i * 100}k",
                description = "Mô tả voucher số $i",
                logo = "",
                expirationDate = "31/12/2025",
                displayStatusLabel = if (i % 7 == 0) "Hết hạn" else "Còn hạn",
                status = if (i % 7 == 0) VoucherStatus.EXPIRED else VoucherStatus.ACTIVE,
            )
        }
    }

    fun getOtherVouchers(page: Int, size: Int = 10): List<MyVoucherListItem> {
        val start = page * size + 1
        val end = minOf(start + size - 1, TOTAL_OTHER_VOUCHERS)
        if (start > TOTAL_OTHER_VOUCHERS) return emptyList()
        return (start..end).map { i ->
            MyVoucherListItem(
                voucherId = "other_voucher_$i",
                merchantName = "Brand $i",
                title = "Tặng quà trị giá ${i * 50}k",
                description = "Mô tả ưu đãi khác số $i",
                logo = "",
                expirationDate = "28/02/2025",
                displayStatusLabel = if (i % 5 == 0) "Hết hạn" else "Còn hạn",
                status = if (i % 5 == 0) VoucherStatus.EXPIRED else VoucherStatus.ACTIVE,
            )
        }
    }

    fun getTotalMyVoucherCount(): Int = TOTAL_MY_VOUCHERS

    fun getTotalOtherVoucherCount(): Int = TOTAL_OTHER_VOUCHERS

    fun isLastPage(page: Int): Boolean = page >= LAST_PAGE
}