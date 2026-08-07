package com.ttcn.promotionsdk

import com.ttcn.promotionsdk.config.PromotionRequestContextProvider
import com.ttcn.promotionsdk.data.remote.KtorPromotionApiService
import com.ttcn.promotionsdk.data.remote.PromotionHttpClient
import com.ttcn.promotionsdk.data.remote.PromotionRemoteDataSource
import com.ttcn.promotionsdk.data.repository.PromotionRepositoryImpl
import com.ttcn.promotionsdk.domain.model.voucher.SearchCustomerVouchersRequest
import com.ttcn.promotionsdk.domain.usecase.GetCustomerVoucherDetailUseCase
import com.ttcn.promotionsdk.domain.usecase.SearchCustomerVouchersUseCase
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Nhánh field-theo-field của `VoucherMapper`: brand/logo/codes có và không có.
 * `logo` lấy phần tử ĐẦU của mảng, `codes` loại mã rỗng — hai chỗ dễ sai âm thầm.
 */
class VoucherDetailFieldBranchTest {

    private class NoCtx : PromotionRequestContextProvider
    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

    private fun repo(json: String) = PromotionRepositoryImpl(
        PromotionRemoteDataSource(
            KtorPromotionApiService(
                HttpClient(MockEngine { respond(json, HttpStatusCode.OK, jsonHeaders) }) {
                    with(PromotionHttpClient) { configure("https://api.example.com", NoCtx(), false) }
                },
            ),
        ),
    )

    @Test
    fun detail_fullVoucher_mapsBrandLogoAndCodes() = runTest {
        val json = """
        {"status":200,"data":{
          "voucher":{"id":"c1","title":"Tieu de","description":"Mo ta ngan","content":"Noi dung chi tiet",
                     "guideline":"Huong dan",
                     "image":"http://banner",
                     "brand":{"name":"Thuong hieu","logo":["http://logo1","http://logo2"]}},
          "startDate":"2020-01-01","endDate":"2099-01-01",
          "codes":[{"codex":"CODE1"},{"codex":"  "},{"codex":"CODE2"}],
          "metadata":{"usable":"true","usageGuideUrl":"http://guide"}
        }}
        """.trimIndent()

        val d = GetCustomerVoucherDetailUseCase(repo(json)).invoke("v", null)!!
        assertEquals("Thuong hieu", d.merchantName)
        assertEquals("http://logo1", d.logo)       // lấy phần tử ĐẦU của mảng logo
        assertEquals("http://banner", d.banner)
        assertEquals("Tieu de", d.title)
        assertEquals("Noi dung chi tiet", d.description)
        assertEquals("Huong dan", d.guideline)
        assertEquals("2099-01-01", d.expirationDate)
        assertEquals("http://guide", d.usageGuideUrl)
        assertEquals(listOf("CODE1", "CODE2"), d.codes)   // mã trắng bị loại
        assertEquals("c1", d.campaignId)
    }

    @Test
    fun detail_guidelineMissing_isNull() = runTest {
        val json = """
        {"status":200,"data":{"voucher":{"id":"c1","content":"Noi dung cu, khong con dung"}}}
        """.trimIndent()
        val d = GetCustomerVoucherDetailUseCase(repo(json)).invoke("v", null)!!
        assertNull(d.guideline)
    }

    @Test
    fun detail_minimalVoucher_leavesOptionalFieldsNull() = runTest {
        val json = """{"status":200,"data":{"voucher":{"id":"c9"}}}"""

        val d = GetCustomerVoucherDetailUseCase(repo(json)).invoke("v", null)!!
        assertNull(d.merchantName)     // không có brand
        assertNull(d.logo)
        assertNull(d.banner)
        assertNull(d.title)
        assertNull(d.usageGuideUrl)
        assertTrue(d.codes.isEmpty())
        assertEquals("c9", d.campaignId)
    }

    @Test
    fun detail_brandWithoutLogoArray_givesNullLogo() = runTest {
        val json = """{"status":200,"data":{"voucher":{"id":"c1","brand":{"name":"B"}}}}"""
        val d = GetCustomerVoucherDetailUseCase(repo(json)).invoke("v", null)!!
        assertEquals("B", d.merchantName)
        assertNull(d.logo)
    }

    @Test
    fun detail_emptyLogoArray_givesNullLogo() = runTest {
        val json = """{"status":200,"data":{"voucher":{"id":"c1","brand":{"name":"B","logo":[]}}}}"""
        assertNull(GetCustomerVoucherDetailUseCase(repo(json)).invoke("v", null)!!.logo)
    }

    @Test
    fun detail_allCodesBlank_givesEmptyList() = runTest {
        val json = """{"status":200,"data":{"voucher":{"id":"c1"},"codes":[{"codex":""},{"codex":"   "}]}}"""
        assertTrue(GetCustomerVoucherDetailUseCase(repo(json)).invoke("v", null)!!.codes.isEmpty())
    }

    // ─── Danh sách: pagination có / vắng ─────────────────────────────────────

    @Test
    fun search_fullPagination_isMapped() = runTest {
        val json = """
        {"status":200,"data":{"content":[{"voucher":{"id":"v1","title":"T"}}],
         "number":2,"size":20,"last":false,"totalElements":99,"expireWarningDate":5,
         "selectedTab":"all","tabs":[{"code":"all","label":"Tat ca","count":9,"order":1,"default":true}]}}
        """.trimIndent()

        val r = SearchCustomerVouchersUseCase(repo(json)).invoke(SearchCustomerVouchersRequest())!!
        assertEquals(2, r.number)
        assertEquals(20, r.size)
        assertEquals(false, r.last)
        assertEquals(99, r.totalElements)
        assertEquals(5, r.expireWarningDate)
        assertEquals("all", r.selectedTab)
        assertEquals(1, r.tabs.size)
        assertTrue(r.tabs.single().isDefault)
    }

    @Test
    fun search_withoutPagination_leavesNulls() = runTest {
        val json = """{"status":200,"data":{"content":[]}}"""
        val r = SearchCustomerVouchersUseCase(repo(json)).invoke(SearchCustomerVouchersRequest())!!
        assertNull(r.number)
        assertNull(r.last)
        assertNull(r.totalElements)
        assertNull(r.expireWarningDate)
        assertTrue(r.tabs.isEmpty())
    }
}
