package justfatlard.better_companions.goal;

import justfatlard.better_companions.Companions;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;

/**
 * Walking beside the person you have decided to walk beside.
 *
 * <p>The same shape as the game's own wolf-following goal, written out here because that one only
 * accepts a {@code TamableAnimal} and a cow is not one. It keeps a comfortable distance rather than
 * closing all the way in, so a companion does not spend its life shoving you.
 */
public class FollowOwnerGoal extends Goal {

	/** Far enough that it is worth catching up. */
	private static final float START_DISTANCE = 10.0F;

	/** Near enough to stop. Bigger than the start distance would leave it jittering. */
	private static final float STOP_DISTANCE = 2.0F;

	/** Beyond this it has lost you rather than fallen behind, and walking will not fix it. */
	private static final float TELEPORT_DISTANCE = 20.0F;

	private final Mob mob;
	private final double speed;
	private Player owner;
	private int repathCooldown;

	public FollowOwnerGoal(Mob mob, double speed) {
		this.mob = mob;
		this.speed = speed;
		setFlags(java.util.EnumSet.of(Flag.MOVE, Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		Player found = Companions.findOwner(mob);
		if (found == null || found.isSpectator()) return false;
		if (Companions.isSitting(mob)) return false;
		if (mob.distanceToSqr(found) < START_DISTANCE * START_DISTANCE) return false;

		owner = found;
		return true;
	}

	@Override
	public boolean canContinueToUse() {
		if (owner == null || Companions.isSitting(mob)) return false;
		if (mob.getNavigation().isDone()) return false;

		return mob.distanceToSqr(owner) > STOP_DISTANCE * STOP_DISTANCE;
	}

	@Override
	public void start() {
		repathCooldown = 0;
	}

	@Override
	public void stop() {
		owner = null;
		mob.getNavigation().stop();
	}

	@Override
	public void tick() {
		mob.getLookControl().setLookAt(owner, 10.0F, mob.getMaxHeadXRot());
		if (--repathCooldown > 0) return;

		repathCooldown = adjustedTickDelay(10);

		if (mob.distanceToSqr(owner) >= TELEPORT_DISTANCE * TELEPORT_DISTANCE
				&& !mob.isLeashed() && !mob.isPassenger()) {
			teleportNear(owner);
			return;
		}

		mob.getNavigation().moveTo(owner, speed);
	}

	/**
	 * Appear near the owner, having lost them.
	 *
	 * <p>Tries a handful of spots around them rather than the exact position, so a companion does
	 * not land inside the person it is following, and gives up quietly if none of them will do
	 * rather than dropping an animal into a wall.
	 */
	private void teleportNear(Player target) {
		BlockPos base = target.blockPosition();

		for (int attempt = 0; attempt < 10; attempt++) {
			int x = base.getX() + mob.getRandom().nextInt(5) - 2;
			int y = base.getY() + mob.getRandom().nextInt(3) - 1;
			int z = base.getZ() + mob.getRandom().nextInt(5) - 2;
			if (tryTeleport(x, y, z)) return;
		}
	}

	private boolean tryTeleport(int x, int y, int z) {
		if (Math.abs(x - owner.getX()) < 2.0 && Math.abs(z - owner.getZ()) < 2.0) return false;
		if (!canStandAt(new BlockPos(x, y, z))) return false;

		mob.snapTo(x + 0.5, y, z + 0.5);
		mob.getNavigation().stop();
		return true;
	}

	private boolean canStandAt(BlockPos pos) {
		PathType type = WalkNodeEvaluator.getPathTypeStatic(mob, pos);
		if (type != PathType.WALKABLE) return false;

		return mob.level().noCollision(mob, mob.getBoundingBox().move(
			pos.getX() + 0.5 - mob.getX(), pos.getY() - mob.getY(), pos.getZ() + 0.5 - mob.getZ()));
	}
}
