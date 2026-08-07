package com.ttcn.prm.ui.utils

import android.content.Context
import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.load.resource.bitmap.DownsampleStrategy
import com.bumptech.glide.request.RequestOptions
import com.ttcn.prm.R
import com.ttcn.promotionsdk.di.PromotionContainer

// ─── Constants ───────────────────────────────────────────────────────────────

private const val PROMO_LOGO_DECODE_SIZE = 200
private const val PROMO_BANNER_DECODE_W = 1200
private const val PROMO_BANNER_DECODE_H = 600

internal const val PRM_IMAGE_LOG_TAG = "PRMRemoteImage"

/**
 * Cờ debug của SDK (`PromotionSDKConfig.isDebug` — cùng cờ bật cURL logging). Bản release của host
 * không in gì. Đối ứng `#if DEBUG` bên iOS.
 */
internal fun isPromotionSdkDebug(): Boolean =
    PromotionContainer.isInitialized() && PromotionContainer.requireConfig().isDebug

// ─── Options ─────────────────────────────────────────────────────────────────

/** Màu nền vùng ảnh khi chưa có/không có ảnh — cùng nguồn với `prm_bg_image_placeholder` và iOS. */
private fun placeholderColor(context: Context): Int =
    ContextCompat.getColor(context, R.color.prm_tokenDark10)

/**
 * `downsample(CENTER_INSIDE) + CircleCrop()` = đúng những gì `.circleCrop()` làm (kiểm bằng `javap`
 * trên `glide-5.0.5.aar`: `circleCrop()` → `transform(DownsampleStrategy.CENTER_INSIDE, CircleCrop())`).
 * Viết tách ra để chèn được [PRMEmptyImageTransformation] **trước** `CircleCrop` — sau khi crop thì ảnh
 * rỗng 1×1 đã bị phóng to, không còn nhận ra được nữa.
 */
private fun promotionVoucherLogoBaseOptions(context: Context): RequestOptions =
    RequestOptions()
        .override(PROMO_LOGO_DECODE_SIZE, PROMO_LOGO_DECODE_SIZE)
        .diskCacheStrategy(DiskCacheStrategy.ALL)
        .downsample(DownsampleStrategy.CENTER_INSIDE)
        .transform(PRMEmptyImageTransformation(placeholderColor(context)), CircleCrop())

/**
 * Placeholder của logo phải là bản **TRÒN**: ảnh load xong bị `CircleCrop` thành tròn, mà Glide
 * **không** áp transformation lên placeholder/error. Dùng bản chữ nhật thì lúc chờ hiện ô xám vuông
 * rồi nhảy thành tròn — thấy rõ nhất ở icon service selector (view phẳng, không có mask `CircleView`
 * che giúp như card danh sách).
 */
private fun promotionVoucherLogoUiOptions(context: Context): RequestOptions =
    promotionVoucherLogoBaseOptions(context)
        .placeholder(R.drawable.prm_bg_image_placeholder_circle)
        .error(R.drawable.prm_bg_image_placeholder_circle)

private fun promotionVoucherBannerBaseOptions(context: Context): RequestOptions =
    RequestOptions()
        .override(PROMO_BANNER_DECODE_W, PROMO_BANNER_DECODE_H)
        .diskCacheStrategy(DiskCacheStrategy.ALL)
        .transform(PRMEmptyImageTransformation(placeholderColor(context)))

private fun promotionVoucherBannerUiOptions(context: Context): RequestOptions =
    promotionVoucherBannerBaseOptions(context)
        .placeholder(R.drawable.prm_bg_image_placeholder)
        .error(R.drawable.prm_bg_image_placeholder)

// ─── Generic loader ───────────────────────────────────────────────────────────

/**
 * Generic cache-aware image loader.
 *
 * @param url          URL of the image to load.
 * @param baseOptions  Options without placeholder/error (used for cache-only probe).
 * @param uiOptions    Options with placeholder/error (used for full network load).
 * @param preferCache  If true, try memory/disk cache first; fall back to network on miss.
 *                     If false, load directly from network (with cache write).
 */
private fun ImageView.loadWithCacheStrategy(
    url: String,
    baseOptions: RequestOptions,
    uiOptions: RequestOptions,
    fallbackRes: Int,
    preferCache: Boolean,
) {
    if (url.isBlank()) {
        Glide.with(this).clear(this)
        setImageResource(fallbackRes)
        return
    }

    // KHÔNG crossFade.
    //
    // `withCrossFade()` mặc định fade 300 ms, cộng vào thời gian tải mạng nên ảnh "lên chậm" theo cảm
    // nhận; iOS gán ảnh thẳng, không có transition nào. Bỏ đi để hai nền tảng hiện ảnh cùng một kiểu.
    // Muốn có fade thì phải thêm ở CẢ iOS (`UIView.transition`), đừng bật lại một bên.
    if (!preferCache) {
        Glide.with(this)
            .load(url)
            .apply(uiOptions)
            .into(this)
        return
    }

    // Nhánh cache: KHÔNG gọi `.dontAnimate()`.
    //
    // Trong Glide 4/5 `dontAnimate()` chỉ set `GifOptions.DISABLE_ANIMATION` — nó **không** liên quan
    // tới crossfade (crossfade do `.transition()` quyết định, ở đây không gọi nên vốn đã không có).
    // Giữ nó lại thì GIF **đã nằm trong cache** hiện tĩnh 1 frame, còn GIF tải mới lại chạy → cùng
    // một logo mà lần đầu chạy, vào lại thì đứng. Bỏ đi: ảnh tĩnh không đổi gì, GIF chạy ở cả 2 nhánh,
    // khớp iOS (`UIImage.prmDecoded(from:)`).
    Glide.with(this)
        .load(url)
        .apply(baseOptions)
        .onlyRetrieveFromCache(true)
        .error(
            Glide.with(this)
                .load(url)
                .apply(uiOptions)
        )
        .into(this)
}

/**
 * Load a promotion voucher logo (circle-cropped) into this ImageView.
 *
 * @param urlLogo     Remote URL of the logo.
 * @param preferCache See [loadWithCacheStrategy].
 */
internal fun ImageView.loadPromotionVoucherLogo(urlLogo: String, preferCache: Boolean = false) =
    loadWithCacheStrategy(
        url = urlLogo,
        baseOptions = promotionVoucherLogoBaseOptions(context),
        uiOptions = promotionVoucherLogoUiOptions(context),
        fallbackRes = R.drawable.prm_bg_image_placeholder_circle,
        preferCache = preferCache,
    )

/**
 * Load a promotion voucher banner into this ImageView.
 *
 * @param urlBanner   Remote URL of the banner.
 * @param preferCache See [loadWithCacheStrategy].
 */
internal fun ImageView.loadPromotionVoucherBanner(urlBanner: String, preferCache: Boolean = false) =
    loadWithCacheStrategy(
        url = urlBanner,
        baseOptions = promotionVoucherBannerBaseOptions(context),
        uiOptions = promotionVoucherBannerUiOptions(context),
        fallbackRes = R.drawable.prm_bg_image_placeholder,
        preferCache = preferCache,
    )