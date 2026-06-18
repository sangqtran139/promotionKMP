# Tài liệu nền tảng — TTCN Promotion Android SDK

Thư mục `/docs` chứa toàn bộ tài liệu nền tảng (foundation docs) của project **TTCN Promotion Android SDK**.
Mục tiêu: giúp lập trình viên **và AI agent** hiểu nhanh kiến trúc, quy ước và quy tắc làm việc trước khi viết hoặc sửa code.

> ⚠️ **Trước khi bắt đầu bất kỳ task nào, hãy đọc [AI_AGENT_RULES.md](./AI_AGENT_RULES.md).**

---

## Tổng quan project

- **Loại project:** Android **SDK** (thư viện) — không phải app độc lập.
- **Module:**
  - `vds-promotion` — module thư viện chính (`com.android.library`), namespace `com.ttcn.promotionsdk`.
  - `app` — module demo/tích hợp thử (`com.android.application`), dùng để chạy thử SDK.
- **Ngôn ngữ:** Kotlin (2.2.0), JVM toolchain 17.
- **minSdk:** 24 — **compileSdk:** 35.
- **UI:** XML View + Data Binding + View Binding (**không dùng Jetpack Compose**).
- **Kiến trúc:** Clean Architecture (Data / Domain / Presentation) + **MVI**.
- **DI:** Custom DI tự viết (phong cách Koin), không dùng Hilt/Koin.
- **Networking:** Retrofit + OkHttp + Gson.
- **Database:** Room (đã khai báo dependency, hiện ở dạng scaffold) + SharedPreferences.
- **Bất đồng bộ:** Kotlin Coroutines + Flow (StateFlow/SharedFlow).

---

## Danh mục tài liệu

| File | Nội dung |
|------|----------|
| [README.md](./README.md) | Trang tổng quan này — điểm bắt đầu để đọc docs. |
| [AI_AGENT_RULES.md](./AI_AGENT_RULES.md) | **Quy tắc bắt buộc** cho AI agent khi làm việc trên repo. |
| [Architecture.md](./Architecture.md) | Kiến trúc tổng thể: Clean Architecture + MVI, luồng dữ liệu. |
| [ProjectStructure.md](./ProjectStructure.md) | Cấu trúc thư mục, vai trò từng package. |
| [CodingStandards.md](./CodingStandards.md) | Quy ước code: đặt tên, format, prefix `PRM`, Kotlin style. |
| [AndroidGuide.md](./AndroidGuide.md) | Quy ước Android chung: lifecycle, resource, SDK entry point. |
| [ComposeGuide.md](./ComposeGuide.md) | Trạng thái Jetpack Compose (hiện **không dùng**) và quy tắc liên quan. |
| [XMLViewGuide.md](./XMLViewGuide.md) | Quy ước XML View, Data Binding, View Binding, RecyclerView. |
| [Theming.md](./Theming.md) | Hệ thống theme/token: tùy biến màu, applier, quy tắc thứ tự cấu hình. |
| [NetworkingGuide.md](./NetworkingGuide.md) | Quy tắc Retrofit/OkHttp, interceptor, DTO, xử lý response. |
| [DependencyInjection.md](./DependencyInjection.md) | Cách hoạt động và quy tắc dùng Custom DI. |
| [DatabaseGuide.md](./DatabaseGuide.md) | Quy tắc Room / SharedPreferences / cache cục bộ. |
| [ErrorHandling.md](./ErrorHandling.md) | Quy ước exception, error code, hiển thị lỗi UI. |
| [TestingGuide.md](./TestingGuide.md) | Quy ước viết test (unit / instrumentation). |
| [features/](./features/README.md) | Tài liệu theo từng tính năng (My Promotion, Choose Promotion, Promotion Detail, Search, Endow View, Feature Flag). |

---

## Thứ tự đọc gợi ý

1. **AI_AGENT_RULES.md** — luật chơi.
2. **Architecture.md** — bức tranh lớn.
3. **ProjectStructure.md** — biết file nằm ở đâu.
4. Các guide chuyên đề (Networking, DI, Database, XMLView…) theo nhu cầu task.
5. **CodingStandards.md** + **ErrorHandling.md** — trước khi commit.

---

## Nguyên tắc cập nhật tài liệu

Tài liệu phải **luôn đồng bộ với code thật**. Khi thay đổi API, database, DI hoặc kiến trúc,
**bắt buộc cập nhật file docs tương ứng** trong cùng một thay đổi (xem [AI_AGENT_RULES.md](./AI_AGENT_RULES.md) điều 7).
