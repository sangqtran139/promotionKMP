package com.ttcn.promotionsdk.app

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

private const val TAG = "PromotionTestLogin"

class PromotionTestLoginManager {

    private data class LoginRequest(
        val msisdn: String = "84362634580",
        val username: String = "84362634580",
        val userType: String = "msisdn",
        val pin: String = "123123",
        val loginType: String = "BASIC",
        val notifyToken: String = "1234567899",
        val requestId: String = "",
        val typeOs: String = "ios",
        val otp: String = "1111",
        val imei: String = "1234567891"
    )

    private data class LoginResponse(
        val status: Status?,
        val data: Data?
    ) {
        data class Status(val code: String?, val message: String?)
        data class Data(val requestId: String?, val accessToken: String?)
    }

    private interface LoginApiService {
        @Headers(
            "imei: 1231241",
            "Product: VIETTELPAY",
            "Authority-Party: APP",
            "app-version: 6.8.8"
        )
        @POST("auth/v1/authn/login")
        suspend fun login(@Body body: LoginRequest): LoginResponse
    }

    private val service: LoginApiService by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()
        Retrofit.Builder()
            .baseUrl("http://125.235.38.229:8080/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(LoginApiService::class.java)
    }

    /**
     * Thực hiện 2-bước login và trả về accessToken.
     * Bước 1: gọi API với requestId rỗng → nhận AUT0014 + requestId mới.
     * Bước 2: gọi lại với requestId vừa nhận → nhận accessToken.
     * @throws Exception nếu server trả về lỗi.
     */
    suspend fun loginAndGetAccessToken(): String = withContext(Dispatchers.IO) {
        // ── Bước 1 ──────────────────────────────────────────────
        val step1Body = LoginRequest()
        Log.d(TAG, "Step 1 request: $step1Body")

        val step1 = service.login(step1Body)
        Log.d(TAG, "Step 1 response: $step1")

        val code1 = step1.status?.code
        if (code1 == "00") {
            val token = checkNotNull(step1.data?.accessToken) {
                "Step 1 returned 00 but accessToken is null"
            }
            Log.d(TAG, "accessToken (from step 1): $token")
            return@withContext token
        }

        check(code1 == "AUT0014") {
            "Step 1 failed — code=$code1, message=${step1.status?.message}"
        }

        val requestId = checkNotNull(step1.data?.requestId) {
            "Step 1 returned AUT0014 but requestId is null"
        }

        // ── Bước 2 ──────────────────────────────────────────────
        val step2Body = LoginRequest(requestId = requestId)
        Log.d(TAG, "Step 2 request: $step2Body")

        val step2 = service.login(step2Body)
        Log.d(TAG, "Step 2 response: $step2")

        check(step2.status?.code == "00") {
            "Step 2 failed — code=${step2.status?.code}, message=${step2.status?.message}"
        }

        val token = checkNotNull(step2.data?.accessToken) {
            "Step 2 returned 00 but accessToken is null"
        }
        Log.d(TAG, "accessToken (from step 2): $token")
        token
    }
}
