//
//  DemoLoginStore.swift
//  PromotionSDKDemo
//
//  Đối ứng `DemoLoginPrefs` bên `:androidApp`.
//

import Foundation

/// Nhớ **số điện thoại** của lượt đăng nhập gần nhất để lần sau điền sẵn vào ô.
///
/// Chỉ là tiện lợi của app demo: không có nó thì mỗi lần mở app phải gõ lại 11 chữ số, mà app demo
/// bị mở lại vài chục lần một ngày trong vòng lặp dev.
///
/// **Chỉ lưu số điện thoại.** PIN và OTP không được ghi xuống đĩa — đó là thông tin xác thực, và
/// `UserDefaults` là file plist thường trong sandbox app.
enum DemoLoginStore {

    private static let key = "demo.login.msisdn"

    /// Số đã dùng lần trước; chưa từng đăng nhập thì **rỗng** — không có số demo nào để điền hộ.
    static var lastMsisdn: String {
        get { UserDefaults.standard.string(forKey: key) ?? "" }
        set { UserDefaults.standard.set(newValue, forKey: key) }
    }
}
