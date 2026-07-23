#!/usr/bin/env bash
#
# Build PromotionSDK.xcframework (device + simulator slices).
#
# Usage:
#   ./scripts/build-xcframework.sh [output_dir]
#
# Output: <output_dir>/PromotionSDK.xcframework  (default output_dir = ./build)
#
# Trước khi chạy, phải có Frameworks/PromotionLogic.xcframework — sinh từ Gradle:
#   ./gradlew :promotionLogic:assemblePromotionLogicReleaseXCFramework
#
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
REPO="$(cd "$ROOT/.." && pwd)"
PROJECT="$ROOT/PromotionSDK.xcodeproj"
SCHEME="PromotionSDK"
FRAMEWORK="PromotionSDK.framework"   # = PRODUCT_NAME, khác tên class PromotionSDK
BUILD_DIR="${1:-$ROOT/build}"
OUT="$BUILD_DIR/PromotionSDK.xcframework"

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
# `-debug-symbols` của create-xcframework CHỈ nhận đường dẫn tuyệt đối. Tham số $1 có thể là
# đường dẫn tương đối, nên chuẩn hoá ngay ở đây thay vì nhớ ra lúc lệnh đã fail.
BUILD_DIR="$(cd "$BUILD_DIR" && pwd)"
OUT="$BUILD_DIR/PromotionSDK.xcframework"

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

# Target framework đặt STRIP_STYLE = non-global (xem pbxproj), nên binary giao cho host KHÔNG còn
# local symbol: crash log chỉ symbolicate được bằng dSYM. Mà dSYM nằm trong .xcarchive, bị
# `rm -rf "$BUILD_DIR"` xoá sạch ở lần build sau → phải giữ lại từng bản phát hành.
#
# Để dSYM CẠNH xcframework, không phải TRONG nó (`create-xcframework -debug-symbols`): riêng dSYM
# device đã 26MB, nhét vào thì gói phân phối phồng từ ~35MB lên hơn 100MB, mất trắng chỗ vừa cắt.
# Host không cần dSYM để build — chỉ người giữ bản phát hành cần, khi đọc crash report.
echo "▶︎ Tách dSYM (device) ra cạnh xcframework"
cp -R "$BUILD_DIR/dev.xcarchive/dSYMs/$FRAMEWORK.dSYM" "$BUILD_DIR/"

# `*.abi.json` — mỗi slice một file ~660KB, ba slice là ~2MB, tức 5-6% cả gói. Swift sinh ra nó khi
# bật library evolution, nhưng nó chỉ phục vụ `swift-api-digester` (công cụ so ABI giữa hai bản).
# Host compile theo `.swiftinterface` nằm cùng thư mục, không đọc file này bao giờ.
#
# Xoá được vì framework CHƯA ký (CODE_SIGNING_ALLOWED=NO) — host tự ký lúc embed. Nếu sau này bật ký
# ở đây thì phải xoá TRƯỚC khi ký, không thì hỏng chữ ký.
echo "▶︎ Xoá *.abi.json (chỉ dùng cho công cụ so ABI, host không cần)"
find "$OUT" -name "*.abi.json" -delete

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
echo "🔎 dSYM: $BUILD_DIR/$FRAMEWORK.dSYM"
echo "   Binary đã strip local symbol — GIỮ dSYM này lại theo từng bản phát hành, không có nó thì"
echo "   crash report của app host chỉ còn địa chỉ trần."
echo ""
echo "ℹ️  Framework động app host cần tự nhúng (nếu có):"
ls "$BUILD_DIR/dev.xcarchive/Products/Library/Frameworks/" 2>/dev/null | grep -v "^$FRAMEWORK$" || echo "   (không có — mọi dependency đã link tĩnh)"
