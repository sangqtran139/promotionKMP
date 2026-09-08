#!/usr/bin/env python3
"""Sinh bộ tài liệu **Confluence wiki markup** cho gói bàn giao, TỪ Markdown trong repo.

Markdown là nguồn sự thật; bản trên Confluence chỉ là bản dán. Sửa trên wiki rồi coi đó là bản mới
thì lần chạy sau ghi đè mất — sửa Markdown rồi chạy lại script này.

    python3 scripts/docs_to_confluence.py           # sinh vào dist/docs-confluence/
    python3 scripts/docs_to_confluence.py --list    # xem danh sách sẽ sinh
    python3 scripts/docs_to_confluence.py --out DIR # đổi thư mục đích

Cách dán vào Confluence (xem thêm README.txt sinh kèm):
  Tạo trang mới → đặt đúng tiêu đề ở cột "Tiêu đề trang" → dấu **+** (Insert) →
  *Markup* → chọn **Confluence wiki markup** → dán toàn bộ nội dung file .confluence → Insert.

Không cần cài thư viện ngoài. Cố tình chỉ dùng cú pháp wiki markup có ở **cả** Cloud lẫn
Server/Data Center: heading, bảng, danh sách, {code}, {noformat}, {quote}, {info}. Không dùng macro
riêng của một bản để bản kia khỏi hiện khối lỗi đỏ.
"""
from __future__ import annotations

import argparse
import re
import sys
from datetime import date
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from _docs_manifest import DOCS, PRODUCT, ROOT, git, page_title, sdk_version  # noqa: E402

# Ngôn ngữ có trong macro {code} của CẢ Cloud lẫn Server cũ. Kotlin/Swift/JSON/YAML không nằm trong
# bộ chung — Server cũ gặp `language=kotlin` thì đổ khối lỗi thay vì code, nên hạ về ngôn ngữ gần
# nhất hoặc bỏ tô màu. Mất màu cú pháp còn hơn mất cả khối code.
LANG = {
    "kotlin": "java", "kt": "java", "kts": "java", "gradle": "java", "groovy": "groovy",
    "java": "java", "swift": "", "objc": "", "xml": "xml", "html": "html",
    "json": "javascript", "js": "javascript", "javascript": "javascript",
    "bash": "bash", "sh": "bash", "shell": "bash", "zsh": "bash", "console": "bash",
    "python": "python", "py": "python", "sql": "sql", "css": "css",
    "yaml": "", "yml": "", "toml": "", "properties": "", "text": "", "txt": "", "": "",
}

# Tài liệu nào trong bộ thì link chéo trỏ sang TIÊU ĐỀ TRANG Confluence tương ứng; ngoài bộ thì
# không có trang để trỏ, giữ lại nhãn + tên file cho người đọc tự tra trong repo.
PAGE_TITLE = {src: page_title(name, title) for src, name, title in DOCS}


# ─────────────────────────── inline ───────────────────────────

# `code` được rút ra TRƯỚC bold/italic, vì nội dung code hay chứa chính ký tự đánh dấu: glob
# `com.ttcn.prm.*` nằm giữa một cụm **đậm** từng làm vỡ cả đoạn khi khớp một lượt bằng regex chung.
CODE_SPAN = re.compile(r"`[^`]+`")
MARK = re.compile(
    r"(?P<bold>\*\*[^*]+\*\*)"
    r"|(?P<link>\[[^\]]*\]\([^)]+\))"
    r"|(?P<italic>(?<![*\w])\*[^*\n]+\*(?!\*))"
)
SLOT = "\x00%d\x00"
SLOT_RE = re.compile(r"\x00(\d+)\x00")

# Ký tự mở macro/link của wiki markup — lọt vào văn xuôi là Confluence hiểu nhầm thành cú pháp.
ESCAPE = str.maketrans({"{": r"\{", "}": r"\}", "[": r"\[", "]": r"\]"})

# Confluence tự đổi mấy chuỗi này thành emoticon, kể cả bên trong {{monospace}}. Selector Swift
# `refreshToken(_:)` / `initialize(tokenSource:baseUrl:)` kết thúc bằng `:)` nên bị nuốt thành mặt
# cười — đã dính thật ở trang 3.3.14.3. Chặn bằng dấu \ trước ký tự đầu.
EMOTICON = re.compile(r"(?<!\\)(:\)|:\(|:P|:D|;\)|:\||\((?:y|n|i|/|x|!|\?|\+|-|on|off|\*)\))")


def esc(text: str, in_table: bool = False) -> str:
    out = text.translate(ESCAPE)
    out = EMOTICON.sub(lambda m: "\\" + m.group(1), out)
    if in_table:
        out = out.replace("|", "\\|")   # dấu | trong ô sẽ cắt ô làm đôi
    return out


def resolve_link(src_rel: Path, target: str) -> str | None:
    """Link nội bộ → tiêu đề trang Confluence, nếu tài liệu đích cũng nằm trong bộ bàn giao."""
    path = (src_rel.parent / target.split("#")[0]).as_posix()
    path = re.sub(r"[^/]+/\.\./", "", path).lstrip("./")
    return PAGE_TITLE.get(path)


def restore(text: str, codes: list[str], in_table: bool, mono: bool = True) -> str:
    """Trả code span về chỗ cũ. Trong nhãn link thì để chữ trơn — {{...}} lồng trong [..|..] không
    hiển thị ổn định giữa các bản Confluence."""
    def sub(m: re.Match) -> str:
        # {{...}} là monospace; ngoặc nhọn bên trong vẫn phải escape kẻo đóng macro sớm.
        body = esc(codes[int(m.group(1))], in_table)
        return "{{" + body + "}}" if mono else body
    return SLOT_RE.sub(sub, text)


def markup(text: str, src_rel: Path, codes: list[str], in_table: bool) -> str:
    """Bold / italic / link, chạy trên phần văn bản đã rút hết code span."""
    out, pos = [], 0
    for m in MARK.finditer(text):
        if m.start() > pos:
            out.append(esc(text[pos:m.start()], in_table))
        if m.group("bold"):
            out.append("*" + markup(m.group("bold")[2:-2], src_rel, codes, in_table) + "*")
        elif m.group("italic"):
            out.append("_" + markup(m.group("italic")[1:-1], src_rel, codes, in_table) + "_")
        else:
            label, _, target = m.group("link")[1:-1].partition("](")
            label = restore(esc(label, in_table), codes, in_table, mono=False) or esc(target, in_table)
            if "://" in target or target.startswith("mailto:"):
                out.append(f"[{label}|{target}]")
            elif target.startswith("#"):
                out.append(label)                       # neo trong trang — bỏ, giữ chữ
            else:
                page = resolve_link(src_rel, target)
                out.append(f"[{label}|{page}]" if page else f"{label} _({esc(target, in_table)})_")
        pos = m.end()
    out.append(esc(text[pos:], in_table))
    return "".join(out)


def inline(text: str, src_rel: Path, in_table: bool = False) -> str:
    codes: list[str] = []

    def stash(m: re.Match) -> str:
        codes.append(m.group(0)[1:-1])
        return SLOT % (len(codes) - 1)

    return restore(markup(CODE_SPAN.sub(stash, text), src_rel, codes, in_table), codes, in_table)


# ─────────────────────────── khối ───────────────────────────

def split_row(line: str) -> list[str]:
    return [c.strip() for c in line.strip().strip("|").split("|")]


def is_separator(line: str) -> bool:
    return bool(re.fullmatch(r"\|?[\s:|-]+\|[\s:|-]*", line.strip())) and "-" in line


def fence(lines: list[str], lang: str) -> list[str]:
    body = lines or [""]
    if lang not in LANG:                       # ngôn ngữ lạ → không đoán bừa
        return ["{noformat}", *body, "{noformat}", ""]
    mapped = LANG[lang]
    open_tag = f"{{code:language={mapped}}}" if mapped else "{code}"
    return [open_tag, *body, "{code}", ""]


def convert(md: str, src_rel: Path) -> str:
    lines = md.split("\n")
    out: list[str] = []
    i, n = 0, len(lines)

    while i < n:
        line = lines[i]
        stripped = line.strip()

        if stripped.startswith("```"):
            lang = stripped[3:].strip().lower()
            i += 1
            buf = []
            while i < n and not lines[i].strip().startswith("```"):
                buf.append(lines[i]); i += 1
            i += 1
            out += fence(buf, lang)
            continue

        if not stripped:
            i += 1
            if out and out[-1] != "":
                out.append("")
            continue

        if re.fullmatch(r"(-{3,}|\*{3,}|_{3,})", stripped):
            out += ["----", ""]
            i += 1
            continue

        # Mốc mục lục của bản Markdown — bỏ qua, đã thay bằng macro {toc} ở dưới.
        if stripped.startswith("<!--"):
            while i < n and "-->" not in lines[i]:
                i += 1
            i += 1
            continue

        m = re.match(r"(#{1,6})\s+(.*)", stripped)
        if m:
            # Mục lục Markdown là danh sách link `#anchor` kiểu GitHub — trên wiki chúng KHÔNG
            # nhảy tới đâu cả, để nguyên là dán lên một nắm link chết. Thay bằng {toc}: macro gốc,
            # có ở cả Cloud lẫn Server, tự dựng theo heading của chính trang.
            if m.group(2).strip().lower() == "mục lục":
                out += [f"h{len(m.group(1))}. {inline(m.group(2), src_rel)}", "",
                        "{toc:minLevel=2|maxLevel=3|style=none|indent=16px}", ""]
                i += 1
                while i < n:
                    nxt = re.match(r"(#{1,6})\s+", lines[i].strip())
                    if nxt and len(nxt.group(1)) <= len(m.group(1)):
                        break
                    i += 1
                continue
            out += [f"h{len(m.group(1))}. {inline(m.group(2), src_rel)}", ""]
            i += 1
            continue

        if stripped.startswith("|") and i + 1 < n and is_separator(lines[i + 1]):
            header = split_row(stripped)
            out.append("||" + "||".join(inline(c, src_rel, True) or " " for c in header) + "||")
            i += 2
            while i < n and lines[i].strip().startswith("|"):
                row = split_row(lines[i])
                cells = [inline(row[j], src_rel, True) if j < len(row) else "" for j in range(len(header))]
                out.append("|" + "|".join(c or " " for c in cells) + "|")
                i += 1
            out.append("")
            continue

        if stripped.startswith(">"):
            buf = []
            while i < n and lines[i].strip().startswith(">"):
                buf.append(re.sub(r"^\s*>\s?", "", lines[i])); i += 1
            # Đệ quy chứ không nối chuỗi: khối trích dẫn trong bộ này có cả bảng lẫn code fence
            # (PublicApi §"Luật một dòng"), nối phẳng là mất sạch cấu trúc.
            inner = convert("\n".join(buf), src_rel).rstrip()
            out += ["{quote}", inner, "{quote}", ""]
            continue

        m = re.match(r"^(\s*)([-*+]|\d+[.)])\s+(.*)", line)
        if m:
            while i < n:
                lm = re.match(r"^(\s*)([-*+]|\d+[.)])\s+(.*)", lines[i])
                if not lm:
                    cont = lines[i].strip()
                    # Code fence / bảng thụt vào trong một mục danh sách: wiki markup không lồng
                    # được khối vào bullet, nên nhả ra thành khối riêng ngay sau mục đó. Gộp vào
                    # chữ của bullet thì mất nguyên khối code (đã từng mất khối grep ở
                    # ReleaseChecklist §3).
                    if cont.startswith("```"):
                        lang = cont[3:].strip().lower()
                        i += 1
                        buf = []
                        while i < n and not lines[i].strip().startswith("```"):
                            buf.append(lines[i].strip()); i += 1
                        i += 1
                        out += fence(buf, lang)
                        continue
                    if cont.startswith(("|", "#", ">")):
                        break
                    if cont:                    # dòng nối tiếp của mục trước
                        out[-1] += " " + inline(cont, src_rel)
                        i += 1
                        continue
                    break
                indent, marker, text = lm.group(1), lm.group(2), lm.group(3)
                level = len(indent) // 2 + 1
                bullet = "#" if marker[0].isdigit() else "*"
                cb = re.match(r"^\[([ xX])\]\s*(.*)", text)
                # Ô tick/chéo là emoticon mình CỐ Ý chèn, nên phải nối sau khi convert — cho nó đi
                # qua inline() thì esc() lại escape mất, ra chữ "(x)" trần.
                box = ""
                if cb:
                    box = ("(/) " if cb.group(1).lower() == "x" else "(x) ")
                    text = cb.group(2)
                out.append(bullet * level + " " + box + inline(text, src_rel))
                i += 1
            out.append("")
            continue

        # Đoạn văn: gộp các dòng xuống dòng mềm thành một đoạn, nếu không Confluence ngắt lung tung.
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
        para = inline(" ".join(buf), src_rel)
        if para[:1] in ("#", "-"):              # đầu dòng là # / - thì bị đọc thành danh sách
            para = "\\" + para
        out += [para, ""]

    return "\n".join(out).rstrip() + "\n"


def header(title: str, src: str, version: str, commit: str) -> str:
    return (
        "{info:title=Tài liệu sinh tự động — đừng sửa trực tiếp trên Confluence}\n"
        f"Nguồn: {{{{{src}}}}} trong repo {PRODUCT}. "
        "Sửa file Markdown rồi chạy lại {{scripts/docs_to_confluence.py}} và dán lại trang này; "
        "sửa thẳng trên wiki sẽ mất ở lần dán sau.\n"
        "{info}\n\n"
        "||Sản phẩm|" + PRODUCT + "|\n"
        f"||Tài liệu|{title}|\n"
        f"||Phiên bản SDK|{version}|\n"
        f"||Commit|{commit}|\n"
        f"||Ngày sinh|{date.today().isoformat()}|\n\n"
        "----\n\n"
    )


README = """\
Bộ tài liệu Confluence — {product} {version} (commit {commit}, sinh ngày {today})

CÁCH DÁN VÀO CONFLUENCE
1. Tạo trang mới trong space đích, đặt tiêu đề ĐÚNG như cột "Tiêu đề trang" bên dưới.
   Link chéo giữa các tài liệu được sinh theo tiêu đề này — đặt sai tiêu đề là link gãy.
2. Trong trình soạn thảo: nút + (Insert)  ->  Markup  ->  chọn "Confluence wiki markup".
   (Bản Server/DC cũ: menu Insert > Markup, hoặc gõ dấu {{ rồi chọn Markup.)
3. Dán TOÀN BỘ nội dung file .confluence tương ứng vào ô bên trái, xem preview bên phải, Insert.
4. Lưu trang.

Gợi ý cây trang (tài liệu 01 làm trang cha, còn lại làm trang con):

{tree}

LƯU Ý
- Markdown trong repo là nguồn sự thật. Sửa trên Confluence thì lần dán sau mất sạch.
- Mục lục: thêm macro Table of Contents ở đầu trang nếu muốn — script không chèn sẵn vì tên macro
  khác nhau giữa Cloud và Server.
- Code block: Kotlin/Swift/JSON/YAML không có trong bộ ngôn ngữ chung của macro {{code}}, đã hạ về
  ngôn ngữ gần nhất (Kotlin -> java) hoặc bỏ tô màu. Nội dung code không đổi.
- Link tới tài liệu KHÔNG nằm trong bộ 17 file này được giữ dạng chữ kèm tên file, vì trên
  Confluence không có trang tương ứng để trỏ.
"""


def main() -> int:
    ap = argparse.ArgumentParser(description="Sinh bộ Confluence wiki markup cho gói bàn giao")
    ap.add_argument("--out", default="dist/docs-confluence", help="thư mục đích")
    ap.add_argument("--list", action="store_true", help="chỉ liệt kê, không sinh file")
    args = ap.parse_args()

    if args.list:
        for src, out, title in DOCS:
            mark = " " if (ROOT / src).exists() else "!"
            print(f"{mark} {out + '.confluence':40s} ← {src}   ({title})")
        return 0

    out_dir = Path(args.out) if Path(args.out).is_absolute() else ROOT / args.out
    out_dir.mkdir(parents=True, exist_ok=True)
    version, commit = sdk_version(), git("rev-parse", "--short", "HEAD")

    made, missing, tree = 0, [], []
    for src, out, title in DOCS:
        path = ROOT / src
        if not path.exists():
            missing.append(src)
            continue
        body = header(title, src, version, commit) + convert(path.read_text(encoding="utf-8"), Path(src))
        (out_dir / f"{out}.confluence").write_text(body, encoding="utf-8")
        tree.append(f"  {out}.confluence".ljust(42) + f"-> {title}")
        print(f"✓ {out}.confluence")
        made += 1

    (out_dir / "README.txt").write_text(
        README.format(product=PRODUCT, version=version, commit=commit,
                      today=date.today().isoformat(), tree="\n".join(tree)),
        encoding="utf-8")

    print(f"\n{made} tài liệu + README.txt → {out_dir}")
    if missing:
        print("Thiếu nguồn: " + ", ".join(missing))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
