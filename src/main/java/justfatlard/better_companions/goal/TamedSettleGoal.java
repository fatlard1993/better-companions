package justfatlard.better_companions.goal;

import java.util.EnumSet;

import justfatlard.better_companions.CompanionState;
import justfatlard.better_companions.Companions;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.phys.Vec3;

/**
 * Settling in, for the game's own tamed animals: a dog told to sit, left for a while, gets up now
 * and then, potters about the spot, and sits down somewhere else - rather than sitting on the one
 * block for as long as nobody comes back, in whatever has piled up around it.
 *
 * <p>The same rule {@link StayGoal} gives the mod's own companions, done over the game's sitting
 * rather than beside it: the animal stays ordered to sit, so it still does not follow or fight, and
 * this goal outranks the game's sit-when-ordered for as long as it runs. It holds movement itself,
 * so the animal's own wandering never gets a turn to take it out of the yard.
 */
public class TamedSettleGoal extends Goal {

	/** How far from where it was told to sit a settled animal may get. */
	private static final int LEASH = 6;

	private static final int SIT_MIN_TICKS = 20 * 15;
	private static final int SIT_MAX_TICKS = 20 * 45;

	private static final double SPEED = 0.8;

	private static final double MIN_STROLL = 2.0;

	private final TamableAnimal mob;
	private int sitting;

	public TamedSettleGoal(TamableAnimal mob) {
		this.mob = mob;
		setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP));
	}

	/**
	 * Keeps the record of when and where it sat down in step with the game's sit order, since the
	 * order is the game's to give and take: a sneak-click, a whistle, a sit when tamed.
	 */
	@Override
	public boolean canUse() {
		CompanionState state = Companions.stateOf(mob);
		boolean ordered = mob.isTame() && mob.isOrderedToSit();
		if (!ordered) {
			if (state.sitting()) Companions.setState(mob, state.standing());
			return false;
		}
		if (!state.sitting()) {
			Companions.setState(mob, state.sittingAt(mob.level().getGameTime(), mob.blockPosition()));
			return false;
		}
		return mob.getNavigation() instanceof GroundPathNavigation && StayGoal.hasSettled(mob)
			&& !mob.isPassenger() && !mob.isLeashed();
	}

	@Override
	public boolean canContinueToUse() {
		return mob.isTame() && mob.isOrderedToSit() && !mob.isPassenger() && !mob.isLeashed();
	}

	/** It has sat long enough: up, and off for a look round. */
	@Override
	public void start() {
		sitting = 0;
		mob.setInSittingPose(false);
		wander();
	}

	@Override
	public void stop() {
		mob.getNavigation().stop();
		mob.setInSittingPose(mob.isOrderedToSit());
	}

	@Override
	public void tick() {
		if (sitting > 0) {
			if (--sitting == 0) {
				mob.setInSittingPose(false);
				wander();
			}
			return;
		}
		if (mob.getNavigation().isDone()) {
			mob.setInSittingPose(true);
			sitting = SIT_MIN_TICKS + mob.getRandom().nextInt(SIT_MAX_TICKS - SIT_MIN_TICKS);
		}
	}

	/** A stroll to somewhere else in the yard: far enough from where it sits to be a move at all. */
	private void wander() {
		BlockPos post = Companions.stateOf(mob).post().orElse(mob.blockPosition());
		for (int attempt = 0; attempt < 16; attempt++) {
			Vec3 spot = LandRandomPos.getPos(mob, LEASH, 3);
			if (spot == null || spot.distanceToSqr(Vec3.atCenterOf(post)) > LEASH * LEASH) continue;
			if (spot.distanceToSqr(mob.position()) < MIN_STROLL * MIN_STROLL) continue;
			if (mob.getNavigation().moveTo(spot.x, spot.y, spot.z, SPEED)) return;
		}
		// Nowhere in the yard to go: back to the spot itself.
		mob.getNavigation().moveTo(post.getX() + 0.5, post.getY(), post.getZ() + 0.5, SPEED);
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}
}
