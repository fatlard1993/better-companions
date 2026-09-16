package justfatlard.better_companions.goal;

import justfatlard.better_companions.CompanionPoses;
import justfatlard.better_companions.Companions;
import java.util.EnumSet;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;

/**
 * Waiting where you were told to wait.
 *
 * <p>Holds the movement flag rather than actively standing still, which is what stops every other
 * goal that would wander off. It sits at the very front of the list so that "stay" beats whatever
 * the animal would otherwise be doing.
 *
 * <p>It lets go after a while. An animal told to wait and then left there for several minutes has
 * settled in rather than been abandoned, and standing to attention for hours is not what waiting
 * looks like - so once it has been there long enough it keeps its post but stops holding itself
 * rigid, and the wandering goals below get their turn within a short leash of the spot.
 */
public class StayGoal extends Goal {

	/** How long a companion holds perfectly still before it is allowed to settle in. */
	public static final int SETTLE_TICKS = 20 * 60 * 5;

	private final Mob mob;

	public StayGoal(Mob mob) {
		this.mob = mob;
		setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP));
	}

	/** Whether this companion has been waiting long enough to be allowed to potter about. */
	public static boolean hasSettled(Mob mob) {
		var state = Companions.stateOf(mob);
		if (!state.sitting()) return false;

		return mob.level().getGameTime() - state.sittingSince() > SETTLE_TICKS;
	}

	@Override
	public boolean canUse() {
		return Companions.isSitting(mob) && !hasSettled(mob);
	}

	@Override
	public void start() {
		mob.getNavigation().stop();
		CompanionPoses.set(mob, CompanionPoses.Pose.SITTING);
	}

	/**
	 * Back on its feet - which happens when it has settled, not when it stops waiting.
	 *
	 * <p>A settled companion potters and lies down by turns, and sitting bolt upright through the
	 * pottering would be the one pose that never matched what it was doing.
	 */
	@Override
	public void stop() {
		CompanionPoses.set(mob, CompanionPoses.Pose.STANDING);
	}
}
