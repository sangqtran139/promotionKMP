package com.ttcn.promotionsdk.ui.feature.promotion.promotiondetail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.ttcn.promotionsdk.core.utils.PRMSimpleSpanBuilder
import com.ttcn.promotionsdk.databinding.FragmentContentDetailEndowPrmBinding
import com.ttcn.promotionsdk.ui.base.PRMBaseFragment

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
        binding.wvContent.loadDataWithBaseURL(
            null,
            buildHtmlContent(htmlContent),
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

    private fun buildHtmlContent(content: String): String {
        val simpleSpanBuilder = PRMSimpleSpanBuilder()

        simpleSpanBuilder.append(
            """
            <HTML>
            <HEAD>
                <LINK href="detail_endow.css" type="text/css" rel="stylesheet"/>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
            </HEAD>
            <body>
            """.trimIndent(),
        )

        simpleSpanBuilder.append(content)
        simpleSpanBuilder.append("</body></HTML>")

        return simpleSpanBuilder.build()
            .toString()
            .replace("style=\"width:(| )\\w{1,}pt;\"".toRegex(), " ")
            .replace("width=\"\\w{1,}\"".toRegex(), " ")
            .replace("width:\\w{1,}pt".toRegex(), "word-wrap: break-word")
            .replace("width: \\w{1,}pt".toRegex(), "word-wrap: break-word")
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
