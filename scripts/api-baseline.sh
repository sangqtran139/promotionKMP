#!/usr/bin/env bash
#
# Baseline bề mặt public của lõi KMP — snapshot header Kotlin→ObjC.
#
#   ./scripts/api-baseline.sh check    # so với baseline đã commit (mặc định)
#   ./scripts/api-baseline.sh update   # ghi đè baseline sau khi ĐÃ REVIEW diff
#
# Vì sao cần: mọi thứ `public` ở `promotionLogic` đều lọt ra `PromotionLogic.h` của xcframework, tức
# thành bề mặt iOS nhìn thấy. Repo lại dùng **SKIE** — nó sinh phần Swift API TỪ Kotlin, nên chỉ cần
# nâng version SKIE là bề mặt Swift đổi **mà không commit nào chạm source**. Không có baseline thì
# không cơ chế nào phát hiện.
#
# `scripts/build-ios.sh` chủ động xoá `*.abi.json` khỏi gói giao host (đúng), nên baseline phải sống
# trong repo chứ không lấy từ artifact.
#
# Chuẩn hoá trước khi so: bỏ comment sinh tự động, dòng trống, và các `#define` mang version/timestamp
# — không thì mỗi lần build lại ra một diff giả.
#
set -euo pipefail
cd "$(dirname "$0")/.."

MODE="${1:-check}"
BASELINE="docs/api/PromotionLogic.baseline.h"
HEADER="promotionLogic/build/XCFrameworks/release/PromotionLogic.xcframework/ios-arm64/PromotionLogic.framework/Headers/PromotionLogic.h"

if [ ! -f "$HEADER" ]; then
    echo "Chưa có header. Dựng trước: ./scripts/build-ios.sh local --skip-app" >&2
    exit 1
fi

normalize() {
    grep -vE '^\s*(//|$)' "$1" \
      | grep -vE '#define (KOTLIN_VERSION|__has_attribute)' \
      | sed -E 's/[[:space:]]+$//'
}

tmp="$(mktemp)"; trap 'rm -f "$tmp"' EXIT
normalize "$HEADER" > "$tmp"

case "$MODE" in
    update)
        mkdir -p "$(dirname "$BASELINE")"
        cp "$tmp" "$BASELINE"
        echo "✅ Đã ghi baseline: $BASELINE ($(wc -l < "$BASELINE" | tr -d ' ') dòng)"
        echo "   Commit nó CÙNG commit đổi API, để diff giải thích được vì sao bề mặt đổi."
        ;;
    check)
        if [ ! -f "$BASELINE" ]; then
            echo "Chưa có baseline. Tạo lần đầu: $0 update" >&2
            exit 1
        fi
        if diff -u "$BASELINE" "$tmp"; then
            echo "✅ Bề mặt public của lõi khớp baseline"
        else
            echo "" >&2
            echo "❌ Bề mặt public của lõi ĐÃ ĐỔI so với baseline (xem diff trên)." >&2
            echo "   Cố ý đổi → review diff rồi chạy: $0 update" >&2
            echo "   Không cố ý → có thứ gì đó vừa thành 'public' mà không ai định thế." >&2
            exit 1
        fi
        ;;
    *)
        echo "Dùng: $0 [check|update]" >&2; exit 1 ;;
esac
