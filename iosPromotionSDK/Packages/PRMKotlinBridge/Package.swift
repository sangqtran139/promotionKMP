// swift-tools-version: 5.10

import PackageDescription

/// Lớp keo mỏng giữa lõi Kotlin và tầng UI iOS. **Không chứa nghiệp vụ.**
///
/// Nghiệp vụ nằm trọn trong `PromotionLogic.xcframework` (Kotlin Multiplatform).
///
/// Gói này chỉ làm các việc keo mà Kotlin không làm được:
/// 1. `boxed(_:)` — bọc `Int?` của Swift thành `KotlinInt?` mà Kotlin/Native chờ.
/// 2. `toPromotionError(_:)` — bóc exception Kotlin trong `NSError` thành `PromotionError` chuẩn hoá.
/// (Tầng UI gọi `suspend` Kotlin trực tiếp bằng async/await trong `Task`.)
let package = Package(
    name: "PRMKotlinBridge",
    platforms: [.iOS(.v13)],
    products: [
        .library(name: "PRMKotlinBridge", targets: ["PRMKotlinBridge"]),
    ],
    dependencies: [
        .package(path: "../PRMFoundation"),
    ],
    targets: [
        .binaryTarget(
            name: "PromotionLogic",
            path: "../../Frameworks/PromotionLogic.xcframework"
        ),
        .target(
            name: "PRMKotlinBridge",
            dependencies: [
                "PRMFoundation",
                "PromotionLogic",
            ]
        ),
    ]
)
