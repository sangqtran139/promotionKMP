package com.ttcn.promotionsdk.app.mock.promotion

import android.util.Log
import okhttp3.HttpUrl
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

internal class PromotionMockDispatcher : Dispatcher() {

    override fun dispatch(request: RecordedRequest): MockResponse {
        val method = request.method.orEmpty()
        val path = request.path.orEmpty()
        val query = request.requestUrl?.query.orEmpty()
        Log.d(TAG, "Incoming request method=$method path=$path query=$query")

        if (request.method != GET_METHOD) {
            return notFound(path = path)
        }

        val url = request.requestUrl ?: return notFound(path = path)
        val pathSegments = url.pathSegments.filter { it.isNotBlank() }
        val voucherIndex = pathSegments.indexOfLast { it == PATH_CUSTOMER_VOUCHERS }
        if (voucherIndex == -1) return notFound(path = path)

        val hasVoucherId = voucherIndex < pathSegments.lastIndex
        return if (hasVoucherId) {
            handleDetail(url, pathSegments[voucherIndex + 1], path)
        } else {
            handleSearch(url, path)
        }
    }

    private fun handleSearch(url: HttpUrl, path: String): MockResponse {
        val keyword = url.queryParameter("keyword").orEmpty().trim()
        val requestedTab =
            url.queryParameter("tab").orEmpty().trim().ifBlank { PromotionMockData.TAB_ALL }
        val myPage = url.queryParameter("myVouchers.page")?.toIntOrNull() ?: 0
        val mySize =
            max(url.queryParameter("myVouchers.size")?.toIntOrNull() ?: DEFAULT_PAGE_SIZE, 1)

        if (keyword.length == 1) {
            Log.d(
                TAG,
                "SEARCH route matched, page=$myPage, size=$mySize, tab=$requestedTab, contentSize=0, status=400"
            )
            return errorResponse(
                status = 400,
                code = "KEYWORD_TOO_SHORT",
                message = "Keyword must have at least 2 characters",
            )
        }

        val keywordFiltered = filterByKeyword(PromotionMockData.vouchers, keyword)
        val tabFiltered = filterByTab(keywordFiltered, requestedTab)
        val page = buildPage(tabFiltered, myPage, mySize)
        val tabsJson = buildTabs(keywordFiltered, requestedTab)
        val itemsWithDescription = page.content.count { containsHtml(it.description) }

        val body = """
            {
              "status": 200,
              "code": "SUCCESS",
              "success": true,
              "message": "OK",
              "data": {
                "mode": "TAB",
                "keyword": "${escape(keyword)}",
                "serviceCode": ${nullableString(url.queryParameter("serviceCode"))},
                "defaultTab": "${PromotionMockData.TAB_ALL}",
                "selectedTab": "${escape(requestedTab)}",
                "tabs": $tabsJson,
                "myVouchers": ${buildPageJson(page)},
                "otherVouchers": ${buildPageJson(buildPage(emptyList(), 0, mySize))}
              }
            }
        """.trimIndent()
        Log.d(
            TAG,
            "SEARCH route matched, page=$myPage, size=$mySize, tab=$requestedTab, contentSize=${page.content.size}, itemsWithDescription=$itemsWithDescription, status=200, path=$path"
        )
        return success(body)
    }

    private fun handleDetail(url: HttpUrl, voucherId: String, path: String): MockResponse {
        val customerId = url.queryParameter("customerId")?.trim().orEmpty()
        if (customerId.isBlank()) {
            Log.d(TAG, "DETAIL route matched, voucherId=$voucherId, status=400, path=$path")
            return errorResponse(400, "MISSING_CUSTOMER_ID", "Missing customerId")
        }
        if (customerId == "forbidden") {
            Log.d(TAG, "DETAIL route matched, voucherId=$voucherId, status=403, path=$path")
            return errorResponse(403, "VOUCHER_NOT_OWNED", "Voucher not owned by customer")
        }

        val voucher = PromotionMockData.vouchers.firstOrNull { it.voucherId == voucherId }
            ?: run {
                Log.d(TAG, "DETAIL route matched, voucherId=$voucherId, status=404, path=$path")
                return errorResponse(404, "VOUCHER_NOT_FOUND", "Voucher not found")
            }

        val body = """
            {
              "status": 200,
              "code": "SUCCESS",
              "success": true,
              "message": "OK",
              "data": {
                "voucherId": "${voucher.voucherId}",
                "customerId": "${escape(customerId)}",
                "merchantName": "${escape(voucher.merchantName)}",
                "logo": "${escape(voucher.logo)}",
                "banner": "${escape(voucher.banner)}",
                "title": "${escape(voucher.title)}",
                "description": "${escape(voucher.description)}",
                "guideline": "${escape(voucher.guideline)}",
                "startDate": "${voucher.startDate}",
                "expirationDate": "${voucher.expirationDate}",
                "timeSlot": "${escape(voucher.timeSlot)}",
                "status": "${voucher.status}",
                "displayStatusLabel": "${escape(voucher.displayStatusLabel)}",
                "discountType": {
                  "discountType": "VOUCHER",
                  "discountMethod": "${escape(voucher.discountMethod)}",
                  "discountValue": "${escape(voucher.discountValue)}",
                  "discountPercentage": "${escape(voucher.discountPercentage)}",
                  "includedProducts": ${stringArray(voucher.includedProducts)},
                  "excludedProducts": ${stringArray(voucher.excludedProducts)}
                },
                "conditions": {
                  "validationRules": [
                    {
                      "ruleCode": "MIN_ORDER",
                      "description": "Ap dung cho hoa don tu 100000d"
                    },
                    {
                      "ruleCode": "ONE_TIME",
                      "description": "Moi khach hang su dung 1 lan"
                    }
                  ]
                }
              }
            }
        """.trimIndent()
        val hasDescriptionHtml = containsHtml(voucher.description)
        val hasGuidelineHtml = containsHtml(voucher.guideline)
        Log.d(
            TAG,
            "DETAIL route matched, voucherId=$voucherId, status=200, hasDescriptionHtml=$hasDescriptionHtml, hasGuidelineHtml=$hasGuidelineHtml, path=$path"
        )
        return success(body)
    }

    private fun buildTabs(source: List<MockVoucher>, selectedTab: String): String {
        val tabs = listOf(
            TabMeta(PromotionMockData.TAB_ALL, "Tất cả", source.size, 0),
            TabMeta(
                PromotionMockData.TAB_EXPIRING_SOON,
                "Sắp hết hạn",
                filterByTab(source, PromotionMockData.TAB_EXPIRING_SOON).size,
                1
            ),
            TabMeta(
                PromotionMockData.TAB_ACTIVE,
                "Đang hoạt động",
                filterByTab(source, PromotionMockData.TAB_ACTIVE).size,
                2
            ),
            TabMeta(
                PromotionMockData.TAB_REDEEMED,
                "Đã sử dụng",
                filterByTab(source, PromotionMockData.TAB_REDEEMED).size,
                3
            ),
            TabMeta(
                PromotionMockData.TAB_EXPIRED,
                "Hết hạn",
                filterByTab(source, PromotionMockData.TAB_EXPIRED).size,
                4
            ),
        )
        return tabs.joinToString(prefix = "[", postfix = "]") { tab ->
            """
            {
              "code": "${tab.code}",
              "label": "${tab.label}",
              "count": ${tab.count},
              "order": ${tab.order},
              "selected": ${tab.code == selectedTab}
            }
            """.trimIndent()
        }
    }

    private fun filterByKeyword(source: List<MockVoucher>, keyword: String): List<MockVoucher> {
        if (keyword.isBlank()) return source
        val lookup = keyword.lowercase()
        return source.filter { voucher ->
            voucher.voucherId.lowercase().contains(lookup) ||
                    voucher.title.lowercase().contains(lookup) ||
                    voucher.merchantName.lowercase().contains(lookup) ||
                    voucher.description.lowercase().contains(lookup)
        }
    }

    private fun filterByTab(source: List<MockVoucher>, tab: String): List<MockVoucher> {
        return when (tab) {
            PromotionMockData.TAB_ALL -> source
            PromotionMockData.TAB_EXPIRING_SOON -> source.filter {
                it.status == "ACTIVE" && it.expirationDate <= PromotionMockData.EXPIRING_SOON_END
            }

            PromotionMockData.TAB_ACTIVE -> source.filter { it.status == "ACTIVE" }
            PromotionMockData.TAB_REDEEMED -> source.filter { it.status == "REDEEMED" }
            PromotionMockData.TAB_EXPIRED -> source.filter { it.status == "EXPIRED" }
            else -> emptyList()
        }
    }

    private fun buildPage(
        items: List<MockVoucher>,
        requestPage: Int,
        requestSize: Int
    ): PageResult {
        val safePage = max(requestPage, 0)
        val totalElements = items.size
        val totalPages =
            if (totalElements == 0) 0 else ceil(totalElements / requestSize.toDouble()).toInt()
        val fromIndex = safePage * requestSize
        val content = if (fromIndex >= totalElements) {
            emptyList()
        } else {
            val toIndex = min(fromIndex + requestSize, totalElements)
            items.subList(fromIndex, toIndex)
        }
        val isEmpty = content.isEmpty()
        val isFirst = safePage == 0
        val isLast = totalElements == 0 || fromIndex + requestSize >= totalElements
        return PageResult(
            content = content,
            totalElements = totalElements,
            totalPages = totalPages,
            first = isFirst,
            last = isLast,
            size = requestSize,
            number = safePage,
            numberOfElements = content.size,
            empty = isEmpty,
        )
    }

    private fun buildPageJson(page: PageResult): String {
        val contentJson = page.content.joinToString(prefix = "[", postfix = "]") { voucher ->
            """
            {
              "voucherId": "${voucher.voucherId}",
              "merchantName": "${escape(voucher.merchantName)}",
              "title": "${escape(voucher.title)}",
              "description": "${escape(voucher.description)}",
              "logo": "${escape(voucher.logo)}",
              "startDate": "${voucher.startDate}",
              "expirationDate": "${voucher.expirationDate}",
              "timeSlot": "${escape(voucher.timeSlot)}",
              "status": "${voucher.status}",
              "displayStatusLabel": "${escape(voucher.displayStatusLabel)}",
              "campaignId": "${escape(voucher.campaignId)}",
              "campaignType": "${escape(voucher.campaignType)}"
            }
            """.trimIndent()
        }
        return """
            {
              "content": $contentJson,
              "totalElements": ${page.totalElements},
              "totalPages": ${page.totalPages},
              "first": ${page.first},
              "last": ${page.last},
              "size": ${page.size},
              "number": ${page.number},
              "numberOfElements": ${page.numberOfElements},
              "empty": ${page.empty}
            }
        """.trimIndent()
    }

    private fun stringArray(values: List<String>): String {
        return values.joinToString(prefix = "[", postfix = "]") { "\"${escape(it)}\"" }
    }

    private fun nullableString(value: String?): String =
        value?.let { "\"${escape(it)}\"" } ?: "null"

    private fun containsHtml(value: String): Boolean {
        return value.contains('<') && value.contains('>')
    }

    private fun success(body: String): MockResponse {
        return MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody(body)
    }

    private fun errorResponse(status: Int, code: String, message: String): MockResponse {
        val body = """
            {
              "status": $status,
              "code": "$code",
              "success": false,
              "message": "$message",
              "data": null,
              "timestamp": "${OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)}"
            }
        """.trimIndent()
        return MockResponse()
            .setResponseCode(status)
            .setHeader("Content-Type", "application/json")
            .setBody(body)
    }

    private fun notFound(path: String): MockResponse {
        Log.d(TAG, "NOT_FOUND route, path=$path")
        return errorResponse(404, "NOT_FOUND", "Mock route not found")
    }

    private fun escape(raw: String): String {
        return raw
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
    }

    private data class TabMeta(
        val code: String,
        val label: String,
        val count: Int,
        val order: Int,
    )

    private data class PageResult(
        val content: List<MockVoucher>,
        val totalElements: Int,
        val totalPages: Int,
        val first: Boolean,
        val last: Boolean,
        val size: Int,
        val number: Int,
        val numberOfElements: Int,
        val empty: Boolean,
    )

    private companion object {
        const val TAG = "PromotionMockApi"
        const val GET_METHOD = "GET"
        const val PATH_CUSTOMER_VOUCHERS = "customer-vouchers"
        const val DEFAULT_PAGE_SIZE = 10
    }
}
