// swift-tools-version: 5.10

import Foundation
import PackageDescription

/// Đường thoát khỏi vòng "phải publish xong mới verify được".
///
/// Đặt biến môi trường trỏ vào xcframework **cục bộ** thì app demo link thẳng bản vừa dựng, không
/// đụng Artifactory:
///
///     PRM_LOCAL_XCFRAMEWORK=../iosPromotionSDK/build/Promotion.xcframework \
///       xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp ...
///
/// Vì sao cần: `binaryTarget(url:checksum:)` đòi checksum, mà checksum chỉ có **sau** khi zip đã
/// publish. Không có đường này thì mỗi lần muốn thử một thay đổi Swift là phải đốt một số version
/// trên repo phát hành (repo không cho ghi đè), và bản đẩy lên chưa ai link thử bao giờ.
///
/// Đường dẫn tương đối tính từ **thư mục chứa Package.swift này**, đúng như SwiftPM hiểu `path:`.
/// Không đặt biến → giữ nguyên hành vi cũ (kéo từ Artifactory), nên CI và máy dev khác không đổi gì.
let localXCFramework = ProcessInfo.processInfo.environment["PRM_LOCAL_XCFRAMEWORK"]

let promotionTarget: Target = localXCFramework.map { path in
    .binaryTarget(name: "Promotion", path: path)
} ?? .binaryTarget(
    name: "Promotion",
    url: "https://mobile-data.viettelmoney.vn/artifactory/vdo-ios-frameworks/Martech/Promotion/1.0.0/Promotion-1.0.0.xcframework.zip",
    checksum: "9da231216a057b570df3164e06ec51d5087a7fd64a74f2466f4a2da557caf065"
)

/// Cầu nối để `iosApp` lấy SDK **từ Artifactory** thay vì từ thư mục build cục bộ.
///
/// `binaryTarget` chỉ nhận `url:` khi nằm trong một Swift package — không khai thẳng vào
/// `.xcodeproj` được. Package này tồn tại đúng vì lý do đó, nó không chứa code.
///
/// ## Mỗi lần phát hành bản mới, phải sửa TAY hai dòng dưới
///
/// Thứ tự bắt buộc — không đảo được:
///
///   1. `./scripts/build-ios.sh publish -v <version>`   (dựng + đẩy lên Artifactory)
///   2. Chép `download_url` + `checksum` từ output của script vào `url:` / `checksum:` dưới đây
///   3. Xoá cache SPM nếu Xcode còn giữ bản cũ (xem ghi chú cuối file)
///
/// Con gà quả trứng: `checksum` chỉ có sau khi zip đã publish, nên **không thể** trỏ package này
/// vào code SDK đang sửa dở. Sửa Swift trong SDK xong mà chưa publish thì app demo vẫn chạy bản cũ
/// trên server — không có cảnh báo nào cả. Đây là cái giá của cách này, không phải lỗi cấu hình.
///
/// ## Xác thực
///
/// Repo Artifactory cần Basic auth, mà SwiftPM không bao giờ hỏi mật khẩu — nó đọc `~/.netrc`:
///
///     machine mobile-data.viettelmoney.vn login <user> password <identity token>
///
/// Thiếu dòng đó thì Xcode chỉ báo "failed downloading" cụt lủn, không nói là 401. Máy dev mới và
/// CI runner đều phải tự khai.
///
/// `checksum` = sha256 thường của file zip (đã kiểm: `swift package compute-checksum` ra đúng con
/// số mà `shasum -a 256` cho), nên lấy thẳng từ `metadata.json` cạnh zip trên Artifactory.
let package = Package(
    name: "PromotionRemote",
    platforms: [.iOS(.v13)],
    products: [
        .library(name: "PromotionRemote", targets: ["Promotion"]),
    ],
    targets: [
        // Tên target BẮT BUỘC trùng tên xcframework bên trong zip (`Promotion.xcframework`),
        // không phải tên framework lõi (`PromotionKit.framework`, module host `import PromotionKit`).
        // Nguồn (Artifactory hay đĩa cục bộ) chọn ở đầu file — xem `PRM_LOCAL_XCFRAMEWORK`.
        promotionTarget,
    ]
)

// Ghi chú cache: SPM lưu artifact đã tải ở `<DerivedData>/SourcePackages/artifacts/`. Đổi zip mà
// GIỮ NGUYÊN url thì nó dùng lại bản cũ. Vì repo phát hành không cho ghi đè version nên tình huống
// này hiếm — nhưng nếu gặp, xoá `iosApp/build/DerivedData/SourcePackages` rồi resolve lại.
