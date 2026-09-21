package justfatlard.better_companions;

import java.util.Objects;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import justfatlard.pandorical.api.ActionMenuApi;
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
	/** Per player: whether their companions leave passive mobs alone altogether. */
	public static justfatlard.pandorical.api.SettingsApi.Setting<Boolean> SPARE_PASSIVE;


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
			if (entity instanceof net.minecraft.world.entity.TamableAnimal tamable) {
				CompanionGoals.installSettling(tamable);
			}
			if (entity instanceof Mob mob && Companions.hasOwner(mob)) {
				CompanionArmor.show(mob);
				Roster.note(mob);
			}
		});

		// Where each companion is when the server stops thinking about it, so a whistle knows which
		// chunk to wake. Everything else the mod does needs the animal in hand; this is the one
		// question asked about animals that are not.
		ServerEntityEvents.ENTITY_UNLOAD.register((entity, level) -> {
			if (entity instanceof Mob mob && Companions.hasOwner(mob)) Roster.noteOrForget(mob);
		});

		UseEntityCallback.EVENT.register(TameInteraction::onUseEntity);
		PortalFollow.register();
		Whistle.register();

		CompanionArmor.dress();

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
		// The player's own friends, readable and prunable from the menu; adding stays with the
		// command, which knows how to look a player up by name.
		// Whether the pack joins in against animals at all. Off, a swing at a cow is the owner's
		// own business and the dogs stay out of it; the animals the owner has fed lately are
		// left alone either way (see Fed).
		SPARE_PASSIVE = PandoricalApi.settings().group(MOD_ID, "Better Companions")
			.toggle("spare_passive", "Leave passive mobs alone", false)
			.describe("Your companions never join a fight against an animal or villager; they still defend themselves");
		Fed.init();

		// Every companion on one page, because twenty animals across three dimensions is a thing
		// nobody can see any other way. Read from the roster rather than from the world, which is
		// the only list that knows about the ones that are asleep.
		PandoricalApi.settings().group(MOD_ID, "Better Companions")
			.list("companions", "Companions", CompanionMenu::of, CompanionMenu::dismiss)
			.describe("Everything that follows you, and where it is; the button lets one go for good");

		PandoricalApi.settings().group(MOD_ID, "Better Companions")
			.list("friends", "Friends",
				player -> {
					java.util.Map<String, String> named = new java.util.LinkedHashMap<>();
					Friends.get(player.level()).of(player.getUUID())
						.forEach((id, name) -> named.put(id.toString(), name));
					return named;
				},
				(player, id) -> Friends.get(player.level()).remove(player.getUUID(), java.util.UUID.fromString(id)))
			.describe("Your companions never turn on them, whoever swung first; /companions friend add names one");
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
		PandoricalApi.keybinds().register(MOD_ID + ":pet", 19, "Pet Animal",
			Petting::pet);
		// Whistling is worth a button: it is occasional, and easy to forget there is a key for.
		// Petting is not - it is a right-click on the animal, and a menu would be the slow way.
		PandoricalApi.actionMenus().promoteKeybind(MOD_ID + ":whistle", "minecraft:goat_horn");

		// Releasing is a command rather than a key, so it is offered as a button of its own. A key
		// on purpose it is not: this is the one thing here that cannot be undone, and a menu you
		// have to open is the right amount of deliberate for it. A lead for the icon, since the
		// button's whole subject is the tie between a person and an animal.
		PandoricalApi.actionMenus().suggestButton(ActionMenuApi.Button.runs(
			"minecraft:lead", "Release", "companions release"));

		PandoricalApi.commandHelp().describe("/companions whistle", "Call every companion of yours to you.");
		PandoricalApi.commandHelp().describe("/companions pet", "Make a fuss of the nearest animal.");
		PandoricalApi.commandHelp().describe("/companions unequip", "Take the armour off the nearest companion.");
		PandoricalApi.commandHelp().describe("/companions release",
			"Let the nearest companion go; the mods menu lists the rest.");
		PandoricalApi.commandHelp().describe("/companions friend",
			"Who your companions will not fight back against.");

		// The petting cooldown is keyed by player and nothing else ever removes an entry.
		net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.DISCONNECT.register(
			(handler, server) -> Petting.forget(handler.getPlayer().getUUID()));

		if (net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("block-tip")) {
			justfatlard.better_companions.integration.CompanionTips.register();
		}

		LOGGER.info("Better Companions loaded - {} animals will keep you company",
			CompanionSpecies.offerings().size());

		// Guarded, and the guard is why the call sits behind its own class: naming a
		// village-quests type here would load it whether or not that mod is installed.
		if (net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("village-quests-justfatlard")) {
			justfatlard.better_companions.integration.CompanionRemarks.register();
		}
	}
}
