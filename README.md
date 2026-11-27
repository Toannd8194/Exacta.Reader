# Exacta Reader

Ứng dụng đọc EPUB cho máy đọc sách e-ink Android (thử trên iReader Neo 3: Android 11, 300 ppi,
cảm ứng, không có phím lật trang), mang lối chơi tu tiên của Exacta.OS: đọc sách là tu luyện.
Lõi đọc là Readium Kotlin Toolkit; giao diện vẽ riêng theo kiểu Exacta.OS, đen trắng, không hiệu ứng.

Tải bản cài: [`dist/Exacta-Reader-0.7.apk`](dist/Exacta-Reader-0.7.apk) (chép vào máy rồi cài; cài đè lên bản cũ được). Từ bản 0.3, mỗi bản APK được giữ lại trong `dist/` với tên phiên bản.

## Bản 0.7

- **Lò luyện 6 ô, sách luyện theo chặng**: một cuốn chiếm tối đa 3 ô, mỗi ô một phẩm. Chặng: cứ đọc đủ số chữ của phẩm
  đó trong cuốn này (Hoàng 2,7 vạn · Huyền 6,75 vạn · Địa 13,5 vạn · Thiên 27 vạn · Âm Dương 67,5 vạn · Hỗn Độn 135 vạn
  từ) là ra một pháp bảo rồi luyện tiếp; hoặc luyện cả cuốn (đọc xong thành pháp bảo theo độ dày). Khoáng thạch tự vào
  lò tối đa 3 ô, các ô còn lại để dành cho sách.
- **Hình mới**: lò luyện đan (ba chân, nắp vòm, quẻ bát quái, lửa); 6 khoáng thạch mỗi phẩm một dáng (đá thô, tinh thể,
  cụm tinh thể, đá quý, ngọc âm dương, sao hỗn độn), vẽ xám, có thanh tiến độ.
- **Home Bảng tu hành**: bỏ hàng ba ô; ô tu hành chứa luôn Thiên kiếp và Bí cảnh (chạm vào dòng nào mở đúng tab đó);
  hàng dưới cùng Đạo hiệu (1/2) · Niên kỷ · Cài đặt; ô pháp bảo hiện 6 ô lò.
- **Màn đọc**: dòng trên có % đã đọc (trái), tên sách, đồng hồ (phải); dòng dưới có tên chương và thời gian dự kiến
  đọc xong sách và xong chương. Bảng chỉnh nhỏ gọn hơn.
- Cỡ giao diện chung còn +10% (bản 0.6 là +20%).

## Bản 0.6

- **Sửa gốc lỗi giãn dòng**: Readium 3.0 bỏ qua hàm biến đổi truyền lúc tạo `PublicationOpener`, nên CSS của app chưa
  từng vào được chương (giãn dòng và nét đậm cho font khác Bitter không ăn). Giờ CSS được gắn ngay khi mở sách
  (`Library.open`), có test kiểm tra; quy tắc giãn dòng mạnh hơn cả CSS gắn id hay `!important` của sách.
  Mỗi lần mở sách và mỗi lần chỉnh, app ghi khoảng cách dòng thực tế vào `exacta-log.txt`.
- **Đếm chữ đúng hơn**: chỉ lật trang mới được cộng chữ, tối đa 450 chữ một trang; nhảy chương, mục lục, thanh tua chỉ
  dời mốc (trước đây với sách rất dài, nhảy một chương cũng bị tính là đã đọc).
- **Lò luyện nhận sách**: chạm ô trống trong Pháp bảo › Lò luyện để đặt sách đang đọc (hoặc khoáng thạch). Sách trong lò
  đọc xong luyện thành pháp bảo theo độ dày; đọc xong ngoài lò nhận một khoáng thạch cùng phẩm.
- **Bảng chỉnh khi đọc**: nút chương to "‹ Chương / Chương ›"; font là ô tên font xổ danh sách, nút "+ Thêm" bên cạnh;
  tab Khác gồm 4 nút có icon (Home, Thư viện, Tu vi, Làm sạch) và danh sách tuỳ chọn.
- **Mặc định**: cỡ chữ 100% lớn bằng 120% bản 0.5; độ đậm mặc định Đậm vừa; giãn dòng 1,7; giao diện lớn thêm 20%.
- **Home**: ô sách cao hơn, nút Thư viện ở góc phải ô sách, thanh dưới chỉ còn các màn chưa có lối vào (mỗi nút một
  khung); tên cảnh giới to hơn; icon lò luyện khí mới (cửa lò có lửa) khi chưa có pháp bảo.
- Tu vi tách tab Thiên kiếp (kèm bảng 9 cửa ải) và Bí cảnh (kèm luật).

## Bản 0.5

- **Mọi chỉnh khi đọc nằm trong màn đọc** (chạm giữa trang); Cài đặt chung bỏ phần đọc sách.
- **Chỉnh bằng − / +**: font (lần lượt qua các font, cả font thêm vào), cỡ chữ (bước 5%), độ đậm (6 mức), giãn dòng
  (bước 0,1), lề hai bên (bước 10%), giãn đoạn (bước 0,1; "Theo sách" là giữ của sách). Chạm vào giữa ô số để về mặc định.
- **Cỡ chữ gốc lớn hơn**: 100% bây giờ bằng 130% cũ; cỡ đã chọn trước giữ nguyên độ lớn.
- **Độ đậm làm lại**: Readium 3.0 không có quy tắc cho độ đậm. Font Bitter đổi sang đúng tệp Light → ExtraBold
  theo từng mức; font khác được làm dày nét bằng viền chữ. Cả hai đổi ngay, không phải tải lại trang.
- **Thêm font**: chép tệp TTF/OTF vào `ExactaReader/fonts` trên máy, hoặc tab Chữ › Thêm font để chọn tệp.
  Tên họ font lấy theo tên tệp ("Tên Bold Italic.ttf"). Gỡ font ở Cài đặt › Thư viện.
- **Bớt bóng mờ (lưu ảnh)**: cứ 8 trang màn hình được làm sạch một lần (nháy đen); chỉnh 4/8/12/20 trang hoặc tắt,
  và có nút "Làm sạch màn hình ngay", ở tab Khác.
- Home kiểu Bảng tu hành: ô tu hành 2/3, ô pháp bảo 1/3; icon lò luyện mới cho lúc chưa có pháp bảo; logo mới
  (trăng, núi, sách mở). Ảnh 6 kiểu Home: [`docs/home-styles.png`](docs/home-styles.png).

## Bản 0.4

- **Mọi màn dạng tab, không cuộn**: Thư viện (Đang đọc · Chưa đọc · Đã xong · Tất cả, lưới bìa theo trang), Tu vi (Cảnh giới ·
  Độ kiếp · Thống kê · Kinh Các), Pháp bảo (Lò luyện · Bảo khố · Bộ pháp bảo), Đạo hiệu (6 nhóm), Niên kỷ, Cài đặt.
  Nội dung dài lật trang bằng nút ‹ › hoặc vuốt ngang.
- **6 kiểu Home** chọn trong Cài đặt › Giao diện: Bảng tu hành, Cổ điển (A2), Bìa lớn, Kệ sách, Tối giản, Tu tiên.
- **Màn đọc**: dòng tên sách trên đầu trang giữ chữ khỏi mép trên; bảng chỉnh như Exacta.OS: thanh tua có nút chương,
  tab Mục lục (theo trang), Chữ (font, cỡ, độ đậm 5 mức), Trang (giãn dòng, lề, căn chữ, chân trang), Khác.
- **Sửa giãn dòng không ăn**: CSS của sách đè quy tắc của Readium; app chèn CSS riêng vào từng chương lúc mở sách.
  **Độ đậm chữ** mới: font Bitter biến thiên (100–900) kèm nét viền mảnh cho font gốc của sách.
- **Bộ pháp bảo**: 9 bộ × 4 loại, gom đủ bộ có hiệu quả mãi mãi (hiệu quả điều chỉnh cho app: quà gieo hai lần, lò
  nhanh 10%, bí cảnh 4 ngày, hộ pháp phù giữ chuỗi, thiên kiếp tha 1 ngày, bí cảnh thêm khoáng thạch, ngày lễ lò ×2,
  đọc xong thêm khoáng thạch, tu vi +5%); đủ bộ lần đầu thưởng khoáng thạch Địa phẩm.
- **Niên kỷ**: tổng kết mỗi năm âm lịch (giờ đọc, chữ, ngày đọc, chuỗi, ngày và tháng đọc nhiều nhất, sách xong,
  pháp bảo, đạo hiệu, cảnh giới đầu và cuối năm); dịp Tết có chấm báo trên Home.
- **Cài đặt** 5 tab: Giao diện, Đọc sách (font, cỡ, độ đậm, giãn dòng, lề, ngắt từ, thụt đầu dòng, tên sách, chân trang,
  báo tin tu tiên), Tu tiên (mục tiêu đọc mỗi ngày, thời gian chờ tính giờ, sinh nhật, luật chơi), Thư viện (quét lại,
  cách xếp, làm mới bìa, đếm lại chữ), Thông tin (sao lưu và khôi phục dữ liệu vào `ExactaReader/backup`).
- **Logo** app và bộ icon vẽ lại nét mịn cho màn 300 ppi.
- `app/src/test/.../ScreensTest.kt` vẽ mọi màn ở cỡ Neo 3 (1072×1448, 300 ppi) bằng Robolectric:
  `.\gradlew.bat testReleaseUnitTest`, ảnh ra ở `app/build/screens`.

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

- `game/`: cảnh giới (`Realm`), lịch âm và tiết khí (`Lunar`), pháp bảo (`Treasure`), đạo hiệu (`DaoHieu`), Niên kỷ (`NienKy`), chữ của Home (`Texts`).
- `data/`: dữ liệu lưu trong một tệp JSON (`Store`, kèm thiên kiếp, bí cảnh, đạo hiệu), quét sách, bìa, đếm chữ (`Library`), CSS chèn vào chương (`BookCss`).
- `ui/`: bộ vẽ kiểu Exacta.OS (`Ink`), Home 6 kiểu (`HomeView`), khung màn có tab (`TabScreen`), bảng chỉnh khi đọc (`ReaderSheet`).
- Các màn: `HomeActivity`, `LibraryActivity`, `ReaderActivity`, `TuViActivity`, `PhapBaoActivity`, `DaoHieuActivity`, `NienKyActivity`, `SettingsActivity`.

## Hướng tiếp

Đồng bộ với Exacta.OS; kỳ ngộ, bế quan, hợp luyện, linh thạch (chưa có trên app).

Đạo hiệu khác Exacta.OS ở bốn chỗ, vì app chưa có kỳ ngộ, bế quan, hợp luyện, linh thạch: Phúc Duyên Thâm
Hậu (20 khoáng thạch quà), Bế Quan Khổ Tu (tổng 100 giờ đọc), Lò Hỏa Thuần Thanh (30 pháp bảo), Nguyệt Hạ Độc
Thư (đọc đêm Trung Thu).

## Giấy phép bên thứ ba

- Readium Kotlin Toolkit: BSD-3-Clause.
- Font Bitter: SIL Open Font License 1.1 (`app/src/main/assets/fonts/OFL-Bitter.txt`).
