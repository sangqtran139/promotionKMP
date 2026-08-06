package com.ttcn.promotionsdk.core

import com.ttcn.promotionsdk.core.config.EmptyPromotionRequestContextProvider
import com.ttcn.promotionsdk.core.data.local.FeatureFlagLocalDataSource
import com.ttcn.promotionsdk.core.data.local.PromotionPreferences
import com.ttcn.promotionsdk.core.data.remote.KtorFeatureFlagApiService
import com.ttcn.promotionsdk.core.data.remote.PromotionHttpClient
import com.ttcn.promotionsdk.core.data.remote.FeatureFlagRemoteDataSource
import com.ttcn.promotionsdk.core.data.repository.FeatureFlagRepositoryImpl
import com.ttcn.promotionsdk.core.domain.model.featureflag.PromotionFeatureFlag
import com.ttcn.promotionsdk.core.domain.usecase.FetchFeatureFlagsUseCase
import com.ttcn.promotionsdk.core.domain.usecase.GetFeatureFlagsUseCase
import com.ttcn.promotionsdk.core.domain.usecase.GetPromotionFeatureFlagsUseCase
import com.ttcn.promotionsdk.core.domain.usecase.IsFeatureEnabledUseCase
import com.ttcn.promotionsdk.core.domain.usecase.PromotionFeatureFlagUseCases
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private class InMemoryStorage : PromotionPreferences {
    private val map = mutableMapOf<String, Any>()
    override fun putBoolean(key: String, value: Boolean) { map[key] = value }
    override fun getBoolean(key: String, default: Boolean): Boolean = map[key] as? Boolean ?: default
    override fun putString(key: String, value: String) { map[key] = value }
    override fun getString(key: String): String? = map[key] as? String
    override fun contains(key: String): Boolean = map.containsKey(key)
    override fun remove(key: String) { map.remove(key) }
    override fun clear() = map.clear()
}

private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

private fun featureFlags(
    storage: PromotionPreferences,
    handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
): PromotionFeatureFlagUseCases {
    val client = HttpClient(MockEngine(handler)) {
        with(PromotionHttpClient) {
            configure("https://api.example.com", EmptyPromotionRequestContextProvider(), isDebug = false)
        }
    }
    val repo = FeatureFlagRepositoryImpl(
        remoteDataSource = FeatureFlagRemoteDataSource(KtorFeatureFlagApiService(client)),
        localDataSource = FeatureFlagLocalDataSource(storage),
    )
    return PromotionFeatureFlagUseCases(
        FetchFeatureFlagsUseCase(repo),
        IsFeatureEnabledUseCase(repo),
        GetFeatureFlagsUseCase(repo),
        GetPromotionFeatureFlagsUseCase(repo),
    )
}

private fun flagsResponse(enableAll: Boolean, voucherList: Boolean) = """
{"success":true,"data":[
  {"flagName":"PROMOTION.ENABLE_ALL","enabled":$enableAll},
  {"flagName":"PROMOTION.VOUCHER_LIST","enabled":$voucherList},
  {"flagName":"PROMOTION.VOUCHER_APPLY","enabled":true}
]}
"""

class FeatureFlagTest {

    @Test
    fun withoutCache_defaultsToAllEnabled() {
        val flags = featureFlags(InMemoryStorage()) { respond("", HttpStatusCode.OK) }

        assertTrue(flags.all().enableAll)
        assertTrue(flags.isEnabled(PromotionFeatureFlag.VOUCHER_LIST))
    }

    @Test
    fun refresh_persistsFlagsAndUpdatesCache() = runTest {
        val storage = InMemoryStorage()
        val flags = featureFlags(storage) {
            respond(flagsResponse(enableAll = true, voucherList = false), HttpStatusCode.OK, jsonHeaders)
        }

        flags.refresh()

        assertTrue(flags.isEnabled(PromotionFeatureFlag.VOUCHER_APPLY))
        assertFalse(flags.isEnabled(PromotionFeatureFlag.VOUCHER_LIST))
        // Ghi xuống storage để lần mở app sau không phải chờ API.
        assertTrue(storage.contains("feature_flag_has_cache"))
        assertFalse(storage.getBoolean(PromotionFeatureFlag.VOUCHER_LIST, default = true))
    }

    @Test
    fun enableAllOff_disablesEveryChildFlag() = runTest {
        val flags = featureFlags(InMemoryStorage()) {
            respond(flagsResponse(enableAll = false, voucherList = true), HttpStatusCode.OK, jsonHeaders)
        }

        flags.refresh()

        assertFalse(flags.isEnabled(PromotionFeatureFlag.VOUCHER_LIST))
        assertFalse(flags.isEnabled(PromotionFeatureFlag.VOUCHER_APPLY))
    }

    @Test
    fun refresh_onHttpError_keepsPreviousFlags_andDoesNotThrow() = runTest {
        val storage = InMemoryStorage()
        FeatureFlagLocalDataSource(storage).save(
            com.ttcn.promotionsdk.core.domain.model.featureflag.PromotionFeatureFlags(
                enableAll = true, voucherApply = true, voucherRedeem = false,
                voucherSelection = false, voucherDetail = false, voucherList = true,
            )
        )
        val flags = featureFlags(storage) { respondError(HttpStatusCode.InternalServerError) }

        flags.refresh() // không được ném

        assertTrue(flags.isEnabled(PromotionFeatureFlag.VOUCHER_LIST))
        assertFalse(flags.isEnabled(PromotionFeatureFlag.VOUCHER_REDEEM))
    }

    /**
     * `all()` thoát ra tới host, nên đọc thẳng field phải cho kết quả giống `isEnabled`: không được
     * để `voucherList = true` trong khi công tắc tổng đang tắt.
     */
    @Test
    fun all_appliesEnableAll_soRawFieldsMatchIsEnabled() = runTest {
        val flags = featureFlags(InMemoryStorage()) {
            respond(flagsResponse(enableAll = false, voucherList = true), HttpStatusCode.OK, jsonHeaders)
        }

        flags.refresh()

        val snapshot = flags.all()
        assertFalse(snapshot.enableAll)
        assertFalse(snapshot.voucherList)
        assertFalse(snapshot.voucherApply)
        for (name in listOf(
            PromotionFeatureFlag.ENABLE_ALL,
            PromotionFeatureFlag.VOUCHER_LIST,
            PromotionFeatureFlag.VOUCHER_APPLY,
        )) {
            assertEquals(flags.isEnabled(name), snapshot.isEnabled(name), name)
        }
    }

    /** Công tắc tổng bật thì chuẩn hoá không được đụng vào giá trị riêng của từng cờ con. */
    @Test
    fun all_keepsPerFlagValues_whenEnableAllOn() = runTest {
        val flags = featureFlags(InMemoryStorage()) {
            respond(flagsResponse(enableAll = true, voucherList = false), HttpStatusCode.OK, jsonHeaders)
        }

        flags.refresh()

        val snapshot = flags.all()
        assertTrue(snapshot.enableAll)
        assertFalse(snapshot.voucherList)
        assertTrue(snapshot.voucherApply)
    }

    @Test
    fun flagsOf_returnsNameEnabledPairs() = runTest {
        val flags = featureFlags(InMemoryStorage()) {
            respond(flagsResponse(enableAll = true, voucherList = false), HttpStatusCode.OK, jsonHeaders)
        }
        flags.refresh()

        val result = flags.flagsOf(
            listOf(PromotionFeatureFlag.VOUCHER_APPLY, PromotionFeatureFlag.VOUCHER_LIST)
        )

        assertEquals(2, result.size)
        assertTrue(result.first { it.name == PromotionFeatureFlag.VOUCHER_APPLY }.enabled)
        assertFalse(result.first { it.name == PromotionFeatureFlag.VOUCHER_LIST }.enabled)
    }
}
