# Better Companions

Sixteen more animals can be befriended, and will follow, wait and fight alongside you the way only a
wolf could before.

Server-side; Pandorical carries the client's half (the whistle keybind). Nothing is replaced: a
companion cow is still a cow, with a note attached saying whose it is, so everything else that knows
about cows keeps working.

## Befriending

**Crouch** and offer an animal the thing it already likes:

| | | | |
|---|---|---|---|
| Fox — sweet berries | Panda — bamboo | Pig — carrot | Rabbit — carrot |
| Chicken — wheat seeds | Cow — wheat | Mooshroom — wheat | Sheep — wheat |
| Goat — wheat | Ocelot — cod | Armadillo — spider eye | Bee — honeycomb |
| Frog — slimeball | Axolotl — tropical fish | Polar bear — salmon | Snow golem — snowball |

The offering is the one the game already gave the animal an opinion about, so there is no second
table to learn. With **block-tip** installed, looking at an animal tells you what it wants.

Crouching is what separates befriending from feeding, and it has to, because for most of this table
they are the same item: wheat breeds a cow, a carrot breeds a pig, a slime ball breeds a frog. On an
ordinary click the animal is fed exactly as it always was. Without the crouch, the first wheat handed
to each of twenty cows would befriend it rather than breed it, and a farm would quietly become a herd
that follows you home.

Told to wait, an animal the game already has a sitting pose for uses its own; everything else is
posed by the mod. Those poses name the bones of more than one body plan, because a frog's legs and a
cow's are spelled differently and an animation skips a bone the model has not got - so a pose aimed
at the wrong parts does not look broken, it looks like nothing happened at all.

**Wolves, cats and parrots are not on the list** — the game already tames those. They are companions
here too and everything below applies to them; they simply arrive already tamed.

## Living with one

- **Empty hand** tells a companion to wait, or to come along again. The same gesture a wolf has
  always answered to. It is specifically an *empty* hand, so a cow still takes a bucket, a sheep
  still takes shears and a mooshroom still takes a bowl.
- **They fight for you.** A companion takes your side whether you started it or not — but never
  against a friend.
- **They settle in.** After a minute of waiting, a companion stops standing to attention: it
  potters about within a few blocks of where you left it and lies down for a while at a time. It is
  still where you left it when you come back.
- **`/companions whistle`**, or the keybind (V by default, rebindable), calls everyone in. Anything
  further than shouting distance is put down nearby and walks the rest, so they arrive walking
  rather than appearing at your elbow. A whistle cancels a stay order — the newer instruction wins.

## Petting

**The pet keybind**, or `/companions pet`, makes a fuss of whichever of your companions you are
looking at: it looks up at you, there are hearts, and it makes its own contented noise - the one
the game already agrees that animal makes, rather than a table of one sound per species that
somebody has to keep in step with every animal ever added.

Looking straight at one always wins. Looking at nothing in particular, the nearest within arm's
reach will do, because being in the middle of your own herd and pressing the button should not
require aim.

It does nothing else, and that is the point. A companion already follows, waits and fights, and
none of that ever gives you a reason to touch it.

The keybind lives in the same pool the whistle does, so it appears in your normal controls screen
under "Pandorical" and rebinds like any other key. A pool slot can arrive unbound, which is why
there is a command for it too.

## Forgiveness

**The first hit is free.** Catching your own animal by accident is the commonest thing that happens
in a crowded pen, and a companion that turns on you for it is one you stop keeping. A second blow
within eight seconds is not a slip, and is treated as one.

Grudges live in memory and die with the server: an animal left alone since yesterday has no reason
to still be cross.

## Friends

Nobody on the list is ever a target, whoever swung first.

```
/companions friend add <player>
/companions friend remove <name>
/companions friend list
```

**Vanilla teams count too.** Anyone on the owner's scoreboard team is a friend automatically —
reading the game's own answer rather than keeping a second list that means the same thing, which
gets plugins and anything else that sets teams for free. So does anyone else's companion whose
owner is a friend: turning on a friend's dog is the same unkindness as turning on the friend.

The friends list is one list for the server, not one per player. A companion mauling the person who
runs the place is everybody's problem.

## Companion armour

Horse armour fits any companion. Six tiers already exist, already balanced, already crafted from the
obvious things, and they only ever fitted one animal — so nothing new is added, the existing barding
is simply allowed onto anything that walks with you. Right-click a companion holding it; take it
back with `/companions unequip`.

It sits in the same body slot the game uses for a horse's barding and a wolf's armour, so the
protection comes from the item's own attributes rather than a number invented here, and it drops
when the companion does.

## Poses

A waiting companion sits up; a settled one lies down. Where the game already drew a pose it is used
— a fox curls, a panda and the game's own tamed animals sit — because those were drawn for that
animal. Everything else gets one of the mod's own animations, played through Pandorical, naming the
parts vanilla's four-legged models all share, so the same two poses serve a cow, a pig, a sheep and
a goat without knowing which is which.

## What this does not do

- **Armour is not drawn on most companions.** It equips and it protects, but only horses and wolves
  have a model for wearing something. Drawing it on the other shapes is an art and rendering job,
  not a server one.
- **Horse armour is still called horse armour.** Renaming a vanilla item means overriding its
  translation on every client, which is a different mechanism from the asset syncing this mod uses.

## Pandorical

Better Companions uses Pandorical for the whistle keybind, for the sit and lie-down animations on
animals the game never drew a pose for, and for the riding tweaks (two riders, free look).

**The Pandorical mod must be installed client-side** for those. Without it a companion still
follows, waits and fights, but the whistle has no key, an animal without a vanilla pose stands
still instead of settling, and the riding tweaks are off. Block Tip, if installed, names a
companion and its owner when you look at it.

## Installation

Install server-side alongside its declared dependencies (see `fabric.mod.json`); connecting clients
need only Pandorical. Version targets live in `gradle.properties` (Minecraft, loader, Fabric API) and
`fabric.mod.json` (Java).

## License

MIT, see [LICENSE](LICENSE).
