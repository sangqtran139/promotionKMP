#!/bin/sh
#
# Copy resource bundle của các SPM package vào trong PromotionSDKUI.framework.
#
# Vì sao cần: package khai `resources:` (PRMDesignKit, PRMPromotionUI) sinh ra bundle riêng
# `PRMDesignKit_PRMDesignKit.bundle`, `PRMPromotionUI_PRMPromotionUI.bundle`. Khi package được link **tĩnh** vào
# framework này, Xcode không copy bundle vào framework. Đến runtime, `Bundle.module` do SwiftPM
# sinh ra không tìm thấy bundle và gọi `fatalError` — app chết ngay khi mở màn đầu tiên.
#
# `Bundle.module` tìm trong `Bundle(for: BundleFinder.self).resourceURL`, tức gốc framework.
#
# Lúc `xcodebuild archive`, bundle của package mang SKIP_INSTALL=YES nên **không** nằm ở
# BUILT_PRODUCTS_DIR mà ở ${OBJROOT}/UninstalledProducts/${PLATFORM_NAME}. Quét cả hai.
#
set -eu

DEST="${TARGET_BUILD_DIR}/${CONTENTS_FOLDER_PATH}"
SEARCH_DIRS="${BUILT_PRODUCTS_DIR} ${OBJROOT}/UninstalledProducts/${PLATFORM_NAME}"

found=0
seen=" "
for dir in ${SEARCH_DIRS}; do
    [ -d "${dir}" ] || continue
    for bundle in "${dir}"/*.bundle; do
        [ -e "${bundle}" ] || continue
        name=$(basename "${bundle}")

        # Bỏ qua bundle của chính framework này — resource của nó đã nằm sẵn bên trong.
        # KHÔNG viết `[ ... ] && continue`: với `set -e`, điều kiện sai trả mã 1 và shell thoát.
        if [ "${name}" = "${PRODUCT_MODULE_NAME}_${PRODUCT_MODULE_NAME}.bundle" ]; then
            continue
        fi

        # Cùng một bundle có mặt ở cả BUILT_PRODUCTS_DIR lẫn UninstalledProducts.
        # Xử lý một lần; `cp -R` lần hai sẽ chép lồng vào trong và hỏng thư mục.
        case "${seen}" in
            *" ${name} "*) continue ;;
        esac
        seen="${seen}${name} "

        # `-L` bắt buộc: bundle ở UninstalledProducts là **symlink** trỏ ra ngoài app.
        # Copy nguyên symlink sẽ tạo liên kết gãy trong framework, và `Bundle.module` vẫn crash.
        echo "embedding ${name}"
        rm -rf "${DEST}/${name}"
        cp -RL "${bundle}" "${DEST}/${name}"

        if [ ! -d "${DEST}/${name}" ]; then
            echo "error: ${name} khong phai thu muc sau khi copy" >&2
            exit 1
        fi
        found=$((found + 1))
    done
done

echo "embedded ${found} SPM resource bundle(s) into ${CONTENTS_FOLDER_PATH}"

if [ "${found}" -eq 0 ]; then
    echo "error: không tìm thấy resource bundle nào — Bundle.module sẽ crash lúc runtime" >&2
    exit 1
fi
