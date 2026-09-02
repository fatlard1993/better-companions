package justfatlard.better_companions;

import justfatlard.pandorical.api.PandoricalApi;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.fox.Fox;
import net.minecraft.world.entity.animal.panda.Panda;

/**
 * What a waiting companion looks like.
 *
 * <p>One place decides, because there are three things that can want to change a companion's pose -
 * being told to wait, lying down once settled, and being whistled for - and three callers each
 * setting their own would eventually disagree about which won.
 *
 * <p>Where the game already has a pose it is used: a fox curls up, a panda sits, and so do the
 * game's own tamed animals. Those were drawn for that animal and look better than anything layered
 * on top. They have only the one pose between sitting and resting, which is fine - the distinction
 * matters more to the code than to the animal.
 *
 * <p>Everything else gets one of the mod's own animations, played through Pandorical. They name the
 * parts vanilla's four-legged models all share, so the same two poses serve a cow, a pig, a sheep
 * and a goat without knowing which is which, and quietly do nothing to the parts of an animal
 * shaped differently.
 */
public final class CompanionPoses {
	private CompanionPoses() {}

	/** How a companion is holding itself. */
	public enum Pose {
		/** On its feet, doing whatever it would normally do. */
		STANDING,
		/** Told to wait, and sitting up to show it. */
		SITTING,
		/** Settled in, lying down. */
		RESTING,
	}

	/**
	 * The poses for animals the game has no sitting pose of its own for.
	 *
	 * <p>Each names the bones of more than one body plan - a quadruped's {@code left_hind_leg} and
	 * a frog's or a chicken's {@code left_leg} - because an animation quietly skips a bone the
	 * model does not have. That silence is the trap: the first version of this named only the
	 * quadruped bones, so a frog was sent an animation that moved nothing but its body by two
	 * pixels, and sitting looked like a feature that did not work rather than one aimed at the
	 * wrong parts. Naming both costs nothing on an animal that has only one of them.
	 *
	 * <p>So an animal added to the roster with legs spelled some third way needs its spelling added
	 * here, and the way that shows up is that it stands there when told to wait.
	 */
	private static final String SIT = Main.MOD_ID + ":sit";
	private static final String LIE_DOWN = Main.MOD_ID + ":lie_down";

	public static void set(Mob mob, Pose pose) {
		boolean settled = pose != Pose.STANDING;

		// The game's own animals have one pose for both, so both ask for it.
		if (mob instanceof Fox fox) {
			fox.setSitting(settled);
			return;
		}
		if (mob instanceof Panda panda) {
			panda.sit(settled);
			return;
		}
		if (mob instanceof TamableAnimal tamable) {
			tamable.setInSittingPose(settled);
			return;
		}

		switch (pose) {
			case SITTING -> PandoricalApi.animations().play(mob, SIT, false);
			case RESTING -> PandoricalApi.animations().play(mob, LIE_DOWN, false);
			case STANDING -> PandoricalApi.animations().stop(mob);
		}
	}
}
