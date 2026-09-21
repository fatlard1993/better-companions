package justfatlard.better_companions;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ChunkPos;

/**
 * Getting hold of a companion the server is not currently thinking about.
 *
 * <p>An animal in a chunk nobody is standing in is in no list the game will hand out, so anything
 * that wants one has to name the chunk and ask for it back. Two ways of asking, because two things
 * want it: a whistle asks for a dozen at once and can afford to look again next tick, while a
 * button pressed against one animal has to answer now and can afford the pause.
 *
 * <p>Awake is not the same as unpacked. A chunk reaches the world a tick or more before the animals
 * in it do, and asking in between is how a companion that is perfectly fine gets written off as
 * gone - which is why {@link #settle} exists and why nothing here concludes anything without it.
 */
public final class Waking {
	private Waking() {}

	/** How long a woken chunk is given to hand its animals over. */
	public static final int TICKS = 10;

	/**
	 * The hold put on a sleeping companion's chunk. The same shape as the one an ender pearl uses
	 * to land in a dimension nobody is standing in, and for the same reason. It expires on its own
	 * a little after the caller has finished with it, which is the whole point of a timeout rather
	 * than the forced-chunk flag: a server that stops in between must not come back still holding
	 * a chunk somebody whistled at once.
	 */
	private static final TicketType HOLD = new TicketType(TICKS + 10L,
		TicketType.FLAG_LOADING | TicketType.FLAG_SIMULATION | TicketType.FLAG_KEEP_DIMENSION_ACTIVE);

	/** Ask for a chunk back, without waiting for it. */
	public static void hold(ServerLevel level, BlockPos where) {
		level.getChunkSource().addTicketWithRadius(HOLD, chunkOf(where), 0);
	}

	/** Whether the chunk at this spot has come back yet. */
	public static boolean awake(ServerLevel level, BlockPos where) {
		ChunkPos chunk = chunkOf(where);
		return level.getChunkSource().getChunkNow(chunk.x(), chunk.z()) != null;
	}

	/** Let a chunk that is back finish handing its animals over, so a miss means what it says. */
	public static void settle(ServerLevel level, BlockPos where) {
		level.waitForEntities(chunkOf(where), 0);
	}

	/**
	 * One animal, now. Waits on the chunk rather than looking again later, which costs the server
	 * thread a pause - affordable for a button pressed once, not for a whistle answering a pack.
	 */
	public static Mob fetch(ServerLevel level, BlockPos where, UUID who) {
		ChunkPos chunk = chunkOf(where);
		hold(level, where);
		level.getChunk(chunk.x(), chunk.z());
		settle(level, where);

		return level.getEntity(who) instanceof Mob mob ? mob : null;
	}

	private static ChunkPos chunkOf(BlockPos where) {
		return new ChunkPos(where.getX() >> 4, where.getZ() >> 4);
	}
}
