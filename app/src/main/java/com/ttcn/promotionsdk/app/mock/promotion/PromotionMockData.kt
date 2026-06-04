package com.ttcn.promotionsdk.app.mock.promotion

internal data class MockVoucher(
    val voucherId: String,
    val merchantName: String,
    val title: String,
    val description: String,
    val logo: String,
    val banner: String,
    val guideline: String,
    val startDate: String,
    val expirationDate: String,
    val timeSlot: String,
    val status: String,
    val displayStatusLabel: String,
    val campaignId: String,
    val campaignType: String,
    val discountMethod: String,
    val discountValue: String,
    val discountPercentage: String,
    val includedProducts: List<String>,
    val excludedProducts: List<String>,
)

internal object PromotionMockData {
    const val TAB_ALL = "all"
    const val TAB_EXPIRING_SOON = "expiring_soon"
    const val TAB_ACTIVE = "active"
    const val TAB_REDEEMED = "redeemed"
    const val TAB_EXPIRED = "expired"

    /** Matches [PromotionMockDispatcher] expiring-soon cutoff for tab filtering. */
    const val EXPIRING_SOON_END = "2026-06-05T23:59:59+07:00"

    /** Total vouchers including generated set (for paging: 4 pages @ size 10). */
    const val TOTAL_VOUCHER_COUNT = 35

    private val coreVouchers: List<MockVoucher> = listOf(
        MockVoucher(
            voucherId = "VCH-ACTIVE-001",
            merchantName = "Highland Coffee",
            title = "Giảm 30% cho hóa đơn từ 100K",
            description = "<h3>Thông tin ưu đãi</h3><p>Giảm <b>30%</b> tối đa <b>50.000đ</b> cho hóa đơn từ <b>100.000đ</b> tại Highland Coffee.</p><p>Áp dụng cho khách hàng sở hữu voucher hợp lệ trong thời gian chương trình.</p>",
            logo = "https://placehold.co/120x120/png?text=Highland",
            banner = "https://placehold.co/720x320/png?text=Highland+Coffee",
            guideline = "<h3>Hướng dẫn sử dụng</h3><ol><li>Chọn voucher tại màn thanh toán.</li><li>Đảm bảo đơn hàng đạt giá trị tối thiểu theo điều kiện.</li><li>Kiểm tra số tiền giảm trước khi xác nhận thanh toán.</li></ol>",
            startDate = "2026-05-01T00:00:00+07:00",
            expirationDate = "2026-06-30T23:59:59+07:00",
            timeSlot = "09:00-22:00",
            status = "ACTIVE",
            displayStatusLabel = "Sử dụng",
            campaignId = "CAMP-HLC-2026-05",
            campaignType = "DISCOUNT_COUPON",
            discountMethod = "ORDER_AMOUNT",
            discountValue = "30000",
            discountPercentage = "30",
            includedProducts = listOf("Coffee", "Tea", "Freeze"),
            excludedProducts = listOf("Combo khuyen mai khac"),
        ),
        MockVoucher(
            voucherId = "VCH-ACTIVE-SOON-002",
            merchantName = "Phuc Long",
            title = "Mua 1 tặng 1 trà sữa",
            description = "<h3>Ưu đãi sắp hết hạn</h3><p>Voucher còn hiệu lực trong thời gian ngắn. Hãy sử dụng trước thời điểm hết hạn để không bỏ lỡ ưu đãi.</p>",
            logo = "https://placehold.co/120x120/png?text=Coffee",
            banner = "https://placehold.co/720x320/png?text=Expiring+Soon",
            guideline = "<h3>Cách sử dụng</h3><ul><li>Mở chi tiết voucher.</li><li>Sử dụng trước thời điểm hết hạn hiển thị trên màn hình.</li><li>Kiểm tra điều kiện áp dụng trước khi thanh toán.</li></ul>",
            startDate = "2026-05-20T00:00:00+07:00",
            expirationDate = "2026-05-31T23:59:59+07:00",
            timeSlot = "14:00-18:00",
            status = "ACTIVE",
            displayStatusLabel = "Sử dụng",
            campaignId = "CAMP-PL-2026-05",
            campaignType = "BUY_X_GET_Y",
            discountMethod = "ITEM",
            discountValue = "1",
            discountPercentage = "0",
            includedProducts = listOf("Milk Tea", "Latte"),
            excludedProducts = listOf("Bottled Drink"),
        ),
        MockVoucher(
            voucherId = "VCH-RESERVED-003",
            merchantName = "KFC",
            title = "Combo ga 99K",
            description = "<h3>Voucher đã được giữ chỗ</h3><p>Voucher đang được giữ cho giao dịch hiện tại và chưa thể dùng cho giao dịch khác.</p>",
            logo = "https://placehold.co/120x120/png?text=Reserved",
            banner = "https://placehold.co/720x320/png?text=Reserved+Voucher",
            guideline = "<h3>Hướng dẫn</h3><ul><li>Hoàn tất giao dịch trong thời gian giữ chỗ.</li><li>Nếu giao dịch bị hủy, trạng thái voucher có thể được cập nhật lại.</li></ul>",
            startDate = "2026-05-01T00:00:00+07:00",
            expirationDate = "2026-06-15T23:59:59+07:00",
            timeSlot = "10:00-21:00",
            status = "RESERVED",
            displayStatusLabel = "Đã đặt chỗ",
            campaignId = "CAMP-KFC-2026-05",
            campaignType = "FIXED_PRICE",
            discountMethod = "ORDER_AMOUNT",
            discountValue = "20000",
            discountPercentage = "0",
            includedProducts = listOf("Combo ga"),
            excludedProducts = listOf("Nuoc uong lon"),
        ),
        MockVoucher(
            voucherId = "VCH-REDEEMED-004",
            merchantName = "Circle K",
            title = "Giảm 20K hóa đơn 80K",
            description = "<h3>Voucher đã sử dụng</h3><p>Voucher này đã được dùng trong một giao dịch trước đó.</p>",
            logo = "https://placehold.co/120x120/png?text=Redeemed",
            banner = "https://placehold.co/720x320/png?text=Redeemed+Voucher",
            guideline = "<h3>Thông tin sử dụng</h3><ul><li>Voucher đã dùng không thể sử dụng lại.</li><li>Kiểm tra lịch sử giao dịch để xem chi tiết ưu đãi đã áp dụng.</li></ul>",
            startDate = "2026-04-01T00:00:00+07:00",
            expirationDate = "2026-05-15T23:59:59+07:00",
            timeSlot = "00:00-23:59",
            status = "REDEEMED",
            displayStatusLabel = "Đã sử dụng",
            campaignId = "CAMP-CK-2026-04",
            campaignType = "DISCOUNT_COUPON",
            discountMethod = "ORDER_AMOUNT",
            discountValue = "20000",
            discountPercentage = "0",
            includedProducts = listOf("Snack", "Nuoc"),
            excludedProducts = listOf("Thuoc la"),
        ),
        MockVoucher(
            voucherId = "VCH-EXPIRED-005",
            merchantName = "Pizza Hut",
            title = "Giảm 50K cho pizza size L",
            description = "<h3>Voucher đã hết hạn</h3><p>Voucher không còn hiệu lực sau thời gian quy định của chương trình.</p>",
            logo = "https://placehold.co/120x120/png?text=Expired",
            banner = "https://placehold.co/720x320/png?text=Expired+Voucher",
            guideline = "<h3>Lưu ý</h3><ul><li>Không thể sử dụng voucher đã hết hạn.</li><li>Vui lòng chọn ưu đãi còn hiệu lực khác nếu có.</li></ul>",
            startDate = "2026-03-01T00:00:00+07:00",
            expirationDate = "2026-04-01T23:59:59+07:00",
            timeSlot = "10:00-22:00",
            status = "EXPIRED",
            displayStatusLabel = "Hết hạn",
            campaignId = "CAMP-PH-2026-03",
            campaignType = "DISCOUNT_COUPON",
            discountMethod = "ORDER_AMOUNT",
            discountValue = "50000",
            discountPercentage = "0",
            includedProducts = listOf("Pizza"),
            excludedProducts = listOf("Nuoc ngot"),
        ),
        MockVoucher(
            voucherId = "VCH-REVOKED-006",
            merchantName = "The Coffee House",
            title = "Giảm 15% toàn menu",
            description = "<h3>Voucher đã bị thu hồi</h3><p>Voucher không còn khả dụng do điều kiện chương trình thay đổi hoặc không còn hợp lệ.</p>",
            logo = "https://placehold.co/120x120/png?text=Revoked",
            banner = "https://placehold.co/720x320/png?text=Revoked+Voucher",
            guideline = "<h3>Hỗ trợ</h3><ul><li>Không thể sử dụng voucher đã bị thu hồi.</li><li>Liên hệ bộ phận hỗ trợ nếu cần kiểm tra thêm.</li></ul>",
            startDate = "2026-04-15T00:00:00+07:00",
            expirationDate = "2026-06-15T23:59:59+07:00",
            timeSlot = "08:00-22:00",
            status = "REVOKED",
            displayStatusLabel = "Bị thu hồi",
            campaignId = "CAMP-TCH-2026-04",
            campaignType = "DISCOUNT_COUPON",
            discountMethod = "ORDER_AMOUNT",
            discountValue = "0",
            discountPercentage = "15",
            includedProducts = listOf("Coffee", "Cake"),
            excludedProducts = listOf("Combo"),
        ),
        MockVoucher(
            voucherId = "VCH-SUSPENDED-007",
            merchantName = "CGV",
            title = "Giảm 40K vé xem phim",
            description = "<h3>Voucher đang tạm dừng</h3><p>Voucher tạm thời chưa thể sử dụng trong thời gian chương trình được tạm dừng.</p>",
            logo = "https://placehold.co/120x120/png?text=Suspended",
            banner = "https://placehold.co/720x320/png?text=Suspended+Voucher",
            guideline = "<h3>Hướng dẫn</h3><ul><li>Vui lòng quay lại sau khi voucher được kích hoạt lại.</li><li>Theo dõi trạng thái voucher trên màn hình chi tiết.</li></ul>",
            startDate = "2026-05-10T00:00:00+07:00",
            expirationDate = "2026-07-01T23:59:59+07:00",
            timeSlot = "09:00-23:00",
            status = "SUSPENDED",
            displayStatusLabel = "Tạm ngừng",
            campaignId = "CAMP-CGV-2026-05",
            campaignType = "DISCOUNT_COUPON",
            discountMethod = "ORDER_AMOUNT",
            discountValue = "40000",
            discountPercentage = "0",
            includedProducts = listOf("2D Ticket"),
            excludedProducts = listOf("IMAX", "4DX"),
        ),
    )

    /**
     * Extra vouchers for manual / integration tests: load-more, tabs, keyword search.
     * With default page size 10: page 0 (10), page 1 (10), page 2 (10), page 3 (5, last).
     */
    private val generatedVouchers: List<MockVoucher> = buildGeneratedVouchers(count = 56)

    private fun buildGeneratedVouchers(count: Int): List<MockVoucher> {
        val merchants = listOf(
            MerchantTemplate("Highland Coffee", "highland", "Cà phê"),
            MerchantTemplate("Phuc Long", "phuc long", "Trà sữa"),
            MerchantTemplate("Starbucks", "coffee", "Beverage"),
            MerchantTemplate("The Coffee House", "coffee house", "Cà phê"),
            MerchantTemplate("Katinat", "katinat", "Trà"),
            MerchantTemplate("Passio Coffee", "passio", "Cà phê"),
            MerchantTemplate("Trung Nguyen", "trung nguyen", "Cà phê"),
        )
        return List(count) { index ->
            val sequence = index + 8
            val merchant = merchants[index % merchants.size]
            val status = statusForGeneratedIndex(index)
            val expiringSoon = status == "ACTIVE" && index % 5 == 0
            val expirationDate = when {
                status == "EXPIRED" -> "2026-04-10T23:59:59+07:00"
                expiringSoon -> "2026-06-03T23:59:59+07:00"
                else -> "2026-08-30T23:59:59+07:00"
            }
            val displayStatusLabel = when (status) {
                "ACTIVE" -> "Sử dụng"
                "REDEEMED" -> "Đã sử dụng"
                "EXPIRED" -> "Hết hạn"
                "RESERVED" -> "Đã đặt chỗ"
                else -> status
            }
            MockVoucher(
                voucherId = "VCH-GEN-${sequence.toString().padStart(3, '0')}",
                merchantName = merchant.name,
                title = "Ưu đãi ${merchant.keyword} #$sequence - Giảm ${10 + (index % 5) * 5}%",
                description = "<p>Voucher mock #$sequence tại ${merchant.name}. Từ khóa: ${merchant.keyword}.</p>",
                logo = "https://placehold.co/120x120/png?text=${merchant.name.take(3)}",
                banner = "https://placehold.co/720x320/png?text=Voucher+$sequence",
                guideline = "<p>Hướng dẫn sử dụng voucher mock #$sequence.</p>",
                startDate = "2026-05-01T00:00:00+07:00",
                expirationDate = expirationDate,
                timeSlot = "08:00-22:00",
                status = status,
                displayStatusLabel = displayStatusLabel,
                campaignId = "CAMP-GEN-2026-${sequence.toString().padStart(3, '0')}",
                campaignType = "DISCOUNT_COUPON",
                discountMethod = "ORDER_AMOUNT",
                discountValue = "${(index + 1) * 5000}",
                discountPercentage = "${10 + (index % 5) * 5}",
                includedProducts = listOf(merchant.category),
                excludedProducts = listOf("Hàng khuyến mãi"),
            )
        }
    }

    private fun statusForGeneratedIndex(index: Int): String {
        return when {
            index % 11 == 0 -> "REDEEMED"
            index % 13 == 0 -> "EXPIRED"
            index % 17 == 0 -> "RESERVED"
            else -> "ACTIVE"
        }
    }

    private data class MerchantTemplate(
        val name: String,
        val keyword: String,
        val category: String,
    )

    val vouchers: List<MockVoucher> = coreVouchers + generatedVouchers
}
