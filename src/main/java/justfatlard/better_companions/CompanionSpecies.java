package justfatlard.better_companions;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Which animals will keep you company, and what wins them over.
 *
 * <p>The offering is the one the animal already cares about wherever the game gave it an opinion -
 * a fox's sweet berries, a panda's bamboo, a pig's carrot - so nobody has to learn a second table
 * of what an animal likes. Where vanilla has no opinion the choice is the obvious one: something the
 * animal eats, or in the snow golem's case something it is made of.
 *
 * <p>Wolves, cats and parrots are missing on purpose. The game already lets you tame those, and this
 * mod would rather extend what they do than replace it - they are companions here too, they simply
 * arrive already tamed. Everything else in the mod treats them the same as the rest.
 */
public final class CompanionSpecies {
	private CompanionSpecies() {}

	/**
	 * More than one item per animal, because another mod may know a second thing this animal
	 * wants: beets-bears gives the bears honey and beetroot without taking their salmon away. The
	 * first in the list is the one the tip names, so the one declared here stays the answer to
	 * "what does this like" and the rest are extras that also work.
	 */
	private static final Map<EntityType<?>, List<Item>> OFFERINGS = new LinkedHashMap<>();

	static {
		// What the animal already likes, where the game says so.
		offer(EntityTypes.FOX, Items.SWEET_BERRIES);
		offer(EntityTypes.PANDA, Items.BAMBOO);
		offer(EntityTypes.PIG, Items.CARROT);
		offer(EntityTypes.RABBIT, Items.CARROT);
		offer(EntityTypes.CHICKEN, Items.WHEAT_SEEDS);
		offer(EntityTypes.COW, Items.WHEAT);
		offer(EntityTypes.MOOSHROOM, Items.WHEAT);
		offer(EntityTypes.SHEEP, Items.WHEAT);
		offer(EntityTypes.GOAT, Items.WHEAT);
		offer(EntityTypes.OCELOT, Items.COD);
		offer(EntityTypes.ARMADILLO, Items.SPIDER_EYE);
		offer(EntityTypes.BEE, Items.HONEYCOMB);
		offer(EntityTypes.FROG, Items.SLIME_BALL);
		offer(EntityTypes.AXOLOTL, Items.TROPICAL_FISH);
		offer(EntityTypes.POLAR_BEAR, Items.SALMON);

		// No opinion of its own, so: what it is made of.
		offer(EntityTypes.SNOW_GOLEM, Items.SNOWBALL);
	}

	private static void offer(EntityType<?> type, Item item) {
		OFFERINGS.put(type, new ArrayList<>(List.of(item)));
	}

	/**
	 * Another mod adding something this animal will also come for, during onInitialize.
	 *
	 * <p>Added after whatever this mod declared, so the tip keeps naming the animal's own food and
	 * the addition is a second way in rather than a replacement. An animal this mod does not tame
	 * at all can be added outright this way.
	 */
	public static void alsoOffer(EntityType<?> type, Item... items) {
		OFFERINGS.computeIfAbsent(type, key -> new ArrayList<>()).addAll(List.of(items));
	}

	/** The item this animal is best known for wanting, or null if it is not one of ours. */
	public static Item offeringFor(EntityType<?> type) {
		List<Item> items = OFFERINGS.get(type);
		return items == null || items.isEmpty() ? null : items.getFirst();
	}

	/** Whether this is something you could hold out to this animal. */
	public static boolean accepts(EntityType<?> type, ItemStack held) {
		List<Item> items = OFFERINGS.get(type);
		if (items == null) return false;
		for (Item item : items) {
			if (held.is(item)) return true;
		}
		return false;
	}

	/** Every animal this mod can tame, in the order they were declared. */
	public static Map<EntityType<?>, List<Item>> offerings() {
		return OFFERINGS;
	}

	/** Whether this animal can be befriended by this mod - as opposed to by the game already. */
	public static boolean isTameable(Entity entity) {
		return OFFERINGS.containsKey(entity.getType());
	}

	/**
	 * Whether this is something the mod's behaviour applies to at all.
	 *
	 * <p>Covers the game's own tamed animals as well as ours, so a wolf answers a whistle and takes
	 * companion armour without having to be re-tamed into a second system that means the same thing.
	 */
	public static boolean isCompanionKind(Entity entity) {
		return entity instanceof TamableAnimal || (entity instanceof Mob && isTameable(entity));
	}
}
