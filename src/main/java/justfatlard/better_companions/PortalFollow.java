package justfatlard.better_companions;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Companions following you go through a portal with you.
 *
 * <p>A following animal walks to you, and once you are in another dimension there is nowhere to
 * walk to: it stood by the portal on the far side for as long as you were gone. So the ones
 * following, near where you stepped through, come out beside you. One told to wait stays waiting.
 *
 * <p>The game moves a player first and says so after, with the player already gone; where they
 * stepped through is kept from the tick before.
 */
public final class PortalFollow {
	private PortalFollow() {}

	/** Close enough behind you to be coming through the portal too. */
	private static final double BEHIND = 16.0;

	private static final int ARRIVAL_SPREAD = 2;

	private record Seen(ServerLevel level, Vec3 position) {}

	private static final Map<UUID, Seen> lastSeen = new ConcurrentHashMap<>();

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				lastSeen.put(player.getUUID(), new Seen(player.level(), player.position()));
			}
		});
		ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register(PortalFollow::bring);
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
			lastSeen.remove(handler.getPlayer().getUUID()));
	}

	private static void bring(ServerPlayer player, ServerLevel origin, ServerLevel destination) {
		Seen seen = lastSeen.get(player.getUUID());
		if (seen == null || seen.level() != origin || player.isSpectator()) return;

		List<Mob> party = origin.getEntitiesOfClass(Mob.class, new AABB(seen.position(), seen.position()).inflate(BEHIND),
			mob -> mob.isAlive() && Companions.isOwnedBy(mob, player) && !Companions.isSitting(mob)
				&& !mob.isPassenger() && !mob.isVehicle() && !mob.isLeashed());

		for (Mob mob : party) {
			Vec3 landing = Arrivals.beside(destination, player, mob, ARRIVAL_SPREAD);
			mob.teleport(new TeleportTransition(destination, landing, Vec3.ZERO, mob.getYRot(), mob.getXRot(),
				TeleportTransition.DO_NOTHING));
		}
	}
}
