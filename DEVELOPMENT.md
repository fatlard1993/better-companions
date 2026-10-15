# Better Companions - Development Guide

For what the mod is and how it plays, see [README.md](README.md).

## Installation

Install server-side alongside its declared dependencies (see `fabric.mod.json`); connecting clients
need only Pandorical, and only for the keybinds, the poses and the armour (see the README). Version targets live in `gradle.properties` (Minecraft, loader, Fabric API) and
`fabric.mod.json` (Java).

## Internals

- **Whistle and sleeping chunks.** An animal in a chunk nobody is standing in is in no list the game
  will hand out, so each companion's chunk is written to the roster (`Roster`) when it unloads. A
  whistle, or a release from the mods-menu list, wakes those chunks with a short ticket (`Waking`),
  which is why the whistle's count arrives half a second after the sound.
- **Poses.** Where the game already has a pose it is used; everything else plays the mod's own
  animations through Pandorical. Those name the bones of more than one body plan, because a frog's
  legs and a cow's are named differently and an animation silently skips a bone the model lacks.
- **Armour.** Companion armour is painted onto each animal's skin layout, like vanilla's wolf collar:
  21 shapes, six tiers each, with the cold and warm cow and cold pig and chicken variants as their own
  models. Babies have their own models and skin layouts, so their plates are generated from the game's
  baby models by `generate_baby_armor.py`.
- **Not done:** wolves could use vanilla's raised wolf armour model by adding a wolf layer to the
  horse armour equipment assets. That is one animal on a second path.
