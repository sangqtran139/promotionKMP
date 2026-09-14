#!/usr/bin/env bash
#
# Gác chiều phụ thuộc **nền → feature** trong `:promotionLogic`.
#
# Tầng nền (`host/`, `common/`, `di/`, `config/`, `data/`, `domain/`, `presentation/base|common/`)
# KHÔNG được import từ một feature cụ thể (`presentation/<feature>/`). Chiều đúng là feature → nền.
#
# Vì sao cần gác: vi phạm kiểu này không làm build đỏ — nó chỉ âm thầm khoá cánh cửa tách feature
# thành module Gradle riêng, và lúc đó mới phát hiện thì đã hàng chục chỗ. Ví dụ thật: danh mục tên
# event từng nằm ở `common/PromotionAnalytics.kt` và liệt kê tên của màn Chọn ưu đãi + Chi tiết —
# tầng nền biết danh sách feature.
#
# bash 3.2 (macOS mặc định): không dùng `mapfile`, không dùng `find -printf`.
#
# Chạy: ./scripts/check-core-feature-boundary.sh
set -euo pipefail

cd "$(dirname "$0")/.."

SRC="promotionLogic/src/commonMain/kotlin/com/ttcn/promotionsdk"
PRESENTATION="$SRC/presentation"

# Feature = mọi thư mục con của `presentation/` trừ hạ tầng dùng chung (`base`, `common`).
FEATURES=""
for dir in "$PRESENTATION"/*/; do
    name=$(basename "$dir")
    case "$name" in
        base|common) continue ;;
    esac
    FEATURES="$FEATURES $name"
done
FEATURES=$(echo "$FEATURES" | tr -s ' ' | sed 's/^ //')

if [ -z "$FEATURES" ]; then
    echo "❌ Không tìm thấy feature nào dưới $PRESENTATION — script hỏng chứ không phải repo sạch."
    exit 1
fi

# File nền = mọi .kt ngoài `presentation/`, cộng `presentation/base` + `presentation/common`.
core_files() {
    find "$SRC" -name '*.kt' | grep -v "^$PRESENTATION/"
    find "$PRESENTATION/base" "$PRESENTATION/common" -name '*.kt' 2>/dev/null || true
}

violations=0
for feature in $FEATURES; do
    pattern="import com\.ttcn\.promotionsdk\.presentation\.$feature\."
    while IFS= read -r file; do
        [ -n "$file" ] || continue
        if grep -qE "$pattern" "$file"; then
            echo "❌ $file"
            echo "   import feature '$feature' — tầng nền không được biết feature nào tồn tại."
            violations=$((violations + 1))
        fi
    done <<EOF
$(core_files)
EOF
done

if [ "$violations" -gt 0 ]; then
    echo
    echo "Cách sửa: chuyển thứ đang bị import xuống tầng nền (nếu thật sự dùng chung), hoặc chuyển"
    echo "chỗ dùng nó lên feature. Xem docs/common/HostCapabilities.md §2."
    exit 1
fi

feature_count=$(echo "$FEATURES" | wc -w | tr -d ' ')
echo "✅ Ranh giới nền ↔ feature sạch ($feature_count feature: $FEATURES)"
