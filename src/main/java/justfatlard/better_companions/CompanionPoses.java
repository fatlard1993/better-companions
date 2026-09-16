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
 * <p>Everything else gets one of the mod's own animations, played through Pandorical. They move
 * the body and head, the parts every model has by those names, so the same two poses serve a cow,
 * a pig, a frog and a goat without knowing which is which.
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
	 * <p>They lower the body and leave the legs alone. An offset is in the model's own pixels, the
	 * same for every animal it plays on, and legs are not the same length on any two of them: a
	 * fold that sets a cow on the ground leaves a pig underground, and one sized for a pig leaves
	 * a cow hanging in the air above its folded legs.
	 */
	private static final String SIT = Main.MOD_ID + ":sit";
	private static final String LIE_DOWN = Main.MOD_ID + ":lie_down";

	/** The pose last set on each animal the mod poses itself, for anything played over it. */
	private static final java.util.Map<java.util.UUID, Pose> HELD = new java.util.concurrent.ConcurrentHashMap<>();

	/** How this animal is holding itself, as far as the mod set it; standing if it never did. */
	public static Pose current(Mob mob) {
		return HELD.getOrDefault(mob.getUUID(), Pose.STANDING);
	}

	/** Whether the mod draws this animal's poses itself, rather than the game's own pose for it. */
	public static boolean posedByMod(Mob mob) {
		return !(mob instanceof Fox) && !(mob instanceof Panda) && !(mob instanceof TamableAnimal);
	}

	public static void forget(java.util.UUID mob) {
		HELD.remove(mob);
	}

	public static void set(Mob mob, Pose pose) {
		boolean settled = pose != Pose.STANDING;
		if (pose == Pose.STANDING) HELD.remove(mob.getUUID());
		else HELD.put(mob.getUUID(), pose);

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
