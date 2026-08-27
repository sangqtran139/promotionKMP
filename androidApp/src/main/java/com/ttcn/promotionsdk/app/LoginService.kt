package com.ttcn.promotionsdk.app

import android.util.Log
import com.ttcn.prm.entry.PromotionTokenSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
/** Số nhận OTP của tài khoản demo. Đổi số thì đổi đúng một dòng này. */
internal const val DEMO_MSISDN = "84971410156"

internal const val DEMO_BASE_URL = "https://api24cdn.vtmoney.vn/uatmm"

/**
 * Kho token của app demo — đứng cho thứ mà host thật đã có sẵn (session manager / repository).
 * SDK đọc lại field này ở **mỗi** request qua `PromotionTokenSource`, nên đổi nó lúc nào cũng được,
 * không phải báo gì cho SDK.
 *
 * `@Volatile` vì SDK đọc từ **thread nền** còn luồng đăng nhập ghi từ thread khác.
 */
@Volatile
internal var demoAccessToken: String = ""

/**
 * Nguồn token mà app demo đưa cho SDK — cài đặt [PromotionTokenSource].
 *
 * `object` (sống bằng tuổi process) chứ không phải field của Fragment: SDK giữ object này tới tận
 * `PromotionSDK.release()`. Trỏ vào màn hình là rò rỉ màn hình đó vĩnh viễn.
 *
 * - [currentToken] — SDK gọi ở **mỗi** request. Chỉ đọc [demoAccessToken], không I/O, không chặn.
 * - [refreshToken] — SDK gọi khi ăn 401. Ở app thật đây là API refresh token của bạn; demo không có
 *   nên login lại từ đầu. Điều **quan trọng** là ghi vào [demoAccessToken] **trước** khi báo `true`:
 *   lượt thử lại của SDK đọc token qua [currentToken] chứ không dùng giá trị nào ta trả về.
 */
internal object DemoTokenSource : PromotionTokenSource {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** SDK đã gộp các request 401 cùng lúc thành một lần gọi; khoá này chặn nốt hai màn mở cách nhau vài giây. */
    private val refreshMutex = Mutex()

    override fun currentToken(): String = demoAccessToken

    /**
     * **Chịu ngay.** Luồng đăng nhập giờ CẦN người dùng nhập OTP, nên app không tự lấy token mới
     * được — không có mã để điền hộ.
     *
     * Trả `false` để SDK để lỗi `TOKEN_EXPIRED` nổi lên và bắn `onExpireToken()`; host thật sẽ đưa
     * người dùng về màn đăng nhập. Tự gọi lại `requestOtp` ở đây chỉ tốn một lượt OTP trong hạn 5
     * lần rồi vẫn phải chờ người nhập.
     */
    override fun refreshToken(onResult: (Boolean) -> Unit) {
        Log.d(TAG, "SDK ăn 401 → luồng đăng nhập cần OTP, app không tự làm mới được")
        onResult(false)
    }
}

/**
 * **Chỉ để demo.** Ghi một chuỗi rác vào kho token để lượt gọi API kế tiếp của SDK ăn 401 — nhìn
 * thấy cơ chế refresh chạy mà không phải ngồi chờ token thật hết hạn (~15 phút).
 */
internal fun expireTokenForDemo() {
    demoAccessToken = "expired-token-for-demo"
    Log.d(TAG, "Đã làm hỏng token trong kho app (SDK KHÔNG được báo gì)")
}

/**
 * Login demo (test-only) để lấy accessToken truyền vào SDK. Đối ứng `LoginService` bên iOS — cùng tên
 * class + entry `login()`. Cơ chế bất đồng bộ khác nhau theo ngôn ngữ (suspend ở đây vs completion
 * handler bên iOS) là đặc thù nền tảng, chấp nhận.
 */
class LoginService {

    private data class LoginRequest(
        val msisdn: String = DEMO_MSISDN,
        val username: String = DEMO_MSISDN,
        val userType: String = "msisdn",
        val pin: String = "139200",
        val loginType: String = "BASIC",
        val notifyToken: String = "1234567899",
        val requestId: String = "",
        val typeOs: String = "ios",
        /**
         * RỖNG ở bước 1 — bước đó chỉ để server gửi OTP về máy. Mã thật do **người dùng nhập** rồi
         * truyền vào ở bước 2.
         *
         * Trước đây gắn cứng `"1111"`. Mã đó không còn đúng, và mỗi lượt gửi sai bị tính là một lần
         * nhập sai: quá 5 lần liên tiếp là server khoá tài khoản một phút
         * ("Tài khoản tạm khóa vì sai OTP quá 5 lần").
         */
        val otp: String = "",
        val imei: String = "1234567891"
    )

    private data class LoginResponse(
        val status: Status?,
        val data: Data?
    ) {
        data class Status(
            val code: String?,
            val message: String?,
            /** Câu server soạn sẵn cho người dùng — ưu tiên nó hơn `message` kỹ thuật. */
            val displayMessage: String?,
        )
        data class Data(val requestId: String?, val accessToken: String?)
    }

    private interface LoginApiService {
        @Headers(
            "imei: 1231241",
            "Product: VIETTELPAY",
            "Authority-Party: APP",
            "app-version: 8.8.59"
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
    /**
     * Bước 1 — xin OTP. Server gửi mã về [msisdn] và trả `requestId` để bước 2 dùng lại.
     *
     * Gọi **đúng một lượt** mỗi lần người dùng bấm: mỗi lượt là một lần gửi OTP và server đếm số
     * lần không hoàn tất. Đừng tự thử lại ở đây.
     */
    /**
     * @param previousRequestId `requestId` còn giữ từ lần xin trước, nếu có.
     */
    suspend fun requestOtp(
        msisdn: String = DEMO_MSISDN,
        previousRequestId: String? = null,
    ): OtpChallenge = withContext(Dispatchers.IO) {
        val response = service.login(LoginRequest(msisdn = msisdn, username = msisdn))
        Log.d(TAG, "Bước 1 response: $response")

        val token = response.data?.accessToken
        if (response.status?.code == "00" && token != null) {
            return@withContext OtpChallenge.Token(token)
        }

        // Mã khác AUT0014 → hỏng thật, ném câu `displayMessage` của server.
        check(response.status?.code == "AUT0014") { response.describe() }

        // AUT0014 nghĩa là "cần OTP" — mã ĐÃ được gửi về máy. Nhưng server chỉ cấp `requestId`
        // MỚI khi chưa có OTP nào còn hiệu lực; xin lại quá sớm thì nó trả AUT0014 kèm
        // `requestId` RỖNG.
        //
        // Rỗng KHÔNG phải lỗi: mã cũ vẫn dùng được. Nên rơi về `requestId` đang giữ thay vì báo
        // hỏng — bản trước vứt nó đi rồi bắt người dùng bấm "Gửi lại OTP" mãi mãi, vì càng bấm
        // càng chắc chắn rỗng.
        val requestId = response.data?.requestId?.takeIf { it.isNotBlank() }
            ?: previousRequestId?.takeIf { it.isNotBlank() }
        checkNotNull(requestId) { response.describe() }

        OtpChallenge.NeedOtp(requestId, response.describe())
    }

    /** Bước 2 — gửi OTP người dùng vừa nhập, lấy access token. */
    suspend fun submitOtp(requestId: String, otp: String, msisdn: String = DEMO_MSISDN): String =
        withContext(Dispatchers.IO) {
            val response = service.login(
                LoginRequest(msisdn = msisdn, username = msisdn, requestId = requestId, otp = otp),
            )
            Log.d(TAG, "Bước 2 response: $response")

            check(response.status?.code == "00") { response.describe() }
            checkNotNull(response.data?.accessToken) {
                "Đăng nhập thành công nhưng server không trả accessToken."
            }
        }

    /**
     * Câu để hiện cho người dùng. `displayMessage` của server nói đúng chuyện đang xảy ra (vd
     * "Bạn đã nhập sai/không nhập mã OTP quá 5 lần liên tiếp. Vui lòng thực hiện lại sau 1 phút")
     * — thứ mà thông báo tự soạn ở client không đoán nổi.
     */
    private fun LoginResponse.describe(): String =
        status?.displayMessage
            ?: status?.message
            ?: "Mã lỗi ${status?.code ?: "(không rõ)"}"
}

/** Kết quả bước 1: hoặc xong luôn (hiếm), hoặc cần người dùng nhập OTP. */
sealed interface OtpChallenge {
    data class Token(val accessToken: String) : OtpChallenge
    data class NeedOtp(val requestId: String, val message: String) : OtpChallenge
}

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
