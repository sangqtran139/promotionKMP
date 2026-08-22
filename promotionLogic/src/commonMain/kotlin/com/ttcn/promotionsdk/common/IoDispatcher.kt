package com.ttcn.promotionsdk.common

import kotlinx.coroutines.CoroutineDispatcher

/**
 * Dispatcher chạy I/O mạng của tầng data. `Dispatchers.IO` không có ở `commonMain` (chỉ tồn tại
 * riêng cho JVM và cho Kotlin/Native), nên phải qua `expect`/`actual` — cùng kiểu với [SdkLock].
 *
 * **Vì sao cần:** store dùng chung nhận `scope` từ nền tảng, và Android truyền thẳng
 * `viewModelScope` (`Dispatchers.Main.immediate`). Ktor chạy pipeline **phía client** trong context
 * của coroutine gọi nó — chỉ engine mới tự nhảy sang thread nền. Nghĩa là `defaultRequest { }` của
 * `PromotionHttpClient` (dựng header, đọc token/ngôn ngữ/context đơn hàng từ host qua
 * `PromotionRequestContextProvider`) đang chạy **trên main thread** ở Android. Host cấp token bằng
 * một lambda đọc Keychain/EncryptedSharedPreferences là jank ngay.
 *
 * Bọc `apiCall` bằng dispatcher này đẩy toàn bộ phần đó xuống thread nền, trên cả hai nền tảng,
 * bất kể nơi gọi truyền scope nào.
 */
internal expect val ioDispatcher: CoroutineDispatcher
