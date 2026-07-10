#!/usr/bin/env bash
#
# Build PromotionSDKUI.xcframework (device + simulator slices).
#
# Usage:
#   ./scripts/build-xcframework.sh [output_dir]
#
# Output: <output_dir>/PromotionSDKUI.xcframework  (default output_dir = ./build)
#
# Trước khi chạy, phải có Frameworks/PromotionLogic.xcframework — sinh từ Gradle:
#   ./gradlew :promotionLogic:assemblePromotionLogicReleaseXCFramework
#
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
REPO="$(cd "$ROOT/.." && pwd)"
PROJECT="$ROOT/PromotionSDKUI.xcodeproj"
SCHEME="PromotionSDKUI"
FRAMEWORK="PromotionSDKUI.framework"   # = PRODUCT_NAME, khác tên class PromotionSDK
BUILD_DIR="${1:-$ROOT/build}"
OUT="$BUILD_DIR/PromotionSDKUI.xcframework"

KOTLIN_XCF="$ROOT/Frameworks/PromotionLogic.xcframework"
# Luôn dựng lại và đồng bộ, KHÔNG chỉ khi thiếu.
#
# Bản cũ chỉ copy khi thư mục chưa tồn tại. Sửa Kotlin xong chạy script này thì Swift vẫn compile
# với header cũ, và lỗi hiện ra ở tận `SwiftCompile` với thông báo "cannot find ... in scope" —
# rất khó lần ra nguyên nhân. Gradle đã có up-to-date check nên gọi lại không tốn gì.
echo "▶︎ Đồng bộ PromotionLogic.xcframework từ Gradle"
(cd "$REPO" && ./gradlew :promotionLogic:assemblePromotionLogicReleaseXCFramework)
rm -rf "$KOTLIN_XCF"
mkdir -p "$ROOT/Frameworks"
cp -R "$REPO/promotionLogic/build/XCFrameworks/release/PromotionLogic.xcframework" "$ROOT/Frameworks/"

rm -rf "$BUILD_DIR"
mkdir -p "$BUILD_DIR"

archive() {
  local destination="$1" archive_path="$2"
  echo "▶︎ Archiving: $destination"
  xcodebuild archive \
    -project "$PROJECT" \
    -scheme "$SCHEME" \
    -destination "$destination" \
    -archivePath "$archive_path" \
    -clonedSourcePackagesDirPath "$ROOT/.spm" \
    SKIP_INSTALL=NO \
    CODE_SIGNING_ALLOWED=NO \
    -verbose
}

archive 'generic/platform=iOS Simulator' "$BUILD_DIR/sim.xcarchive"
archive 'generic/platform=iOS'           "$BUILD_DIR/dev.xcarchive"

echo "▶︎ Creating $OUT"
xcodebuild -create-xcframework \
  -framework "$BUILD_DIR/sim.xcarchive/Products/Library/Frameworks/$FRAMEWORK" \
  -framework "$BUILD_DIR/dev.xcarchive/Products/Library/Frameworks/$FRAMEWORK" \
  -output "$OUT"

# Xcode dịch sẵn `.swiftinterface` của framework thành module nhị phân rồi cache ở
# `SwiftExplicitPrecompiledModules/` (explicit module build). Cache đó **không** tự hết hạn khi
# xcframework được dựng lại: đổi một chữ ký public xong, app host vẫn compile theo chữ ký cũ và
# báo lỗi trỏ vào file interface đang ghi đúng thứ khác. Đã mất một buổi vì chuyện này:
#
#     error: extra arguments at positions #1, #2, #3 in call
#     note: 'getVouchers(keyword:serviceCode:tab:myPage:mySize:completion:)' declared here
#
# Cùng họ với cái bẫy header Kotlin ở đầu file, chỉ lùi thêm một tầng — sang phía app host.
# Xoá đây cho khỏi phải nhớ ⇧⌘K. Chỉ đụng cache của chính framework này; Xcode dựng lại khi cần.
DERIVED_DATA="$HOME/Library/Developer/Xcode/DerivedData"
if [ -d "$DERIVED_DATA" ]; then
  echo "▶︎ Dọn precompiled module cache của $SCHEME trong DerivedData"
  find "$DERIVED_DATA" \
    -type d \
    -path "*/SwiftExplicitPrecompiledModules" \
    -exec sh -c 'rm -rf "$1"/'"$SCHEME"'-*.swiftmodule' _ {} \; 2>/dev/null || true
fi

echo ""
echo "✅ Xong: $OUT"
echo ""
echo "ℹ️  Framework động app host cần tự nhúng (nếu có):"
ls "$BUILD_DIR/dev.xcarchive/Products/Library/Frameworks/" 2>/dev/null | grep -v "^$FRAMEWORK$" || echo "   (không có — mọi dependency đã link tĩnh)"
