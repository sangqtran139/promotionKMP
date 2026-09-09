#!/usr/bin/env bash
#
# Tên field của theme token phải khớp Android ↔ iOS.
#
# Theme là hợp đồng UI với host: host truyền 6 token và mong mọi màn SDK đổi theo. Sáu token đó được
# map THỦ CÔNG hai lượt ở mỗi nền tảng (`applyTheme` và `currentTheme`) — tổng 4 bảng map viết tay,
# và không có gì kiểm tên field hai bên còn khớp. Test round-trip cũng không bắt được: nó chỉ kiểm
# một nền tảng tự nhất quán với chính nó.
#
# Chạy: ./scripts/check-theme-parity.sh
#
set -euo pipefail
cd "$(dirname "$0")/.."

tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT

grep -rhoE 'public var [a-zA-Z]+' \
     iosPromotionSDK/PromotionSDKUI/Theme/Token/*.swift \
  | awk '{print $3}' | sort -u > "$tmp/ios.txt"

grep -rhoE 'val [a-zA-Z]+:' \
     AndroidPromotionSDK/src/main/java/com/ttcn/prm/ui/theme/token/*.kt \
  | sed 's/val //; s/://' | sort -u > "$tmp/android.txt"

if ! diff -u "$tmp/android.txt" "$tmp/ios.txt"; then
    echo "❌ Theme token lệch giữa Android và iOS (trái = Android, phải = iOS)." >&2
    echo "   Sửa cả hai bên, đừng sửa một bên cho script xanh." >&2
    exit 1
fi

echo "✅ Theme token khớp hai nền tảng ($(wc -l < "$tmp/ios.txt" | tr -d ' ') field)"
