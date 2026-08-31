#!/usr/bin/env python3
"""Build docs/modrinth-icon.png — the Modrinth project icon.

    ./venv/bin/python scripts/make-modrinth-icon.py

There is no Pillow system-wide on the dev box and PEP 668 blocks pip, so use the venv:
`python3 -m venv venv && ./venv/bin/pip install Pillow`. It is gitignored.

Why this is not the same artwork as the CurseForge icon
-------------------------------------------------------
Modrinth runs a **no-generative-AI policy** over uploaded art, and the shield lockup
(`docs/main-logo-icon.png` — zombie head, spikes, chains) tripped it. The slime banner did not.
So Modrinth gets the banner, padded to a square. Do not switch this back to the shield to match
CurseForge; it will be rejected again.

Two limits it has to clear: **square**, and **under 256 KiB**. A non-square icon gets cropped or
letterboxed, and an oversized one is simply refused.
"""

from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
# slime-logo.png (1118x556), not slime-logo-850.png: the 850px one is cut to CurseForge's
# description-image limit, and we are scaling down, so start from the most pixels available.
SRC = ROOT / "docs" / "slime-logo.png"
DEST = ROOT / "docs" / "modrinth-icon.png"
SIZE = 512
LIMIT = 256 * 1024

src = Image.open(SRC).convert("RGBA")
scale = SIZE / max(src.size)
resized = src.resize((round(src.width * scale), round(src.height * scale)), Image.LANCZOS)

# The banner is about 2:1, so squaring it leaves transparent bands above and below rather than
# cropping the wordmark's ends off.
canvas = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
canvas.paste(resized, ((SIZE - resized.width) // 2, (SIZE - resized.height) // 2))

# FASTOCTREE specifically: it is the quantiser that preserves alpha. The default drops it and the
# icon gains a black box. Quantising buys a sharper icon than shrinking does.
canvas.quantize(colors=256, method=Image.FASTOCTREE).save(DEST, "PNG", optimize=True)

written = DEST.stat().st_size
print(f"{DEST.relative_to(ROOT)}: {SIZE}x{SIZE}, {written:,} bytes")
if written > LIMIT:
    raise SystemExit(f"!! over Modrinth's {LIMIT:,}-byte icon limit — quantise harder or shrink")
