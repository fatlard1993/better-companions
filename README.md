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

- **Sneak and click with an empty hand** tells a companion to wait, or to come along again: one
  order for every animal you have fed into your company, a wolf included. It is specifically an
  *empty* hand and a crouch, so a cow still takes a bucket, a sheep still takes shears, a
  mooshroom still takes a bowl.
- **A plain click with an empty hand** is a hand on them: the petting below, without reaching for
  the key. Not on a mount - a horse, a nautilus, anything saddled - where that click is how you
  climb on; pet a mount with the key.
- **They fight for you.** A companion takes your side whether you started it or not — but never
  against a friend.
- **They settle in.** After five minutes of waiting, a companion stops standing to attention: it
  potters about within a few blocks of where you left it and lies down for a while at a time. It is
  still where you left it when you come back. A dog or a cat told to sit does the same, getting up
  now and then to sit down somewhere else nearby; it is still told to sit, so it neither follows
  nor fights.
- **They come through portals with you.** Companions following you, within sixteen blocks of where
  you step through, come out beside you on the far side. One told to wait stays where it is.
- **`/companions whistle`**, or the keybind (V by default, rebindable), calls everyone in. Anything
  further than shouting distance is put down nearby and walks the rest, so they arrive walking
  rather than appearing at your elbow. A whistle cancels a stay order — the newer instruction wins.

## Petting

**A plain empty-hand click**, the pet keybind, or `/companions pet`, makes a fuss of whichever
animal you are touching or looking at, yours, anyone's or nobody's (on a mount the click rides it,
so a mount is petted with the key or the command, and on an animal on your lead it lets it off):
your hand swings out and strokes it twice, it tips its head, gives a little hop and wags whatever
tail it has, there are hearts, and it makes its own contented noise - the one
the game already agrees that animal makes, rather than a table of one sound per species that
somebody has to keep in step with every animal ever added.

Looking straight at one always wins. Looking at nothing in particular, the nearest within arm's
reach will do, because being in the middle of your own herd and pressing the button should not
require aim.

An animal the mod has sitting or lying down stays sitting or lying through it.

It does nothing else, and that is the point. A companion already follows, waits and fights, and
none of that ever gives you a reason to touch it.

The keybind lives in the same pool the whistle does, so it appears in your normal controls screen
under "Pandorical" and rebinds like any other key. A pool slot can arrive unbound, which is why
there is a command for it too.

## What the village makes of it

With [village-quests](../village-quests) installed, villagers notice. The whole
remark rests on the one fact this mod created that nothing else will comment on:
a cow that follows a person. The shepherd has spent a life getting animals to do
that on purpose and failing. The farmer has spent two seasons on a fence you
walked a pig straight past. The butcher has had a thousand animals through the
shop and not one of them ever chose somebody.

Dogs are exempt, because wolves, cats and parrots were tameable before this mod
and nobody would look twice. The lines name whatever is actually at your heel,
so it is a cow or a polar bear or a bee and never "an animal".

## Forgiveness

**The first hit is free.** Catching your own animal by accident is the commonest thing that happens
in a crowded pen, and a companion that turns on you for it is one you stop keeping. A second blow
within eight seconds is not a slip, and is treated as one.

Grudges live in memory and die with the server: an animal left alone since yesterday has no reason
to still be cross.

## Friends

Nobody on your list is ever a target of your companions, whoever swung first. A stray hit from a
friend in a shared fight is not a reason for your dogs to go for their throat.

```
/companions friend add <player>
/companions friend remove <name>
/companions friend list
/companions friend all        (operators: everyone online befriends everyone, both ways)
```

**Vanilla teams count too.** Anyone on the owner's scoreboard team is a friend automatically —
reading the game's own answer rather than keeping a second list that means the same thing, which
gets plugins and anything else that sets teams for free. So does anyone else's companion whose
owner is a friend: turning on a friend's dog is the same unkindness as turning on the friend.

Each player names their own friends: who you trust around your animals is your call, and a friend
of yours is no promise about anyone else's pack. The list is also on the Better Companions page of
the mod menu, where a friend can be taken off again.

These rules hold for the game's own tamed animals too. A wolf, a cat or a parrot keeps the game's own
following, sitting and taking-your-side, but whatever it is about to go for is checked against your
friends and against the animals you have fed or asked to be left alone. A friend is never a target,
whoever swung first, the same promise the list makes everywhere else. A passive mob that strikes the
wolf itself is still answered: that one is the wolf's own business.

**Farm animals are not a fight.** The pack takes your side against whatever you swing at, and a cow
you are butchering looks like a fight to them. Two things stop it. An animal you have fed in the last
five minutes is left alone whatever you do to it: that is a farm, and the swing is your own business.
And the Better Companions page of the mods menu has a switch, *Leave passive mobs alone*, that keeps
your companions out of fights with animals, villagers and golems altogether. It is off by default
and each player's own. They still fight back when one of those goes for them.

## Companion armour

Horse armour fits any companion. Six tiers already exist, already balanced, already crafted from the
obvious things, and they only ever fitted one animal — so nothing new is added, the existing barding
is simply allowed onto anything that walks with you. Right-click a companion holding it; take it
back with `/companions unequip`.

On a Pandorical client it is called what it is - Leather Companion Armor through Netherite Companion
Armor - and drawn as a plate over a back rather than a horse, in each tier's own colours. Same
item, same recipe, same protection; a vanilla client sees the horse.

**And it is drawn on the animal wearing it.** The game only ever drew barding on a horse; here every
companion wears its armour visibly, a plate over the back and sides with the tier's blanket on top,
painted in the animal's own skin layout so it follows every joint the animal has. Twenty-one
shapes, six tiers each, and the cold and warm cows and the cold pigs and chickens get their own
since they are their own models. It is painted on the skin rather than standing off it, the way vanilla paints a wolf's
collar, and it needs the Pandorical client to be seen. A sheep's wool covers it until the sheep is
sheared. Babies wear it too, in armour of their own: a baby has been its own model since 26.3, with
its own skin layout, so each one's plates are painted onto that layout by `generate_baby_armor.py`
from the game's own baby models, the same plates and blanket at a baby's size.

It sits in the same body slot the game uses for a horse's barding and a wolf's armour, so the
protection comes from the item's own attributes rather than a number invented here, and it drops
when the companion does. On a snow golem it is a coat as well: an armoured golem does not melt in
the heat or the rain, so it can follow you out of the tundra and back.

## Poses

A waiting companion sits up; a settled one lies down. Where the game already drew a pose it is used
— a fox curls, a panda and the game's own tamed animals sit — because those were drawn for that
animal. Everything else gets one of the mod's own animations, played through Pandorical, naming the
parts vanilla's four-legged models all share, so the same two poses serve a cow, a pig, a sheep and
a goat without knowing which is which.

## What this does not do

- **Armour is painted, not modelled.** It sits on the skin rather than standing off the body, so it
  reads as plate rather than as a saddle-shaped slab. Wolves could get vanilla's own raised wolf
  armour model by adding a wolf layer to the horse armour equipment assets; that is one animal on a
  second path, and has not been done.

## Pandorical

Pandorical is required on the server. Better Companions uses it for the whistle and pet keybinds, for the sit and lie-down animations on
animals the game never drew a pose for, for the riding tweaks (two riders, free look), to give the
armour its name and its icon, and to draw it on the animal wearing it.

**The Pandorical mod must be installed client-side** for those. Without it a companion still
follows, waits and fights, but the whistle has no key, an animal without a vanilla pose stands
still instead of settling, and the riding tweaks are off. Block Tip, if installed, says of a
companion you look at whether it is yours and whether it is waiting, and of a wild one what would
befriend it.

## Development

Installing is in [DEVELOPMENT.md](DEVELOPMENT.md).

## License

MIT, see [LICENSE](LICENSE).
