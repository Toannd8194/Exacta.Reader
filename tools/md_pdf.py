"""
A Markdown document as a PDF the size of the iReader Neo 3's screen, in the style of the Cẩm nang
(tools/cam_nang.py). Handles the Markdown the docs use: # ## ### headings, paragraphs, - and 1.
lists, | tables |, **bold**, *italic*. The first # heading and the italic line under it make the
cover; each ## starts a page and goes in the contents.

    python tools/md_pdf.py docs/Chien-luoc-nang-cap-tu-tien.md
"""
import html
import os
import re
import sys

import pymupdf

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import cam_nang as cn  # noqa: E402  (the style, the printer and the footer)


def inline(s):
    s = html.escape(s, quote=False)
    s = re.sub(r'\*\*(.+?)\*\*', r'<b>\1</b>', s)
    s = re.sub(r'(?<![\w*])\*(?!\s)(.+?)(?<!\s)\*(?![\w*])', r'<i>\1</i>', s)
    return s


def convert(md):
    """(html body, the ## headings in order)."""
    lines = md.splitlines()
    out, heads = [], []
    title = sub = ''
    i = 0
    para = []

    def flush():
        if para:
            out.append('<p>' + inline(' '.join(para)) + '</p>')
            para.clear()

    while i < len(lines):
        line = lines[i].rstrip()
        if not line.strip():
            flush()
            i += 1
            continue
        if line.startswith('# ') and not title:
            title = line[2:].strip()
            if i + 2 < len(lines) and lines[i + 2].startswith('*'):
                sub = lines[i + 2].strip().strip('*')
                i += 3
            else:
                i += 1
            continue
        if line.startswith('## '):
            flush()
            h = line[3:].strip()
            heads.append(h)
            if out:
                out.append('</section>')
            out.append(f'<section class="page"><h1 class="mdh">{inline(h)}</h1>')
            i += 1
            continue
        if line.startswith('### '):
            flush()
            out.append(f'<h2>{inline(line[4:].strip())}</h2>')
            i += 1
            continue
        if line.startswith('|'):
            flush()
            rows = []
            while i < len(lines) and lines[i].startswith('|'):
                cells = [c.strip() for c in lines[i].strip().strip('|').split('|')]
                if not all(re.fullmatch(r':?-+:?', c) for c in cells):
                    rows.append(cells)
                i += 1
            cols = len(rows[0])
            cls = 'md wide' if cols >= 5 else 'md'
            head = ''.join(f'<th>{inline(c)}</th>' for c in rows[0])
            body = ''.join('<tr>' + ''.join(f'<td>{inline(c)}</td>' for c in r) + '</tr>' for r in rows[1:])
            out.append(f'<table class="{cls}"><thead><tr>{head}</tr></thead><tbody>{body}</tbody></table>')
            continue
        m = re.match(r'(\s*)(-|\d+\.)\s+(.*)', line)
        if m:
            flush()
            tag = 'ul' if m.group(2) == '-' else 'ol'
            items = []
            while i < len(lines):
                m = re.match(r'(\s*)(-|\d+\.)\s+(.*)', lines[i])
                if m:
                    items.append(m.group(3).strip())
                elif lines[i].startswith('  ') and lines[i].strip() and items:
                    items[-1] += ' ' + lines[i].strip()
                else:
                    break
                i += 1
            out.append(f'<{tag}>' + ''.join(f'<li>{inline(t)}</li>' for t in items) + f'</{tag}>')
            continue
        para.append(line.strip())
        i += 1
    flush()
    if out:
        out.append('</section>')
    return title, sub, ''.join(out), heads


EXTRA_CSS = '''
.mdh { font-size: 13.5pt; margin-top: 1mm; padding-bottom: 1mm; border-bottom: .5mm solid #000; }
table.md { font-size: 6.9pt; }
table.md.wide { font-size: 6pt; line-height: 1.2; }
table.md.wide td, table.md.wide th { padding: .5mm .5mm; }
table.md td:first-child { font-weight: 600; }
'''


def build(title, sub, body, heads, numbers=None):
    toc = ''.join(f'<div class="toc1"><span>{inline(h)}</span><i></i><b>{numbers[k] if numbers else "··"}</b></div>' for k, h in enumerate(heads))
    cover = f'''<section class="cover"><div class="cv-top">VẠN QUYỂN TIÊN ĐỒ</div>{cn.img('logo', 'cv-logo')}
<div class="cv-title" style="font-size:19pt">{inline(title)}</div><div class="cv-rule"></div><div class="cv-sub">{inline(sub)}</div>
<div class="cv-ver">Bản thiết kế · sau {cn.e(cn.VERSION)} · {cn.e(cn.GENERATION)}</div></section>'''
    contents = f'<section class="contents"><h1 class="toch">Mục lục</h1>{toc}</section>'
    return f'<!doctype html><html lang="vi"><head><meta charset="utf-8"><style>{cn.CSS}{EXTRA_CSS}</style></head><body>{cover}{contents}{body}</body></html>'


def pages_of(pdf, heads):
    doc = pymupdf.open(pdf)
    texts = [cn.norm(p.get_text()) for p in doc]
    found, start = [], 2
    for h in heads:
        k = cn.norm(re.sub(r'\*', '', h))
        page = next((p for p in range(start, len(texts)) if k in texts[p]), None)
        assert page is not None, h
        found.append(page)
        start = page + 1
    return found


def main(src):
    title, sub, body, heads = convert(open(src, encoding='utf-8').read())
    os.makedirs(cn.OUT_DIR, exist_ok=True)
    draft = os.path.join(cn.OUT_DIR, 'md-draft.pdf')
    cn.print_pdf(build(title, sub, body, heads), draft)
    pages = pages_of(draft, heads)
    cn.print_pdf(build(title, sub, body, heads, [p + 1 for p in pages]), draft)
    assert pages_of(draft, heads) == pages, 'the contents moved the pages'
    doc = pymupdf.open(draft)
    font = pymupdf.Font(fontfile=os.path.join(cn.FONTS, 'Bitter-Regular.ttf'))
    for n, page in enumerate(doc):
        if n == 0:
            continue
        page.wrap_contents()
        w, h = page.rect.width, page.rect.height
        tw = pymupdf.TextWriter(page.rect)
        tw.append((15.6, h - 9.5), 'Vạn Quyển Tiên Đồ · ' + title, font=font, fontsize=5.6)
        num = str(n + 1)
        tw.append((w - 15.6 - font.text_length(num, 5.6), h - 9.5), num, font=font, fontsize=5.6)
        tw.write_text(page, color=(0.25, 0.25, 0.25))
        page.draw_line((15.6, h - 15), (w - 15.6, h - 15), color=(0.6, 0.6, 0.6), width=0.3)
    doc.set_toc([[1, re.sub(r'\*', '', h), pages[k] + 1] for k, h in enumerate(heads)])
    doc.set_metadata({'title': title, 'author': 'Vạn Quyển Tiên Đồ'})
    out = os.path.splitext(src)[0] + '.pdf'
    doc.save(out, garbage=4, deflate=True)
    print(out, doc.page_count, 'pages')


if __name__ == '__main__':
    sys.stdout.reconfigure(encoding='utf-8')
    main(sys.argv[1])
