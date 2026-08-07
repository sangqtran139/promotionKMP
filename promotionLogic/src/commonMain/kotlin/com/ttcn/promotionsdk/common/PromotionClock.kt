package com.ttcn.promotionsdk.common

import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Thời điểm hiện tại (epoch millis), để lõi tính "còn X ngày".
 *
 * Trước đây là `expect/actual` (`System.currentTimeMillis()` / `NSDate().timeIntervalSince1970`) vì
 * stdlib chưa có đồng hồ đa nền tảng. Từ Kotlin 2.1.20 `kotlin.time.Clock` **nằm sẵn trong stdlib**,
 * nên hai file `actual` đã bỏ — không cần `kotlinx-datetime`, không thêm dependency nào.
 *
 * `Clock` còn `@ExperimentalTime`. Rủi ro được chặn ở đây: hàm này `internal` nên trạng thái
 * experimental **không rò ra host**, và mọi chỗ gọi đều đi qua đúng cái tên này — nếu API stdlib đổi
 * thì chỉ sửa một dòng, hoặc quay lại `expect/actual`, mà không call site nào phải động tới.
 */
@OptIn(ExperimentalTime::class)
internal fun currentEpochMillis(): Long = Clock.System.now().toEpochMilliseconds()

/**
 * Parse chuỗi ngày ISO (`yyyy-MM-dd` hoặc `yyyy-MM-ddTHH:mm:ss`) → epoch millis (UTC), null nếu sai.
 * Dùng thuật toán days-from-civil (Howard Hinnant) — thuần Kotlin, không phụ thuộc `kotlinx-datetime`.
 */
internal fun isoDateToEpochMillis(raw: String?): Long? {
    if (raw.isNullOrBlank()) return null
    val date = raw.take(10).split("-")
    if (date.size != 3) return null
    val y = date[0].toIntOrNull() ?: return null
    val m = date[1].toIntOrNull() ?: return null
    val d = date[2].toIntOrNull() ?: return null
    if (m !in 1..12 || d !in 1..31) return null
    return daysFromCivil(y, m, d) * 86_400_000L
}

/** Số ngày (làm tròn lên) từ hiện tại đến [expiryIso]; null nếu không parse được. */
internal fun daysUntil(expiryIso: String?, now: Long = currentEpochMillis()): Int? {
    val expiryMs = isoDateToEpochMillis(expiryIso) ?: return null
    val diff = expiryMs - now
    // ceil để "còn <1 ngày" vẫn hiển thị 1 (khớp logic cũ 2 nền tảng).
    return ((diff + 86_400_000L - 1) / 86_400_000L).toInt()
}

private fun daysFromCivil(year: Int, month: Int, day: Int): Long {
    val y = if (month <= 2) year - 1 else year
    val era = (if (y >= 0) y else y - 399) / 400
    val yoe = (y - era * 400).toLong()                       // [0, 399]
    val mp = if (month > 2) month - 3 else month + 9         // [0, 11]
    val doy = (153L * mp + 2) / 5 + day - 1                  // [0, 365]
    val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy          // [0, 146096]
    return era.toLong() * 146097 + doe - 719468
}
