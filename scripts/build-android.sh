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
#   ./scripts/build-android.sh publish                  # hỏi version rồi đẩy lên Viettelmoney
#   ./scripts/build-android.sh publish -v 1.2.0 --yes   # cho CI, không hỏi gì
#   ./scripts/build-android.sh publish --target local   # publish thử vào ~/.m2 (không build app)
#
# Tuỳ chọn chung:
#   -v, --version X         version cho CẢ HAI module (mặc định: SDK_VERSION trong gradle.properties)
#       --clean             dọn build cũ trước
#   -h, --help              in phần này
#
# Riêng `local`:   --skip-app, --install, --run
# Riêng `publish`: --target viettelmoney|artifactory|local, --yes, --write, --dry-run
#
#   -f, --force             ĐÈ thẳng bản đã có trên server (khỏi hỏi), và dọn cache local của
#                           đúng version đó (~/.m2 + ~/.gradle/caches). Xem mục "Build đè" bên dưới.
#
# Chế độ đặt ở đâu cũng được: `publish --force` hay `--force publish` đều nhận.
#
# ── Build đè ─────────────────────────────────────────────────────────────────────────────────
# Script KHÔNG bao giờ tự đè: bản đã phát hành là thứ người khác đang build theo.
#
# Chạy tay (có TTY, không `--yes`) mà version đã tồn tại thì nó HỎI "Ghi đè bản đang có? [y/N]" —
# trả lời `y` là đúng bằng `--force`. Hỏi ngay lúc probe, trước khi build. Không có ai để trả lời
# (CI, pipe) hoặc đang `--yes` thì CHẶN, phải `--force` tường minh. Giống hệt build-ios.sh.
#
# Chốt này chỉ có với `--target viettelmoney` và version không phải -SNAPSHOT: chỉ URL đó là cố
# định nên probe được. `--target artifactory` không probe — server tự từ chối nếu repo immutable.
#
# Đè xong thì cache local thành BẪY: Gradle đã giữ bản cũ theo đúng toạ độ group:module:version,
# nên máy bạn vẫn build với artifact cũ trong im lặng — không lỗi, không cảnh báo. Vì vậy `--force`
# luôn kèm dọn cache. Xoá ĐÚNG <module>/<version> chứ không xoá cả group: cùng group
# `vn.viettelpay.library` còn hàng chục thư viện khác đang nằm nhờ trong cache.
#
# Chỉ dọn được máy CHẠY LỆNH. Máy đồng nghiệp và CI đã kéo bản cũ về thì vẫn giữ nó.
#
# MỘT version `SDK_VERSION` cho CẢ HAI module (`promotion` + `promotionLogic`) — đối xứng iOS, nơi
# `MARKETING_VERSION` là một số cho cả gói. Trước đây tách `LOGIC_VERSION` riêng; bỏ vì hai số không
# tách được trên thực tế: `promotion` trỏ lõi trong metadata nên hai module LUÔN phải publish cùng
# lượt, đẩy lệch một bên là host resolve ra bản lõi không tồn tại.
#
# Chế độ `local` KHÔNG hỏi version (vòng lặp dev chạy mấy chục lần một ngày); `publish` hỏi một lần,
# Enter suông là giữ nguyên.
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
DO_CLEAN=false
SKIP_APP=false
DO_INSTALL=true
DO_RUN=true
ASSUME_YES=false
DO_WRITE=false
DRY_RUN=false
FORCE=false
OVERWRITING=false   # bật khi probe thấy version đã tồn tại và người dùng cho đè

APP_ID="com.ttcn.promotionsdk.app"
LAUNCH_ACTIVITY="$APP_ID/.MainActivity"
VIETTELMONEY_URL="https://mobile-data.viettelmoney.vn/artifactory/gradle-viettelmoney"

usage() { sed -n '3,31p' "$0" | sed 's/^# \{0,1\}//'; }

# ─── Tham số ─────────────────────────────────────────────────────────────────────────────────
# Chế độ là tham số vị trí ĐẦU TIÊN. Bắt riêng trước vòng lặp để `local`/`publish` không bị nhầm
# thành giá trị của cờ đứng trước nó.

if [[ $# -gt 0 && "$1" != -* ]]; then
    MODE="$1"; shift
fi

while [[ $# -gt 0 ]]; do
    case "$1" in
        -v|--version)       SDK_VERSION="${2:-}"; shift 2 ;;
        -t|--target)        TARGET="${2:-}"; shift 2 ;;
        --clean)            DO_CLEAN=true; shift ;;
        --skip-app)         SKIP_APP=true; shift ;;
        --install)          DO_INSTALL=true; shift ;;
        --run)              DO_RUN=true; shift ;;   # --run bao gồm cả --install
        -y|--yes)           ASSUME_YES=true; shift ;;
        --write)            DO_WRITE=true; shift ;;
        --dry-run)          DRY_RUN=true; shift ;;
        -f|--force)         FORCE=true; shift ;;
        # Chế độ đứng sau cờ vẫn nhận (`--force publish`). Bản đầu chỉ đọc chế độ ở vị trí thứ nhất,
        # nên `--force publish` chết với "Tham số lạ: publish" — thông báo không hề nói ra vấn đề thật.
        local|publish)
            if [[ -n "$MODE" && "$MODE" != "$1" ]]; then
                echo "Đã chọn chế độ '$MODE' rồi, không nhận thêm '$1'." >&2; exit 1
            fi
            MODE="$1"; shift ;;
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

# `|| true` KHÔNG thừa: `set -euo pipefail` ở đầu file biến "grep không tìm thấy key" (thoát 1)
# thành script CHẾT CÂM giữa chừng — không thông báo, không dòng lỗi nào. Đã dính đúng bẫy đó lúc bỏ
# `LOGIC_VERSION` khỏi gradle.properties: script tắt ngay sau khi user vừa chọn chế độ. Thiếu key
# phải ra chuỗi rỗng để nhánh `${...:-mặc định}` bên dưới lo tiếp.
read_gradle_property()  { grep -E "^$1=" gradle.properties 2>/dev/null | head -1 | cut -d= -f2- || true; }
read_local_property()   { grep -E "^$1=" local.properties  2>/dev/null | head -1 | cut -d= -f2- || true; }

SDK_GROUP="$(read_gradle_property 'SDK_GROUP')";           SDK_GROUP="${SDK_GROUP:-vn.viettelpay.library}"
CURRENT_SDK_VERSION="$(read_gradle_property 'SDK_VERSION')";     CURRENT_SDK_VERSION="${CURRENT_SDK_VERSION:-1.0.0}"

# Chỉ chế độ `publish` mới hỏi version — phát hành thì con số là quyết định, hỏi mới đáng. Vòng lặp
# dev thì không: lấy thẳng số trong gradle.properties, cờ -v để override khi cần.
if [[ "$MODE" == "publish" && "$ASSUME_YES" != true && -z "$SDK_VERSION" ]]; then
    if [[ ! -t 0 ]]; then
        echo "Không có TTY để hỏi version — truyền thẳng: $0 publish --version x.y.z" >&2
        exit 1
    fi
    echo "Version cần publish cho cả promotion + promotionLogic (Enter = giữ nguyên):"
    printf '  %-14s [%s]: ' 'SDK_VERSION' "$CURRENT_SDK_VERSION"; read -r answer
    SDK_VERSION="${answer:-$CURRENT_SDK_VERSION}"
    echo
fi

SDK_VERSION="${SDK_VERSION:-$CURRENT_SDK_VERSION}"

# ─── Dọn artifact cũ trong cache local ───────────────────────────────────────────────────────
# Gradle đánh cache theo toạ độ group:module:version. Đè một version đã publish mà không dọn thì
# máy này vẫn resolve ra bản CŨ, im lặng — không lỗi, không cảnh báo, chỉ là chạy sai code.
#
# Ba chỗ giữ bản cũ, phải dọn cả ba:
#   ~/.m2/repository/<group>/<module>/<version>                        (publishToMavenLocal)
#   ~/.gradle/caches/modules-2/files-2.1/<group>/<module>/<version>    (artifact tải từ remote)
#   ~/.gradle/caches/modules-2/metadata-*/descriptors/<group>/<module>/<version>   (pom/module đã parse)
#
# Xoá tới cấp <version>, KHÔNG xoá cấp <module> hay <group>: cùng group `vn.viettelpay.library`
# còn hàng chục thư viện khác (blurview, flexbox, cameraview…) và cả version cũ của chính
# `promotion` đang nằm nhờ trong cache — xoá rộng tay là bắt máy tải lại hết.
purge_local_artifacts() {
    local group_path="${SDK_GROUP//.//}"
    local removed=0 target

    local version="$SDK_VERSION"
    for module in promotion promotionLogic; do

        for target in \
            "$HOME/.m2/repository/$group_path/$module/$version" \
            "$HOME/.gradle/caches/modules-2/files-2.1/$SDK_GROUP/$module/$version"
        do
            if [[ -d "$target" ]]; then
                rm -rf "$target"
                echo "  ✗ $target"
                removed=$((removed + 1))
            fi
        done

        # metadata-* có nhiều bản (một thư mục cho mỗi phiên bản Gradle từng chạy trên máy).
        for target in "$HOME/.gradle/caches/modules-2"/metadata-*/descriptors/"$SDK_GROUP/$module/$version"; do
            if [[ -d "$target" ]]; then
                rm -rf "$target"
                echo "  ✗ $target"
                removed=$((removed + 1))
            fi
        done
    done

    if [[ "$removed" -eq 0 ]]; then
        echo "  (cache local không có bản cũ nào của promotion / promotionLogic $SDK_VERSION)"
    fi
}

# x.y.z, cho phép hậu tố -SNAPSHOT / -rc1 / .1. Chặn ở đây vì version sai định dạng vẫn publish được
# (Maven không kén), chỉ vỡ ra sau — lúc host resolve không thấy, hoặc thấy sai thứ tự version.
check_version_format() {   # $1 = nhãn, $2 = version
    if ! printf '%s' "$2" | grep -qE '^[0-9]+\.[0-9]+\.[0-9]+([.-][A-Za-z0-9]+)*$'; then
        echo "Version $1 '$2' không đúng định dạng x.y.z (hậu tố -SNAPSHOT/-rc1 thì được)." >&2
        exit 1
    fi
}
check_version_format 'SDK_VERSION' "$SDK_VERSION"

# Một `-P` cho cả hai module: `:promotionLogic/build.gradle.kts` cũng đọc `SDK_VERSION`.
GRADLE_ARGS=("-PSDK_VERSION=$SDK_VERSION")
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

    # Dọn TRƯỚC khi publish vào ~/.m2: ở chế độ local ta ghi đè cùng một version mỗi lần chạy, nên
    # bản cũ trong cache Gradle là thứ duy nhất có thể che mất bản vừa publish.
    if [[ "$FORCE" == true ]]; then
        echo "▸ Dọn cache local của promotion + promotionLogic $SDK_VERSION"
        purge_local_artifacts
    fi

    # Bước dễ quên nhất: sửa SDK xong mà không publish thì app vẫn build với bản cũ trong ~/.m2 —
    # im lặng, không cảnh báo. Script ép đúng thứ tự publish → build.
    echo "▸ Publish vào ~/.m2: promotion + promotionLogic $SDK_VERSION"
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
        if [[ "$FORCE" == true ]]; then
            OVERWRITING=true
            echo "⚠ Version $SDK_VERSION ĐÃ có trên repo — ĐÈ theo --force. Repo release bật immutable thì server vẫn từ chối." >&2
        elif [[ "$ASSUME_YES" != true && -t 0 ]]; then
            # Có người ngồi trước máy thì HỎI — đối xứng build-ios.sh. Probe chạy TRƯỚC khi build
            # nên trả lời "không" cũng chưa mất phút nào.
            #
            # Trả "y" thì bật luôn $FORCE chứ không chỉ cho đi tiếp: trên Android `--force` còn kéo
            # theo dọn cache local. Đè mà không dọn thì Gradle giữ nguyên artifact cũ theo đúng toạ
            # độ group:module:version, máy bạn build với bản cũ TRONG IM LẶNG — bẫy tả ở đầu file.
            echo "⚠ Version $SDK_VERSION ĐÃ có trên repo — bản đã phát hành là thứ người khác đang build theo." >&2
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
            # `--yes` nghĩa là "khỏi hỏi lại những gì tôi đã quyết", không phải "đồng ý sẵn cả việc
            # chưa từng được hỏi". Muốn CI đè thì phải viết --force ra tường minh.
            echo "❌ Version $SDK_VERSION ĐÃ có trên repo — bản đã phát hành là thứ người khác" >&2
            echo "   đang build theo. Tăng version, hoặc --force nếu thực sự muốn ghi đè." >&2
            exit 1
        fi
    fi
fi

echo
echo "  Đích           : $TARGET_LABEL" "$([[ "$OVERWRITING" == true ]] && echo '← GHI ĐÈ bản đang có')"
echo "  Version        : $SDK_VERSION" "$([[ "$SDK_VERSION" != "$CURRENT_SDK_VERSION" ]] && echo "(gradle.properties đang là $CURRENT_SDK_VERSION)")"
echo "  Module         : promotion + promotionLogic (chung một số)"
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

echo "▸ Build + publish promotion + promotionLogic $SDK_VERSION → $TARGET"
gradle "${GRADLE_TASKS[@]}"

# Dọn SAU khi publish (ngược với chế độ local): dọn trước rồi publish thì Gradle vẫn kịp kéo bản cũ
# về trong lúc build, thành ra dọn xong lại có ngay bản cũ nằm đó.
if [[ "$FORCE" == true ]]; then
    echo "▸ Dọn cache local để lần resolve sau lấy đúng bản vừa đè"
    purge_local_artifacts
fi

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
fi

echo
echo "✓ Xong. Host khai:"
echo "    implementation(\"$SDK_GROUP:promotion:$SDK_VERSION\")"
