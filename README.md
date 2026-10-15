# Better Companions

Sixteen more animals can be befriended, and will follow, wait and fight alongside you the way a wolf does. A companion cow is still a cow, so everything else that knows about cows keeps working with it.

## Befriending

**Crouch** and offer an animal the thing it already likes:

| | | | |
|---|---|---|---|
| Fox - sweet berries | Panda - bamboo | Pig - carrot | Rabbit - carrot |
| Chicken - wheat seeds | Cow - wheat | Mooshroom - wheat | Sheep - wheat |
| Goat - wheat | Ocelot - cod | Armadillo - spider eye | Bee - honeycomb |
| Frog - slimeball | Axolotl - tropical fish | Polar bear - salmon | Snow golem - snowball |

It has to be a crouch: for most of the table the offering is also what breeds the animal, and an ordinary click still feeds it exactly as before. The same item mends the animal once it is yours.

**Wolves, cats and parrots** are tamed the way the game already tames them. They are companions here too, and everything below applies to them.

## Controls

- **Crouch and click with an empty hand** on your own companion: wait here, or come along again. One order for every companion, a wolf included. A cow still takes a bucket, a sheep shears and a mooshroom a bowl, because the order needs an empty hand.
- **Plain click with an empty hand** on any animal: pet it (below). On a mount (a horse, a nautilus, a happy ghast, anything saddled) that click rides it instead, and on an animal on your own lead it lets the lead off; use the pet key or command for those.
- **Plain click with its offering** on a hurt companion, anyone's: two hearts back. At full health the click means what it always meant, so a pen of companions is farmed as before.
- **Click with horse armour**: puts it on (see Companion armour).
- **V** whistles. A second key pets. Both are in your controls screen under "Pandorical" and rebind like any other; the pet key may arrive unbound, so bind it there or use the command.

| Command | Does |
|---|---|
| `/companions whistle` | Calls every companion you have |
| `/companions pet` | Pets the animal you are looking at, or the nearest within reach |
| `/companions unequip` | Takes the armour off your nearest companion within 8 blocks |
| `/companions release` | Lets your nearest companion within 8 blocks go for good |
| `/companions friend add <player>` | Adds a friend |
| `/companions friend remove <name>` | Removes one |
| `/companions friend list` | Lists yours |
| `/companions friend all` | Operators only: everyone online befriends everyone, both ways |

Whistle, Unequip, Release and Friend are also offered as action-menu buttons.

## Following, waiting and fighting

- **They fight for you**, whether you started it or not, and fight back when something hits them. Never against a friend (see Friends).
- **They settle in.** A waiting companion sits. After five minutes it potters about within a few blocks of where you left it and lies down now and then. It is still there when you come back. A dog or cat told to sit does the same, and stays told to sit, so it neither follows nor fights.
- **They come through portals with you.** Companions following you within 16 blocks of where you step through come out beside you on the far side. One told to wait, on a lead, or with a rider stays behind.
- **The whistle reaches all of them**, anywhere in the world or in another dimension. Those already within 12 blocks stay where they stand; the rest are put down nearby and walk the last stretch. One on a lead or carrying a rider stays put. A whistle cancels a stay order.

The whistle only knows about a companion once it has been loaded with this mod installed. One left somewhere before that and not visited since will not answer; walk past it once and it will.

## Petting

Petting works on any animal, yours, anyone's or nobody's. Your hand strokes it, it tips its head, hops, wags whatever tail it has, shows hearts and makes its own contented noise. Looking straight at an animal picks that one; otherwise the nearest within 4 blocks does. A sitting or lying companion stays sitting or lying through it. It has no other effect.

## Releasing

`/companions release`, or the Release button, lets a companion go. The owner, the stay order and its line in the list all go, and any armour it wore comes back to you wherever you are. Offer it the same thing again and it comes back; a wolf, cat or parrot wants what the game asks for.

To let go of one that is not beside you, use the companion list: the Better Companions page of the mods menu lists everything that follows you, nearest first and this dimension first (`Rex - nearby`, `Cow - 312 blocks away`, `Panda - the nether`). Each line has a button that releases that one, including companions asleep in unloaded chunks. **The button has no second press**: a misclick costs you that companion.

## Getting hit

A companion never fights back against its owner or a friend. Anyone else gets the first hit free. A second hit from the same player within 8 seconds of the first and the companion fights back against them. Any other mob that hits it is answered straight away.

Grudges are forgotten when the server restarts.

## Friends

Nobody on your friends list is ever a target of your companions, whoever swung first. Friends also include:

- anyone on your vanilla scoreboard team, however the team was set;
- anyone else's companion whose owner is your friend.

Each player keeps their own list. It is also on the Better Companions page of the mods menu, where you can take a friend off.

These rules apply to the game's own tamed wolves, cats and parrots too: they keep their vanilla following and sitting, but never go for a friend.

## Farm animals

Your companions join in when you hit a cow. Two things keep them out of it:

- An animal you have fed in the last five minutes is left alone, whatever you do to it.
- **Leave passive mobs alone**, a switch on the Better Companions page of the mods menu, keeps your companions out of fights with animals, villagers and golems altogether. Off by default, and each player's own. Companions still defend themselves when one of those attacks them.

## Companion armour

Horse armour, in all six tiers, fits any companion. Right-click a companion with it to put it on; take it off with `/companions unequip`. Protection comes from the item's own stats, and it drops when the companion dies. A sheep's wool covers it until the sheep is sheared.

On a snow golem it is a coat as well: an armoured snow golem does not melt in warm biomes or in the rain.

With Pandorical on the client, the armour is called Leather through Netherite Companion Armor, has its own icon, and is drawn on the animal wearing it, babies included. It is painted onto the skin, the way a wolf's collar is, rather than modelled standing off the body.

## With other mods

- [block-tip](https://github.com/fatlard1993/block-tip): looking at a wild animal tells you what befriends it; looking at a companion says whether it is yours and whether it is waiting, and what will mend it if it is hurt.
- [village-quests](https://github.com/fatlard1993/village-quests): shepherds, farmers, butchers and cartographers remark on whatever animal is following you. Wolves, cats and parrots do not earn a comment.

## Pandorical

Better Companions goes on the server, and **Pandorical is required there**. It adds no new items, so a vanilla client can connect: its companions still follow, wait, fight and wear armour. Without Pandorical on the client, though, there are no whistle or pet keys (the commands still work), animals without a vanilla pose stand instead of sitting or lying down, the armour looks like horse armour and is not drawn on the animal, the riding tweaks (two riders on a mount, free look) are off, and there is no mods menu for the companion list, friends list or passive-mobs switch.

## Development

Installing is in [DEVELOPMENT.md](DEVELOPMENT.md).

## License

MIT, see [LICENSE](LICENSE).
