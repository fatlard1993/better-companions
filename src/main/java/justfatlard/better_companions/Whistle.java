package justfatlard.better_companions;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

/**
 * Calling everyone in.
 *
 * <p>A whistle brings every companion you have to you, wherever it is - across the world, or out of
 * another dimension. The ones already at your heel stay where they stand; anything further is put
 * down nearby and walks the rest of the way in, so they arrive walking rather than appearing at
 * your elbow. An animal on a lead, or one being ridden, is being held by something more deliberate
 * than a whistle and stays where it is.
 *
 * <p>Most of them are not there to be called. An animal in a chunk nobody is standing in is in no
 * list the game will hand out, so the roster is asked which chunk each one went to sleep in, those
 * chunks are woken, and half a second later whoever woke up is collected. That half second is why
 * the count arrives just after the sound rather than with it.
 *
 * <p>It cancels "stay". A whistle and a stay order are opposite instructions and the newer one
 * wins - being unable to recall an animal you told to wait an hour ago would make the stay order a
 * trap rather than a tool.
 */
public final class Whistle {
	private Whistle() {}

	/** Companions closer than this are already here and are left where they stand. */
	private static final double ALREADY_HERE = 12.0;

	/** Where the far ones are put down: close enough to see you, far enough to walk in. */
	private static final int ARRIVAL_SPREAD = 6;

	/**
	 * The call: two notes, the second higher, a few ticks apart.
	 *
	 * <p>The ear takes the shape before the timbre, and the shape of a whistle is a rise. One note
	 * is somebody playing a flute; two rising are somebody calling a dog. That is why this is two
	 * sounds and a short wait rather than one sound played higher, which only ever got a flute.
	 */
	private static final float[] CALL = {1.5F, 2.0F};

	/** Between the notes: long enough to be two of them, short enough to be one gesture. */
	private static final int NOTE_GAP = 3;

	/**
	 * Past 1.0 a sound is already at full volume and the number is how far it carries instead. Both
	 * are wanted here: full in your own ears, and out to the people you are playing with, because
	 * being heard is most of the point of whistling out loud.
	 */
	private static final float LOUDNESS = 2.0F;

	/** A note of the call that has not sounded yet, and the tick it is due on. */
	private record Note(ServerLevel level, BlockPos where, float pitch, long due) {}

	private static final List<Note> ringing = new ArrayList<>();

	/** A whistle that has been blown and is waiting on chunks. */
	private static final class Call {
		private final UUID player;
		private final List<Roster.Whereabouts> asleep;
		private int answered;
		private int ticksLeft = Waking.TICKS;

		private Call(UUID player, int answered, List<Roster.Whereabouts> asleep) {
			this.player = player;
			this.answered = answered;
			this.asleep = asleep;
		}
	}

	private static final List<Call> calls = new ArrayList<>();

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			sound();
			collect(server);
		});
		// Both lists outlive a world otherwise, and a note still owing holds the level it was to
		// play in. Three ticks of state is not worth carrying into somebody's next world.
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			ringing.clear();
			calls.clear();
		});
	}

	/** Sound the call: the first note now, the rest owed. */
	private static void whistle(ServerLevel level, BlockPos where) {
		play(level, where, CALL[0]);
		for (int note = 1; note < CALL.length; note++) {
			ringing.add(new Note(level, where, CALL[note], level.getGameTime() + (long) note * NOTE_GAP));
		}
	}

	private static void play(ServerLevel level, BlockPos where, float pitch) {
		level.playSound(null, where, SoundEvents.NOTE_BLOCK_FLUTE.value(), SoundSource.PLAYERS,
			LOUDNESS, pitch);
	}

	private static void sound() {
		for (int index = ringing.size() - 1; index >= 0; index--) {
			Note note = ringing.get(index);
			if (note.level().getGameTime() < note.due()) continue;

			ringing.remove(index);
			play(note.level(), note.where(), note.pitch());
		}
	}

	public static int blow(ServerPlayer player) {
		if (!(player.level() instanceof ServerLevel level)) return 0;
		MinecraftServer server = level.getServer();

		whistle(level, player.blockPosition());

		int answered = 0;
		List<Roster.Whereabouts> asleep = new ArrayList<>();
		for (Roster.Whereabouts one : Roster.get(server).of(player.getUUID())) {
			Mob awake = find(level.getEntityInAnyDimension(one.companion()), player);
			if (awake == null) {
				asleep.add(one);
			} else {
				recall(player, awake);
				answered++;
			}
		}

		// A second whistle before the first has finished is the same whistle. The ones already
		// awake answer it again, and the chunks being woken are left to the call that woke them.
		if (asleep.isEmpty() || waitingOn(player)) {
			report(player, answered);
			return answered;
		}

		for (Roster.Whereabouts one : asleep) wake(server, one);
		calls.add(new Call(player.getUUID(), answered, asleep));
		return answered;
	}

	private static boolean waitingOn(ServerPlayer player) {
		for (Call call : calls) {
			if (call.player.equals(player.getUUID())) return true;
		}
		return false;
	}

	/** Hold the chunk a sleeping companion is in open long enough to take it out of. */
	private static void wake(MinecraftServer server, Roster.Whereabouts one) {
		ServerLevel level = server.getLevel(one.dimension());
		if (level != null) Waking.hold(level, one.where());
	}

	// Walked backwards so that a whistle blown from inside this tick - a command runs there - lands
	// after the cursor rather than under it.
	private static void collect(MinecraftServer server) {
		Roster roster = Roster.get(server);
		for (int index = calls.size() - 1; index >= 0; index--) {
			Call call = calls.get(index);
			if (--call.ticksLeft > 0) continue;

			calls.remove(index);
			ServerPlayer player = server.getPlayerList().getPlayer(call.player);
			if (player == null) continue;

			for (Roster.Whereabouts one : call.asleep) {
				ServerLevel level = server.getLevel(one.dimension());
				if (level == null) {
					roster.drop(one.companion());
					continue;
				}
				// A chunk that did not wake in time is not an answer either way, so the line stays
				// and the next whistle asks again.
				if (!Waking.awake(level, one.where())) continue;
				Waking.settle(level, one.where());

				Mob woken = find(level.getEntity(one.companion()), player);
				if (woken == null) {
					roster.drop(one.companion());
					continue;
				}
				recall(player, woken);
				call.answered++;
			}

			report(player, call.answered);
		}
	}

	/** This player's own living companion, or null if that is not what turned up. */
	private static Mob find(Entity found, ServerPlayer player) {
		return found instanceof Mob mob && mob.isAlive() && Companions.isOwnedBy(mob, player) ? mob : null;
	}

	private static void report(ServerPlayer player, int answered) {
		player.sendOverlayMessage(answered == 0
			? Component.translatable("message.better-companions-justfatlard.whistle.none")
			: Component.translatable("message.better-companions-justfatlard.whistle", answered));
	}

	private static void recall(ServerPlayer player, Mob mob) {
		stopWaiting(mob);
		bringCloser(player, mob);
	}

	/** Releases a stay order. */
	private static void stopWaiting(Mob mob) {
		if (!Companions.isSitting(mob)) return;

		if (mob instanceof TamableAnimal tamable) {
			tamable.setOrderedToSit(false);
			tamable.setInSittingPose(false);
		} else {
			Companions.setState(mob, Companions.stateOf(mob).standing());
		}
		CompanionPoses.set(mob, CompanionPoses.Pose.STANDING);
	}

	/** Puts a companion that is not already here down near the player. */
	private static void bringCloser(ServerPlayer player, Mob mob) {
		if (!(player.level() instanceof ServerLevel here)) return;
		if (mob.isPassenger() || mob.isLeashed()) return;

		boolean elsewhere = mob.level() != here;
		if (!elsewhere && mob.distanceToSqr(player) < ALREADY_HERE * ALREADY_HERE) return;

		Vec3 landing = Arrivals.beside(here, player, mob, ARRIVAL_SPREAD);
		if (elsewhere) {
			mob.teleport(new TeleportTransition(here, landing, Vec3.ZERO, mob.getYRot(), mob.getXRot(),
				TeleportTransition.DO_NOTHING));
		} else {
			mob.snapTo(landing.x, landing.y, landing.z);
			mob.getNavigation().stop();
		}
	}
}
