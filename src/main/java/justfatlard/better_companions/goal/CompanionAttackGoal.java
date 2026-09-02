package justfatlard.better_companions.goal;

import justfatlard.better_companions.Companions;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.server.level.ServerLevel;

/**
 * Actually landing the blow, for animals that have never thrown one.
 *
 * <p>A cow has no attack damage attribute at all - passive animals are not given one - so the usual
 * melee goal would walk it up to a creeper and headbutt it for nothing. This deals the damage
 * itself, which is also the only place the amount can be decided: it comes from the animal's size
 * rather than from an attribute nobody gave it.
 *
 * <p>Anything that already knows how to fight keeps its own attack; this is only added to the ones
 * that do not.
 */
public class CompanionAttackGoal extends MeleeAttackGoal {

	private final PathfinderMob companion;
	private final float damage;

	public CompanionAttackGoal(PathfinderMob companion, double speed, float damage) {
		super(companion, speed, true);
		this.companion = companion;
		this.damage = damage;
	}

	@Override
	public boolean canUse() {
		// A companion told to wait does not go looking for a fight, though it will finish one that
		// comes to it once it has settled.
		if (Companions.isSitting(companion) && !StayGoal.hasSettled(companion)) return false;

		return super.canUse();
	}

	@Override
	protected void checkAndPerformAttack(LivingEntity target) {
		if (!canPerformAttack(target)) return;

		resetAttackCooldown();
		companion.swing(net.minecraft.world.InteractionHand.MAIN_HAND,
			net.minecraft.world.item.component.SwingAnimation.DEFAULT, true);

		if (companion.level() instanceof ServerLevel level) {
			DamageSource source = companion.damageSources().mobAttack(companion);
			target.hurtServer(level, source, damage);
		}
	}
}
