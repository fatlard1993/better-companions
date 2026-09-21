package justfatlard.better_companions.gametest;

import justfatlard.better_companions.Companions;
import justfatlard.better_companions.TameInteraction;
import justfatlard.better_companions.Whistle;
import justfatlard.better_companions.goal.StayGoal;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * Petting any animal, a dog that has waited long enough getting up to potter about its spot, a
 * following dog coming through a portal while a waiting one stays, and a whistle that reaches the
 * one that stayed.
 */
public final class CompanionBehaviour implements FabricClientGameTest {

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			TestServerContext server = world.getServer();
			TestServerConnection connection = world.getConnection();
			connection.waitForChunksRender();
			server.runCommand("gamemode survival @a");
			BlockPos spawn = server.computeOnServer(s -> connection.getServerPlayer().blockPosition());

			// Any animal takes a fuss; a horse is still ridden.
			server.runOnServer(s -> {
				ServerPlayer player = connection.getServerPlayer();
				Mob cow = spawn(s.overworld(), EntityTypes.COW, spawn.east(2));
				check(click(player, cow) == InteractionResult.SUCCESS, "a wild cow could not be petted");
				Mob horse = spawn(s.overworld(), EntityTypes.HORSE, spawn.west(3));
				check(click(player, horse) == InteractionResult.PASS, "petting took a horse's click");
			});

			// A dog told to sit, left long enough, gets up and potters, and stays near its spot.
			BlockPos yard = spawn.south(8);
			UUID sitter = server.computeOnServer(s -> {
				ServerLevel level = s.overworld();
				for (int x = -8; x <= 8; x++) for (int z = -8; z <= 8; z++) {
					level.setBlockAndUpdate(yard.offset(x, -1, z), Blocks.GRASS_BLOCK.defaultBlockState());
				}
				Wolf wolf = (Wolf) spawn(level, EntityTypes.WOLF, yard);
				wolf.tame(connection.getServerPlayer());
				wolf.setOrderedToSit(true);
				wolf.setInSittingPose(true);
				return wolf.getUUID();
			});
			context.waitTicks(5);
			server.runOnServer(s -> {
				Wolf wolf = (Wolf) s.overworld().getEntity(sitter);
				var state = Companions.stateOf(wolf);
				check(state.sitting(), "the sit was not recorded");
				Companions.setState(wolf, state.sittingAt(s.overworld().getGameTime() - StayGoal.SETTLE_TICKS - 20,
					state.post().orElse(yard)));
			});
			double furthest = 0;
			boolean stood = false;
			for (int tick = 0; tick < 400; tick++) {
				context.waitTick();
				double[] seen = server.computeOnServer(s -> {
					Wolf wolf = (Wolf) s.overworld().getEntity(sitter);
					return new double[] {wolf.position().distanceTo(Vec3.atBottomCenterOf(yard)),
						wolf.isInSittingPose() ? 0 : 1, wolf.isOrderedToSit() ? 1 : 0};
				});
				furthest = Math.max(furthest, seen[0]);
				stood |= seen[1] == 1;
				check(seen[2] == 1, "settling in cancelled the sit order");
			}
			check(stood, "a settled dog never got up");
			check(furthest > 1.0, "a settled dog got up but never left the spot: " + furthest);
			check(furthest < 8.5, "a settled dog wandered off: " + furthest);

			// Through a portal: the following dog comes, the waiting one does not.
			UUID follower = server.computeOnServer(s -> {
				Wolf wolf = (Wolf) spawn(s.overworld(), EntityTypes.WOLF, spawn.north(2));
				wolf.tame(connection.getServerPlayer());
				return wolf.getUUID();
			});
			context.waitTicks(5);
			server.runOnServer(s -> {
				ServerPlayer player = connection.getServerPlayer();
				ServerLevel nether = s.getLevel(Level.NETHER);
				// Somewhere to stand: the nether at a fixed height is as often a drop as a floor.
				for (int x = -4; x <= 4; x++) for (int z = -4; z <= 4; z++) {
					nether.setBlockAndUpdate(new BlockPos(x, 69, z), Blocks.OBSIDIAN.defaultBlockState());
					for (int y = 70; y <= 72; y++) nether.setBlockAndUpdate(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
				}
				player.teleport(new TeleportTransition(nether, new Vec3(0.5, 70, 0.5), Vec3.ZERO, 0, 0,
					TeleportTransition.DO_NOTHING));
			});
			context.waitTicks(10);
			server.runOnServer(s -> {
				ServerLevel nether = s.getLevel(Level.NETHER);
				boolean came = !nether.getEntitiesOfClass(Wolf.class, new AABB(new BlockPos(0, 70, 0)).inflate(8),
					wolf -> Companions.isOwnedBy(wolf, connection.getServerPlayer())).isEmpty();
				check(came, "the following dog did not come through");
				check(nether.getEntity(follower) != null, "the dog that came through is not the follower");
				// The overworld may have unloaded behind the player, so the waiting dog is looked
				// for where it must not be rather than where it should.
				check(nether.getEntity(sitter) == null, "the waiting dog came through as well");
			});

			// And a whistle reaches the one left behind: another dimension, and a chunk nobody is
			// standing in, which between them are every reason a whistle used to find nothing.
			server.runOnServer(s -> Whistle.blow(connection.getServerPlayer()));
			context.waitTicks(30);
			server.runOnServer(s -> {
				ServerPlayer player = connection.getServerPlayer();
				Wolf called = (Wolf) s.getLevel(Level.NETHER).getEntity(sitter);
				check(called != null, "a whistle did not reach the dog left in the overworld");
				check(called.distanceTo(player) < 16, "the whistled dog arrived nowhere near the player");
				check(!called.isOrderedToSit(), "the whistle did not cancel the stay order");
			});
		}
	}

	private static Mob spawn(ServerLevel level, EntityType<? extends Mob> type, BlockPos at) {
		Mob mob = type.create(level, EntitySpawnReason.COMMAND);
		mob.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
		level.addFreshEntity(mob);
		return mob;
	}

	private static InteractionResult click(ServerPlayer player, Entity target) {
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		return TameInteraction.onUseEntity(player, player.level(), InteractionHand.MAIN_HAND, target,
			new EntityHitResult(target));
	}

	private static void check(boolean holds, String otherwise) {
		if (!holds) throw new AssertionError(otherwise);
	}
}
