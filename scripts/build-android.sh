#!/usr/bin/env bash
#
# Script DUY NHẤT cho SDK Android: build vòng lặp dev, hoặc phát hành lên Artifactory.
#
# Chạy không tham số thì nó hỏi chọn chế độ:
#
#   ./scripts/build-android.sh
#     1) local    publish SDK vào ~/.m2 → build app demo
#     2) publish  đẩy SDK lên Artifactory (mặc định Viettelmoney)
#
# Chọn thẳng khỏi hỏi:
#
#   ./scripts/build-android.sh local                    # publish ~/.m2 → build APK debug
#   ./scripts/build-android.sh local --run              # build → cài → mở app trên máy đang cắm
#   ./scripts/build-android.sh local --skip-app         # chỉ publish vào ~/.m2
#   ./scripts/build-android.sh publish                  # hỏi 2 version rồi đẩy lên Viettelmoney
#   ./scripts/build-android.sh publish -v 1.2.0 -l 1.0.0 --yes    # cho CI, không hỏi gì
#   ./scripts/build-android.sh publish --target local   # publish thử vào ~/.m2 (không build app)
#
# Tuỳ chọn chung:
#   -v, --version X         version của `promotion`     (mặc định: SDK_VERSION trong gradle.properties)
#   -l, --logic-version X   version của `promotionLogic` (mặc định: LOGIC_VERSION)
#       --clean             dọn build cũ trước
#   -h, --help              in phần này
#
# Riêng `local`:   --skip-app, --install, --run
# Riêng `publish`: --target viettelmoney|artifactory|local, --yes, --write, --dry-run
#
# HAI version ĐỘC LẬP, mỗi module một số. Chế độ `local` KHÔNG hỏi (vòng lặp dev chạy mấy chục lần
# một ngày); chế độ `publish` hỏi từng số, Enter suông là giữ nguyên. Cả hai module LUÔN publish
# cùng lượt: `promotion` trỏ LOGIC_VERSION trong metadata, đẩy lệch một bên là host resolve ra bản
# lõi không tồn tại.
#
# App demo lấy SDK từ đâu là do field `useMavenLocal` trong gradle.properties (true = ~/.m2,
# false = Artifactory) — chế độ `local` tự truyền `-PuseMavenLocal=true` nên chạy đúng bất kể
# giá trị đang khai trong file.
#
set -euo pipefail

cd "$(dirname "$0")/.."   # luôn chạy từ gốc repo, gọi script từ đâu cũng được

MODE=""
TARGET="viettelmoney"
SDK_VERSION=""
LOGIC_VERSION=""
DO_CLEAN=false
SKIP_APP=false
DO_INSTALL=false
DO_RUN=false
ASSUME_YES=false
DO_WRITE=false
DRY_RUN=false

APP_ID="com.ttcn.promotionsdk.app"
LAUNCH_ACTIVITY="$APP_ID/.MainActivity"
VIETTELMONEY_URL="https://mobile-data.viettelmoney.vn/artifactory/gradle-viettelmoney"

usage() { sed -n '3,36p' "$0" | sed 's/^# \{0,1\}//'; }

# ─── Tham số ─────────────────────────────────────────────────────────────────────────────────
# Chế độ là tham số vị trí ĐẦU TIÊN. Bắt riêng trước vòng lặp để `local`/`publish` không bị nhầm
# thành giá trị của cờ đứng trước nó.

if [[ $# -gt 0 && "$1" != -* ]]; then
    MODE="$1"; shift
fi

while [[ $# -gt 0 ]]; do
    case "$1" in
        -v|--version)       SDK_VERSION="${2:-}"; shift 2 ;;
        -l|--logic-version) LOGIC_VERSION="${2:-}"; shift 2 ;;
        -t|--target)        TARGET="${2:-}"; shift 2 ;;
        --clean)            DO_CLEAN=true; shift ;;
        --skip-app)         SKIP_APP=true; shift ;;
        --install)          DO_INSTALL=true; shift ;;
        --run)              DO_RUN=true; shift ;;   # --run bao gồm cả --install
        -y|--yes)           ASSUME_YES=true; shift ;;
        --write)            DO_WRITE=true; shift ;;
        --dry-run)          DRY_RUN=true; shift ;;
        -h|--help)          usage; exit 0 ;;
        *)                  echo "Tham số lạ: $1 (xem --help)" >&2; exit 1 ;;
    esac
done

[[ "$DO_RUN" == true ]] && DO_INSTALL=true

# ─── Chọn chế độ ─────────────────────────────────────────────────────────────────────────────

if [[ -z "$MODE" ]]; then
    if [[ ! -t 0 ]]; then
        echo "Không có TTY để hỏi — chọn thẳng: $0 local   hoặc   $0 publish" >&2
        exit 1
    fi
    echo "Chế độ:"
    echo "  1) local    publish SDK vào ~/.m2 → build app demo"
    echo "  2) publish  đẩy SDK lên Artifactory ($TARGET)"
    printf 'Chọn [1]: '
    read -r answer
    case "${answer:-1}" in
        1|local)   MODE="local" ;;
        2|publish) MODE="publish" ;;
        *) echo "Chọn 1 hoặc 2 (nhận được: '$answer')" >&2; exit 1 ;;
    esac
    echo
fi

case "$MODE" in
    local|publish) ;;
    *) echo "Chế độ phải là: local | publish (nhận được: '$MODE')" >&2; exit 1 ;;
esac

if [[ "$MODE" == "publish" ]]; then
    case "$TARGET" in
        viettelmoney|artifactory|local) ;;
        *) echo "--target phải là: viettelmoney | artifactory | local (nhận được: '$TARGET')" >&2; exit 1 ;;
    esac
fi

# ─── Toạ độ & version ────────────────────────────────────────────────────────────────────────

read_gradle_property()  { grep -E "^$1=" gradle.properties | head -1 | cut -d= -f2-; }
read_local_property()   { grep -E "^$1=" local.properties 2>/dev/null | head -1 | cut -d= -f2-; }

SDK_GROUP="$(read_gradle_property 'SDK_GROUP')";           SDK_GROUP="${SDK_GROUP:-vn.viettelpay.library}"
CURRENT_SDK_VERSION="$(read_gradle_property 'SDK_VERSION')";     CURRENT_SDK_VERSION="${CURRENT_SDK_VERSION:-1.0.0}"
CURRENT_LOGIC_VERSION="$(read_gradle_property 'LOGIC_VERSION')"; CURRENT_LOGIC_VERSION="${CURRENT_LOGIC_VERSION:-1.0.0}"

# Chỉ chế độ `publish` mới hỏi version — phát hành thì con số là quyết định, hỏi mới đáng. Vòng lặp
# dev thì không: lấy thẳng số trong gradle.properties, hai cờ -v/-l để override khi cần.
if [[ "$MODE" == "publish" && "$ASSUME_YES" != true ]] \
   && [[ -z "$SDK_VERSION" || -z "$LOGIC_VERSION" ]]; then
    if [[ ! -t 0 ]]; then
        echo "Không có TTY để hỏi version — truyền thẳng: $0 publish --version x.y.z --logic-version a.b.c" >&2
        exit 1
    fi
    echo "Version cần publish (Enter = giữ nguyên):"
    if [[ -z "$SDK_VERSION" ]]; then
        printf '  %-14s [%s]: ' 'promotion' "$CURRENT_SDK_VERSION"; read -r answer
        SDK_VERSION="${answer:-$CURRENT_SDK_VERSION}"
    fi
    if [[ -z "$LOGIC_VERSION" ]]; then
        printf '  %-14s [%s]: ' 'promotionLogic' "$CURRENT_LOGIC_VERSION"; read -r answer
        LOGIC_VERSION="${answer:-$CURRENT_LOGIC_VERSION}"
    fi
    echo
fi

SDK_VERSION="${SDK_VERSION:-$CURRENT_SDK_VERSION}"
LOGIC_VERSION="${LOGIC_VERSION:-$CURRENT_LOGIC_VERSION}"

# x.y.z, cho phép hậu tố -SNAPSHOT / -rc1 / .1. Chặn ở đây vì version sai định dạng vẫn publish được
# (Maven không kén), chỉ vỡ ra sau — lúc host resolve không thấy, hoặc thấy sai thứ tự version.
check_version_format() {   # $1 = nhãn, $2 = version
    if ! printf '%s' "$2" | grep -qE '^[0-9]+\.[0-9]+\.[0-9]+([.-][A-Za-z0-9]+)*$'; then
        echo "Version $1 '$2' không đúng định dạng x.y.z (hậu tố -SNAPSHOT/-rc1 thì được)." >&2
        exit 1
    fi
}
check_version_format 'promotion' "$SDK_VERSION"
check_version_format 'promotionLogic' "$LOGIC_VERSION"

GRADLE_ARGS=("-PSDK_VERSION=$SDK_VERSION" "-PLOGIC_VERSION=$LOGIC_VERSION")
gradle() { ./gradlew "$@" "${GRADLE_ARGS[@]}"; }

# ─── Điều kiện cần: Android SDK ──────────────────────────────────────────────────────────────
# Gradle tự báo thiếu JDK, nhưng thiếu Android SDK thì thông báo khó hiểu — chặn sớm cho rõ.

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

# ═══ CHẾ ĐỘ: local ═══════════════════════════════════════════════════════════════════════════

if [[ "$MODE" == "local" ]]; then
    [[ "$DO_CLEAN" == true ]] && { echo "▸ Dọn build cũ"; gradle clean; }

    # Bước dễ quên nhất: sửa SDK xong mà không publish thì app vẫn build với bản cũ trong ~/.m2 —
    # im lặng, không cảnh báo. Script ép đúng thứ tự publish → build.
    echo "▸ Publish vào ~/.m2: promotion $SDK_VERSION + promotionLogic $LOGIC_VERSION"
    gradle :promotionLogic:publishToMavenLocal :AndroidPromotionSDK:publishToMavenLocal

    if [[ "$SKIP_APP" == true ]]; then
        echo "✓ Xong. SDK nằm ở ~/.m2/repository/${SDK_GROUP//.//}/"
        exit 0
    fi

    # `-PuseMavenLocal=true` truyền tường minh: script chạy đúng kể cả khi gradle.properties đang
    # khai `false`. Thiếu cờ này thì app đi tìm trên Artifactory chứ không phải bản vừa publish.
    echo "▸ Build app demo (SDK lấy từ ~/.m2)"
    gradle :androidApp:assembleDebug -PuseMavenLocal=true

    echo "✓ Xong: androidApp/build/outputs/apk/debug/androidApp-debug.apk"

    if [[ "$DO_INSTALL" == true ]]; then
        echo "▸ Cài vào thiết bị đang cắm"
        gradle :androidApp:installDebug -PuseMavenLocal=true
        echo "✓ Đã cài."
    fi

    if [[ "$DO_RUN" == true ]]; then
        # Tìm adb: PATH → ANDROID_HOME/ANDROID_SDK_ROOT → sdk.dir trong local.properties.
        ADB=""
        if command -v adb >/dev/null 2>&1; then
            ADB=adb
        else
            sdk="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
            [[ -z "$sdk" && -f local.properties ]] && sdk="$(read_local_property 'sdk\.dir')"
            [[ -n "$sdk" && -x "$sdk/platform-tools/adb" ]] && ADB="$sdk/platform-tools/adb"
        fi
        if [[ -z "$ADB" ]]; then
            echo "Không thấy adb (PATH/ANDROID_HOME/local.properties) — app đã cài, tự mở giúp." >&2
            exit 1
        fi
        if ! "$ADB" get-state >/dev/null 2>&1; then
            echo "Chưa có thiết bị/emulator nào đang cắm. Mở emulator rồi chạy lại --run." >&2
            exit 1
        fi
        echo "▸ Mở app: $LAUNCH_ACTIVITY"
        "$ADB" shell am start -n "$LAUNCH_ACTIVITY" >/dev/null
        echo "✓ Đã mở app demo trên thiết bị."
    fi
    exit 0
fi

# ═══ CHẾ ĐỘ: publish ═════════════════════════════════════════════════════════════════════════

case "$TARGET" in
    viettelmoney)
        MAVEN_USER="$(read_local_property 'maven\.username')"
        MAVEN_PASS="$(read_local_property 'maven\.password')"
        if [[ -z "$MAVEN_USER" || -z "$MAVEN_PASS" ]]; then
            cat >&2 <<'MSG'
Thiếu credentials Artifactory Viettelmoney.

Thêm vào local.properties (KHÔNG commit, đã gitignore):
  maven.username=<user>
  maven.password=<identity token>
MSG
            exit 1
        fi
        GRADLE_TASKS=(publishSdkToViettelmoney)
        TARGET_LABEL="Artifactory Viettelmoney ($VIETTELMONEY_URL)"
        ;;
    artifactory)
        # Thiếu artifactoryUrl thì repo "artifactory" không được đăng ký ở build.gradle.kts gốc và
        # Gradle chỉ báo "Task not found" — chặn sớm cho rõ nguyên nhân.
        if [[ -z "${ARTIFACTORY_URL:-}" ]] && \
           ! grep -qE '^\s*artifactoryUrl\s*=' "${GRADLE_USER_HOME:-$HOME/.gradle}/gradle.properties" 2>/dev/null; then
            cat >&2 <<'MSG'
Chưa cấu hình repo Artifactory chung.

Thêm vào ~/.gradle/gradle.properties (KHÔNG commit):
  artifactoryUrl=https://<host>/artifactory
  artifactoryUser=<user>
  artifactoryPassword=<identity token>

Hoặc export ARTIFACTORY_URL / ARTIFACTORY_USER / ARTIFACTORY_PASSWORD.
Chi tiết: docs/android/Distribution.md §3.4
MSG
            exit 1
        fi
        GRADLE_TASKS=(
            :promotionLogic:publishAllPublicationsToArtifactoryRepository
            :AndroidPromotionSDK:publishAllPublicationsToArtifactoryRepository
        )
        TARGET_LABEL="Artifactory chung (${ARTIFACTORY_URL:-artifactoryUrl trong ~/.gradle})"
        ;;
    local)
        GRADLE_TASKS=(:promotionLogic:publishToMavenLocal :AndroidPromotionSDK:publishToMavenLocal)
        TARGET_LABEL="~/.m2/repository"
        ;;
esac

# Repo release của Artifactory thường bật "immutable": đẩy đè version cũ bị từ chối GIỮA CHỪNG, sau
# khi đã build xong. Hỏi trước cho đỡ mất công — chỉ kiểm được với viettelmoney vì URL cố định; lỗi
# mạng / thiếu curl thì bỏ qua, không chặn phát hành.
if [[ "$TARGET" == "viettelmoney" && "$SDK_VERSION" != *SNAPSHOT ]] && command -v curl >/dev/null 2>&1; then
    probe_url="$VIETTELMONEY_URL/${SDK_GROUP//.//}/promotion/$SDK_VERSION/promotion-$SDK_VERSION.pom"
    http_code="$(curl -s -o /dev/null -w '%{http_code}' -u "$MAVEN_USER:$MAVEN_PASS" \
        --max-time 15 -I "$probe_url" 2>/dev/null || echo "000")"
    if [[ "$http_code" == "200" ]]; then
        echo "⚠ Version $SDK_VERSION ĐÃ có trên repo. Repo release thường không cho ghi đè — nhiều khả năng publish sẽ bị từ chối." >&2
        ASSUME_YES=false   # trường hợp này bắt buộc phải có người xác nhận
    fi
fi

echo
echo "  Đích           : $TARGET_LABEL"
echo "  promotion      : $SDK_VERSION" "$([[ "$SDK_VERSION" != "$CURRENT_SDK_VERSION" ]] && echo "(gradle.properties đang là $CURRENT_SDK_VERSION)")"
echo "  promotionLogic : $LOGIC_VERSION" "$([[ "$LOGIC_VERSION" != "$CURRENT_LOGIC_VERSION" ]] && echo "(gradle.properties đang là $CURRENT_LOGIC_VERSION)")"
echo "  Gradle         : ./gradlew ${GRADLE_TASKS[*]} ${GRADLE_ARGS[*]}"
echo

if [[ "$DRY_RUN" == true ]]; then
    echo "(--dry-run) Dừng ở đây, không chạy gì."
    exit 0
fi

if [[ "$ASSUME_YES" != true ]]; then
    if [[ ! -t 0 ]]; then
        echo "Không có TTY để xác nhận — thêm --yes nếu chắc chắn." >&2
        exit 1
    fi
    printf 'Publish? [y/N]: '
    read -r answer
    [[ "$answer" == "y" || "$answer" == "Y" ]] || { echo "Đã huỷ."; exit 0; }
fi

[[ "$DO_CLEAN" == true ]] && { echo "▸ Dọn build cũ"; gradle clean; }

echo "▸ Build + publish promotion $SDK_VERSION + promotionLogic $LOGIC_VERSION → $TARGET"
gradle "${GRADLE_TASKS[@]}"

# Mặc định KHÔNG ghi lại vào gradle.properties: publish thử một bản -SNAPSHOT/-rc không nên làm bẩn
# nguồn version tập trung. Ghi rồi thì nhớ đồng bộ tay MARKETING_VERSION bên iOS.
if [[ "$DO_WRITE" == true ]]; then
    write_property() {   # $1 = key, $2 = giá trị mới, $3 = giá trị cũ
        [[ "$2" == "$3" ]] && return 0
        tmp="$(mktemp)"
        sed "s/^$1=.*/$1=$2/" gradle.properties > "$tmp"
        mv "$tmp" gradle.properties
        echo "▸ Đã ghi $1=$2 vào gradle.properties"
    }
    write_property SDK_VERSION "$SDK_VERSION" "$CURRENT_SDK_VERSION"
    write_property LOGIC_VERSION "$LOGIC_VERSION" "$CURRENT_LOGIC_VERSION"
fi

echo
echo "✓ Xong. Host khai:"
echo "    implementation(\"$SDK_GROUP:promotion:$SDK_VERSION\")"
