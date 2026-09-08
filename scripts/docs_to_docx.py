#!/usr/bin/env python3
"""Sinh bộ tài liệu .docx cho gói bàn giao, TỪ Markdown trong repo.

Markdown là nguồn sự thật; .docx chỉ là bản in. Đừng sửa .docx rồi coi đó là bản mới —
sửa Markdown rồi chạy lại script này.

    python3 scripts/docs_to_docx.py            # sinh toàn bộ vào dist/docs-docx/
    python3 scripts/docs_to_docx.py --list     # xem danh sách sẽ sinh
    python3 scripts/docs_to_docx.py --out DIR  # đổi thư mục đích

Yêu cầu: python-docx (cài trong virtualenv để không đụng Python hệ thống)

    python3 -m venv .venv && ./.venv/bin/pip install python-docx
    ./.venv/bin/python scripts/docs_to_docx.py
"""
from __future__ import annotations

import argparse
import re
import sys
from datetime import date
from pathlib import Path

try:
    from docx import Document
    from docx.enum.text import WD_ALIGN_PARAGRAPH
    from docx.oxml import OxmlElement
    from docx.oxml.ns import qn
    from docx.shared import Pt, RGBColor, Inches
    _MISSING_DEP = None
except ImportError:
    # Không thoát ngay: `--list` và các script khác (docs_toc.py) vẫn dùng được danh sách DOCS
    # mà không cần python-docx. Chỉ lúc thật sự dựng file mới bắt buộc có thư viện.
    _MISSING_DEP = (
        "Thiếu python-docx. Cài trong virtualenv:\n"
        "  python3 -m venv .venv && ./.venv/bin/pip install python-docx\n"
        "  ./.venv/bin/python scripts/docs_to_docx.py"
    )

sys.path.insert(0, str(Path(__file__).resolve().parent))
from _docs_manifest import DOCS, PRODUCT, ROOT, git, sdk_version  # noqa: E402

MONO = "Consolas"


# ─────────────────────────── tiện ích OOXML ───────────────────────────

def shade(paragraph, hex_fill: str) -> None:
    pr = paragraph._p.get_or_add_pPr()
    el = OxmlElement("w:shd")
    el.set(qn("w:val"), "clear")
    el.set(qn("w:fill"), hex_fill)
    pr.append(el)


def add_toc(paragraph) -> None:
    """Chèn field TOC. Word hỏi 'cập nhật mục lục?' khi mở lần đầu."""
    run = paragraph.add_run()
    begin = OxmlElement("w:fldChar"); begin.set(qn("w:fldCharType"), "begin")
    instr = OxmlElement("w:instrText"); instr.set(qn("xml:space"), "preserve")
    instr.text = r'TOC \o "1-3" \h \z \u'
    sep = OxmlElement("w:fldChar"); sep.set(qn("w:fldCharType"), "separate")
    hint = OxmlElement("w:t"); hint.text = "Mở bằng Word rồi nhấn F9 để dựng mục lục."
    end = OxmlElement("w:fldChar"); end.set(qn("w:fldCharType"), "end")
    for el in (begin, instr, sep, hint, end):
        run._r.append(el)


def add_page_number_footer(section) -> None:
    p = section.footer.paragraphs[0]
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    run = p.add_run()
    begin = OxmlElement("w:fldChar"); begin.set(qn("w:fldCharType"), "begin")
    instr = OxmlElement("w:instrText"); instr.set(qn("xml:space"), "preserve"); instr.text = "PAGE"
    end = OxmlElement("w:fldChar"); end.set(qn("w:fldCharType"), "end")
    for el in (begin, instr, end):
        run._r.append(el)


# ─────────────────────────── inline markdown ───────────────────────────

INLINE = re.compile(
    r"(?P<code>`[^`]+`)"
    r"|(?P<bold>\*\*[^*]+\*\*)"
    r"|(?P<link>\[[^\]]+\]\([^)]+\))"
    r"|(?P<italic>(?<![*\w])\*[^*\n]+\*(?!\*))"
)


def write_inline(paragraph, text: str, base_bold: bool = False) -> None:
    pos = 0
    for m in INLINE.finditer(text):
        if m.start() > pos:
            r = paragraph.add_run(text[pos:m.start()]); r.bold = base_bold
        if m.group("code"):
            r = paragraph.add_run(m.group("code")[1:-1])
            r.font.name = MONO
            r.font.size = Pt(9.5)
            r.font.color.rgb = RGBColor(0xB0, 0x30, 0x60)
        elif m.group("bold"):
            r = paragraph.add_run(m.group("bold")[2:-2]); r.bold = True
        elif m.group("link"):
            label, _, target = m.group("link")[1:-1].partition("](")
            r = paragraph.add_run(label); r.bold = base_bold
            r.font.color.rgb = RGBColor(0x1A, 0x5F, 0xB4)
            r.underline = True
            if not target.startswith("#") and "://" not in target:
                note = paragraph.add_run(f" ({target})")
                note.font.size = Pt(8); note.italic = True
                note.font.color.rgb = RGBColor(0x88, 0x88, 0x88)
        elif m.group("italic"):
            r = paragraph.add_run(m.group("italic")[1:-1]); r.italic = True; r.bold = base_bold
        pos = m.end()
    if pos < len(text):
        r = paragraph.add_run(text[pos:]); r.bold = base_bold


# ─────────────────────────── khối markdown ───────────────────────────

def split_row(line: str) -> list[str]:
    return [c.strip() for c in line.strip().strip("|").split("|")]


def is_separator(line: str) -> bool:
    return bool(re.fullmatch(r"\|?[\s:|-]+\|[\s:|-]*", line.strip())) and "-" in line


def add_code_block(doc, lines: list[str]) -> None:
    for ln in lines or [""]:
        p = doc.add_paragraph()
        p.paragraph_format.space_after = Pt(0)
        p.paragraph_format.space_before = Pt(0)
        p.paragraph_format.left_indent = Inches(0.2)
        shade(p, "F4F4F4")
        r = p.add_run(ln if ln else " ")
        r.font.name = MONO
        r.font.size = Pt(8.5)
    doc.add_paragraph().paragraph_format.space_after = Pt(4)


def add_table(doc, rows: list[list[str]]) -> None:
    header, body = rows[0], rows[1:]
    table = doc.add_table(rows=1, cols=len(header))
    table.style = "Table Grid"
    table.autofit = True
    for i, cell_text in enumerate(header):
        cell = table.rows[0].cells[i]
        cell.text = ""
        shade(cell.paragraphs[0], "E8EEF7")
        write_inline(cell.paragraphs[0], cell_text, base_bold=True)
    for row in body:
        cells = table.add_row().cells
        for i in range(len(header)):
            cells[i].text = ""
            write_inline(cells[i].paragraphs[0], row[i] if i < len(row) else "")
    for row in table.rows:
        for cell in row.cells:
            for p in cell.paragraphs:
                p.paragraph_format.space_after = Pt(2)
                for r in p.runs:
                    r.font.size = Pt(9.5)
    doc.add_paragraph()


def render(doc, md: str) -> None:
    lines = md.split("\n")
    i, n = 0, len(lines)
    while i < n:
        line = lines[i]
        stripped = line.strip()

        if stripped.startswith("```"):
            i += 1
            buf = []
            while i < n and not lines[i].strip().startswith("```"):
                buf.append(lines[i]); i += 1
            i += 1
            add_code_block(doc, buf)
            continue

        if not stripped:
            i += 1
            continue

        if re.fullmatch(r"(-{3,}|\*{3,}|_{3,})", stripped):
            p = doc.add_paragraph()
            p.paragraph_format.space_before = Pt(2)
            p.paragraph_format.space_after = Pt(2)
            shade(p, "DDDDDD")
            p.add_run(" ").font.size = Pt(2)
            i += 1
            continue

        # Chú thích HTML (mốc <!-- toc -->…) không phải nội dung — bỏ qua.
        if stripped.startswith("<!--"):
            while i < n and "-->" not in lines[i]:
                i += 1
            i += 1
            continue

        m = re.match(r"(#{1,6})\s+(.*)", stripped)
        if m:
            level = min(len(m.group(1)), 4)
            # Bản .docx đã có mục lục riêng (field TOC ở trang bìa, Word tự dựng kèm số trang).
            # Giữ thêm mục lục Markdown nữa là hai mục lục chồng nhau, nên bỏ qua cả khối.
            if m.group(2).strip().lower() == "mục lục":
                i += 1
                while i < n:
                    nxt = re.match(r"(#{1,6})\s+", lines[i].strip())
                    if nxt and len(nxt.group(1)) <= len(m.group(1)):
                        break
                    i += 1
                continue
            h = doc.add_heading(level=level)
            write_inline(h, m.group(2))
            i += 1
            continue

        if stripped.startswith("|") and i + 1 < n and is_separator(lines[i + 1]):
            rows = [split_row(stripped)]
            i += 2
            while i < n and lines[i].strip().startswith("|"):
                rows.append(split_row(lines[i])); i += 1
            add_table(doc, rows)
            continue

        if stripped.startswith(">"):
            buf = []
            while i < n and lines[i].strip().startswith(">"):
                buf.append(re.sub(r"^\s*>\s?", "", lines[i])); i += 1
            # Khối trích dẫn có khi chứa cả bảng lẫn code fence (PublicApi §"Luật một dòng"). Nối
            # phẳng thành một đoạn là mất sạch cấu trúc, nên đệ quy; đổi lại mất nền vàng.
            if any(x.strip().startswith(("|", "```")) for x in buf):
                render(doc, "\n".join(buf))
                continue
            p = doc.add_paragraph()
            p.paragraph_format.left_indent = Inches(0.3)
            shade(p, "FFF8E1")
            write_inline(p, " ".join(x.strip() for x in buf if x.strip()))
            continue

        m = re.match(r"^(\s*)([-*+]|\d+[.)])\s+(.*)", line)
        if m:
            indent, marker, text = m.group(1), m.group(2), m.group(3)
            level = min(len(indent) // 2, 2)
            ordered = marker[0].isdigit()
            checkbox = ""
            cb = re.match(r"^\[([ xX])\]\s*(.*)", text)
            if cb:
                checkbox = "☒ " if cb.group(1).lower() == "x" else "☐ "
                text = cb.group(2)
            style = "List Number" if ordered and not checkbox else "List Bullet"
            if checkbox:
                p = doc.add_paragraph()
                p.paragraph_format.left_indent = Inches(0.25 + 0.25 * level)
                p.add_run(checkbox)
            else:
                p = doc.add_paragraph(style=style)
                if level:
                    p.paragraph_format.left_indent = Inches(0.25 * (level + 1))
            p.paragraph_format.space_after = Pt(2)
            write_inline(p, text)
            i += 1
            continue

        # Đoạn văn: gộp các dòng liền nhau (Markdown xuống dòng mềm) thành MỘT paragraph,
        # nếu không thì mỗi dòng nguồn thành một đoạn rời và bản in vỡ giữa câu.
        buf = [stripped]
        i += 1
        while i < n:
            nxt = lines[i].strip()
            if not nxt or nxt.startswith(("#", ">", "|", "```")) \
               or re.fullmatch(r"(-{3,}|\*{3,}|_{3,})", nxt) \
               or re.match(r"^(\s*)([-*+]|\d+[.)])\s+", lines[i]):
                break
            buf.append(nxt)
            i += 1
        p = doc.add_paragraph()
        write_inline(p, " ".join(buf))


# ─────────────────────────── dựng file ───────────────────────────

def build(src: Path, dst: Path, title: str, version: str, commit: str) -> None:
    doc = Document()

    normal = doc.styles["Normal"]
    normal.font.name = "Calibri"
    normal.font.size = Pt(10.5)
    normal.paragraph_format.space_after = Pt(6)
    rpr = normal.element.get_or_add_rPr().get_or_add_rFonts()
    rpr.set(qn("w:eastAsia"), "Calibri")

    add_page_number_footer(doc.sections[0])

    # Trang bìa
    t = doc.add_paragraph(); t.alignment = WD_ALIGN_PARAGRAPH.CENTER
    r = t.add_run(PRODUCT); r.bold = True; r.font.size = Pt(24)
    s = doc.add_paragraph(); s.alignment = WD_ALIGN_PARAGRAPH.CENTER
    r = s.add_run(title); r.font.size = Pt(16); r.font.color.rgb = RGBColor(0x44, 0x44, 0x44)
    doc.add_paragraph()

    meta = doc.add_table(rows=0, cols=2)
    meta.style = "Table Grid"
    for k, v in (
        ("Phiên bản sản phẩm", version),
        ("Ngày sinh tài liệu", date.today().isoformat()),
        ("Commit", commit),
        ("Nguồn", str(src.relative_to(ROOT))),
        ("Trạng thái", "Trình nghiệm thu"),
    ):
        cells = meta.add_row().cells
        cells[0].text = ""; cells[1].text = ""
        write_inline(cells[0].paragraphs[0], k, base_bold=True)
        write_inline(cells[1].paragraphs[0], v)

    doc.add_paragraph()
    note = doc.add_paragraph()
    shade(note, "FFF8E1")
    write_inline(note, "Tài liệu này được **sinh tự động từ Markdown trong repo**. "
                       "Sửa file Markdown nguồn rồi chạy lại `scripts/docs_to_docx.py`; "
                       "đừng sửa trực tiếp bản .docx.")

    doc.add_page_break()
    h = doc.add_heading("Mục lục", level=1)
    add_toc(doc.add_paragraph())
    doc.add_page_break()

    render(doc, src.read_text(encoding="utf-8"))
    dst.parent.mkdir(parents=True, exist_ok=True)
    doc.save(dst)


def main() -> int:
    ap = argparse.ArgumentParser(description="Sinh bộ .docx cho gói bàn giao")
    ap.add_argument("--out", default="dist/docs-docx", help="thư mục đích (mặc định dist/docs-docx)")
    ap.add_argument("--list", action="store_true", help="chỉ liệt kê, không sinh file")
    args = ap.parse_args()

    if args.list:
        for src, out, title in DOCS:
            out = f"{out}.docx"
            mark = " " if (ROOT / src).exists() else "!"
            print(f"{mark} {out:38s} ← {src}   ({title})")
        return 0

    if _MISSING_DEP:
        sys.exit(_MISSING_DEP)

    out_dir = (ROOT / args.out) if not Path(args.out).is_absolute() else Path(args.out)
    version, commit = sdk_version(), git("rev-parse", "--short", "HEAD")

    made, missing = 0, []
    for src, out, title in DOCS:
        path = ROOT / src
        if not path.exists():
            missing.append(src)
            continue
        out = f"{out}.docx"
        build(path, out_dir / out, title, version, commit)
        print(f"✓ {out}")
        made += 1

    print(f"\n{made} tài liệu → {out_dir}")
    if missing:
        print("Thiếu nguồn: " + ", ".join(missing))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
