package justfatlard.better_companions.goal;

import java.util.EnumSet;
import justfatlard.better_companions.Companions;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.phys.Vec3;

/**
 * Pottering about, once told to wait has become where it lives.
 *
 * <p>An animal held perfectly still for hours is not waiting, it is frozen. After a while a
 * companion is allowed to move again - but only around the spot it was left, so "stay here" still
 * means something and you can come back and find it.
 *
 * <p>It always keeps its post in mind: a wander that would end up outside the yard is refused
 * rather than clipped, so it drifts within the area instead of pacing its edge.
 */
public class SettledWanderGoal extends Goal {

	/** How far from its post a settled companion may get. A yard, not a journey. */
	private static final int LEASH = 6;

	private static final int WANDER_CHANCE = 120;

	private final PathfinderMob mob;
	private final double speed;
	private Vec3 destination;

	public SettledWanderGoal(PathfinderMob mob, double speed) {
		this.mob = mob;
		this.speed = speed;
		setFlags(EnumSet.of(Flag.MOVE));
	}

	@Override
	public boolean canUse() {
		if (!StayGoal.hasSettled(mob)) return false;
		if (mob.getRandom().nextInt(WANDER_CHANCE) != 0) return false;

		BlockPos post = Companions.stateOf(mob).post().orElse(mob.blockPosition());
		destination = LandRandomPos.getPos(mob, LEASH, 3);

		if (destination == null) return false;
		return destination.distanceToSqr(Vec3.atCenterOf(post)) <= LEASH * LEASH;
	}

	@Override
	public boolean canContinueToUse() {
		return StayGoal.hasSettled(mob) && !mob.getNavigation().isDone();
	}

	@Override
	public void start() {
		mob.getNavigation().moveTo(destination.x, destination.y, destination.z, speed);
	}
}
