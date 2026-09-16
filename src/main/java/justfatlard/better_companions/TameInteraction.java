package justfatlard.better_companions;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.core.particles.ParticleTypes;

/**
 * Winning an animal over, and telling it to wait.
 *
 * <p>Crouch and offer it the thing it likes and it decides to come with you. Then an empty hand
 * tells it to stay or to come along again - the same gesture a wolf has always answered to, so there
 * is nothing new to learn.
 *
 * <p>Empty hand specifically, and that matters: a cow still takes a bucket, a sheep still takes
 * shears, a mooshroom still takes a bowl. An animal that stopped doing its job the moment it became
 * a friend would be a poor trade.
 */
public final class TameInteraction {
	private TameInteraction() {}

	public static InteractionResult onUseEntity(Player player, Level level, InteractionHand hand,
			Entity entity, EntityHitResult hit) {
		// Main hand only, and this is load-bearing rather than tidiness. Everything here is server
		// side, so the client never finds out the click meant anything: it tries the main hand,
		// sees vanilla do nothing, and tries the off hand too. Both reach here, and an action that
		// toggles ran twice - sit, then immediately stand again. The animal ended up where it
		// started, the only message anyone ever saw was the second one, and the whole feature read
		// as if it did not work.
		if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;

		if (!(level instanceof ServerLevel serverLevel)) return InteractionResult.PASS;
		if (!(entity instanceof Mob mob)) return InteractionResult.PASS;

		ItemStack held = player.getItemInHand(hand);

		// An empty hand: crouched, on your own companion, it is the one order they all answer -
		// wait, or come along; standing, on any animal, it is a hand laid on them, which is the
		// petting. The crouch is what marks a touch as an order.
		// Except that a mount is ridden by the empty-handed click, as vanilla has it: petting took
		// that click, so nobody could climb onto their own nautilus or horse bare-handed. Petting
		// a mount is the pet key's.
		if (held.isEmpty() && !player.isShiftKeyDown() && isMount(mob)) return InteractionResult.PASS;
		// And an empty hand on an animal on your own lead is how you let it off.
		if (held.isEmpty() && mob.isLeashed() && mob.getLeashHolder() == player) return InteractionResult.PASS;
		if (held.isEmpty()) return player.isShiftKeyDown() ? toggleSitting(player, mob) : pet(player, mob);

		// Barding first: a companion already wearing one tier should take another rather than
		// having the click fall through to whatever else the item might mean.
		InteractionResult armored = CompanionArmor.equip(serverLevel, player, mob, held);
		if (armored != InteractionResult.PASS) return armored;

		if (mob instanceof TamableAnimal) return InteractionResult.PASS;

		return tryTame(serverLevel, player, mob, held);
	}

	/**
	 * Something ridden by clicking on it: a horse of any kind, a nautilus, a happy ghast, anything
	 * wearing a saddle.
	 */
	private static boolean isMount(Mob mob) {
		return mob instanceof net.minecraft.world.entity.animal.equine.AbstractHorse
			|| mob instanceof net.minecraft.world.entity.animal.nautilus.AbstractNautilus
			|| mob instanceof net.minecraft.world.entity.animal.happyghast.HappyGhast
			|| !mob.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.SADDLE).isEmpty();
	}

	/** Anyone may pet any animal. Whose it is decides who it obeys, not who it lets near. */
	private static InteractionResult pet(Player player, Mob mob) {
		if (!Petting.isPettable(mob)) return InteractionResult.PASS;
		if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer && Petting.pet(serverPlayer, mob)) {
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.PASS;
	}

	private static InteractionResult toggleSitting(Player player, Mob mob) {
		if (!Companions.isOwnedBy(mob, player)) return InteractionResult.PASS;

		boolean sitting = Companions.toggleSitting(mob);
		if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
			serverPlayer.sendOverlayMessage(net.minecraft.network.chat.Component.translatable(
				sitting ? "message.better-companions-justfatlard.staying" : "message.better-companions-justfatlard.following",
				mob.getDisplayName()));
		}

		return InteractionResult.SUCCESS;
	}

	/**
	 * Offer the animal the thing it likes, while crouching.
	 *
	 * <p>Crouching, because the offering an animal likes is nearly always the thing you already
	 * breed it with - wheat for a cow, a carrot for a pig, a slime ball for a frog. Taking the
	 * ordinary click would mean the first wheat handed to each of twenty cows befriended it instead
	 * of breeding it, and a farm would quietly become a herd that follows you home. Feeding stays
	 * exactly what it was; asking the animal to come with you is the deliberate gesture.
	 */
	private static InteractionResult tryTame(ServerLevel level, Player player, Mob mob, ItemStack held) {
		if (!player.isShiftKeyDown()) return InteractionResult.PASS;
		if (Companions.hasOwner(mob)) return InteractionResult.PASS;

		var offering = CompanionSpecies.offeringFor(mob.getType());
		if (offering == null || !held.is(offering)) return InteractionResult.PASS;

		if (!player.isCreative()) held.shrink(1);

		Companions.tame(mob, player);
		CompanionGoals.install(mob);

		level.sendParticles(ParticleTypes.HEART,
			mob.getX(), mob.getY() + mob.getBbHeight(), mob.getZ(), 7, 0.4, 0.3, 0.4, 0.0);
		level.playSound(null, mob.blockPosition(), SoundEvents.NOTE_BLOCK_CHIME.value(),
			SoundSource.NEUTRAL, 1.0F, 1.2F);

		return InteractionResult.SUCCESS;
	}
}
