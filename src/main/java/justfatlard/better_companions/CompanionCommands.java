package justfatlard.better_companions;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import java.util.Collection;
import java.util.UUID;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.server.players.NameAndId;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;

/**
 * Naming your friends, calling everyone in, and letting one go.
 *
 * <p>Friends are each player's own: the people whose stray hit should not have your companions
 * go for them. Who that is depends on whose animals they are, so it is nobody else's to set.
 */
public final class CompanionCommands {
	private CompanionCommands() {}

	/** How far "the nearest one" looks. Close, because these commands end in a particular animal. */
	private static final double REACH = 8.0;

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("companions")
			.then(Commands.literal("whistle")
				.executes(context -> Whistle.blow(context.getSource().getPlayerOrException())))
			.then(Commands.literal("pet")
				.executes(context -> Petting.pet(context.getSource().getPlayerOrException())))
			.then(Commands.literal("unequip")
				.executes(context -> unequipNearby(context.getSource().getPlayerOrException())))
			.then(Commands.literal("release")
				.executes(context -> releaseNearest(context.getSource().getPlayerOrException())))
			.then(Commands.literal("friend")
				.then(Commands.literal("add")
					.then(Commands.argument("player", GameProfileArgument.gameProfile())
						.executes(context -> addFriends(context.getSource(),
							GameProfileArgument.getGameProfiles(context, "player")))))
				.then(Commands.literal("remove")
					.then(Commands.argument("player", StringArgumentType.word())
						.executes(context -> removeFriend(context.getSource(),
							StringArgumentType.getString(context, "player")))))
				.then(Commands.literal("list")
					.executes(context -> listFriends(context.getSource())))
				// For an operator: everyone here is on everyone's side, in one go.
				.then(Commands.literal("all")
					.requires(source -> Commands.LEVEL_GAMEMASTERS.check(source.permissions()))
					.executes(context -> befriendAll(context.getSource())))));
	}

	/**
	 * Every player online named a friend of every other, both ways.
	 *
	 * <p>A household server is one where everybody is already on everybody's side, and the
	 * friend list exists for the dogs to know it. Saying so pair by pair is a chore nobody does,
	 * so an operator says it once for the room.
	 */
	private static int befriendAll(CommandSourceStack source) {
		Friends friends = Friends.get(source.getLevel());
		List<ServerPlayer> online = source.getServer().getPlayerList().getPlayers();

		int added = 0;
		for (ServerPlayer owner : online) {
			for (ServerPlayer other : online) {
				if (other == owner) continue;
				if (friends.add(owner.getUUID(), other.getUUID(), other.getName().getString())) added++;
			}
		}

		int count = added;
		int people = online.size();
		source.sendSuccess(() -> Component.literal(
			people < 2 ? "Nobody else is online to befriend"
				: count == 0 ? "Everyone online was already friends"
				: count + " friendship(s) added among " + people + " players"), true);
		return added;
	}

	/**
	 * Take the armour off whichever companion is nearest.
	 *
	 * <p>A command rather than a gesture because every gesture was already spoken for: an empty
	 * hand tells a companion to wait, and a full one puts armour on. Reaching for a third would
	 * have meant taking one of those away.
	 */
	private static int unequipNearby(ServerPlayer player) {
		var nearby = nearest(player, mob -> !CompanionArmor.worn(mob).isEmpty());

		if (nearby.isEmpty() || !CompanionArmor.unequip(player, nearby.getFirst())) {
			player.sendSystemMessage(Component.translatable("command.better-companions-justfatlard.unequip.none"));
			return 0;
		}

		player.sendSystemMessage(Component.translatable("command.better-companions-justfatlard.unequip",
			nearby.getFirst().getDisplayName()));
		return 1;
	}

	/** This player's own companions within reach, nearest first: what "the one I mean" means. */
	private static List<Mob> nearest(ServerPlayer player, java.util.function.Predicate<Mob> which) {
		List<Mob> found = player.level().getEntitiesOfClass(Mob.class,
			player.getBoundingBox().inflate(REACH),
			mob -> Companions.isOwnedBy(mob, player) && which.test(mob));
		found.sort(java.util.Comparator.comparingDouble(player::distanceToSqr));
		return found;
	}

	/**
	 * Let the nearest companion go.
	 *
	 * <p>The nearest one, and close, because this is the end of the relationship and the animal you
	 * are standing next to is the one you mean. Letting go of one you cannot see is the mod menu's,
	 * where the whole list is laid out and there is something to pick from.
	 */
	private static int releaseNearest(ServerPlayer player) {
		var nearby = nearest(player, mob -> true);
		if (nearby.isEmpty()) {
			player.sendSystemMessage(Component.translatable(
				"command.better-companions-justfatlard.release.none"));
			return 0;
		}

		Mob letting = nearby.getFirst();
		Component named = letting.getDisplayName();
		if (!Companions.release(player, letting)) return 0;

		player.sendSystemMessage(Component.translatable(
			"command.better-companions-justfatlard.release", named));
		return 1;
	}

	private static int addFriends(CommandSourceStack source, Collection<NameAndId> profiles) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		UUID owner = source.getPlayerOrException().getUUID();
		Friends friends = Friends.get(source.getLevel());

		int added = 0;
		for (NameAndId profile : profiles) {
			if (profile.id().equals(owner)) continue;
			if (friends.add(owner, profile.id(), profile.name())) added++;
		}

		int count = added;
		source.sendSuccess(() -> Component.translatable(
			"command.better-companions-justfatlard.friend.added", count), true);
		return added;
	}

	private static int removeFriend(CommandSourceStack source, String name) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		UUID owner = source.getPlayerOrException().getUUID();
		Friends friends = Friends.get(source.getLevel());

		UUID found = null;
		for (var entry : friends.of(owner).entrySet()) {
			if (entry.getValue().equalsIgnoreCase(name)) {
				found = entry.getKey();
				break;
			}
		}

		if (found == null || !friends.remove(owner, found)) {
			source.sendFailure(Component.translatable("command.better-companions-justfatlard.friend.unknown", name));
			return 0;
		}

		source.sendSuccess(() -> Component.translatable(
			"command.better-companions-justfatlard.friend.removed", name), true);
		return 1;
	}

	private static int listFriends(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		var friends = Friends.get(source.getLevel()).of(source.getPlayerOrException().getUUID());
		if (friends.isEmpty()) {
			source.sendSuccess(() -> Component.translatable("command.better-companions-justfatlard.friend.none"), false);
			return 0;
		}

		String names = String.join(", ", friends.values());
		source.sendSuccess(() -> Component.translatable(
			"command.better-companions-justfatlard.friend.list", friends.size(), names), false);
		return friends.size();
	}

	/** Kept so the level lookup reads the same way everywhere Friends is reached. */
	static ServerLevel levelOf(ServerPlayer player) {
		return player.level() instanceof ServerLevel level ? level : null;
	}
}
