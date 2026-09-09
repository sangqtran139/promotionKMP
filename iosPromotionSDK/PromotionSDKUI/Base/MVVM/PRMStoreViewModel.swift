//
//  PRMStoreViewModel.swift
//  PromotionSDK
//
//  Lớp bọc DUY NHẤT quanh store dùng chung (promotionLogic) — đối ứng `PRMStoreViewModel<S, I>`
//  bên Android. Gom phần mà trước đây mỗi ViewModel chép y hệt nhau: giữ store, observe state
//  (hop main), đổi `errorCode` thành effect một-lần, huỷ đúng lúc deinit.
//

import Foundation
@_implementationOnly import PRMKotlinBridge

/// Hình dạng chung của mọi store — đối ứng `PRMStore<S, I>` bên Kotlin.
///
/// **Không cần lớp adapter nào**: các class store Kotlin đã phơi sẵn đúng những member này với đúng
/// type (xem `PromotionLogic.h`), nên chỉ cần khai conformance rỗng ở cuối file này.
///
/// Vì sao không dùng thẳng protocol `PRMStore` mà Kotlin sinh ra: generic của interface Kotlin **bị
/// erase** khi export sang ObjC (`dispatch(intent: Any)`, `state: StateFlow` không tham số), nên
/// dùng nó là mất sạch type safety. Class cụ thể thì vẫn giữ nguyên type — protocol Swift này bám
/// vào class, không bám vào protocol Kotlin.
protocol PRMStoreBridge: AnyObject {
    associatedtype State
    associatedtype Intent

    func currentState() -> State
    func watchState(onEach: @escaping (State) -> Void) -> PromotionCancellable
    func dispatch(intent: Intent)
    func errorOf(state: State) -> String?
    var consumeErrorIntent: Intent { get }
    func clear()
}

/// Bọc store — **không** biết gì về điều hướng, nên dùng được cho cả widget (`PRMEndowView`) lẫn
/// màn hình. Màn hình thì dùng [PRMScreenViewModel] để có thêm router.
///
/// - `state` / `onState`: state của store. Gán `onState` là **nhận ngay** state hiện tại — mô phỏng
///   hành vi replay của `StateFlow` bên Android (`collectFlow` nhận value mới nhất khi bắt đầu).
/// - `onEffect`: sự kiện **một lần**, KHÔNG replay.
/// - `dispatch(_:)`: đẩy Intent xuống store.
///
/// **Vì sao effect được suy ở đây chứ không collect `store.effects` như Android:** `effects` là
/// default member của interface `PRMStore`, mà Kotlin/Native chỉ đặt default member lên *protocol*
/// chứ không lên class, và protocol thì bị erase generic → Swift không collect nổi. Ngữ nghĩa vẫn y
/// hệt: mỗi `errorCode` đi ra đúng một lần rồi tự `ConsumeError`.
@MainActor
class PRMStoreViewModel<Store: PRMStoreBridge> {

    let store: Store

    private(set) var state: Store.State {
        didSet { onState?(state) }
    }

    /// Closure `@MainActor`: [bindStore] **luôn** phát trên main thread. Trước đây điều đó chỉ nằm
    /// trong doc comment, nên nơi nhận (vd `PromotionSDKImpl.render`) phải là hàm nonisolated dù nó
    /// đụng UIKit — và compiler không có cách nào giữ. Đưa cam kết vào **kiểu** thì nơi nhận đánh
    /// `@MainActor` được, và ai gán một closure chạy nền vào đây sẽ bị chặn ngay lúc biên dịch.
    var onState: (@MainActor (Store.State) -> Void)? {
        didSet { onState?(state) }
    }

    /// Giá trị **đồng bộ** mới nhất từ store.
    ///
    /// Khác [state]: [state] được cập nhật qua `DispatchQueue.main.async` nên có thể trễ một nhịp.
    /// Nơi nào đọc ngay sau khi [dispatch] (hoặc cần chắc chắn là bản mới nhất) thì dùng cái này.
    var currentState: Store.State { store.currentState() }

    /// Một-lần, KHÔNG replay: subscriber mới không nhận lại lỗi cũ. `@MainActor` cùng lý do với [onState].
    var onEffect: (@MainActor (PRMEffect) -> Void)?

    /// Mỗi `errorCode` thành một [PRMEffect] rồi tự `ConsumeError`.
    ///
    /// Widget `PRMEndowView` **tắt** cái này: nó phải giữ lỗi lại cho tới khi vòng validate kết thúc
    /// (xem `EndowViewModel.handleSettle`), tự xoá sớm là nuốt mất lỗi ngay lúc user đang chờ kết
    /// quả bấm "Áp dụng". Đối ứng `consumeErrorUnlessSettling` bên Android.
    var autoConsumesError: Bool { true }

    private var storeCancellable: PromotionCancellable?

    init(store: Store) {
        self.store = store
        self.state = store.currentState()
        bindStore()
    }

    deinit {
        storeCancellable?.cancel()
        store.clear()
    }

    /// Luôn gọi trên main thread (view chỉ việc render).
    ///
    /// Bản thân hàm chạy từ `init` (đã ở MainActor); thứ **không** ở main là closure mà `watchState`
    /// gọi lại — nó đến từ coroutine của Kotlin (`Dispatchers.Default`). Việc hop nằm bên trong.
    ///
    /// `DispatchQueue.main.async` + `assumeIsolated` chứ **không** `Task { @MainActor in }`: `Task`
    /// không bảo đảm thứ tự giữa nhiều lần phát, mà `StateFlow` là conflated — hai state tới gần nhau
    /// mà chạy đảo thứ tự là bản cũ ghi đè bản mới. `DispatchQueue.main` giữ FIFO; `assumeIsolated`
    /// chỉ nói với compiler điều đã đúng ở dòng trên, không thêm một nhịp hop nào.
    private func bindStore() {
        storeCancellable = store.watchState { [weak self] state in
            DispatchQueue.main.async {
                MainActor.assumeIsolated {
                    guard let self = self else { return }
                    self.state = state
                    guard self.autoConsumesError else { return }
                    self.emitErrorIfNeeded(state)
                }
            }
        }
    }

    func dispatch(_ intent: Store.Intent) {
        store.dispatch(intent: intent)
    }

    /// `@MainActor` vì nó gọi `PromotionSDK.getCallback()` (bề mặt public, nay MainActor-isolated)
    /// và đụng `onEffect` — cả hai đều là chuyện của UI.
    ///
    /// Không phải ràng buộc mới: hàm này **vốn đã** chỉ được gọi từ trong `DispatchQueue.main.async`
    /// ở [bindStore]. Khác biệt là trước đây điều đó chỉ được bảo đảm bằng một dòng comment; nay
    /// compiler giữ. Đây đúng là chỗ mà `@MainActor` ở `PromotionSDK` vừa chỉ ra.
    @MainActor
    private func emitErrorIfNeeded(_ state: Store.State) {
        guard let code = store.errorOf(state: state) else { return }
        // 401 (map sẵn ở `Throwable.toErrorCode()`, promotionLogic) → báo host qua callback toàn
        // cục, một chỗ cho cả bốn màn kế thừa lớp này — effect vẫn chảy tiếp xuống UI như cũ, không
        // nuốt lỗi. Đối ứng `PRMStoreViewModel.kt` bên Android.
        if code == PromotionErrorCodes.shared.TOKEN_EXPIRED {
            PromotionSDK.getCallback()?.onExpireToken()
        }
        onEffect?(PRMEffectShowError(errorCode: code))   // view map code → chuỗi
        store.dispatch(intent: store.consumeErrorIntent)
    }
}

/// [PRMStoreViewModel] + router — dùng cho ViewModel của **màn hình**.
class PRMScreenViewModel<R: PRMBaseRouterProtocol, Store: PRMStoreBridge>: PRMStoreViewModel<Store> {

    let router: R

    init(router: R, store: Store) {
        self.router = router
        super.init(store: store)
    }

    func routeToParent() {
        router.routeToParent()
    }
}

// ─── Conformance: store Kotlin đã đúng hình dạng, không cần viết gì thêm ────────

extension MyPromotionStore: PRMStoreBridge {}
extension ChoosePromotionStore: PRMStoreBridge {}
extension SearchMyPromotionStore: PRMStoreBridge {}
extension PromotionDetailStore: PRMStoreBridge {}
extension EndowStore: PRMStoreBridge {}
