package justfatlard.better_companions;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Making a fuss of one.
 *
 * <p>Does nothing except say so, which is the point: a companion already follows, waits and fights,
 * and none of that ever gives you a reason to touch it. This is the reason.
 *
 * <p>Picks whichever of your companions you are looking at, so a pen full of them does not need a
 * gesture per animal. Looking at nothing in particular, the nearest one within arm's reach will do
 * - being in a crowd of your own animals and pressing the button should not require aim.
 */
public final class Petting {
	private Petting() {}

	/** Arm's reach, near enough. Petting something across a paddock is not petting it. */
	private static final double REACH = 4.0;

	/** Off-centre tolerance for "looking at": generous, because animals move while you aim. */
	private static final double AIM = 0.35;

	/** Long enough that holding the key does not turn one animal into a fountain of hearts. */
	private static final long COOLDOWN_MS = 700;

	private static final String ANIMATION = Main.MOD_ID + ":pet";

	private static final Map<UUID, Long> lastPet = new ConcurrentHashMap<>();

	public static int pet(ServerPlayer player) {
		if (!(player.level() instanceof ServerLevel level)) return 0;

		long now = System.currentTimeMillis();
		Long previous = lastPet.get(player.getUUID());
		if (previous != null && now - previous < COOLDOWN_MS) return 0;

		Mob companion = nearestLookedAt(player, level);
		if (companion == null) {
			player.sendOverlayMessage(Component.translatable("message.better-companions-justfatlard.pet.none"));
			return 0;
		}

		lastPet.put(player.getUUID(), now);
		fussOver(level, player, companion);
		return 1;
	}

	/** Hearts, a pleased noise, and a moment of looking up at you. */
	private static void fussOver(ServerLevel level, ServerPlayer player, Mob companion) {
		PandoricalApiBridge.playPetAnimation(companion);

		companion.getLookControl().setLookAt(player, 30F, 30F);

		Vec3 over = companion.position().add(0, companion.getBbHeight() * 0.9, 0);
		level.sendParticles(ParticleTypes.HEART, over.x, over.y, over.z,
			3, companion.getBbWidth() * 0.4, 0.2, companion.getBbWidth() * 0.4, 0.02);

		// The animal's own voice, whatever animal it is. A table of one sound per species is a
		// table somebody has to keep in step with every animal ever added; this is the noise the
		// game already agrees a cow makes.
		companion.playAmbientSound();
	}

	/**
	 * The companion this player means.
	 *
	 * <p>Aim first, proximity second. Looking straight at one is unambiguous and should always
	 * win; falling back to the nearest is what makes the gesture work in a crowded pen, where
	 * every direction you point has an animal in it anyway.
	 */
	private static Mob nearestLookedAt(ServerPlayer player, ServerLevel level) {
		AABB reach = player.getBoundingBox().inflate(REACH);
		List<Mob> mine = level.getEntitiesOfClass(Mob.class, reach,
			mob -> Companions.isOwnedBy(mob, player) && mob.isAlive());

		if (mine.isEmpty()) return null;

		Vec3 eye = player.getEyePosition();
		Vec3 look = player.getViewVector(1.0F);

		Mob best = null;
		double bestScore = -1;

		for (Mob mob : mine) {
			Vec3 toward = mob.position().add(0, mob.getBbHeight() * 0.5, 0).subtract(eye);
			double distance = toward.length();
			if (distance > REACH || distance < 1.0E-4) continue;

			double alignment = look.dot(toward.scale(1 / distance));
			if (alignment < AIM) continue;

			// Straight ahead beats close by, and close by breaks the tie.
			double score = alignment - distance / (REACH * 100);
			if (score > bestScore) {
				bestScore = score;
				best = mob;
			}
		}

		if (best != null) return best;

		return mine.stream()
			.min(java.util.Comparator.comparingDouble(mob -> mob.distanceToSqr(player)))
			.orElse(null);
	}

	/** Forgets a player's cooldown when they leave, so the map does not grow forever. */
	public static void forget(UUID player) {
		lastPet.remove(player);
	}

	/** Kept apart so the animation call has one place to change if Pandorical's does. */
	private static final class PandoricalApiBridge {
		private PandoricalApiBridge() {}

		static void playPetAnimation(Mob companion) {
			justfatlard.pandorical.api.PandoricalApi.animations().play(companion, ANIMATION, false);
		}
	}
}
