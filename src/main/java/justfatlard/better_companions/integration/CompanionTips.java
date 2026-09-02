package justfatlard.better_companions.integration;

import justfatlard.better_companions.CompanionSpecies;
import justfatlard.better_companions.Companions;
import justfatlard.block_tip.api.BlockTipApi;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;

/**
 * Telling you what an animal wants, when you are looking at one.
 *
 * <p>Sixteen animals with sixteen different offerings is exactly the sort of thing nobody should
 * have to memorise or alt-tab for, and block-tip is already the place this server answers "what do
 * I do with this". So a wild one says what would win it over, and a tamed one says whose it is.
 *
 * <p>Compiled against block-tip's API and guarded at the call site by a mod-loaded check, so a
 * server without it never loads this class.
 */
public final class CompanionTips {
	private CompanionTips() {}

	public static void register() {
		BlockTipApi.describeEntity((entity, player) -> describe(entity, player));
	}

	private static String describe(Entity entity, net.minecraft.server.level.ServerPlayer player) {
		if (!(entity instanceof Mob mob)) return null;

		if (Companions.hasOwner(mob)) {
			if (Companions.isOwnedBy(mob, player)) {
				return Companions.isSitting(mob) ? "Waiting for you" : "Following you";
			}
			return "Someone else's companion";
		}

		Item offering = CompanionSpecies.offeringFor(mob.getType());
		if (offering == null) return null;

		// Naming the crouch, because the item alone is also what you feed it with: without the
		// gesture the tip would read as an instruction to do the thing that breeds it instead.
		return "Sneak + "
			+ new net.minecraft.world.item.ItemStack(offering).getHoverName().getString()
			+ " to befriend";
	}
}
