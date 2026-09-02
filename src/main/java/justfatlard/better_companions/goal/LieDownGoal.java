package justfatlard.better_companions.goal;

import java.util.EnumSet;
import justfatlard.better_companions.CompanionPoses;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;

/**
 * Letting a settled companion lie down.
 *
 * <p>An animal left on guard does not stand to attention the whole time. Once it has settled it
 * spends most of its time lying down and gets up now and then to potter, which is what an animal
 * that lives somewhere looks like rather than one that has been switched off.
 *
 * <p>Where the game gives the animal a pose for it - a fox curls up, a cat and a panda sit - that
 * pose is used. Most animals have none, and there is no honest way to invent one from the server
 * side: those simply settle in place, still and quiet, which reads as resting even without an
 * animation. See {@link CompanionPoses}.
 */
public class LieDownGoal extends Goal {

	private static final int LIE_DOWN_CHANCE = 80;
	private static final int MIN_TICKS = 20 * 8;
	private static final int MAX_TICKS = 20 * 40;

	private final Mob mob;
	private int remaining;

	public LieDownGoal(Mob mob) {
		this.mob = mob;
		setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP));
	}

	@Override
	public boolean canUse() {
		if (!StayGoal.hasSettled(mob)) return false;
		if (mob.getTarget() != null) return false;

		return mob.getRandom().nextInt(LIE_DOWN_CHANCE) == 0;
	}

	@Override
	public boolean canContinueToUse() {
		return remaining > 0 && StayGoal.hasSettled(mob) && mob.getTarget() == null;
	}

	@Override
	public void start() {
		remaining = MIN_TICKS + mob.getRandom().nextInt(MAX_TICKS - MIN_TICKS);
		mob.getNavigation().stop();
		CompanionPoses.set(mob, CompanionPoses.Pose.RESTING);
	}

	@Override
	public void stop() {
		CompanionPoses.set(mob, CompanionPoses.Pose.STANDING);
	}

	@Override
	public void tick() {
		remaining--;
	}
}
