#!/usr/bin/env bash
#
# Chặn màu hardcode trong XIB TĂNG thêm.
#
# Màu đặt trong XIB không đi qua `PRMThemeRegistry` — host đổi theme thì những chỗ này không đổi, và
# không có cảnh báo nào. Lint không kiểm được XIB, nên phải là script riêng.
#
# **Baseline giảm dần**, không phải gate đỏ ngay: bật được hôm nay mà không phải sửa 13 chỗ trước, và
# mỗi lần ai đó dọn bớt thì script tự nhắc hạ ngưỡng. Gate đỏ ngay thì cách duy nhất để đi tiếp là
# tắt nó đi.
#
# Giảm ngưỡng bằng cách: xoá màu khỏi XIB → set trong `setupUI()` từ token → hạ BASELINE ở dưới.
#
set -euo pipefail
cd "$(dirname "$0")/.."

BASELINE=13

FOUND=$(grep -rhoE '<color [^/]*red="[0-9.]+"' --include='*.xib' iosPromotionSDK | wc -l | tr -d ' ')

if [ "$FOUND" -gt "$BASELINE" ]; then
    echo "❌ Màu hardcode trong XIB: $FOUND (baseline $BASELINE)." >&2
    echo "   Set màu từ token trong setupUI() thay vì đặt trong XIB." >&2
    exit 1
fi

if [ "$FOUND" -lt "$BASELINE" ]; then
    echo "🎉 Còn $FOUND — hạ BASELINE trong scripts/check-xib-theme.sh xuống $FOUND để khoá tiến bộ lại"
fi

echo "✅ XIB: $FOUND màu hardcode (≤ baseline $BASELINE)"
