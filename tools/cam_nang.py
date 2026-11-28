"""
Cẩm nang tu tiên: the players' guide to Vạn Quyển Tiên Đồ, as a PDF the size of the iReader Neo 3's
screen (1072 x 1448 at 300 ppi = 91 x 123 mm), black and white for e-ink.

The numbers and names are read from the Kotlin sources, so the guide follows the game. The page is
written as HTML, printed by Edge (headless), then numbered and given an outline with PyMuPDF.

    python tools/cam_nang.py            -> dist/Van-Quyen-Tien-Do-<version>-Cam-nang.pdf
"""
import html
import os
import re
import subprocess
import sys
import unicodedata

import pymupdf

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, 'app', 'src', 'main', 'java', 'com', 'exacta', 'reader')
ART = os.path.join(ROOT, 'app', 'src', 'main', 'res', 'drawable-nodpi')
FONTS = os.path.join(ROOT, 'app', 'src', 'main', 'assets', 'fonts')
OUT_DIR = os.path.join(ROOT, 'build', 'cam-nang')
EDGE = r'C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe'


# --- Reading the game's numbers from the Kotlin sources ---------------------------------------

def read(*parts):
    return open(os.path.join(SRC, *parts), encoding='utf-8').read()


TREASURE = read('game', 'Treasure.kt')
REALM = read('game', 'Realm.kt')
DAO = read('game', 'DaoHieu.kt')
STORE = read('data', 'Store.kt')
TULUYEN = read('data', 'TuLuyen.kt')
GRADLE = open(os.path.join(ROOT, 'app', 'build.gradle.kts'), encoding='utf-8').read()
BIEN_NIEN = read('BienNienActivity.kt')


def block(src, name):
    """The text of `val NAME ... = xxxArrayOf(...)` up to its closing bracket."""
    m = re.search(r'val ' + name + r'\b[^=]*=\s*\w+\(', src)
    assert m, name
    depth, i = 1, m.end()
    while depth:
        c = src[i]
        if c == '(':
            depth += 1
        elif c == ')':
            depth -= 1
        elif c == '"':
            i = src.index('"', i + 1)
        i += 1
    return src[m.end():i - 1]


def strings(src, name):
    return re.findall(r'"((?:[^"\\]|\\.)*)"', block(src, name))


def numbers(src, name):
    return [int(x.replace('_', '')) for x in re.findall(r'\d[\d_]*', block(src, name))]


TIER_NAMES = strings(TREASURE, 'TIER_NAMES')
CLANS = strings(TREASURE, 'CLANS')
NAMES = strings(TREASURE, 'NAMES')
SPIRITS = strings(TREASURE, 'SPIRITS')
SET_NAMES = strings(TREASURE, 'SET_NAMES')
SET_EFFECTS = strings(TREASURE, 'SET_EFFECTS')
SET_DETAILS = strings(TREASURE, 'SET_DETAILS')
ITEM_NAMES = strings(TREASURE, 'ITEM_NAMES')
ITEM_TEXTS = strings(TREASURE, 'ITEM_TEXTS')
ITEM_WEIGHTS = numbers(TREASURE, 'ITEM_WEIGHTS')
GATES = strings(TREASURE, 'GATES')
FORGE_WORDS = numbers(TREASURE, 'FORGE_WORDS')
GIFT_ODDS = numbers(TREASURE, 'GIFT_ODDS')
STAGE_WORDS = numbers(STORE, 'STAGE_WORDS')
ORE_PRICES = numbers(STORE, 'ORE_PRICES')
ITEM_PRICES = numbers(STORE, 'ITEM_PRICES')
TRIAL_MINUTES = numbers(STORE, 'TRIAL_MINUTES')
TRIAL_DAYS = numbers(STORE, 'TRIAL_DAYS')
FLOOR_VAN = numbers(REALM, 'FLOOR_VAN')
TIEN_VAN = numbers(REALM, 'TIEN_VAN')
REALM_NAMES = strings(REALM, 'NAMES')
PHAM = strings(REALM, 'PHAM')
BASE = numbers(TULUYEN, 'BASE')
SUB_NEED = numbers(TULUYEN, 'SUB_NEED')
TAM_CANH_TIERS = strings(TULUYEN, 'TAM_CANH_TIERS')
TAM_CANH_AT = numbers(TULUYEN, 'TAM_CANH_AT')
TRONG = strings(TULUYEN, 'TRONG')
THAN_THUC_AT = numbers(TULUYEN, 'THAN_THUC_AT')
THE_TIERS = strings(TULUYEN, 'THE_TIERS')
THE_AT = numbers(TULUYEN, 'THE_AT')
TRUC_CO_PHAM = strings(TULUYEN, 'TRUC_CO_PHAM')
TRUC_CO_GOC = numbers(TULUYEN, 'TRUC_CO_GOC')
DAO_GROUPS = strings(DAO, 'GROUP_NAMES')
DAO_NAMES = strings(DAO, 'NAMES')
DAO_RULES = strings(DAO, 'RULES')
VERSION = re.search(r'versionName\s*=\s*"([^"]+)"', GRADLE).group(1)
GENERATION = re.findall(r'Era\("[^"]+",\s*"([^"]+)"', BIEN_NIEN.split('GENERATIONS')[-1])[-1]


def set_members():
    """Treasure.SET_MEMBERS: the first nine sets were written in the old six-kind numbering."""
    groups = [[int(x) for x in g.split(',')] for g in re.findall(r'intArrayOf\(([\d,\s]+)\)', TREASURE[TREASURE.index('val SET_MEMBERS'):TREASURE.index('val SET_EFFECTS')])]
    old = lambda s: (s // 6) * 12 + s % 6
    return [[old(s) for s in g] for g in groups[:9]] + groups[9:]


SETS = set_members()

assert len(NAMES) == 72 and len(SETS) == 18 and len(ITEM_NAMES) == 18 and len(DAO_NAMES) == 72
assert len(DAO_RULES) == 72 and len(ITEM_PRICES) == 18 and len(ITEM_WEIGHTS) == 18
assert sorted(s for g in SETS for s in g) == list(range(72)), 'every kind in exactly one set'
assert len(TRIAL_MINUTES) == 9 and len(STAGE_WORDS) == 6
assert len(BASE) == 10 and len(SUB_NEED) == 4 and len(TAM_CANH_AT) == 5 and len(THAN_THUC_AT) == 9 and len(THE_AT) == len(THE_TIERS)

WEIGHT_SUM = sum(ITEM_WEIGHTS)


def set_of(species):
    return next(i for i, g in enumerate(SETS) if species in g)


# --- Small formatting helpers --------------------------------------------------------------------

def e(s):
    return html.escape(str(s), quote=True)


def num(n):
    """12.345 (Vietnamese thousands)."""
    return f'{n:,}'.replace(',', '.')


def van(words):
    """Words in vạn: 2,7 or 13,5 or 135."""
    v = words / 10000
    s = f'{v:.2f}'.rstrip('0').rstrip('.')
    return s.replace('.', ',')


def img(name, cls=''):
    path = os.path.join(ART, name + '.png').replace('\\', '/')
    return f'<img class="{cls}" src="file:///{path}">'


def rarity(w):
    if w >= 12:
        return 'Thường gặp'
    if w >= 8:
        return 'Khá thường'
    if w >= 6:
        return 'Ít gặp'
    if w >= 4:
        return 'Hiếm'
    return 'Rất hiếm'


def table(head, rows, cls=''):
    h = ''.join(f'<th>{c}</th>' for c in head)
    b = ''.join('<tr>' + ''.join(f'<td>{c}</td>' for c in r) + '</tr>' for r in rows)
    return f'<table class="{cls}"><thead><tr>{h}</tr></thead><tbody>{b}</tbody></table>'


# --- The chapters ------------------------------------------------------------------------------------

TOC = []  # (level, title shown in the contents, the heading text to find on the page)


def chapter(no, title, lead):
    key = f'Quyển {no} · {title}' if no else title
    TOC.append((1, key, key))
    label = f'<div class="qno">QUYỂN {no}</div>' if no else '<div class="qno">PHỤ LỤC</div>'
    return f'<section class="chapter">{label}<h1>{e(key)}</h1><p class="lead">{lead}</p>'


def h2(title, toc=False):
    if toc:
        TOC.append((2, title, title))
    return f'<h2>{e(title)}</h2>'


def tip(text, label='Mẹo'):
    return f'<div class="tip"><b>{label}.</b> {text}</div>'


def cover():
    return f'''<section class="cover">
<div class="cv-top">VẠN QUYỂN TIÊN ĐỒ</div>
{img('logo', 'cv-logo')}
<div class="cv-title">Cẩm Nang<br>Tu Tiên</div>
<div class="cv-rule"></div>
<div class="cv-sub">Hướng dẫn cày game đọc sách · chiến lược luyện bảo, phá cảnh, tiêu linh thạch · thư viện pháp bảo, đan dược, phù chú, đạo hiệu</div>
<div class="cv-ver">Bản {e(VERSION)} · {e(GENERATION)}</div>
</section>'''


def contents(numbers=None):
    rows = []
    for i, (lvl, title, _) in enumerate(TOC):
        n = numbers[i] if numbers else '··'
        rows.append(f'<div class="toc{lvl}"><span>{e(title)}</span><i></i><b>{n}</b></div>')
    return f'<section class="contents"><h1 class="toch">Mục lục</h1>{"".join(rows)}</section>'


def intro():
    return '''<section class="page">
<h2>Đọc cẩm nang này thế nào</h2>
<p>Mỗi chữ bạn đọc là một chút tu vi, một ngọn lửa trong lò, một cơ hội kỳ ngộ. Cẩm nang chia ba phần:</p>
<ul>
<li><b>Quyển I – IX: luật chơi</b>, mọi con số lấy thẳng từ mã nguồn bản này.</li>
<li><b>Quyển X: chiến lược cày</b>: lộ trình, xếp lò, tiêu linh thạch.</li>
<li><b>Quyển XI – XV: thư viện</b> khoáng thạch, pháp bảo, bộ, đan phù, đạo hiệu; cuối sách là bảng tra nhanh.</li>
</ul>
<h2>Vài chữ dùng trong sách</h2>
<table class="defs"><tbody>
<tr><td>vạn</td><td>10.000 chữ. «2,7 vạn» là 27.000 chữ.</td></tr>
<tr><td>tu vi</td><td>số chữ đã đọc được tính, cộng thưởng; quyết định cảnh giới.</td></tr>
<tr><td>phẩm</td><td>bậc của khoáng thạch và pháp bảo: Hoàng, Huyền, Địa, Thiên, Âm Dương, Hỗn Độn.</td></tr>
<tr><td>ô lò</td><td>một trong 6 chỗ của lò luyện.</td></tr>
<tr><td>LT</td><td>linh thạch, tiền của giới tu tiên; tiêu ở Phường thị.</td></tr>
<tr><td>ngộ tính</td><td>điểm lĩnh ngộ từ sách: mở tầng 4, 7, 9 của công pháp và học thuật pháp.</td></tr>
<tr><td>ngày lễ</td><td>mồng một, ngày rằm âm lịch và ba ngày Tết.</td></tr>
<tr><td>bộ</td><td>bốn loại pháp bảo; gom đủ thì có hiệu quả mãi mãi.</td></tr>
</tbody></table>
</section>'''


def part1():
    return chapter('I', 'Nhập môn: đọc là tu', 'Mọi thứ trong game đều sinh ra từ hai đại lượng: <b>số chữ</b> bạn đọc tiến lên và <b>số phút</b> bạn đọc mỗi ngày.') + f'''
{h2('Vòng tu luyện')}
<div class="flow">
<div class="fl-src">Đọc sách<br><small>chữ · phút</small></div>
<div class="fl-grid">
<div><b>Tu vi</b><br>cảnh giới, đạo hiệu</div>
<div><b>Lò luyện</b><br>khoáng thạch → pháp bảo</div>
<div><b>Kỳ ngộ</b><br>36 loại, theo lúc đọc</div>
<div><b>Linh thạch</b><br>5 LT mỗi vạn chữ</div>
<div><b>Quà 10 vạn</b><br>một khoáng thạch</div>
<div><b>Phút mỗi ngày</b><br>thiên kiếp, bí cảnh, phá lô</div>
</div></div>
<p>Pháp bảo gom thành bộ; bộ cho hiệu quả mãi mãi. Linh thạch đổi lấy khoáng thạch và đan phù ở Phường thị; đan phù
lại đẩy lò, giữ chuỗi, đỡ thiên kiếp. Đọc càng đều, vòng này quay càng nhanh.</p>

{h2('Chữ nào được tính')}
<ul>
<li><b>Lật trang tới</b> (chạm hoặc vuốt) được tính số chữ của trang đó, tối đa <b>450 chữ một trang</b>.</li>
<li><b>Nhảy</b> bằng mục lục, nút chương hay thanh kéo chỉ dời dấu đọc, <b>không tính chữ</b>.</li>
<li>Đọc lại đoạn đã qua không tính lần hai: chỉ phần vượt qua chỗ xa nhất từng tới mới được cộng.</li>
<li>Chữ của mọi cuốn đều vào tu vi, kỳ ngộ, linh thạch và các ô khoáng thạch.</li>
<li>Một cuốn được coi là <b>đọc xong</b> khi tới 95%.</li>
</ul>
{tip('Muốn tới một chương xa, cứ nhảy bằng mục lục rồi đọc tiếp: phần nhảy qua không tính, nhưng từ chỗ mới mọi trang lật tới vẫn được tính bình thường.')}

{h2('Phút đọc')}
<p>Thời gian trang sách mở trên màn hình là phút đọc của ngày đó. Một ngày có <b>từ 1 phút</b> thì giữ chuỗi ngày đọc;
<b>30 phút</b> là mốc của bí cảnh tuần và cũng là mức thấp nhất của thiên kiếp. Phút còn được dùng cho bế quan, kiếp phá lô
và nhiều đạo hiệu (đọc đêm, đọc sáng sớm, đọc liền 2 giờ…).</p>

{h2('Tầng sách và ngộ tính')}
<p>Tiến độ mỗi cuốn vẫn chia từ Nhất tầng tới Cửu tầng, đọc xong là <b>Đại viên mãn</b>. Từ bản 2.1, tầng sách không còn là công
pháp mà là nguồn <b>ngộ tính</b>: đọc tới tầng 4 và tầng 7 của một cuốn (lật trang, không nhảy) mỗi lần +1, đọc trọn +1, cuốn từ
20 vạn chữ thêm +1. Mỗi lần lên tầng sách vẫn được đếm cho đạo hiệu «Bách Tầng Công Pháp» và «Tầng Tầng Đột Phá».</p>

{h2('Một ngày của người tu')}
<ol>
<li>Mở Home: đọc dòng thông báo (pháp bảo xuất thế, kỳ ngộ, thiên kiếp).</li>
<li>Nếu đang độ kiếp hay có lò qua kiếp phá lô: đọc cho đủ phút của hôm nay trước đã.</li>
<li>Nhìn lò: ô nào trống thì đặt sách đang đọc hoặc khoáng thạch vào.</li>
<li>Đọc từ 30 phút để tính một ngày bí cảnh.</li>
<li>Cuối ngày ghé Túi đồ, Phường thị: hợp luyện món dư, dùng đan phù đang có.</li>
</ol>
</section>'''


def part2():
    asks = ['—', f'căn cơ từ {SUB_NEED[1]}', 'ngưng đan: 3 ngày, mỗi ngày một buổi liền 45′',
            f'lôi kiếp {TRIAL_DAYS[2]} ngày × {TRIAL_MINUTES[2]}′ kèm tâm ma kiếp', 'thần thức Lục trọng; 3 lần ngộ đạo'] + \
        [f'thiên kiếp {TRIAL_DAYS[r - 1]} ngày × {TRIAL_MINUTES[r - 1]}′' for r in range(5, 10)]
    rows = []
    for r in range(10):
        floor = f'{num(FLOOR_VAN[r])} vạn' if r < 9 else f'{num(TIEN_VAN[0])} vạn'
        rows.append([img(f'realm_{r}', 'ri'), f'<b>{e(REALM_NAMES[r])}</b>', floor, '—' if r == 0 else e(GATES[r]), asks[r],
                     f'{BASE[r]}%' if r else '—'])
    realm_table = table(['', 'Cảnh giới', 'Từ', 'Cửa ải', 'Phải vượt', 'Nền'], rows, 'realms')
    pham = ' · '.join(f'{e(PHAM[i])} {num(TIEN_VAN[i])}' for i in range(9))
    tam_canh = ' · '.join(f'{e(TAM_CANH_TIERS[i])} {TAM_CANH_AT[i]}' for i in range(5))
    the = ' · '.join(f'{e(THE_TIERS[i])} {THE_AT[i]}' for i in range(len(THE_TIERS)))
    measures = table(['Thước đo', 'Tính từ nhật ký đọc'], [
        ['<b>Linh lực</b>', 'tu vi: chữ đọc lật trang, theo các luật dưới đây'],
        ['<b>Căn cơ</b> 0 – 100', '2 × ngày đọc từ 30′ (30 ngày) + 10 × cuốn đọc trọn (90 ngày, tối đa 4) + gốc Trúc Cơ; '
                                  'trừ 1 cho mỗi lần nhảy chương quá 5 lần trong 30 ngày'],
        ['<b>Tâm cảnh</b> 0 – 100', f'chuỗi ngày (tối đa 30) + 3 × buổi từ 45′ (30 ngày, tối đa 10) + (30 − 2 × lần nhảy chương) + 10 nếu '
                                    f'đọc xong một cuốn trong 30 ngày; trừ 10 mỗi lần hỏng, 3 mỗi cuốn bỏ dở. Bậc: {tam_canh}'],
        ['<b>Thần thức</b>', f'5 × số tác giả đã đọc trọn một cuốn + số cuốn đọc trọn; Nhất trọng tới Cửu trọng '
                             f'({" · ".join(str(x) for x in THAN_THUC_AT)})'],
        ['<b>Thể phách</b>', f'giờ đọc + 3 × buổi liền từ 60′. Bậc: {the}'],
    ], 'measures')
    rescue = table(['Trợ lực', 'Công dụng'], [
        ['Phá Cảnh Đan', 'ở bình cảnh: lần đột phá kế tiếp +15%; khi đang độ lôi kiếp, thiên kiếp: thêm một ngày'],
        ['Hộ Mạch Đan', 'kỳ ngưng đan, lôi kiếp, thiên kiếp đang độ được lỡ thêm một ngày'],
        ['Định Thần Phù', 'hôm nay tính thêm 15 phút (cả buổi ngưng đan); tâm ma −10'],
        ['Thọ Nguyên Đan', 'lấy lại một nửa tu vi đã mất; không mất gì thì chữa lành kinh mạch'],
        ['Bộ Đạo Môn Tứ Bảo', 'mỗi kỳ kiếp được lỡ một ngày'],
        ['Bộ Thất Tinh Bắc Đẩu', 'mỗi ngày độ kiếp cần ít hơn 5 phút (trừ ngưng đan)'],
    ])
    fails = table(['Khi hỏng', 'Lúc nào', 'Hậu quả'], [
        ['<b>Hụt khí</b>', 'tỉ lệ từ 50%', 'mất 2% linh lực'],
        ['<b>Kinh mạch tổn thương</b>', 'tỉ lệ dưới 50%', '5 ngày tu luyện chỉ còn 70%'],
        ['<b>Tẩu hỏa nhập ma</b>', 'tâm ma từ 70', 'tâm cảnh −20, căn cơ −10 trong 30 ngày; chỉ phá lại khi tâm ma dưới 50'],
    ])
    return chapter('II', 'Cảnh giới và đột phá', 'Từ bản 2.0, người tu không chỉ có tu vi: năm thước đo, tâm ma, bình cảnh, và mỗi lần lên đại cảnh giới là một lần đột phá do bạn tự bấm.') + f'''
{h2('Mười cảnh giới')}
{realm_table}
<p class="small">Nhân giới là Luyện Khí tới Hóa Thần, Linh giới là Luyện Hư tới Phi Thăng; Độ Kiếp là chặng đặc biệt giữa Đại Thừa và Phi Thăng.
Từ Phi Thăng lên Tiên nhân phải qua <b>Lục Giới Môn</b>. Giới phi thăng không do bạn chọn: sáu giới
(Cửu Tiêu Vân Khuyết, Huyết Nguyệt Trầm Uyên, Bát Nhã Liên Đài, Hồng Hoang Cổ Địa, Vong Xuyên Bỉ Ngạn, Thư Hải Vô Nhai) cảm ứng với
cả đời tu, giới đáp lại mạnh nhất tiếp dẫn bạn thành một trong sáu loại tiên nhân (xem Quyển VI). Tiên nhân có chín phẩm
(vạn chữ): {pham}. Từ Tiên nhân không còn cửa ải.</p>

{h2('Năm thước đo', True)}
{measures}
<p><b>Tâm ma</b> (0 – 100) tăng khi đứt chuỗi (+10), nhảy chương quá 3 lần một ngày (+2 mỗi lần), đột phá hỏng (+10), kiếp hỏng
(+15); giảm khi có buổi đọc từ 45 phút (−3), ngày đọc đủ 30 phút (−1), đọc xong sách (−10), Định Thần Phù (−10). Tâm ma trừ thẳng
một nửa vào tỉ lệ đột phá.</p>

{h2('Tiểu cảnh, nút thắt, bình cảnh', True)}
<ul>
<li>Mỗi cảnh giới gọi theo kỳ: <b>sơ kỳ, trung kỳ, hậu kỳ, đại viên mãn</b>. Luyện Khí có chín tầng trên thanh tiến độ, ba tầng một kỳ.
Từ Kim Đan, vào kỳ mới còn phải <b>phá tiểu cảnh</b> (Tu vi › Đột phá): tỉ lệ 60% + căn cơ / 3 + bậc tâm cảnh × 5 − tâm ma / 3; hỏng chỉ
tâm ma +3, mai thử lại, thêm 10%.</li>
<li><b>Nút thắt</b>: vào trung kỳ, hậu kỳ, đại viên mãn cần căn cơ {SUB_NEED[1]}, {SUB_NEED[2]}, {SUB_NEED[3]}. Thiếu thì kẹt lại, chữ đọc
chỉ 50% thành linh lực cho tới khi đọc đều trở lại. Tiểu cảnh đã mở thì không mất.</li>
<li><b>Bình cảnh</b>: tới đỉnh cảnh giới, đan điền đầy. Chữ đọc thêm chỉ 20% thành linh lực, 80% thành <b>áp chế</b>. Mỗi 10% áp chế
(so với cả cảnh giới) cộng 5% tỉ lệ đột phá, tối đa 20%. Phá ngay hay đè cảnh giới cho chắc là lựa chọn của bạn.</li>
</ul>

{h2('Đột phá diễn ra thế nào', True)}
<ol>
<li>Tới bình cảnh và đủ điều kiện của cửa ải: xem tab <b>Tu vi › Đột phá</b>, mỗi điều kiện có ô đánh dấu.</li>
<li>Cửa có kiếp (ngưng đan, lôi kiếp, thiên kiếp): bấm <b>Bắt đầu</b>. Phải có đủ số ngày đạt số phút; ngày đầu và hôm nay chỉ
được tính khi đã đủ. Một ngày ở giữa thiếu là kiếp hỏng: tâm ma +15, hồi khí 2 ngày, <b>không mất tu vi</b>. Kiếp đã vượt
thì giữ, đột phá hỏng không phải độ lại.</li>
<li>Bấm <b>Đột phá</b> hai lần. Tỉ lệ = nền + căn cơ / 2 + bậc tâm cảnh × 5 + áp chế + công pháp + Phá Cảnh Đan 15 + 10 mỗi
lần hỏng trước (tối đa 30) − tâm ma / 2, trong khoảng 5 – 95%, hiện từng phần trước khi bấm.</li>
<li>Thành: cảnh giới mở, nhận <b>50 LT × số thứ tự</b> (Trúc Cơ 50 … Tiên nhân 450). Hỏng: tâm ma +10, hồi khí 2 ngày và một
trong ba mức dưới đây. Không mức nào làm rớt cảnh giới.</li>
</ol>
{fails}

{h2('Bốn cửa ải Nhân giới', True)}
<ul>
<li><b>Trúc Cơ Quan</b>: căn cơ từ {SUB_NEED[1]}. Phẩm Trúc Cơ theo căn cơ cộng hai lần điểm áp chế lúc phá: dưới 45 hạ phẩm, dưới 65
trung, dưới 85 thượng, từ 85 hoàn mỹ; căn cơ gốc {" / ".join(f"+{x}" for x in TRUC_CO_GOC)} theo suốt đời tu.</li>
<li><b>Ngưng Đan</b>: 3 ngày, mỗi ngày <b>một buổi liền</b> từ 45 phút (thoát ra là buổi mới). Phẩm Kim Đan theo độ dư của lần gieo,
từ cửu phẩm tới nhất phẩm; tu tới Nguyên Anh nhanh thêm 2% mỗi phẩm. Vô Hà Kim Đan (+25%) cần dư từ 70 và căn cơ từ 90.</li>
<li><b>Phá Đan Thành Anh</b>: lôi kiếp {TRIAL_DAYS[2]} ngày × {TRIAL_MINUTES[2]} phút, cùng lúc là <b>tâm ma kiếp</b>: mỗi lần nhảy chương,
đổi sang cuốn khác, bỏ buổi đọc dưới 10 phút là một lần tâm ma tấn công; ba lần là kiếp hỏng. Chọn một cuốn và đọc tới nơi.</li>
<li><b>Cảm Ngộ Thiên Địa</b>: thần thức Lục trọng và ba lần <b>ngộ đạo</b>: ba cuốn từ 20 vạn chữ, mỗi cuốn đọc trọn trong 30 ngày kể
từ ngày mở, đọc xong sau khi vào Nguyên Anh.</li>
</ul>
<p class="small">Từ Kim Đan, cửa ải còn đòi công pháp đủ phẩm (Quyển III). Linh giới tạm giữ thiên kiếp của bản 1.0; pháp tắc, phi thăng kiếp,
cửu cửu thiên kiếp sẽ tới ở các bản sau.</p>

{h2('Đồ trợ kiếp')}
{rescue}
</section>'''


PHAPMON = read('game', 'PhapMon.kt')
ATTR = strings(PHAPMON, 'ATTR')
ATTR_CONST = ['KIM', 'MOC', 'THUY', 'HOA', 'THO', 'LOI', 'BANG', 'PHONG', 'KIEM', 'THE', 'THAN']
CP_PHAM = strings(PHAPMON, 'PHAM')
CP_REACH = numbers(PHAPMON, 'REACH')
CP_BONUS = numbers(PHAPMON, 'PHAM_BONUS')
CP_WORDS = numbers(PHAPMON, 'TANG_WORDS')
CP_PRICE = numbers(PHAPMON, 'PRICE')
EFFECTS = strings(PHAPMON, 'EFFECTS')
EFFECT_CONST = re.findall(r'const val (E_\w+) = (\d+)', PHAPMON)
EFFECT_OF = {name: int(i) for name, i in EFFECT_CONST}
KIND_NAMES = strings(PHAPMON, 'KINDS')
KIND_CONST = {'K_CONG_KICH': 0, 'K_PHONG_NGU': 1, 'K_DON': 2, 'K_PHU_TRO': 3, 'K_THAN_THONG': 4}
ROOT_BONUS = [int(x) for x in re.findall(r'-?\d+', block(PHAPMON, 'ROOT_BONUS'))]
DI_BONUS = int(re.search(r'DI_BONUS = (\d+)', PHAPMON).group(1))
MASTERY = strings(PHAPMON, 'MASTERY')
MASTERY_AT = numbers(PHAPMON, 'MASTERY_AT')
TAM_PRICE = int(re.search(r'TAM_PRICE = (\d+)', PHAPMON).group(1))
TAM_NHAP = int(re.search(r'TAM_NHAP_MON = (\d+)', PHAPMON).group(1))
TAM_DAI = int(re.search(r'TAM_DAI_THANH = (\d+)', PHAPMON).group(1))
DAO_PHAP = read('data', 'DaoPhap.kt')
WASH_PRICE = int(re.search(r'WASH_PRICE = (\d+)', DAO_PHAP).group(1))
WASH_HOURS = int(re.search(r'WASH_HOURS = (\d+)', DAO_PHAP).group(1))
ROOT_SHARE = int(re.search(r'ROOT_SHARE = (\d+)', DAO_PHAP).group(1))
BI_CANH_SCROLL = int(re.search(r'BI_CANH_SCROLL = (\d+)', STORE).group(1))


def attr_name(c):
    return ATTR[ATTR_CONST.index(c)] if c in ATTR_CONST else 'vô thuộc tính'


CONG_PHAP = [(n, int(p), attr_name(a), EFFECTS[EFFECT_OF[f]]) for n, p, a, f in
             re.findall(r'CongPhap\("([^"]+)", (\d), (\w+), (\w+)\)', PHAPMON)]
TAM_PHAP = re.findall(r'TamPhap\("([^"]+)", "([^"]+)"\)', PHAPMON)
THUAT_PHAP = [(n, attr_name(a), KIND_NAMES[KIND_CONST[k]], t) for n, a, k, t in
              re.findall(r'ThuatPhap\("([^"]+)", (\w+), (\w+), "([^"]+)"\)', PHAPMON)]
assert len(CONG_PHAP) >= 30 and len(TAM_PHAP) == 7 and len(THUAT_PHAP) == 15 and len(ROOT_BONUS) == 6


def part2b():
    hours = table(['Hành', 'Khung giờ đọc', 'Tính chất'], [
        ['<b>Kim</b>', '5 – 9 giờ', 'kiếm khí, phá cấm chế'],
        ['<b>Mộc</b>', '9 – 13 giờ', 'sinh trưởng, dược viên'],
        ['<b>Hỏa</b>', '13 – 18 giờ', 'luyện đan, luyện khí'],
        ['<b>Thổ</b>', '18 – 23 giờ', 'phòng ngự, căn cơ'],
        ['<b>Thủy</b>', '23 – 5 giờ', 'thần thức, tâm cảnh'],
    ])
    roots = table(['Linh căn', 'Linh lực', 'Ghi chú'], [
        ['Thiên linh căn (1 hành)', f'+{ROOT_BONUS[1]}%', 'tu nhanh nhất; thuật pháp khác hành tốn 2, hành bị khắc tốn 3 ngộ tính'],
        ['Song linh căn (2)', f'+{ROOT_BONUS[2]}%', ''],
        ['Tam linh căn (3)', f'+{ROOT_BONUS[3]}%', ''],
        ['Tứ linh căn (4)', f'{ROOT_BONUS[4]}%'.replace('-', '−'), ''],
        ['Ngũ hành tạp linh căn (5)', f'{ROOT_BONUS[5]}%'.replace('-', '−'), 'không bị khắc; học mọi thuật pháp ngũ hành không tốn ngộ tính'],
        ['Dị linh căn', f'thêm +{DI_BONUS}%', 'Lôi: 10% giờ đọc từ 0 tới 3 giờ sáng và 2 ngày rằm có đọc · Băng: 20 ngày đọc từ 30 phút trong một mùa '
         'Đông chí (22/12) tới Đại hàn (20/1) · Phong: 3 cuốn đọc trọn trong một tuần'],
    ])
    pham = table(['Phẩm', 'Tu tới', 'Linh lực', 'Chữ mỗi tầng', 'Giá Tàng Kinh'], [
        [f'<b>{e(CP_PHAM[i])}</b>', e(REALM_NAMES[CP_REACH[i]] if CP_REACH[i] < 9 else 'Tiên cảnh'), f'+{CP_BONUS[i]}% + tầng',
         f'{van(CP_WORDS[i])} vạn', f'{CP_PRICE[i]} LT'] for i in range(6)])
    lib = table(['Công pháp', 'Phẩm', 'Thuộc tính', 'Đặc hiệu'],
                [[f'<b>{e(n)}</b>', e(CP_PHAM[p]), e(a), e(f)] for n, p, a, f in CONG_PHAP], 'lib')
    tam = table(['Tâm pháp', 'Hiệu quả khi nhập môn'], [[f'<b>{e(n)}</b>', e(t)] for n, t in TAM_PHAP], 'lib')
    thuat = table(['Thuật pháp', 'Hành', 'Loại', 'Ra tay khi'], [[f'<b>{e(n)}</b>', e(a), e(k), e(t)] for n, a, k, t in THUAT_PHAP], 'lib')
    return chapter('III', 'Linh căn và công pháp', 'Từ bản 2.1, người tu có linh căn bẩm sinh từ giờ đọc, vận một bộ công pháp, giữ lòng bằng tâm pháp và mang theo thuật pháp.') + f'''
{h2('Linh căn', True)}
<p>Linh căn thức tỉnh ở <b>Luyện Khí trung kỳ</b> (khi đã đọc đủ một giờ), dựa trên <b>khung giờ bạn hay đọc</b> từ trước tới lúc đó.
Hành nào chiếm từ {ROOT_SHARE}% giờ đọc là một linh căn; hành nhiều giờ nhất là <b>chủ hành</b>.</p>
{hours}
{roots}
<p>Công pháp, thuật pháp cùng hành với linh căn là <b>hợp</b>: công pháp +20% linh lực, thuật pháp học không tốn ngộ tính.
Hành bị chủ hành khắc hoặc khắc chủ hành là <b>khắc</b>: công pháp −20%. Kim khắc Mộc, Mộc khắc Thổ, Thổ khắc Thủy, Thủy khắc Hỏa,
Hỏa khắc Kim. Thức tỉnh xong, sư phụ truyền một công pháp Hoàng phẩm và một thuật pháp hợp chủ hành.</p>
{tip(f'<b>Tẩy linh</b> một lần duy nhất: {WASH_PRICE} LT, cần {WASH_HOURS} giờ đọc từ lúc thức tỉnh; linh căn tính lại theo giờ đọc từ lúc thức tỉnh. Muốn đổi linh căn, hãy đổi giờ đọc trước đã.')}

{h2('Công pháp', True)}
<p>Mỗi lúc chỉ <b>vận một công pháp</b>. Chữ đọc lật trang luyện công pháp đang vận qua chín tầng; mỗi tầng +1% linh lực, cộng
với linh lực của phẩm. Tầng <b>4, 7, 9</b> cần <b>lĩnh ngộ</b>: đủ chữ rồi thì tiêu ngộ tính (1, 2, 3, cộng thêm một nửa số phẩm)
để mở. Đổi công pháp không mất gì: mỗi bộ giữ chữ đã luyện.</p>
{pham}
<p>Cửa ải đòi phẩm: vào Kim Đan cần Huyền, Nguyên Anh cần Địa, Hóa Thần cần Thiên, Linh giới cần Linh, Phi Thăng và Tiên nhân
cần Tiên phẩm. Tốc độ tu = 100% + linh căn + công pháp + hợp/khắc (trong khoảng 50 – 200%), rồi nhân với kim đan, bình cảnh,
nút thắt, thương thế.</p>

{h2('Tâm pháp', True)}
<p>Tâm pháp có <b>một ô từ Trúc Cơ</b>, hai ô từ Nguyên Anh. Mỗi buổi đọc liền từ 30 phút là một lần <b>vận chuyển</b> cho mọi tâm
pháp đang vận: {TAM_NHAP} lần là <b>nhập môn</b> (hiệu quả bắt đầu), {TAM_DAI} lần là <b>đại thành</b> (tâm cảnh +5). Phá Trúc Cơ được
truyền Thanh Tâm Quyết.</p>

{h2('Thuật pháp', True)}
<p>Thuật pháp là lá bài tự ra tay khi biến cố tới: bí cảnh tuần (công kích), ngày lỡ của kiếp (phòng ngự), đột phá hỏng (độn thuật),
tâm ma tấn công hay nhảy chương (phụ trợ), lần đột phá (thần thông, học từ Nguyên Anh). Ô mang: 2 ở Luyện Khí, thêm một ô mỗi đại
cảnh giới, tối đa 6. Mỗi lần ra tay thêm thuần thục: {" · ".join(f"{e(MASTERY[i])} {MASTERY_AT[i]}" for i in range(len(MASTERY)))};
bậc cao thì mạnh hơn (thêm linh thạch, đỡ thêm ngày, hồi khí ngắn hơn).</p>
<p>Học: hợp linh căn 0 ngộ tính, khác hành 1, bị khắc 2 (Thiên linh căn 2 và 3); chưa thức tỉnh thì 1.</p>

{h2('Ngọc giản và Tàng Kinh Các', True)}
<ul>
<li><b>Kỳ ngộ</b>: một phần kỳ ngộ là «tàn hồn truyền thừa», cho một ngọc giản công pháp, thuật pháp hoặc tâm pháp.</li>
<li><b>Bí cảnh tuần</b>: qua bí cảnh có {BI_CANH_SCROLL}% gặp thêm một ngọc giản.</li>
<li><b>Tàng Kinh Các</b>: mỗi thứ Hai đổi kho ba công pháp, hai tâm pháp, ba thuật pháp, hai đan phương; luôn có một công
pháp đúng phẩm cửa kế tiếp cần. Tâm pháp {TAM_PRICE} LT, thuật pháp 80 LT, thần thông 400 LT. Ngọc giản đã có thì thành 1 ngộ tính.</li>
</ul>
</section>
<section class="page">
{h2('Thư viện công pháp', True)}
{lib}
</section>
<section class="page">
{h2('Thư viện tâm pháp và thuật pháp', True)}
{tam}
{thuat}
</section>'''


KYNGO = read('game', 'KyNgo.kt')
KY_EVENTS = re.findall(r'e\("([^"]+)", (\d), T_\w+, -?\d+, -?\d+, (\d+), "([^"]+)",', KYNGO)
KY_RARITY = strings(KYNGO, 'RARITY')
KY_TIERS = strings(KYNGO, 'TIERS')
assert len(KY_EVENTS) == 72, len(KY_EVENTS)


def kyngo_table():
    order = sorted(KY_EVENTS, key=lambda ev: (int(ev[1]), -int(ev[2])))
    rows = [[f'<b>{e(n)}</b>', e(KY_RARITY[int(r)]), e(t), f'{(int(p) + 5) // 10}%'] for n, r, p, t in order]
    return table(['Kỳ ngộ', 'Độ hiếm', 'Gặp khi', 'Mỗi lần'], rows, 'lib')


DANDAO = read('game', 'DanDao.kt')
HERBS = strings(DANDAO, 'HERBS')
SEED_PRICES = numbers(DANDAO, 'SEED_PRICES')
GIAI = strings(DANDAO, 'GIAI')
MACH_PRICES = numbers(DANDAO, 'MACH_PRICES')
DAN_CAP = strings(DANDAO, 'CAP')
DAN_CAP_AT = numbers(DANDAO, 'CAP_AT')
DAN_PHAM = strings(DANDAO, 'PHAM')
DOC_PCT = numbers(DANDAO, 'DOC_PCT')
DAN_GROUPS = strings(DANDAO, 'GROUPS')
RECIPES = re.findall(r'Recipe\("([^"]+)", (G_\w+), listOf\(([^)]*)\), (\d+), (\d), (\d+), "([^"]+)"', DANDAO)
GROUP_CONST = ['G_TU_LUYEN', 'G_PHA_CANH', 'G_CHUA_THUONG', 'G_TAM_CANH', 'G_LUYEN_THE', 'G_GIAI_DOC', 'G_HIEM', 'G_LUYEN_THAN', 'G_THO', 'G_CAN_CO']
RECIPES = re.findall(r'Recipe\("([^"]+)", (G_\w+), listOf\(([^)]*)\), (\d+), (\d), (\d+), "([^"]+)"', DANDAO)
assert len(RECIPES) == 32 and len(HERBS) == 12, (len(RECIPES), len(HERBS))


def herb_list(spec):
    out = []
    for kind, years in re.findall(r'(\d+) to ([\d_]+)', spec):
        out.append(f'{HERBS[int(kind)]} {num(int(years.replace("_", "")))} năm')
    return ', '.join(out)


PHUS27 = re.findall(r'Phu\((\d+), (\d), (\d), (\d+), (\d)\)', read('data', 'TrangBi.kt'))
assert len(PHUS27) == 9, len(PHUS27)
THAN_TRONG = ['Nhất trọng', 'Nhị trọng', 'Tam trọng', 'Tứ trọng', 'Ngũ trọng', 'Lục trọng', 'Thất trọng', 'Bát trọng', 'Cửu trọng']
TELUYEN = read('data', 'TeLuyen.kt')
TRANS27 = re.findall(r'Tran\("([^"]+)", "([^"]+)", (\d+), (\d)\)', TELUYEN)
assert len(TRANS27) == 7, len(TRANS27)


def part2c():
    phus = table(['Phù', 'Phù chỉ', 'Chu sa', 'Buổi đọc', 'Thần thức'], [[f'<b>{e(ITEM_NAMES[int(i)])}</b>', p, k, f'{m}′', e(THAN_TRONG[int(tr)])] for i, p, k, m, tr in PHUS27], 'lib')
    trans = table(['Trận', 'Hiệu quả', 'Giá', 'Linh mạch'], [[f'<b>{e(n)}</b>', e(g), f'{p} LT', e(GIAI[int(gi) - 1])] for n, g, p, gi in TRANS27], 'lib')
    recipes = table(['Đan', 'Loại', 'Linh thảo', 'Hỏa hầu', 'Cần', 'Độc', 'Công dụng'], [
        [f'<b>{e(n)}</b>', e(DAN_GROUPS[GROUP_CONST.index(g)]), e(herb_list(h)), f'{m}′', e(DAN_CAP[int(c) - 1]), d, e(t)]
        for n, g, h, m, c, d, t in RECIPES], 'lib')
    mach = ' · '.join(f'{e(GIAI[g])} {num(MACH_PRICES[g])}' for g in range(1, 9))
    caps = ' · '.join(f'{e(DAN_CAP[i])} {DAN_CAP_AT[i]}' for i in range(9))
    seeds = ' · '.join(f'{e(HERBS[i])} {SEED_PRICES[i]}' for i in range(len(HERBS)))
    return chapter('IV', 'Động phủ và đan đạo', 'Từ bản 2.2, người tu có động phủ: linh mạch nuôi tu vi, dược viên trồng linh thảo, đan phòng luyện đan bằng những buổi đọc liền.') + f'''
{h2('Linh mạch', True)}
<p>Động phủ nằm trên linh mạch từ Nhất giai tới Cửu giai. Mỗi giai trên Nhất giai: linh lực +2%, đột phá +1%, dược linh thêm 1 năm
mỗi ngày. Luống dược viên mở ở Nhị, Tứ, Lục, Bát giai (2 tới 6 luống). Giá nâng (linh thạch): {mach}.</p>
<p>Động phủ có năm gian: Linh mạch, Dược viên, Đan phòng, Đan dược, Tĩnh thất (bế quan); Luyện khí thất là lò luyện, Tàng thư là
Kinh Các, đi từ tab Linh mạch.</p>

{h2('Dược viên', True)}
<p>Gieo một hạt vào luống trống. Mỗi ngày đọc từ 30 phút, linh thảo thêm <b>10 năm dược linh</b> (cộng giai linh mạch). Thảo 100 năm
cần khoảng 10 ngày, 1.000 năm khoảng 100 ngày: đan cao cấp đòi kiên trì thật. Thu hoạch lúc nào cũng được; mỗi lần thu được thêm
một hạt cùng loại. Hạt mua ở Dược viên (LT): {seeds}; còn hái được ở bí cảnh tuần và kỳ ngộ.</p>

{h2('Đan phòng và hỏa hầu', True)}
<p>Chọn đan phương, đủ linh thảo (dùng cây non nhất đủ tuổi), bấm <b>Khai lò</b>. Lò đỏ chờ <b>hỏa hầu</b>: buổi đọc tới phải liền
đủ số phút của đan. Thoát giữa chừng (buổi từ một phút trở lên) là <b>nổ lò</b>, mất linh thảo; tắt lò trước khi đọc thì lấy lại thảo.</p>
<p>Tỉ lệ thành = 60% + 8% mỗi bậc luyện đan sư trên mức đan cần + 3% mỗi 100 năm thảo dư (tối đa 15%), trong khoảng 20 – 98%.
Thành thì phẩm đan theo độ dư: dư từ 20 trung phẩm, 40 thượng phẩm, 60 cực phẩm. Luyện đan sư Tứ phẩm ra 2 viên một lò, Thất phẩm
3 viên. Cấp luyện đan sư theo số lò thành: {caps}.</p>

{h2('Phù thất', True)}
<p>Từ bản 2.7.2 phù lục vẽ được bằng tay (Pháp bảo › Túi đồ, trang 3): phù chỉ, chu sa và một buổi đọc liền giữ nét bút; dừng sớm
là đứt nét, mất vật liệu. Tỉ lệ 55% + 6% mỗi trọng thần thức trên mức phù cần, Bách nghệ +10%, Phù Lục Sơn +10% (20 – 95%); dư từ
40% thì được hai tấm. Phù chỉ mua 10 LT hoặc nhặt ở bí cảnh (2 mỗi tuần); chu sa chỉ có ở bí cảnh tuần (1 – 2).</p>
{phus}

{h2('Trận pháp', True)}
<p>Từ bản 2.7.1. Dựng trận bằng linh thạch ở Động phủ › Trận pháp; trận chỉ có hiệu lực khi bày vào <b>trận nhãn</b>: một trận nhãn,
thêm một ở Tứ giai và một ở Thất giai linh mạch. Thu trận ra rồi bày trận khác lúc nào cũng được.</p>
{trans}

{h2('Đan độc', True)}
<p>Mỗi viên đan để lại độc: hạ phẩm trọn phần, trung {DOC_PCT[1]}%, thượng {DOC_PCT[2]}%, cực phẩm {DOC_PCT[3]}%. Đan mua ở Phường thị
để lại 4. Đan độc trên 60: đan chỉ còn nửa công hiệu, căn cơ −10. Mọi lúc, tỉ lệ đột phá trừ đan độc / 4. Mỗi ngày đọc 30 phút
tan 5; Giải Độc Đan tan 25. Phẩm đan cao còn mạnh hơn: mỗi phẩm +20% công dụng (đan phá cảnh +2% mỗi phẩm).</p>
</section>
<section class="page">
{h2('Mười lăm đan phương', True)}
<p class="small">Đan phá cảnh chỉ dùng cho đúng cửa của nó, một viên mỗi lần đột phá. Có từ đầu: Tụ Khí Đan, Trúc Cơ Đan, Hồi Khí Đan,
Giải Độc Đan; còn lại tìm ở kỳ ngộ, bí cảnh tuần, Tàng Kinh Các.</p>
{recipes}
</section>'''


TONGMON = read('data', 'TongMon.kt')
SECT_ROWS = re.findall(r'Sect\("([^"]+)", (\d)', TONGMON)
WAYS5 = ['Pháp tu', 'Thể tu', 'Thần tu', 'Kiếm tu', 'Bách nghệ']
PERKS5 = re.findall(r'^        "([^"]+)",$', TONGMON[TONGMON.index('val PERKS'):TONGMON.index('private fun r(')], re.M)
SECTS = [(WAYS5[f], [n for n, ff in SECT_ROWS if int(ff) == f], PERKS5[f]) for f in range(5)]
TASKS = re.findall(r'Task\("([^"]+)", [\d_]+, (\d+)\)', TONGMON)
RANKS = strings(TONGMON, 'RANKS')
RANK_AT = numbers(TONGMON, 'RANK_AT')
SALARY = numbers(TONGMON, 'SALARY')
BICANH = read('data', 'BiCanh.kt')
BC_TYPES = strings(BICANH, 'TYPES')
assert len(SECT_ROWS) == 36 and len(PERKS5) == 5 and len(TASKS) == 12 and len(BC_TYPES) == 7


def part2d():
    sects = table(['Dòng', 'Tông', 'Ưu đãi'], [[f'<b>{e(w)}</b>', e(', '.join(ns)), e(pk)] for w, ns, pk in SECTS], 'lib')
    tasks = '<p class="small">Mười hai việc có thể được giao: ' + '; '.join(f'{e(t[0].lower() + t[1:])} ({r})' for t, r in TASKS) + '.</p>'
    ranks = ' · '.join(f'{e(RANKS[i])} {num(RANK_AT[i])}' for i in range(5))
    salary = ' / '.join(str(x) for x in SALARY)
    return chapter('V', 'Tông môn và bí cảnh', 'Từ bản 2.3, người tu có sơn môn để bái, đồng môn để đua, và bí cảnh thành một bản đồ đầy biến cố.') + f'''
{h2('Ba mươi sáu tông môn', True)}
<p>Linh căn thức tỉnh (Luyện Khí trung kỳ) thì các tông mới nhận đệ tử. Ba mươi sáu tông chia năm dòng, mỗi dòng một ưu đãi chung;
mỗi tông đòi điều riêng (linh căn, dị linh căn, công pháp, đạo lộ, căn cơ, thể phách, thần thức, chuỗi ngày, sách đọc trọn, lò
đan, pháp bảo, cảnh giới). Mỗi lúc chỉ 6 tông hợp nhất chịu nhận: 7 với song linh căn hoặc dị linh căn, 8 với Thiên linh căn.
Chuyển tông được một lần, cống hiến ở lại tông cũ.</p>
{sects}
<p>Thân phận theo tổng cống hiến đã nhận (Chân truyền cần Kim Đan, Trưởng lão cần Nguyên Anh, Thái thượng cần Hóa Thần): {ranks}.</p>

{h2('Nhiệm vụ, bổng lộc, bảo khố', True)}
<p>Mỗi thứ Hai chấp sự đường giao ba việc (việc đầu theo sở trường của tông). Xong việc nào bấm nhận việc ấy; xong cả ba nhận bổng
lộc tuần: 50 cống hiến và linh thạch theo thân phận ({salary}). Hết tuần chưa nhận là mất. Cống hiến còn đến từ sách đọc trọn (20),
vượt đối thủ (40), cứu đồng đạo trong bí cảnh (30), đại bỉ.</p>
{tasks}
<p>Bảo khố đổi cống hiến lấy ngọc giản công pháp đúng phẩm cửa kế tiếp, thuật pháp, tâm pháp, đan phương, hạt linh thảo, khoáng
thạch Địa và Thiên phẩm, và nâng linh mạch một giai.</p>

{h2('Đối thủ và đại bỉ', True)}
<p>Bái nhập xong có một <b>sư huynh hoặc sư tỷ</b> bắt đầu nhỉnh hơn bạn 5%. Mỗi ngày người ấy tu thêm bằng nhịp đọc 30 ngày qua của
chính bạn nhân 1,1: muốn vượt phải đọc đều hơn chính mình tháng trước. <b>Tông môn đại bỉ</b> cuối mỗi quý: chữ
đọc trong quý đọ với bảy đồng môn (người đọc đều khoảng 7.000 chữ một ngày). Hạng nhất 200 cống hiến
và một ngọc giản; hạng 2 – 3: 120 và một đan phương; 4 – 5: 60; còn lại 20.</p>

{h2('Bí cảnh thành bản đồ', True)}
<p>Mỗi tuần một bản đồ 5 nút (4 với bộ Thương Kích Liệt Trận). Mỗi ngày đọc từ 30 phút mở thêm một nút. Trước mỗi nút chọn rẽ trái
hay phải: mỗi bên hiện biến cố đang chờ. Biến cố: {e(", ".join(BC_TYPES[:6]))}; nút cuối là {e(BC_TYPES[6])}.</p>
<ul>
<li>Mỗi biến cố có hai tới bốn cách giải. Thuật pháp công kích, độn thuật, phòng ngự, Thiên Nhãn, pháp bảo từ Địa phẩm, căn cơ, thần
thức mở cách giải tốt hơn; liều đánh hay cưỡng phá thì có tỉ lệ và có thể bị thương.</li>
<li>Hai lần bị thương là <b>trọng thương</b>: rời bí cảnh ngay, tuần sau tu luyện chỉ còn 85%.</li>
<li><b>Thượng cổ di tích</b>: 20 ngày đọc 30 phút trong tháng mở di tích: ngọc giản, đan phương, bảo vật, 100 LT.</li>
</ul>
</section>'''


WAYS24 = [
    ('Pháp tu', 'chữ đọc', 'Pháp Anh · Thuần Dương Nguyên Thần', 'linh lực +15%', 'lôi kiếp, thiên kiếp −5%', '3 ngày × 45′'),
    ('Thể tu', 'buổi liền 60′: +1.000 linh lực', 'Thể Anh · Bất Hoại Pháp Thân', 'luyện thể ×2; +2% mỗi bậc thể ở kiếp', 'linh lực −10%', '2 ngày × 60′'),
    ('Thần tu', 'sách đọc trọn +3.000, tác giả mới +5.000', 'Thần Anh · Phân Thần Hóa Thân', 'luyện thần ×2; tâm ma tăng một nửa', 'thể phách −25%', '4 ngày × 35′'),
    ('Kiếm tu', 'cuốn xong trong 14 ngày +4.000; chuỗi từ 7 ngày +10%', 'Kiếm Anh · Kiếm Ý Thông Thiên', '1/4 lần đột phá thành vào thẳng trung kỳ', 'tâm ma ×1,5', '2 ngày × 50′'),
    ('Bách nghệ', 'mỗi lò đan, mỗi pháp bảo +1.500', 'Hỏa Anh · Thiên Hỏa Hóa Thân', 'luyện đan +15%, đan độc −25%', 'linh lực −15%', '3 ngày × 45′'),
]

LINHGIOI = read('data', 'LinhGioi.kt')
LAWS25 = [(m[0], m[2], m[3], *m[4:]) for m in re.findall(
    r'Law\("([^"]+)", "([^"]+)", "([^"]+)", "([^"]+)",\s*listOf\("([^"]+)", "([^"]+)", "([^"]+)"\)\)', LINHGIOI)]
assert len(LAWS25) == 8, len(LAWS25)

TIEN_HIEU = strings(read('data', 'TienDinh.kt'), 'TIEN_HIEU')
NHATKY = read('data', 'NhatKy.kt')
RESOURCE28 = strings(NHATKY, 'RESOURCE')
WAY28 = strings(NHATKY, 'WAY')
assert len(RESOURCE28) == 6 and len(WAY28) == 6, (RESOURCE28, WAY28)
assert len(TIEN_HIEU) == 6
DOKIEP = read('data', 'DoKiep.kt')
KIEP26 = re.findall(r'Tang\("([^"]+)", (\w+), (\d+), "([^"]+)"\)', DOKIEP)
KIEP_BY = {'LOI': 'pháp bảo, bản mệnh, nhục thân, Tử Lôi pháp tắc', 'PHAP_TAC': 'pháp tắc, đạo vực, thần thức',
           'TAM_MA': 'tâm cảnh, tâm pháp', 'CUU_CUU': 'tất cả, nửa sức; tâm lôi chỉ tâm cảnh'}
assert len(KIEP26) == 9, len(KIEP26)

DANS24 = [
    ('Canh Kim Đan', 'Ngũ hành', 'linh căn Kim, công pháp Kim', 'linh lực +5%, kết anh +6%'),
    ('Ất Mộc Đan', 'Ngũ hành', 'linh căn Mộc, công pháp Mộc', 'căn cơ +8, kết anh +3%'),
    ('Nhâm Thủy Đan', 'Ngũ hành', 'linh căn Thủy, công pháp Thủy', 'tâm ma chậm 25%, kết anh +4%'),
    ('Bính Hỏa Đan', 'Ngũ hành', 'linh căn Hỏa, công pháp Hỏa', 'linh lực +12%, tâm ma +15%, kết anh −3%'),
    ('Mậu Thổ Đan', 'Ngũ hành', 'linh căn Thổ, hoặc khi chưa có đan khác', 'căn cơ +5, lôi kiếp −1 ngày, kết anh +4%'),
    ('Tử Lôi Đan', 'Biến dị', 'dị linh căn Lôi', 'linh lực +8%, lôi kiếp −2 ngày, kết anh +8%'),
    ('Huyền Băng Đan', 'Biến dị', 'dị linh căn Băng', 'tâm ma chậm 40%, kết anh +6%'),
    ('Thanh Phong Đan', 'Biến dị', 'dị linh căn Phong', 'linh lực +15%'),
    ('Thái Dương Đan', 'Âm Dương', 'Thanh Minh, đọc ban ngày', 'linh lực +15%, kết anh +5%'),
    ('Thái Âm Đan', 'Âm Dương', 'Thanh Minh, đọc đêm khuya', 'căn cơ +8, tâm ma chậm 40%, kết anh +5%'),
    ('Trường Sinh Đan', 'Sinh Tử', 'căn cơ 80, chưa hỏng, chuỗi 60 ngày', 'căn cơ +12, tâm ma chậm 20%, kết anh +10%'),
    ('Tịch Diệt Đan', 'Sinh Tử', 'ba lần hỏng hoặc từng tẩu hỏa', 'linh lực +20%, tâm ma +30%, kết anh +10%'),
]

THANS24 = [
    ('Thuần Dương Nguyên Thần', 'Pháp tu', 'linh lực +10% · Hợp Thể −5% · Độ Kiếp +10%'),
    ('Bất Hoại Pháp Thân', 'Thể tu', 'thể phách +20% · Hợp Thể +10%, thiên kiếp −1 ngày · Độ Kiếp +5%'),
    ('Phân Thần Hóa Thân', 'Thần tu', 'thần thức +20% · Hợp Thể −10% · Độ Kiếp +8%'),
    ('Kiếm Ý Thông Thiên', 'Kiếm tu', 'linh lực +5% · Hợp Thể +5% · Độ Kiếp −10%'),
    ('Thiên Hỏa Hóa Thân', 'Bách nghệ', 'luyện đan +10% · Hợp Thể thiên kiếp +1 ngày · Độ Kiếp +15%'),
    ('Song Thần Đồng Tu', 'Song Anh', 'linh lực +5% · Hợp Thể +15%, thiên kiếp −1 ngày · Độ Kiếp +5%'),
]

GIOI24 = [
    ('Cửu Tiêu Vân Khuyết', 'Thiên Tiên', 'Kim, Lôi', 'Pháp tu', 'Vạn Quyển Tông', 'năm thước đo cân bằng'),
    ('Huyết Nguyệt Trầm Uyên', 'Nghịch Tiên', 'Hỏa, Kiếm', 'Kiếm tu', 'Kiếm Lư', 'đột phá hỏng, tẩu hỏa'),
    ('Bát Nhã Liên Đài', 'Thiền Tiên', 'Thổ, Phong', '—', 'Minh Tâm Tự', 'tâm cảnh, chuỗi ngày'),
    ('Hồng Hoang Cổ Địa', 'Chiến Tiên', 'Mộc, Thể', 'Thể tu', 'Thiết Cốt Môn', 'thể phách'),
    ('Vong Xuyên Bỉ Ngạn', 'Minh Tiên', 'Thủy, Băng', 'Bách nghệ', 'Bách Thảo Cốc', 'sách bỏ dở rồi đọc xong'),
    ('Thư Hải Vô Nhai', 'Văn Tiên', 'Thần', 'Thần tu', '—', 'thần thức, tạp linh căn'),
]


def part2e():
    ways28 = table(['Giới', 'Tài nguyên', 'Sống bằng'], [[f'<b>{e(GIOI24[i][0])}</b>', e(RESOURCE28[i]), e(WAY28[i])] for i in range(6)], 'lib')
    ways = table(['Đường', 'Sống bằng', 'Kết anh · hóa thần', 'Mạnh', 'Yếu', 'Ngưng đan'],
                 [[f'<b>{e(w[0])}</b>'] + [e(x) for x in w[1:]] for w in WAYS24], 'lib')
    gioi = table(['Giới', 'Thành', 'Hành', 'Đạo lộ', 'Tông môn', 'Đời tu'],
                 [[f'<b>{e(g[0])}</b>'] + [e(x) for x in g[1:]] for g in GIOI24], 'lib')
    dans = table(['Kim Đan', 'Nhóm', 'Kết khi', 'Được'], [[f'<b>{e(d[0])}</b>', e(d[1]), e(d[2]), e(d[3])] for d in DANS24], 'lib')
    thans = table(['Hóa Thần', 'Từ', 'Lợi hại, Hợp Thể, Độ Kiếp'], [[f'<b>{e(t[0])}</b>', e(t[1]), e(t[2])] for t in THANS24], 'lib')
    laws = table(['Pháp tắc', 'Mở khi', 'Được', 'Ba thử thách'], [[f'<b>{e(l[0])}</b>', e(l[1]), e(l[2]), e(' → '.join(l[3:]))] for l in LAWS25], 'lib')
    kiep = table(['Tầng', 'Kiếp', 'Luật', 'Nền', 'Đỡ bằng'], [[str(i + 1), f'<b>{e(n)}</b>', e(r), f'{b}%', e(KIEP_BY[k])] for i, (n, k, b, r) in enumerate(KIEP26)], 'lib')
    tienhieu = table(['Giới', 'Thành', 'Tiên hiệu'], [[f'<b>{e(g)}</b>', e(k), e(h)] for g, k, h in zip(
        ['Cửu Tiêu Vân Khuyết', 'Huyết Nguyệt Trầm Uyên', 'Bát Nhã Liên Đài', 'Hồng Hoang Cổ Địa', 'Vong Xuyên Bỉ Ngạn', 'Thư Hải Vô Nhai'],
        ['Thiên Tiên', 'Nghịch Tiên', 'Thiền Tiên', 'Chiến Tiên', 'Minh Tiên', 'Văn Tiên'], TIEN_HIEU)], 'lib')
    return chapter('VI', 'Đạo lộ và Lục Giới', 'Từ bản 2.4, mỗi người tu đi một trong năm con đường; bản 2.5 mở Linh giới, bản 2.6 dựng Độ Kiếp chín tầng, bản 2.7 mở Lục Giới Môn và khép phần một. Con đường ấy, cùng cả đời tu, quyết định giới nào tiếp dẫn khi phi thăng.') + f'''
{h2('Năm đạo lộ', True)}
<p>Lên Trúc Cơ thì chọn đạo lộ, một lần cho cả đời tu, trước khi ngưng đan (cửa Kim Đan đòi điều này). Người đã qua Kim Đan từ bản cũ
chọn được bất cứ lúc nào. Màn Đạo lộ đánh dấu đường «hợp nhất» với cách đọc của bạn.</p>
{ways}
<p class="small">Linh lực từ cách đọc riêng của đường đi thẳng vào đan điền (ở bình cảnh thì thành áp chế). Đạo lộ trùng sở trường
tông môn: đột phá +2%.</p>

{h2('Luyện thể, luyện thần', True)}
<p>Hai cột mọi đường đều theo. <b>Luyện thể</b> theo thể phách, bảy bậc Phàm Thể tới Bất Diệt Thể: mỗi bậc +1% ở lôi kiếp và thiên
kiếp (Thể tu +2%). <b>Luyện thần</b> theo thần thức, chín trọng: mỗi trọng +1% khi phá tiểu cảnh (Thần tu +2%).</p>

{h2('Kiêm tu và Song Anh', True)}
<p>Từ Nguyên Anh được <b>kiêm tu</b> một đường thứ hai: có cái mạnh và cách sống của đường ấy, không mang cái yếu, đổi lại linh lực
−20%. Ai kiêm tu cả Thể tu và Thần tu, tới Nguyên Anh đại viên mãn, thể phách Kim Thân, thần thức Thất trọng, tâm ma dưới 30 được
thử <b>Song Anh</b>: 20% + căn cơ/4 + 5 mỗi bậc tâm cảnh + 10% mỗi lần hỏng trước (5 – 80%). Thành: linh lực +10%, mọi đột phá sau
+5%. Hỏng: tâm ma +10, nghỉ 7 ngày.</p>

{h2('Mười hai Kim Đan, sáu Hóa Thần', True)}
<p>Kim Đan kết ra theo cả đời tu, xét theo thứ tự: <b>Sinh Tử</b> (Tịch Diệt Đan khi đã ba lần đột phá hỏng hoặc từng tẩu hỏa;
Trường Sinh Đan khi căn cơ 80, chưa hỏng lần nào, chuỗi 60 ngày), <b>Âm Dương</b> (tâm cảnh Thanh Minh, 3/4 giờ đọc từ 6 tới 18
giờ là Thái Dương; nửa giờ đọc từ 22 tới 5 giờ là Thái Âm), <b>biến dị</b> theo dị linh căn (Tử Lôi, Huyền Băng, Thanh Phong),
cuối cùng <b>ngũ hành</b> theo linh căn và công pháp (Canh Kim, Ất Mộc, Nhâm Thủy, Bính Hỏa, Mậu Thổ). Nguyên Anh kế thừa đan:
tên «Tử Lôi Kiếm Anh», tỉ lệ kết anh của đan, tốc độ của đan tăng thêm một nửa.</p>
{dans}
{thans}

{h2('Linh giới: phi thăng, pháp tắc, đạo vực', True)}
<p>Từ bản 2.5. <b>Phi thăng</b> (cửa vào Luyện Hư) có ba bước: <b>không gian tiết điểm</b> là qua bí cảnh bốn tuần liền; <b>trảm trần
duyên</b> là chọn một bản mệnh pháp bảo (hai nếu Song Anh) và ba món mang theo; rồi <b>phi thăng kiếp</b> 7 ngày × 55 phút. Ở Linh
giới, bộ nào không có món nào được mang lên thì bị áp chế, mất hiệu quả (tu vi không bị ảnh hưởng); mỗi 30 ngày tế luyện thêm một
món. Bản mệnh lớn một tầng mỗi 20 giờ đọc, tới tầng 9, mỗi tầng linh lực +1%.</p>
{laws}
<p>Mỗi lúc tham ngộ một pháp tắc; thử thách tính từ ngày bắt đầu, đổi sang pháp tắc khác thì mất tiến độ. Lĩnh ngộ được 300 LT.
<b>Hợp Thể</b> cần một pháp tắc và thân thần hợp nhất: thể phách 300, thần thức 24 (ngũ hành tạp linh căn 150 và 12). <b>Đại Thừa</b>
cần ba pháp tắc và một <b>đạo vực</b> tự đặt tên: ba pháp tắc trong vực tính gấp đôi, linh lực thêm 10%. Lôi pháp tắc bớt một ngày
mọi thiên kiếp từ Hợp Thể; Thời gian bớt một ngày hồi khí.</p>

{h2('Độ Kiếp chín tầng, ngũ suy, thọ nguyên', True)}
<p>Từ bản 2.6, cửa Độ Kiếp (Đại Thừa vào Phi Thăng) là <b>chín tầng kiếp</b>, độ lần lượt ở Linh giới › Độ Kiếp. Mỗi tầng: đọc
đúng luật cho tới khi đủ, rồi kiếp giáng (một lần gieo tỉ lệ). Hỏng (lỡ ngày, nhảy chương, đổi sách, hay không đỡ nổi) thì tầng
đã qua vẫn giữ: tâm ma +10, kinh mạch tổn thương 5 ngày, nghỉ 2 ngày, lần sau +10%.</p>
{kiep}
<p>Chữ đọc từ lúc qua tầng trước là <b>kiếp lực</b>: 1% mỗi 4 vạn, tối đa 20%. Độ Kiếp Đan cộng 8% cho tầng kế. Mỗi tầng qua cho
100 LT × số tầng và một <b>kiếp ấn</b> ghi nhờ gì mà đỡ: pháp bảo, nhục thân, pháp tắc, thần thức, tâm cảnh, kiên trì. Mỗi ấn
cộng 3 cảm ứng cho giới hợp (pháp bảo, pháp tắc về Cửu Tiêu; nhục thân về Hồng Hoang; thần thức về Thư Hải; tâm cảnh về Bát Nhã;
kiên trì về Huyết Nguyệt). Qua tầng chín là vào Phi Thăng, 400 LT.</p>
<p><b>Thiên nhân ngũ suy</b>: vào Phi Thăng, căn cơ và tâm cảnh −15. Trong 30 ngày phải đạt cả năm: căn cơ 60, tâm cảnh Thanh Minh,
thần thức Thất trọng, thể phách Kim Thân, 20 trong 30 ngày đọc 30 phút. Không kịp thì ngũ suy lặp lại tháng sau. Qua rồi cửa Lục
Giới Môn mới mở.</p>
<p><b>Thọ nguyên</b>: mỗi ngày là một năm. Hạn: Luyện Khí 120, Trúc Cơ 250, Kim Đan 500, Nguyên Anh 1.000, Hóa Thần 2.000, Luyện Hư
3.000, Hợp Thể 5.000, Đại Thừa 8.000, Phi Thăng 12.000; Tiên nhân vô tận. Một phần mười cuối hạn: đột phá liều mạng +10%. Diên Thọ
Đan +50 năm. Ở <b>chế độ Chân Tu</b> (Cài đặt › Chế độ tu) cạn hạn là tọa hóa: chuyển thế, tu lại từ Luyện Khí, nhanh gấp rưỡi tới
cảnh giới kiếp trước; pháp bảo, đạo hiệu, Niên kỷ, chữ đã đọc vẫn giữ. Mỗi lần chuyển thế cộng 10 cảm ứng Vong Xuyên Bỉ Ngạn.</p>

{h2('Lục Giới Môn, màn kết, tiền đình', True)}
<p>Từ bản 2.7. Phi Thăng đại viên mãn, qua ngũ suy, cảm ứng rõ một giới và vượt thiên kiếp cuối là <b>qua Lục Giới Môn</b>. Giới
cảm ứng mạnh nhất tiếp dẫn; người tu thành tiên nhân của giới ấy và nhận <b>tiên hiệu</b>:</p>
{tienhieu}
<p>Qua cửa chỉ mang theo <b>bản mệnh pháp bảo, công pháp đang vận, linh căn và kiếp ấn</b>; phần còn lại ở lại Linh giới làm di sản
(vẫn trong bộ sưu tập và Niên kỷ). Màn <b>Tiên đồ</b> tự mở một lần: đó là <b>màn kết phần một</b>, kể lại trọn đời tu (năm tháng,
chữ, giờ, sách, tác giả, chuỗi, kỳ ngộ, pháp bảo, đạo hiệu, nghiệp lực), đạo đồ từng cảnh giới và hành trang. Năm giới chưa có
phần hai: người tu đứng ở <b>tiền đình</b>, mọi chữ đọc từ ngày qua cửa được ghi lại để tính khi giới mở. Cửu Tiêu Vân Khuyết tiếp
nối chín phẩm Thiên Tiên.</p>

{h2('Sổ đọc cho phần hai', True)}
<p>Từ bản 2.8 app ghi sổ theo từng ngày: giờ đọc, từng buổi đọc, giây đọc mỗi cuốn, <b>chữ đọc lại</b> (sách đã trọn đọc lại từng
trang; nhảy không tính, không cộng tu vi), chữ ở <b>sách quay lại</b> sau 90 ngày bỏ dở, thể loại và ngôn ngữ sách (thông tin
EPUB). Mỗi giới phần hai sống bằng một lối đọc; sổ này để khi giới mở, chữ ở tiền đình được quy đổi đúng cách người tu đã đọc.
Niên kỷ › Lối đọc cho xem lối đọc 30 ngày qua và tính thử tài nguyên của cả sáu giới; tiền đình hiện tài nguyên dự tính của giới
mình. Luật phần hai còn là nháp, con số có thể đổi.</p>
{ways28}

{h2('Lục Giới cảm ứng', True)}
<p>Không ai chọn được giới để phi thăng. Mỗi giới chấm điểm cả đời tu; giới cao điểm nhất tiếp dẫn. Cửa Lục Giới Môn đòi cảm ứng
<b>rõ</b>: giới đứng đầu hơn giới kế tiếp từ 5 điểm. Đột phá được cộng điểm cảm ứng chia mười (tối đa 10%).</p>
<ul>
<li><b>Linh căn</b> cùng hành với giới: căn mạnh nhất 30, rồi 18, 12, 8, 6 (dị linh căn tính như căn mạnh nhất).</li>
<li><b>Công pháp</b> đang vận cùng thuộc tính: 20, đại thành thêm 5. <b>Tâm pháp</b> đã nhập môn: 8 (Tĩnh Tọa Kinh về Cửu Tiêu; Đạo Tâm Chủng Ma 15 về Huyết Nguyệt; Thanh Tâm, Vong Ngã về Bát Nhã; Dưỡng Khí về Hồng Hoang; Băng Tâm, Định Hồn về Vong Xuyên).</li>
<li><b>Đạo lộ</b> chính 20, kiêm tu 10. <b>Tông môn</b> dẫn tới giới ấy 10. <b>Đời tu</b> tới 25 điểm, mỗi giới đòi một điều riêng.</li>
<li><b>Kim Đan</b> và <b>Hóa Thần</b> nghiêng về một giới: 10 mỗi thứ.</li>
<li><b>Nghiệp lực</b> hiện ở Phi Thăng: thiện nghiệp từ sách đọc trọn (2 mỗi cuốn, tối đa 40), chuỗi dài nhất, tâm cảnh, cống hiến
tông môn; ác nghiệp từ sách bỏ rơi quá ba tháng, nhảy chương, tâm ma, tẩu hỏa, Đạo Tâm Chủng Ma, đan độc. Thiện nghiệp nâng Cửu
Tiêu (× 0,4), Bát Nhã (× 0,5), Thư Hải (× 0,2); ác nghiệp kéo về Huyết Nguyệt (× 0,5), Vong Xuyên (× 0,3); Hồng Hoang ưa nghiệp
lực quân bình. Cửa Lục Giới Môn cộng nghiệp lực chia mười.</li>
</ul>
{gioi}
</section>'''


def part3():
    rows = []
    for t in range(6):
        lo = [0, 40000, 108000, 202000, 405000, 810000][t]
        hi = [40000, 108000, 202000, 405000, 810000, None][t]
        length = f'dưới {van(hi)} vạn' if t == 0 else (f'{van(lo)} – {van(hi)} vạn' if hi else f'từ {van(lo)} vạn')
        rows.append([img(f'art_ore_{t}', 'oi'), f'<b>{e(TIER_NAMES[t])}</b>', num(FORGE_WORDS[t]), num(STAGE_WORDS[t]), length])
    kiep = [[f'<b>{e(TIER_NAMES[t])}</b>', f'{max(2, min(4, t - 1))} ngày', f'{10 * max(2, min(4, t - 1))} phút', f'{30 * (t - 1)} LT'] for t in (3, 4, 5)]
    return chapter('VII', 'Lò luyện', 'Sáu ô lò cháy cùng lúc với mỗi chữ bạn đọc. Xếp lò khéo hay vụng quyết định bạn có bao nhiêu pháp bảo.') + f'''
{h2('Sáu ô, ba kiểu')}
<ul>
<li><b>Khoáng thạch</b>: cháy bằng chữ của <i>mọi</i> cuốn; đủ chữ thì ra một pháp bảo cùng phẩm, ô trống ra. Lò tự nạp tới
3 ô khoáng thạch từ túi, phẩm cao trước; bạn có thể tự đặt thêm vào ô trống.</li>
<li><b>Chặng sách</b>: chỉ cháy bằng chữ của cuốn đó. Cứ đủ số chữ của chặng là ra một pháp bảo, rồi luyện tiếp chặng sau,
cho tới khi đọc xong sách. Mỗi cuốn đặt mỗi phẩm một chặng.</li>
<li><b>Trọn quyển</b>: đọc hết cuốn (95%) thì ra một pháp bảo, phẩm theo độ dài sách.</li>
</ul>
<p>Mỗi cuốn chiếm tối đa 3 ô. Sách đã đọc xong không đặt vào lò được nữa.</p>
{table(['', 'Phẩm', 'Khoáng thạch', 'Một chặng', 'Trọn quyển'], rows, 'ores')}
<p class="small">Số chữ cần cho mỗi ô. Bộ Khai Thiên Phủ Đao giảm 10% cho khoáng thạch và chặng sách.</p>

{h2('Lửa nhân lửa')}
<p>Ba thứ làm lò cháy nhanh hơn, và chúng <b>nhân với nhau</b>:</p>
{table(['Nguồn', 'Hệ số', 'Bao lâu'], [
    ['Thiên Hỏa Phù', '×2', '2 vạn chữ (4 vạn với bộ Võ Lâm Ám Khí)'],
    ['Bế quan', '×1,5 – ×2,0 theo đẳng tĩnh thất', 'tới khi bế quan viên mãn'],
    ['Bộ Tứ Hải Long Cung', '×2', 'mồng một, ngày rằm, ba ngày Tết'],
])}
<p>Cả ba cùng lúc: mỗi chữ cháy như <b>6 chữ</b> ở mọi ô. Ô trọn quyển không theo hệ số, vì nó tính bằng % của sách.</p>

{h2('Đọc xong một cuốn')}
<ul>
<li>Có ô trọn quyển: ô đó thành pháp bảo phẩm theo độ dài.</li>
<li>Không có: bạn nhận một <b>khoáng thạch</b> phẩm theo độ dài, bỏ vào túi.</li>
<li>Các ô chặng của cuốn đó được gỡ ra; <b>chặng đang dở thì mất</b>.</li>
</ul>

{h2('Kiếp phá lô')}
<p>Ô từ <b>Thiên phẩm</b> trở lên khi đủ chữ chưa ra ngay: lò phải qua kiếp phá lô, mỗi ngày đọc đủ số phút. Trong lúc
qua kiếp, ô đó ngừng cháy.</p>
{table(['Phẩm', 'Số ngày', 'Mỗi ngày', 'Thưởng'], kiep)}
<ul class="small">
<li>Ngày đầu và hôm nay chỉ tính khi đủ phút; một ngày ở giữa thiếu phút thì lò nứt: ô khoáng thạch hay chặng lui về 70%
chữ và phải luyện lại phần còn lại; ô trọn quyển thì kiếp bắt đầu lại từ hôm đó.</li>
<li>Vượt kiếp: pháp bảo ra lò mang dấu «Kiếp Luyện».</li>
<li>Trợ lực: Dẫn Lôi Phù (+1 ngày), Hộ Lô Phù (tự cháy thay khi lỡ), Định Thần Phù (+15 phút), bộ Thiết Huyết Quân Trận
(bớt 1 ngày), bộ Thái Cực Lưỡng Nghi (tha 1 ngày lỡ).</li>
</ul>
</section>'''


def part4():
    clans = ''.join(f'<tr><td><b>{e(TIER_NAMES[t])}</b></td><td>{e(CLANS[t])}</td><td>{e(NAMES[t * 12])}, {e(NAMES[t * 12 + 1])}…</td></tr>' for t in range(6))
    merge = ', '.join(f'{e(TIER_NAMES[t])} {10 * (t + 1)}' for t in range(5))
    return chapter('VIII', 'Pháp bảo: bộ, khí linh, hợp luyện', '72 loại pháp bảo, sáu phẩm, mười tám bộ. Đây là thứ để sưu tầm, và cũng là chìa khóa của những hiệu quả mạnh nhất.') + f'''
{h2('Sáu phẩm, sáu tộc')}
<table class="clans"><thead><tr><th>Phẩm</th><th>Tộc</th><th>Ví dụ</th></tr></thead><tbody>{clans}</tbody></table>
<p>Mỗi phẩm có 12 loại. Pháp bảo ra lò thuộc một loại <b>ngẫu nhiên</b> trong 12 loại của phẩm đó, mang thêm một chữ «chi»
đứng đầu tên (Hỗn, Nguyên, Long, Phượng…) cho có cá tính: «Long Đơn đao», «Minh Hồ lô».</p>

{h2('Tế luyện cấm chế', True)}
<p>Từ bản 2.7.1. Ở Pháp bảo › Tế luyện, đặt một pháp bảo lên lửa: mỗi chữ đọc luyện cấm chế cho món ấy (không bớt tu vi). Tầng n
cần 2.000 + 1.000 × n chữ; trọn 36 tầng khoảng 74 vạn chữ. Tầng 1 – 9 là <b>pháp khí</b>, 10 – 18 <b>pháp bảo</b>, 19 – 27 <b>linh
bảo</b>, 28 – 36 <b>thông thiên linh bảo</b>. Qua mốc tầng 9, 18, 27 phải phá cấm chế: một khoáng thạch (từ phẩm của món, tối đa Thiên
phẩm) và 50 / 150 / 400 LT; thông thiên linh bảo chỉ luyện được ở Linh giới. Đổi món đang luyện thì giữ tầng đã luyện, mất phần chữ dở.
Món luyện sâu nhất cho: linh lực +1% mỗi 4 tầng, đột phá +1% mỗi phẩm, bí cảnh liều đánh và điện chủ +4% mỗi phẩm, lôi kiếp Độ Kiếp
+2% mỗi phẩm.</p>

{h2('Ô trang bị', True)}
<p>Từ bản 2.7.2. Ở Kim Đan người tu đeo được 2 pháp bảo, mỗi đại cảnh giới thêm một ô (Nguyên Anh 3 … Tiên nhân 9); bản mệnh luôn
đeo, không tốn ô. Từ Kim Đan, chỉ món <b>đang đeo</b> mới dùng được trong biến cố bí cảnh (liều đánh, phá bằng pháp bảo, điện chủ)
và lôi kiếp Độ Kiếp; mỗi món đeo thêm sau món đầu là hộ thân +1% ở lôi kiếp và thiên kiếp, tối đa 5%. Trước Kim Đan cả bộ sưu tập
cùng ra tay. Đeo, tháo ở Bảo khố: chạm một món.</p>

{h2('Bộ pháp bảo')}
<p>18 bộ, mỗi bộ bốn loại; mỗi loại thuộc đúng một bộ. Có đủ bốn loại thì bộ <b>kích hoạt mãi mãi</b> (kể cả khi sau này
bạn dùng món trong bộ để hợp luyện), lần đầu được thưởng thêm một khoáng thạch Địa phẩm và 50 LT. Danh sách đầy đủ ở Quyển XIII.</p>

{h2('Khí linh')}
<p>Pháp bảo có thể thức tỉnh khí linh với một trong tám danh: {e(', '.join(SPIRITS))}. Khí linh đến từ kỳ ngộ phẩm quý hoặc Khai Linh Đan. Món có khí linh được hợp luyện chừa lại; năm khí linh cho đạo hiệu «Khí Linh Chi Chủ».</p>

{h2('Hợp luyện')}
<p>Ba <b>món dư</b> cùng phẩm hợp thành <b>một khoáng thạch phẩm trên</b>, kèm linh thạch ({merge} LT). Món dư là mọi món
trừ một món giữ lại cho mỗi loại (món có khí linh, không thì món cũ nhất), nên hợp luyện không bao giờ làm bạn mất một loại.
Hỗn Độn phẩm là phẩm cao nhất, không hợp luyện được.</p>
{tip('Hợp luyện dồn hàng trùng Hoàng, Huyền phẩm lên phẩm cao. Với món trùng Âm Dương, Hỗn Độn phẩm, Luyện Hóa Phù có lời hơn: một món đổi một khoáng thạch cùng phẩm, thay vì một phần ba khoáng thạch.')}
</section>'''


def part5():
    item_cond = 'Đan phù chỉ rơi loại dùng được lúc đó: Tụ Linh Đan cần một ô Thiên phẩm trở lên đang luyện; Phá Cảnh Đan cần đã mở Kim Đan; Thọ Nguyên Đan cần đã mất tu vi hoặc đang bị thương kinh mạch; Hộ Lô Phù, Dẫn Lôi Phù cần lò Thiên phẩm hoặc đã mở Nguyên Anh.'
    gift = ' · '.join(f'{e(TIER_NAMES[t])} {str(GIFT_ODDS[t] / 10).replace(".", ",").replace(",0", "")}%' for t in range(6))
    return chapter('IX', 'Kỳ ngộ, linh thạch, bế quan, bí cảnh', 'Những dòng chảy phụ của tu hành: cơ duyên ngẫu nhiên, tiền tiêu, những buổi bế quan và cửa bí cảnh mỗi tuần.') + f'''
{h2('Kỳ ngộ', True)}
<p>Có <b>72 loại</b> kỳ ngộ theo độ hiếm: 36 thường gặp, 18 ít gặp, 9 hiếm gặp, 6 cực kỳ hiếm, 3 thiên cổ kỳ ngộ. Mỗi loại chờ một lúc riêng của việc đọc; tới lúc
đó thì gieo một lần trong ngày (mốc chữ thì mỗi lần chạm mốc). Mốc chữ là một số ngẫu nhiên từ <b>10 tới 50 vạn</b>, rút lại sau
mỗi lần chạm. Gặp kỳ ngộ thì hiện khung báo, kể cả khi đang đọc (tắt được ở Cài đặt › Tu tiên), và ghi vào Niên kỷ › Kỳ ngộ.</p>
<p>Càng hiếm thì thưởng càng quý: thường gặp cho phẩm thường, ít gặp phẩm khá, hiếm gặp phẩm quý, cực kỳ hiếm phẩm hiếm, thiên cổ phẩm
tuyệt thế kèm một món hiếm; 10% được lên một phẩm, 15% xuống một phẩm. Cột «mỗi lần» là tỉ lệ khi lúc ấy tới. Tỉ lệ gặp
nhân với bộ Cầm Tiêu Hợp Tấu ×1,5, Tầm Bảo Phù ×3, bế quan ×2, công pháp có đặc hiệu kỳ ngộ ×1,3; tối đa 95%.</p>
{kyngo_table()}
{table(['Phẩm', 'Phần thưởng'], [
    ['Thường', '15 – 40 linh thạch, hạt linh thảo, khoáng thạch Hoàng hay Huyền'],
    ['Khá', 'một đan dược hoặc phù chú, khoáng thạch Địa, linh thảo 100 – 200 năm, 60 – 120 linh thạch'],
    ['Quý', 'ngọc giản, đan phương, khí linh thức tỉnh, khoáng thạch Thiên, ngộ tính +2'],
    ['Hiếm', 'pháp bảo Địa hay Thiên phẩm, khoáng thạch Âm Dương, linh thảo 500 – 1.000 năm, ngộ tính +4 và 150 LT'],
    ['Tuyệt thế', 'pháp bảo Âm Dương hay Hỗn Độn, hoặc khoáng thạch Hỗn Độn; kèm ngộ tính +3'],
])}
<p class="small">Linh thạch nhân đôi ngày rằm, mồng một, nhân ba ba ngày Tết. Bộ Thất Bảo Tiên Quang: phẩm thường và khá có 15% thành
một pháp bảo Hoàng hay Huyền. {item_cond}</p>

{h2('Quà 10 vạn chữ')}
<p>Mỗi 10 vạn chữ (tính tổng mọi cuốn) được một khoáng thạch: {gift}. Bộ Vạn Kiếm Quy Tông gieo hai lần lấy phẩm cao;
Tụ Bảo Phù gieo ba lần.</p>

{h2('Linh thạch')}
{table(['Nguồn', 'Linh thạch'], [
    ['Mỗi vạn chữ đọc', '5 (×2 ngày rằm, mồng một; ×3 Tết)'],
    ['Bế quan viên mãn 30 / 45 / 60 / 90 / 120 / 180 phút', '15 / 25 / 40 / 65 / 100 / 170 (cũng nhân ngày lễ)'],
    ['Qua bí cảnh tuần', '20'],
    ['Đột phá đại cảnh giới', '50 × số thứ tự cảnh giới'],
    ['Vượt kiếp phá lô Thiên / Âm Dương / Hỗn Độn', '60 / 90 / 120'],
    ['Hợp luyện', '10 – 50 theo phẩm'],
    ['Gom đủ một bộ (lần đầu)', '50'],
    ['Kỳ ngộ', '20 – 80'],
])}

{h2('Bế quan')}
<p>Từ bản 2.6, tĩnh thất có sáu đẳng: Thạch Thất (30 phút), Mộc Thất (45), Ngọc Thất (60), Linh Thất (90), Tử Phủ (120), Thiên
Cung (180). Mỗi đẳng giữ được một độ dài bế quan; nâng đẳng bằng linh thạch (150, 400, 900, 1.800, 3.500). Trong lúc bế quan, mọi
phút đọc được cộng dần; đủ thì viên mãn, nhận linh thạch. Suốt buổi: <b>lò cháy ×1,5 ở Nhất đẳng, thêm ×0,1 mỗi đẳng, kỳ ngộ ×2</b>. Hủy giữa chừng chỉ mất phần thưởng. 20 lần viên mãn (hoặc tổng 100 giờ đọc) cho
đạo hiệu «Bế Quan Khổ Tu».</p>

{h2('Bí cảnh tuần')}
<p>Từ bản 2.3, bí cảnh là một bản đồ (Quyển V). Qua nút cuối là qua bí cảnh: một khoáng thạch từ <b>Huyền phẩm</b> trở lên và
20 LT. Bộ Thương Kích Liệt Trận rút bản đồ còn 4 nút; Phật Quang Phổ Chiếu nâng khoáng thạch một phẩm; Phong Thần Bảo Lục cho
thêm một khoáng thạch. Bí Cảnh Phù mở thêm một nút.</p>

{h2('Chuỗi ngày đọc')}
<p>Ngày nào có từ 1 phút đọc là giữ chuỗi. Phong Ấn Phù niêm một ngày lỡ trong bảy ngày qua; bộ Phật Môn Kim Cang tha một
ngày lỡ mỗi tuần. Chuỗi 7, 14, 30, 60, 100, 200, 365 ngày đều có đạo hiệu.</p>
</section>'''


def stage_yields():
    lengths = [10, 20, 30, 50, 80, 150]
    rows = []
    for t in range(6):
        rows.append([f'<b>{e(TIER_NAMES[t])}</b>'] + [str(L * 10000 // STAGE_WORDS[t]) or '0' for L in lengths])
    return table(['Chặng', *[f'{L} vạn' for L in lengths]], rows, 'yields')


def set_ranking():
    ranked = sorted(range(18), key=lambda s: (max(m // 12 for m in SETS[s]), sum(m // 12 for m in SETS[s])))
    out = []
    for s in ranked:
        top = max(m // 12 for m in SETS[s])
        out.append(f'<tr><td><b>{e(SET_NAMES[s])}</b></td><td>tới {e(TIER_NAMES[top])}</td><td>{e(SET_EFFECTS[s])}</td></tr>')
    return '<table class="rank"><thead><tr><th>Bộ</th><th>Cần</th><th>Hiệu quả</th></tr></thead><tbody>' + ''.join(out) + '</tbody></table>'


def part6():
    return chapter('X', 'Chiến lược cày', 'Luật thì ai cũng đọc được; đây là cách dùng luật cho lợi nhất. Mọi phép tính đều theo đúng con số của game.') + f'''
{h2('Mười điều tâm pháp', True)}
<ol class="tenets">
<li><b>Không để ô lò trống.</b> Mỗi chữ đốt cùng lúc cả sáu ô; một ô trống là một phần sáu sản lượng bỏ phí.</li>
<li><b>Lật trang, đừng kéo.</b> Chữ chỉ được tính khi lật tới; nhảy qua thì mất chữ.</li>
<li><b>Ba mươi phút mỗi ngày.</b> Đủ cho bí cảnh, kiếp phá lô Âm Dương, và giữ căn cơ 60 để không kẹt tiểu cảnh.</li>
<li><b>Ngày rằm, mồng một là ngày vàng.</b> Linh thạch ×2, Tết ×3; bế quan 120 phút + Thiên Hỏa Phù để lò cháy dồn.</li>
<li><b>Bồi Nguyên Đan vào ô có số chữ cần lớn nhất.</b> 10% của chặng Hỗn Độn là 13,5 vạn chữ, cho 50 LT.</li>
<li><b>Đè cảnh giới rồi mới phá.</b> Áp chế cộng tới 20%; một lần hỏng mất 2% linh lực, hai ngày hồi khí và thêm tâm ma.</li>
<li><b>Không để nhiều lò Thiên phẩm chín cùng lúc trước tuần bận.</b> Mỗi lò là mấy ngày phải đọc đủ phút.</li>
<li><b>Hợp luyện đều tay hàng Hoàng, Huyền dư</b>; hàng Âm Dương, Hỗn Độn trùng thì để Luyện Hóa Phù.</li>
<li><b>Tẩy Tủy Đan dành cho khoáng thạch phẩm cao nhất trong túi.</b> Âm Dương lên Hỗn Độn chênh 800 LT giá chợ.</li>
<li><b>Sách dài thì đặt trọn quyển.</b> Sách từ 40,5 vạn chữ cho thẳng một pháp bảo Âm Dương, từ 81 vạn là Hỗn Độn.</li>
</ol>

{h2('Lộ trình theo giai đoạn', True)}
<div class="stage"><b>Tân thủ · Luyện Khí → Kim Đan (0 – 30 vạn).</b> Đọc đều 30 phút. Để lò tự nạp 3 ô khoáng thạch; 3 ô
còn lại đặt cuốn đang đọc: chặng Hoàng (2,7 vạn) và chặng Huyền (6,75 vạn), cộng trọn quyển nếu sách dài. Mục tiêu: pháp bảo
đầu tiên, chuỗi 7 ngày, bí cảnh tuần đầu, bộ đầu tiên. Trúc Cơ Quan chỉ cần căn cơ 30; ngưng đan cần ba buổi liền 45 phút.</div>
<div class="stage"><b>Trung kỳ · Kim Đan → Hợp Thể (30 – 250 vạn).</b> Lôi kiếp kèm tâm ma kiếp: một cuốn, đọc tới nơi; Hóa Thần cần đọc rộng (ngộ đạo). Đẩy chặng
sách lên Địa (13,5 vạn) và Thiên (27 vạn); kiếp phá lô Thiên phẩm chỉ 2 ngày × 20 phút, rất dễ. Bắt đầu nhắm bộ: xem bảng bộ
theo độ khó ở cuối quyển này. Giữ sẵn 2 – 3 Định Thần Phù.</div>
<div class="stage"><b>Hậu kỳ · Hợp Thể → Phi Thăng (250 – 1.000 vạn).</b> Kiếp 50 – 60 phút, 6 – 7 ngày: dùng Phá Cảnh
Đan, Hộ Mạch Đan. Đặt chặng Âm Dương, Hỗn Độn rồi dồn Bồi Nguyên Đan, Tụ Linh Đan vào; dành linh thạch cho việc này.</div>
<div class="stage"><b>Tiên nhân (1.000 – 4.300 vạn).</b> Không còn thiên kiếp; chín phẩm tiên chỉ cần đọc. Mục tiêu dài hạn: đủ
72 loại, 18 bộ, 72 đạo hiệu.</div>

{h2('Xếp lò', True)}
<p><b>Sáu ô là sáu dây chuyền chạy song song.</b> Câu hỏi không phải «luyện gì trước» mà «ô nào cũng phải có việc».</p>
<ul>
<li><b>Túi còn khoáng thạch:</b> đặt hết vào ô trống. Khoáng thạch cháy bằng chữ của mọi cuốn, không lo đổi sách.</li>
<li><b>Túi rỗng:</b> đặt cuốn đang đọc vào ba chặng. Chặng phẩm thấp ra nhiều món (nuôi bộ, nuôi hợp luyện); chặng phẩm
cao ra ít nhưng quý. Chọn phẩm sao cho sách đủ dài, vì chặng dở dang mất khi đọc xong.</li>
<li><b>Trọn quyển</b> đáng giá khi sách dài: nó tiết kiệm đúng số chữ luyện một khoáng thạch cùng phẩm (Âm Dương 13,5 vạn,
Hỗn Độn 27 vạn). Sách ngắn thì để ô đó làm chặng, đọc xong vẫn được một khoáng thạch.</li>
</ul>
<p class="cap">Số pháp bảo một chặng cho ra theo độ dài sách</p>
{stage_yields()}
<p class="small">Ví dụ: cuốn 50 vạn chữ, ba ô Hoàng + Huyền + Địa cho 18 + 7 + 3 = 28 pháp bảo, rồi thêm một khoáng thạch
Âm Dương lúc đọc xong. Đặt trọn quyển thay chặng Địa: 25 món và một pháp bảo Âm Dương ra thẳng.</p>

{h2('Ngày vàng', True)}
<p>Một ngày rằm có bộ Tứ Hải Long Cung, bế quan 120 phút và một lá Thiên Hỏa Phù: mỗi chữ cháy như 6 chữ ở cả 6 ô, linh
thạch ×2, kỳ ngộ ×2. Thêm Tầm Bảo Phù thì mọi kỳ ngộ gặp gấp sáu (tối đa 95%): ngày ấy mà đọc liền 90 phút vào rằm tháng Tám là cơ duyên lớn nhất năm.
Chưa có bộ vẫn được ×3 lửa. Hãy để dành phù cho những ngày này.</p>

{h2('Tiêu linh thạch', True)}
{table(['Món', 'Giá', 'Đổi được'], [
    ['Bồi Nguyên Đan', '50', '10% một ô: 13,5 vạn chữ ở chặng Hỗn Độn'],
    ['Tụ Linh Đan', '120', '15% (30% với bộ) một ô Thiên+: 20 vạn chữ ở chặng Hỗn Độn'],
    ['Tẩy Tủy Đan', '120', 'khoáng thạch Âm Dương → Hỗn Độn (chênh 800)'],
    ['Thiên Hỏa Phù', '60', '2 vạn chữ ×2 ở cả 6 ô: 12 vạn chữ-ô'],
    ['Định Thần Phù', '60', '15 phút cứu một ngày độ kiếp'],
    ['Cửu Chuyển Kim Đan', '600', 'xong ngay một ô dưới Thiên; đắt hơn 10 Bồi Nguyên'],
    ['Khoáng thạch Hoàng', '30', '6 vạn chữ đọc mới đủ tiền; tự luyện rẻ hơn'],
], 'shop')}
<p>Thứ tự nên tiêu: <b>Bồi Nguyên Đan vào chặng phẩm cao</b> → Tẩy Tủy Đan khi có khoáng thạch Âm Dương → đồ hộ kiếp khi sắp
tới cửa → Thiên Hỏa Phù cho ngày vàng. Gần như không bao giờ nên mua khoáng thạch Hoàng, Huyền.</p>
{tip('Mười viên Bồi Nguyên Đan (500 LT) đẩy trọn một chặng Hỗn Độn: một pháp bảo Thần khí sau 4 ngày kiếp phá lô × 40 phút. 500 LT là khoảng 100 vạn chữ đọc, hoặc ít hơn nhiều nếu biết dùng ngày lễ và bế quan.', 'Đường tắt Hỗn Độn')}

{h2('Gom đủ 72 loại', True)}
<p>Loại rơi ngẫu nhiên đều trong 12 loại của phẩm, nên muốn đủ 12 loại một phẩm cần trung bình khoảng <b>37 món</b> phẩm đó
(món trùng không phí: đem hợp luyện). Phẩm thấp đầy nhanh; Âm Dương, Hỗn Độn là nút thắt, nên dồn chặng phẩm cao và đan dược
vào đó từ trung kỳ. Bộ nên nhắm theo phẩm cao nhất cần có:</p>
{set_ranking()}

{h2('Đạo hiệu dễ bỏ lỡ', True)}
<ul>
<li><b>Sinh Thần Đọc Sách</b>: nhập ngày sinh ở Cài đặt, rồi đọc 10 phút đúng ngày đó.</li>
<li><b>Tân Xuân Tu Đạo, Nguyệt Hạ Độc Thư</b>: đọc 10 phút trong ba ngày Tết, và đêm Trung Thu.</li>
<li><b>Sóc Vọng Tu Hành</b>: 12 ngày mồng một hay rằm có đọc từ 10 phút; ngày lễ còn cho linh thạch ×2.</li>
<li><b>Dạ Tu Giả, Thần Khởi Giả, Ngọ Thời Độc Thư, Hoàng Hôn Thư Khách</b>: đọc theo khung giờ, cộng dồn nhiều ngày.</li>
<li><b>Bại Nhi Bất Khuất, Vạn Kiếp Bất Ma</b>: chỉ có khi từng thất bại; đừng cố tình, chúng sẽ tự đến.</li>
</ul>
</section>'''


def part7():
    cards = []
    for t in range(6):
        cards.append(f'''<div class="ore">{img(f'art_ore_{t}', 'oimg')}<div><h3>{e(TIER_NAMES[t])} khoáng thạch</h3>
<p>Luyện: <b>{num(FORGE_WORDS[t])}</b> chữ → pháp bảo {e(CLANS[t].lower())}. Chặng sách: {num(STAGE_WORDS[t])} chữ.
Phường thị: <b>{num(ORE_PRICES[t])} LT</b>. Quà 10 vạn: {str(GIFT_ODDS[t] / 10).replace('.', ',').replace(',0', '')}%.
{'Phải qua kiếp phá lô.' if t >= 3 else ''}</p></div></div>''')
    return chapter('XI', 'Thư viện khoáng thạch', 'Sáu phẩm khoáng thạch, nguyên liệu của mọi pháp bảo.') + ''.join(cards) + f'''
<p class="small">Khoáng thạch đến từ: quà 10 vạn chữ, kỳ ngộ, bí cảnh tuần (từ Huyền phẩm), đọc xong sách không đặt trọn quyển,
thưởng gom bộ (Địa phẩm), hợp luyện, Luyện Hóa Phù, Tẩy Tủy Đan và Phường thị.</p>
</section>'''


def part8():
    pages = []
    for t in range(6):
        head = chapter('XII', 'Thư viện pháp bảo', '') if t == 0 else '<section class="page">'
        title = f'{TIER_NAMES[t]} · {CLANS[t]}'
        TOC.append((2, title, title))
        cells = []
        for k in range(12):
            s = t * 12 + k
            cells.append(f'<div class="pb">{img(f"badge_{s:02d}", "pbi")}<b>{e(NAMES[s])}</b><span>{e(SET_NAMES[set_of(s)])}</span></div>')
        first = t == 0
        pages.append(f'''{head}<h2 class="tierh">{e(title)}</h2>
<p class="tierp">Lò {e(TIER_NAMES[t])}: {van(FORGE_WORDS[t])} vạn chữ · chặng {van(STAGE_WORDS[t])} vạn{' · kiếp phá lô' if t >= 3 else ''}. Dòng nghiêng là bộ của món.</p>
<div class="pbgrid">{''.join(cells)}</div></section>''')
        if first:
            pass
    return ''.join(pages)


def part9():
    cards = []
    for s in range(18):
        mem = ''.join(f'<div class="sm">{img(f"badge_{m:02d}", "smi")}<b>{e(NAMES[m])}</b><span>{e(TIER_NAMES[m // 12])}</span></div>' for m in SETS[s])
        cards.append(f'''<div class="set"><div class="seth"><b>{s + 1}. {e(SET_NAMES[s])}</b><span>{e(SET_EFFECTS[s])}</span></div>
<div class="setm">{mem}</div><p>{e(SET_DETAILS[s])}</p></div>''')
    return chapter('XIII', '18 bộ pháp bảo', 'Gom đủ bốn loại là bộ kích hoạt mãi mãi, kèm một khoáng thạch Địa phẩm và 50 LT.') + ''.join(cards) + '</section>'


ITEM_TIPS = [
    'Rót vào ô có số chữ cần lớn nhất: chặng Hỗn Độn được 20 vạn chữ một viên (40 vạn với bộ Linh Đăng Chiếu Lộ).',
    'Dùng khi cả 6 ô đều đang cháy, tốt nhất trong buổi bế quan vào ngày rằm hay mồng một.',
    'Hai cách dùng: ở bình cảnh, lần đột phá kế tiếp +15% (hết sau lần bấm, thành hay hỏng); khi đang độ lôi kiếp, thiên kiếp, thêm một ngày. Dùng một lần là được đạo hiệu «Phá Cảnh Thần Tốc».',
    'Không cần bấm: nằm trong túi là tự cháy thay khi lò lỡ một ngày. Mang 1 – 2 lá khi có lò Thiên phẩm sắp chín.',
    'Món đáng mua nhất Phường thị. Rót vào chặng Âm Dương, Hỗn Độn: mỗi viên 6,75 – 13,5 vạn chữ.',
    'Dùng khi đã ở bình cảnh: 1 vạn tu vi này vào thẳng, không bị chia còn 20% như chữ đọc.',
    'Uống ngay đầu kỳ ngưng đan, lôi kiếp hay thiên kiếp nếu đã biết trước có ngày bận; chỉ dùng được khi đang độ.',
    'Dùng cho khoáng thạch phẩm cao nhất còn nằm trong túi; Âm Dương lên Hỗn Độn là lời nhất.',
    'Đặt vào chặng Địa phẩm vừa luyện lại từ đầu để được trọn 13,5 vạn chữ. Đừng mua: 10 Bồi Nguyên Đan rẻ hơn mà làm được hơn.',
    'Dành cho món bạn quý nhất, như một Thần khí: món có khí linh không bao giờ bị hợp luyện lấy đi.',
    'Chỉ rơi khi bạn đã mất tu vi (hụt khí) hay đang bị kinh mạch tổn thương: lấy lại một nửa phần đã mất, không mất gì thì chữa lành kinh mạch.',
    'Bật đầu buổi bế quan và trước một buổi đọc dài, một ngày lễ: gấp ba của phù nhân gấp đôi của bế quan.',
    'Dùng vào cuối tuần khi đã có 4 ngày; không dùng được khi bí cảnh tuần đã qua.',
    'Rẻ và cứu được cả kỳ kiếp; còn hạ tâm ma 10, nên đốt một lá trước khi đột phá. Không tính cho chuỗi ngày đọc hay bế quan.',
    'Dùng cho lò Hỗn Độn (4 ngày × 40 phút) để bớt một ngày phải đọc đủ phút.',
    'Niêm ngày lỡ gần nhất trong 7 ngày qua. Giữ cho các mốc chuỗi 30, 60, 100, 200, 365 ngày.',
    'Không mất hạn: lá phù chờ tới lần quà kế tiếp. Nhiều lá thì nhiều lần quà được gieo ba lần.',
    'Nung món trùng mới nhất thành khoáng thạch cùng phẩm. Dành cho Âm Dương, Hỗn Độn trùng; hàng thấp thì hợp luyện.',
]


def part10():
    head = chapter('XIV', 'Đan dược và phù chú', 'Mười tám món nhặt được từ kỳ ngộ hoặc mua ở Phường thị. Dùng trong Túi đồ.')
    order = [i for i in range(18) if ITEM_NAMES[i].endswith('Đan')] + [i for i in range(18) if not ITEM_NAMES[i].endswith('Đan')]
    cards = []
    for n, i in enumerate(order):
        if n == 0:
            cards.append(h2('Chín đan dược', True))
        if n == 9:
            cards.append(h2('Chín phù chú', True))
        kind = 'Đan dược' if ITEM_NAMES[i].endswith('Đan') else 'Phù chú'
        pct = round(ITEM_WEIGHTS[i] * 100 / WEIGHT_SUM)
        cards.append(f'''<div class="item">{img(f'art_item_{i}', 'ii')}<div class="ib"><div class="ih"><b>{e(ITEM_NAMES[i])}</b><span>{num(ITEM_PRICES[i])} LT</span></div>
<div class="im">{kind} · {rarity(ITEM_WEIGHTS[i])} (~{pct}% số lần rơi đan phù)</div>
<p>{e(ITEM_TEXTS[i])}</p><p class="it"><b>Dùng khéo:</b> {e(ITEM_TIPS[i])}</p></div></div>''')
    return head + ''.join(cards) + '</section>'


def part11():
    pages = []
    for g in range(6):
        head = chapter('XV', '72 đạo hiệu', 'Danh hiệu theo thói quen đọc; chọn một để mang trên Home.') if g == 0 else '<section class="page">'
        title = f'Đạo hiệu · {DAO_GROUPS[g]}'
        TOC.append((2, title, title))
        rows = ''.join(f'<tr><td><b>{e(DAO_NAMES[g * 12 + k])}</b></td><td>{e(DAO_RULES[g * 12 + k])}</td></tr>' for k in range(12))
        pages.append(f'{head}<h2 class="tierh">{e(title)}</h2><table class="dao"><tbody>{rows}</tbody></table></section>')
    return ''.join(pages)


def appendix():
    rows = [
        ['Chữ tối đa một trang', '450'],
        ['Đọc xong sách', '95%'],
        ['Linh thạch mỗi vạn chữ', '5 (lễ ×2, Tết ×3)'],
        ['Kỳ ngộ', '36 loại; mốc chữ ngẫu nhiên 10 – 50 vạn; bộ ×1,5, bế quan ×2, Tầm Bảo ×3'],
        ['Quà', '1 khoáng thạch mỗi 10 vạn chữ'],
        ['Lò', '6 ô; tự nạp 3 khoáng thạch; mỗi cuốn 3 ô'],
        ['Kiếp phá lô', 'Thiên 2×20′ · Âm Dương 3×30′ · Hỗn Độn 4×40′'],
        ['Tiểu cảnh', f'căn cơ {SUB_NEED[1]} / {SUB_NEED[2]} / {SUB_NEED[3]} để vào trung, hậu kỳ, đại viên mãn; từ Kim Đan phá tiểu cảnh'],
        ['Bình cảnh', '20% chữ thành linh lực, 80% thành áp chế (tối đa +20%)'],
        ['Đột phá', 'nền 40% … 15% + căn cơ/2 + tâm cảnh + áp chế − tâm ma/2'],
        ['Thiên kiếp', '4×35′ … 7×60′ (xem Quyển II)'],
        ['Bí cảnh', '5 ngày × 30′ trong tuần → khoáng thạch ≥ Huyền + 20 LT'],
        ['Bế quan', 'sáu đẳng 30 – 180′ → 15 – 170 LT; lò ×1,5 – ×2,0; kỳ ngộ ×2'],
        ['Hợp luyện', '3 món dư → 1 khoáng thạch phẩm trên'],
        ['Chuỗi ngày', 'từ 1 phút mỗi ngày'],
    ]
    ore = [[e(TIER_NAMES[t]), num(FORGE_WORDS[t]), num(STAGE_WORDS[t]), num(ORE_PRICES[t]), f'{10 * (t + 1)}' if t < 5 else '—'] for t in range(6)]
    return chapter('', 'Bảng tra nhanh', 'Mọi con số quan trọng, gom về một chỗ.') + table(['Mục', 'Số'], rows, 'quick') + \
        '<div class="keep"><p class="cap">Theo phẩm</p>' + table(['Phẩm', 'Khoáng thạch', 'Chặng', 'Giá', 'Hợp luyện LT'], ore, 'quick') + '</div>' + \
        '<p class="end">Đọc vạn quyển, tu thành tiên đồ.</p></section>'


CSS = '''
@font-face { font-family: B; src: url("file:///FONTS/Bitter-Regular.ttf"); font-weight: 400; }
@font-face { font-family: B; src: url("file:///FONTS/Bitter-Italic.ttf"); font-weight: 400; font-style: italic; }
@font-face { font-family: B; src: url("file:///FONTS/Bitter-SemiBold.ttf"); font-weight: 600; }
@font-face { font-family: B; src: url("file:///FONTS/Bitter-Bold.ttf"); font-weight: 700; }
@font-face { font-family: B; src: url("file:///FONTS/Bitter-ExtraBold.ttf"); font-weight: 800; }
@page { size: 91mm 123mm; margin: 5.5mm 5.5mm 8mm 5.5mm; }
* { box-sizing: border-box; -webkit-print-color-adjust: exact; print-color-adjust: exact; }
html { font-family: B, serif; font-size: 8.2pt; line-height: 1.36; color: #000; background: #fff; }
body { margin: 0; }
section { break-before: page; }
p { margin: 0 0 1.6mm; text-align: justify; hyphens: auto; }
ul, ol { margin: 0 0 1.8mm; padding-left: 4.2mm; }
li { margin-bottom: .7mm; }
b { font-weight: 700; }
h1 { font-size: 15pt; line-height: 1.12; margin: 0 0 2mm; font-weight: 800; }
h2 { font-size: 10pt; margin: 3.2mm 0 1.4mm; font-weight: 800; break-after: avoid; border-bottom: .25mm solid #000; padding-bottom: .6mm; }
h3 { font-size: 9pt; margin: 0 0 .6mm; }
.qno { font-size: 7pt; letter-spacing: .6mm; font-weight: 700; margin-top: 1mm; }
.qno::before { content: "◆ "; }
.lead { font-style: italic; border-left: .8mm solid #000; padding-left: 2mm; margin-bottom: 2.5mm; }
.small, .note { font-size: 7pt; line-height: 1.3; color: #222; }
.note { font-style: italic; }
.cap { font-weight: 700; font-size: 7.4pt; margin: 2mm 0 .8mm; }
table { width: 100%; border-collapse: collapse; margin: 0 0 2mm; font-size: 7.2pt; line-height: 1.25; }
th { text-align: left; font-weight: 700; border-bottom: .35mm solid #000; padding: .6mm .8mm; }
td { border-bottom: .15mm solid #999; padding: .7mm .8mm; vertical-align: middle; }
tr { break-inside: avoid; }
.tip { border: .3mm solid #000; border-radius: 1.6mm; padding: 1.6mm 2mm; margin: 2mm 0; font-size: 7.4pt; background: #eee; break-inside: avoid; }
/* cover */
.cover { text-align: center; padding-top: 5mm; break-before: auto; }
.cv-top { font-weight: 800; letter-spacing: 1mm; font-size: 8pt; }
.cv-logo { width: 38mm; height: 38mm; margin: 6mm auto 4mm; display: block; }
.cv-title { font-size: 25pt; font-weight: 800; line-height: 1.05; }
.cv-rule { width: 22mm; border-top: .5mm solid #000; margin: 4mm auto; }
.cv-sub { font-size: 7.6pt; padding: 0 4mm; line-height: 1.4; }
.cv-ver { margin-top: 7mm; font-size: 7pt; font-weight: 700; }
/* contents */
.toch { margin-top: 1mm; }
.toc1, .toc2 { display: flex; align-items: baseline; }
.toc1 { font-weight: 700; margin-top: 1.3mm; font-size: 8pt; }
.toc2 { padding-left: 3.5mm; font-size: 7.2pt; color: #222; }
.toc1 i, .toc2 i { flex: 1; border-bottom: .2mm dotted #000; margin: 0 1mm; }
.defs td:first-child { font-weight: 700; white-space: nowrap; }
/* flow */
.flow { border: .3mm solid #000; border-radius: 1.6mm; padding: 1.8mm; margin-bottom: 2mm; }
.fl-src { text-align: center; font-weight: 800; border: .5mm solid #000; border-radius: 1.4mm; margin: 0 auto 1.6mm; width: 30mm; padding: .8mm; background: #ddd; }
.fl-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 1.2mm; font-size: 6.8pt; line-height: 1.25; }
.fl-grid div { border: .2mm solid #000; border-radius: 1.2mm; padding: .8mm 1.2mm; }
.ri { width: 6.5mm; height: 6.5mm; display: block; }
.realms td { padding: .4mm .6mm; }
.oi { width: 6mm; height: 6mm; display: block; }
.ore { display: flex; gap: 2.5mm; align-items: center; border: .3mm solid #000; border-radius: 1.6mm; padding: 1.4mm 2mm; margin-bottom: 1.6mm; break-inside: avoid; }
.ore p { font-size: 7.2pt; margin: 0; text-align: left; }
.oimg { width: 12mm; height: 12mm; flex: none; }
.stage { border-left: .8mm solid #000; padding: .4mm 0 .4mm 2mm; margin-bottom: 2mm; text-align: justify; break-inside: avoid; }
.tenets li { margin-bottom: 1mm; }
.yields td, .yields th { text-align: center; }
.yields td:first-child, .yields th:first-child { text-align: left; }
.rank td:nth-child(2) { white-space: nowrap; }
/* treasures */
.tierh { margin-top: 1mm; }
.tierp { font-size: 7pt; margin-bottom: 2mm; }
.pbgrid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 1.4mm 1.2mm; }
.pb { text-align: center; border: .25mm solid #000; border-radius: 1.4mm; padding: 1mm .6mm; height: 27.5mm; }
.pbi { width: 13mm; height: 13mm; display: block; margin: 0 auto .6mm; }
.pb b { display: block; font-size: 6.6pt; line-height: 1.15; }
.pb span { display: block; font-size: 5.4pt; font-style: italic; line-height: 1.15; margin-top: .4mm; color: #333; }
.chapter .pbgrid .pb { height: 24mm; }
.chapter .pbgrid .pbi { width: 11mm; height: 11mm; }
/* sets */
.set { border: .3mm solid #000; border-radius: 1.6mm; padding: 1.4mm 1.8mm; margin-bottom: 1.8mm; break-inside: avoid; }
.seth b { display: block; font-size: 8.4pt; }
.seth span { display: block; font-size: 7pt; font-weight: 700; font-style: italic; }
.setm { display: grid; grid-template-columns: repeat(4, 1fr); gap: 1mm; margin: 1mm 0; }
.sm { text-align: center; }
.smi { width: 9mm; height: 9mm; display: block; margin: 0 auto .3mm; }
.sm b { display: block; font-size: 5.8pt; line-height: 1.1; }
.sm span { display: block; font-size: 5.2pt; color: #333; }
.set p { font-size: 6.8pt; margin: 0; line-height: 1.25; }
/* items */
.item { display: flex; gap: 2mm; border: .3mm solid #000; border-radius: 1.6mm; padding: 1.4mm 1.8mm; margin-bottom: 1.6mm; break-inside: avoid; }
.ii { width: 11mm; height: 11mm; flex: none; }
.ib { flex: 1; }
.ih { display: flex; justify-content: space-between; font-size: 8.4pt; }
.ih span { font-weight: 700; font-size: 7pt; }
.im { font-size: 6.4pt; font-style: italic; margin-bottom: .6mm; }
.item p { font-size: 6.9pt; line-height: 1.25; margin: 0 0 .5mm; text-align: left; }
.it { color: #111; }
/* đạo hiệu */
.dao td { padding: 1.05mm .8mm; font-size: 7pt; }
.dao td:first-child { width: 38%; }
.quick td:first-child { font-weight: 700; }
.keep { break-inside: avoid; }
.end { text-align: center; font-style: italic; margin-top: 4mm; }
'''.replace('FONTS', FONTS.replace('\\', '/'))


def build_html(numbers=None):
    TOC.clear()
    body = [cover(), None, intro(), part1(), part2(), part2b(), part2c(), part2d(), part2e(), part3(), part4(), part5(), part6(), part7(), part8(), part9(), part10(), part11(), appendix()]
    body[1] = contents(numbers)
    return f'<!doctype html><html lang="vi"><head><meta charset="utf-8"><title>Cẩm nang tu tiên</title><style>{CSS}</style></head><body>{"".join(body)}</body></html>'


def print_pdf(html_text, pdf):
    os.makedirs(OUT_DIR, exist_ok=True)
    src = os.path.join(OUT_DIR, 'cam-nang.html')
    open(src, 'w', encoding='utf-8').write(html_text)
    profile = os.path.join(OUT_DIR, 'edge-profile')
    subprocess.run([EDGE, '--headless=new', '--disable-gpu', '--no-first-run', f'--user-data-dir={profile}',
                    '--no-pdf-header-footer', '--generate-pdf-document-outline', f'--print-to-pdf={pdf}',
                    'file:///' + src.replace('\\', '/')], check=True, capture_output=True, timeout=180)


def norm(s):
    return re.sub(r'\s+', ' ', unicodedata.normalize('NFC', s))


def find_pages(pdf):
    doc = pymupdf.open(pdf)
    texts = [norm(p.get_text()) for p in doc]
    start = 2  # past the cover and the contents
    while start < len(texts) and 'Đọc cẩm nang này thế nào' not in texts[start]:
        start += 1
    found = []
    for _, _, key in TOC:
        k = norm(key)
        page = next((i for i in range(start, len(texts)) if k in texts[i]), None)
        assert page is not None, key
        found.append(page)
        start = page
    return found


def main():
    os.makedirs(OUT_DIR, exist_ok=True)
    draft = os.path.join(OUT_DIR, 'draft.pdf')
    print_pdf(build_html(), draft)
    pages = find_pages(draft)
    print_pdf(build_html([p + 1 for p in pages]), draft)
    again = find_pages(draft)
    assert again == pages, 'the contents moved the pages'

    doc = pymupdf.open(draft)
    font = pymupdf.Font(fontfile=os.path.join(FONTS, 'Bitter-Regular.ttf'))
    for i, page in enumerate(doc):
        if i == 0:
            continue
        page.wrap_contents()
        w, h = page.rect.width, page.rect.height
        tw = pymupdf.TextWriter(page.rect)
        y = h - 9.5
        left = 'Vạn Quyển Tiên Đồ · Cẩm nang tu tiên'
        tw.append((15.6, y), left, font=font, fontsize=5.6)
        n = str(i + 1)
        tw.append((w - 15.6 - font.text_length(n, 5.6), y), n, font=font, fontsize=5.6)
        tw.write_text(page, color=(0.25, 0.25, 0.25))
        page.draw_line((15.6, h - 15), (w - 15.6, h - 15), color=(0.6, 0.6, 0.6), width=0.3)
    doc.set_toc([[lvl, title, pages[i] + 1] for i, (lvl, title, _) in enumerate(TOC)])
    doc.set_metadata({'title': 'Vạn Quyển Tiên Đồ · Cẩm nang tu tiên', 'author': 'Vạn Quyển Tiên Đồ', 'subject': f'Bản {VERSION}'})
    out = os.path.join(ROOT, 'dist', f'Van-Quyen-Tien-Do-{VERSION}-Cam-nang.pdf')
    doc.save(out, garbage=4, deflate=True)
    print(out, doc.page_count, 'pages')


if __name__ == '__main__':
    sys.stdout.reconfigure(encoding='utf-8')
    main()
