#!/usr/bin/env bash
#
# Build SDK Android + app demo trên một máy bất kỳ.
#
# Từ khi SDK phát hành qua Maven (docs/android/Distribution.md), `:androidApp` KHÔNG còn đọc file AAR
# trong libs/ nữa — nó khai toạ độ `$SDK_GROUP:promotionSDK`. Nghĩa là **phải publish SDK trước**,
# nếu không Gradle báo "Could not find …:promotionSDK". Script này ép đúng thứ tự đó.
#
#   ./scripts/build-android.sh                 # publish SDK → build app demo (APK debug)
#   ./scripts/build-android.sh --skip-app      # chỉ publish SDK vào ~/.m2
#   ./scripts/build-android.sh --remote        # publish LÊN Artifactory (không build app demo)
#   ./scripts/build-android.sh --clean         # dọn build cũ rồi làm lại từ đầu
#   ./scripts/build-android.sh --install       # build xong cài luôn vào máy/emulator đang cắm
#   ./scripts/build-android.sh --run           # build → cài → MỞ app trên máy/emulator đang cắm
#   ./scripts/build-android.sh --version 1.2.0 # publish số version khác (mặc định 1.0.0)
#
set -euo pipefail

cd "$(dirname "$0")/.."   # luôn chạy từ gốc repo, gọi script từ đâu cũng được

SKIP_APP=false
DO_CLEAN=false
DO_INSTALL=false
DO_RUN=false
DO_REMOTE=false
SDK_VERSION=""

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
        -h|--help)  sed -n '3,16p' "$0" | sed 's/^# \{0,1\}//'; exit 0 ;;
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

GRADLE_ARGS=()
[[ -n "$SDK_VERSION" ]] && GRADLE_ARGS+=("-PSDK_VERSION=$SDK_VERSION")

# bash 3.2 (mặc định trên macOS) coi mảng RỖNG là "unbound" dưới `set -u`, nên không thể viết
# thẳng "${GRADLE_ARGS[@]}". Hàm này bung mảng an toàn khi nó rỗng.
gradle() { ./gradlew "$@" ${GRADLE_ARGS[@]+"${GRADLE_ARGS[@]}"}; }

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

# Toạ độ SDK lấy từ gradle.properties (nguồn tập trung), chỉ để in ra cho người dùng —
# Gradle tự đọc lại các property này, không phụ thuộc hai dòng dưới.
SDK_GROUP="$(grep -E '^SDK_GROUP=' gradle.properties | head -1 | cut -d= -f2-)"
SDK_GROUP="${SDK_GROUP:-com.ttcn.promotion}"
PUBLISHED_VERSION="$SDK_VERSION"
if [[ -z "$PUBLISHED_VERSION" ]]; then
    PUBLISHED_VERSION="$(grep -E '^SDK_VERSION=' gradle.properties | head -1 | cut -d= -f2-)"
fi

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

    echo "▸ Publish SDK lên Artifactory (promotionLogic + promotionSDK)"
    gradle :promotionLogic:publishAllPublicationsToArtifactoryRepository \
           :AndroidPromotionSDK:publishAllPublicationsToArtifactoryRepository
    echo "✓ Xong. Host khai: implementation(\"$SDK_GROUP:promotionSDK:$PUBLISHED_VERSION\")"
    exit 0
fi

echo "▸ Publish SDK vào ~/.m2 (promotionLogic + promotionSDK)"
gradle :promotionLogic:publishToMavenLocal :AndroidPromotionSDK:publishToMavenLocal

if [[ "$SKIP_APP" == true ]]; then
    echo "✓ Xong. SDK đã nằm trong ~/.m2/repository/${SDK_GROUP//.//}/"
    exit 0
fi

# ─── 2. Build app demo (tiêu thụ SDK từ ~/.m2 như host thật) ──────────────────────────────────

echo "▸ Build app demo"
gradle :androidApp:assembleDebug

APK="androidApp/build/outputs/apk/debug/androidApp-debug.apk"
echo "✓ Xong: $APK"

if [[ "$DO_INSTALL" == true ]]; then
    echo "▸ Cài vào thiết bị đang cắm"
    gradle :androidApp:installDebug
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
