//
//  DemoEnvironment.swift
//  PromotionSDKDemo
//
//  Môi trường của app demo — ĐỐI ỨNG product flavor `staging`/`uat`/`product` bên `:androidApp`.
//

import Foundation

/// Cấu hình theo môi trường, đọc từ `Info.plist` (giá trị do configuration đang build bơm vào).
///
/// Đối ứng `BuildConfig.DEMO_BASE_URL` bên Android: cả hai đều **chọn lúc build**, không phải lúc
/// chạy. Android đổi bằng Build Variants / `assembleUatDebug`; iOS đổi bằng **scheme**
/// (`iosApp-Staging` / `iosApp-Uat` / `iosApp-Product`), mỗi scheme trỏ vào configuration cùng tên.
///
/// Chuỗi URL nằm ở `project.pbxproj` (build setting `PRM_BASE_URL` của từng configuration) chứ không
/// ở đây — nếu để trong Swift thì lại phải rẽ nhánh bằng `#if`, tức thêm một bộ cờ biên dịch song
/// song với configuration, hai nguồn sự thật cho cùng một câu hỏi.
enum DemoEnvironment {

    /// Giá trị lùi khi `Info.plist` không có key (project bị sửa, hoặc chạy từ một target khác chưa
    /// khai `PRM_BASE_URL`). Trùng môi trường UAT — đúng thứ app dùng trước khi có nhiều môi trường.
    private static let fallbackBaseUrl = "https://api24cdn.vtmoney.vn/uatmm"

    /// Base URL Promotion BFF + server login demo. Không có dấu `/` cuối: `PromotionSDK.initialize`
    /// nhận dạng này, chỗ nào cần thì tự nối — cùng quy ước với `DEMO_BASE_URL` bên Android.
    static let baseUrl: String = {
        let raw = Bundle.main.object(forInfoDictionaryKey: "PRM_BASE_URL") as? String
        // Chuỗi RỖNG cũng phải lùi, không chỉ `nil`: Xcode để nguyên `$(PRM_BASE_URL)` thành "" khi
        // configuration không khai setting đó — app sẽ gọi vào URL rỗng và chết ở tận tầng mạng với
        // "unsupported URL", không nói gì về nguyên nhân thật.
        guard let raw, !raw.trimmingCharacters(in: .whitespaces).isEmpty else {
            print("[Demo] Thiếu PRM_BASE_URL trong Info.plist → lùi về \(fallbackBaseUrl)")
            return fallbackBaseUrl
        }
        // Cắt dấu '/' thừa cho chắc: người sửa project sau này rất dễ gõ kèm.
        return raw.hasSuffix("/") ? String(raw.dropLast()) : raw
    }()

    /// Tên môi trường đang build: `staging` / `uat` / `product`. Đối ứng `BuildConfig.DEMO_ENV` bên
    /// `:androidApp`. Thiếu key → coi như `uat`, cùng nước lùi với [baseUrl].
    static let name: String = {
        let raw = (Bundle.main.object(forInfoDictionaryKey: "PRM_ENV") as? String) ?? ""
        return raw.isEmpty ? "uat" : raw
    }()

    /// Rẽ nhánh theo **tên môi trường**, không theo [baseUrl]: so chuỗi URL là mong manh, đổi địa chỉ
    /// một chữ là nhánh này im lặng ngừng chạy. Xem `LoginService.requestOtp`.
    static var isStaging: Bool { name == "staging" }
}
