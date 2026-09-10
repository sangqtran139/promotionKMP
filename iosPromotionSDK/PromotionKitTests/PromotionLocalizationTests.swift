//
//  PromotionLocalizationTests.swift
//  PromotionKitTests
//
//  Khoá lại tầng chuỗi hiển thị: `PromotionSessionConfig.language` phải **đổi được chữ trên UI**.
//
//  Vì sao đáng test: trước đây `language` là tham số public chỉ đi xuống header của Ktor — một
//  tham số không làm điều mà tên nó nói, và không có gì phát hiện ra. Đây cũng đúng loại lỗi mà
//  "0 test tầng Swift" để lọt: code biên dịch được, doc đầy đủ, hành vi không có.
//

import XCTest
@testable import PromotionKit

final class PromotionLocalizationTests: XCTestCase {

    /// Mỗi test tự đặt ngôn ngữ nó cần; trả về mặc định sau đó để không rò trạng thái sang test khác
    /// (`PRMLocalization.languageCode` là state tĩnh, dùng chung cả process test).
    override func tearDown() {
        PRMLocalization.configure(languageCode: PRMLocalization.defaultLanguageCode)
        super.tearDown()
    }

    func test_macDinh_laTiengViet() {
        PRMLocalization.configure(languageCode: PRMLocalization.defaultLanguageCode)

        XCTAssertEqual(PromotionUIStrings.useNow, "Sử dụng ngay")
        XCTAssertEqual(PromotionUIStrings.A11y.backButton, "Quay lại")
    }

    /// Đây là vế chính của CODE-3: host truyền `language` khác thì chữ trên UI phải đổi theo.
    func test_doiLanguage_thiChuTrenUIDoiTheo() {
        PRMLocalization.configure(languageCode: "en")

        XCTAssertEqual(PromotionUIStrings.useNow, "Use now")
        XCTAssertEqual(PromotionUIStrings.A11y.backButton, "Back")
    }

    /// Host hay truyền mã đầy đủ (`"en-US"`) trong khi thư mục là `en.lproj`. Không thử mã ngắn thì
    /// luôn trượt về tiếng Việt — hỏng một cách im lặng.
    func test_maNgonNguDayDu_vanTraDungBang() {
        PRMLocalization.configure(languageCode: "en-US")

        XCTAssertEqual(PromotionUIStrings.useNow, "Use now")
    }

    /// Ngôn ngữ không có bảng → **không** được hiện key trần (`prm_use_now`) lên màn hình.
    func test_ngonNguLa_luiVeTiengViet() {
        PRMLocalization.configure(languageCode: "fr-FR")

        XCTAssertEqual(PromotionUIStrings.useNow, "Sử dụng ngay")
    }

    /// Chuỗi rỗng/khoảng trắng từ host không được làm hỏng việc tra bảng.
    func test_languageRong_luiVeMacDinh() {
        PRMLocalization.configure(languageCode: "   ")

        XCTAssertEqual(PRMLocalization.languageCode, PRMLocalization.defaultLanguageCode)
        XCTAssertEqual(PromotionUIStrings.useNow, "Sử dụng ngay")
    }

    /// `PromotionUIStrings` phải là `static var` (computed). Nếu ai đó đổi lại thành `static let`,
    /// giá trị đóng băng ở lần đọc đầu và test này đỏ — đó chính là điều nó canh.
    func test_doiLanguageGiuaChung_giaTriKhongBiDongBang() {
        PRMLocalization.configure(languageCode: PRMLocalization.defaultLanguageCode)
        let vietnamese = PromotionUIStrings.apply

        PRMLocalization.configure(languageCode: "en")
        let english = PromotionUIStrings.apply

        XCTAssertEqual(vietnamese, "Áp dụng")
        XCTAssertEqual(english, "Apply")
        XCTAssertNotEqual(vietnamese, english)
    }

    /// Hai bảng phải có **cùng tập khoá**. Thiếu khoá bên `en` thì màn hình lẫn hai ngôn ngữ, và đó
    /// là thứ chỉ QA mắt thường mới bắt được nếu không có test này.
    func test_haiBangChuoi_cungTapKhoa() throws {
        let bundle = Bundle(for: PromotionLocalizationTests.self)
        // Test chạy trong bundle test, còn `.strings` nằm trong framework → lấy bundle của SDK.
        let sdkBundle = Bundle(for: PromotionSDKImpl.self)

        let vi = try keys(in: sdkBundle, language: "vi")
        let en = try keys(in: sdkBundle, language: "en")

        XCTAssertFalse(vi.isEmpty, "Không đọc được vi.lproj/PromotionKit.strings (bundle: \(bundle))")
        XCTAssertEqual(vi.symmetricDifference(en), [], "Khoá lệch giữa vi.lproj và en.lproj")
    }

    private func keys(in bundle: Bundle, language: String) throws -> Set<String> {
        let path = try XCTUnwrap(
            bundle.path(forResource: language, ofType: "lproj"),
            "Không thấy \(language).lproj trong framework"
        )
        let stringsPath = path + "/PromotionKit.strings"
        let dict = try XCTUnwrap(
            NSDictionary(contentsOfFile: stringsPath) as? [String: String],
            "Không đọc được \(stringsPath)"
        )
        return Set(dict.keys)
    }
}
