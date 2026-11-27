# Exacta Reader

Ứng dụng đọc EPUB cho máy đọc sách e-ink Android (thử trên iReader Neo 3: Android 11, 300 ppi,
cảm ứng, không có phím lật trang), mang lối chơi tu tiên của Exacta.OS: đọc sách là tu luyện.
Lõi đọc là Readium Kotlin Toolkit; giao diện vẽ riêng theo kiểu Exacta.OS, đen trắng, không hiệu ứng.

Tải bản cài: [`dist/Exacta-Reader-0.2.apk`](dist/Exacta-Reader-0.2.apk) (chép vào máy rồi cài).

## Có gì trong bản 0.2

- **Home** (bố cục A2 của Exacta.OS): sách đang đọc (bìa, tên, tầng công pháp, câu tu luyện),
  ô Pháp bảo, các nút Thư viện, Tu vi, Cài đặt, và thẻ tu hành (lời chào, tiết khí, cảnh giới,
  số vạn từ, thanh 9 ô, hôm nay · 7 ngày · chuỗi ngày, một câu nói). Thanh trạng thái hiện giờ,
  ngày âm lịch kiểu cổ ("Mạnh Đông · Vọng nhật") và pin.
- **Thư viện**: mọi tệp EPUB trên thẻ nhớ, có bìa, tác giả, tầng và % đã đọc; cuốn đọc gần nhất lên đầu.
- **Màn đọc**: chạm bên phải lật tới, bên trái lật lui, giữa trang mở bảng chỉnh: cỡ chữ, font
  (Bitter, gốc, có chân, không chân), giãn dòng, lề, căn chữ, thanh kéo tới vị trí bất kỳ, mục lục,
  chương trước/sau, thời gian còn lại theo tốc độ đọc của bạn. Chân trang: chương, %, giờ.
- **Tu tiên**: số chữ đã đọc (đọc tuần tự mới tính; nhảy qua mục lục chỉ dời mốc) quy ra 36 cấp
  cảnh giới từ Luyện Khí tới Tiên nhân nhất phẩm; tầng công pháp theo % mỗi cuốn.
- **Pháp bảo**: lò luyện 3 ô, mỗi 10 vạn từ được tặng một khoáng thạch; khoáng thạch luyện bằng
  chữ đọc ở bất kỳ cuốn nào; đọc hết một cuốn luyện thành một pháp bảo theo độ dày sách.
  36 loại pháp bảo, 6 phẩm; Bảo khố xem từng món.
- **Tu vi**: cảnh giới và bậc thang 10 cảnh giới, thống kê, 14 ngày qua, Kinh Các (sách đã đọc xong).
- Nhật ký lỗi: `ExactaReader/exacta-log.txt` trên bộ nhớ máy (máy không có gỡ lỗi USB).

## Build (Windows, dòng lệnh)

Công cụ đặt ở `D:\Android` (JDK 17, Android SDK 34, Gradle), không đụng thư mục người dùng.

```powershell
$env:JAVA_HOME = 'D:\Android\jdk-17'
$env:ANDROID_HOME = 'D:\Android\sdk'
$env:GRADLE_USER_HOME = 'D:\Android\gradle-home'
.\gradlew.bat assembleRelease
```

Cần tệp `local.properties` với `sdk.dir=D\:/Android/sdk` (dùng dấu `/`).
APK ra ở `app\build\outputs\apk\release\app-release.apk`; bản phát hành chép vào `dist\`.

**Khóa ký:** bản phát hành ký bằng khóa riêng `keystore\exacta-reader.jks` (mật khẩu trong
`keystore.properties`). Hai tệp này **không** đưa lên GitHub; hãy giữ bản sao lưu. APK ký bằng khóa
khác không cài đè lên được, phải gỡ app và mất dữ liệu đọc. Khi không có khóa, bản build tự ký bằng
khóa debug.

## Cấu trúc

- `game/`: cảnh giới (`Realm`), lịch âm và tiết khí (`Lunar`), pháp bảo (`Treasure`), chữ của Home (`Texts`).
- `data/`: dữ liệu lưu trong một tệp JSON (`Store`), quét sách, bìa, đếm chữ (`Library`).
- `ui/`: bộ vẽ kiểu Exacta.OS (`Ink`), Home (`HomeView`), trang cuộn (`PageView`).
- Các màn: `HomeActivity`, `LibraryActivity`, `ReaderActivity`, `TuViActivity`, `PhapBaoActivity`, `SettingsActivity`.

## Hướng tiếp

Thiên kiếp, bí cảnh tuần, bộ pháp bảo, 36 đạo hiệu, Niên kỷ (đã có trên Exacta.OS Omega), đồng bộ.

## Giấy phép bên thứ ba

- Readium Kotlin Toolkit: BSD-3-Clause.
- Font Bitter: SIL Open Font License 1.1 (`app/src/main/assets/fonts/OFL-Bitter.txt`).
