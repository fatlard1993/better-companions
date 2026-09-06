package justfatlard.better_companions;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;

/**
 * Animals a player has fed lately.
 *
 * <p>The companions take their owner's side in a fight, and to them a cow their owner just
 * swung at is a fight. It is not, if the owner fed that cow a minute ago: that is a farm, and
 * the swing was the owner's own business. So a feeding is remembered a while, and an animal
 * fed within it is nobody's quarry.
 *
 * <p>Held in memory only. A farm outlives a restart; the grudge about which cow was fed when
 * does not need to.
 */
public final class Fed {
	private Fed() {}

	/** How long a feeding protects the animal: five minutes of game time. */
	public static final long RECENT_TICKS = 5L * 60L * 20L;

	/** By player, the animals they have fed and when. */
	private static final Map<UUID, Map<UUID, Long>> fed = new HashMap<>();

	public static void init() {
		UseEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
			if (level.isClientSide() || !(entity instanceof Animal animal)) return InteractionResult.PASS;
			if (animal.isFood(player.getItemInHand(hand))) note(player, animal, level.getGameTime());
			return InteractionResult.PASS;
		});
	}

	private static void note(Player player, Animal animal, long now) {
		Map<UUID, Long> mine = fed.computeIfAbsent(player.getUUID(), id -> new HashMap<>());
		mine.values().removeIf(at -> now - at > RECENT_TICKS);
		mine.put(animal.getUUID(), now);
	}

	/** Whether this player fed this animal within the last while. */
	public static boolean recently(Player player, LivingEntity animal, long now) {
		Map<UUID, Long> mine = fed.get(player.getUUID());
		if (mine == null) return false;
		Long at = mine.get(animal.getUUID());
		return at != null && now - at <= RECENT_TICKS;
	}
}
