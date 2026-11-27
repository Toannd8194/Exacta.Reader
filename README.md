# Exacta Reader

Ứng dụng đọc EPUB cho máy đọc sách e-ink Android (thử trên iReader Neo 3: Android 11, 300 ppi,
cảm ứng, không có phím lật trang), mang lối chơi tu tiên của Exacta.OS: đọc sách là tu luyện.
Lõi đọc là Readium Kotlin Toolkit; giao diện vẽ riêng theo kiểu Exacta.OS, đen trắng, không hiệu ứng.

Tải bản cài: [`dist/Exacta-Reader-0.3.apk`](dist/Exacta-Reader-0.3.apk) (chép vào máy rồi cài; cài đè lên bản cũ được).

## Bản 0.3

- **Sửa lỗi mở sách là thoát app** (bản 0.2): dòng chân trang ghép `%` vào chuỗi định dạng nên trang đầu
  tiên đã văng lỗi. Màn đọc giờ bắt mọi lỗi khi mở sách và báo ngay trên trang thay vì thoát; lỗi gần nhất
  hiện trong Cài đặt.
- **Giao diện theo mật độ điểm ảnh thật** của màn hình (300 ppi trên Neo 3) thay vì kéo giãn bố cục 480 px
  của X4, nên chữ và khung nhỏ lại, màn hình chứa nhiều thông tin hơn. Cài đặt › Cỡ giao diện: Nhỏ, Vừa,
  Lớn, Rất lớn.
- **Home** thêm: dải sách đọc gần đây (chạm để đọc tiếp), dòng thời gian đã đọc và còn lại của cuốn đang
  đọc, tốc độ đọc, và ba thẻ Thiên kiếp, Bí cảnh tuần, Đạo hiệu.
- **Thiên kiếp**: mỗi đại cảnh giới chỉ mở sau một kỳ độ kiếp: đọc liền 3 đến 7 ngày, mỗi ngày 30 đến 60
  phút (càng lên cao càng khó). Lỡ một ngày ở giữa là độ kiếp thất bại, tu vi lui về gần cửa ải và phải
  đọc lại tới cửa ải.
- **Bí cảnh tuần**: từ thứ Hai đến Chủ nhật, đủ 5 ngày đọc từ 30 phút thì nhận một khoáng thạch Huyền phẩm
  trở lên vào lò luyện.
- **Đạo hiệu**: 36 đạo hiệu trong 6 nhóm (Tu hành, Đạo tâm, Thời khắc, Kinh các, Pháp bảo, Kỳ duyên);
  chạm một đạo hiệu đã đắc để mang. Ngày sinh nhật đặt trong Cài đặt.
- **Hình ảnh độ phân giải cao**: 36 pháp bảo 256 px (phóng từ bản 64 px của Exacta.OS bằng Scale2x hai lần),
  10 biểu tượng cảnh giới 160 px vẽ lại từ bản gốc, vẽ có làm mịn.
- Tu vi thêm thiên kiếp và bí cảnh; thống kê hai cột (ngày đọc nhiều nhất, lần đọc dài nhất, tầng công pháp,
  số lần độ kiếp...); biểu đồ 14 ngày ghi số phút.

## Có gì từ bản 0.2

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

- `game/`: cảnh giới (`Realm`), lịch âm và tiết khí (`Lunar`), pháp bảo (`Treasure`), đạo hiệu (`DaoHieu`), chữ của Home (`Texts`).
- `data/`: dữ liệu lưu trong một tệp JSON (`Store`, kèm thiên kiếp, bí cảnh, đạo hiệu), quét sách, bìa, đếm chữ (`Library`).
- `ui/`: bộ vẽ kiểu Exacta.OS (`Ink`), Home (`HomeView`), trang cuộn (`PageView`).
- Các màn: `HomeActivity`, `LibraryActivity`, `ReaderActivity`, `TuViActivity`, `PhapBaoActivity`, `DaoHieuActivity`, `SettingsActivity`.

## Hướng tiếp

Bộ pháp bảo, Niên kỷ (đã có trên Exacta.OS Omega), đồng bộ.

Đạo hiệu khác Exacta.OS ở năm chỗ, vì app chưa có kỳ ngộ, bế quan, hợp luyện, linh thạch: Phúc Duyên Thâm
Hậu (20 khoáng thạch quà), Bế Quan Khổ Tu (tổng 100 giờ đọc), Lò Hỏa Thuần Thanh (30 pháp bảo), Tứ Bảo
Tề Tụ (pháp bảo của 4 phẩm), Nguyệt Hạ Độc Thư (đọc đêm Trung Thu).

## Giấy phép bên thứ ba

- Readium Kotlin Toolkit: BSD-3-Clause.
- Font Bitter: SIL Open Font License 1.1 (`app/src/main/assets/fonts/OFL-Bitter.txt`).
