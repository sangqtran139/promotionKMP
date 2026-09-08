#!/usr/bin/env python3
"""Kiểm link nội bộ giữa các file Markdown: file đích có tồn tại, và `#anchor` có trỏ đúng tiêu đề.

Chạy sau mỗi lần `docs_toc.py` đánh lại số đầu mục — đánh số làm **đổi anchor**
(`#1-tổng-quan` → `#2-tổng-quan`), và link cũ thì vẫn "trông có vẻ đúng".

    python3 scripts/docs_links.py           # quét toàn repo (trừ thư mục sinh ra)
    python3 scripts/docs_links.py FILE...   # chỉ quét file chỉ định

Trả mã 1 nếu có link hỏng — dùng được cho CI.
"""
from __future__ import annotations

import re
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from docs_toc import slug  # noqa: E402  — dùng CHUNG hàm slug với bộ sinh mục lục

ROOT = Path(__file__).resolve().parent.parent
SKIP_DIRS = {".git", "build", "dist", "node_modules", ".spm", ".gradle", ".kotlin", ".venv", ".idea"}
LINK = re.compile(r"\[[^\]]*\]\(([^)\s]+)(?:\s+\"[^\"]*\")?\)")


def anchors(path: Path) -> set[str]:
    """Tập anchor một file Markdown cung cấp: từ tiêu đề, cộng id của thẻ <a name>/<a id>."""
    out: set[str] = set()
    seen: dict[str, int] = {}
    in_fence = False
    for line in path.read_text(encoding="utf-8").split("\n"):
        if line.strip().startswith("```"):
            in_fence = not in_fence
            continue
        if in_fence:
            continue
        m = re.match(r"#{1,6}\s+(.+?)\s*$", line)
        if m:
            base = slug(m.group(1))
            n = seen.get(base, 0)
            seen[base] = n + 1
            out.add(base if n == 0 else f"{base}-{n}")
        for tag in re.finditer(r"<a\s+(?:name|id)=[\"']([^\"']+)[\"']", line):
            out.add(tag.group(1))
    return out


def md_files() -> list[Path]:
    out = []
    for p in ROOT.rglob("*.md"):
        if not any(part in SKIP_DIRS for part in p.relative_to(ROOT).parts):
            out.append(p)
    return sorted(out)


def main() -> int:
    args = sys.argv[1:]
    files = [Path(a) if Path(a).is_absolute() else ROOT / a for a in args] or md_files()

    cache: dict[Path, set[str]] = {}
    bad_file, bad_anchor = [], []

    for f in files:
        text = f.read_text(encoding="utf-8")
        for m in LINK.finditer(text):
            target = m.group(1)
            if "://" in target or target.startswith(("mailto:", "tel:")):
                continue
            rel_path, _, frag = target.partition("#")
            dest = f if not rel_path else (f.parent / rel_path).resolve()
            if not dest.exists():
                bad_file.append((f, target))
                continue
            if not frag or dest.suffix.lower() != ".md":
                continue
            if dest not in cache:
                cache[dest] = anchors(dest)
            if frag not in cache[dest]:
                bad_anchor.append((f, target))

    def show(title: str, rows) -> None:
        if not rows:
            return
        print(f"\n{title} ({len(rows)}):")
        for f, t in rows:
            print(f"  {f.relative_to(ROOT)} → {t}")

    show("File đích không tồn tại", bad_file)
    show("Anchor không khớp tiêu đề nào", bad_anchor)

    total = len(bad_file) + len(bad_anchor)
    print(f"\nĐã quét {len(files)} file — {total} link hỏng.")
    return 1 if total else 0


if __name__ == "__main__":
    raise SystemExit(main())
