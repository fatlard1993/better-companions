package justfatlard.better_companions;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;

/**
 * Every companion you have, on one page, with a button to let each one go.
 *
 * <p>The thing a keeper of twenty animals cannot do is see them. They are scattered across three
 * dimensions and most of them are asleep, so the world itself cannot be asked; the roster can, and
 * this is the roster read out loud. Each line says what the animal is and where it is, because the
 * question being answered here is nearly always "which one did I lose".
 *
 * <p>Letting one go has to work on an animal that is not there, which is the whole point of the
 * page. So the button wakes the chunk it is sleeping in, releases it, and answers - a pause on one
 * button press, in exchange for an answer that is true.
 */
public final class CompanionMenu {
	private CompanionMenu() {}

	/** Far enough that a number is more use than a direction. */
	private static final int NEARBY = 16;

	public static Map<String, String> of(ServerPlayer player) {
		List<Roster.Whereabouts> mine = Roster.get(player.level().getServer()).of(player.getUUID());

		// Nearest first, and this dimension before anywhere else: the one you are looking for is
		// usually the one you last had.
		mine.sort(java.util.Comparator
			.comparing((Roster.Whereabouts one) -> one.dimension() != player.level().dimension())
			.thenComparingDouble(one -> one.where().distSqr(player.blockPosition())));

		Map<String, String> listed = new LinkedHashMap<>();
		for (Roster.Whereabouts one : mine) listed.put(one.companion().toString(), describe(player, one));
		return listed;
	}

	private static String describe(ServerPlayer player, Roster.Whereabouts one) {
		String name = one.name().isBlank() ? "Companion" : one.name();

		if (one.dimension() != player.level().dimension()) {
			return name + " - " + one.dimension().identifier().getPath().replace('_', ' ');
		}

		long away = Math.round(Math.sqrt(one.where().distSqr(player.blockPosition())));
		return away <= NEARBY ? name + " - nearby" : name + " - " + away + " blocks away";
	}

	/**
	 * Let one go by the name it is listed under.
	 *
	 * <p>What arrives here is whatever the screen sent, so the line is found on this player's own
	 * page before anything is done with it. An identifier alone is no claim on an animal: taken at
	 * face value it would let anyone who knows one strike a companion off somebody else's list.
	 *
	 * <p>A line on the right page with no animal behind it is worth losing either way. Whatever
	 * became of the thing, the page should stop offering it.
	 */
	public static void dismiss(ServerPlayer player, String id) {
		UUID companion;
		try {
			companion = UUID.fromString(id);
		} catch (IllegalArgumentException malformed) {
			return;
		}

		MinecraftServer server = player.level().getServer();
		Roster roster = Roster.get(server);
		Roster.Whereabouts one = roster.find(companion);
		if (one == null || !one.owner().equals(player.getUUID())) return;

		Mob found = reach(player, server, one);
		if (found == null || !Companions.release(player, found)) {
			roster.drop(companion);
			return;
		}

		player.sendSystemMessage(Component.translatable(
			"command.better-companions-justfatlard.release", found.getDisplayName()));
	}

	/** The animal itself: awake wherever it is, or woken where the roster left it. */
	private static Mob reach(ServerPlayer player, MinecraftServer server, Roster.Whereabouts one) {
		Entity awake = player.level().getEntityInAnyDimension(one.companion());
		if (awake instanceof Mob mob) return mob;

		ServerLevel level = server.getLevel(one.dimension());
		return level == null ? null : Waking.fetch(level, one.where(), one.companion());
	}
}
