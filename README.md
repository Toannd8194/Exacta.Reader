# Vạn Quyển Tiên Đồ (Exacta Reader)

Ứng dụng đọc EPUB cho máy đọc sách e-ink Android (thử trên iReader Neo 3: Android 11, 300 ppi,
cảm ứng, không có phím lật trang), mang lối chơi tu tiên của Exacta.OS: đọc sách là tu luyện.
Lõi đọc là Readium Kotlin Toolkit; giao diện vẽ riêng theo kiểu Exacta.OS, đen trắng, không hiệu ứng.

Tải bản cài: [`dist/Van-Quyen-Tien-Do-3.4.apk`](dist/Van-Quyen-Tien-Do-3.4.apk) (chép vào máy rồi cài; cài đè lên bản cũ được). Từ bản 0.3, mỗi bản APK được giữ lại trong `dist/` với tên phiên bản.

Cẩm nang tu tiên (PDF, đúng khổ màn iReader Neo 3, theo luật bản 2.8): [`dist/Van-Quyen-Tien-Do-2.8-Cam-nang.pdf`](dist/Van-Quyen-Tien-Do-2.8-Cam-nang.pdf).
Gồm luật chơi, chiến lược cày (xếp lò, phá cảnh, tiêu linh thạch, gom 72 loại), thư viện công pháp, tâm pháp, thuật
pháp, 72 pháp bảo, 18 bộ, 18 đan dược và phù chú, 72 đạo hiệu. Dựng lại bằng `python tools/cam_nang.py` (số liệu đọc thẳng từ mã nguồn, in bằng Edge headless).

Bản thiết kế nâng cấp hệ tu tiên (Nhân giới tới Linh giới, Độ Kiếp, dừng ở Phi Thăng kỳ trước cửa Lục Giới):
[`docs/Chien-luoc-nang-cap-tu-tien.md`](docs/Chien-luoc-nang-cap-tu-tien.md), bản PDF
[`docs/Chien-luoc-nang-cap-tu-tien.pdf`](docs/Chien-luoc-nang-cap-tu-tien.pdf) (`python tools/md_pdf.py <tệp .md>`).
Bản 2.0, 2.1, 2.2 là ba bước đầu của lộ trình; các bước 2.3 – 2.7 còn đó. Từ 2.1, bản thiết kế có thêm chương Độ Kiếp
(giai đoạn đặc biệt chín tầng kiếp) và chương Phi Thăng kỳ: cảnh giới cuối của Linh giới, nơi Lục Giới Môn mở và người
tu được một trong sáu đại thiên thế giới tiếp dẫn, theo cả đời tu và nghiệp lực.

Bản thiết kế phần hai (sáu giới sau Lục Giới Môn, lộ trình 2.8 và 3.0 – 3.6):
[`docs/Thiet-ke-phan-hai-luc-gioi.md`](docs/Thiet-ke-phan-hai-luc-gioi.md), bản PDF
[`docs/Thiet-ke-phan-hai-luc-gioi.pdf`](docs/Thiet-ke-phan-hai-luc-gioi.pdf).

Kế hoạch nâng cấp công pháp, tâm pháp, thuật pháp, Tàng Kinh Các (3.1 – 3.4):
[`docs/Ke-hoach-nang-cap-cong-phap.md`](docs/Ke-hoach-nang-cap-cong-phap.md).

## Bản 3.4 · Tàng Kinh Cửu Lâu

- **Chín tầng lầu**: mỗi ngọc giản nằm ở một tầng (công pháp theo phẩm: Hoàng ở Nhất lâu, Huyền Nhị lâu, Địa Tam lâu, Thiên
  Ngũ lâu, Linh Thất lâu, Tiên Cửu lâu; tâm pháp và thuật pháp mỗi bộ một tầng).
  - Tầng 1 – 3 mở cho mọi người, mượn bằng linh thạch (40, 60, 90).
  - Tầng 4 – 6 cần ở trong tông và cống hiến từng có 300, 800, 1.500; tầng 7 – 9 cần chức Chân truyền, Trưởng lão, Thái thượng
    trưởng lão. Từ tầng 4 trả bằng **cống hiến**, và đó là kinh các của tông: giữ trước các mạch hợp đường tu của tông.
  - Bộ công pháp đúng phẩm cửa kế tiếp cần luôn có: ở tầng của nó nếu đã mở, không thì ở Tam lâu, giá cao hơn. Không ai bị kẹt
    cửa vì chưa vào tông.
  - Mỗi thứ Hai các tầng đổi sách. Đan phương vẫn học được ngay.
- **Án tham ngộ**: ngọc giản mượn ở Tàng Kinh hay đổi ở Bảo khố tông môn nằm trên giá; quyển đầu nằm trên án và nhận chữ đọc
  ở bất kỳ cuốn nào, đủ chữ thì học được: Hoàng 1 vạn, Huyền 1,5 vạn, Địa 2,5 vạn … Tiên 30 vạn. Chạm quyển trên giá để đặt lên
  án. Tông Vạn Quyển tham ngộ nhanh 25%. Ngọc giản rơi từ kỳ ngộ, bí cảnh vẫn học ngay.
- **Tàn quyển**: công pháp Linh và Tiên phẩm rơi ở kỳ ngộ, bí cảnh thành 3 – 5 mảnh; đủ mảnh thì tự ghép, lên giá tham ngộ.
- **Phản tông thì phế công**: bộ học từ kinh các hay Bảo khố của một tông đã phản mất đặc hiệu tầng 5 và tuyệt học, cho tới khi
  đổi sang bộ khác cùng mạch.
- **Đồ phổ** (tab mới): 126 công pháp, tâm pháp, thuật pháp; bộ đã thấy hiện tên, bộ chưa thấy hiện bóng mờ với mạch, phẩm và
  tầng lầu cất nó.
- **Tự sáng công pháp** (từ Hóa Thần): hai bộ viên mãn và 10 ngộ tính thành một bộ mang tên người tu đặt. Phẩm bằng bộ thấp hơn
  cộng một, giữ mạch của bộ thứ nhất, lấy một đặc hiệu mỗi bên (đặc hiệu của bộ thứ nhất làm tuyệt học), không có khuyết điểm.
  Tối đa ba bộ.
- versionCode 340.

## Bản 3.3 · Thần Thông Quảng Đại

- **36 thuật pháp, sáu loại**: công kích 9, phòng ngự 7, độn thuật 5, phụ trợ 4, thần thông 5, và loại mới **khống chế** 6
  (Khốn Long Tác, Đằng La Triền, Băng Phong Thuật, Định Thân Thuật, Lôi Võng Thuật, Mê Hồn Chú). 15 thuật cũ giữ nguyên.
- **Thi triển**: thuật được dùng khi biến cố tới.
  - Bí cảnh: thi triển công kích (trúng hay trượt theo thuần thục), khống chế trói yêu thú lấy thưởng chắc chắn, trấn áp chủ
    bí cảnh rồi quyết chiến, độn thuật thoát thân.
  - Kiếp: phòng ngự đỡ ngày lỡ, Thiên Nhãn và Thần Thức Hóa Kiếm chặn tâm ma.
  - Đột phá hỏng: độn thuật bớt hậu quả.
  - Thuật mới tự ra tay: Thanh Tâm Chú (tâm ma vượt 60 thì −15), Hồi Xuân Thuật (bị thương trong bí cảnh thì chữa ngay), Lôi
    Phạt Thiên Uy (quyết chiến chắc thắng), Băng Phong Vạn Lý (đột phá hỏng tâm ma không tăng).
- **Hồi chiêu tính bằng ngày đọc**, không phải ngày lịch: mỗi ngày đọc từ 10 phút bớt một ngày. Công kích 1 ngày, phụ trợ và
  khống chế 2, phòng ngự và độn 3, thần thông 5 – 7. Thuật luôn có tác dụng (Liễm Tức, Pháp Tướng) không có hồi chiêu.
- **Thuần thục** năm bậc theo điểm: mỗi lần dùng 3 điểm, mỗi buổi đọc 30 phút lúc đang mang 1 điểm (Tiểu Thành 9, Đại Thành 24,
  Viên Mãn 45, Hóa Cảnh 75; save cũ giữ nguyên bậc). Mỗi bậc thuật trúng dễ hơn 10%, hai bậc hồi chiêu bớt một ngày.
- **Hợp kích**: mang hai thuật cùng hành thì mở một hiệu ứng thứ ba. Kim chắc trúng, Mộc kiếp lỡ thêm một ngày, Thủy chặn thêm
  một lần tâm ma, Hỏa gấp đôi linh thạch, Thổ đột phá hỏng không hụt linh lực, Lôi quyết chiến +15%, Băng khống chế thêm một
  phần thưởng, Phong mọi thuật hồi chiêu nhanh một ngày.
- **Bản mệnh thần thông**: mỗi loại trong 12 Kim Đan mở một thần thông riêng, có tác dụng như đặc hiệu công pháp (ví dụ Canh Kim
  Kiếm Khí: đột phá +10%; Đại Nhật Chiếu Thế: linh thạch từ chữ +40%; Thanh Phong Ngự Không: bí cảnh tuần bớt một ngày).
- **Ô mang thuật**: hai ở Luyện Khí, thêm một mỗi đại cảnh giới, tối đa sáu.
- Tab Thuật pháp: trang đầu là các thuật đang mang (thuần thục, hồi chiêu), hợp kích và bản mệnh; các trang sau là thuật đã có,
  chạm «Xem» để học, mang hay cất.
- Sửa lỗi có từ 2.1: thuật đầu tiên nhận khi linh căn thức tỉnh bị mang hai lần (phòng ngự có thể bị tính đôi); save cũ được
  dọn khi mở.
- versionCode 330.

## Bản 3.2.1 · Bát Quái Khai Trận

- **Khoáng thạch vẽ lại thành pixel thật**: lưới 32 × 32 ô, viền một ô, mỗi chất liệu một dải sáu màu (bóng ngả lạnh, sáng ngả
  ấm), chọn độ sáng để màn e-ink đen trắng vẫn rõ khối. Sáu cấp:
  1. Hoàng: hòn đá góc cạnh, vỡ thành những mặt phẳng.
  2. Huyền: tinh thiết vừa đào lên, sáng như thép, đất còn bám.
  3. Địa: hồng ngọc ẩn trong đá, ló ra từ khe vỡ.
  4. Thiên: khối thiên thạch rực lửa, kéo đuôi lửa.
  5. Âm Dương: viên ngọc Thái Cực, hai luồng khí tức sáng tối quấn quanh rồi bốc lên.
  6. Hỗn Độn: viên ngọc hỗn độn trong đài cánh vàng, nửa thật nửa ảo: một bên đặc, một bên lúc có lúc không, bóng viên ngọc
     trôi đi, cánh vỡ thành bụi sáng.

  Linh thạch cũng vẽ lại ở lưới 32.
- **Tĩnh thất**: bế quan tối đa 90 phút (30, 45, 60, 75, 90); đẳng cao nhất Thiên Cung là **tiềm tu** 120 phút. Bỏ mức 180
  phút. Sáu đẳng xếp hai cột, vừa một trang. Bế quan dở ở mức cũ quá 120 phút được hạ về 120.
- **18 trận pháp** (thêm 11): Tụ Tài (linh thạch từ chữ +15%), Cố Nguyên (căn cơ +5), Minh Ngộ (đọc trọn một cuốn ngộ tính +1),
  Định Thần (tâm cảnh +5), Kim Cương (thể phách +15%), Ngũ Hành (chữ vào tầng công pháp +20%), Chiêu Duyên (kỳ ngộ +15%), Dẫn
  Lôi (đột phá +3%), Luyện Khí (tế luyện +20%), Tụ Hồn (thần thức +5), Cửu Cung Bát Quái (linh lực +6%, tâm ma chậm 10%, cần
  linh mạch Bát giai). Vẫn tối đa ba trận nhãn, nên phải chọn. Tab Trận pháp chia trang, trận đang bày đứng đầu.
- versionCode 321.

## Bản 3.2 · Tâm Đăng Bất Diệt

- **18 tâm pháp trong bốn phái**: Đạo gia (Thanh Tâm Quyết, Tĩnh Tọa Kinh, Băng Tâm Quyết, Vong Ngã Chân Quyết, Dưỡng Khí
  Thiên), Phật môn (Định Hồn Chân Ngôn, Vô Niệm Kinh, Kim Cương Bất Động Tâm, Từ Bi Quán, Bát Nhã Tâm Đăng), Ma đạo (Đạo Tâm
  Chủng Ma, Huyết Tâm Quyết, Thiên Ma Giải Thể, Vạn Niệm Phệ Tâm), Nho môn (Hạo Nhiên Chính Khí, Thư Hương Dưỡng Tâm, Tam Tỉnh
  Ngô Thân, Văn Tâm Quyết). Bảy bộ cũ giữ nguyên đặc chất và số lần vận chuyển.
- **Năm cảnh**: Nhập môn 5, Tiểu thành 15, Đại thành 30, Viên mãn 60, Hóa cảnh 100 lần vận chuyển. Hiệu ứng thứ nhất mở ở
  Nhập môn và mạnh lên ở Tiểu thành, Viên mãn; Đại thành mở hiệu ứng thứ hai (gấp đôi ở Hóa cảnh) và tâm cảnh +5; Hóa cảnh
  nâng tâm cảnh một bậc. Hiệu ứng tâm pháp là các đặc hiệu của công pháp, cộng dồn với công pháp.
- **Vận chuyển theo lối đọc**: mỗi buổi đọc liền 30 phút vẫn là một lần; đọc đúng lối của bộ thì tính hai. Lối gồm: không nhảy
  chương trong ngày, buổi tới 45, 60, 90 phút, chuỗi từ 7 ngày, đọc sớm, đọc đêm, cả ngày một cuốn, buổi thứ ba trong ngày, tác
  giả mới, đọc lại, đọc khi tâm ma từ 30, ngày đã nhảy chương, cuốn đã quá nửa. Hạo Nhiên Chính Khí: mỗi cuốn đọc trọn tính 5 lần.
- **Tương sinh, xung khắc**: Ma đạo không vận cùng Phật môn hay Nho môn, và mỗi bộ Ma đạo đang vận làm tâm ma tăng nhanh 15%.
  Đạo gia đi cùng Phật môn thì cả hai vận chuyển nhanh 20%.
- **Ba ô tâm pháp**: một ở Trúc Cơ, hai ở Nguyên Anh, ba ở Luyện Hư.
- Tab Tâm pháp: trang 1 là các ô đang vận, lối của từng bộ, tương sinh xung khắc và mọi thứ tâm pháp đang cho; các trang sau
  là các bộ đã có (chạm «Xem» để vận hay thôi vận).
- **Khoáng thạch và linh thạch vẽ lại** ở lưới 128 (gấp bốn), 16 tầng xám, mặt cắt nhận ánh sáng. Mỗi phẩm một vẻ, đẹp dần:
  Hoàng chỉ là hòn đá có vân thạch anh; Huyền là đá bổ đôi, vân mã não quanh hốc tinh thể; Địa là cụm bảy tinh thể; Thiên là
  thiên thạch rực lửa; Âm Dương là cả một hành tinh, ngày đêm chia theo Thái Cực, có vành đá bay quanh; Hỗn Độn là cả một vũ
  trụ trong vỏ cầu, hư vô mà thật chất: thiên hà xoay giữa hư không, vỏ cứng sáng, một bên vỡ ra trôi đi. Linh thạch: cụm
  khối tinh thể trong suốt trên ụ đất.
- versionCode 320.

## Bản 3.1 · Vạn Pháp Trùng Tu

- **72 công pháp trong 12 mạch** (Kim, Mộc, Thủy, Hỏa, Thổ, Lôi, Băng, Phong, Kiếm, Thể, Thần, Hỗn Nguyên), mỗi mạch đủ sáu phẩm
  Hoàng → Tiên. 36 bộ cũ giữ nguyên, thêm 36 bộ mới.
- **Ba đặc hiệu mỗi bộ**: mở ở tầng 1, tầng 5, và **tuyệt học** ở tầng 9 (tính gấp đôi). Cùng một loại đặc hiệu thì cộng dồn
  (ví dụ Liệt Hỏa Công viên mãn: lò luyện nhanh 60%). Thêm loại mới: linh lực, tâm cảnh, luyện đan, tế luyện, Độ Kiếp, chữ vào
  tầng công pháp, bí cảnh tuần bớt một ngày.
- **Khuyết điểm** từ Địa phẩm: tâm ma tăng nhanh, linh thạch ít đi, căn cơ hay tâm cảnh −5, lĩnh ngộ tốn thêm, hồi khí lâu hơn,
  thể phách giảm, kỳ ngộ ít đi.
- **Lối vận công**: mỗi mạch một lối đọc (giờ hành của năm hành; sau nửa đêm hay đêm rằm cho Lôi; buổi liền không nhảy chương
  cho Băng; sách mới mở cho Phong; sách đã quá nửa cho Kiếm; buổi từ 45 phút cho Thể; tác giả mới cho Thần; đọc đủ bốn khung
  giờ trong tuần cho Hỗn Nguyên). Đọc đúng lối: chữ tính ×1,5 vào tầng công pháp (không vào tu vi).
- **Xung quan** thay lĩnh ngộ ở tầng 4, 7, 9: đặt ngộ tính, rồi một buổi đọc liền 30, 60, 90 phút. Dừng sớm là khí nghịch: tâm
  ma +5, ngộ tính trả lại để thử lần sau.
- **Chuyển tu**: bộ viên mãn truyền một nửa số chữ cho bộ phẩm cao hơn cùng mạch (tới tầng 3), một lần.
- **Phụ tu** từ Kim Đan: vận thêm một bộ khác mạch, nhận đặc hiệu tầng 1 của nó, nó lên tầng bằng nửa số chữ.
- Màn Công pháp: trang 1 bộ đang vận (tầng, ba đặc hiệu, khuyết điểm, lối vận công, phụ tu), trang 2 tốc độ tu và mọi đặc hiệu
  đang có, các trang sau là các bộ khác (chạm «Xem» để vận hay đặt phụ tu).
- **Khoáng thạch vẽ lại** kiểu pixel như linh thạch: đá thô, tinh thể phá đá, hốc tinh thể, ngọc giác cắt, cầu Âm Dương, tinh thể
  Hỗn Độn.
- versionCode 310.

## Bản 3.0.1 · Sơn Môn Định Phận

- **Tông môn mở dần theo cảnh giới**: 10 tông nhận đệ tử Luyện Khí (năm tông khai sơn, Thanh Vân Môn, Hắc Thạch Bảo, Bách Gia
  Học Cung, Lạc Hà Kiếm Trang, Phù Lục Sơn), thêm 9 ở Trúc Cơ, 9 ở Kim Đan, 4 ở Nguyên Anh (Bá Thể Tông, Long Tượng Môn, Thái
  Thượng Thư Viện, Thiên Kiếm Tông), 4 ở Hóa Thần (Vô Niệm Thiền Viện, Thục Sơn Kiếm Phái, Đan Đỉnh Tông, Thiên Công Các).
  Trong các tông đã mở, vẫn chỉ 6 – 8 tông hợp tư chất chịu nhận.
- **Chức vụ cần ở lâu**: Nội môn 14 ngày, Chân truyền 60, Trưởng lão 150, Thái thượng trưởng lão 300 ngày ở tông, cùng cống
  hiến và cảnh giới như trước.
- **Bổng ngày**: mỗi ngày đọc từ 10 phút nhận linh thạch theo chức (1, 2, 4, 7, 12) cộng 1 mỗi 500 cống hiến (tối đa 10).
  Bổng lộc tuần khi xong ba nhiệm vụ vẫn giữ.
- **Phản tông** thay cho «chuyển tông một lần»: rời tông lúc nào cũng được, nhưng mất toàn bộ cống hiến và chức, tông truy thu
  30% linh thạch, tâm ma +15, 30 ngày không tông nào nhận, tông cũ không bao giờ nhận lại, nghiệp lực −10 mỗi lần.
- versionCode 301.

## Bản 3.0 · Cửu Tiêu Khai Thiên

Phần hai bắt đầu với **Cửu Tiêu Vân Khuyết**. Năm giới kia vẫn đứng ở tiền đình, chữ đọc vẫn được ghi.

- **Mở giới**: người tu Cửu Tiêu vào tiên lộ; những ngày ở tiền đình quy ra tiên lực từ sổ đọc của 2.8 (ngày chưa có sổ tính
  1,5 điểm), linh thạch đổi ra tiên thạch 100 : 1, phẩm Thiên Tiên đã có thành bậc tương ứng. Một màn khai thiên, một lần.
- **Tiên lộ chín bậc** (Thiên Tiên … Cửu Tiêu Tiên Đế): mỗi bậc cần đủ chữ từ ngày qua cửa (60 … 1.040 vạn) và tiên lực
  (200 … 4.400). Mỗi bậc chia sơ, trung, hậu kỳ, đại viên mãn theo chữ.
- **Tiên lực**: mỗi 2.500 chữ một điểm, nhân hệ số cân bằng năm thước đo (60 – 140%); cộng khi ngày đọc xong. Hành trang
  cộng thêm: tiên khí (cấm chế bản mệnh, mỗi cấp +1%), tiên công (+1% mỗi phẩm, hợp giới +5%), tiên căn hợp giới +10%, kiếp ấn,
  đạo vực có Kim hay Lôi pháp tắc +5%.
- **Thiên quy tuần**: mỗi tuần một điều (đọc 5 ngày, không nhảy chương, đọc trọn một cuốn…); giữ được +30 tiên lực.
- **Tiên chức** theo bậc (Thiên Binh … Tiên Đế), ưu đãi cộng dồn.
- **Tiên phủ**: Tiên điền, Tiên lô, Tiên trận, mỗi gian 5 cấp, nâng bằng tiên thạch (mỗi 4 vạn chữ, lên bậc, qua kiếp, việc
  thế lực).
- **Ba thế lực** Thiên Đình, Dao Trì, Tử Phủ: ba việc mỗi tháng, cống hiến lên chức.
- **Ba tiên kiếp**: Thiên Nhân kiếp ở bậc 3, 6 (21 ngày trong 30, năm thước đo không dưới ngưỡng), Đại kiếp Tiên Đế ở bậc 9
  (81 ngày đọc trong 90, ngày cuối cân bằng từ 120%). Hỏng: thương thế 7 ngày, tiên lực giảm nửa, lần sau +10%.
- Bậc 9: màn kết phần hai của Cửu Tiêu, đạo hiệu tối thượng «Cửu Tiêu Tiên Đế».
- Home: nút **Thiên Đình**, thẻ cảnh giới hiện tiên vị và tiên lực. Linh thạch vẽ lại thành cụm tinh thể.
- versionCode 300.

## Bản 2.8 · Tàng Kinh Lưu Ảnh

Bản chuẩn bị cho phần hai: mỗi giới sống bằng một lối đọc, nên từ nay app ghi sổ theo **từng ngày**.

- **Sổ đọc theo ngày**: giây đọc theo từng giờ, từng buổi đọc và độ dài, giây đọc ở mỗi cuốn, tâm ma cao nhất và độ cân bằng năm
  thước đo cuối buổi. Thêm sự kiện: sách bỏ từ 90 ngày được mở lại, đọc lại trọn một cuốn, đột phá hay kiếp hỏng, chuyển thế.
- **Chữ đọc lại**: sách đã đọc trọn, mở ra đọc lại từng trang từ chỗ đang đọc lại; nhảy bằng mục lục hay thanh trượt không tính,
  như lần đọc đầu. Không cộng tu vi. Đọc tới cuối là một lần đọc lại trọn.
- **Chữ ở sách quay lại**: chữ đọc trong cuốn bỏ dở được mở lại sau 90 ngày, tới khi đọc xong.
- **Thể loại, ngôn ngữ**: đọc từ thông tin EPUB (subject, language). Sách cũ được đọc lại thông tin một lần khi mở Thư viện.
- **Niên kỷ › Lối đọc** (2 trang): lối đọc 30 ngày qua (đọc đêm, sáng sớm, các buổi, nhảy chương, chữ đọc lại, sách quay lại,
  tác giả, thể loại, ngôn ngữ), biểu đồ giờ đọc; trang 2 tính thử tài nguyên của cả sáu giới theo luật nháp phần hai.
- **Tiền đình** (Tiên đồ): tài nguyên dự tính của giới mình từ ngày qua cửa; ngày trước khi có sổ tính 1,5 điểm.
- Sổ bắt đầu từ ngày cài bản 2.8; dữ liệu cũ chỉ có tổng (không theo giờ từng ngày) nên không dựng lại được.
- versionCode 280.

## Bản 2.7.2 · Hộ Thân Họa Phù

- **Ô trang bị** (Pháp bảo › Bảo khố, chạm một món để đeo hay tháo): từ Kim Đan đeo được 2 món, thêm một ô mỗi đại cảnh giới
  (9 ở Tiên nhân); bản mệnh luôn đeo, không tốn ô. Từ Kim Đan chỉ món đeo mới dùng được trong biến cố bí cảnh (liều đánh, phá
  bằng pháp bảo, điện chủ) và lôi kiếp Độ Kiếp; mỗi món đeo thêm sau món đầu là hộ thân +1% ở lôi kiếp, thiên kiếp (tối đa 5%).
  Bản cũ: máy đeo sẵn những món mạnh nhất.
- **Phù thất** (Pháp bảo › Túi đồ, trang 3): vẽ chín loại phù (Thiên Hỏa, Hộ Lô, Định Thần, Tầm Bảo, Bí Cảnh, Tụ Bảo, Luyện Hóa,
  Phong Ấn, Dẫn Lôi) bằng phù chỉ, chu sa và một buổi đọc liền 20 – 45 phút; dừng sớm là đứt nét. Tỉ lệ theo thần thức (phù cao
  cần thần thức cao), Bách nghệ và Phù Lục Sơn +10%; vẽ khéo được hai tấm. Phù chỉ mua 10 LT hoặc nhặt ở bí cảnh; chu sa chỉ có
  ở bí cảnh tuần.
- versionCode 272.

## Bản 2.7.1 · Tế Luyện Bố Trận

- **Tế luyện cấm chế** (Pháp bảo › Tế luyện): đặt một pháp bảo lên lửa, chữ đọc luyện cấm chế, tầng n cần 2.000 + 1.000 × n
  chữ. 1 – 9 pháp khí, 10 – 18 pháp bảo, 19 – 27 linh bảo, 28 – 36 thông thiên linh bảo (chỉ ở Linh giới). Qua mốc tầng 9, 18,
  27 cần một khoáng thạch và 50 / 150 / 400 linh thạch. Món luyện sâu nhất: linh lực +1% mỗi 4 tầng, đột phá +1% mỗi phẩm,
  bí cảnh liều đánh +4% mỗi phẩm, lôi kiếp Độ Kiếp +2% mỗi phẩm.
- **Trận pháp** (Động phủ › Trận pháp): Tụ Linh (linh lực +10%), Hộ Sơn (mỗi kỳ kiếp lỡ thêm một ngày, lôi kiếp +8%), Mê Tung
  (bí cảnh +10%), Thanh Tâm (tâm ma chậm 20%), Dưỡng Linh (dược viên +5 năm mỗi ngày), Đan Hỏa (luyện đan +8%), Tụ Bảo (lò luyện
  nhanh 15%). Dựng bằng linh thạch; trận nhãn 1, thêm một ở Tứ giai và Thất giai linh mạch.
- Ảnh linh thạch vẽ lại kiểu pixel. Tab sáu ô thu chữ nhỏ hơn thay vì cắt; Phường thị đổi tên tab thành Chợ.
- versionCode 271.

## Bản 2.7 · Tiên Môn Tiếp Dẫn (hết phần một)

- **Qua Lục Giới Môn** (đột phá vào Tiên nhân): giới cảm ứng mạnh nhất tiếp dẫn; ghi lại những gì mang qua cửa (bản mệnh
  pháp bảo, công pháp đang vận, linh căn, kiếp ấn), phần còn lại ở lại Linh giới làm di sản.
- **Tiên hiệu theo giới**: Tiếp Dẫn Tiên Quân, Huyết Nguyệt Ma Tôn, Bát Nhã Bồ Tát, Hồng Hoang Chiến Hoàng, Vong Xuyên Minh
  Vương, Thư Hải Văn Chủ. Hiện trên thanh trên của Home (trừ khi đang chọn mang một đạo hiệu khác); chạm để mở Tiên đồ.
- **Màn kết phần một** (màn Tiên đồ, tự mở một lần khi vừa qua cửa; nút Tiên đồ thay Linh giới ở Home): Qua cửa (tiên hiệu,
  lời tiễn của giới, một đời tu bằng con số), Đạo đồ (từng cảnh giới, ngày tới, Kim Đan, Nguyên Anh, Hóa Thần, pháp tắc, đạo
  vực, kiếp ấn), Hành trang, Tiền đình.
- **Tiền đình**: năm giới chưa có phần hai; người tu đứng ở tiền đình, tên cảnh giới là «Nghịch Tiên · tiền đình»…, mọi chữ
  đọc từ ngày qua cửa được ghi lại để tính khi giới mở. Cửu Tiêu Vân Khuyết tiếp nối chín phẩm Thiên Tiên.
- Ai đã là Tiên nhân từ bản cũ: được tiếp dẫn theo cảm ứng hiện tại và xem màn kết một lần.
- versionCode 270.

## Bản 2.6 · Cửu Cửu Quy Nhất

- **Độ Kiếp chín tầng** (Linh giới › Độ Kiếp, mở ở Đại Thừa): Thiên Lôi, Địa Hỏa, Cương Phong (5 ngày × 55 phút, đỡ bằng
  pháp bảo, bản mệnh, nhục thân); ba tầng pháp tắc kiếp (đọc trọn một cuốn của tác giả mới trong 30 ngày, đỡ bằng pháp tắc,
  đạo vực, thần thức); hai tầng tâm ma kiếp (7 ngày một cuốn, không nhảy chương, không đổi sách, đỡ bằng tâm cảnh, tâm pháp);
  cửu cửu thiên kiếp (chín đợt 60 phút trong 15 ngày, đợt cuối là tâm lôi). Chữ đọc giữa hai tầng là **kiếp lực**. Hỏng
  một tầng không mất tầng đã qua. Mỗi tầng qua để lại một **kiếp ấn**, nghiêng Lục Giới. Qua tầng chín là vào Phi Thăng.
- **Thiên nhân ngũ suy** ở Phi Thăng: căn cơ, tâm cảnh −15; trong 30 ngày kéo năm thước đo về ngưỡng thì Lục Giới Môn mới mở.
- **Thọ nguyên**: mỗi ngày là một năm, mỗi cảnh giới một hạn (Tu vi › Thân, trang 2); sắp cạn thì đột phá liều mạng +10%.
  **Chế độ Chân Tu** (Cài đặt › Chế độ tu, tùy chọn): cạn thọ nguyên là tọa hóa, chuyển thế tu lại từ Luyện Khí, nhanh gấp
  rưỡi tới cảnh giới kiếp trước; Vong Xuyên Bỉ Ngạn nhớ những lần chuyển thế.
- **Động phủ**: tĩnh thất sáu đẳng (Thạch Thất 30 phút tới Thiên Cung: tiềm tu 120 phút), nâng bằng linh thạch, đẳng cao lò nóng hơn;
  32 đan phương (thêm luyện thần, thọ nguyên, căn cơ, ngộ đạo, đan phá cảnh Hợp Thể, Đại Thừa, Độ Kiếp, Song Anh), mua đan
  phương ngay ở đan phòng; 12 linh thảo; chọn hạt gieo bằng thẻ.
- versionCode 260.

## Bản 2.5 · Phá Toái Hư Không

- **Linh giới** (màn mới; từ Hóa Thần, nút Linh giới thay chỗ Đạo hiệu ở Home, Đạo hiệu vẫn mở từ thanh trên):
  - **Phi thăng** ba bước: không gian tiết điểm (bí cảnh 4 tuần liền), trảm trần duyên (một bản mệnh pháp bảo, hai nếu Song
    Anh, và ba món mang theo), phi thăng kiếp 7 ngày × 55 phút.
  - Ở Linh giới, bộ pháp bảo nào không có món nào được mang lên thì bị linh khí áp chế, mất hiệu quả; mỗi 30 ngày tế luyện
    thêm được một món. Bản mệnh ôn dưỡng, lớn một tầng mỗi 20 giờ đọc (linh lực +1% mỗi tầng).
  - **Tám pháp tắc** (Kim, Mộc, Thủy, Hỏa, Thổ, Lôi, Không gian, Thời gian), mở theo linh căn, công pháp, đạo lộ; mỗi lúc
    tham ngộ một, qua ba thử thách đọc. Hợp Thể cần một pháp tắc và **thân thần hợp nhất** (thể phách 300, thần thức 24;
    tạp linh căn được nửa). Đại Thừa cần ba pháp tắc và một **đạo vực** tự đặt tên: ba pháp tắc trong vực tính gấp đôi.
- **36 tông môn** theo năm dòng; mỗi tông đòi điều riêng (linh căn, dị linh căn, công pháp, đạo lộ, căn cơ, thể phách,
  thần thức, chuỗi ngày, sách đọc trọn, lò đan, pháp bảo, cảnh giới). Mỗi lúc chỉ 6 – 8 tông hợp nhất chịu nhận (8 với
  Thiên linh căn). Thẻ tông hai dòng.
- **Cửa chưa tới thì khóa**: tab nào chưa tới gần (Kim Đan mở ở Trúc Cơ, Hóa Thần ở Nguyên Anh, pháp tắc ở Luyện Hư…) phủ
  xám nhạt, giữa trang hiện khung «Tiên lộ còn xa». Chạm loại Kim Đan để xem điều kiện kết đan và mình đang ở đâu.
- **Độ đậm** khi đọc theo %, bước 5% như cỡ chữ; mức đậm đang dùng thành 100%.
- versionCode 250.

## Bản 2.4 · Ngũ Lộ Đồng Quy

- **Đạo lộ** (màn mới, nút Đạo lộ ở Home): năm đường Pháp tu, Thể tu, Thần tu, Kiếm tu, Bách nghệ, chọn một lần ở Trúc Cơ,
  trước khi ngưng đan. Mỗi đường sống bằng một cách đọc riêng (chữ đọc, buổi liền 60 phút, sách đọc trọn và tác giả mới, đọc
  trọn nhanh và chuỗi ngày, lò đan và pháp bảo), có kỳ ngưng đan riêng, một cái mạnh và một cái yếu thật.
- **Mười hai Kim Đan**: ngũ hành (Canh Kim, Ất Mộc, Nhâm Thủy, Bính Hỏa, Mậu Thổ), biến dị (Tử Lôi, Huyền Băng, Thanh
  Phong), Âm Dương (Thái Dương, Thái Âm), Sinh Tử (Trường Sinh, Tịch Diệt), kết ra theo linh căn, công pháp, giờ đọc ngày
  đêm, căn cơ và những lần ngã. **Nguyên Anh kế thừa đan**: mang tên đan («Tử Lôi Kiếm Anh»), tỉ lệ kết anh riêng, tốc
  độ của đan tăng thêm một nửa.
- **Sáu Hóa Thần** theo đạo lộ (Song Anh thành Song Thần Đồng Tu), mỗi loại một lợi một hại đè lên cửa **Hợp Thể** (tỉ lệ,
  số ngày thiên kiếp) và **Độ Kiếp**.
- **Luyện thể, luyện thần**: bảy bậc thể giúp qua lôi kiếp, thiên kiếp; chín trọng thần giúp phá tiểu cảnh. **Kiêm tu**
  từ Nguyên Anh; **Song Anh** cho người kiêm Thể và Thần.
- **Lục Giới** không còn tự chọn, sáu giới có tên mới: Cửu Tiêu Vân Khuyết, Huyết Nguyệt Trầm Uyên, Bát Nhã Liên Đài, Hồng
  Hoang Cổ Địa, Vong Xuyên Bỉ Ngạn, Thư Hải Vô Nhai. **Lục Giới cảm ứng** chấm điểm cả đời tu: linh căn, công pháp, tâm pháp,
  đạo lộ, Kim Đan, Hóa Thần, đời tu, tông môn, và từ Phi Thăng là **Nghiệp lực** (thiện nghiệp, ác nghiệp của cả đời tu).
  Giới cao điểm nhất tiếp dẫn; cửa Lục Giới Môn đòi cảm ứng rõ.
- Tu vi › Đột phá trang 2 gọn lại: đường tu thành một hàng dấu, chỉ cửa ải trước mắt và cửa kế tiếp.
- Home kiểu Bìa lớn và Kệ sách: dải cảnh giới có lời chào và tiết khí kèm lời bình. Hàng nút có thêm Đạo lộ.
- versionCode 240.

## Bản 2.3.1 · Lục Giới Phân Đồ

- **Phi Thăng** là cảnh giới cuối của Linh giới; **Độ Kiếp** là chặng đặc biệt giữa Đại Thừa và Phi Thăng. Từ Phi Thăng lên
  Tiên nhân qua **Lục Giới Môn**: chọn một trong sáu giới (Tiên, Ma, Phật, Yêu, Minh, Thần), thành một trong sáu loại tiên
  nhân (Thiên Tiên, Ma Tiên, Phật Tiên, Yêu Tiên, Quỷ Tiên, Thần Tiên). Mỗi giới có một lời gọi rút từ cả đời tu: có lời gọi
  thì đột phá +10%, cửa tối −10%. Ai đã là Tiên nhân được chọn một lần.
- **Home**: thanh trên hiện đạo hiệu đang mang, căn giữa (chạm để mở Đạo hiệu); đạo hiệu bỏ khỏi thẻ cảnh giới; hàng nút
  dưới đều nhau (thêm Tông môn, Đạo hiệu thành nút thường). Thẻ cảnh giới sắp lại: lời chào riêng một dòng, cảnh giới lớn
  kèm tu vi, căn cơ, tâm ma, thanh tiến độ, số liệu trong ngày, tiết khí kèm một lời bình về việc tu (24 lời), cửa ải, bí
  cảnh. Kiểu Tu tiên: thẻ thứ ba là Tông môn.
- Các màn khác: thanh trên hiện ngày dương và ngày âm dễ đọc. Tên truyện khi đọc được căn giữa.
- Chữ dài trên mọi màn thu nhỏ một chút trước khi phải cắt; Cẩm nang có chương Tông môn và bí cảnh, bảng 72 kỳ ngộ.
- versionCode 231.

## Bản 2.3 · Bái Nhập Sơn Môn

- **Tông môn** (màn mới): năm tông (Vạn Quyển Tông, Thiết Cốt Môn, Minh Tâm Tự, Kiếm Lư, Bách Thảo Cốc), mỗi tông một ưu
  đãi; bái nhập khi linh căn thức tỉnh, chuyển tông một lần. Thân phận Ngoại môn tới Thái thượng trưởng lão theo cống hiến
  và cảnh giới. Ba nhiệm vụ đọc mỗi tuần, bổng lộc tuần, Bảo khố đổi cống hiến, sư huynh/sư tỷ đối thủ tu theo nhịp đọc
  30 ngày của bạn nhân 1,1, tông môn đại bỉ mỗi quý.
- **Bí cảnh** (màn mới): bản đồ tuần, mỗi ngày đọc 30 phút mở một nút, rẽ trái hay phải, biến cố (yêu thú, cấm chế, dược
  viên hoang, động phủ cổ, tàn hồn, đồng đạo gặp nạn, điện chủ) với các cách giải mở bằng thuật pháp, pháp bảo, căn cơ;
  hai lần bị thương là trọng thương, tuần sau tu luyện 85%. Thượng cổ di tích: 20 ngày đọc trong tháng.
- **72 kỳ ngộ** theo độ hiếm: 36 thường gặp, 18 ít gặp, 9 hiếm gặp, 6 cực kỳ hiếm, 3 thiên cổ kỳ ngộ; càng hiếm thưởng
  càng quý; lời kể tiên hiệp; Niên kỷ › Kỳ ngộ xếp từ dễ tới khó gặp.
- **Cảnh giới gọi theo kỳ**: sơ kỳ, trung kỳ, hậu kỳ, đại viên mãn (tầng chỉ trên thanh tiến độ); linh căn thức tỉnh ở
  Luyện Khí trung kỳ. Từ Kim Đan, vào kỳ mới phải **phá tiểu cảnh** (nhẹ: hỏng chỉ tâm ma +3, mai thử lại).
- Hộp về Home khi đang đọc có **12 giọng tiên hiệp**; khung Home bỏ nhãn %; vẽ lại icon Công pháp, Động phủ, Pháp bảo,
  Đạo hiệu, Niên kỷ, thêm Tông môn. versionCode 230.

## Bản 2.2.1 · Kim Khung Ngọc Khuyết

Bản sửa giao diện Home sau 2.2.

- **Khung riêng** cho thẻ sách đang đọc và thẻ tu vi: một đai mực chạy quanh khung theo % đọc (thẻ sách) hay % đường
  qua cảnh giới (thẻ tu vi), chấm mốc mỗi 10%, nhãn ở giữa cạnh trên («Tứ tầng · 37%», «Kim Đan · 64%»), và hoa văn góc
  lớn dần theo cảnh giới: chấm, vòng, vòng có tâm, hình thoi, mây cuộn, nút thắt, sao tỏa tia; từ Kim Đan có dấu giữa cạnh.
- **Thẻ trong game đẹp dần theo cảnh giới**: chấm góc từ Trúc Cơ, viền trong mảnh từ Nguyên Anh, hình thoi góc từ Luyện Hư.
- **Hết chữ bị cắt trên Home**: lời chào bớt dần từng phần thay vì «…», tên đạo hiệu và tên sách trên kệ xuống hai dòng,
  khí linh và phẩm pháp bảo tách hai dòng, tên cảnh giới nhỏ lại khi chật. Test màn hình ghi lại mọi chữ bị cắt.
- **Nút Thư viện** trên thẻ sách thành ô vuông lớn (icon trên chữ), vùng chạm rộng hơn.
- **Bốn kiểu Home**: Bảng tu hành, Bìa lớn, Kệ sách, Tu tiên. Ai đang dùng Cổ điển chuyển sang Bảng tu hành, Tối giản sang
  Bìa lớn.
- versionCode 221.

## Bản 2.2 · Động Phủ Khai Lô

Bước ba của bản thiết kế: người tu có động phủ, trồng linh thảo và luyện đan bằng chính những buổi đọc liền. Màn mới
**Động phủ** (5 tab: Linh mạch, Dược viên, Đan phòng, Đan dược, Tĩnh thất), mở từ Tu vi › Cảnh giới và từ Home.

- **Linh mạch** Nhất tới Cửu giai, nâng bằng linh thạch: mỗi giai linh lực +2%, đột phá +1%, dược linh thêm 1 năm mỗi ngày;
  luống dược viên mở dần từ 2 tới 6.
- **Dược viên**: 8 loại linh thảo. Mỗi ngày đọc từ 30 phút, thảo thêm 10 năm dược linh; thu hoạch được thêm hạt.
- **Đan phòng**: 15 đan phương (tu luyện, phá cảnh, chữa thương, tâm cảnh, luyện thể, giải độc, Tẩy Linh Đan). **Hỏa hầu** là
  buổi đọc tới: phải liền đủ phút (15 – 60), thoát giữa chừng là nổ lò. Đan đạo cấp Nhất tới Cửu phẩm theo số lò thành,
  quyết định tỉ lệ, số viên; đan có bốn phẩm.
- **Đan độc**: mỗi viên đan để lại độc (đan phẩm cao ít độc hơn, đan Phường thị 4). Trên 60 thì đan giảm nửa công hiệu,
  căn cơ −10; tỉ lệ đột phá luôn trừ đan độc / 4. Tan 5 mỗi ngày đọc 30 phút, Giải Độc Đan tan 25.
- **Tĩnh thất**: bế quan bật ngay trong động phủ.
- **36 kỳ ngộ** thay cho lần gieo mỗi vạn chữ: mốc chữ ngẫu nhiên 10 – 50 vạn, buổi đọc liền 30 – 120 phút, ngày đọc 60 –
  180 phút, giờ Mão, Ngọ, Dậu, Tý, Sửu, mồng một, rằm, các lễ Tết âm lịch, tiết khí, mở sách mới, nửa cuốn, đọc xong (trong
  ba ngày, tác giả mới, cuốn 100 vạn), chuỗi ngày 7/30/100, đột phá, bí cảnh. Mỗi loại một tỉ lệ và một phẩm cơ duyên
  (thường, khá, quý, hiếm, tuyệt thế). Gặp kỳ ngộ thì hiện **khung nổi**, cả khi đang đọc (tắt được ở Cài đặt › Tu tiên).
- **Niên kỷ** có thêm tab **Thống kê** (chuyển từ Tu vi sang) và **Kỳ ngộ** (kỳ ngộ lục: 36 loại, đã gặp bao nhiêu lần).
- Mọi popup của app có khung đen; chữ giao diện đậm hơn một bậc (Bitter 700/800/900), Bitter là font mặc định cả ở popup.
- Sửa: tab Thông tin trong Cài đặt đếm trang sai (trang đầu có logo); Biên niên ghi đúng số phiên bản; Công pháp không còn
  trang thừa khi chỉ có một bộ. Test màn hình giờ lật qua mọi trang của mọi tab và báo nếu có trang trống. Bỏ mục «Mã nguồn
  mở» trong Cài đặt.
- **Menu khi đọc**: chạm giữa trang mở thẳng mục Khác; thanh dưới chỉ còn biểu tượng (Mục lục, Chữ, Trang, Khác) và
  thêm Home; về Home hỏi xác nhận, nhắc bế quan sẽ dừng (mất thưởng nếu chưa đủ) và lò đan đang đỏ sẽ nổ. Thanh trên chỉ
  còn tên sách; Home, Thư viện nằm trong Khác, và Khác vừa một trang.
- versionCode 220.

## Bản 2.1 · Vạn Pháp Quy Nguyên

Bước hai của bản thiết kế: người tu có linh căn bẩm sinh, có công pháp để luyện, tâm pháp để giữ lòng, thuật pháp để
ra tay. Màn mới **Công pháp** (5 tab: Linh căn, Công pháp, Tâm pháp, Thuật pháp, Tàng Kinh), mở từ Tu vi › Cảnh giới,
từ dock các kiểu Home.

- **Linh căn** thức tỉnh ở Luyện Khí tầng 3 theo khung giờ hay đọc: Kim 5 – 9 giờ, Mộc 9 – 13, Hỏa 13 – 18, Thổ 18 –
  23, Thủy 23 – 5; hành nào từ 15% giờ đọc là một linh căn. Một hành +30% linh lực, năm hành −20% nhưng không bị
  khắc. Dị linh căn Lôi, Băng, Phong cho thói quen hiếm (+10%). Tẩy linh một lần, 300 linh thạch.
- **Công pháp**: 36 bộ, sáu phẩm (Hoàng tới Tiên), mỗi bộ một thuộc tính và một đặc hiệu. Vận một bộ; chữ đọc luyện
  chín tầng, tầng 4, 7, 9 mở bằng **ngộ tính**. Hợp linh căn +20%, khắc −20%. Từ Kim Đan, cửa ải đòi phẩm công pháp
  (Huyền cho Kim Đan, Địa cho Nguyên Anh, Thiên cho Hóa Thần…), và phẩm, tầng công pháp cộng vào tỉ lệ đột phá.
- **Ngộ tính** thay tầng công pháp của từng cuốn: đọc tới tầng 4, 7 của cuốn, đọc trọn, cuốn dài đều cho ngộ tính.
- **Tâm pháp**: 7 bộ, một ô từ Trúc Cơ, hai ô từ Nguyên Anh; mỗi buổi đọc liền 30 phút là một lần vận chuyển,
  nhập môn ở 5 lần, đại thành ở 30.
- **Thuật pháp**: 15 thuật (công kích, phòng ngự, độn thuật, phụ trợ, thần thông) tự ra tay ở bí cảnh tuần, ngày lỡ
  của kiếp, đột phá hỏng, tâm ma tấn công; mỗi lần dùng thêm thuần thục, từ Sơ Học tới Hóa Cảnh.
- **Ngọc giản** từ kỳ ngộ (tàn hồn truyền thừa), bí cảnh tuần (35%) và **Tàng Kinh Các** (mỗi tuần một kho, đổi bằng
  linh thạch, luôn có công pháp đúng phẩm cửa kế tiếp cần).
- **Đồ họa vẽ lại**: sáu khoáng thạch (đá thô, tinh thể, cụm tinh thể, bảo thạch, thái cực cầu, tinh cầu hỗn độn),
  mười biểu tượng cảnh giới, và khung **Âm Dương phẩm** của pháp bảo (hào quang, vành nửa trắng nửa đen, đỉnh nhật
  nguyệt) để khác hẳn khung Hoàng phẩm. Vẽ bằng `python tools/ve_do_hoa.py`.
- Dữ liệu cũ chuyển sang: tầng sách cũ thành ngộ tính, giờ đọc cũ thức tỉnh linh căn ngay, người đã ở Kim Đan trở lên
  được một công pháp đủ phẩm cho cảnh giới đã mở, người đã Trúc Cơ được Thanh Tâm Quyết.
- versionCode 210.

## Bản 2.0 · Ngũ Khí Triều Nguyên

Bước đầu của bản thiết kế nâng cấp: người tu có thân, có tâm, và tự quyết lúc phá cảnh.

- **Năm thước đo** (Tu vi › tab Thân), tính từ nhật ký đọc: linh lực (tu vi), căn cơ (ngày đọc từ 30 phút, sách đọc
  trọn), tâm cảnh (chuỗi ngày, buổi đọc dài, đọc theo thứ tự), thần thức (tác giả và sách đọc trọn), thể phách (giờ
  đọc, buổi liền một tiếng). Thêm **tâm ma**: đứt chuỗi, nhảy chương, thất bại làm tăng; buổi đọc dài, đọc xong sách
  làm giảm.
- **Tiểu cảnh**: Luyện Khí 9 tầng; từ Trúc Cơ mỗi cảnh giới có sơ, trung, hậu kỳ, viên mãn. Vào trung, hậu kỳ, viên
  mãn cần căn cơ 30, 45, 60; thiếu thì kẹt, chữ đọc chỉ 50% thành tu vi.
- **Bình cảnh**: tới đỉnh cảnh giới, chữ đọc chỉ 20% thành tu vi, phần còn lại thành áp chế (tối đa +20% đột phá).
- **Đột phá tự bấm** (tab Đột phá): điều kiện có ô đánh dấu, tỉ lệ hiện từng phần, bấm hai lần mới gieo. Hỏng thì
  hụt khí, kinh mạch tổn thương hay tẩu hỏa nhập ma, không bao giờ rớt cảnh giới; lần sau thêm 10%.
- **Bốn cửa Nhân giới**: Trúc Cơ Quan (căn cơ, quyết định phẩm Trúc Cơ), Ngưng Đan (3 buổi đọc liền 45 phút, ra phẩm
  Kim Đan), Phá Đan Thành Anh (lôi kiếp kèm tâm ma kiếp: không nhảy chương, không đổi sách, không bỏ buổi dưới 10
  phút), Cảm Ngộ Thiên Địa (thần thức Lục trọng, ba cuốn từ 20 vạn chữ đọc trọn trong 30 ngày). Linh giới giữ thiên
  kiếp cũ trước khi đột phá.
- Phá Cảnh Đan còn cộng 15% cho lần đột phá; Thọ Nguyên Đan chữa kinh mạch; Định Thần Phù hạ tâm ma 10.
- Dữ liệu cũ chuyển sang nguyên vẹn: cảnh giới đã mở giữ nguyên, phần tu vi vượt đỉnh thành áp chế.

## Bản 1.0.1 · Khắc Lậu Cổ Pháp

- **Giờ kiểu xưa** khi đọc: góc trên phải là canh giờ và khắc (vd. "Giờ Thân 4 khắc"; mỗi canh giờ 2 tiếng, mỗi khắc
  15 phút). Thời gian còn lại ghi gọn "còn 8 canh giờ (còn nửa khắc)": cả quyển, rồi chương trong ngoặc. Phần đã đọc
  vẫn ghi bằng %.
- **Mục lục trọn trang**: mở ra chiếm cả màn hình, mỗi trang đúng 10 mục, không còn hàng chuyển chương. Chạm nút giữa
  ("Mục 311 – 320 · trang 32/40") để chọn nhanh khoảng mục: 1 – 10, 11 – 20… Xem [`docs/muc-luc.png`](docs/muc-luc.png).
- **Khác**: các tùy chọn thành thẻ, hai thẻ một hàng, chạm để đổi.
- Khi mở menu đọc, thanh chân trang (chương, giờ còn lại) ẩn dưới menu.

## Bản 1.0 · Vạn Pháp Quy Nhất (bản chính thức)

- **Hợp luyện** (Pháp bảo › Bảo khố, thanh trên cùng): ba pháp bảo dư cùng phẩm luyện thành một khoáng thạch phẩm
  trên, kèm 10 × phẩm linh thạch. Mỗi loại luôn giữ lại một món (món có khí linh, hoặc món lâu nhất), nên bộ sưu tập
  không mất loại nào. Hỗn Độn phẩm không hợp luyện. Đạo hiệu Lò Hỏa Thuần Thanh: có 30 pháp bảo hoặc hợp luyện 10 lần.
  Xem [`docs/hop-luyen.png`](docs/hop-luyen.png).
- **Biên niên** (Cài đặt › Thông tin › Biên niên phát triển): tab Tiền kiếp kể Exacta.OS trên Xteink X4 (từ CrossPoint
  2.0.0, Linh Thú, Beta 9, Beta 11 Trúc Cơ Vấn Đạo, Omega.v1 Vạn Pháp Quy Tông, Omega.v1.1); tab Thế hệ kể 11 thế hệ
  của app, mỗi thế hệ một danh hiệu riêng. Xem [`docs/bien-nien.png`](docs/bien-nien.png).

  | Bản | Tên | Nội dung chính |
  |---|---|---|
  | 0.1 | Nhất Quyển Sơ Khai | bản dựng thử: Readium mở EPUB |
  | 0.2 | Chuyển Thế Trùng Sinh | trò tu tiên của Exacta.OS sang Android |
  | 0.3 | Minh Kính Thông Thấu | hết văng app, giao diện 300 ppi, thiên kiếp, bí cảnh, đạo hiệu |
  | 0.4 | Lục Diện Thiên Môn | tab thay cuộn, 6 kiểu Home, bộ pháp bảo, Niên kỷ |
  | 0.5 | Mặc Bút Sinh Hoa | cài đặt đọc trong sách, độ đậm chữ, font riêng |
  | 0.6 | Chân Ngôn Bất Hư | CSS vào chương sách, đếm chữ trung thực, sách vào lò |
  | 0.7 | Lục Lô Tề Hỏa | lò 6 ô, ba chặng mỗi cuốn, vẽ lại khoáng thạch |
  | 0.8 | Vạn Bảo Xuất Thế | 72 pháp bảo, 18 bộ, kỳ ngộ, kiếp phá lô, tên mới |
  | 0.9 | Đan Hương Phù Ảnh | 18 đan dược và phù chú |
  | 0.10 | Bế Quan Tụ Linh | linh thạch, Phường thị, bế quan |
  | 1.0 | Vạn Pháp Quy Nhất | hợp luyện, tối ưu, Biên niên |
  | 1.0.1 | Khắc Lậu Cổ Pháp | giờ canh khắc, mục lục trọn trang, tùy chọn dạng thẻ |
  | 2.0 | Ngũ Khí Triều Nguyên | năm thước đo, tâm ma, tiểu cảnh, bình cảnh, đột phá tự bấm |
  | 2.1 | Vạn Pháp Quy Nguyên | linh căn, công pháp, tâm pháp, thuật pháp, ngộ tính, Tàng Kinh Các, vẽ lại đồ họa |
  | 2.2 | Động Phủ Khai Lô | động phủ, linh mạch, dược viên, đan phòng, đan độc, 36 kỳ ngộ, popup có khung |
  | 2.2.1 | Kim Khung Ngọc Khuyết | khung Home theo % và cảnh giới, hết cắt chữ, nút Thư viện vuông, 4 kiểu Home |
  | 2.3 | Bái Nhập Sơn Môn | tông môn, bí cảnh bản đồ, 72 kỳ ngộ, tiểu đột phá, cảnh giới theo kỳ |
  | 2.3.1 | Lục Giới Phân Đồ | Phi Thăng, Lục Giới Môn, sáu loại tiên nhân, Home gọn lại |
  | 2.4 | Ngũ Lộ Đồng Quy | Đạo lộ, mười hai Kim Đan, sáu Hóa Thần, Song Anh, Lục Giới cảm ứng, nghiệp lực |
  | 2.5 | Phá Toái Hư Không | Phi thăng Linh giới, pháp tắc, đạo vực, 36 tông môn, độ đậm theo % |
  | 2.6 | Cửu Cửu Quy Nhất | Độ Kiếp chín tầng, ngũ suy, thọ nguyên, Chân Tu, tĩnh thất sáu đẳng, 32 đan phương |
  | 2.7 | Tiên Môn Tiếp Dẫn | Lục Giới Môn, tiên hiệu theo giới, màn kết phần một, tiền đình |
  | 2.7.1 | Tế Luyện Bố Trận | Tế luyện 36 tầng cấm chế, bảy trận pháp |
  | 2.7.2 | Hộ Thân Họa Phù | Ô trang bị, phù thất vẽ chín loại phù |
  | 2.8 | Tàng Kinh Lưu Ảnh | Sổ đọc theo ngày cho phần hai, Niên kỷ › Lối đọc, tài nguyên dự tính |
  | 3.0 | Cửu Tiêu Khai Thiên | Phần hai: Cửu Tiêu, chín bậc tiên vị, tiên lực, Thiên Đình |
  | 3.0.1 | Sơn Môn Định Phận | Tông môn mở theo cảnh giới, chức vụ theo thời gian, bổng ngày, phản tông |
  | 3.1 | Vạn Pháp Trùng Tu | 72 công pháp, 12 mạch, ba đặc hiệu, lối vận công, xung quan, chuyển tu, phụ tu |
  | 3.2 | Tâm Đăng Bất Diệt | 18 tâm pháp, bốn phái, năm cảnh, vận chuyển theo lối đọc, xung khắc, ba ô |
  | 3.2.1 | Bát Quái Khai Trận | Khoáng thạch pixel 32 ô có màu, tĩnh thất tới 90 phút và tiềm tu 120, 18 trận pháp |
  | 3.3 | Thần Thông Quảng Đại | 36 thuật pháp, khống chế, thi triển, hồi chiêu theo ngày đọc, hợp kích, bản mệnh thần thông |
  | 3.4 | Tàng Kinh Cửu Lâu | Chín tầng lầu, kinh các tông môn, án tham ngộ, tàn quyển, phế công, đồ phổ, tự sáng công pháp |

- **Tối ưu**: các nét chữ và nét viền của giao diện được tạo một lần thay vì mỗi lần vẽ; mã hình vẽ được tra một lần
  rồi nhớ; bỏ hình lò cũ không còn dùng. Luật tu tiên (Cài đặt › Tu tiên) viết lại theo luật hiện tại.
- versionCode 100.

## Bản 0.10

- **Linh thạch**: đọc mỗi vạn từ +5 (Tết ×3, mồng một và rằm ×2), bế quan viên mãn (15/35/60/90), qua bí cảnh +20,
  vượt thiên kiếp +50 × cảnh giới, vượt kiếp phá lô, gom đủ bộ +50, và kỳ ngộ (nhặt được 20–80 viên).
- **Phường thị** (Pháp bảo › Phường thị): mua khoáng thạch 6 phẩm (30 → 1.500) và 18 loại đan phù bằng linh thạch.
  Xem [`docs/phuong-thi.png`](docs/phuong-thi.png).
- **Bế quan**: trong màn đọc, tab Khác › Bế quan, chọn 30/60/90/120 phút. Trong lúc bế quan lò luyện ×1,5, kỳ ngộ ×2,
  dòng trên trang hiện tiến độ; đọc đủ giờ là viên mãn, nhận linh thạch. Đạo hiệu Bế Quan Khổ Tu: bế quan viên mãn 20 lần.

## Bản 0.9

- **18 loại đan dược và phù chú** (9 đan, 9 phù), nhặt được qua kỳ ngộ theo độ hiếm; chạm một món ở Pháp bảo › Túi đồ
  để xem và dùng. Xem [`docs/tui-do.png`](docs/tui-do.png).
  - Đan: Tụ Linh Đan, Phá Cảnh Đan, Bồi Nguyên Đan (+10% lò bất kỳ), Ngưng Thần Đan (+1 vạn tu vi), Hộ Mạch Đan (thiên kiếp
    tha thêm một ngày), Tẩy Tủy Đan (khoáng thạch lên một phẩm), Cửu Chuyển Kim Đan (luyện xong ngay một lò dưới Thiên
    phẩm), Khai Linh Đan (đánh thức khí linh tự chọn), Thọ Nguyên Đan (lấy lại nửa tu vi mất vì độ kiếp).
  - Phù: Thiên Hỏa Phù, Hộ Lô Phù, Tầm Bảo Phù (kỳ ngộ ×3 trong 3 vạn từ), Bí Cảnh Phù (thêm một ngày bí cảnh), Định Thần
    Phù (hôm nay tính thêm 15 phút), Dẫn Lôi Phù (kiếp phá lô bớt một ngày), Phong Ấn Phù (vá một ngày lỡ của chuỗi),
    Tụ Bảo Phù (quà 10 vạn từ gieo ba lần), Luyện Hóa Phù (nung pháp bảo trùng loại thành khoáng thạch).
- Túi đồ trang 2: các đan phù đang có hiệu lực và nhật ký kỳ ngộ.

## Bản 0.8

- **Tên mới: Vạn Quyển Tiên Đồ** (đọc vạn quyển, tu thành tiên đồ); logo mới: cuốn sách mở, một viên tinh thạch
  lơ lửng bay lên.
- **72 loại pháp bảo** (12 mỗi phẩm), 36 loại mới vẽ cùng phong cách lệnh bài; cả 72 huy hiệu vẽ lại pixel art ở lưới
  128×128 (gấp đôi chi tiết), có viền sáng và bóng chấm. Xem [`docs/phap-bao-72.png`](docs/phap-bao-72.png).
  Pháp bảo cũ giữ nguyên loại.
- **18 bộ pháp bảo**: 9 bộ mới (Thiết Huyết Quân Trận, Cầm Tiêu Hợp Tấu, Linh Đăng Chiếu Lộ, Võ Lâm Ám Khí,
  Thất Tinh Bắc Đẩu, Phật Quang Phổ Chiếu, Thái Cực Lưỡng Nghi, Thất Bảo Tiên Quang, Tam Hoàng Thánh Khí).
- **Kỳ ngộ**: mỗi vạn từ một lần cơ duyên (khoảng 12%): khoáng thạch, đan dược, phù chú, hay khí linh của một pháp bảo
  thức tỉnh (danh hiệu riêng cho pháp bảo). Nhật ký kỳ ngộ ở Pháp bảo › Túi đồ.
- **Đan dược và phù**: Tụ Linh Đan (+15% cho lò Thiên phẩm trở lên), Thiên Hỏa Phù (lò ×2 trong 2 vạn từ),
  Phá Cảnh Đan (thêm một ngày khi độ kiếp từ Phá Đan Thành Anh trở lên), Hộ Lô Phù (tự cháy khi kiếp phá lô lỡ ngày).
- **Kiếp phá lô**: pháp bảo Thiên, Âm Dương, Hỗn Độn phẩm luyện xong còn phải qua 2, 3, 4 ngày đọc đủ 20, 30, 40 phút;
  vượt kiếp thì pháp bảo mang dấu «Kiếp Luyện», lỡ ngày thì lò nứt, lửa lui về bảy phần.
- **Cửa ải thiên kiếp có tên**: Trúc Cơ, Kết Đan, Phá Đan Thành Anh, Anh Biến Hóa Thần, Luyện Thần Phản Hư,
  Hợp Thể Quy Nguyên, Đại Thừa Viên Mãn, Độ Kiếp, Phi Thăng.
- **72 đạo hiệu** (12 mỗi nhóm): thêm các đạo hiệu cảnh giới, chuỗi dài, giờ đọc, sách dày, 72 loại pháp bảo,
  kiếp phá lô, khí linh, kỳ ngộ, đan phù.
- Ô lò trống hiện viên tinh thạch: «Linh tinh chờ luyện · Thiên tài địa bảo, đợi ngày khai lô».

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

- `game/`: cảnh giới (`Realm`), lịch âm và tiết khí (`Lunar`), pháp bảo (`Treasure`), danh mục công pháp, tâm pháp, thuật pháp (`PhapMon`), linh thảo và đan phương (`DanDao`), 36 kỳ ngộ (`KyNgo`), đạo hiệu (`DaoHieu`), Niên kỷ (`NienKy`), chữ của Home (`Texts`).
- `data/`: dữ liệu lưu trong một tệp JSON (`Store`, kèm thiên kiếp, bí cảnh, đạo hiệu), luật tu luyện 2.0 (`TuLuyen`: năm thước đo, tâm ma, tiểu cảnh, đột phá), linh căn và công pháp 2.1 (`DaoPhap`), động phủ 2.2 (`DongPhu`), quét sách, bìa, đếm chữ (`Library`), CSS chèn vào chương (`BookCss`).
- `ui/`: bộ vẽ kiểu Exacta.OS (`Ink`, kèm khung theo cảnh giới), Home 4 kiểu (`HomeView`), khung màn có tab (`TabScreen`), bảng chỉnh khi đọc (`ReaderSheet`).
- Các màn: `HomeActivity`, `LibraryActivity`, `ReaderActivity`, `TuViActivity`, `CongPhapActivity`, `DongPhuActivity`, `PhapBaoActivity`, `DaoHieuActivity`, `NienKyActivity`, `SettingsActivity`.

## Hướng tiếp

Đồng bộ với Exacta.OS; kỳ ngộ, bế quan, hợp luyện, linh thạch (chưa có trên app).

Đạo hiệu khác Exacta.OS ở bốn chỗ, vì app chưa có kỳ ngộ, bế quan, hợp luyện, linh thạch: Phúc Duyên Thâm
Hậu (20 khoáng thạch quà), Bế Quan Khổ Tu (tổng 100 giờ đọc), Lò Hỏa Thuần Thanh (30 pháp bảo), Nguyệt Hạ Độc
Thư (đọc đêm Trung Thu).

## Giấy phép bên thứ ba

- Readium Kotlin Toolkit: BSD-3-Clause.
- Font Bitter: SIL Open Font License 1.1 (`app/src/main/assets/fonts/OFL-Bitter.txt`).
