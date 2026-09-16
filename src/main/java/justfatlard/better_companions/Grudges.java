package justfatlard.better_companions;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

/**
 * Forgiving the first swing.
 *
 * <p>Hitting your own animal by accident is the commonest thing that happens in a crowded pen, and
 * a companion that turns on you for it is a companion you stop keeping. So the first blow from a
 * person is let go. The second, soon after, is not: twice in a few seconds is not a slip.
 *
 * <p>Grudges live in memory and die with the server. A resentment that survived a restart would be
 * a bug report nobody could reproduce, and an animal that has been left alone since yesterday has
 * no reason to still be cross.
 */
public final class Grudges {
	private Grudges() {}

	/** Two hits inside this window are a decision; further apart they are two accidents. */
	private static final long FORGIVENESS_WINDOW_TICKS = 20L * 8L;

	/** @param angry whether this blow was the second in the window, which is what is held against them */
	private record Strike(UUID striker, long at, boolean angry) {}

	private static final Map<UUID, Strike> LAST_STRIKE = new HashMap<>();

	/**
	 * Record a blow and say whether it earns retaliation.
	 *
	 * @return true if this companion is now entitled to be angry at this player
	 */
	public static boolean strike(Mob companion, Player striker, long gameTime) {
		UUID id = companion.getUUID();
		Strike previous = LAST_STRIKE.get(id);

		boolean repeat = previous != null
			&& previous.striker().equals(striker.getUUID())
			&& gameTime - previous.at() <= FORGIVENESS_WINDOW_TICKS;

		LAST_STRIKE.put(id, new Strike(striker.getUUID(), gameTime, repeat));
		return repeat;
	}

	/** Whether this companion is currently cross with this player. */
	public static boolean holdsAgainst(Mob companion, Player player, long gameTime) {
		Strike strike = LAST_STRIKE.get(companion.getUUID());

		return strike != null
			&& strike.angry()
			&& strike.striker().equals(player.getUUID())
			&& gameTime - strike.at() <= FORGIVENESS_WINDOW_TICKS;
	}

	public static void forget(Mob companion) {
		LAST_STRIKE.remove(companion.getUUID());
	}
}
