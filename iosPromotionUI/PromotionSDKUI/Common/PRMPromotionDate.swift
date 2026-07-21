//
//  PRMPromotionDate.swift
//  PRMPromotionUI
//
//  Thay cho `DateUseCase` của gói PromotionLogic (Swift) cũ. Đây **không** phải nghiệp vụ:
//  lõi Kotlin trả `expirationDate` dạng chuỗi ISO8601, còn UIKit cần `Date` để định dạng theo
//  locale. Vì vậy nó ở lại phía Swift, và bỏ chữ "UseCase" khỏi tên.
//

import Foundation

enum PRMPromotionDate {

    private static let vnTimeZone = TimeZone(identifier: "Asia/Ho_Chi_Minh")

    private static let iso8601: ISO8601DateFormatter = {
        let f = ISO8601DateFormatter()
        if let tz = vnTimeZone { f.timeZone = tz }
        return f
    }()

    private static let iso8601Fractional: ISO8601DateFormatter = {
        let f = ISO8601DateFormatter()
        f.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        if let tz = vnTimeZone { f.timeZone = tz }
        return f
    }()

    /// Server không phải lúc nào cũng trả ISO8601 chuẩn — thiếu timezone, có mili-giây, hoặc
    /// date-time thường. Giữ nguyên danh sách fallback của bản cũ.
    private static let fallbackFormats = [
        "yyyy-MM-dd'T'HH:mm:ss.SSSXXXXX",
        "yyyy-MM-dd'T'HH:mm:ssXXXXX",
        "yyyy-MM-dd'T'HH:mm:ss.SSS",
        "yyyy-MM-dd'T'HH:mm:ss",
        "yyyy-MM-dd HH:mm:ss",
        "yyyy-MM-dd",
    ]

    static func parse(_ string: String?) -> Date? {
        guard let trimmed = string?.trimmingCharacters(in: .whitespacesAndNewlines),
              !trimmed.isEmpty else { return nil }

        if let date = iso8601.date(from: trimmed) { return date }
        if let date = iso8601Fractional.date(from: trimmed) { return date }

        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "en_US_POSIX")
        if let tz = vnTimeZone { formatter.timeZone = tz }
        for format in fallbackFormats {
            formatter.dateFormat = format
            if let date = formatter.date(from: trimmed) { return date }
        }
        return nil
    }

    private static let displayFormatter: DateFormatter = {
        let f = DateFormatter()
        f.dateFormat = "dd/MM/yyyy"
        if let tz = vnTimeZone { f.timeZone = tz }
        f.locale = Locale(identifier: "vi_VN")
        return f
    }()

    static func display(_ date: Date) -> String {
        displayFormatter.string(from: date)
    }

    /// Đã hết hạn thì **không** tính là "sắp hết hạn" — giữ nguyên logic bản cũ.
    static func isExpiringSoon(_ expirationDate: Date, thresholdDays: Int) -> Bool {
        let now = Date()
        guard expirationDate >= now else { return false }
        return expirationDate.timeIntervalSince(now) / 86_400 <= Double(thresholdDays)
    }
}
