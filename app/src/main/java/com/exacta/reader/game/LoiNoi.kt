package com.exacta.reader.game

/**
 * 2.3: twelve voices of the cave for leaving the book (the Home button while reading). One is
 * picked each time; {m} {n} are the minutes of the bế quan done and asked, {lt} its linh thạch,
 * {lo} the pill on the fire.
 */
object LoiNoi {
    class Voice(val title: String, val open: String, val beQuan: String, val lo: String, val yes: String, val no: String)

    val VOICES = listOf(
        Voice("Đạo hữu muốn xuất quan?",
            "Trang sách này sẽ được giữ lại, lần sau mở ra vẫn đúng chỗ cũ.",
            "Bế quan mới được {m}/{n} phút. Xuất quan lúc này, công phu dở dang, {lt} linh thạch sẽ theo mây khói mà tan.",
            "Lò {lo} còn đang đỏ lửa. Rời đi giữa chừng, hỏa hầu đứt đoạn, lò ắt nổ, linh thảo hóa tro.",
            "Xuất quan", "Tu luyện tiếp"),
        Voice("Sư phụ dặn dò",
            "Đồ nhi, đọc sách như luyện kiếm, dừng tay đúng lúc cũng là một môn công phu.",
            "Con đang bế quan {m}/{n} phút. Bỏ dở lúc này thì {lt} linh thạch sư môn hứa thưởng, ta không giữ được cho con.",
            "Lò {lo} của con còn cháy. Con mà đi, lửa tắt, thuốc hỏng, đừng trách sư phụ không nhắc.",
            "Đệ tử xin cáo lui", "Đệ tử đọc tiếp"),
        Voice("Tiểu đồng gác cửa",
            "Chủ nhân định ra khỏi động phủ ạ? Tiểu đồng sẽ kẹp sẵn trang sách cho người.",
            "Nhưng chủ nhân đang bế quan {m}/{n} phút đó. Ra lúc này là mất {lt} linh thạch, tiểu đồng tiếc lắm.",
            "Lò {lo} vẫn đang reo lửa, chủ nhân đi rồi tiểu đồng không biết canh đâu, lò sẽ nổ mất.",
            "Ra ngoài một chút", "Thôi, ở lại"),
        Voice("Khí linh lên tiếng",
            "Chủ nhân, linh lực đang vận chuyển êm, dừng lại chẳng sao, chỉ cần nhớ quay về.",
            "Bế quan còn thiếu, mới {m}/{n} phút. Cắt ngang lúc này, {lt} linh thạch kia khí linh cũng không giữ hộ được.",
            "Lò {lo} đang đỏ. Khí linh không thể thay chủ nhân giữ lửa: rời đi là nổ lò.",
            "Thu công", "Vận công tiếp"),
        Voice("Thư linh níu áo",
            "Trang sách khẽ rung: đạo hữu đi đâu vội thế? Ta sẽ chờ ở đúng dòng này.",
            "Đạo hữu hứa bế quan {n} phút mà mới được {m}. Đi bây giờ, {lt} linh thạch kia coi như bỏ.",
            "Lò {lo} còn chờ đạo hữu đọc cho trọn hỏa hầu. Gấp ta lại lúc này, lò sẽ nổ.",
            "Gấp sách lại", "Đọc thêm vài trang"),
        Voice("Tàn hồn tiền bối",
            "Lão phu sống mấy ngàn năm, biết rằng nghỉ ngơi cũng là đạo. Ngươi cứ đi, sách vẫn ở đây.",
            "Có điều ngươi đang bế quan {m}/{n} phút. Đứt mạch lúc này, {lt} linh thạch tan thành hư vô, uổng lắm.",
            "Lò {lo} chưa tới hỏa hầu. Năm xưa lão phu cũng vì bỏ lò mà nổ mất nửa động phủ, ngươi nghĩ cho kỹ.",
            "Vãn bối xin đi", "Vãn bối ở lại"),
        Voice("Thiên đạo thì thầm",
            "Thiên đạo vô thường, người tu có lúc tiến, có lúc dừng. Muốn dừng ở đây chăng?",
            "Bế quan chưa viên mãn, mới {m}/{n} phút. Nghịch thiên mà xuất quan thì {lt} linh thạch cơ duyên tự tiêu tán.",
            "Lò {lo} đang luyện, hỏa hầu chưa đủ. Thuận thiên thì đọc tiếp, nghịch thiên thì nổ lò.",
            "Thuận lòng mà đi", "Thuận đạo mà đọc"),
        Voice("Sư tỷ nhắc khéo",
            "Sư đệ định về rồi à? Để sư tỷ đánh dấu trang cho, mai lại đọc tiếp nhé.",
            "Mà sư đệ đang bế quan {m}/{n} phút đấy. Ra giữa chừng thì {lt} linh thạch tháng này hết phần đó.",
            "Lò {lo} của sư đệ còn đang đỏ, đi bây giờ là sư tỷ phải dọn tro giùm đấy.",
            "Đệ về đây", "Đệ đọc thêm chút"),
        Voice("Đạo đồng giữ lò",
            "Bẩm, tiên sư muốn rời án thư? Đạo đồng xin ghi lại trang này.",
            "Bẩm, kỳ bế quan mới được {m}/{n} phút. Tiên sư xuất quan, {lt} linh thạch trong sổ sẽ bị gạch đi.",
            "Bẩm, lò {lo} đang ở hỏa hầu then chốt. Tiên sư đứng dậy là lửa loạn, lò nổ ngay.",
            "Ta đi đây", "Ta ngồi lại"),
        Voice("Lão giả bên khe suối",
            "Này người trẻ, suối chảy mãi chẳng vội, sách đọc mãi chẳng hết. Đi nghỉ cũng được.",
            "Nhưng ngươi đã hẹn với mình {n} phút bế quan, mới giữ được {m}. Lời hứa dở dang thì {lt} linh thạch cũng dở dang.",
            "Còn cái lò {lo} kia nữa, lửa đang đượm. Người đi, lửa tàn, thuốc thành tro.",
            "Tạm biệt lão trượng", "Ngồi nghe suối thêm"),
        Voice("Linh thú bên cạnh",
            "Linh thú dụi đầu vào tay áo, như muốn hỏi: chủ nhân đi đâu đấy?",
            "Chủ nhân đang bế quan {m}/{n} phút mà. Đi bây giờ, {lt} linh thạch sẽ bay mất, linh thú buồn lắm.",
            "Lò {lo} còn nóng hừng hực. Linh thú không biết canh lửa, chủ nhân đi là lò nổ.",
            "Đi một lát rồi về", "Ở lại với linh thú"),
        Voice("Tâm ma thì thầm",
            "Đi đi, thế gian còn bao điều vui hơn trang sách... Hay đạo tâm ngươi đủ vững để ở lại?",
            "Bế quan {m}/{n} phút thôi mà, bỏ đi, {lt} linh thạch có là gì? Tâm ma cười khẽ.",
            "Cái lò {lo} kia nổ thì đã sao? Tâm ma nói vậy, nhưng linh thảo thì chẳng mọc lại trong một ngày.",
            "Nghe theo, rời đi", "Trấn áp tâm ma, đọc tiếp"),
    )

    fun fill(s: String, m: Long, n: Int, lt: Long, lo: String) =
        s.replace("{m}", m.toString()).replace("{n}", n.toString()).replace("{lt}", lt.toString()).replace("{lo}", lo)
}
