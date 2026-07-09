# DatabaseGuide — ĐÃ THAY THẾ

> ⛔ **File này không còn hiệu lực.** Nội dung cũ mô tả Room + `SharedPrefStorage` của SDK Android.
>
> `:promotionLogic` **không có database**: không Room, không SQLDelight, không kapt/KSP.
> Room trong repo gốc chưa từng được dùng — bốn class trong `core/data/local/` là stub rỗng và
> toàn repo có 0 dòng `import androidx.room`.
>
> Lưu trữ cục bộ hiện tại chỉ gồm cờ feature flag, qua `KeyValueStorage`
> (`SharedPreferences` trên Android, `NSUserDefaults` trên iOS).

👉 Đọc **[StorageGuide.md](./StorageGuide.md)**.
