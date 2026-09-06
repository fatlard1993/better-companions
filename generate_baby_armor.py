#!/usr/bin/env python3
"""Armour for the babies, painted onto their own skins.

A baby is not a small adult any more: since 26.3 every animal's baby has a model of its own,
with its own texture layout, so armour painted in the adult's layout lands on the wrong parts of
it - half a plate on a flank, the rest across nothing. This paints each tier fresh onto the baby
body, in the same arrangement the adult wears: plates on the flanks, chest and rear, the blanket
over the back, the belly bare. Where the body is read from is the game's own model class, so a
baby that changes shape next version needs this run again and nothing drawn by hand.

Needs the deobfuscated Minecraft jar loom leaves in the Gradle cache and the Vineflower it
fetches alongside it; both are there after one build.
"""
import glob
import os
import re
import subprocess
import sys
import tempfile
import zipfile

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
NS = "better-companions-justfatlard"
ARMOR = os.path.join(HERE, "src/main/resources/assets", NS, "textures/entity/armor")
TIERS = ("leather", "copper", "iron", "golden", "diamond", "netherite")

# The armour layout -> the game's baby model that layout's babies are drawn with, and the part
# that is the body. Cold and warm cows, pigs and chickens share their babies with the plain ones.
BABIES = {
    "cow": ("BabyCowModel", "body"), "cow_cold": ("BabyCowModel", "body"), "cow_warm": ("BabyCowModel", "body"),
    "pig": ("BabyPigModel", "body"), "pig_cold": ("BabyPigModel", "body"),
    "chicken": ("BabyChickenModel", "body"), "chicken_cold": ("BabyChickenModel", "body"),
    "sheep": ("BabySheepModel", "body"), "goat": ("BabyGoatModel", "body"),
    "panda": ("BabyPandaModel", "body"), "polar_bear": ("BabyPolarBearModel", "body"),
    "fox": ("BabyFoxModel", "body"), "rabbit": ("BabyRabbitModel", "body_r1"),
    "wolf": ("BabyWolfModel", "body"), "cat": ("BabyFelineModel", "body"),
    "axolotl": ("BabyAxolotlModel", "body"), "bee": ("BabyBeeModel", "body"),
    "armadillo": ("BabyArmadilloModel", "body"),
}

# Where the adult cow's flank plate and back blanket sit on its armour: the tier's colours are
# read off those, so the babies wear exactly the adult's paint.
ADULT_FLANK = (18, 14, 28, 32)
ADULT_BACK = (50, 14, 62, 32)


def minecraft_version():
    for line in open(os.path.join(HERE, "gradle.properties")):
        key, sep, value = line.partition("=")
        if sep and key.strip() == "minecraft_version":
            return value.strip()
    sys.exit("no minecraft_version in gradle.properties")


def deobf_jar():
    version = minecraft_version()
    found = glob.glob(os.path.expanduser(
        f"~/.gradle/caches/fabric-loom/minecraftMaven/net/minecraft/minecraft-merged-deobf/{version}/*-deobf-{version}.jar"))
    if not found:
        sys.exit(f"no deobfuscated {version} jar in the loom cache: build once first")
    return found[0]


def vineflower():
    found = glob.glob(os.path.expanduser("~/.gradle/caches/modules-2/files-2.1/org.vineflower/vineflower/*/*/vineflower-*.jar"))
    if not found:
        sys.exit("no Vineflower in the Gradle cache: run a genSources once")
    return max(found, key=os.path.getmtime)


def baby_sources():
    """Every Baby*Model class decompiled, as name -> source."""
    work = tempfile.mkdtemp(prefix="baby-models-")
    inside, outside = os.path.join(work, "in"), os.path.join(work, "out")
    os.makedirs(inside)
    os.makedirs(outside)
    with zipfile.ZipFile(deobf_jar()) as jar:
        for entry in jar.namelist():
            if entry.startswith("net/minecraft/client/model/") and re.search(r"/Baby\w+Model(\$\w+)?\.class$", entry):
                jar.extract(entry, inside)
    subprocess.run(["java", "-jar", vineflower(), "-log=ERROR", inside, outside],
                   check=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    sources = {}
    for path in glob.glob(os.path.join(outside, "**/*.java"), recursive=True):
        sources[os.path.basename(path)[:-5]] = open(path).read()
    return sources


def body_box(source, part):
    """(u, v, w, h, d) of the part's first box, and the texture size."""
    for m in re.finditer(r'addOrReplaceChild\(\s*"(\w+)"\s*,(.*?)PartPose', source, re.S):
        if m.group(1) != part:
            continue
        u = v = 0
        for t in re.finditer(r"\.(texOffs|addBox)\(((?:[^()]|\([^()]*\))*)\)", m.group(2)):
            args = [a.strip().rstrip("F") for a in t.group(2).split(",")]
            if t.group(1) == "texOffs":
                u, v = int(args[0]), int(args[1])
            else:
                if args[0].startswith('"'):
                    args = args[1:]
                w, h, d = (int(float(a)) for a in args[3:6])
                size = re.search(r"LayerDefinition\.create\(\w+,\s*(\d+),\s*(\d+)\)", source)
                return (u, v, w, h, d), (int(size.group(1)), int(size.group(2)))
    sys.exit(f"no box for part {part}")


def shades(image, box):
    """Dark, mid and light of what is painted in this box, by brightness."""
    seen = {}
    for x in range(box[0], box[2]):
        for y in range(box[1], box[3]):
            px = image.getpixel((x, y))
            if px[3] > 0:
                seen[px[:3]] = seen.get(px[:3], 0) + 1
    colours = sorted(seen, key=lambda c: sum(c))
    if not colours:
        sys.exit("nothing painted where the adult's plate should be")
    return colours[0], colours[len(colours) // 2], colours[-1]


def plate(canvas, x, y, w, h, dark, mid, light):
    """A bordered plate filling the face, with a highlight along its top and left."""
    if w <= 0 or h <= 0:
        return
    for i in range(x, x + w):
        for j in range(y, y + h):
            edge = i in (x, x + w - 1) or j in (y, y + h - 1)
            canvas.putpixel((i, j), (*dark, 255) if edge else (*mid, 255))
    if w >= 4 and h >= 4:
        for i in range(x + 1, x + w - 1):
            canvas.putpixel((i, y + 1), (*light, 255))
        for j in range(y + 1, y + h - 1):
            canvas.putpixel((x + 1, j), (*light, 255))


def paint(box, size, metal, blanket):
    """The baby's body as the adult wears it: plates on flanks, chest and rear; blanket on the back."""
    u, v, w, h, d = box
    canvas = Image.new("RGBA", size, (0, 0, 0, 0))
    plate(canvas, u + d, v, w, d, *blanket)             # top: the back, blanket
    plate(canvas, u, v + d, d, h, *metal)               # one flank
    plate(canvas, u + d + w, v + d, d, h, *metal)       # the other flank
    plate(canvas, u + d, v + d, w, h, *metal)           # chest
    plate(canvas, u + 2 * d + w, v + d, w, h, *metal)   # rear
    return canvas


def main():
    sources = baby_sources()
    for tier in TIERS:
        adult = Image.open(os.path.join(ARMOR, "cow", tier + ".png")).convert("RGBA")
        metal = shades(adult, ADULT_FLANK)
        blanket = shades(adult, ADULT_BACK)
        for layout, (model, part) in BABIES.items():
            box, size = body_box(sources[model], part)
            out = os.path.join(ARMOR, layout + "_baby", tier + ".png")
            os.makedirs(os.path.dirname(out), exist_ok=True)
            paint(box, size, metal, blanket).save(out)
            print("wrote", os.path.relpath(out, HERE), box, size)


if __name__ == "__main__":
    main()
