package justfatlard.better_companions;

import justfatlard.better_companions.goal.CompanionAttackGoal;
import justfatlard.better_companions.goal.DefendOwnerGoal;
import justfatlard.better_companions.goal.FollowOwnerGoal;
import justfatlard.better_companions.goal.LieDownGoal;
import justfatlard.better_companions.goal.SettledWanderGoal;
import justfatlard.better_companions.goal.RetaliateGoal;
import justfatlard.better_companions.goal.StayGoal;
import justfatlard.better_companions.mixin.MobAccessor;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;

/**
 * Teaching an animal to keep company, without replacing what it already knew.
 *
 * <p>The goals are added to the animal's own lists rather than swapped in for them, so a fox still
 * hunts and a sheep still eats grass; they simply now have somebody to keep up with first. The
 * priorities put waiting above everything, then following, then the rest of the animal's own life.
 *
 * <p>Only ever installed on an animal that has an owner. An untamed cow is a cow, and giving every
 * cow in the world four extra goals to evaluate would be a tax on nothing.
 */
public final class CompanionGoals {
	private CompanionGoals() {}

	/** Told to wait beats everything, including the animal's own idea of what to do next. */
	private static final int STAY_PRIORITY = 0;
	private static final int LIE_DOWN_PRIORITY = 1;
	private static final int SETTLED_WANDER_PRIORITY = 2;
	private static final int FOLLOW_PRIORITY = 6;
	private static final int ATTACK_PRIORITY = 5;

	private static final double FOLLOW_SPEED = 1.1;
	private static final double ATTACK_SPEED = 1.2;
	private static final double WANDER_SPEED = 0.8;

	/**
	 * How hard a companion hits.
	 *
	 * <p>Scaled off how big the animal is, because a chicken and a polar bear should not agree. Low
	 * on purpose: a companion is meant to help with a fight, not to be the reason you win it.
	 */
	private static float damageFor(Mob mob) {
		float size = mob.getBbWidth() * mob.getBbHeight();
		return Math.clamp(1.0F + size * 1.5F, 1.0F, 6.0F);
	}

	public static void install(Mob mob) {
		mob.getGoalSelector().addGoal(STAY_PRIORITY, new StayGoal(mob));

		// Both only ever fire once the animal has settled, which is why they sit above following:
		// a companion that has made itself at home should not be dragged off by the sight of you
		// across the yard, and it is still where you left it when you come back.
		mob.getGoalSelector().addGoal(LIE_DOWN_PRIORITY, new LieDownGoal(mob));

		mob.getGoalSelector().addGoal(FOLLOW_PRIORITY, new FollowOwnerGoal(mob, FOLLOW_SPEED));

		// Melee goals steer a body around, so they need something that knows how to path. Every
		// animal on the roster is one; the check is here so a future addition that is not fails by
		// simply not fighting rather than by throwing on load.
		if (mob instanceof net.minecraft.world.entity.PathfinderMob walker) {
			mob.getGoalSelector().addGoal(SETTLED_WANDER_PRIORITY,
				new SettledWanderGoal(walker, WANDER_SPEED));

			// Anything that already knows how to fight keeps its own attack; this is for the ones
			// with no attack damage attribute at all, which is most of them.
			boolean armed = mob.getAttributes()
				.hasAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);

			mob.getGoalSelector().addGoal(ATTACK_PRIORITY, armed
				? new net.minecraft.world.entity.ai.goal.MeleeAttackGoal(walker, ATTACK_SPEED, true)
				: new CompanionAttackGoal(walker, ATTACK_SPEED, damageFor(mob)));
		}

		var targets = ((MobAccessor) mob).betterCompanions$targetSelector();
		targets.addGoal(1, new DefendOwnerGoal(mob));
		targets.addGoal(2, new RetaliateGoal(mob));
	}

	/** Whether this animal should be carrying companion goals right now. */
	public static boolean shouldInstall(Mob mob) {
		// The game's own tamed animals already have following, sitting and taking their owner's
		// side. Adding a second set would have a wolf answering to two minds at once.
		if (mob instanceof TamableAnimal) return false;

		return CompanionSpecies.isTameable(mob) && Companions.hasOwner(mob);
	}
}
