#!/usr/bin/env bash
#
# Chạy toàn bộ test của :promotionLogic trên CẢ HAI target rồi sinh một file báo cáo duy nhất.
#
#   ./scripts/test-report.sh                    # → build/reports/test-report.md
#   ./scripts/test-report.sh --out bao-cao.md   # đổi đường dẫn output
#   ./scripts/test-report.sh --json             # sinh thêm bản .json cho CI
#   ./scripts/test-report.sh --skip-ios         # bỏ target Native (nhanh hơn khi lặp)
#
# Vì sao chạy cả hai target: `commonTest` biên dịch và chạy trên cả JVM (androidHostTest) lẫn
# Kotlin/Native (iosSimulatorArm64Test). Native có khác biệt về thread/lazy-init mà JVM không lộ ra,
# nên "xanh trên JVM" KHÔNG đủ kết luận. Riêng coverage chỉ đo được ở JVM — xem docs/common/TestingGuide.md §5b.
set -euo pipefail

cd "$(dirname "$0")/.."

OUT="promotionLogic/build/reports/test-report.md"
WITH_JSON=false
SKIP_IOS=false

while [[ $# -gt 0 ]]; do
    case "$1" in
        --out)       OUT="${2:?thiếu đường dẫn sau --out}"; shift 2 ;;
        --json)      WITH_JSON=true; shift ;;
        --skip-ios)  SKIP_IOS=true; shift ;;
        -h|--help)   sed -n '3,15p' "$0" | sed 's/^# \{0,1\}//'; exit 0 ;;
        *)           echo "Tham số lạ: $1 (xem --help)" >&2; exit 1 ;;
    esac
done

GRADLE_TASKS=(":promotionLogic:testAndroidHostTest")
[[ "$SKIP_IOS" == false ]] && GRADLE_TASKS+=(":promotionLogic:iosSimulatorArm64Test")
GRADLE_TASKS+=(":promotionLogic:koverXmlReport" ":promotionLogic:koverHtmlReport")

echo "▸ Chạy: ${GRADLE_TASKS[*]}"
# `--continue`: test đỏ vẫn chạy tiếp để báo cáo có đủ số liệu, thay vì dừng ở lỗi đầu tiên.
set +e
./gradlew "${GRADLE_TASKS[@]}" --continue
GRADLE_EXIT=$?
set -e

mkdir -p "$(dirname "$OUT")"

python3 - "$OUT" "$WITH_JSON" "$GRADLE_EXIT" <<'PY'
import glob, json, os, sys, xml.etree.ElementTree as ET
from datetime import datetime, timezone

out_path, with_json, gradle_exit = sys.argv[1], sys.argv[2] == "true", int(sys.argv[3])

# ─── 1. Kết quả test: gộp mọi JUnit XML của mọi target ────────────────────────
suites, total, failed, skipped, time_s = {}, 0, 0, 0, 0.0
failures = []
for f in glob.glob("promotionLogic/build/test-results/**/*.xml", recursive=True):
    target = "iOS (Native)" if "ios" in f.lower() else "Android (JVM)"
    try:
        root = ET.parse(f).getroot()
    except ET.ParseError:
        continue
    t = int(root.get("tests", 0)); fl = int(root.get("failures", 0)) + int(root.get("errors", 0))
    sk = int(root.get("skipped", 0))
    total += t; failed += fl; skipped += sk; time_s += float(root.get("time", 0) or 0)
    s = suites.setdefault(target, {"tests": 0, "failed": 0, "skipped": 0})
    s["tests"] += t; s["failed"] += fl; s["skipped"] += sk
    for tc in root.iter("testcase"):
        for bad in list(tc.findall("failure")) + list(tc.findall("error")):
            failures.append((target, tc.get("classname", "").split(".")[-1], tc.get("name", ""),
                             (bad.get("message") or "").split("\n")[0][:160]))

# ─── 2. Coverage từ Kover ─────────────────────────────────────────────────────
def counters(el):
    d = {}
    for t in ("INSTRUCTION", "BRANCH", "LINE"):
        c = el.find(f"counter[@type='{t}']")
        if c is not None:
            m, cv = int(c.get("missed")), int(c.get("covered"))
            d[t] = (cv, m + cv, (100 * cv / (m + cv)) if m + cv else 100.0)
    return d

cov, packages = {}, []
kover = "promotionLogic/build/reports/kover/report.xml"
if os.path.exists(kover):
    root = ET.parse(kover).getroot()
    cov = counters(root)
    for p in root.findall("package"):
        c = counters(p)
        if "LINE" in c:
            packages.append((c["LINE"][2], p.get("name").replace("/", "."), c))
    packages.sort()

# ─── 3. Kết xuất Markdown ─────────────────────────────────────────────────────
now = datetime.now(timezone.utc).astimezone().strftime("%Y-%m-%d %H:%M:%S %Z")
ok = failed == 0
L = []
L.append("# Test report — :promotionLogic\n")
L.append(f"_Sinh tự động bởi `scripts/test-report.sh` lúc {now}_\n")
L.append(f"\n**Kết quả: {'✅ PASS' if ok else '❌ FAIL'}** — {total} test, {failed} lỗi, {skipped} bỏ qua ({time_s:.1f}s)\n")

L.append("\n## Test theo target\n")
L.append("| Target | Test | Lỗi | Bỏ qua |")
L.append("|---|---:|---:|---:|")
for name in sorted(suites):
    s = suites[name]
    L.append(f"| {name} | {s['tests']} | {s['failed']} | {s['skipped']} |")

if failures:
    L.append("\n## Test lỗi\n")
    L.append("| Target | Class | Test | Thông báo |")
    L.append("|---|---|---|---|")
    for tgt, cls, name, msg in failures:
        L.append(f"| {tgt} | `{cls}` | `{name}` | {msg.replace('|', '\\|')} |")

if cov:
    L.append("\n## Coverage (Kover — đo trên target Android/JVM)\n")
    L.append("| Chỉ số | Đã phủ | Tổng | % |")
    L.append("|---|---:|---:|---:|")
    for t in ("LINE", "BRANCH", "INSTRUCTION"):
        if t in cov:
            cv, tot, pct = cov[t]
            L.append(f"| {t} | {cv} | {tot} | **{pct:.1f}%** |")
    L.append("\n<details><summary>Chi tiết theo package (thấp → cao)</summary>\n")
    L.append("\n| Package | Line | Branch |")
    L.append("|---|---:|---:|")
    for pct, name, c in packages:
        br = f"{c['BRANCH'][2]:.0f}%" if "BRANCH" in c else "—"
        L.append(f"| `{name}` | {pct:.0f}% | {br} |")
    L.append("\n</details>\n")
    L.append("\n> DTO (`@Serializable`) và cầu `expect`/`actual` được loại khỏi phép đo — `equals`/"
             "`hashCode`/`copy` do compiler sinh, đo chúng là đo compiler chứ không phải code ta viết.")
    L.append("\n> Hàm `suspend` biên dịch thành state machine (`Xxx$method$1`), mỗi điểm treo sinh thêm "
             "nhánh dispatch — vì vậy BRANCH luôn thấp hơn LINE một cách cố hữu.")

L.append(f"\n\n## Báo cáo chi tiết\n")
L.append(f"- HTML coverage: `promotionLogic/build/reports/kover/html/index.html`")
L.append(f"- HTML test: `promotionLogic/build/reports/tests/testAndroidHostTest/index.html`")

os.makedirs(os.path.dirname(out_path) or ".", exist_ok=True)
with open(out_path, "w") as fh:
    fh.write("\n".join(L) + "\n")

if with_json:
    jp = os.path.splitext(out_path)[0] + ".json"
    with open(jp, "w") as fh:
        json.dump({
            "generatedAt": now,
            "passed": ok,
            "tests": {"total": total, "failed": failed, "skipped": skipped, "durationSeconds": round(time_s, 2)},
            "targets": suites,
            "coverage": {t: {"covered": v[0], "total": v[1], "percent": round(v[2], 2)} for t, v in cov.items()},
            "failures": [{"target": t, "class": c, "test": n, "message": m} for t, c, n, m in failures],
        }, fh, ensure_ascii=False, indent=2)
    print(f"✓ JSON: {jp}")

print(f"✓ Markdown: {out_path}")
print(f"  {total} test, {failed} lỗi" + (f", LINE {cov['LINE'][2]:.1f}%" if "LINE" in cov else ""))
sys.exit(1 if (failed or gradle_exit != 0) else 0)
PY
