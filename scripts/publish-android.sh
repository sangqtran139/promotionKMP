#!/usr/bin/env bash
#
# Phát hành SDK Android lên Maven repo.
#
# Khác `build-android.sh` (script vòng lặp dev: publish ~/.m2 rồi build app demo), script này chỉ
# làm MỘT việc — đẩy artifact lên repo — nhưng hỏi version tử tế và chặn sớm các lỗi hay gặp
# (sai định dạng version, thiếu credentials, đè lên version đã phát hành).
#
#   ./scripts/publish-android.sh                    # hỏi cả 2 version rồi publish lên Viettelmoney
#   ./scripts/publish-android.sh 1.2.0              # điền version promotion, lõi vẫn hỏi
#   ./scripts/publish-android.sh -v 1.2.0 -l 2.0.0 --yes   # không hỏi gì (dùng cho CI)
#   ./scripts/publish-android.sh --target artifactory   # đẩy lên repo Artifactory chung
#   ./scripts/publish-android.sh --target local     # chỉ publish vào ~/.m2 (thử trước khi phát hành)
#   ./scripts/publish-android.sh --clean            # dọn build cũ rồi publish
#   ./scripts/publish-android.sh --write            # ghi luôn 2 version vừa publish vào gradle.properties
#   ./scripts/publish-android.sh --dry-run          # in ra lệnh Gradle sẽ chạy, không chạy thật
#
# HAI version độc lập, mỗi module một số (gradle.properties):
#   -v / --version        SDK_VERSION   → $SDK_GROUP:promotion      (toạ độ host khai)
#   -l / --logic-version  LOGIC_VERSION → $SDK_GROUP:promotionLogic (lõi KMP)
# Cả hai luôn được publish cùng lượt: `promotion` trỏ LOGIC_VERSION trong metadata, đẩy lệch một
# bên là host resolve ra bản lõi không tồn tại.
#
# Đích phát hành (xem docs/android/Distribution.md §3.4):
#   viettelmoney  $SDK_GROUP:promotion / promotionLogic  ← mặc định
#                 credentials: maven.username / maven.password trong local.properties
#   artifactory   $SDK_GROUP:promotion / promotionLogic
#                 credentials: artifactoryUrl/User/Password ở ~/.gradle/gradle.properties hoặc ARTIFACTORY_*
#   local         ~/.m2/repository (không cần credentials)
#
set -euo pipefail

cd "$(dirname "$0")/.."   # luôn chạy từ gốc repo, gọi script từ đâu cũng được

TARGET="viettelmoney"
SDK_VERSION=""
LOGIC_VERSION=""
DO_CLEAN=false
ASSUME_YES=false
DO_WRITE=false
DRY_RUN=false

VIETTELMONEY_URL="https://mobile-data.viettelmoney.vn/artifactory/gradle-viettelmoney"

while [[ $# -gt 0 ]]; do
    case "$1" in
        -v|--version) SDK_VERSION="${2:-}"; shift 2 ;;
        -l|--logic-version) LOGIC_VERSION="${2:-}"; shift 2 ;;
        -t|--target)  TARGET="${2:-}"; shift 2 ;;
        --clean)      DO_CLEAN=true; shift ;;
        -y|--yes)     ASSUME_YES=true; shift ;;
        --write)      DO_WRITE=true; shift ;;
        --dry-run)    DRY_RUN=true; shift ;;
        -h|--help)    sed -n '3,29p' "$0" | sed 's/^# \{0,1\}//'; exit 0 ;;
        -*)           echo "Tham số lạ: $1 (xem --help)" >&2; exit 1 ;;
        *)            # version dạng positional: ./scripts/publish-android.sh 1.2.0
                      if [[ -z "$SDK_VERSION" ]]; then SDK_VERSION="$1"; shift
                      else echo "Thừa tham số: $1 (xem --help)" >&2; exit 1; fi ;;
    esac
done

case "$TARGET" in
    viettelmoney|artifactory|local) ;;
    *) echo "--target phải là: viettelmoney | artifactory | local (nhận được: '$TARGET')" >&2; exit 1 ;;
esac

# ─── Version ─────────────────────────────────────────────────────────────────────────────────
# Hai số ĐỘC LẬP, mặc định lấy từ gradle.properties: SDK_VERSION (promotion) và LOGIC_VERSION
# (promotionLogic).

read_gradle_property() {
    grep -E "^$1=" gradle.properties | head -1 | cut -d= -f2-
}

CURRENT_VERSION="$(read_gradle_property 'SDK_VERSION')"
CURRENT_VERSION="${CURRENT_VERSION:-1.0.0}"
CURRENT_LOGIC_VERSION="$(read_gradle_property 'LOGIC_VERSION')"
CURRENT_LOGIC_VERSION="${CURRENT_LOGIC_VERSION:-1.0.0}"
SDK_GROUP="$(read_gradle_property 'SDK_GROUP')"
SDK_GROUP="${SDK_GROUP:-vn.viettelpay.library}"

# Hỏi từng số, mỗi số một default riêng. Enter suông = giữ nguyên số trong gradle.properties, nên
# bump một module mà không đụng module kia là thao tác mặc định.
prompt_version() {   # $1 = nhãn, $2 = default, $3 = tên biến cần gán
    local answer
    if [[ ! -t 0 ]]; then
        echo "Không có TTY để hỏi version — truyền thẳng: $0 --version x.y.z --logic-version a.b.c" >&2
        exit 1
    fi
    printf 'Version %s [%s]: ' "$1" "$2"
    read -r answer
    printf -v "$3" '%s' "${answer:-$2}"
}

[[ -z "$SDK_VERSION" ]] && prompt_version 'promotion' "$CURRENT_VERSION" SDK_VERSION
[[ -z "$LOGIC_VERSION" ]] && prompt_version 'promotionLogic' "$CURRENT_LOGIC_VERSION" LOGIC_VERSION

# x.y.z, cho phép hậu tố -SNAPSHOT / -rc1 / .1. Chặn ở đây vì version sai định dạng vẫn publish
# được (Maven không kén), chỉ vỡ ra sau, lúc host resolve không thấy hoặc thấy sai thứ tự version.
check_version_format() {   # $1 = nhãn, $2 = version
    if ! printf '%s' "$2" | grep -qE '^[0-9]+\.[0-9]+\.[0-9]+([.-][A-Za-z0-9]+)*$'; then
        echo "Version $1 '$2' không đúng định dạng x.y.z (hậu tố -SNAPSHOT/-rc1 thì được)." >&2
        exit 1
    fi
}
check_version_format 'promotion' "$SDK_VERSION"
check_version_format 'promotionLogic' "$LOGIC_VERSION"

# ─── Điều kiện cần theo từng đích ────────────────────────────────────────────────────────────

read_local_property() {
    grep -E "^$1=" local.properties 2>/dev/null | head -1 | cut -d= -f2-
}

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
        # Một dòng là đủ: POM/module.json auto-gen đã khai promotionLogic + Ktor + Glide… ở scope
        # runtime, Gradle tự kéo về. (Trước đây POM viết tay không khai gì nên phải dặn host khai
        # tay cả lõi — bỏ rồi.)
        COORD_LINES=("implementation(\"$SDK_GROUP:promotion:$SDK_VERSION\")")
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
        COORD_LINES=("implementation(\"$SDK_GROUP:promotion:$SDK_VERSION\")")
        TARGET_LABEL="Artifactory chung (${ARTIFACTORY_URL:-artifactoryUrl trong ~/.gradle})"
        ;;
    local)
        GRADLE_TASKS=(:promotionLogic:publishToMavenLocal :AndroidPromotionSDK:publishToMavenLocal)
        COORD_LINES=("implementation(\"$SDK_GROUP:promotion:$SDK_VERSION\")  // từ mavenLocal(), cần -PuseMavenLocal=true")
        TARGET_LABEL="~/.m2/repository"
        ;;
esac

# ─── Cảnh báo đè version đã phát hành ────────────────────────────────────────────────────────
# Repo release của Artifactory thường bật "immutable": đẩy đè version cũ bị từ chối giữa chừng,
# sau khi đã build xong. Hỏi trước cho đỡ mất công — chỉ kiểm tra được với đích viettelmoney vì
# URL cố định; lỗi mạng/curl thiếu thì bỏ qua, không chặn phát hành.
if [[ "$TARGET" == "viettelmoney" && "$SDK_VERSION" != *SNAPSHOT ]] && command -v curl >/dev/null 2>&1; then
    group_path="${SDK_GROUP//.//}"
    probe_url="$VIETTELMONEY_URL/$group_path/promotion/$SDK_VERSION/promotion-$SDK_VERSION.pom"
    http_code="$(curl -s -o /dev/null -w '%{http_code}' -u "$MAVEN_USER:$MAVEN_PASS" \
        --max-time 15 -I "$probe_url" 2>/dev/null || echo "000")"
    if [[ "$http_code" == "200" ]]; then
        echo "⚠ Version $SDK_VERSION ĐÃ có trên repo. Repo release thường không cho ghi đè — nhiều khả năng publish sẽ bị từ chối." >&2
        ASSUME_YES=false   # trường hợp này bắt buộc phải có người xác nhận
    fi
fi

# ─── Xác nhận ────────────────────────────────────────────────────────────────────────────────

echo
echo "  Đích     : $TARGET_LABEL"
echo "  promotion      : $SDK_VERSION" "$([[ "$SDK_VERSION" != "$CURRENT_VERSION" ]] && echo "(gradle.properties đang là $CURRENT_VERSION)")"
echo "  promotionLogic : $LOGIC_VERSION" "$([[ "$LOGIC_VERSION" != "$CURRENT_LOGIC_VERSION" ]] && echo "(gradle.properties đang là $CURRENT_LOGIC_VERSION)")"
echo "  Gradle   : ./gradlew ${GRADLE_TASKS[*]} -PSDK_VERSION=$SDK_VERSION -PLOGIC_VERSION=$LOGIC_VERSION"
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

# ─── Chạy ────────────────────────────────────────────────────────────────────────────────────

if [[ "$DO_CLEAN" == true ]]; then
    echo "▸ Dọn build cũ"
    ./gradlew clean -PSDK_VERSION="$SDK_VERSION" -PLOGIC_VERSION="$LOGIC_VERSION"
fi

echo "▸ Build + publish promotion $SDK_VERSION + promotionLogic $LOGIC_VERSION → $TARGET"
./gradlew "${GRADLE_TASKS[@]}" -PSDK_VERSION="$SDK_VERSION" -PLOGIC_VERSION="$LOGIC_VERSION"

# ─── Ghi version vào gradle.properties (tuỳ chọn) ────────────────────────────────────────────
# Mặc định KHÔNG ghi: publish thử một bản -SNAPSHOT/-rc không nên làm bẩn nguồn version tập trung.
# Ghi rồi thì nhớ đồng bộ tay MARKETING_VERSION bên iOS (PromotionSDKUI.xcodeproj) — xem gradle.properties.

if [[ "$DO_WRITE" == true ]]; then
    if [[ "$SDK_VERSION" != "$CURRENT_VERSION" ]]; then
        tmp="$(mktemp)"
        sed "s/^SDK_VERSION=.*/SDK_VERSION=$SDK_VERSION/" gradle.properties > "$tmp"
        mv "$tmp" gradle.properties
        echo "▸ Đã ghi SDK_VERSION=$SDK_VERSION vào gradle.properties (nhớ đồng bộ MARKETING_VERSION bên iOS)"
    fi
    if [[ "$LOGIC_VERSION" != "$CURRENT_LOGIC_VERSION" ]]; then
        tmp="$(mktemp)"
        sed "s/^LOGIC_VERSION=.*/LOGIC_VERSION=$LOGIC_VERSION/" gradle.properties > "$tmp"
        mv "$tmp" gradle.properties
        echo "▸ Đã ghi LOGIC_VERSION=$LOGIC_VERSION vào gradle.properties"
    fi
fi

echo
echo "✓ Xong. Host khai:"
for line in "${COORD_LINES[@]}"; do
    echo "    $line"
done
