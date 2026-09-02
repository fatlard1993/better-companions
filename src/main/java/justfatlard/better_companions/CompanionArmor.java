package justfatlard.better_companions;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Horse armour, put on any companion.
 *
 * <p>Six tiers already exist, already balanced, already crafted from the obvious things - and they
 * only ever fitted one animal. Nothing new is added here; the existing barding is simply allowed
 * onto anything that walks with you.
 *
 * <p>It goes in the body slot, which is where the game already puts a horse's barding and a wolf's
 * armour, so the protection it gives comes from the item's own attributes rather than from a number
 * invented here. Dropped on death like any other worn thing, so losing a companion loses the
 * armour with it.
 */
public final class CompanionArmor {
	private CompanionArmor() {}

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
		return true;
	}
}
