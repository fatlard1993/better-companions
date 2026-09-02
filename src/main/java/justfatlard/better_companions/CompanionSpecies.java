package justfatlard.better_companions;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.item.Item;
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

	private static final Map<EntityType<?>, Item> OFFERINGS = new LinkedHashMap<>();

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
		OFFERINGS.put(type, item);
	}

	/** The item this animal will befriend you for, or null if it is not one of ours. */
	public static Item offeringFor(EntityType<?> type) {
		return OFFERINGS.get(type);
	}

	/** Every animal this mod can tame, in the order they were declared. */
	public static Map<EntityType<?>, Item> offerings() {
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
