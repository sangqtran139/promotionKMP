#!/usr/bin/env bash
#
# Build SDK Android + app demo trên một máy bất kỳ.
#
# Từ khi SDK phát hành qua Maven (docs/android/Distribution.md), `:androidApp` KHÔNG còn đọc file AAR
# trong libs/ nữa — nó khai toạ độ `$SDK_GROUP:promotion` và mặc định kéo từ Artifactory
# Viettelmoney. Script này chạy vòng lặp DEV: publish vào ~/.m2 rồi build app với `-PuseMavenLocal=true`
# (không có cờ đó thì ~/.m2 không được đăng ký và app lấy bản trên server). Ép đúng thứ tự đó.
#
#   ./scripts/build-android.sh                 # publish SDK → build app demo (APK debug)
#   ./scripts/build-android.sh --skip-app      # chỉ publish SDK vào ~/.m2
#   ./scripts/build-android.sh --remote        # publish LÊN Artifactory (không build app demo)
#   ./scripts/build-android.sh --clean         # dọn build cũ rồi làm lại từ đầu
#   ./scripts/build-android.sh --install       # build xong cài luôn vào máy/emulator đang cắm
#   ./scripts/build-android.sh --run           # build → cài → MỞ app trên máy/emulator đang cắm
#   ./scripts/build-android.sh --version 1.2.0 # promotion ở version khác (khỏi hỏi)
#   ./scripts/build-android.sh --logic-version 2.0.0  # promotionLogic ở version khác (khỏi hỏi)
#   ./scripts/build-android.sh --yes           # không hỏi gì, lấy y nguyên gradle.properties
#
# Hai module hai version ĐỘC LẬP (SDK_VERSION / LOGIC_VERSION trong gradle.properties). Script HỎI
# từng số trước khi publish, default là số đang có trong file — Enter suông là giữ nguyên. Số đã
# truyền bằng cờ thì không hỏi lại; `--yes` (hoặc chạy không có TTY, ví dụ CI) thì bỏ qua cả hai câu.
#
# Cả hai module LUÔN publish cùng lượt, kể cả khi chỉ đổi một số: `promotion` trỏ LOGIC_VERSION
# trong metadata, đẩy lệch một bên là app resolve ra bản lõi không tồn tại.
#
set -euo pipefail

cd "$(dirname "$0")/.."   # luôn chạy từ gốc repo, gọi script từ đâu cũng được

SKIP_APP=false
DO_CLEAN=false
DO_INSTALL=false
DO_RUN=false
DO_REMOTE=false
ASSUME_YES=false
SDK_VERSION=""
LOGIC_VERSION=""

APP_ID="com.ttcn.promotionsdk.app"
LAUNCH_ACTIVITY="$APP_ID/.MainActivity"

while [[ $# -gt 0 ]]; do
    case "$1" in
        --skip-app) SKIP_APP=true; shift ;;
        --clean)    DO_CLEAN=true; shift ;;
        --install)  DO_INSTALL=true; shift ;;
        --run)      DO_RUN=true; shift ;;   # --run bao gồm cả --install
        --remote)   DO_REMOTE=true; shift ;;
        --version)  SDK_VERSION="${2:-}"; shift 2 ;;
        --logic-version) LOGIC_VERSION="${2:-}"; shift 2 ;;
        -y|--yes)   ASSUME_YES=true; shift ;;
        -h|--help)  sed -n '3,25p' "$0" | sed 's/^# \{0,1\}//'; exit 0 ;;
        *)          echo "Tham số lạ: $1 (xem --help)" >&2; exit 1 ;;
    esac
done

# --run kéo theo --install (phải cài mới mở được).
[[ "$DO_RUN" == true ]] && DO_INSTALL=true

# --remote là thao tác PHÁT HÀNH, không phải vòng lặp dev: nó đẩy artifact lên Artifactory chứ không
# bỏ gì vào ~/.m2, nên build app demo ngay sau đó sẽ kéo bản CŨ trong ~/.m2 và cho cảm giác sai.
[[ "$DO_REMOTE" == true ]] && SKIP_APP=true

# Tìm adb: PATH → ANDROID_HOME/ANDROID_SDK_ROOT → sdk.dir trong local.properties.
resolve_adb() {
    if command -v adb >/dev/null 2>&1; then echo adb; return; fi
    local sdk="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
    if [[ -z "$sdk" && -f local.properties ]]; then
        sdk="$(grep -E '^sdk\.dir=' local.properties | head -1 | cut -d= -f2-)"
    fi
    [[ -n "$sdk" && -x "$sdk/platform-tools/adb" ]] && echo "$sdk/platform-tools/adb"
}

# ─── Chọn version cho CẢ HAI module ──────────────────────────────────────────────────────────
# Toạ độ lấy từ gradle.properties (nguồn tập trung). Hai module hai số độc lập, hỏi riêng từng số.

read_gradle_property() {
    grep -E "^$1=" gradle.properties | head -1 | cut -d= -f2-
}

SDK_GROUP="$(read_gradle_property 'SDK_GROUP')"
SDK_GROUP="${SDK_GROUP:-vn.viettelpay.library}"
CURRENT_SDK_VERSION="$(read_gradle_property 'SDK_VERSION')"
CURRENT_SDK_VERSION="${CURRENT_SDK_VERSION:-1.0.0}"
CURRENT_LOGIC_VERSION="$(read_gradle_property 'LOGIC_VERSION')"
CURRENT_LOGIC_VERSION="${CURRENT_LOGIC_VERSION:-1.0.0}"

# Không có TTY (CI, chạy qua pipe) thì không hỏi được — im lặng dùng số trong gradle.properties.
[[ -t 0 ]] || ASSUME_YES=true

prompt_version() {   # $1 = nhãn, $2 = default, $3 = tên biến cần gán
    local answer
    printf '  %-14s [%s]: ' "$1" "$2"
    read -r answer
    printf -v "$3" '%s' "${answer:-$2}"
}

if [[ "$ASSUME_YES" != true ]] && [[ -z "$SDK_VERSION" || -z "$LOGIC_VERSION" ]]; then
    echo "Version cần publish (Enter = giữ nguyên):"
    [[ -z "$SDK_VERSION" ]] && prompt_version 'promotion' "$CURRENT_SDK_VERSION" SDK_VERSION
    [[ -z "$LOGIC_VERSION" ]] && prompt_version 'promotionLogic' "$CURRENT_LOGIC_VERSION" LOGIC_VERSION
    echo
fi

# Sau bước trên hai biến luôn có giá trị (trừ nhánh --yes) — điền nốt từ gradle.properties.
SDK_VERSION="${SDK_VERSION:-$CURRENT_SDK_VERSION}"
LOGIC_VERSION="${LOGIC_VERSION:-$CURRENT_LOGIC_VERSION}"

# x.y.z, cho phép hậu tố -SNAPSHOT / -rc1. Chặn ở đây vì version sai định dạng vẫn publish được
# (Maven không kén), chỉ vỡ ra sau lúc app resolve không thấy hoặc thấy sai thứ tự version.
check_version_format() {   # $1 = nhãn, $2 = version
    if ! printf '%s' "$2" | grep -qE '^[0-9]+\.[0-9]+\.[0-9]+([.-][A-Za-z0-9]+)*$'; then
        echo "Version $1 '$2' không đúng định dạng x.y.z (hậu tố -SNAPSHOT/-rc1 thì được)." >&2
        exit 1
    fi
}
check_version_format 'promotion' "$SDK_VERSION"
check_version_format 'promotionLogic' "$LOGIC_VERSION"

# Truyền cho MỌI lệnh gradle bên dưới, gồm cả bước build app demo: `:androidApp` khai
# `$SDK_GROUP:promotion:$SDK_VERSION`, thiếu cờ thì nó đi tìm số trong gradle.properties chứ
# không phải số vừa publish.
GRADLE_ARGS=("-PSDK_VERSION=$SDK_VERSION" "-PLOGIC_VERSION=$LOGIC_VERSION")

gradle() { ./gradlew "$@" "${GRADLE_ARGS[@]}"; }

# ─── Điều kiện cần: JDK + Android SDK ────────────────────────────────────────────────────────
# Gradle sẽ tự báo lỗi thiếu JDK, nhưng Android SDK thì thông báo khó hiểu — chặn sớm cho rõ.

if [[ ! -f local.properties && -z "${ANDROID_HOME:-}" && -z "${ANDROID_SDK_ROOT:-}" ]]; then
    cat >&2 <<'MSG'
Chưa thấy Android SDK.

Làm một trong hai:
  • Mở project bằng Android Studio một lần (nó tự sinh local.properties), hoặc
  • Tạo tay:  echo "sdk.dir=$HOME/Library/Android/sdk" > local.properties   (macOS)
              echo "sdk.dir=$HOME/Android/Sdk"          > local.properties   (Linux)
MSG
    exit 1
fi

if [[ "$DO_CLEAN" == true ]]; then
    echo "▸ Dọn build cũ"
    gradle clean
fi

# ─── 1. Publish SDK vào ~/.m2 ────────────────────────────────────────────────────────────────
# Bước bắt buộc, và là bước dễ quên nhất: sửa SDK xong mà không publish thì app vẫn build với bản
# cũ trong ~/.m2 — im lặng, không cảnh báo (docs/Distribution.md §5).

if [[ "$DO_REMOTE" == true ]]; then
    # Cần artifactoryUrl + credentials ở ~/.gradle/gradle.properties hoặc env ARTIFACTORY_*
    # (docs/android/Distribution.md §3.4). Thiếu URL thì repo "artifactory" không được đăng ký và
    # Gradle báo "Task ... not found" — chặn sớm cho rõ nguyên nhân.
    if [[ -z "${ARTIFACTORY_URL:-}" ]] && ! grep -qE '^\s*artifactoryUrl\s*=' "${GRADLE_USER_HOME:-$HOME/.gradle}/gradle.properties" 2>/dev/null; then
        cat >&2 <<'MSG'
Chưa cấu hình Artifactory.

Thêm vào ~/.gradle/gradle.properties (KHÔNG commit):
  artifactoryUrl=https://<host>/artifactory
  artifactoryUser=<user>
  artifactoryPassword=<identity token>

Hoặc export ARTIFACTORY_URL / ARTIFACTORY_USER / ARTIFACTORY_PASSWORD.
Chi tiết: docs/android/Distribution.md §3.4
MSG
        exit 1
    fi

    echo "▸ Publish lên Artifactory: promotion $SDK_VERSION + promotionLogic $LOGIC_VERSION"
    gradle :promotionLogic:publishAllPublicationsToArtifactoryRepository \
           :AndroidPromotionSDK:publishAllPublicationsToArtifactoryRepository
    echo "✓ Xong. Host khai: implementation(\"$SDK_GROUP:promotion:$SDK_VERSION\")"
    exit 0
fi

echo "▸ Publish vào ~/.m2: promotion $SDK_VERSION + promotionLogic $LOGIC_VERSION"
gradle :promotionLogic:publishToMavenLocal :AndroidPromotionSDK:publishToMavenLocal

if [[ "$SKIP_APP" == true ]]; then
    echo "✓ Xong. SDK đã nằm trong ~/.m2/repository/${SDK_GROUP//.//}/"
    exit 0
fi

# ─── 2. Build app demo (tiêu thụ SDK từ ~/.m2 như host thật) ──────────────────────────────────
# `-PuseMavenLocal=true` là BẮT BUỘC ở đây: settings.gradle.kts mặc định KHÔNG đăng ký ~/.m2 (để bản
# local cũ không âm thầm che bản trên Artifactory), nên thiếu cờ này app sẽ kéo bản trên server chứ
# không phải bản vừa publish ở bước 1.

echo "▸ Build app demo (SDK lấy từ ~/.m2)"
gradle :androidApp:assembleDebug -PuseMavenLocal=true

APK="androidApp/build/outputs/apk/debug/androidApp-debug.apk"
echo "✓ Xong: $APK"

if [[ "$DO_INSTALL" == true ]]; then
    echo "▸ Cài vào thiết bị đang cắm"
    gradle :androidApp:installDebug -PuseMavenLocal=true
    echo "✓ Đã cài."
fi

if [[ "$DO_RUN" == true ]]; then
    ADB="$(resolve_adb || true)"
    if [[ -z "$ADB" ]]; then
        echo "Không thấy adb (PATH/ANDROID_HOME/local.properties) — app đã cài, tự mở giúp." >&2
        exit 1
    fi
    if ! "$ADB" get-state >/dev/null 2>&1; then
        echo "Chưa có thiết bị/emulator nào đang cắm. Mở emulator (hoặc cắm máy) rồi chạy lại --run." >&2
        exit 1
    fi
    echo "▸ Mở app: $LAUNCH_ACTIVITY"
    "$ADB" shell am start -n "$LAUNCH_ACTIVITY" >/dev/null
    echo "✓ Đã mở app demo trên thiết bị."
fi
