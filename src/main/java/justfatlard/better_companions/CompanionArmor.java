package justfatlard.better_companions;

import justfatlard.pandorical.api.PandoricalApi;
import justfatlard.pandorical.api.VanillaItemOverride;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.armadillo.Armadillo;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.animal.chicken.ChickenVariant;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.cow.CowVariant;
import net.minecraft.world.entity.animal.cow.MushroomCow;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.animal.feline.Ocelot;
import net.minecraft.world.entity.animal.fox.Fox;
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.animal.golem.SnowGolem;
import net.minecraft.world.entity.animal.panda.Panda;
import net.minecraft.world.entity.animal.parrot.Parrot;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.animal.pig.PigVariant;
import net.minecraft.world.entity.animal.polarbear.PolarBear;
import net.minecraft.world.entity.animal.rabbit.Rabbit;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Companion armour, which is horse armour allowed onto any companion.
 *
 * <p>Six tiers already exist, already balanced, already crafted from the obvious things - and they
 * only ever fitted one animal. Nothing new is added here; the existing barding is simply allowed
 * onto anything that walks with you, and renamed and redrawn on Pandorical clients so that it
 * stops saying "horse" while it hangs off a cow.
 *
 * <p>It goes in the body slot, which is where the game already puts a horse's barding and a wolf's
 * armour, so the protection it gives comes from the item's own attributes rather than from a number
 * invented here. Dropped on death like any other worn thing, so losing a companion loses the
 * armour with it.
 */
public final class CompanionArmor {
	private CompanionArmor() {}

	/** Vanilla's tiers, by the prefix each carries in its id and its name. */
	private static final String[] TIERS = {"leather", "copper", "iron", "golden", "diamond", "netherite"};

	/**
	 * Make the barding look like what it is here.
	 *
	 * <p>The item stays vanilla's: same id, same recipe, same protection, and a vanilla client
	 * still sees horse armour. What a Pandorical client sees is a name that says companion and an
	 * icon that is a plate over a back rather than a horse. The icons are this mod's own files;
	 * so are the item definitions that point vanilla's ids at them, shipped under the
	 * {@code minecraft} namespace because that is where the game looks for them.
	 */
	public static void dress() {
		for (String tier : TIERS) {
			String name = Character.toUpperCase(tier.charAt(0)) + tier.substring(1) + " Companion Armor";
			PandoricalApi.content().overrideVanillaItem("minecraft:" + tier + "_horse_armor",
				new VanillaItemOverride().name(name));
		}
		PandoricalApi.content().registerModAssets(Main.MOD_ID);
	}

	/** Vanilla's barding, by the suffix they all share. */
	public static boolean isCompanionArmor(ItemStack stack) {
		var id = BuiltInRegistries.ITEM.getKey(stack.getItem());
		return id != null && id.getPath().endsWith("_horse_armor");
	}

	/** Whether this animal is one that can wear it. */
	public static boolean canWear(Mob mob) {
		return Companions.hasOwner(mob) && mob.canUseSlot(EquipmentSlot.BODY);
	}

	public static ItemStack worn(Mob mob) {
		return mob.getItemBySlot(EquipmentSlot.BODY);
	}

	/**
	 * Put armour on a companion, swapping out whatever it was wearing.
	 *
	 * @return whether anything happened
	 */
	public static InteractionResult equip(ServerLevel level, Player player, Mob mob, ItemStack held) {
		if (!isCompanionArmor(held) || !canWear(mob)) return InteractionResult.PASS;
		if (!Companions.isOwnedBy(mob, player)) return InteractionResult.PASS;

		ItemStack previous = worn(mob).copy();
		if (ItemStack.isSameItemSameComponents(previous, held)) return InteractionResult.PASS;

		mob.setItemSlot(EquipmentSlot.BODY, held.copyWithCount(1));
		// Worn armour is the owner's property, not loot the game may decide to eat.
		mob.setDropChance(EquipmentSlot.BODY, 1.0F);
		show(mob);

		if (!player.isCreative()) held.shrink(1);
		if (!previous.isEmpty()) player.getInventory().placeItemBackInInventory(previous, net.minecraft.util.Prediction.SERVER_ONLY);

		level.playSound(null, mob.blockPosition(), SoundEvents.HORSE_ARMOR.value(),
			SoundSource.NEUTRAL, 0.7F, 1.0F);

		return InteractionResult.SUCCESS;
	}

	/** Take it back off. Returns whether there was anything to take. */
	public static boolean unequip(Player player, Mob mob) {
		ItemStack worn = worn(mob);
		if (worn.isEmpty()) return false;

		mob.setItemSlot(EquipmentSlot.BODY, ItemStack.EMPTY);
		player.getInventory().placeItemBackInInventory(worn.copy(), net.minecraft.util.Prediction.SERVER_ONLY);
		show(mob);
		return true;
	}

	/**
	 * Draw what the companion is wearing, or stop drawing it.
	 *
	 * <p>The game draws body armour on a horse and nothing else. For everyone else the armour is
	 * painted on through Pandorical's entity overlay: the mob's own model submitted a second time
	 * with a texture in its own layout, the way vanilla paints a wolf's collar. One texture per
	 * layout per tier, the layout chosen here because the server is the one that knows which
	 * animal - and which variant, since a cold cow and a warm one are different models.
	 *
	 * <p>Overlays live only in memory on the client, so this is also called as a companion loads,
	 * re-reading the body slot the game persisted.
	 */
	public static void show(Mob mob) {
		ItemStack worn = worn(mob);
		// Babies too, in their own layout: the overlay re-submits whichever model the renderer
		// is using, and since 26.3 a baby's is its own, with its own skin.
		String layout = isCompanionArmor(worn) ? layoutFor(mob) : null;
		if (layout == null) {
			PandoricalApi.entityOverlays().clear(mob);
			return;
		}
		String tier = BuiltInRegistries.ITEM.getKey(worn.getItem()).getPath().replace("_horse_armor", "");
		PandoricalApi.entityOverlays().set(mob, Identifier.fromNamespaceAndPath(Main.MOD_ID,
			"textures/entity/armor/" + layout + "/" + tier + ".png"));
	}

	/**
	 * Which texture layout this animal's model uses, or null for one nothing was painted for.
	 *
	 * <p>The names are the client model classes', because that is what a layout is: a cow, a cold
	 * cow and a warm cow are three models and three layouts, while a mooshroom is a cow's, and a
	 * cat and an ocelot share one.
	 */
	/**
	 * The shapes whose babies have armour of their own. A baby has been its own model since
	 * 26.3, with its own skin layout, and the adult's paint laid on it landed half on a flank
	 * and half on nothing. These are painted by {@code generate_baby_armor.py} from the game's
	 * own baby models; a shape not here shows a baby bare.
	 */
	private static final java.util.Set<String> BABY_LAYOUTS = java.util.Set.of(
		"cow", "cow_cold", "cow_warm", "pig", "pig_cold", "chicken", "chicken_cold", "sheep", "goat",
		"panda", "polar_bear", "fox", "rabbit", "wolf", "cat", "axolotl", "bee", "armadillo");

	private static String layoutFor(Mob mob) {
		String grown = grownLayoutFor(mob);
		if (grown == null || !mob.isBaby()) return grown;
		return BABY_LAYOUTS.contains(grown) ? grown + "_baby" : null;
	}

	private static String grownLayoutFor(Mob mob) {
		return switch (mob) {
			case Cow cow -> switch (cow.getVariant().value().modelAndTexture().model()) {
				case COLD -> "cow_cold";
				case WARM -> "cow_warm";
				case NORMAL -> "cow";
			};
			case MushroomCow ignored -> "cow";
			case Pig pig -> pig.getVariant().value().modelAndTexture().model() == PigVariant.ModelType.COLD
				? "pig_cold" : "pig";
			case Chicken chicken -> chicken.getVariant().value().modelAndTexture().model() == ChickenVariant.ModelType.COLD
				? "chicken_cold" : "chicken";
			case Sheep ignored -> "sheep";
			case Goat ignored -> "goat";
			case Panda ignored -> "panda";
			case PolarBear ignored -> "polar_bear";
			case Frog ignored -> "frog";
			case SnowGolem ignored -> "snow_golem";
			case Parrot ignored -> "parrot";
			case Axolotl ignored -> "axolotl";
			case Bee ignored -> "bee";
			case Armadillo ignored -> "armadillo";
			case Fox ignored -> "fox";
			case Rabbit ignored -> "rabbit";
			case Wolf ignored -> "wolf";
			case Cat ignored -> "cat";
			case Ocelot ignored -> "cat";
			default -> null;
		};
	}
}
