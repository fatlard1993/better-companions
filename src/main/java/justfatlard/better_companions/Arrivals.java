package justfatlard.better_companions;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

/**
 * Where a companion is put down when it turns up beside you.
 *
 * <p>Both ways an animal arrives - through a portal at your heels, or called in by a whistle - want
 * the same thing: a patch of floor near you with room to stand on it. Asked of the level being
 * arrived in rather than the one being left, so it answers the same way across a dimension.
 */
public final class Arrivals {
	private Arrivals() {}

	private static final int ATTEMPTS = 10;

	/**
	 * A spot beside the player with ground under it and room above it, or failing that the player's
	 * own feet. Arriving in a crowd beats not arriving.
	 */
	public static Vec3 beside(ServerLevel level, ServerPlayer player, Mob mob, int spread) {
		BlockPos base = player.blockPosition();
		for (int attempt = 0; attempt < ATTEMPTS; attempt++) {
			BlockPos spot = base.offset(mob.getRandom().nextInt(spread * 2 + 1) - spread, 0,
				mob.getRandom().nextInt(spread * 2 + 1) - spread);
			Vec3 there = Vec3.atBottomCenterOf(spot);
			if (level.noCollision(mob, mob.getBoundingBox().move(there.subtract(mob.position())))
					&& !level.getBlockState(spot.below()).getCollisionShape(level, spot.below()).isEmpty()) {
				return there;
			}
		}
		return player.position();
	}
}
