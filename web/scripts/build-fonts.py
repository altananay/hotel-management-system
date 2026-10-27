"""Build the three self-hosted WOFF2 files used by the public web app.

The source fonts are the SIL OFL licensed families published in the Google
Fonts repository.  This script is a development-time asset tool; it is not
part of the application runtime or the CI build.

Requirements:
    python -m pip install fonttools brotli

Usage:
    python web/scripts/build-fonts.py
"""

from __future__ import annotations

import io
import sys
import urllib.request
from pathlib import Path

from fontTools.subset import Options, Subsetter
from fontTools.ttLib import TTFont
from fontTools.varLib.instancer import instantiateVariableFont

ROOT = Path(__file__).resolve().parents[1]
OUT_DIR = ROOT / "public" / "fonts"
TMP_DIR = ROOT / ".font-build"

GF_BASE = "https://raw.githubusercontent.com/google/fonts/main/ofl"
SOURCES = {
    "cormorant-garamond": {
        "url": f"{GF_BASE}/cormorantgaramond/CormorantGaramond%5Bwght%5D.ttf",
        "license": f"{GF_BASE}/cormorantgaramond/OFL.txt",
        "license_file": "OFL-CormorantGaramond.txt",
        "weights": [600],
        "output": "cormorant-garamond-{weight}.woff2",
    },
    "source-sans-3": {
        "url": f"{GF_BASE}/sourcesans3/SourceSans3%5Bwght%5D.ttf",
        "license": f"{GF_BASE}/sourcesans3/OFL.txt",
        "license_file": "OFL-SourceSans3.txt",
        "weights": [400, 600],
        "output": "source-sans-3-{weight}.woff2",
    },
}

# Basic Latin + Latin Extended-A/B, general punctuation, currency symbols and
# the characters used by the approved copy deck (Turkish included).
UNICODES = "U+0000-024F,U+2000-206F,U+20AC,U+20BA,U+2122,U+2192"
TURKISH = "İıĞğŞşÇçÖöÜü"
EXPECTED_TOTAL_MAX = 204_800


def download(url: str, destination: Path) -> None:
    if destination.exists():
        return
    destination.parent.mkdir(parents=True, exist_ok=True)
    request = urllib.request.Request(url, headers={"User-Agent": "hotel-web-font-builder/1.0"})
    with urllib.request.urlopen(request) as response:
        destination.write_bytes(response.read())


def build_weight(source: Path, weight: int, destination: Path) -> int:
    font = TTFont(source)
    instantiateVariableFont(font, {"wght": weight}, inplace=True)

    options = Options()
    options.flavor = "woff2"
    options.layout_features = ["*"]
    options.name_IDs = ["*"]
    options.name_languages = ["*"]
    options.notdef_outline = True
    options.recalc_bounds = True
    options.recalc_timestamp = False

    subsetter = Subsetter(options=options)
    subsetter.populate(unicodes=range(0x0000, 0x0250))
    subsetter.populate(unicodes=range(0x2000, 0x2070))
    subsetter.populate(unicodes=[0x20AC, 0x20BA, 0x2122, 0x2192])
    subsetter.subset(font)

    cmap = font.getBestCmap() or {}
    missing = [character for character in TURKISH if ord(character) not in cmap]
    if missing:
        raise RuntimeError(f"{destination.name}: missing Turkish glyphs {missing!r}")

    destination.parent.mkdir(parents=True, exist_ok=True)
    font.flavor = "woff2"
    font.save(destination)
    font.close()
    return destination.stat().st_size


def main() -> int:
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    TMP_DIR.mkdir(parents=True, exist_ok=True)

    total = 0
    for family, config in SOURCES.items():
        source = TMP_DIR / f"{family}.ttf"
        download(config["url"], source)
        download(config["license"], OUT_DIR / config["license_file"])
        for weight in config["weights"]:
            destination = OUT_DIR / config["output"].format(weight=weight)
            size = build_weight(source, weight, destination)
            total += size
            print(f"{destination.name}: {size:,} B")

    print(f"total woff2: {total:,} B (limit {EXPECTED_TOTAL_MAX:,} B)")
    if total > EXPECTED_TOTAL_MAX:
        print("font budget exceeded", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
