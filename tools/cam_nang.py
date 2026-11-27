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
<li><b>Quyển I – V: luật chơi</b>, mọi con số lấy thẳng từ mã nguồn bản này.</li>
<li><b>Quyển VI: chiến lược cày</b>: tâm pháp, lộ trình, xếp lò, tiêu linh thạch.</li>
<li><b>Quyển VII – XI: thư viện</b> khoáng thạch, pháp bảo, bộ, đan phù, đạo hiệu; cuối sách là bảng tra nhanh.</li>
</ul>
<h2>Vài chữ dùng trong sách</h2>
<table class="defs"><tbody>
<tr><td>vạn</td><td>10.000 chữ. «2,7 vạn» là 27.000 chữ.</td></tr>
<tr><td>tu vi</td><td>số chữ đã đọc được tính, cộng thưởng; quyết định cảnh giới.</td></tr>
<tr><td>phẩm</td><td>bậc của khoáng thạch và pháp bảo: Hoàng, Huyền, Địa, Thiên, Âm Dương, Hỗn Độn.</td></tr>
<tr><td>ô lò</td><td>một trong 6 chỗ của lò luyện.</td></tr>
<tr><td>LT</td><td>linh thạch, tiền của giới tu tiên; tiêu ở Phường thị.</td></tr>
<tr><td>ngày lễ</td><td>mồng một, ngày rằm âm lịch và ba ngày Tết.</td></tr>
<tr><td>bộ</td><td>bốn loại pháp bảo; gom đủ thì có hiệu quả mãi mãi.</td></tr>
</tbody></table>
<p class="note">Các tỉ lệ (kỳ ngộ, quà, loại pháp bảo rơi ra) là ngẫu nhiên: con số trong sách là trung bình.</p>
</section>'''


def part1():
    return chapter('I', 'Nhập môn: đọc là tu', 'Mọi thứ trong game đều sinh ra từ hai đại lượng: <b>số chữ</b> bạn đọc tiến lên và <b>số phút</b> bạn đọc mỗi ngày.') + f'''
{h2('Vòng tu luyện')}
<div class="flow">
<div class="fl-src">Đọc sách<br><small>chữ · phút</small></div>
<div class="fl-grid">
<div><b>Tu vi</b><br>cảnh giới, đạo hiệu</div>
<div><b>Lò luyện</b><br>khoáng thạch → pháp bảo</div>
<div><b>Kỳ ngộ</b><br>mỗi vạn chữ một lần gieo</div>
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

{h2('Tầng công pháp')}
<p>Mỗi cuốn sách là một bộ công pháp chín tầng: tiến độ chia đều từ Nhất tầng tới Cửu tầng, đọc xong là <b>Đại viên mãn</b>.
Mỗi lần lên tầng được đếm cho đạo hiệu «Bách Tầng Công Pháp» và «Tầng Tầng Đột Phá».</p>

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
    rows = []
    for r in range(10):
        floor = f'{num(FLOOR_VAN[r])} vạn' if r < 9 else f'{num(TIEN_VAN[0])} vạn'
        if r == 0:
            gate, need = '—', 'bắt đầu'
        else:
            gate = e(GATES[r])
            need = f'{TRIAL_DAYS[r - 1]} ngày × {TRIAL_MINUTES[r - 1]} phút'
        rows.append([img(f'realm_{r}', 'ri'), f'<b>{e(REALM_NAMES[r])}</b>', floor, gate, need, f'{50 * r} LT' if r else '—'])
    realm_table = table(['', 'Cảnh giới', 'Từ', 'Cửa thiên kiếp', 'Phải đọc liền', 'Thưởng'], rows, 'realms')
    pham = ' · '.join(f'{e(PHAM[i])} {num(TIEN_VAN[i])}' for i in range(9))
    return chapter('II', 'Cảnh giới và thiên kiếp', 'Tu vi đưa bạn qua chín đại cảnh giới tới Tiên nhân; mỗi đại cảnh giới có một cửa thiên kiếp chặn đường.') + f'''
{h2('Mười cảnh giới')}
{realm_table}
<p class="small">Mỗi đại cảnh giới chia đều ba kỳ: sơ kỳ, trung kỳ, hậu kỳ (36 cấp tính cả Tiên nhân). Tiên nhân có chín phẩm
(vạn chữ): {pham}. Từ Tiên nhân không còn thiên kiếp.</p>

{h2('Độ kiếp diễn ra thế nào')}
<ol>
<li>Tu vi chạm ngưỡng cảnh giới mới thì thiên kiếp bắt đầu ngay hôm đó. Cảnh giới bị giữ ở hậu kỳ cảnh giới cũ,
nhưng <b>chữ vẫn cộng vào tu vi</b>: vượt kiếp xong có thể nhảy liền mấy cấp.</li>
<li>Phải có đủ số ngày đạt số phút của cửa kiếp. Ngày đầu tiên và hôm nay chỉ được tính khi đã đủ phút, không bị phạt khi thiếu.</li>
<li>Một <b>ngày ở giữa thiếu phút</b> là độ kiếp thất bại: tu vi lui về một phần chín dưới cửa (vẫn ở hậu kỳ), phần mất
được ghi sổ. Kiếp sẽ bắt đầu lại khi tu vi chạm ngưỡng lần nữa.</li>
<li>Đủ ngày: cảnh giới mở, nhận <b>50 LT × số thứ tự cảnh giới</b> (Trúc Cơ 50, Kim Đan 100… Tiên nhân 450).</li>
</ol>

{h2('Đồ trợ kiếp')}
{table(['Trợ lực', 'Công dụng'], [
    ['Hộ Mạch Đan', 'kỳ kiếp đang độ được lỡ thêm một ngày'],
    ['Phá Cảnh Đan', 'tính thêm một ngày đã vượt (từ cửa Phá Đan Thành Anh)'],
    ['Định Thần Phù', 'hôm nay được tính thêm 15 phút'],
    ['Thọ Nguyên Đan', 'sau thất bại: lấy lại một nửa tu vi đã mất'],
    ['Bộ Đạo Môn Tứ Bảo', 'mỗi kỳ kiếp được lỡ một ngày'],
    ['Bộ Thất Tinh Bắc Đẩu', 'mỗi ngày độ kiếp cần ít hơn 5 phút'],
])}
{tip('Trang Tu vi ghi số chữ còn tới cửa kiếp. Khi còn dưới 2 vạn, hãy nhìn lịch tuần tới: nếu có mấy ngày bận, mua sẵn Định Thần Phù (60 LT) và Hộ Mạch Đan (250 LT), hoặc giữ lại Ngưng Thần Đan chưa dùng để khỏi bị đẩy vào kiếp sớm.')}
</section>'''


def part3():
    rows = []
    for t in range(6):
        lo = [0, 40000, 108000, 202000, 405000, 810000][t]
        hi = [40000, 108000, 202000, 405000, 810000, None][t]
        length = f'dưới {van(hi)} vạn' if t == 0 else (f'{van(lo)} – {van(hi)} vạn' if hi else f'từ {van(lo)} vạn')
        rows.append([img(f'art_ore_{t}', 'oi'), f'<b>{e(TIER_NAMES[t])}</b>', num(FORGE_WORDS[t]), num(STAGE_WORDS[t]), length])
    kiep = [[f'<b>{e(TIER_NAMES[t])}</b>', f'{max(2, min(4, t - 1))} ngày', f'{10 * max(2, min(4, t - 1))} phút', f'{30 * (t - 1)} LT'] for t in (3, 4, 5)]
    return chapter('III', 'Lò luyện', 'Sáu ô lò cháy cùng lúc với mỗi chữ bạn đọc. Xếp lò khéo hay vụng quyết định bạn có bao nhiêu pháp bảo.') + f'''
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
    ['Bế quan', '×1,5', 'tới khi bế quan viên mãn'],
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
    return chapter('IV', 'Pháp bảo: bộ, khí linh, hợp luyện', '72 loại pháp bảo, sáu phẩm, mười tám bộ. Đây là thứ để sưu tầm, và cũng là chìa khóa của những hiệu quả mạnh nhất.') + f'''
{h2('Sáu phẩm, sáu tộc')}
<table class="clans"><thead><tr><th>Phẩm</th><th>Tộc</th><th>Ví dụ</th></tr></thead><tbody>{clans}</tbody></table>
<p>Mỗi phẩm có 12 loại. Pháp bảo ra lò thuộc một loại <b>ngẫu nhiên</b> trong 12 loại của phẩm đó, mang thêm một chữ «chi»
đứng đầu tên (Hỗn, Nguyên, Long, Phượng…) cho có cá tính: «Long Đơn đao», «Minh Hồ lô».</p>

{h2('Bộ pháp bảo')}
<p>18 bộ, mỗi bộ bốn loại; mỗi loại thuộc đúng một bộ. Có đủ bốn loại thì bộ <b>kích hoạt mãi mãi</b> (kể cả khi sau này
bạn dùng món trong bộ để hợp luyện), lần đầu được thưởng thêm một khoáng thạch Địa phẩm và 50 LT. Danh sách đầy đủ ở Quyển IX.</p>

{h2('Khí linh')}
<p>Pháp bảo có thể thức tỉnh khí linh với một trong tám danh: {e(', '.join(SPIRITS))}. Khí linh đến từ kỳ ngộ (khoảng 6%
mỗi lần) hoặc Khai Linh Đan. Món có khí linh được hợp luyện chừa lại; năm khí linh cho đạo hiệu «Khí Linh Chi Chủ».</p>

{h2('Hợp luyện')}
<p>Ba <b>món dư</b> cùng phẩm hợp thành <b>một khoáng thạch phẩm trên</b>, kèm linh thạch ({merge} LT). Món dư là mọi món
trừ một món giữ lại cho mỗi loại (món có khí linh, không thì món cũ nhất), nên hợp luyện không bao giờ làm bạn mất một loại.
Hỗn Độn phẩm là phẩm cao nhất, không hợp luyện được.</p>
{tip('Hợp luyện dồn hàng trùng Hoàng, Huyền phẩm lên phẩm cao. Với món trùng Âm Dương, Hỗn Độn phẩm, Luyện Hóa Phù có lời hơn: một món đổi một khoáng thạch cùng phẩm, thay vì một phần ba khoáng thạch.')}
</section>'''


def part5():
    item_cond = 'Đan phù chỉ rơi loại dùng được lúc đó: Tụ Linh Đan cần một ô Thiên phẩm trở lên đang luyện; Phá Cảnh Đan cần đã mở Kim Đan; Thọ Nguyên Đan cần đã mất tu vi; Hộ Lô Phù, Dẫn Lôi Phù cần lò Thiên phẩm hoặc đã mở Nguyên Anh.'
    gift = ' · '.join(f'{e(TIER_NAMES[t])} {str(GIFT_ODDS[t] / 10).replace(".", ",").replace(",0", "")}%' for t in range(6))
    return chapter('V', 'Kỳ ngộ, linh thạch, bế quan, bí cảnh', 'Những dòng chảy phụ của tu hành: cơ duyên ngẫu nhiên, tiền tiêu, những buổi bế quan và cửa bí cảnh mỗi tuần.') + f'''
{h2('Kỳ ngộ')}
<p>Cứ mỗi vạn chữ là một lần gieo kỳ ngộ, cơ hội <b>12%</b>. Bộ Cầm Tiêu Hợp Tấu nâng lên 18%; Tầm Bảo Phù ×3 và bế quan ×2
nhân thêm. Khi gặp:</p>
{table(['Kết quả', 'Tỉ lệ'], [
    ['Khoáng thạch (phẩm gieo như quà 10 vạn)', '22%'],
    ['20 – 80 linh thạch (×2 ngày rằm, mồng một; ×3 Tết)', '10%'],
    ['Khí linh của một pháp bảo thức tỉnh', '6%'],
    ['Một đan dược hoặc phù chú', 'phần còn lại, ~62%'],
])}
<p class="small">Bộ Thất Bảo Tiên Quang: 6% đầu thành một pháp bảo Hoàng hay Huyền phẩm (phần khoáng thạch còn 16%). {item_cond}</p>

{h2('Quà 10 vạn chữ')}
<p>Mỗi 10 vạn chữ (tính tổng mọi cuốn) được một khoáng thạch: {gift}. Bộ Vạn Kiếm Quy Tông gieo hai lần lấy phẩm cao;
Tụ Bảo Phù gieo ba lần.</p>

{h2('Linh thạch')}
{table(['Nguồn', 'Linh thạch'], [
    ['Mỗi vạn chữ đọc', '5 (×2 ngày rằm, mồng một; ×3 Tết)'],
    ['Bế quan viên mãn 30 / 60 / 90 / 120 phút', '15 / 35 / 60 / 90 (cũng nhân ngày lễ)'],
    ['Qua bí cảnh tuần', '20'],
    ['Vượt thiên kiếp', '50 × số thứ tự cảnh giới'],
    ['Vượt kiếp phá lô Thiên / Âm Dương / Hỗn Độn', '60 / 90 / 120'],
    ['Hợp luyện', '10 – 50 theo phẩm'],
    ['Gom đủ một bộ (lần đầu)', '50'],
    ['Kỳ ngộ', '20 – 80'],
])}

{h2('Bế quan')}
<p>Chọn 30, 60, 90 hoặc 120 phút. Trong lúc bế quan, mọi phút đọc được cộng dần; đủ thì bế quan viên mãn, nhận linh thạch.
Suốt buổi: <b>lò cháy ×1,5, kỳ ngộ ×2</b>. Hủy giữa chừng chỉ mất phần thưởng. 20 lần viên mãn (hoặc tổng 100 giờ đọc) cho
đạo hiệu «Bế Quan Khổ Tu».</p>

{h2('Bí cảnh tuần')}
<p>Tuần tính từ thứ Hai tới Chủ nhật. Có <b>5 ngày đọc từ 30 phút</b> là qua bí cảnh: một khoáng thạch từ <b>Huyền phẩm</b>
trở lên và 20 LT. Bộ Thương Kích Liệt Trận chỉ cần 4 ngày; Phật Quang Phổ Chiếu nâng khoáng thạch một phẩm; Phong Thần Bảo
Lục cho thêm một khoáng thạch. Bí Cảnh Phù tính thêm một ngày.</p>

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
    return chapter('VI', 'Chiến lược cày', 'Luật thì ai cũng đọc được; đây là cách dùng luật cho lợi nhất. Mọi phép tính đều theo đúng con số của game.') + f'''
{h2('Mười điều tâm pháp', True)}
<ol class="tenets">
<li><b>Không để ô lò trống.</b> Mỗi chữ đốt cùng lúc cả sáu ô; một ô trống là một phần sáu sản lượng bỏ phí.</li>
<li><b>Lật trang, đừng kéo.</b> Chữ chỉ được tính khi lật tới; nhảy qua thì mất chữ.</li>
<li><b>Ba mươi phút, năm ngày một tuần.</b> Một nhịp đủ cho bí cảnh, cho cửa kiếp đầu và cho kiếp phá lô Âm Dương.</li>
<li><b>Ngày rằm, mồng một là ngày vàng.</b> Linh thạch ×2, Tết ×3; bế quan 120 phút + Thiên Hỏa Phù để lò cháy dồn.</li>
<li><b>Bồi Nguyên Đan vào ô có số chữ cần lớn nhất.</b> 10% của chặng Hỗn Độn là 13,5 vạn chữ, cho 50 LT.</li>
<li><b>Tích đồ hộ thân trước cửa kiếp.</b> Một lần thất bại mất cả một phần chín cảnh giới và mấy ngày công.</li>
<li><b>Không để nhiều lò Thiên phẩm chín cùng lúc trước tuần bận.</b> Mỗi lò là mấy ngày phải đọc đủ phút.</li>
<li><b>Hợp luyện đều tay hàng Hoàng, Huyền dư</b>; hàng Âm Dương, Hỗn Độn trùng thì để Luyện Hóa Phù.</li>
<li><b>Tẩy Tủy Đan dành cho khoáng thạch phẩm cao nhất trong túi.</b> Âm Dương lên Hỗn Độn chênh 800 LT giá chợ.</li>
<li><b>Sách dài thì đặt trọn quyển.</b> Sách từ 40,5 vạn chữ cho thẳng một pháp bảo Âm Dương, từ 81 vạn là Hỗn Độn.</li>
</ol>

{h2('Lộ trình theo giai đoạn', True)}
<div class="stage"><b>Tân thủ · Luyện Khí → Kim Đan (0 – 30 vạn).</b> Đọc đều 30 phút. Để lò tự nạp 3 ô khoáng thạch; 3 ô
còn lại đặt cuốn đang đọc: chặng Hoàng (2,7 vạn) và chặng Huyền (6,75 vạn), cộng trọn quyển nếu sách dài. Mục tiêu: pháp bảo
đầu tiên, chuỗi 7 ngày, bí cảnh tuần đầu, bộ đầu tiên. Hai cửa kiếp đầu chỉ 3 ngày × 30 phút.</div>
<div class="stage"><b>Trung kỳ · Kim Đan → Hợp Thể (30 – 250 vạn).</b> Cửa kiếp dài dần (35 – 45 phút, 4 – 5 ngày). Đẩy chặng
sách lên Địa (13,5 vạn) và Thiên (27 vạn); kiếp phá lô Thiên phẩm chỉ 2 ngày × 20 phút, rất dễ. Bắt đầu nhắm bộ: xem bảng bộ
theo độ khó ở cuối quyển này. Giữ sẵn 2 – 3 Định Thần Phù.</div>
<div class="stage"><b>Hậu kỳ · Hợp Thể → Độ Kiếp Phi Thăng (250 – 1.000 vạn).</b> Kiếp 50 – 60 phút, 6 – 7 ngày: dùng Phá Cảnh
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
thạch ×2, kỳ ngộ ×2. Thêm Tầm Bảo Phù thì mỗi vạn chữ có <b>72%</b> gặp kỳ ngộ (108%, tức chắc chắn, với Cầm Tiêu Hợp Tấu).
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
    return chapter('VII', 'Thư viện khoáng thạch', 'Sáu phẩm khoáng thạch, nguyên liệu của mọi pháp bảo.') + ''.join(cards) + f'''
<p class="small">Khoáng thạch đến từ: quà 10 vạn chữ, kỳ ngộ, bí cảnh tuần (từ Huyền phẩm), đọc xong sách không đặt trọn quyển,
thưởng gom bộ (Địa phẩm), hợp luyện, Luyện Hóa Phù, Tẩy Tủy Đan và Phường thị.</p>
</section>'''


def part8():
    pages = []
    for t in range(6):
        head = chapter('VIII', 'Thư viện pháp bảo', '') if t == 0 else '<section class="page">'
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
    return chapter('IX', '18 bộ pháp bảo', 'Gom đủ bốn loại là bộ kích hoạt mãi mãi, kèm một khoáng thạch Địa phẩm và 50 LT.') + ''.join(cards) + '</section>'


ITEM_TIPS = [
    'Rót vào ô có số chữ cần lớn nhất: chặng Hỗn Độn được 20 vạn chữ một viên (40 vạn với bộ Linh Đăng Chiếu Lộ).',
    'Dùng khi cả 6 ô đều đang cháy, tốt nhất trong buổi bế quan vào ngày rằm hay mồng một.',
    'Chỉ dùng khi đang độ kiếp, từ cửa Phá Đan Thành Anh trở lên; dành cho kiếp dài (Phi Thăng 7 ngày × 60 phút). Dùng một lần là được đạo hiệu «Phá Cảnh Thần Tốc».',
    'Không cần bấm: nằm trong túi là tự cháy thay khi lò lỡ một ngày. Mang 1 – 2 lá khi có lò Thiên phẩm sắp chín.',
    'Món đáng mua nhất Phường thị. Rót vào chặng Âm Dương, Hỗn Độn: mỗi viên 6,75 – 13,5 vạn chữ.',
    'Cẩn thận khi gần cửa kiếp: thêm 1 vạn tu vi có thể kéo bạn vào thiên kiếp sớm hơn dự tính. Dùng ngay sau khi vượt kiếp.',
    'Uống ngay đầu kỳ kiếp nếu đã biết trước có ngày bận; chỉ dùng được khi đang độ kiếp.',
    'Dùng cho khoáng thạch phẩm cao nhất còn nằm trong túi; Âm Dương lên Hỗn Độn là lời nhất.',
    'Đặt vào chặng Địa phẩm vừa luyện lại từ đầu để được trọn 13,5 vạn chữ. Đừng mua: 10 Bồi Nguyên Đan rẻ hơn mà làm được hơn.',
    'Dành cho món bạn quý nhất, như một Thần khí: món có khí linh không bao giờ bị hợp luyện lấy đi.',
    'Chỉ rơi khi bạn đã mất tu vi vì độ kiếp thất bại. Uống trước khi chạm lại cửa kiếp để lấy lại một nửa phần đã mất.',
    'Bật đầu buổi bế quan: gấp ba của phù nhân gấp đôi của bế quan, mỗi vạn chữ có 72% gặp kỳ ngộ.',
    'Dùng vào cuối tuần khi đã có 4 ngày; không dùng được khi bí cảnh tuần đã qua.',
    'Rẻ và cứu được cả kỳ kiếp. Giữ sẵn 2 – 3 lá. Không tính cho chuỗi ngày đọc hay bế quan.',
    'Dùng cho lò Hỗn Độn (4 ngày × 40 phút) để bớt một ngày phải đọc đủ phút.',
    'Niêm ngày lỡ gần nhất trong 7 ngày qua. Giữ cho các mốc chuỗi 30, 60, 100, 200, 365 ngày.',
    'Không mất hạn: lá phù chờ tới lần quà kế tiếp. Nhiều lá thì nhiều lần quà được gieo ba lần.',
    'Nung món trùng mới nhất thành khoáng thạch cùng phẩm. Dành cho Âm Dương, Hỗn Độn trùng; hàng thấp thì hợp luyện.',
]


def part10():
    head = chapter('X', 'Đan dược và phù chú', 'Mười tám món nhặt được từ kỳ ngộ hoặc mua ở Phường thị. Dùng trong Túi đồ.')
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
        head = chapter('XI', '72 đạo hiệu', 'Danh hiệu theo thói quen đọc; chọn một để mang trên Home.') if g == 0 else '<section class="page">'
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
        ['Kỳ ngộ mỗi vạn chữ', '12% (bộ 18%; bế quan ×2; Tầm Bảo ×3)'],
        ['Quà', '1 khoáng thạch mỗi 10 vạn chữ'],
        ['Lò', '6 ô; tự nạp 3 khoáng thạch; mỗi cuốn 3 ô'],
        ['Kiếp phá lô', 'Thiên 2×20′ · Âm Dương 3×30′ · Hỗn Độn 4×40′'],
        ['Thiên kiếp', '3×30′ … 7×60′ (xem Quyển II)'],
        ['Bí cảnh', '5 ngày × 30′ trong tuần → khoáng thạch ≥ Huyền + 20 LT'],
        ['Bế quan', '30/60/90/120′ → 15/35/60/90 LT; lò ×1,5; kỳ ngộ ×2'],
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
    body = [cover(), None, intro(), part1(), part2(), part3(), part4(), part5(), part6(), part7(), part8(), part9(), part10(), part11(), appendix()]
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
