// swift-tools-version: 5.10
// The swift-tools-version declares the minimum version of Swift required to build this package.

import PackageDescription

let package = Package(
    name: "PRMFoundation",
    defaultLocalization: "vi",
    platforms: [.iOS(.v13)],
    products: [
        // Products define the executables and libraries a package produces, making them visible to other packages.
        .library(
            name: "PRMFoundation",
            targets: ["PRMFoundation"]
        ),
    ],
    dependencies: [],
    targets: [
        .target(
            name: "PRMFoundation",
            dependencies: []
        ),
        .testTarget(
            name: "PRMFoundationTests",
            dependencies: ["PRMFoundation"]
        ),
    ]
)
