#!/usr/bin/env bash
#
# Đẩy Promotion.xcframework.zip lên Artifactory nội bộ (repo generic `vdo-ios-frameworks`).
#
# Khác Android: iOS KHÔNG đi qua Maven, nên không có `./gradlew publish`. Host tải thẳng một file
# zip theo URL (docs/ios/Distribution.md). Script này là "publish" của phía iOS.
#
# Bố cục trên Artifactory — bám đúng convention các SDK khác trong cùng repo
# (Martech/CEP, VDONetwork, VDOUtils):
#
#   vdo-ios-frameworks/Martech/Promotion/<version>/
#       ├── Promotion-<version>.xcframework.zip
#       └── metadata.json          ← download_url + sha256, host đọc để khai binaryTarget
#
# Thứ tự BẮT BUỘC: tạo thư mục version TRƯỚC, kiểm tra nó tồn tại, rồi mới nhét file vào.
# Artifactory có tạo folder ngầm khi upload, nhưng làm tường minh thì lỗi lộ ra ở đúng bước sai
# (sai quyền ghi hiện ngay lúc tạo folder, không phải sau khi đã đẩy xong 12MB).
#
# Usage:
#   ./scripts/publish-xcframework.sh                    # version lấy từ Info.plist của framework
#   SDK_VERSION=1.2.3 ./scripts/publish-xcframework.sh   # ép version
#   ./scripts/publish-xcframework.sh --dry-run           # in ra sẽ làm gì, không gửi gì lên
#   ./scripts/publish-xcframework.sh --force             # cho phép ghi đè version đã tồn tại
#
# Credentials (không bao giờ commit) — theo thứ tự ưu tiên:
#   1. env ARTIFACTORY_USER / ARTIFACTORY_PASSWORD
#   2. local.properties: maven.username / maven.password  ← cùng tài khoản Android đang dùng
#
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
REPO="$(cd "$ROOT/.." && pwd)"

BASE_URL="https://mobile-data.viettelmoney.vn/artifactory"
REPO_KEY="vdo-ios-frameworks"
GROUP="Martech"
SDK_NAME="Promotion"

DRY_RUN=false
FORCE=false

while [[ $# -gt 0 ]]; do
    case "$1" in
        --dry-run) DRY_RUN=true; shift ;;
        --force)   FORCE=true; shift ;;
        -h|--help) sed -n '3,30p' "$0" | sed 's/^# \{0,1\}//'; exit 0 ;;
        *)         echo "Tham số lạ: $1 (xem --help)" >&2; exit 1 ;;
    esac
done

# ─── 1. Gói phát hành phải có sẵn ────────────────────────────────────────────────────────────
# Script này CỐ Ý không tự build: publish là hành động một chiều, phải đẩy đúng thứ vừa kiểm tra
# bằng tay, không phải thứ vừa được dựng lại sau lưng.

ZIP="$ROOT/build/Promotion.xcframework.zip"
XCF="$ROOT/build/Promotion.xcframework"

if [[ ! -f "$ZIP" ]]; then
    echo "Không thấy $ZIP — chạy ./scripts/build-xcframework.sh trước." >&2
    exit 1
fi

# Version lấy từ chính binary sắp đẩy (MARKETING_VERSION nhồi lúc archive), không phải từ biến rời —
# tránh cảnh đặt tên file 1.0.1 mà bên trong Info.plist vẫn ghi 1.0.0.
PLIST="$XCF/ios-arm64/PromotionKit.framework/Info.plist"
if [[ -f "$PLIST" ]]; then
    BUILT_VERSION="$(/usr/libexec/PlistBuddy -c "Print :CFBundleShortVersionString" "$PLIST")"
else
    BUILT_VERSION=""
fi

VERSION="${SDK_VERSION:-${BUILT_VERSION:-}}"
if [[ -z "$VERSION" ]]; then
    echo "Không xác định được version (thiếu $PLIST). Truyền SDK_VERSION=x.y.z." >&2
    exit 1
fi

if [[ -n "$BUILT_VERSION" && "$VERSION" != "$BUILT_VERSION" ]]; then
    echo "⚠️  SDK_VERSION=$VERSION nhưng framework build ra là $BUILT_VERSION." >&2
    echo "    Dựng lại: SDK_VERSION=$VERSION ./scripts/build-xcframework.sh" >&2
    exit 1
fi

ARTIFACT="$SDK_NAME-$VERSION.xcframework.zip"
DIR_URL="$BASE_URL/$REPO_KEY/$GROUP/$SDK_NAME/$VERSION"
ZIP_URL="$DIR_URL/$ARTIFACT"
META_URL="$DIR_URL/metadata.json"
API_DIR="$BASE_URL/api/storage/$REPO_KEY/$GROUP/$SDK_NAME/$VERSION"

# ─── 2. Credentials ──────────────────────────────────────────────────────────────────────────

USER="${ARTIFACTORY_USER:-}"
PASS="${ARTIFACTORY_PASSWORD:-}"

if [[ -z "$USER" || -z "$PASS" ]]; then
    LOCAL_PROPS="$REPO/local.properties"
    if [[ -f "$LOCAL_PROPS" ]]; then
        USER="${USER:-$(grep '^maven.username=' "$LOCAL_PROPS" | cut -d= -f2-)}"
        PASS="${PASS:-$(grep '^maven.password=' "$LOCAL_PROPS" | cut -d= -f2-)}"
    fi
fi

if [[ -z "$USER" || -z "$PASS" ]]; then
    echo "Thiếu credentials. Khai ARTIFACTORY_USER/ARTIFACTORY_PASSWORD, hoặc" >&2
    echo "maven.username/maven.password trong local.properties." >&2
    exit 1
fi

# `-u` nhận thẳng chuỗi user:pass sẽ lộ password trong `ps`. Dùng --netrc-file với file tạm
# quyền 600, xoá lúc thoát (kể cả khi lỗi).
NETRC="$(mktemp)"
chmod 600 "$NETRC"
trap 'rm -f "$NETRC"' EXIT
printf 'machine mobile-data.viettelmoney.vn login %s password %s\n' "$USER" "$PASS" > "$NETRC"

curl_auth() { curl -s --netrc-file "$NETRC" --max-time 300 "$@"; }

SHA256="$(shasum -a 256 "$ZIP" | cut -d' ' -f1)"
SIZE_MB="$(echo "scale=1; $(stat -f%z "$ZIP") / 1048576" | bc)"

echo "▶︎ SDK      : $SDK_NAME $VERSION"
echo "▶︎ File     : $ARTIFACT (${SIZE_MB}MB)"
echo "▶︎ Đích     : $ZIP_URL"
echo "▶︎ sha256   : $SHA256"
echo ""

# ─── 3. Version đã tồn tại chưa ──────────────────────────────────────────────────────────────
# Bản đã phát hành là thứ người khác đang build theo. Ghi đè im lặng nghĩa là host cùng một
# version lại nhận hai binary khác nhau — phải cố ý mới được làm.

EXISTING="$(curl_auth -o /dev/null -w '%{http_code}' "$API_DIR")"
if [[ "$EXISTING" == "200" ]]; then
    if [[ "$FORCE" != true ]]; then
        echo "❌ $GROUP/$SDK_NAME/$VERSION ĐÃ TỒN TẠI trên Artifactory." >&2
        echo "   Tăng version, hoặc --force nếu thực sự muốn ghi đè bản đã phát hành." >&2
        exit 1
    fi
    echo "⚠️  Version đã tồn tại — ghi đè theo --force."
fi

if [[ "$DRY_RUN" == true ]]; then
    echo "(dry-run) Dừng ở đây, không gửi gì lên."
    exit 0
fi

# ─── 4. Tạo thư mục version TRƯỚC ────────────────────────────────────────────────────────────
# PUT vào path có dấu `/` cuối = tạo directory rỗng.

echo "▶︎ Tạo package $GROUP/$SDK_NAME/$VERSION"
CODE="$(curl_auth -o /dev/null -w '%{http_code}' -X PUT "$DIR_URL/")"
if [[ "$CODE" != "201" && "$CODE" != "200" ]]; then
    echo "❌ Tạo thư mục thất bại (HTTP $CODE) — kiểm tra quyền ghi trên $REPO_KEY." >&2
    exit 1
fi

# Xác nhận folder có thật rồi mới đẩy file, thay vì tin vào mã trả về.
CODE="$(curl_auth -o /dev/null -w '%{http_code}' "$API_DIR")"
if [[ "$CODE" != "200" ]]; then
    echo "❌ Đã PUT nhưng $API_DIR vẫn không truy vấn được (HTTP $CODE)." >&2
    exit 1
fi
echo "  ✓ Package sẵn sàng"

# ─── 5. Đẩy zip vào trong ────────────────────────────────────────────────────────────────────
# `X-Checksum-Sha256` để Artifactory tự đối chiếu — hỏng đường truyền thì nó từ chối, không âm thầm
# nhận file lỗi (host tải về mới phát hiện thì đã muộn).

echo "▶︎ Upload $ARTIFACT (${SIZE_MB}MB)"
CODE="$(curl_auth -o /tmp/prm_upload.json -w '%{http_code}' \
    -X PUT \
    -H "X-Checksum-Sha256: $SHA256" \
    -T "$ZIP" \
    "$ZIP_URL")"
if [[ "$CODE" != "201" && "$CODE" != "200" ]]; then
    echo "❌ Upload thất bại (HTTP $CODE):" >&2
    cat /tmp/prm_upload.json >&2
    exit 1
fi
echo "  ✓ Đã lên"

# ─── 6. metadata.json ────────────────────────────────────────────────────────────────────────
# SINH TỰ ĐỘNG từ zip + git, không viết tay: checksum viết tay là thứ sai lặng lẽ nhất — SPM chỉ
# báo "checksum mismatch" mà không nói bên nào sai.

echo "▶︎ Sinh + upload metadata.json"
GIT_BRANCH="$(cd "$REPO" && git rev-parse --abbrev-ref HEAD 2>/dev/null || echo '')"
GIT_TAG="$(cd "$REPO" && git describe --tags --exact-match 2>/dev/null || echo '')"
BUILD_NUMBER="$(date +%Y%m%d%H%M%S)"
BUILD_TIME="$(date '+%Y-%m-%d %H:%M:%S')"

META_FILE="$(mktemp)"
cat > "$META_FILE" <<EOF
{
  "sdk": "$SDK_NAME",
  "upload": "release",
  "builder": "local",
  "app_branch": "",
  "sdk_branch": "$GIT_BRANCH",
  "git_tag": "$GIT_TAG",
  "build_time": "$BUILD_TIME",
  "xcframework": {
    "version": "$VERSION",
    "build_number": "$BUILD_NUMBER"
  },
  "integration": {
    "artifact_version": "$VERSION",
    "download_url": "$ZIP_URL",
    "checksum": "$SHA256"
  }
}
EOF

CODE="$(curl_auth -o /dev/null -w '%{http_code}' -X PUT -T "$META_FILE" "$META_URL")"
rm -f "$META_FILE"
if [[ "$CODE" != "201" && "$CODE" != "200" ]]; then
    echo "⚠️  Upload metadata.json thất bại (HTTP $CODE) — zip đã lên, đẩy lại metadata sau." >&2
    exit 1
fi
echo "  ✓ Đã lên"

echo ""
echo "✅ Phát hành xong: $GROUP/$SDK_NAME/$VERSION"
echo "📦 $ZIP_URL"
echo "🔑 sha256: $SHA256"
echo ""
echo "ℹ️  dSYM KHÔNG nằm trong gói này. Giữ lại $ROOT/build/PromotionKit.framework.dSYM theo bản phát hành —"
echo "   không có nó thì crash report của app host chỉ còn địa chỉ trần."
