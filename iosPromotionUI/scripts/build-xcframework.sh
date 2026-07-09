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
if [[ ! -d "$KOTLIN_XCF" ]]; then
  echo "▶︎ Chưa có $KOTLIN_XCF — build từ Gradle"
  (cd "$REPO" && ./gradlew :promotionLogic:assemblePromotionLogicReleaseXCFramework)
  rm -rf "$KOTLIN_XCF"
  cp -R "$REPO/promotionLogic/build/XCFrameworks/release/PromotionLogic.xcframework" "$ROOT/Frameworks/"
fi

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

echo ""
echo "✅ Xong: $OUT"
echo ""
echo "ℹ️  Framework động app host cần tự nhúng (nếu có):"
ls "$BUILD_DIR/dev.xcarchive/Products/Library/Frameworks/" 2>/dev/null | grep -v "^$FRAMEWORK$" || echo "   (không có — mọi dependency đã link tĩnh)"
