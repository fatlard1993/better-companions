package justfatlard.better_companions;

import java.util.UUID;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;

/**
 * Who belongs to whom, and whether they have been told to wait.
 *
 * <p>Every question here has two answers underneath: one for the animals the game already knows how
 * to tame, which keep their own owner and sitting flag, and one for the animals this mod tames,
 * which carry an attachment. Callers should never have to know which is which - a wolf and a cow
 * are both somebody's companion - so nothing outside this class asks.
 */
public final class Companions {
	private Companions() {}

	public static final AttachmentType<CompanionState> STATE = AttachmentRegistry.createPersistent(
		Identifier.fromNamespaceAndPath(Main.MOD_ID, "state"), CompanionState.CODEC);

	public static CompanionState stateOf(Entity entity) {
		return entity.getAttachedOrElse(STATE, CompanionState.NONE);
	}

	public static void setState(Entity entity, CompanionState state) {
		entity.setAttached(STATE, state);
	}

	/** Who this animal follows, or null if nobody. */
	public static UUID ownerOf(Entity entity) {
		if (entity instanceof TamableAnimal tamable) {
			return tamable.getOwnerReference() == null ? null : tamable.getOwnerReference().getUUID();
		}
		return stateOf(entity).owner().orElse(null);
	}

	public static boolean isOwnedBy(Entity entity, Player player) {
		UUID owner = ownerOf(entity);
		return owner != null && owner.equals(player.getUUID());
	}

	public static boolean hasOwner(Entity entity) {
		return ownerOf(entity) != null;
	}

	/** The owner, if they are loaded and nearby enough to be found. */
	public static Player findOwner(Entity entity) {
		UUID owner = ownerOf(entity);
		return owner == null ? null : entity.level().getPlayerByUUID(owner);
	}

	public static boolean isSitting(Entity entity) {
		if (entity instanceof TamableAnimal tamable) return tamable.isOrderedToSit();

		return stateOf(entity).sitting();
	}

	/**
	 * Tell a companion to wait, or to come along again.
	 *
	 * @return the state it is now in, so the caller can say which happened
	 */
	public static boolean toggleSitting(Mob mob) {
		if (mob instanceof TamableAnimal tamable) {
			boolean sitting = !tamable.isOrderedToSit();
			tamable.setOrderedToSit(sitting);
			tamable.setInSittingPose(sitting);
			return sitting;
		}

		CompanionState state = stateOf(mob);
		boolean sitting = !state.sitting();
		setState(mob, sitting
			? state.sittingAt(mob.level().getGameTime(), mob.blockPosition())
			: state.standing());
		if (sitting) mob.getNavigation().stop();
		return sitting;
	}

	/** Bind an animal to a player, the first time they win it over. */
	public static void tame(Mob mob, Player player) {
		if (mob instanceof TamableAnimal tamable) {
			tamable.tame(player);
		} else {
			setState(mob, stateOf(mob).withOwner(player.getUUID()));
		}
		Awards.befriended(player instanceof net.minecraft.server.level.ServerPlayer owner ? owner : null);
	}

	/** A mob that is not out to get anyone: an animal, a villager, a golem. Never a monster or a person. */
	public static boolean isPassive(LivingEntity mob) {
		return mob instanceof net.minecraft.world.entity.Mob
			&& !(mob instanceof net.minecraft.world.entity.monster.Enemy);
	}

	/** Whether this owner's companions stay out of fights with passive mobs altogether. */
	public static boolean sparesPassive(Player owner) {
		return owner instanceof net.minecraft.server.level.ServerPlayer player
			&& Main.SPARE_PASSIVE != null && Boolean.TRUE.equals(Main.SPARE_PASSIVE.get(player));
	}

	/**
	 * Whether an attack should be taken personally.
	 *
	 * <p>A companion never turns on the person it follows, whatever happens - the forgiveness rules
	 * elsewhere are about strangers, and about the owner they do not apply at all.
	 */
	public static boolean isOwnerOf(LivingEntity attacker, Entity companion) {
		return attacker instanceof Player player && isOwnedBy(companion, player);
	}
}
