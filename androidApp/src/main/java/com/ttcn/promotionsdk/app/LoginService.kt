package com.ttcn.promotionsdk.app

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import okio.Buffer
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

private const val TAG = "LoginService"

/**
 * Server demo — **một nguồn duy nhất** cho cả login lẫn `PromotionSDK.initialize(baseUrl = …)`.
 *
 * Trước đây IP này bị chép ở hai file, đổi môi trường mà quên một chỗ thì login và SDK trỏ về hai
 * server khác nhau — hỏng theo kiểu rất khó đoán. Không có dấu '/' ở cuối: SDK nhận dạng này,
 * riêng Retrofit thì tự nối thêm.
 */
internal const val DEMO_BASE_URL = "http://125.235.38.229:8080"

/**
 * Login demo (test-only) để lấy accessToken truyền vào SDK. Đối ứng `LoginService` bên iOS — cùng tên
 * class + entry `login()`. Cơ chế bất đồng bộ khác nhau theo ngôn ngữ (suspend ở đây vs completion
 * handler bên iOS) là đặc thù nền tảng, chấp nhận.
 */
class LoginService {

    private data class LoginRequest(
        val msisdn: String = "84346801339",
        val username: String = "84346801339",
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
            .addInterceptor(CurlLoggingInterceptor())
            .addInterceptor(logging)
            .build()
        Retrofit.Builder()
            // Retrofit đòi baseUrl kết thúc bằng '/'; hằng dùng chung giữ bản không có dấu '/' để
            // truyền thẳng vào `PromotionSDK.initialize(baseUrl = …)` được.
            .baseUrl("$DEMO_BASE_URL/")
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
    suspend fun login(): String = withContext(Dispatchers.IO) {
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

/**
 * In mỗi request login ra **lệnh cURL** copy-paste được — dán thẳng vào terminal/Postman khi đối chiếu
 * với BE. Đối ứng plugin `PromotionCurlLogging` bên trong SDK (login là API của host, không đi qua
 * HttpClient của SDK nên cần interceptor riêng ở đây).
 */
private class CurlLoggingInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val sb = StringBuilder("curl -X ").append(request.method)

        request.headers.forEach { (name, value) ->
            sb.append(" \\\n  -H '").append(name).append(": ").append(value).append('\'')
        }

        request.body?.let { body ->
            // Content-Type nằm trên RequestBody (do converter đặt), không ở headers builder.
            body.contentType()?.let { sb.append(" \\\n  -H 'Content-Type: ").append(it).append('\'') }
            val buffer = Buffer()
            body.writeTo(buffer)
            val charset = body.contentType()?.charset() ?: Charsets.UTF_8
            val payload = buffer.readString(charset)
            if (payload.isNotEmpty()) {
                // Escape nháy đơn cho shell: ' -> '\''
                sb.append(" \\\n  -d '").append(payload.replace("'", "'\\''")).append('\'')
            }
        }

        sb.append(" \\\n  '").append(request.url).append('\'')
        Log.d(TAG, "cURL:\n$sb")
        return chain.proceed(request)
    }
}
