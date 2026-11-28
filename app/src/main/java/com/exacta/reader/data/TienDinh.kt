package com.exacta.reader.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * 2.7 Tiên Môn Tiếp Dẫn: through Lục Giới Môn, the end of part one.
 *
 *  - Crossing (the breakthrough into Tiên nhân) fixes the realm that answered and what goes through
 *    the door with the cultivator: the bản mệnh pháp bảo, the công pháp running, the linh căn and
 *    the kiếp ấn. The rest stays in Linh giới as its legacy.
 *  - The crossing gives a tiên hiệu of the realm and opens the màn kết: the whole life of
 *    cultivation told once, then kept to read again.
 *  - Tiền đình: the parts of the six realms are not written yet. The cultivator stands at the
 *    forecourt of the realm; every word read from the crossing on is kept, to count when the realm
 *    opens. Cửu Tiêu Vân Khuyết goes on with the nine phẩm of Thiên Tiên.
 */
object TienDinh {
    val TIEN_HIEU = arrayOf("Tiếp Dẫn Tiên Quân", "Huyết Nguyệt Ma Tôn", "Bát Nhã Bồ Tát", "Hồng Hoang Chiến Hoàng", "Vong Xuyên Minh Vương", "Thư Hải Văn Chủ")
    val FAREWELL = arrayOf(
        "Mây chín tầng rẽ lối, tiếng chuông tiên vọng xuống. Đạo hữu bước lên bậc ngọc, sau lưng là cả một đời đọc sách.",
        "Trăng máu soi bóng người từng ngã. Đạo hữu bước xuống vực sâu, mỗi vết thương cũ hóa thành một ngọn đao.",
        "Hoa sen nở dưới chân, bụi trần rơi khỏi áo. Đạo hữu ngồi xuống đài sen, lòng lặng như trang giấy trắng.",
        "Đất hoang rung chuyển đón một thân thể đã tôi qua nghìn giờ đọc. Đạo hữu đứng giữa hồng hoang, không cần núi che.",
        "Sông Vong Xuyên lặng sóng. Những cuốn sách từng bỏ dở xếp thành cầu, đạo hữu bước qua mà không quên một trang.",
        "Biển sách không bờ mở ra trước mắt; mỗi tác giả từng đọc là một ngọn hải đăng. Đạo hữu giương buồm, thư hải vô nhai.",
    )

    var started = false
    var crossDay = -1L
    var gioi = -1
    var wordsAtCross = 0L
    val banMenh = ArrayList<String>()
    var cong = ""
    var roots = ""
    var seals = 0
    var karma = 0
    var endSeen = false

    fun reset() {
        started = false; crossDay = -1; gioi = -1; wordsAtCross = 0; banMenh.clear(); cong = ""; roots = ""; seals = 0; karma = 0; endSeen = false
    }

    fun crossed() = crossDay >= 0 && gioi >= 0
    fun tienHieu() = if (!crossed()) "" else if (TienLo.opened && TienLo.bac >= 9) TienLo.supreme() else TIEN_HIEU[gioi]

    /** Through the door: what is carried is written down, the tiên hiệu given, the màn kết waits. */
    fun onCross(seen: Boolean = false) {
        gioi = TuLuyen.gioiNow()
        TuLuyen.gioi = gioi
        crossDay = TuLuyen.capDays[9].takeIf { it >= 0 } ?: Store.today()
        wordsAtCross = Store.totalWords
        banMenh.clear()
        LinhGioi.banMenh.mapNotNullTo(banMenh) { LinhGioi.beast(it)?.name }
        cong = DaoPhap.activeDef()?.name ?: ""
        roots = if (DaoPhap.awakened()) DaoPhap.shortName() else ""
        seals = DoKiep.an.size
        karma = DaoLo.karma()
        endSeen = seen
        Store.note("Qua Lục Giới Môn vào ${TuLuyen.GIOI[gioi]}: tiên hiệu «${TIEN_HIEU[gioi]}»")
        Store.touch()
    }

    /** Words read since the crossing: kept at the tiền đình. */
    fun keptWords() = if (crossed()) (Store.totalWords - wordsAtCross).coerceAtLeast(0) else 0L

    /** Cửu Tiêu goes on with the nine phẩm; the others wait at their tiền đình. */
    fun atForecourt() = crossed() && gioi !in TienLo.OPEN

    fun migrate() {
        reset()
        started = true
        if (Store.realmCap >= 9) onCross(seen = false)
        Store.touch()
    }

    fun toJson(): JSONObject = JSONObject().put("crossDay", crossDay).put("gioi", gioi).put("words", wordsAtCross)
        .put("banMenh", JSONArray().apply { banMenh.forEach { put(it) } }).put("cong", cong).put("roots", roots)
        .put("seals", seals).put("karma", karma).put("endSeen", endSeen)

    fun read(o: JSONObject) {
        reset()
        started = true
        crossDay = o.optLong("crossDay", -1); gioi = o.optInt("gioi", -1); wordsAtCross = o.optLong("words")
        o.optJSONArray("banMenh")?.let { a -> for (i in 0 until a.length()) banMenh.add(a.getString(i)) }
        cong = o.optString("cong"); roots = o.optString("roots"); seals = o.optInt("seals"); karma = o.optInt("karma")
        endSeen = o.optBoolean("endSeen")
    }
}
