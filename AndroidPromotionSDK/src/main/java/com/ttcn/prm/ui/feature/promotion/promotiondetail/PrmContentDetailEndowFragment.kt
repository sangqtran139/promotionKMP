package com.ttcn.prm.ui.feature.promotion.promotiondetail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.ttcn.prm.databinding.FragmentContentDetailEndowPrmBinding
import com.ttcn.prm.ui.base.PRMBaseFragment
import com.ttcn.promotionsdk.presentation.promotiondetail.wrapPromotionHtml

class PrmContentDetailEndowFragment :
    PRMBaseFragment<FragmentContentDetailEndowPrmBinding>() {

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
    ): FragmentContentDetailEndowPrmBinding {
        return FragmentContentDetailEndowPrmBinding.inflate(inflater, container, false)
    }

    override fun setupUI() {
        val htmlContent = arguments?.getString(ARG_CONTENT).orEmpty()
        initWebView()
        // Bọc HTML bằng hàm DÙNG CHUNG ở promotionLogic (iOS nạp đúng chuỗi này vào WKWebView).
        binding.wvContent.loadDataWithBaseURL(
            null,
            wrapPromotionHtml(htmlContent),
            MIME_TYPE_HTML,
            ENCODING_UTF8,
            null,
        )
    }

    private fun initWebView() {
        with(binding.wvContent.settings) {
            allowFileAccess = true
            javaScriptEnabled = true
            domStorageEnabled = true
            defaultFontSize = 14
        }

        binding.wvContent.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest,
            ): Boolean = true
        }
    }

    companion object {
        private const val ARG_CONTENT = "prm_detail_html_content"
        private const val MIME_TYPE_HTML = "text/html"
        private const val ENCODING_UTF8 = "UTF-8"

        fun newInstance(content: String): PrmContentDetailEndowFragment {
            return PrmContentDetailEndowFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_CONTENT, content)
                }
            }
        }
    }
}
