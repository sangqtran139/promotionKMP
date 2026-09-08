#!/usr/bin/env python3
"""Đánh số đầu mục + sinh/cập nhật mục lục cho các tài liệu trong gói bàn giao.

Hai việc, chạy cùng một lượt:

1. **Đánh số phân cấp** cho tiêu đề H2–H4 theo thứ tự xuất hiện — `1.` → `1.1.` → `1.1.1.`.
   Số cũ (nếu có) bị bóc ra rồi đánh lại, nên chèn/xoá/đổi chỗ một mục là cả tài liệu tự dồn số.
2. **Mục lục** đặt giữa hai mốc `<!-- toc -->` / `<!-- /toc -->`, khớp đúng số vừa đánh.

Chạy lại bao nhiêu lần cũng ra cùng kết quả.

    python3 scripts/docs_toc.py            # cập nhật toàn bộ
    python3 scripts/docs_toc.py --check    # chỉ báo file nào lệch (dùng cho CI), không sửa
    python3 scripts/docs_toc.py --no-number # chỉ dựng mục lục, không đụng số đầu mục
    python3 scripts/docs_toc.py --all      # áp cho mọi tài liệu trong docs/
    python3 scripts/docs_toc.py FILE...    # chỉ định file cụ thể

Mặc định chỉ áp cho **bộ tài liệu bàn giao** (danh sách ở `scripts/_docs_manifest.py`) — đó là bộ
đưa cho đối tác nên phải nhất quán. Muốn đánh số cả các guide nội bộ thì thêm `--all`.

⚠️ Đánh số làm **đổi anchor** của tiêu đề (`#1-tổng-quan` → `#2-tổng-quan`). Sau khi chạy, kiểm
link nội bộ bằng `python3 scripts/docs_links.py`.

Danh sách file mặc định lấy từ `scripts/docs_to_docx.py` — cùng một bộ tài liệu bàn giao.
Chỉ chèn mục lục cho file có từ MIN_H2 tiêu đề cấp 2 trở lên.
"""
from __future__ import annotations

import argparse
import re
import sys
import unicodedata
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
BEGIN, END = "<!-- toc -->", "<!-- /toc -->"
MIN_H2 = 4          # ít hơn thì mục lục chỉ tổ nhiễu
MAX_LEVEL = 3       # đưa vào mục lục tới H3
NUMBER_LEVEL = 4    # đánh số tới H4 (1. → 1.1. → 1.1.1.)

# Tiêu đề không đánh số: mục lục, và các mục phụ trợ không thuộc mạch nội dung.
NO_NUMBER = {"mục lục"}

# Mục đã được đánh số `0.` là cố ý (phần mở đầu đứng TRƯỚC mục 1, ví dụ "0. TL;DR"): giữ nguyên
# số và KHÔNG tính vào bộ đếm. Nếu dồn nó thành `1.` thì cả tài liệu lệch một nhịp và mọi tham
# chiếu "§N" ở nơi khác trong repo hỏng theo — cái giá không đáng cho một con số.
KEEP_ZERO = re.compile(r"^0[.)]\s+")

# Số thứ tự đã có sẵn ở đầu tiêu đề, ví dụ "3.3.14. " hay "2) " — bóc ra trước khi đánh lại.
EXISTING_NUMBER = re.compile(r"^\d+(?:\.\d+)*[.)]?\s+")


def slug(text: str) -> str:
    """Anchor kiểu GitHub: bỏ markdown, hạ chữ thường, bỏ dấu câu, khoảng trắng → '-'."""
    t = re.sub(r"`([^`]*)`", r"\1", text)
    t = re.sub(r"\*\*([^*]*)\*\*", r"\1", t)
    t = re.sub(r"(?<!\*)\*([^*]+)\*(?!\*)", r"\1", t)
    t = re.sub(r"\[([^\]]+)\]\([^)]+\)", r"\1", t)
    t = unicodedata.normalize("NFC", t).strip().lower()
    t = "".join(c for c in t if c.isalnum() or c in " -_")
    return t.strip().replace(" ", "-")


def strip_number(text: str) -> str:
    return EXISTING_NUMBER.sub("", text.strip()).strip()


def renumber(md: str) -> str:
    """Đánh lại số phân cấp cho H2–H4. Giữ nguyên mọi thứ khác của dòng tiêu đề."""
    out: list[str] = []
    counters: list[int] = []
    in_fence = False
    for line in md.split("\n"):
        if line.strip().startswith("```"):
            in_fence = not in_fence
            out.append(line)
            continue
        m = None if in_fence else re.match(r"(#{2,6})(\s+)(.+?)(\s*)$", line)
        if not m:
            out.append(line)
            continue

        level, gap, text, tail = len(m.group(1)), m.group(2), m.group(3), m.group(4)
        bare = strip_number(text)
        if level > NUMBER_LEVEL or bare.lower() in NO_NUMBER:
            out.append(f"{m.group(1)}{gap}{bare}{tail}")
            continue

        if level == 2 and KEEP_ZERO.match(text.strip()):
            out.append(f"{m.group(1)}{gap}0. {bare}{tail}")
            continue

        depth = level - 1                      # H2 → 1 cấp, H3 → 2 cấp, H4 → 3 cấp

        # Mục con đứng TRƯỚC mục cấp 2 đầu tiên (docs/features/* mở đầu bằng một loạt H3 ghi chú)
        # thì không có cha để bám. Đánh số sẽ ra "0.1", "0.2" — một cấp không tồn tại. Để trần.
        if depth > 1 and (not counters or counters[0] == 0):
            out.append(f"{m.group(1)}{gap}{bare}{tail}")
            continue

        if len(counters) < depth:
            counters += [0] * (depth - len(counters))
        else:
            counters = counters[:depth]
        counters[depth - 1] += 1
        prefix = ".".join(str(c) for c in counters)
        out.append(f"{m.group(1)}{gap}{prefix}. {bare}{tail}")
    return "\n".join(out)


def headings(md: str) -> list[tuple[int, str, str]]:
    out: list[tuple[int, str, str]] = []
    seen: dict[str, int] = {}
    in_fence = False
    for line in md.split("\n"):
        if line.strip().startswith("```"):
            in_fence = not in_fence
            continue
        if in_fence:
            continue
        m = re.match(r"(#{2,6})\s+(.+?)\s*$", line)
        if not m:
            continue
        level, text = len(m.group(1)), m.group(2)
        if level > MAX_LEVEL or text.strip().lower() == "mục lục":
            continue
        base = slug(text)
        n = seen.get(base, 0)
        seen[base] = n + 1
        out.append((level, text, base if n == 0 else f"{base}-{n}"))
    return out


def strip_inline(text: str) -> str:
    t = re.sub(r"\*\*([^*]*)\*\*", r"\1", text)
    t = re.sub(r"(?<!\*)\*([^*]+)\*(?!\*)", r"\1", t)
    return re.sub(r"\[([^\]]+)\]\([^)]+\)", r"\1", t).strip()


def build_toc(items: list[tuple[int, str, str]]) -> str:
    lines = [BEGIN]
    for level, text, anchor in items:
        indent = "  " * (level - 2)
        lines.append(f"{indent}- [{strip_inline(text)}](#{anchor})")
    lines.append(END)
    return "\n".join(lines)


def apply(md: str, number: bool = True) -> str | None:
    """Trả về nội dung mới, hoặc None nếu file này không cần mục lục."""
    if number:
        md = renumber(md)
    items = headings(md)
    if sum(1 for lv, _, _ in items if lv == 2) < MIN_H2:
        return md          # không dựng mục lục, nhưng số đầu mục vẫn phải được giữ
    toc = build_toc(items)

    if BEGIN in md and END in md:
        head, rest = md.split(BEGIN, 1)
        _, tail = rest.split(END, 1)
        return head + toc + tail

    lines = md.split("\n")
    in_fence = False
    idx = None
    for i, line in enumerate(lines):
        if line.strip().startswith("```"):
            in_fence = not in_fence
            continue
        if not in_fence and re.match(r"#{2,3}\s+", line):
            idx = i
            break
    if idx is None:
        return md

    # lùi qua các dòng trống và đường kẻ ngang ngay trước tiêu đề đầu tiên
    cut = idx
    while cut > 0 and (not lines[cut - 1].strip() or re.fullmatch(r"-{3,}", lines[cut - 1].strip())):
        cut -= 1

    block = ["", "## Mục lục", "", toc, "", "---", ""]
    return "\n".join(lines[:cut] + block + lines[idx:])


def rel(path: Path) -> str:
    try:
        return str(path.relative_to(ROOT))
    except ValueError:          # file ngoài repo (chạy thử trên bản sao)
        return str(path)


def all_docs() -> list[Path]:
    """Mọi tài liệu trong docs/ (bỏ tài liệu tự sinh và thư mục build)."""
    skip = {"build", "dist", ".venv"}
    return sorted(p for p in (ROOT / "docs").rglob("*.md")
                  if not any(part in skip for part in p.relative_to(ROOT).parts))


def targets(argv: list[str], everything: bool = False) -> list[Path]:
    if everything:
        return all_docs()
    if argv:
        return [Path(a) if Path(a).is_absolute() else ROOT / a for a in argv]
    sys.path.insert(0, str(ROOT / "scripts"))
    try:
        from docs_to_docx import DOCS  # type: ignore
    except Exception as e:  # pragma: no cover
        sys.exit(f"Không đọc được danh sách tài liệu từ docs_to_docx.py: {e}")
    return [ROOT / src for src, _, _ in DOCS]


def main() -> int:
    ap = argparse.ArgumentParser(description="Đánh số đầu mục + sinh mục lục cho tài liệu bàn giao")
    ap.add_argument("files", nargs="*", help="file cụ thể (mặc định: bộ tài liệu bàn giao)")
    ap.add_argument("--check", action="store_true", help="chỉ báo lệch, không sửa")
    ap.add_argument("--no-number", action="store_true",
                    help="chỉ dựng mục lục, không đánh số đầu mục")
    ap.add_argument("--all", action="store_true",
                    help="áp cho MỌI tài liệu trong docs/, không chỉ bộ bàn giao")
    args = ap.parse_args()

    changed, stale = 0, []
    for path in targets(args.files, everything=args.all):
        if not path.exists():
            print(f"! thiếu nguồn: {rel(path)}")
            continue
        md = path.read_text(encoding="utf-8")
        new = apply(md, number=not args.no_number)
        if new is None or new == md:
            print(f"= đã khớp: {rel(path)}")
        elif args.check:
            stale.append(rel(path))
            print(f"✗ lệch: {rel(path)}")
        else:
            path.write_text(new, encoding="utf-8")
            print(f"✓ cập nhật: {rel(path)}")
            changed += 1

    if args.check and stale:
        print(f"\n{len(stale)} file lệch số đầu mục hoặc mục lục — chạy `python3 scripts/docs_toc.py`.")
        return 1
    print(f"\n{changed} file cập nhật.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
