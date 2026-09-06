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
 * People a player's companions will never turn on, whatever happens.
 *
 * <p>Two kinds, and they answer the same question. A named friend is somebody the owner has
 * said is on their side, so a stray hit from them - a misjudged swing in a fight, an arrow
 * through a doorway - is not a reason for the dogs to go for their throat. A team-mate is
 * anyone the game already says is on the owner's side, read from vanilla's own teams rather
 * than kept as a second list that means the same thing.
 *
 * <p>Each player names their own friends. Who you trust around your animals is your call, and
 * a friend of one owner is no promise about anyone else's pack.
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

	/** One owner and the friends they named. */
	private record Owner(UUID id, List<Friend> friends) {
		static final Codec<Owner> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			UUIDUtil.CODEC.fieldOf("owner").forGetter(Owner::id),
			Friend.CODEC.listOf().fieldOf("friends").forGetter(Owner::friends)
		).apply(instance, Owner::new));
	}

	/**
	 * Under a new field: what was here before was one list for the whole server, and an old
	 * file reads as nobody having named anyone yet rather than as a broken file.
	 */
	private static final Codec<Friends> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		Owner.CODEC.listOf().optionalFieldOf("owners", List.of()).forGetter(Friends::asList)
	).apply(instance, Friends::new));

	private static final SavedDataType<Friends> TYPE = new SavedDataType<>(
		Identifier.parse(STORAGE_KEY), Friends::new, CODEC, DataFixTypes.LEVEL);

	private final Map<UUID, Map<UUID, String>> byOwner = new LinkedHashMap<>();

	private Friends() {}

	private Friends(List<Owner> saved) {
		for (Owner owner : saved) {
			Map<UUID, String> friends = new LinkedHashMap<>();
			for (Friend friend : owner.friends()) friends.put(friend.id(), friend.name());
			byOwner.put(owner.id(), friends);
		}
	}

	public static Friends get(ServerLevel level) {
		return level.getServer().overworld().getDataStorage().computeIfAbsent(TYPE);
	}

	private List<Owner> asList() {
		List<Owner> out = new ArrayList<>(byOwner.size());
		byOwner.forEach((owner, friends) -> {
			List<Friend> named = new ArrayList<>(friends.size());
			friends.forEach((id, name) -> named.add(new Friend(id, name)));
			out.add(new Owner(owner, named));
		});
		return out;
	}

	/** @return true where this was not already a friend of the owner's */
	public boolean add(UUID owner, UUID friend, String name) {
		Map<UUID, String> friends = byOwner.computeIfAbsent(owner, id -> new LinkedHashMap<>());
		if (friends.containsKey(friend)) return false;

		friends.put(friend, name);
		setDirty();
		return true;
	}

	/** @return true where this was a friend of the owner's and now is not */
	public boolean remove(UUID owner, UUID friend) {
		Map<UUID, String> friends = byOwner.get(owner);
		if (friends == null || friends.remove(friend) == null) return false;

		if (friends.isEmpty()) byOwner.remove(owner);
		setDirty();
		return true;
	}

	/** The owner's friends, id to name, in the order they were named. */
	public Map<UUID, String> of(UUID owner) {
		Map<UUID, String> friends = byOwner.get(owner);
		return friends == null ? Map.of() : Map.copyOf(friends);
	}

	public boolean contains(UUID owner, UUID friend) {
		Map<UUID, String> friends = byOwner.get(owner);
		return friends != null && friends.containsKey(friend);
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

		// A friend the owner named, or somebody else's companion whose owner is one - turning on
		// a friend's dog is the same unkindness as turning on the friend.
		Friends list = get(level);
		if (target instanceof net.minecraft.world.entity.player.Player player) {
			return list.contains(owner.getUUID(), player.getUUID());
		}

		UUID theirOwner = Companions.ownerOf(target);
		return theirOwner != null && (theirOwner.equals(owner.getUUID()) || list.contains(owner.getUUID(), theirOwner));
	}
}
