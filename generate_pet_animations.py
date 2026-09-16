#!/usr/bin/env python3
"""
Write the petting animations: one standing, and one for each pose the mod holds an animal in, and
the stroke the petting player's arm makes.

Pandorical plays one animation on an entity at a time, so a pet played on an animal the mod has
sitting would replace the sitting and stand it up. The posed versions are that pose, held, with the
same fuss on top: they end still sitting or lying, because an animation that does not loop keeps
its last frame.

Usage: python3 generate_pet_animations.py
"""
import json
import pathlib

HERE = pathlib.Path(__file__).parent
ANIMATIONS = HERE / "src/main/resources/assets/better-companions-justfatlard/animations"

LENGTH = 1.0

# The head tips one way and the other, the way a dog leans into a hand.
HEAD = [(0.0, (0, 0, 0)), (0.2, (-10, 0, 14)), (0.45, (-10, 0, -14)), (0.7, (-8, 0, 10)), (1.0, (0, 0, 0))]

# Two little hops, standing only: a sitting animal stays sat.
HOP = [(0.0, (0, 0, 0)), (0.15, (0, 2, 0)), (0.3, (0, 0, 0)), (0.45, (0, 1.5, 0)), (0.6, (0, 0, 0))]

# A wag, for whatever has a tail: a wolf's and a fox's are "tail", a cat's starts at "tail1".
WAG = [(0.0, (0, 0, 0)), (0.15, (0, 30, 0)), (0.3, (0, -30, 0)), (0.45, (0, 30, 0)),
       (0.6, (0, -30, 0)), (0.75, (0, 20, 0)), (0.9, (0, 0, 0))]
TAILS = ("tail", "tail1")


# The petting hand: reaches forward and down, strokes back and forth twice, and comes home. Negative
# x swings an arm forward, as vanilla's own poses do.
STROKE = [(0.0, (0, 0, 0)), (0.15, (-55, 0, 0)), (0.35, (-45, -8, 0)), (0.55, (-58, 6, 0)),
          (0.75, (-46, -6, 0)), (1.0, (0, 0, 0))]


def channel(target, frames, base=(0, 0, 0)):
    return {"target": target, "keyframes": [
        {"time": t, "value": [round(b + v, 3) for b, v in zip(base, value)], "interpolation": "catmullrom"}
        for t, value in frames]}


def held(value):
    """A pose's last frame, kept for the whole animation."""
    return [(0.0, (0, 0, 0)), (LENGTH, (0, 0, 0))], tuple(value)


def pet(pose=None):
    bones = {}
    if pose is not None:
        # Every channel of the pose, stood still at where the pose ends.
        for bone, channels in pose["bones"].items():
            for ch in channels:
                frames, base = held(ch["keyframes"][-1]["value"])
                bones.setdefault(bone, []).append(channel(ch["target"], frames, base))

    def add(bone, target, frames):
        for ch in bones.get(bone, []):
            if ch["target"] == target:
                base = tuple(ch["keyframes"][-1]["value"])
                bones[bone] = [c for c in bones[bone] if c is not ch]
                bones[bone].append(channel(target, frames, base))
                return
        bones.setdefault(bone, []).append(channel(target, frames))

    add("head", "rotation", HEAD)
    if pose is None:
        add("body", "position", HOP)
    for tail in TAILS:
        add(tail, "rotation", WAG)
    return {"length": LENGTH, "looping": False, "bones": bones}


def main():
    (ANIMATIONS / "pet.json").write_text(json.dumps(pet(), indent="\t") + "\n")
    stroke = {"length": LENGTH, "looping": False, "bones": {"right_arm": [channel("rotation", STROKE)]}}
    (ANIMATIONS / "pet_stroke.json").write_text(json.dumps(stroke, indent="\t") + "\n")
    for pose, name in (("sit", "pet_sitting"), ("lie_down", "pet_resting")):
        source = json.loads((ANIMATIONS / f"{pose}.json").read_text())
        (ANIMATIONS / f"{name}.json").write_text(json.dumps(pet(source), indent="\t") + "\n")
    print(f"  pet, pet_sitting, pet_resting, pet_stroke -> {ANIMATIONS.relative_to(HERE)}")


if __name__ == "__main__":
    main()
