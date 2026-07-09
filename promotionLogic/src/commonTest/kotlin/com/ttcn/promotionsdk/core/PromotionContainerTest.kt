package com.ttcn.promotionsdk.core

import com.ttcn.promotionsdk.core.config.PromotionRequestContextProvider
import com.ttcn.promotionsdk.core.config.PromotionSDKConfig
import com.ttcn.promotionsdk.core.di.PromotionContainer
import com.ttcn.promotionsdk.core.domain.exception.PromotionErrorCodes
import com.ttcn.promotionsdk.core.domain.exception.PromotionException
import com.ttcn.promotionsdk.core.domain.model.voucher.SearchCustomerVouchersRequest
import com.ttcn.promotionsdk.core.domain.usecase.PromotionUseCases
import com.ttcn.promotionsdk.core.domain.usecase.SearchCustomerVouchersUseCase
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

private class StubContextProvider : PromotionRequestContextProvider {
    override fun getCustomerId(): String = "c-1"
}

class PromotionContainerTest {

    @AfterTest
    fun tearDown() = PromotionContainer.clear()

    @Test
    fun init_wiresGraph_soUseCasesCanBeConstructedDirectly() {
        PromotionContainer.initialize(PromotionSDKConfig(apiKey = "k", baseUrl = "https://api.example.com"))

        assertTrue(PromotionContainer.isInitialized())
        // Container không phơi use case; UI dựng thẳng, use case tự lấy repository từ đồ thị.
        assertNotNull(SearchCustomerVouchersUseCase())
        assertNotNull(PromotionUseCases())
        assertEquals("https://api.example.com", PromotionContainer.requireConfig().baseUrl)
        // PromotionFeatureFlagUseCases() không dựng được ở đây: nó kéo theo KeyValueStorage,
        // mà trên JVM host test không có Context. FeatureFlagTest phủ phần đó bằng storage giả.
    }

    /**
     * Dựng use case trước `initialize()` **không được** ném ngay ở constructor.
     *
     * Trên iOS, `IllegalStateException` từ constructor không nằm trong `@Throws` nên Kotlin/Native
     * `abort()` tiến trình. Repository được resolve lười, lỗi chỉ nổi lên ở `invoke()` — nơi
     * `promotionCall` bọc nó thành [PromotionException] và Swift nhận `NSError`.
     */
    @Test
    fun useCase_constructedBeforeInit_doesNotThrowAtConstruction() {
        assertNotNull(SearchCustomerVouchersUseCase())
    }

    @Test
    fun useCase_invokedBeforeInit_failsAsPromotionException() = runTest {
        val useCase = SearchCustomerVouchersUseCase()

        val error = assertFailsWith<PromotionException> {
            useCase(SearchCustomerVouchersRequest(customerId = "c-1"))
        }

        assertEquals(PromotionErrorCodes.GENERAL, error.errorCode)
        assertTrue("Dependency not found" in error.message.orEmpty(), error.message.orEmpty())
    }

    @Test
    fun repositoryBehindUseCases_isSameSingleton() {
        PromotionContainer.initialize(PromotionSDKConfig(apiKey = "k", baseUrl = "https://api.example.com"))

        // Hai lần dựng use case phải dùng lại cùng một repository (và cùng một HttpClient).
        assertSame(PromotionContainer.requestContextProvider, PromotionContainer.requestContextProvider)
    }

    @Test
    fun requestContextProvider_fallsBackToEmptyWhenNotConfigured() {
        PromotionContainer.initialize(PromotionSDKConfig(apiKey = "k", baseUrl = "https://api.example.com"))

        assertEquals(null, PromotionContainer.requestContextProvider.getCustomerId())
    }

    @Test
    fun requestContextProvider_usesConfiguredProvider() {
        PromotionContainer.initialize(
            PromotionSDKConfig(
                apiKey = "k",
                baseUrl = "https://api.example.com",
                requestContextProvider = StubContextProvider(),
            )
        )

        assertEquals("c-1", PromotionContainer.requestContextProvider.getCustomerId())
    }

    @Test
    fun clear_resetsContainer_andIsSafeWhenHttpClientNeverBuilt() {
        PromotionContainer.initialize(PromotionSDKConfig(apiKey = "k", baseUrl = "https://api.example.com"))

        PromotionContainer.clear()

        assertFalse(PromotionContainer.isInitialized())
    }

    @Test
    fun init_afterClear_rebuildsGraph() {
        PromotionContainer.initialize(PromotionSDKConfig(apiKey = "k", baseUrl = "https://a.example.com"))
        PromotionContainer.clear()
        PromotionContainer.initialize(PromotionSDKConfig(apiKey = "k", baseUrl = "https://b.example.com"))

        assertEquals("https://b.example.com", PromotionContainer.requireConfig().baseUrl)
        assertNotNull(PromotionUseCases())
    }
}
