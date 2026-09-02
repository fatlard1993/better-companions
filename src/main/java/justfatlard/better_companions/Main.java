package justfatlard.better_companions;

import java.util.Objects;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import justfatlard.pandorical.api.PandoricalApi;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Mob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Any animal can be a companion, not only a wolf.
 *
 * <p>Server-side; Pandorical carries the client's half. Nothing here replaces an animal with a
 * different one - a companion cow is still a cow, with a note attached saying whose it is - so
 * everything else that knows about cows keeps working.
 */
public class Main implements ModInitializer {

	public static final String MOD_ID = "better-companions-justfatlard";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// Touch the field so the attachment registers before anything that might carry it is loaded.
		// Reading it rather than calling through: stateOf wants an entity and there is not one yet.
		Objects.requireNonNull(Companions.STATE);

		ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
			if (entity instanceof Mob mob && CompanionGoals.shouldInstall(mob)) {
				CompanionGoals.install(mob);
			}
		});

		UseEntityCallback.EVENT.register(TameInteraction::onUseEntity);

		// Every blow a person lands on a companion is remembered, so the second one within a few
		// seconds can be told from the first.
		AttackEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
			if (entity instanceof Mob mob && Companions.hasOwner(mob) && !level.isClientSide()) {
				Grudges.strike(mob, player, level.getGameTime());
			}
			return InteractionResult.PASS;
		});

		// Riding, the way this server wants it. Both need the rider's own client to agree - it is
		// the client that decides where a mount goes - so Pandorical carries them rather than this
		// mod applying them to a server nobody's client is listening to.
		PandoricalApi.mounts().doubleRiders(true);
		PandoricalApi.mounts().freeLook(true);

		CommandRegistrationCallback.EVENT.register((dispatcher, registry, environment) ->
			CompanionCommands.register(dispatcher));

		// 25 and 19 are V and P in the game's own InputConstants table, which is what this API
		// takes - NOT GLFW's codes for the same letters (86 and 80), which is what these used to
		// be. Named as numbers because the server has no InputConstants to ask, it being client
		// code. Either way it is only a preference: the pool pre-binds one slot and the rest wait
		// for the player to bind them in the controls screen.
		PandoricalApi.keybinds().register(MOD_ID + ":whistle", 25, "Whistle for Companions",
			Whistle::blow);
		PandoricalApi.keybinds().register(MOD_ID + ":pet", 19, "Pet Companion",
			Petting::pet);

		// The petting cooldown is keyed by player and nothing else ever removes an entry.
		net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.DISCONNECT.register(
			(handler, server) -> Petting.forget(handler.getPlayer().getUUID()));

		if (net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("block-tip")) {
			justfatlard.better_companions.integration.CompanionTips.register();
		}

		LOGGER.info("Better Companions loaded - {} animals will keep you company",
			CompanionSpecies.offerings().size());
	}
}
