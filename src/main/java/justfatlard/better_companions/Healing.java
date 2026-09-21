package justfatlard.better_companions;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Patching a companion up with the thing it likes.
 *
 * <p>Feeding a hurt wolf has always mended it, and a companion you cannot mend is one you stop
 * taking anywhere - it goes out with you once, comes back on two hearts, and waits by the door for
 * the rest of the world. The offering is the same item that won the animal over in the first place,
 * so there is nothing new to carry and no second table to learn.
 *
 * <p>Only while it is hurt, and that is what keeps this out of everything else's way. At full
 * health the click falls straight through to whatever the item already meant: wheat still breeds a
 * cow, a carrot still breeds a pig, and a pen full of companions is farmed exactly as it was.
 *
 * <p>Anyone's companion, not only your own. Patching up a friend's dog costs you the carrot and
 * does the dog no harm, which is the whole of the argument.
 */
public final class Healing {
	private Healing() {}

	/** What one offering is worth: two hearts, whatever the animal and whatever the item. */
	private static final float PER_OFFERING = 4.0F;

	public static InteractionResult offer(ServerLevel level, Player player, Mob mob, ItemStack held) {
		if (!Companions.hasOwner(mob)) return InteractionResult.PASS;
		if (mob.getHealth() >= mob.getMaxHealth()) return InteractionResult.PASS;
		if (!CompanionSpecies.accepts(mob.getType(), held)) return InteractionResult.PASS;

		mob.heal(PER_OFFERING);
		if (!player.isCreative()) held.shrink(1);

		level.sendParticles(ParticleTypes.HEART,
			mob.getX(), mob.getY() + mob.getBbHeight(), mob.getZ(), 3, 0.3, 0.2, 0.3, 0.0);
		level.playSound(null, mob.blockPosition(), SoundEvents.GENERIC_EAT.value(),
			SoundSource.NEUTRAL, 0.8F, 1.0F + (level.getRandom().nextFloat() - 0.5F) * 0.2F);

		return InteractionResult.SUCCESS;
	}
}
