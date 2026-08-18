#!/usr/bin/env bash
#
# Script DUY NHẤT cho SDK iOS: build vòng lặp dev, hoặc phát hành lên Artifactory.
# Đối xứng với build-android.sh — cùng hai chế độ, cùng tên cờ.
#
# Chạy không tham số thì nó hỏi chọn chế độ:
#
#   ./scripts/build-ios.sh
#     1) local    dựng Promotion.xcframework → build app demo (SDK lấy từ Artifactory)
#     2) publish  dựng Promotion.xcframework → đẩy lên Artifactory
#
# Chọn thẳng khỏi hỏi:
#
#   ./scripts/build-ios.sh local                    # dựng xcframework → build app demo
#   ./scripts/build-ios.sh local --run              # build → cài → mở trên simulator
#   ./scripts/build-ios.sh local --skip-app         # chỉ dựng Promotion.xcframework
#   ./scripts/build-ios.sh publish                  # hỏi version rồi đẩy lên Viettelmoney
#   ./scripts/build-ios.sh publish -v 1.2.0 --yes   # cho CI, không hỏi gì
#   ./scripts/build-ios.sh publish --dry-run        # dựng + kiểm tra, không gửi gì lên
#
# Tuỳ chọn chung:
#   -v, --version X     version của SDK (mặc định: SDK_VERSION trong gradle.properties)
#       --clean         xoá DerivedData của app demo trước
#       --skip-app      bỏ qua bước build app demo
#   -h, --help          in phần này
#
# Riêng `local`:   --run, --device 'iPhone 17 Pro'
# Riêng `publish`: --yes, --dry-run
#
#   -f, --force         ĐÈ thẳng bản đã có trên Artifactory, khỏi hỏi. Xem mục "Build đè" bên dưới.
#
# Chế độ đặt ở đâu cũng được: `publish --force` hay `--force publish` đều nhận.
#
# ── Dọn cache ────────────────────────────────────────────────────────────────────────────────
# `publish` LUÔN dọn cache SPM của đúng version vừa đẩy, không cần cờ gì. `local --force` cũng dọn,
# dùng khi nghi máy mình đang giữ bản cũ.
#
# ── Build đè ─────────────────────────────────────────────────────────────────────────────────
# Script KHÔNG bao giờ tự đè: bản đã phát hành là thứ người khác đang build theo.
#
# Chạy tay (có TTY, không `--yes`) mà version đã tồn tại thì nó HỎI "Ghi đè bản đang có? [y/N]" —
# trả lời `y` là đúng bằng `--force`. Hỏi ngay lúc probe, trước khi build, nên trả lời "không" cũng
# chưa mất phút nào.
#
# Không có ai để trả lời (CI, pipe) hoặc đang `--yes` thì vẫn CHẶN, phải `--force` tường minh:
# `--yes` là "khỏi hỏi lại những gì tôi đã quyết", không phải đồng ý sẵn cả việc chưa từng được hỏi.
#
# Cache thì đã được dọn sẵn ở mọi lần publish — cần thế vì SPM có HAI tầng,
# cả hai đều đánh key theo URL:
#
#   1. <DerivedData>/SourcePackages/artifacts/…            xcframework đã giải nén
#   2. ~/Library/Caches/org.swift.swiftpm/artifacts/<url>  file zip, DÙNG CHUNG mọi project trên máy
#
# Tầng 2 mới là cái bẫy: tên thư mục sinh từ URL, KHÔNG có checksum trong đó. Đè zip mà giữ nguyên
# URL thì xoá DerivedData cũng vô ích — SPM lấy lại đúng zip cũ từ tầng 2. Triệu chứng dễ lạc hướng:
#
#     error: checksum of downloaded artifact ... does not match checksum specified by the manifest
#
# ("downloaded" là sai — dòng log ngay trên ghi rõ "Fetching binary artifact ... from cache".)
#
# Chỉ dọn được máy CHẠY LỆNH. Máy đồng nghiệp và CI đã kéo bản cũ về thì vẫn giữ nó.
#
# Chuỗi phụ thuộc (phải đúng thứ tự, script này ép sẵn):
#
#   :promotionLogic (Kotlin)  ──gradle──▶  PromotionLogic.xcframework   (lõi, static)
#            └─ link tĩnh vào ──▶  Promotion.xcframework           (UI, Swift)
#                                          └─ iosApp link + embed
#
# Khác Android: iOS **không** đi qua Maven. Host tải thẳng một zip từ repo generic
# `vdo-ios-frameworks` (docs/ios/Distribution.md §4).
#
# App demo KHÔNG còn là cổng kiểm tra trước khi publish — và không thể là, kể từ khi nó chuyển sang
# lấy SDK từ Artifactory (iosApp/PromotionRemote/Package.swift, docs/ios/Distribution.md §5).
#
# Nó nay build theo `url:` + `checksum:` của bản ĐÃ PHÁT HÀNH, nên build nó trước khi đẩy chỉ chứng
# minh bản CŨ trên server còn dùng được — không nói gì về gói vừa dựng. Giữ lại cổng đó thì tệ hơn là
# bỏ: xanh một cách vô nghĩa, đúng loại tín hiệu giả làm người ta tin nhầm.
#
# Muốn xác minh gói vừa đẩy, thứ tự đúng là NGƯỢC LẠI — publish xong mới verify được:
#   1. ./scripts/build-ios.sh publish -v <version>
#   2. cập nhật url: + checksum: trong iosApp/PromotionRemote/Package.swift  (script in sẵn ra)
#   3. ./scripts/build-ios.sh local        ← lúc này app demo mới thật sự kiểm bản vừa đẩy
#
set -euo pipefail

cd "$(dirname "$0")/.."   # luôn chạy từ gốc repo, gọi script từ đâu cũng được

MODE=""
SDK_VERSION=""
SKIP_APP=false
DO_CLEAN=false
DO_RUN=false
DEVICE=""
ASSUME_YES=false
DRY_RUN=false
FORCE=false
OVERWRITING=false   # bật khi probe thấy version đã tồn tại và --force cho đè

DERIVED="$PWD/iosApp/build/DerivedData"
APP_NAME="iosApp.app"
XCFRAMEWORK="iosPromotionSDK/build/Promotion.xcframework"
ARTIFACTORY_DIR="https://mobile-data.viettelmoney.vn/artifactory/api/storage/vdo-ios-frameworks/Martech/Promotion"

usage() { sed -n '3,43p' "$0" | sed 's/^# \{0,1\}//'; }

# ─── Tham số ─────────────────────────────────────────────────────────────────────────────────
# Chế độ là tham số vị trí ĐẦU TIÊN. Bắt riêng trước vòng lặp để `local`/`publish` không bị nhầm
# thành giá trị của cờ đứng trước nó.

if [[ $# -gt 0 && "$1" != -* ]]; then
    MODE="$1"; shift
fi

while [[ $# -gt 0 ]]; do
    case "$1" in
        -v|--version) SDK_VERSION="${2:-}"; shift 2 ;;
        --skip-app)   SKIP_APP=true; shift ;;
        --clean)      DO_CLEAN=true; shift ;;
        --run)        DO_RUN=true; shift ;;
        --device)     DEVICE="${2:-}"; shift 2 ;;
        -y|--yes)     ASSUME_YES=true; shift ;;
        --dry-run)    DRY_RUN=true; shift ;;
        -f|--force)   FORCE=true; shift ;;
        -h|--help)    usage; exit 0 ;;
        # Chế độ đứng sau cờ vẫn nhận (`--force publish`). Bản đầu chỉ đọc chế độ ở vị trí thứ nhất,
        # nên `--force publish` chết với "Tham số lạ: publish" — thông báo không hề nói ra vấn đề thật.
        local|publish)
            if [[ -n "$MODE" && "$MODE" != "$1" ]]; then
                echo "Đã chọn chế độ '$MODE' rồi, không nhận thêm '$1'." >&2; exit 1
            fi
            MODE="$1"; shift ;;
        *)            echo "Tham số lạ: $1 (xem --help)" >&2; exit 1 ;;
    esac
done

if [[ -z "$MODE" ]]; then
    if [[ ! -t 0 ]]; then
        echo "Không có TTY để hỏi — chọn thẳng: $0 local   hoặc   $0 publish" >&2
        exit 1
    fi
    echo "Chế độ:"
    echo "  1) local    dựng Promotion.xcframework → build app demo (SDK lấy từ Artifactory)"
    echo "  2) publish  dựng Promotion.xcframework → đẩy lên Artifactory"
    printf 'Chọn [1/2]: '
    read -r choice
    case "$choice" in
        1|local)   MODE="local" ;;
        2|publish) MODE="publish" ;;
        *) echo "Chọn 1 hoặc 2." >&2; exit 1 ;;
    esac
fi

case "$MODE" in
    local|publish) ;;
    *) echo "Chế độ phải là: local | publish (nhận được: '$MODE')" >&2; exit 1 ;;
esac

# ─── Điều kiện cần ───────────────────────────────────────────────────────────────────────────
# Build iOS chỉ chạy trên macOS có Xcode (không phải Command Line Tools trần).

if ! command -v xcodebuild >/dev/null 2>&1; then
    echo "Không thấy xcodebuild — cần macOS + Xcode. (Android thì dùng scripts/build-android.sh)" >&2
    exit 1
fi

# ─── Version ─────────────────────────────────────────────────────────────────────────────────
# Nguồn mặc định là `SDK_VERSION` trong gradle.properties — CÙNG một con số với Android, đúng ý
# "iOS đồng bộ tay MARKETING_VERSION, giữ trùng với SDK_VERSION" ghi ở đó.

read_gradle_property() {   # $1 = key
    grep -E "^\s*$1\s*=" gradle.properties 2>/dev/null | tail -1 | cut -d= -f2- | tr -d ' \r'
}

CURRENT_SDK_VERSION="$(read_gradle_property 'SDK_VERSION')"
CURRENT_SDK_VERSION="${CURRENT_SDK_VERSION:-1.0.0}"

# Chế độ `local` KHÔNG hỏi version: vòng lặp dev chạy mấy chục lần một ngày, hỏi là phiền.
# Chế độ `publish` hỏi, Enter suông là giữ nguyên — phát hành thì con số là quyết định.
if [[ "$MODE" == "publish" && -z "$SDK_VERSION" && "$ASSUME_YES" != true && -t 0 ]]; then
    printf 'Version cần publish [%s]: ' "$CURRENT_SDK_VERSION"
    read -r answer
    SDK_VERSION="${answer:-$CURRENT_SDK_VERSION}"
fi
SDK_VERSION="${SDK_VERSION:-$CURRENT_SDK_VERSION}"

# x.y.z, cho phép hậu tố -rc1 / .1. Chặn ở đây vì version sai định dạng vẫn đẩy lên được (repo
# generic không kén), chỉ vỡ ra sau — lúc host không resolve nổi, hoặc thấy sai thứ tự version.
if ! printf '%s' "$SDK_VERSION" | grep -qE '^[0-9]+\.[0-9]+\.[0-9]+([.-][A-Za-z0-9]+)*$'; then
    echo "Version '$SDK_VERSION' không đúng định dạng x.y.z (hậu tố -rc1 thì được)." >&2
    exit 1
fi

export SDK_VERSION   # build-xcframework.sh đọc qua biến môi trường

# ─── Dọn cache SPM của một version ───────────────────────────────────────────────────────────
# Xoá đúng version đang thao tác, không quét sạch cache: tầng 2 dùng chung cho MỌI project trên máy
# (đang giữ sẵn Firebase, VDOMiniApp, CEP, VDONetwork… tổng vài trăm MB) — xoá cả cụm là bắt máy
# tải lại hết những thứ chẳng liên quan.
purge_spm_cache() {
    local version="$1"
    local removed=0 target

    # Tầng 1: artifact đã giải nén trong DerivedData của app demo.
    target="$DERIVED/SourcePackages"
    if [[ -d "$target" ]]; then
        rm -rf "$target"
        echo "  ✗ $target"
        removed=$((removed + 1))
    fi

    # Tầng 2: zip trong cache dùng chung. SwiftPM đặt tên thư mục bằng cách thay mọi ký tự không
    # phải chữ/số trong URL thành `_`, nên version 1.2.3 nằm trong tên dưới dạng `1_2_3`. Khớp bằng
    # glob thay vì dựng lại nguyên chuỗi — dựng tay là thứ sẽ lệch âm thầm khi SwiftPM đổi quy tắc.
    local version_key="${version//./_}"
    for target in "$HOME/Library/Caches/org.swift.swiftpm/artifacts"/*Martech_Promotion_"$version_key"_*; do
        if [[ -e "$target" ]]; then
            rm -rf "$target"
            echo "  ✗ $target"
            removed=$((removed + 1))
        fi
    done

    [[ "$removed" -eq 0 ]] && echo "  (cache không có bản cũ nào của version $version)"
}

# ─── Chế độ publish: hỏi TRƯỚC khi build ─────────────────────────────────────────────────────
# Dựng xcframework mất vài phút. Version đã tồn tại thì phát hiện ngay bây giờ, đừng để người ta
# ngồi chờ build xong mới báo không đẩy được — cùng lý do Android probe file .pom trước khi build.

if [[ "$MODE" == "publish" ]]; then
    if command -v curl >/dev/null 2>&1; then
        MAVEN_USER="$(grep '^maven.username=' local.properties 2>/dev/null | cut -d= -f2-)"
        MAVEN_PASS="$(grep '^maven.password=' local.properties 2>/dev/null | cut -d= -f2-)"
        if [[ -n "$MAVEN_USER" && -n "$MAVEN_PASS" ]]; then
            http_code="$(curl -s -o /dev/null -w '%{http_code}' -u "$MAVEN_USER:$MAVEN_PASS" \
                --max-time 15 "$ARTIFACTORY_DIR/$SDK_VERSION" 2>/dev/null || echo "000")"
            if [[ "$http_code" == "200" ]]; then
                if [[ "$FORCE" == true ]]; then
                    # Đè là việc một chiều — phải nói ra, không được lặng lẽ làm.
                    OVERWRITING=true
                    echo "⚠ Version $SDK_VERSION ĐÃ có trên Artifactory — ĐÈ theo --force. Repo release bật immutable thì server vẫn từ chối." >&2
                elif [[ "$ASSUME_YES" != true && -t 0 ]]; then
                    # Có người ngồi trước máy thì HỎI — đối xứng build-android.sh. Probe chạy TRƯỚC
                    # khi build nên trả lời "không" cũng chưa mất phút nào.
                    #
                    # Trả "y" thì bật luôn $FORCE chứ không chỉ $OVERWRITING: cờ này còn được chuyển
                    # xuống publish-xcframework.sh, mà bên đó có chốt "đã tồn tại" RIÊNG — thiếu cờ
                    # là build xong vài phút mới chết ở bước cuối.
                    echo "⚠ Version $SDK_VERSION ĐÃ có trên Artifactory — bản đã phát hành là thứ người khác đang build theo." >&2
                    echo "  Repo release thường bật immutable: đè có thể vẫn bị server từ chối giữa chừng." >&2
                    printf 'Ghi đè bản đang có? [y/N]: '
                    read -r overwrite_answer
                    if [[ "$overwrite_answer" == "y" || "$overwrite_answer" == "Y" ]]; then
                        FORCE=true
                        OVERWRITING=true
                    else
                        echo "Đã huỷ. Tăng version rồi chạy lại."
                        exit 0
                    fi
                else
                    # Không có ai để trả lời (CI, pipe), hoặc đang --yes: KHÔNG tự đè.
                    # `--yes` nghĩa là "khỏi hỏi lại những gì tôi đã quyết", không phải "đồng ý sẵn
                    # cả việc chưa từng được hỏi". Muốn CI đè thì phải viết --force ra tường minh.
                    echo "❌ Version $SDK_VERSION ĐÃ có trên Artifactory — bản đã phát hành là thứ người khác" >&2
                    echo "   đang build theo. Tăng version, hoặc --force nếu thực sự muốn ghi đè." >&2
                    exit 1
                fi
            fi
        fi
    fi

    echo
    echo "  Đích       : Artifactory vdo-ios-frameworks/Martech/Promotion/$SDK_VERSION" "$([[ "$OVERWRITING" == true ]] && echo '← GHI ĐÈ bản đang có')"
    echo "  Version    : $SDK_VERSION" "$([[ "$SDK_VERSION" != "$CURRENT_SDK_VERSION" ]] && echo "(gradle.properties đang là $CURRENT_SDK_VERSION)")"
    echo "  Xác minh   : sau khi đẩy (app demo lấy SDK từ Artifactory, xem chú thích đầu file)"
    echo

    if [[ "$DRY_RUN" != true && "$ASSUME_YES" != true ]]; then
        if [[ ! -t 0 ]]; then
            echo "Không có TTY để xác nhận — thêm --yes nếu chắc chắn." >&2
            exit 1
        fi
        printf 'Publish? [y/N]: '
        read -r answer
        [[ "$answer" == "y" || "$answer" == "Y" ]] || { echo "Đã huỷ."; exit 0; }
    fi
fi

if [[ "$DO_CLEAN" == true ]]; then
    echo "▸ Dọn DerivedData của app demo"
    rm -rf "$DERIVED"
fi

# Chế độ `local --force`: ép app demo kéo lại zip từ Artifactory thay vì dùng bản trong cache.
# Dùng khi nghi ngờ bản trên server đã bị đè mà máy mình còn giữ bản cũ.
if [[ "$MODE" == "local" && "$FORCE" == true ]]; then
    echo "▸ Dọn cache SPM của version $SDK_VERSION"
    purge_spm_cache "$SDK_VERSION"
fi

# ─── 1. Dựng Promotion.xcframework ──────────────────────────────────────────────────────
# Script này tự gọi Gradle dựng PromotionLogic.xcframework rồi copy vào iosPromotionSDK/Frameworks/
# TRƯỚC khi archive Swift — sửa Kotlin xong mà dùng header cũ thì lỗi hiện ra tận SwiftCompile
# ("cannot find ... in scope"), rất khó lần.

echo "▸ Dựng Promotion.xcframework $SDK_VERSION (kèm lõi PromotionLogic từ Gradle)"
./iosPromotionSDK/scripts/build-xcframework.sh

if [[ ! -d "$XCFRAMEWORK" ]]; then
    echo "Không thấy $XCFRAMEWORK sau khi build." >&2
    exit 1
fi

# ─── 2. Build app demo ───────────────────────────────────────────────────────────────────────
# CHỈ chế độ `local`. App demo lấy SDK từ Artifactory theo url+checksum đã ghim trong
# iosApp/PromotionRemote/Package.swift, nên ở chế độ `publish` nó không kiểm được gói vừa dựng —
# lý do đầy đủ ở đầu file.

if [[ "$SKIP_APP" == true || "$MODE" == "publish" ]]; then
    echo "✓ Xong: $XCFRAMEWORK"
    [[ "$MODE" == "local" ]] && exit 0
else
    # Dùng `-scheme` (không phải `-target`): `-derivedDataPath` bắt buộc đi kèm scheme. Scheme `iosApp`
    # đã được **shared** (xcshareddata/xcschemes) nên máy khác clone về là có ngay — để trong xcuserdata
    # thì chỉ máy của người tạo mới thấy.
    echo "▸ Build app demo (simulator)"
    if ! xcodebuild build \
        -project iosApp/iosApp.xcodeproj \
        -scheme iosApp \
        -configuration Debug \
        -sdk iphonesimulator \
        -derivedDataPath "$DERIVED" \
        CODE_SIGNING_ALLOWED=NO \
        -quiet
    then
        echo "❌ App demo không build được với xcframework vừa dựng." >&2
        [[ "$MODE" == "publish" ]] && echo "   Dừng trước khi đẩy — version $SDK_VERSION chưa lên Artifactory." >&2
        exit 1
    fi

    APP="$DERIVED/Build/Products/Debug-iphonesimulator/$APP_NAME"
    echo "✓ Xong: $APP"
fi

# ─── 3. Cài + mở trên simulator (chỉ chế độ local) ───────────────────────────────────────────

if [[ "$MODE" == "local" && "$DO_RUN" == true ]]; then
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

[[ "$MODE" == "local" ]] && exit 0

# ─── 4. Đẩy lên Artifactory ──────────────────────────────────────────────────────────────────
# Việc upload nằm gọn trong publish-xcframework.sh (tạo package version → PUT zip → metadata.json).
# Ở đây chỉ chuyển cờ xuống, không lặp lại logic.

PUBLISH_ARGS=()
[[ "$DRY_RUN" == true ]] && PUBLISH_ARGS+=(--dry-run)
[[ "$FORCE"   == true ]] && PUBLISH_ARGS+=(--force)

echo "▸ Đẩy lên Artifactory"
# `"${PUBLISH_ARGS[@]}"` trần sẽ chết ở bash 3.2 (macOS) khi mảng RỖNG: `set -u` coi đó là unbound
# variable. Dạng `${A[@]+"${A[@]}"}` là cách viết an toàn cho cả bash 3.2 lẫn bản mới.
./iosPromotionSDK/scripts/publish-xcframework.sh ${PUBLISH_ARGS[@]+"${PUBLISH_ARGS[@]}"}

[[ "$DRY_RUN" == true ]] && exit 0

# Publish xong là dọn cache của ĐÚNG version vừa đẩy — luôn luôn, không chờ --force.
#
# Version mới thì cache chưa có gì, dọn chỉ tốn một lần quét thư mục. Nhưng khi đẩy đè lên một
# version đã có, cache của chính máy này đang giữ zip CŨ dưới đúng URL vừa đè — không dọn thì lần
# build sau hoặc chạy nhầm binary cũ, hoặc báo "checksum does not match" mà chẳng hiểu vì sao.
# Dọn vô điều kiện thì không phải nhớ mình vừa đè hay vừa tạo mới.
echo "▸ Dọn cache SPM của version $SDK_VERSION"
purge_spm_cache "$SDK_VERSION"

# App demo ghim url+checksum của bản đã phát hành, nên nó KHÔNG tự thấy bản vừa đẩy. In sẵn hai dòng
# cần dán — bắt người ta tự ghép URL rồi tự chạy shasum là kiểu việc vặt dễ làm sai trong im lặng.
ZIP_URL="https://mobile-data.viettelmoney.vn/artifactory/vdo-ios-frameworks/Martech/Promotion/$SDK_VERSION/Promotion-$SDK_VERSION.xcframework.zip"
CHECKSUM="$(shasum -a 256 iosPromotionSDK/build/Promotion.xcframework.zip | cut -d' ' -f1)"

cat <<EOF

─────────────────────────────────────────────────────────────────────────────
Muốn app demo chạy bản $SDK_VERSION vừa đẩy, sửa iosApp/PromotionRemote/Package.swift:

            url: "$ZIP_URL",
            checksum: "$CHECKSUM"

rồi:  ./scripts/build-ios.sh local
─────────────────────────────────────────────────────────────────────────────
EOF
