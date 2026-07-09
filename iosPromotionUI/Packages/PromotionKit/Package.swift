// swift-tools-version: 5.10

import PackageDescription

/// Lớp keo mỏng giữa lõi Kotlin và tầng UI iOS. **Không chứa nghiệp vụ.**
///
/// Thay cho hai gói cũ `PromotionLogic` (Swift) và `Repository` — cả hai đã bị xoá vì nghiệp vụ
/// nay nằm trọn trong `PromotionLogic.xcframework` (Kotlin Multiplatform).
///
/// Gói này chỉ làm hai việc mà Kotlin không làm được:
/// 1. Chuyển `async throws` thành `RxSwift.Single` — ViewModel hiện tại đều nhận `Single`.
/// 2. Đặt `typealias` để giấu prefix `PromotionLogic*` mà Kotlin/Native sinh ra.
let package = Package(
    name: "PromotionKit",
    platforms: [.iOS(.v13)],
    products: [
        .library(name: "PromotionKit", targets: ["PromotionKit"]),
    ],
    dependencies: [
        .package(path: "../Utility"),
    ],
    targets: [
        .binaryTarget(
            name: "PromotionLogic",
            path: "../../Frameworks/PromotionLogic.xcframework"
        ),
        .target(
            name: "PromotionKit",
            dependencies: [
                "Utility",
                "PromotionLogic",
            ]
        ),
    ]
)
