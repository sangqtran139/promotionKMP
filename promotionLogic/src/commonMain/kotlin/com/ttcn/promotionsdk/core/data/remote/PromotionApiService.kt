package com.ttcn.promotionsdk.core.data.remote

import com.ttcn.promotionsdk.core.data.dto.eligible.EligibleCampaignsRequest
import com.ttcn.promotionsdk.core.data.dto.eligible.EligibleCampaignsResponse
import com.ttcn.promotionsdk.core.data.dto.redemption.RedemptionSessionRequest
import com.ttcn.promotionsdk.core.data.dto.redemption.RedemptionSessionResponse
import com.ttcn.promotionsdk.core.data.dto.stackablediscount.StackableDiscountsRequest
import com.ttcn.promotionsdk.core.data.dto.stackablediscount.StackableDiscountsResponse
import com.ttcn.promotionsdk.core.data.dto.voucher.CustomerVoucherDetail
import com.ttcn.promotionsdk.core.data.dto.voucher.SearchCustomerVouchersResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

internal interface PromotionApiService {

    suspend fun searchCustomerVouchers(
        customerId: String,
        keyword: String?,
        serviceCode: String?,
        tab: String?,
        page: Int?,
        size: Int?,
    ): ApiResponseTemplate<SearchCustomerVouchersResponse>

    suspend fun getCustomerVoucherDetail(
        voucherId: String,
        customerId: String,
        service: String?,
    ): ApiResponseTemplate<CustomerVoucherDetail>

    suspend fun createRedemptionSession(
        request: RedemptionSessionRequest,
    ): ApiResponseTemplate<RedemptionSessionResponse>

    suspend fun validateStackableDiscounts(
        request: StackableDiscountsRequest,
    ): ApiResponseTemplate<StackableDiscountsResponse>

    suspend fun findEligibleCampaigns(
        request: EligibleCampaignsRequest,
    ): ApiResponseTemplate<EligibleCampaignsResponse>
}

/**
 * Bản Ktor của [PromotionApiService]. Thay cho interface `@GET`/`@POST` mà Retrofit sinh
 * implementation lúc runtime — Kotlin/Native không có dynamic proxy nên phải viết tay.
 *
 * `parameter(...)` tự bỏ qua giá trị null, giữ đúng hành vi `@Query` của Retrofit.
 */
internal class KtorPromotionApiService(
    private val client: HttpClient,
) : PromotionApiService {

    override suspend fun searchCustomerVouchers(
        customerId: String,
        keyword: String?,
        serviceCode: String?,
        tab: String?,
        page: Int?,
        size: Int?,
    ): ApiResponseTemplate<SearchCustomerVouchersResponse> =
        client.get("$BASE_PATH/customer-vouchers") {
            parameter("customerId", customerId)
            parameter("keyword", keyword)
            parameter("serviceCode", serviceCode)
            parameter("tab", tab)
            parameter("page", page)
            parameter("size", size)
        }.body()

    override suspend fun getCustomerVoucherDetail(
        voucherId: String,
        customerId: String,
        service: String?,
    ): ApiResponseTemplate<CustomerVoucherDetail> =
        client.get("$BASE_PATH/customer-vouchers/$voucherId") {
            parameter("customerId", customerId)
            parameter("service", service)
        }.body()

    override suspend fun createRedemptionSession(
        request: RedemptionSessionRequest,
    ): ApiResponseTemplate<RedemptionSessionResponse> =
        client.post("$BASE_PATH/redemptions/sessions") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    override suspend fun validateStackableDiscounts(
        request: StackableDiscountsRequest,
    ): ApiResponseTemplate<StackableDiscountsResponse> =
        client.post("$BASE_PATH/redemptions/validate/stackable-discounts") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    // Lưu ý: endpoint này là `redemption` số ít, khác `redemptions` của hai API trên.
    override suspend fun findEligibleCampaigns(
        request: EligibleCampaignsRequest,
    ): ApiResponseTemplate<EligibleCampaignsResponse> =
        client.post("$BASE_PATH/redemption/eligible") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    private companion object {
        const val BASE_PATH = "promotion/promotion-bff-mobile/v1"
    }
}
