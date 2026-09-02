package justfatlard.better_companions;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;

/**
 * What the mod remembers about one animal.
 *
 * <p>Kept as an attachment rather than by making these animals into something the game already has a
 * class for. A cow is a cow; the mod's business is who it has decided to walk beside, and that is a
 * note about the cow rather than a different kind of cow.
 *
 * @param owner     who tamed it, absent if nobody has
 * @param sitting   told to stay put
 * @param sittingSince the world time it was told, so "has it settled yet" is answerable without a
 *                     second counter that would have to be ticked and saved
 * @param post      where it was told to wait, so a settled companion can potter about and still be
 *                  where you left it
 */
public record CompanionState(Optional<UUID> owner, boolean sitting, long sittingSince,
		Optional<BlockPos> post) {

	public static final CompanionState NONE =
		new CompanionState(Optional.empty(), false, 0L, Optional.empty());

	/** Written as a string rather than through a UUID codec: one fewer piece of API to drift. */
	private static final Codec<UUID> UUID_CODEC =
		Codec.STRING.xmap(UUID::fromString, UUID::toString);

	public static final Codec<CompanionState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		UUID_CODEC.optionalFieldOf("owner").forGetter(CompanionState::owner),
		Codec.BOOL.optionalFieldOf("sitting", false).forGetter(CompanionState::sitting),
		Codec.LONG.optionalFieldOf("sitting_since", 0L).forGetter(CompanionState::sittingSince),
		BlockPos.CODEC.optionalFieldOf("post").forGetter(CompanionState::post)
	).apply(instance, CompanionState::new));

	public boolean isOwned() {
		return owner.isPresent();
	}

	public CompanionState withOwner(UUID newOwner) {
		return new CompanionState(Optional.of(newOwner), false, 0L, Optional.empty());
	}

	public CompanionState sittingAt(long worldTime, BlockPos where) {
		return new CompanionState(owner, true, worldTime, Optional.of(where));
	}

	public CompanionState standing() {
		return new CompanionState(owner, false, 0L, Optional.empty());
	}
}
