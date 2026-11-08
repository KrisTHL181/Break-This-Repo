#!/usr/bin/env python3
"""
Subset the Zpix pixel font down to the glyphs this site actually needs,
then emit an inline base64 copy so `file://` opens correctly (browsers
block cross-origin font fetches from file://).

    python3 tools/subset-font.py            # build
    python3 tools/subset-font.py --report   # list missing chars

Re-run this after editing copy in js/config.js or index.html.
"""
import base64
import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parent.parent
SRC = ROOT / "assets/fonts/zpix.woff2"
OUT = ROOT / "assets/fonts/zpix-subset.woff2"
CSS = ROOT / "css/fonts.css"

SCAN = ["index.html", "js/config.js", "js/main.js", "js/scene.js", "js/audio.js", "README.md"]

# Always keep: ASCII, CJK punctuation, fullwidth forms, common symbols,
# and the digits/letters the UI builds at runtime.
EXTRA = (
    "".join(chr(c) for c in range(0x20, 0x7F))
    + "　、。〈〉《》「」『』【】〔〕・ー―‐–—…‥‘’“”※†‡•‰′″¥￥§¶"
    + "←↑→↓↔↕★☆♥■□▲▼◆◇○●◎♪"
    + "①②③④⑤⑥⑦⑧⑨⑩"
    + "０１２３４５６７８９ＡＢＣＤＥＦＧＨＩＪＫＬＭＮＯＰＱＲＳＴＵＶＷＸＹＺ"
)

# The 300 most common Chinese characters — cheap insurance so that small
# copy edits don't produce tofu boxes.
COMMON = (
    "的一是了我不人在他有这个上们来到时大地为子中你说生国年着就那和要她出也得里后自以会家可下而过天去能对小多然于心学么之都好看起发当没成只如事把还用第样道想作种开美总从无情己面最女但现前些所同日手又行意动方期它头经长儿回位分爱老因很给名法间斯知世什两次使身者被高已亲其进此话常与活正感"
)


def collect():
    chars = set(EXTRA) | set(COMMON)
    for rel in SCAN:
        p = ROOT / rel
        if not p.exists():
            continue
        chars |= set(p.read_text(encoding="utf-8"))
    # drop whitespace/control — no glyphs needed
    return {c for c in chars if c.strip() and ord(c) > 0x1F}


def main():
    from fontTools import subset
    from fontTools.ttLib import TTFont

    text = collect()
    print(f"glyph set: {len(text)} characters")

    opts = subset.Options()
    opts.flavor = "woff2"
    opts.desubroutinize = False
    opts.layout_features = ["*"]
    opts.notdef_outline = True
    opts.recalc_bounds = True
    opts.drop_tables += ["DSIG"]

    font = subset.load_font(str(SRC), opts)
    subsetter = subset.Subsetter(options=opts)
    subsetter.populate(text="".join(sorted(text)))
    subsetter.subset(font)

    OUT.parent.mkdir(parents=True, exist_ok=True)
    subset.save_font(font, str(OUT), opts)
    font.close()

    raw = OUT.read_bytes()
    b64 = base64.b64encode(raw).decode("ascii")
    full_kb = SRC.stat().st_size / 1024
    sub_kb = len(raw) / 1024
    print(f"full   {full_kb:8.1f} KB")
    print(f"subset {sub_kb:8.1f} KB  ({sub_kb / full_kb * 100:.1f}%)")

    CSS.write_text(
        "/* 由 tools/subset-font.py 生成，请勿手改。\n"
        " * 子集字体直接内联为 base64：浏览器会因 file:// 的 CORS 限制拒绝\n"
        " * 外部字体文件，而子集只有 ~27 KB，内联后一个请求就够，\n"
        " * 双击 index.html 也能正常显示点阵字。\n"
        " * 想换成外部文件，把下面 src 改成\n"
        " *   url('../assets/fonts/zpix-subset.woff2') format('woff2') 即可。 */\n"
        "@font-face {\n"
        "  font-family: 'Zpix';\n"
        "  font-style: normal;\n"
        "  font-weight: normal;\n"
        "  font-display: block;\n"
        f"  src: url(data:font/woff2;base64,{b64}) format('woff2');\n"
        "}\n",
        encoding="utf-8",
    )
    print(f"wrote {CSS.relative_to(ROOT)}  ({CSS.stat().st_size / 1024:.1f} KB)")

    if "--report" in sys.argv:
        cmap = TTFont(str(OUT)).getBestCmap()
        missing = sorted(c for c in text if ord(c) not in cmap)
        print("missing from subset:", "".join(missing) or "(none)")


if __name__ == "__main__":
    main()
