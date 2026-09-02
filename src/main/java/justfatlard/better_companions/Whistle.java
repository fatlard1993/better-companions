package justfatlard.better_companions;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.phys.AABB;

/**
 * Calling everyone in.
 *
 * <p>A whistle brings your companions to you: any that have wandered further than shouting distance
 * are put down nearby and make their own way the rest of it, so they arrive walking rather than
 * appearing at your elbow.
 *
 * <p>It cancels "stay". A whistle and a stay order are opposite instructions and the newer one
 * wins - being unable to recall an animal you told to wait an hour ago would make the stay order a
 * trap rather than a tool.
 */
public final class Whistle {
	private Whistle() {}

	/** How far a whistle carries. Generous, but not across a world. */
	private static final double RANGE = 128.0;

	/** Companions closer than this are already here and are left where they stand. */
	private static final double ALREADY_HERE = 12.0;

	/** Where the far ones are put down: close enough to see you, far enough to walk in. */
	private static final int ARRIVAL_SPREAD = 6;

	public static int blow(ServerPlayer player) {
		if (!(player.level() instanceof ServerLevel level)) return 0;

		AABB earshot = player.getBoundingBox().inflate(RANGE);
		List<Mob> called = level.getEntitiesOfClass(Mob.class, earshot,
			mob -> Companions.isOwnedBy(mob, player));

		int summoned = 0;
		for (Mob mob : called) {
			if (stopWaiting(mob) | bringCloser(player, mob)) summoned++;
		}

		level.playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_FLUTE.value(),
			SoundSource.PLAYERS, 0.8F, 1.6F);

		player.sendOverlayMessage(summoned == 0
			? Component.translatable("message.better-companions-justfatlard.whistle.none")
			: Component.translatable("message.better-companions-justfatlard.whistle", summoned));

		return summoned;
	}

	/** Releases a stay order. Returns whether there was one to release. */
	private static boolean stopWaiting(Mob mob) {
		if (!Companions.isSitting(mob)) return false;

		if (mob instanceof TamableAnimal tamable) {
			tamable.setOrderedToSit(false);
			tamable.setInSittingPose(false);
		} else {
			Companions.setState(mob, Companions.stateOf(mob).standing());
		}
		CompanionPoses.set(mob, CompanionPoses.Pose.STANDING);
		return true;
	}

	/** Puts a distant companion down near the player. Returns whether it had to be moved. */
	private static boolean bringCloser(ServerPlayer player, Mob mob) {
		if (mob.distanceToSqr(player) < ALREADY_HERE * ALREADY_HERE) return false;
		if (mob.isPassenger() || mob.isLeashed()) return false;

		BlockPos base = player.blockPosition();
		for (int attempt = 0; attempt < 12; attempt++) {
			int x = base.getX() + mob.getRandom().nextInt(ARRIVAL_SPREAD * 2 + 1) - ARRIVAL_SPREAD;
			int z = base.getZ() + mob.getRandom().nextInt(ARRIVAL_SPREAD * 2 + 1) - ARRIVAL_SPREAD;
			BlockPos landing = player.level().getHeightmapPos(
				net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
				new BlockPos(x, base.getY(), z));

			if (!player.level().noCollision(mob, mob.getBoundingBox().move(
				landing.getX() + 0.5 - mob.getX(),
				landing.getY() - mob.getY(),
				landing.getZ() + 0.5 - mob.getZ()))) {
				continue;
			}

			mob.snapTo(landing.getX() + 0.5, landing.getY(), landing.getZ() + 0.5);
			mob.getNavigation().stop();
			return true;
		}
		return false;
	}
}
