package com.ttcn.promotionsdk.app.mock.promotion

import android.util.Log
import okhttp3.mockwebserver.MockWebServer
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread

object PromotionMockApi {
    @Volatile
    private var server: MockWebServer? = null

    @Volatile
    private var resolvedBaseUrl: String? = null

    val baseUrl: String
        get() = checkNotNull(resolvedBaseUrl) {
            "PromotionMockApi is not started."
        }

    @Synchronized
    fun ensureStarted() {
        if (server != null) return

        val mockServer = MockWebServer().apply {
            dispatcher = PromotionMockDispatcher()
        }
        val startError = AtomicReference<Throwable?>(null)
        val startedSignal = CountDownLatch(1)

        thread(name = "promotion-mock-starter", start = true) {
            try {
                mockServer.start()
                server = mockServer
                resolvedBaseUrl = "http://127.0.0.1:${mockServer.port}/"
                Log.d(TAG, "MockWebServer started at $resolvedBaseUrl")
            } catch (throwable: Throwable) {
                startError.set(throwable)
                runCatching { mockServer.close() }
                server = null
                resolvedBaseUrl = null
                Log.e(TAG, "Failed to start MockWebServer", throwable)
            } finally {
                startedSignal.countDown()
            }
        }

        startedSignal.await()
        startError.get()?.let { throwable ->
            throw IllegalStateException("Failed to start PromotionMockApi", throwable)
        }
    }

    private const val TAG = "PromotionMockApi"
}
