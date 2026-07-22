package com.ttcn.promotionsdk.core.util

/** Thời điểm hiện tại (epoch millis). expect/actual để lõi tính "còn X ngày" mà không cần thư viện date. */
internal expect fun currentEpochMillis(): Long

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
