package justfatlard.better_companions;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/**
 * The advancements this mod hands out.
 *
 * <p>Counted where they stand rather than remembered: a warband is a thing you can see behind you,
 * and asking the world how many are there needs no bookkeeping that could drift, survive a death it
 * should not, or have to be migrated later. The cost is that it only notices while you are all in
 * one place, which is the only time it is true anyway.
 */
public final class Awards {
	private Awards() {}

	/** How many at your back makes a warband. */
	private static final int WARBAND = 8;

	/** How far back they may be and still be with you. */
	private static final double GATHERED = 24.0;

	/** One more has come over; see whether that makes a crowd. */
	public static void befriended(@Nullable ServerPlayer owner) {
		if (owner == null) return;
		AABB around = owner.getBoundingBox().inflate(GATHERED);
		int following = 0;
		for (Entity near : owner.level().getEntities(owner, around)) {
			if (!(near instanceof LivingEntity animal) || !animal.isAlive()) continue;
			if (!Companions.isOwnedBy(animal, owner) || Companions.isSitting(animal)) continue;
			if (++following >= WARBAND) {
				award(owner, "warband");
				return;
			}
		}
	}

	private static void award(ServerPlayer player, String path) {
		if (player.level().getServer() == null) return;
		AdvancementHolder holder = player.level().getServer().getAdvancements()
			.get(Identifier.fromNamespaceAndPath(Main.MOD_ID, path));
		if (holder == null) return;

		AdvancementProgress progress = player.getAdvancements().getOrStartProgress(holder);
		if (progress.isDone()) return;
		for (String criterion : progress.getRemainingCriteria()) {
			player.getAdvancements().award(holder, criterion);
		}
	}
}
