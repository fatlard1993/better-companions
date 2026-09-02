package justfatlard.better_companions;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * People a companion will never turn on, whatever happens.
 *
 * <p>Two kinds, and they answer the same question. A permanent friend is named by command and is
 * remembered for good. A team-mate is anyone the game already says is on the owner's side - reading
 * vanilla's own teams rather than asking a server to keep a second list that means the same thing,
 * and getting scoreboard teams, plugins and anything else that sets them for free.
 *
 * <p>The list is deliberately one list, not one per player. A companion that mauls the person who
 * runs the server is everybody's problem, and the answer to it should not have to be repeated by
 * every owner in turn.
 */
public final class Friends extends SavedData {

	private static final String STORAGE_KEY = "better-companions-justfatlard:friends";

	/** A remembered friend, with the name to show when listing them. */
	public record Friend(UUID id, String name) {
		static final Codec<Friend> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			UUIDUtil.CODEC.fieldOf("id").forGetter(Friend::id),
			Codec.STRING.fieldOf("name").forGetter(Friend::name)
		).apply(instance, Friend::new));
	}

	private static final Codec<Friends> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		Friend.CODEC.listOf().optionalFieldOf("friends", List.of()).forGetter(Friends::asList)
	).apply(instance, Friends::new));

	private static final SavedDataType<Friends> TYPE = new SavedDataType<>(
		Identifier.parse(STORAGE_KEY), Friends::new, CODEC, DataFixTypes.LEVEL);

	private final Map<UUID, String> friends = new LinkedHashMap<>();

	private Friends() {}

	private Friends(List<Friend> saved) {
		for (Friend friend : saved) friends.put(friend.id(), friend.name());
	}

	public static Friends get(ServerLevel level) {
		return level.getServer().overworld().getDataStorage().computeIfAbsent(TYPE);
	}

	private List<Friend> asList() {
		List<Friend> out = new ArrayList<>(friends.size());
		friends.forEach((id, name) -> out.add(new Friend(id, name)));
		return out;
	}

	public boolean add(UUID id, String name) {
		if (friends.containsKey(id)) return false;

		friends.put(id, name);
		setDirty();
		return true;
	}

	public boolean remove(UUID id) {
		if (friends.remove(id) == null) return false;

		setDirty();
		return true;
	}

	public Map<UUID, String> all() {
		return friends;
	}

	public boolean contains(UUID id) {
		return friends.containsKey(id);
	}

	/**
	 * Whether a companion belonging to {@code owner} should leave this alone.
	 *
	 * <p>Safe to ask about anything at all, including a creeper, so callers can use it as their only
	 * check rather than remembering to ask what kind of thing they are holding first.
	 */
	public static boolean isFriendly(Entity target, Entity owner) {
		if (target == null || owner == null) return false;
		if (target == owner) return true;

		// The game's own answer first: same team, however that team was set.
		if (target.getTeam() != null && target.isAlliedTo(owner)) return true;

		if (!(target instanceof net.minecraft.world.entity.player.Player)
			&& !(target instanceof LivingEntity living && Companions.hasOwner(living))) {
			return false;
		}

		if (!(owner.level() instanceof ServerLevel level)) return false;

		// A named friend, or somebody else's companion whose owner is a named friend - turning on
		// a friend's dog is the same unkindness as turning on the friend.
		Friends list = get(level);
		if (target instanceof net.minecraft.world.entity.player.Player player) {
			return list.contains(player.getUUID());
		}

		UUID theirOwner = Companions.ownerOf(target);
		return theirOwner != null && (theirOwner.equals(owner.getUUID()) || list.contains(theirOwner));
	}
}
