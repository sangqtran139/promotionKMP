#!/usr/bin/env bash
#
# Build SDK iOS + app demo trên một máy bất kỳ. Đối xứng với build-android.sh.
#
# Chuỗi phụ thuộc (phải đúng thứ tự, script này ép sẵn):
#
#   :promotionLogic (Kotlin)  ──gradle──▶  PromotionLogic.xcframework   (lõi, static)
#            └─ link tĩnh vào ──▶  PromotionSDKUI.xcframework           (UI, Swift)
#                                          └─ iosApp link + embed
#
# Khác Android: iOS **không** đi qua Maven. Host nhận thẳng một XCFramework
# (docs/Distribution.md §6). Nên "publish" ở đây = dựng PromotionSDKUI.xcframework.
#
#   ./scripts/build-ios.sh                 # dựng XCFramework → build app demo (simulator)
#   ./scripts/build-ios.sh --skip-app      # chỉ dựng PromotionSDKUI.xcframework
#   ./scripts/build-ios.sh --clean         # xoá DerivedData của app rồi làm lại
#   ./scripts/build-ios.sh --run           # build xong cài + mở trên simulator đang boot
#   ./scripts/build-ios.sh --device 'iPhone 17 Pro'   # chọn simulator (mặc định: máy đang boot)
#
set -euo pipefail

cd "$(dirname "$0")/.."   # luôn chạy từ gốc repo

SKIP_APP=false
DO_CLEAN=false
DO_RUN=false
DEVICE=""

while [[ $# -gt 0 ]]; do
    case "$1" in
        --skip-app) SKIP_APP=true; shift ;;
        --clean)    DO_CLEAN=true; shift ;;
        --run)      DO_RUN=true; shift ;;
        --device)   DEVICE="${2:-}"; shift 2 ;;
        -h|--help)  sed -n '3,20p' "$0" | sed 's/^# \{0,1\}//'; exit 0 ;;
        *)          echo "Tham số lạ: $1 (xem --help)" >&2; exit 1 ;;
    esac
done

# ─── Điều kiện cần ───────────────────────────────────────────────────────────────────────────
# Build iOS chỉ chạy trên macOS có Xcode (không phải Command Line Tools trần).

if ! command -v xcodebuild >/dev/null 2>&1; then
    echo "Không thấy xcodebuild — cần macOS + Xcode. (Android thì dùng scripts/build-android.sh)" >&2
    exit 1
fi

DERIVED="$PWD/iosApp/build/DerivedData"
APP_NAME="iosApp.app"
XCFRAMEWORK="iosPromotionUI/build/PromotionSDKUI.xcframework"

if [[ "$DO_CLEAN" == true ]]; then
    echo "▸ Dọn DerivedData của app demo"
    rm -rf "$DERIVED"
fi

# ─── 1. Dựng PromotionSDKUI.xcframework ──────────────────────────────────────────────────────
# Script này tự gọi Gradle dựng PromotionLogic.xcframework rồi copy vào iosPromotionUI/Frameworks/
# TRƯỚC khi archive Swift — sửa Kotlin xong mà dùng header cũ thì lỗi hiện ra tận SwiftCompile
# ("cannot find ... in scope"), rất khó lần.

echo "▸ Dựng PromotionSDKUI.xcframework (kèm lõi PromotionLogic từ Gradle)"
./iosPromotionUI/scripts/build-xcframework.sh

if [[ ! -d "$XCFRAMEWORK" ]]; then
    echo "Không thấy $XCFRAMEWORK sau khi build." >&2
    exit 1
fi

if [[ "$SKIP_APP" == true ]]; then
    echo "✓ Xong: $XCFRAMEWORK"
    exit 0
fi

# ─── 2. Build app demo ───────────────────────────────────────────────────────────────────────
# iosApp link XCFramework theo đường dẫn tương đối (../iosPromotionUI/build/…) nên bước 1 là bắt buộc.

# Dùng `-scheme` (không phải `-target`): `-derivedDataPath` bắt buộc đi kèm scheme. Scheme `iosApp`
# đã được **shared** (xcshareddata/xcschemes) nên máy khác clone về là có ngay — để trong xcuserdata
# thì chỉ máy của người tạo mới thấy.
echo "▸ Build app demo (simulator)"
xcodebuild build \
    -project iosApp/iosApp.xcodeproj \
    -scheme iosApp \
    -configuration Debug \
    -sdk iphonesimulator \
    -derivedDataPath "$DERIVED" \
    CODE_SIGNING_ALLOWED=NO \
    -quiet

APP="$DERIVED/Build/Products/Debug-iphonesimulator/$APP_NAME"
echo "✓ Xong: $APP"

# ─── 3. Cài + mở trên simulator ──────────────────────────────────────────────────────────────

if [[ "$DO_RUN" == true ]]; then
    if [[ -n "$DEVICE" ]]; then
        echo "▸ Boot simulator: $DEVICE"
        xcrun simctl boot "$DEVICE" 2>/dev/null || true   # đã boot rồi thì simctl báo lỗi, bỏ qua
        TARGET="$DEVICE"
    elif xcrun simctl list devices | grep -q "(Booted)"; then
        TARGET="booted"                                   # tái dùng simulator đang chạy
    else
        # Không có simulator nào chạy → tự chọn iPhone khả dụng đầu tiên rồi boot (khỏi mở tay).
        TARGET=$(xcrun simctl list devices available | awk -F'[()]' '/iPhone/ {print $2; exit}')
        if [[ -z "$TARGET" ]]; then
            echo "Không thấy simulator iPhone khả dụng — cài runtime iOS trong Xcode, hoặc --device 'iPhone 17 Pro'." >&2
            exit 1
        fi
        echo "▸ Không có simulator đang chạy → tự boot iPhone khả dụng đầu tiên ($TARGET)"
        xcrun simctl boot "$TARGET" 2>/dev/null || true
    fi

    open -a Simulator                                     # mở UI Simulator
    xcrun simctl bootstatus "$TARGET" -b >/dev/null 2>&1 || true   # chờ boot xong nếu vừa boot

    BUNDLE_ID=$(/usr/libexec/PlistBuddy -c "Print :CFBundleIdentifier" "$APP/Info.plist")
    echo "▸ Cài $BUNDLE_ID"
    xcrun simctl install "$TARGET" "$APP"
    xcrun simctl launch "$TARGET" "$BUNDLE_ID"
    echo "✓ Đã mở app trên simulator."
fi
