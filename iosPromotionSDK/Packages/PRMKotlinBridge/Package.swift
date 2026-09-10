// swift-tools-version: 5.10

import PackageDescription

/// Lớp keo mỏng giữa lõi Kotlin và tầng UI iOS. **Không chứa nghiệp vụ.**
///
/// Nghiệp vụ nằm trọn trong `PromotionLogic.xcframework` (Kotlin Multiplatform).
///
/// Gói này chỉ làm các việc keo mà Kotlin không làm được — cả hai nằm trong namespace `PRMKotlin`
/// chứ không phải global function, vì gói này được `@_exported` nên global ở đây là global ở
/// mọi file của tầng UI:
/// 1. `PRMKotlin.boxed(_:)` — bọc `Int?` của Swift thành `KotlinInt?` mà Kotlin/Native chờ.
/// 2. `PRMKotlin.toPromotionError(_:)` — bóc exception Kotlin trong `NSError` thành `PromotionError`.
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
