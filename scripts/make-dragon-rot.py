"""Generate a Zombie Dragon rot overlay and eyes from the vanilla UV *layout* only.

The shipped rot is now Sable's painted skin; this made the first, procedural one and is kept for
the eyes, which it still produces. Don't write its rot output over the shipped file.

The vanilla texture is read for which pixels are used (alpha, and which are bone-grey); none of its
colours reach the output, so nothing of Mojang's is redistributed. Seeded, so a re-run is identical.

Usage (Pillow is in ./venv - see RELEASE.md):
  unzip -oq ~/.gradle/caches/neoformruntime/artifacts/minecraft_26.3_client.jar \
      'assets/minecraft/textures/entity/enderdragon/*' -d /tmp/vanilla
  ./venv/bin/python -I scripts/make-dragon-rot.py \
      /tmp/vanilla/assets/minecraft/textures/entity/enderdragon/dragon.png \
      /tmp/vanilla/assets/minecraft/textures/entity/enderdragon/dragon_eyes.png \
      src/main/resources/assets/zombiemod/textures/entity/zombie_dragon_rot.png \
      src/main/resources/assets/zombiemod/textures/entity/zombie_dragon_eyes.png \
      /tmp/preview.png
"""
import random, sys, math
from PIL import Image

base = Image.open(sys.argv[1]).convert('RGBA')
eyes = Image.open(sys.argv[2]).convert('RGBA')
out_rot, out_eyes, preview = sys.argv[3], sys.argv[4], sys.argv[5]
W, H = base.size
rnd = random.Random(1028)

# Value noise, two octaves, so rot comes in patches rather than speckle.
def grid(step):
    g = {}
    for y in range(0, H + step, step):
        for x in range(0, W + step, step):
            g[(x, y)] = rnd.random()
    return g
def sample(g, step, x, y):
    x0, y0 = (x // step) * step, (y // step) * step
    tx, ty = (x - x0) / step, (y - y0) / step
    tx, ty = tx * tx * (3 - 2 * tx), ty * ty * (3 - 2 * ty)
    a = g[(x0, y0)] * (1 - tx) + g[(x0 + step, y0)] * tx
    b = g[(x0, y0 + step)] * (1 - tx) + g[(x0 + step, y0 + step)] * tx
    return a * (1 - ty) + b * ty
g1, g2 = grid(12), grid(4)
def noise(x, y):
    return 0.7 * sample(g1, 12, x, y) + 0.3 * sample(g2, 4, x, y)

ROT = [(62, 84, 38), (78, 92, 44), (54, 60, 34), (92, 86, 48), (70, 98, 40), (120, 128, 64)]
rot = Image.new('RGBA', (W, H), (0, 0, 0, 0))
px = rot.load()
for y in range(H):
    for x in range(W):
        r, g, b, a = base.getpixel((x, y))
        if a == 0:
            continue
        lum = (r + g + b) / 3
        n = noise(x, y)
        wing = x < 58 and 88 <= y < 202
        if lum > 80:                       # bone: yellowed, not green
            px[x, y] = (190, 178, 128, 115)
        elif wing and n > 0.68:            # torn membrane: dark, ragged
            px[x, y] = (14, 18, 10, 245)
        elif n > 0.66:                     # rot
            c = ROT[rnd.randrange(len(ROT))]
            px[x, y] = (*c, 225)
        else:                              # the wash that turns black skin sick green
            px[x, y] = (40, 62, 26, 95)

# Exposed ribs on the flank block of the body: pale bars with dark gaps, broken by the noise.
for x in range(18, 92, 5):
    for y in range(66, 86):
        if base.getpixel((x, y))[3] and noise(x, y) > 0.35:
            for dx in (0, 1):
                px[x + dx, y] = (226, 214, 176, 255)
            px[x + 2, y] = (24, 30, 16, 255)

ey = Image.new('RGBA', (W, H), (0, 0, 0, 0))
ep = ey.load()
for y in range(H):
    for x in range(W):
        r, g, b, a = eyes.getpixel((x, y))
        if a:
            ep[x, y] = (150, 255, 70, 255) if g > 60 else (70, 210, 40, 255)

rot.save(out_rot)
ey.save(out_eyes)
# Preview only (never shipped): what a client will composite at render time.
p = Image.alpha_composite(base, rot)
p = Image.alpha_composite(p, ey)
p.resize((W * 3, H * 3), Image.NEAREST).save(preview)
