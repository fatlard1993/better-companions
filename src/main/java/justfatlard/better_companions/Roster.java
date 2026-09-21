package justfatlard.better_companions;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Which chunk each companion went to sleep in.
 *
 * <p>A whistle has to reach an animal the server is not currently thinking about: one left at home
 * while you are in the nether, or a thousand blocks behind you. Such an animal is in no list the
 * game will hand out - it is a few bytes in a region file, and the only way to reach it is to know
 * which chunk to wake. So every companion writes down where it is when it loads and again when it
 * is put away, and nothing reads any of it until somebody whistles.
 *
 * <p>One line per companion, kept beside the world rather than on the animal, because the animal
 * is exactly what is missing when the question gets asked.
 */
public final class Roster extends SavedData {

	private static final String STORAGE_KEY = "better-companions-justfatlard:roster";

	/**
	 * One companion, the last place the server had it, and what to call it there.
	 *
	 * <p>The name is written down rather than looked up, because everything that reads this list is
	 * asking about animals that are not here to be asked. A list of bare identifiers would be a
	 * list nobody could choose from.
	 */
	public record Whereabouts(UUID companion, UUID owner, ResourceKey<Level> dimension, BlockPos where,
			String name) {
		static final Codec<Whereabouts> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			UUIDUtil.CODEC.fieldOf("companion").forGetter(Whereabouts::companion),
			UUIDUtil.CODEC.fieldOf("owner").forGetter(Whereabouts::owner),
			Level.RESOURCE_KEY_CODEC.fieldOf("dimension").forGetter(Whereabouts::dimension),
			BlockPos.CODEC.fieldOf("where").forGetter(Whereabouts::where),
			Codec.STRING.optionalFieldOf("name", "").forGetter(Whereabouts::name)
		).apply(instance, Whereabouts::new));
	}

	private static final Codec<Roster> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		Whereabouts.CODEC.listOf().optionalFieldOf("companions", List.of()).forGetter(Roster::asList)
	).apply(instance, Roster::new));

	private static final SavedDataType<Roster> TYPE = new SavedDataType<>(
		Identifier.parse(STORAGE_KEY), Roster::new, CODEC, DataFixTypes.LEVEL);

	private final Map<UUID, Whereabouts> seen = new LinkedHashMap<>();

	private Roster() {}

	private Roster(List<Whereabouts> saved) {
		for (Whereabouts one : saved) seen.put(one.companion(), one);
	}

	public static Roster get(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(TYPE);
	}

	private List<Whereabouts> asList() {
		return new ArrayList<>(seen.values());
	}

	/** Write down where a companion is standing now. */
	public static void note(Entity entity) {
		if (!(entity.level() instanceof ServerLevel level)) return;

		UUID owner = Companions.ownerOf(entity);
		if (owner == null) return;

		Roster roster = get(level.getServer());
		roster.seen.put(entity.getUUID(), new Whereabouts(entity.getUUID(), owner, level.dimension(),
			entity.blockPosition(), entity.getDisplayName().getString()));
		roster.setDirty();
	}

	/**
	 * The game has finished with this animal, and what that means depends on how.
	 *
	 * <p>Put away into its chunk, it is still out there under this name and the line is worth
	 * writing. Destroyed - killed, or discarded by a command - there is nothing left to call and
	 * the line goes. Between those sits the animal that has just crossed a dimension: the copy that
	 * came out the other side carries the same name and has already written its own line, so this
	 * one says nothing rather than talking over it with an address it has just left.
	 */
	public static void noteOrForget(Entity entity) {
		Entity.RemovalReason reason = entity.getRemovalReason();
		if (reason == null || reason.shouldSave()) {
			note(entity);
		} else if (reason.shouldDestroy()) {
			forget(entity);
		}
	}

	public static void forget(Entity entity) {
		if (entity.level() instanceof ServerLevel level) get(level.getServer()).drop(entity.getUUID());
	}

	public void drop(UUID companion) {
		if (seen.remove(companion) != null) setDirty();
	}

	/** One companion's line, or null if the list has never had it. */
	public Whereabouts find(UUID companion) {
		return seen.get(companion);
	}

	/** Everywhere this player's companions were last had. */
	public List<Whereabouts> of(UUID owner) {
		List<Whereabouts> mine = new ArrayList<>();
		for (Whereabouts one : seen.values()) {
			if (one.owner().equals(owner)) mine.add(one);
		}
		return mine;
	}
}
