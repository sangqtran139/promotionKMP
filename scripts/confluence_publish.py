#!/usr/bin/env python3
"""Đẩy bộ tài liệu bàn giao lên Confluence — tạo trang mới hoặc cập nhật trang đã có.

Nguồn là các file .confluence do `scripts/docs_to_confluence.py` sinh ra; script này chỉ mang chúng
lên wiki. Markdown trong repo vẫn là nguồn sự thật: sửa Markdown → sinh lại → đẩy lại.

    python3 scripts/docs_to_confluence.py            # bước 1: sinh markup
    python3 scripts/confluence_publish.py --dry-run   # bước 2: xem sẽ tạo/cập nhật gì
    python3 scripts/confluence_publish.py             # bước 3: đẩy thật

Trang 01 làm trang cha, 16 trang còn lại làm con của nó. Chạy lại lần sau thì **cập nhật đúng trang
cũ** theo tiêu đề, không đẻ trang trùng.

CẤU HÌNH — đặt trong file `.confluence.env` ở gốc repo (đã cho vào .gitignore) hoặc `~/.confluence.env`:

    CONFLUENCE_BASE_URL=https://congty.atlassian.net     # Cloud
    # CONFLUENCE_BASE_URL=https://wiki.noibo.vn          # Server/Data Center
    CONFLUENCE_SPACE=TTCN
    CONFLUENCE_USER=ten@congty.vn                        # Cloud: email; Server PAT: bỏ trống
    CONFLUENCE_TOKEN=xxxxxxxx                            # Cloud: API token; Server: PAT

Lấy API token (Cloud): id.atlassian.com/manage-profile/security/api-tokens
Lấy PAT (Server/DC): ảnh đại diện → Settings → Personal Access Tokens

Chỉ dùng thư viện chuẩn.
"""
from __future__ import annotations

import argparse
import base64
import json
import os
import sys
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from _docs_manifest import DOCS, ROOT, page_title

CONFIG_KEYS = ("CONFLUENCE_BASE_URL", "CONFLUENCE_SPACE", "CONFLUENCE_USER", "CONFLUENCE_TOKEN")


def load_config(explicit: str | None) -> dict[str, str]:
    """Biến môi trường thắng; sau đó tới file config. Token KHÔNG bao giờ được in ra."""
    cfg = {k: os.environ.get(k, "") for k in CONFIG_KEYS}
    candidates = [Path(explicit)] if explicit else [ROOT / ".confluence.env", Path.home() / ".confluence.env"]
    for path in candidates:
        if not path.exists():
            continue
        for line in path.read_text(encoding="utf-8").splitlines():
            line = line.strip()
            if not line or line.startswith("#") or "=" not in line:
                continue
            k, _, v = line.partition("=")
            k, v = k.strip(), v.strip().strip('"').strip("'")
            if k in CONFIG_KEYS and not cfg.get(k):
                cfg[k] = v
        break
    missing = [k for k in ("CONFLUENCE_BASE_URL", "CONFLUENCE_SPACE", "CONFLUENCE_TOKEN") if not cfg[k]]
    if missing:
        sys.exit(
            "Thiếu cấu hình: " + ", ".join(missing) + "\n"
            "Tạo file .confluence.env ở gốc repo (xem docstring đầu file này) rồi chạy lại."
        )
    cfg["CONFLUENCE_BASE_URL"] = cfg["CONFLUENCE_BASE_URL"].rstrip("/")
    return cfg


class Confluence:
    def __init__(self, cfg: dict[str, str]) -> None:
        base = cfg["CONFLUENCE_BASE_URL"]
        # Cloud phục vụ API dưới /wiki; Server/DC thì không. Đoán theo host rồi kiểm lại bằng
        # một lần gọi thật — đoán sai thì mọi request sau đều 404 mà không rõ vì sao.
        self.roots = [base + "/wiki/rest/api", base + "/rest/api"]
        if "atlassian.net" not in base:
            self.roots.reverse()
        self.api = self.roots[0]
        self.space = cfg["CONFLUENCE_SPACE"]
        user, token = cfg["CONFLUENCE_USER"], cfg["CONFLUENCE_TOKEN"]
        if user:                                   # Cloud: email + API token
            raw = f"{user}:{token}".encode()
            self.auth = "Basic " + base64.b64encode(raw).decode()
        else:                                      # Server/DC: Personal Access Token
            self.auth = "Bearer " + token

    # ── HTTP ──
    def _call(self, method: str, path: str, payload: dict | None = None, api: str | None = None) -> dict:
        url = (api or self.api) + path
        data = json.dumps(payload).encode() if payload is not None else None
        req = urllib.request.Request(url, data=data, method=method)
        req.add_header("Authorization", self.auth)
        req.add_header("Accept", "application/json")
        if data:
            req.add_header("Content-Type", "application/json")
        try:
            with urllib.request.urlopen(req, timeout=60) as r:
                body = r.read().decode("utf-8")
                return json.loads(body) if body else {}
        except urllib.error.HTTPError as e:
            detail = e.read().decode("utf-8", "replace")[:400]
            raise RuntimeError(f"{method} {path} → HTTP {e.code}: {detail}") from None
        except urllib.error.URLError as e:
            raise RuntimeError(f"Không nối được {url}: {e.reason}") from None

    def connect(self) -> str:
        """Chốt gốc API và trả về tên người đang đăng nhập — cũng là phép thử token."""
        last = ""
        for api in self.roots:
            try:
                me = self._call("GET", "/user/current", api=api)
                self.api = api
                return me.get("displayName") or me.get("publicName") or me.get("username") or "?"
            except RuntimeError as e:
                last = str(e)
        raise RuntimeError("Không xác thực được với Confluence.\n" + last)

    # ── nội dung ──
    def find(self, title: str) -> dict | None:
        q = urllib.parse.urlencode({"spaceKey": self.space, "title": title, "expand": "version"})
        res = self._call("GET", f"/content?{q}")
        results = res.get("results") or []
        return results[0] if results else None

    def _body(self, wiki: str) -> dict:
        return {"storage": {"value": wiki, "representation": "wiki"}}

    def _to_storage(self, wiki: str) -> dict:
        """Bản nào không nhận representation=wiki thì nhờ server dịch sang storage trước."""
        res = self._call("POST", "/contentbody/convert/storage",
                         {"value": wiki, "representation": "wiki"})
        return {"storage": {"value": res["value"], "representation": "storage"}}

    def create(self, title: str, wiki: str, parent_id: str | None) -> dict:
        payload = {
            "type": "page",
            "title": title,
            "space": {"key": self.space},
            "body": self._body(wiki),
        }
        if parent_id:
            payload["ancestors"] = [{"id": parent_id}]
        try:
            return self._call("POST", "/content", payload)
        except RuntimeError as e:
            if "400" not in str(e):
                raise
            payload["body"] = self._to_storage(wiki)
            return self._call("POST", "/content", payload)

    def update(self, page: dict, title: str, wiki: str, parent_id: str | None) -> dict:
        payload = {
            "id": page["id"],
            "type": "page",
            "title": title,
            "space": {"key": self.space},
            "version": {"number": page["version"]["number"] + 1, "message": "Sinh lại từ Markdown trong repo"},
            "body": self._body(wiki),
        }
        if parent_id:
            payload["ancestors"] = [{"id": parent_id}]
        try:
            return self._call("PUT", f"/content/{page['id']}", payload)
        except RuntimeError as e:
            if "400" not in str(e):
                raise
            payload["body"] = self._to_storage(wiki)
            return self._call("PUT", f"/content/{page['id']}", payload)


def main() -> int:
    ap = argparse.ArgumentParser(description="Đẩy bộ tài liệu bàn giao lên Confluence")
    ap.add_argument("--src", default="dist/docs-confluence", help="thư mục chứa file .confluence")
    ap.add_argument("--config", help="đường dẫn file cấu hình (mặc định .confluence.env)")
    ap.add_argument("--dry-run", action="store_true", help="chỉ báo sẽ tạo/cập nhật gì, không ghi")
    ap.add_argument("--only", nargs="*", metavar="STT", help="chỉ xử lý vài tài liệu, vd: --only 01 04")
    ap.add_argument("--flat", action="store_true", help="không lồng trang con dưới tài liệu 01")
    ap.add_argument("--parent-id", metavar="ID",
                    help="id trang cha có sẵn; cả 17 tài liệu thành con của nó (bỏ qua --flat)")
    args = ap.parse_args()

    src_dir = Path(args.src) if Path(args.src).is_absolute() else ROOT / args.src
    if not src_dir.exists():
        sys.exit(f"Chưa có {src_dir}. Chạy trước: python3 scripts/docs_to_confluence.py")

    cfg = load_config(args.config)
    cf = Confluence(cfg)
    try:
        who = cf.connect()
    except RuntimeError as e:
        sys.exit(f"{e}\n\nKiểm lại CONFLUENCE_BASE_URL (đúng host, không kèm /wiki), token còn hạn, "
                 f"và tài khoản có quyền trên space {cfg['CONFLUENCE_SPACE']}.")
    print(f"Kết nối: {cfg['CONFLUENCE_BASE_URL']}  ·  space {cfg['CONFLUENCE_SPACE']}  ·  {who}")
    print(f"Gốc API: {cf.api}\n")

    wanted = set(args.only or [])
    parent_id: str | None = args.parent_id
    created = updated = skipped = 0

    if args.parent_id:
        try:
            top = cf._call("GET", f"/content/{args.parent_id}")
            print(f"Trang cha: {top['title']} (id {top['id']})\n")
        except RuntimeError as e:
            sys.exit(f"Không đọc được trang cha {args.parent_id}: {e}")

    for src, name, doc_title in DOCS:
        title = page_title(name, doc_title)
        path = src_dir / f"{name}.confluence"
        stt = name.split("_")[0]
        if wanted and stt not in wanted:
            skipped += 1
            continue
        if not path.exists():
            print(f"! bỏ qua {name}: chưa sinh file"); skipped += 1
            continue

        wiki = path.read_text(encoding="utf-8")
        try:
            existing = cf.find(title)
        except RuntimeError as e:
            sys.exit(f"Dừng ở {name}: {e}")
        if args.parent_id:                       # tất cả treo dưới trang cha đã có
            ancestor = args.parent_id
        else:                                    # 01 làm cha, phần còn lại làm con của nó
            ancestor = None if (args.flat or stt == "01") else parent_id

        if args.dry_run:
            what = f"cập nhật (v{existing['version']['number']} → v{existing['version']['number'] + 1})" if existing else "TẠO MỚI"
            print(f"  {stt}  {title:<38} {what}")
            if stt == "01" and existing:
                parent_id = existing["id"]
            continue

        try:
            page = cf.update(existing, title, wiki, ancestor) if existing else cf.create(title, wiki, ancestor)
        except RuntimeError as e:
            # Dừng hẳn thay vì chạy tiếp: đẩy dở dang mà không biết dừng ở đâu còn khó dọn hơn.
            sys.exit(f"Dừng ở {name} ({title}): {e}\n"
                     f"Các trang trước đó đã lên. Sửa xong chạy lại — trang cũ sẽ được cập nhật, "
                     f"không tạo trùng.")
        print(f"  {stt}  {title:<38} {'cập nhật' if existing else 'tạo mới'}  → {page['id']}")
        if stt == "01" and not args.parent_id:
            parent_id = page["id"]
        created += 0 if existing else 1
        updated += 1 if existing else 0

    if args.dry_run:
        print("\n(dry-run — chưa ghi gì lên Confluence)")
    else:
        print(f"\nTạo mới {created} · cập nhật {updated} · bỏ qua {skipped}")
        print(f"Xem tại: {cfg['CONFLUENCE_BASE_URL']}/spaces/{cfg['CONFLUENCE_SPACE']}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
