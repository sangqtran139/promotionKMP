package com.ttcn.promotionsdk.core.data.remote

import com.ttcn.promotionsdk.core.data.dto.redemption.RedemptionSessionRequest
import com.ttcn.promotionsdk.core.data.dto.redemption.RedemptionSessionResponse
import com.ttcn.promotionsdk.core.data.dto.stackablediscount.StackableDiscountsRequest
import com.ttcn.promotionsdk.core.data.dto.stackablediscount.StackableDiscountsResponse
import com.ttcn.promotionsdk.core.data.dto.voucher.CustomerVoucherDetail
import com.ttcn.promotionsdk.core.data.dto.voucher.SearchCustomerVouchersResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

internal interface PromotionApiService {

    @GET("promotion/promotion-vtm-bff/api/v1/vtm/customer-vouchers")
    suspend fun searchCustomerVouchers(
        @Query("customerId") customerId: String,
        @Query("keyword") keyword: String?,
        @Query("serviceCode") serviceCode: String?,
        @Query("tab") tab: String?,
        @Query("sectionCode") sectionCode: String?,
        @Query("myVouchers.page") myVouchersPage: Int?,
        @Query("myVouchers.size") myVouchersSize: Int?,
        @Query("otherVouchers.page") otherVouchersPage: Int?,
        @Query("otherVouchers.size") otherVouchersSize: Int?,
    ): ApiResponseTemplate<SearchCustomerVouchersResponse>

    @GET("promotion/promotion-vtm-bff/api/v1/vtm/customer-vouchers/{voucherId}")
    suspend fun getCustomerVoucherDetail(
        @Path("voucherId") voucherId: String,
        @Query("customerId") customerId: String,
        @Query("service") service: String?,
    ): ApiResponseTemplate<CustomerVoucherDetail>

    @POST("promotion/promotion-vtm-bff/api/v1/vtm/redemptions/sessions")
    suspend fun createRedemptionSession(
        @Body request: RedemptionSessionRequest,
    ): ApiResponseTemplate<RedemptionSessionResponse>

    @POST("promotion/promotion-vtm-bff/api/v1/vtm/redemptions/validate/stackable-discounts")
    suspend fun validateStackableDiscounts(
        @Body request: StackableDiscountsRequest,
    ): ApiResponseTemplate<StackableDiscountsResponse>
}
