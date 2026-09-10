#!/usr/bin/env bash
#
# Sàn iOS mà SDK khai phải **thật sự chạy được**.
#
# SDK dùng Swift concurrency (`Task`/`async`/`await`) — bắt buộc, vì SKIE bridge hàm `suspend` của
# Kotlin thành `async`. Concurrency runtime chỉ nằm sẵn trong iOS từ **15.0**; dưới mức đó Xcode nhúng
# một bản back-deploy (`libswift_Concurrency.dylib`) vào app **của host**. Bản nhúng đó có sàn riêng,
# do phiên bản Xcode quyết định — và nó ĐÃ từng cao hơn sàn ta khai: Xcode 26 ship bản simulator với
# `minos 14.0` trong khi SDK khai 13.0.
#
# Kịch bản hỏng: nâng Xcode → sàn của dylib back-deploy nhích lên → app host build vẫn xanh, nhưng máy
# user ở phiên bản dưới sàn mới thì dyld không nạp được dylib và app chết ngay lúc khởi động. Không
# test nào trong repo bắt được, vì không có simulator nào dưới iOS 15 để chạy.
#
# Script này so **sàn của dylib back-deploy trong toolchain đang dùng** với **sàn SDK khai**.
#
# Chạy: ./scripts/check-backdeploy.sh
#
set -euo pipefail
cd "$(dirname "$0")/.."

pbx="iosPromotionSDK/PromotionKit.xcodeproj/project.pbxproj"

# Sàn SDK khai — lấy giá trị THẤP NHẤT trong pbxproj (nếu các configuration lệch nhau thì chỗ thấp
# nhất mới là lời hứa thực tế với host).
declared="$(grep -oE 'IPHONEOS_DEPLOYMENT_TARGET = [0-9.]+' "$pbx" \
  | awk '{print $3}' | tr -d ';' | sort -V | head -1)"
[ -n "$declared" ] || { echo "✗ không đọc được IPHONEOS_DEPLOYMENT_TARGET từ $pbx"; exit 1; }

lib_dir="$(xcode-select -p)/Toolchains/XcodeDefault.xctoolchain/usr/lib/swift-5.5/iphoneos"
lib="$lib_dir/libswift_Concurrency.dylib"
[ -f "$lib" ] || { echo "✗ không thấy dylib back-deploy: $lib"; exit 1; }

# Xcode ghi sàn bằng LC_VERSION_MIN_IPHONEOS (bản cũ) hoặc LC_BUILD_VERSION (bản mới) — đọc cả hai.
floor="$(vtool -show-build "$lib" 2>/dev/null \
  | awk '/LC_VERSION_MIN_IPHONEOS|LC_BUILD_VERSION/{f=1} f&&/^ *(version|minos) /{print $2; exit}')"
[ -n "$floor" ] || { echo "✗ không đọc được sàn của $lib"; exit 1; }

echo "Xcode:         $(xcodebuild -version | head -1)"
echo "SDK khai sàn:  iOS $declared"
echo "Back-deploy:   iOS $floor  ($lib)"

lowest="$(printf '%s\n%s\n' "$declared" "$floor" | sort -V | head -1)"
if [ "$lowest" != "$floor" ]; then
  cat <<MSG

✗ Sàn back-deploy ($floor) CAO HƠN sàn SDK khai ($declared).
  App host build ở sàn $declared sẽ chết lúc khởi động trên máy iOS $declared–$floor.
  Cách xử lý: nâng IPHONEOS_DEPLOYMENT_TARGET của SDK lên $floor, hoặc giữ lại Xcode cũ.
MSG
  exit 1
fi

echo
echo "✓ Sàn back-deploy nằm dưới sàn SDK khai."
echo "  LƯU Ý: đây chỉ kiểm phần ĐÓNG GÓI. Không simulator nào dưới iOS 15 tồn tại trên Xcode hiện"
echo "  hành, nên phần RUNTIME của back-deploy (iOS 13.0–14.x) vẫn chưa từng được chạy thử."
echo "  Xem docs/ios/Distribution.md §3 mục 'Sàn iOS'."
