package justfatlard.better_companions;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import java.util.Collection;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.server.players.NameAndId;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Naming permanent friends, and calling everyone in.
 *
 * <p>Friends are an operator's list rather than a per-player one, and the reason is what a friend
 * is for: it is how you stop somebody's pet mauling somebody else. That is a decision about the
 * server, not about one animal, so it is made once.
 */
public final class CompanionCommands {
	private CompanionCommands() {}

	/** Naming friends is an operator's business; whistling for your own animals is not. */
	private static final Permission FRIEND_PERMISSION = Permissions.COMMANDS_GAMEMASTER;

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("companions")
			.then(Commands.literal("whistle")
				.executes(context -> Whistle.blow(context.getSource().getPlayerOrException())))
			.then(Commands.literal("pet")
				.executes(context -> Petting.pet(context.getSource().getPlayerOrException())))
			.then(Commands.literal("unequip")
				.executes(context -> unequipNearby(context.getSource().getPlayerOrException())))
			.then(Commands.literal("friend")
				.requires(source -> source.permissions().hasPermission(FRIEND_PERMISSION))
				.then(Commands.literal("add")
					.then(Commands.argument("player", GameProfileArgument.gameProfile())
						.executes(context -> addFriends(context.getSource(),
							GameProfileArgument.getGameProfiles(context, "player")))))
				.then(Commands.literal("remove")
					.then(Commands.argument("player", StringArgumentType.word())
						.executes(context -> removeFriend(context.getSource(),
							StringArgumentType.getString(context, "player")))))
				.then(Commands.literal("list")
					.executes(context -> listFriends(context.getSource())))));
	}

	/**
	 * Take the armour off whichever companion is nearest.
	 *
	 * <p>A command rather than a gesture because every gesture was already spoken for: an empty
	 * hand tells a companion to wait, and a full one puts armour on. Reaching for a third would
	 * have meant taking one of those away.
	 */
	private static int unequipNearby(ServerPlayer player) {
		var nearby = player.level().getEntitiesOfClass(net.minecraft.world.entity.Mob.class,
			player.getBoundingBox().inflate(8.0),
			mob -> Companions.isOwnedBy(mob, player) && !CompanionArmor.worn(mob).isEmpty());

		nearby.sort(java.util.Comparator.comparingDouble(player::distanceToSqr));
		if (nearby.isEmpty() || !CompanionArmor.unequip(player, nearby.getFirst())) {
			player.sendSystemMessage(Component.translatable("command.better-companions-justfatlard.unequip.none"));
			return 0;
		}

		player.sendSystemMessage(Component.translatable("command.better-companions-justfatlard.unequip",
			nearby.getFirst().getDisplayName()));
		return 1;
	}

	private static int addFriends(CommandSourceStack source, Collection<NameAndId> profiles) {
		Friends friends = Friends.get(source.getLevel());

		int added = 0;
		for (NameAndId profile : profiles) {
			if (friends.add(profile.id(), profile.name())) added++;
		}

		int count = added;
		source.sendSuccess(() -> Component.translatable(
			"command.better-companions-justfatlard.friend.added", count), true);
		return added;
	}

	private static int removeFriend(CommandSourceStack source, String name) {
		Friends friends = Friends.get(source.getLevel());

		UUID found = null;
		for (var entry : friends.all().entrySet()) {
			if (entry.getValue().equalsIgnoreCase(name)) {
				found = entry.getKey();
				break;
			}
		}

		if (found == null || !friends.remove(found)) {
			source.sendFailure(Component.translatable("command.better-companions-justfatlard.friend.unknown", name));
			return 0;
		}

		source.sendSuccess(() -> Component.translatable(
			"command.better-companions-justfatlard.friend.removed", name), true);
		return 1;
	}

	private static int listFriends(CommandSourceStack source) {
		Friends friends = Friends.get(source.getLevel());
		if (friends.all().isEmpty()) {
			source.sendSuccess(() -> Component.translatable("command.better-companions-justfatlard.friend.none"), false);
			return 0;
		}

		String names = String.join(", ", friends.all().values());
		source.sendSuccess(() -> Component.translatable(
			"command.better-companions-justfatlard.friend.list", friends.all().size(), names), false);
		return friends.all().size();
	}

	/** Kept so the level lookup reads the same way everywhere Friends is reached. */
	static ServerLevel levelOf(ServerPlayer player) {
		return player.level() instanceof ServerLevel level ? level : null;
	}
}
