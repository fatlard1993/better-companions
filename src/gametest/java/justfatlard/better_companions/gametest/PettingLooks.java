package justfatlard.better_companions.gametest;

import justfatlard.better_companions.Petting;
import justfatlard.pandorical.api.PandoricalApi;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CameraType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;

/**
 * What petting looks like from outside: the player's arm going out to the animal. Screenshots,
 * before and mid-stroke, with the camera turned to face the player.
 */
public final class PettingLooks implements FabricClientGameTest {

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			TestServerContext server = world.getServer();
			TestServerConnection connection = world.getConnection();
			connection.waitForChunksRender();
			server.waitFor(s -> PandoricalApi.isAvailable(connection.getServerPlayer()));
			server.runCommand("gamemode survival @a");
			server.runCommand("time set noon");

			int cow = server.computeOnServer(s -> {
				var player = connection.getServerPlayer();
				BlockPos here = player.blockPosition();
				player.snapTo(here.getX() + 0.5, here.getY(), here.getZ() + 0.5, 0, 20);
				Mob mob = EntityTypes.COW.create(s.overworld(), EntitySpawnReason.COMMAND);
				mob.snapTo(here.getX() + 2.3, here.getY(), here.getZ() + 0.5, 90, 0);
				mob.setNoAi(true);
				s.overworld().addFreshEntity(mob);
				return mob.getId();
			});
			context.runOnClient(client -> client.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
			context.waitTicks(20);
			context.takeScreenshot("petting-before");

			server.runOnServer(s -> Petting.pet(connection.getServerPlayer(), (Mob) s.overworld().getEntity(cow)));
			context.waitTicks(4);
			context.takeScreenshot("petting-reach");
			context.waitTicks(6);
			context.takeScreenshot("petting-stroke");

			// The poses the pet has to keep: one cow sitting, one lying down, side on to the camera.
			server.runOnServer(s -> {
				var player = connection.getServerPlayer();
				BlockPos here = player.blockPosition();
				s.overworld().getEntity(cow).discard();
				for (int i = 0; i < 2; i++) {
					Mob mob = EntityTypes.COW.create(s.overworld(), EntitySpawnReason.COMMAND);
					mob.snapTo(here.getX() + 0.5 + (i == 0 ? -2.2 : 2.2), here.getY(), here.getZ() - 3.5, 90, 0);
					mob.setNoAi(true);
					s.overworld().addFreshEntity(mob);
					justfatlard.better_companions.CompanionPoses.set(mob, i == 0
						? justfatlard.better_companions.CompanionPoses.Pose.SITTING
						: justfatlard.better_companions.CompanionPoses.Pose.RESTING);
				}
			});
			context.waitTicks(20);
			context.takeScreenshot("poses");
		}
	}
}
