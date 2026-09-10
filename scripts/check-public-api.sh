#!/usr/bin/env bash
#
# Không có khai báo public nào lọt ra NGOÀI allowlist bề mặt host.
#
# Gate này thay hai lệnh `grep` từng chép tay trong `docs/common/PublicApi.md` và
# `docs/AI_AGENT_RULES.md`. Bản cũ **luôn đỏ 126 dòng** (Android 17 + iOS 109) vì nó chỉ loại trừ
# `/entry/`, trong khi allowlist do chính `PublicApi.md` khai gồm ba nhánh Android và hai nhánh iOS.
#
# Một gate luôn đỏ còn tệ hơn không có gate: người làm theo checklist chỉ có hai đường, và cả hai đều
# xấu — hoặc "sửa cho xanh" bằng cách đổi `public` → `internal` ở `ui/theme` (tức xoá theme API khỏi
# bề mặt host, breaking, mà trông y như một hành động tuân thủ rule), hoặc học được rằng gate này
# luôn đỏ nên bỏ qua, và từ đó mọi vi phạm THẬT cũng bị bỏ qua cùng.
#
# Chạy: ./scripts/check-public-api.sh
#
set -euo pipefail
cd "$(dirname "$0")/.."

fail=0

# ─── Android ─────────────────────────────────────────────────────────────────
# Allowlist (bảng ở PublicApi.md §1): entry.** · ui.theme.** · ui.feature.offerwidget
#
# Regex KHÔNG neo vào chữ `public`: trong Kotlin, không ghi modifier NGHĨA LÀ public. Neo vào
# `^public` là bỏ lọt đúng dạng khai phổ biến nhất (`data class Foo(`).
android=$(grep -rEn '^(public )?(open |abstract |sealed |data |enum |annotation |value |inline |suspend |const |fun )*(class|interface|object|fun|val|var|typealias) ' \
    --include='*.kt' AndroidPromotionSDK/src/main/java/com/ttcn/prm \
    | grep -vE '/(entry|ui/theme|ui/feature/offerwidget)/' || true)

if [ -n "$android" ]; then
    echo "❌ Android — khai báo public ngoài allowlist (entry / ui.theme / ui.feature.offerwidget):" >&2
    echo "$android" >&2
    fail=1
fi

# ─── iOS ─────────────────────────────────────────────────────────────────────
# Allowlist: Entry/** · PromotionKit/Theme/** (theme là bề mặt công khai, không phải rò rỉ)
ios=$(grep -rn '^[[:space:]]*\(public\|open\)[[:space:]]' --include='*.swift' \
    iosPromotionSDK/PromotionKit | grep -v '/Theme/' || true)

if [ -n "$ios" ]; then
    echo "❌ iOS — khai báo public trong PromotionKit ngoài Theme/:" >&2
    echo "$ios" >&2
    fail=1
fi

if [ "$fail" -ne 0 ]; then
    echo "" >&2
    echo "Sửa bằng cách đổi khai báo sang 'internal', HOẶC — nếu nó đúng là bề mặt host —" >&2
    echo "cập nhật allowlist ở cả script này lẫn bảng trong docs/common/PublicApi.md." >&2
    exit 1
fi

echo "✅ Public API nằm gọn trong allowlist (Android: entry/ui.theme/offerwidget · iOS: Entry/Theme)"
