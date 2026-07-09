package com.ttcn.promotionsdk.ui.utils

import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.ttcn.promotionsdk.R

// ─── Constants ───────────────────────────────────────────────────────────────

private const val PROMO_LOGO_DECODE_SIZE = 200
private const val PROMO_BANNER_DECODE_W = 1200
private const val PROMO_BANNER_DECODE_H = 600

// ─── Options ─────────────────────────────────────────────────────────────────

private fun promotionVoucherLogoBaseOptions(): RequestOptions =
    RequestOptions()
        .override(PROMO_LOGO_DECODE_SIZE, PROMO_LOGO_DECODE_SIZE)
        .diskCacheStrategy(DiskCacheStrategy.ALL)
        .circleCrop()

private fun promotionVoucherLogoUiOptions(): RequestOptions =
    promotionVoucherLogoBaseOptions()
        .placeholder(R.drawable.prm_bg_image_placeholder)
        .error(R.drawable.prm_bg_image_placeholder)

private fun promotionVoucherBannerBaseOptions(): RequestOptions =
    RequestOptions()
        .override(PROMO_BANNER_DECODE_W, PROMO_BANNER_DECODE_H)
        .diskCacheStrategy(DiskCacheStrategy.ALL)

private fun promotionVoucherBannerUiOptions(): RequestOptions =
    promotionVoucherBannerBaseOptions()
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

    if (!preferCache) {
        Glide.with(this)
            .load(url)
            .apply(uiOptions)
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(this)
        return
    }

    Glide.with(this)
        .load(url)
        .apply(baseOptions)
        .onlyRetrieveFromCache(true)
        .dontAnimate()
        .error(
            Glide.with(this)
                .load(url)
                .apply(uiOptions)
                .transition(DrawableTransitionOptions.withCrossFade())
        )
        .into(this)
}

/**
 * Load a promotion voucher logo (circle-cropped) into this ImageView.
 *
 * @param urlLogo     Remote URL of the logo.
 * @param preferCache See [loadWithCacheStrategy].
 */
fun ImageView.loadPromotionVoucherLogo(urlLogo: String, preferCache: Boolean = false) =
    loadWithCacheStrategy(
        url = urlLogo,
        baseOptions = promotionVoucherLogoBaseOptions(),
        uiOptions = promotionVoucherLogoUiOptions(),
        fallbackRes = R.drawable.prm_bg_image_placeholder,
        preferCache = preferCache,
    )

/**
 * Load a promotion voucher banner into this ImageView.
 *
 * @param urlBanner   Remote URL of the banner.
 * @param preferCache See [loadWithCacheStrategy].
 */
fun ImageView.loadPromotionVoucherBanner(urlBanner: String, preferCache: Boolean = false) =
    loadWithCacheStrategy(
        url = urlBanner,
        baseOptions = promotionVoucherBannerBaseOptions(),
        uiOptions = promotionVoucherBannerUiOptions(),
        fallbackRes = R.drawable.prm_bg_image_placeholder,
        preferCache = preferCache,
    )