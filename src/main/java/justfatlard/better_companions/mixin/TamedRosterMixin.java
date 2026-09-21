package justfatlard.better_companions.mixin;

import justfatlard.better_companions.Roster;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The roster is written when a companion loads and when it is put away, which covers every animal
 * the mod did not just this moment acquire. A wolf tamed with a bone is that one exception: it was
 * loaded long before it was anybody's, and it may not unload for hours. Whistled for in between, it
 * was not on the list and did not answer.
 *
 * <p>Taming is the moment an animal becomes somebody's, and every way of doing it - the game's
 * bones and this mod's offerings alike - ends here.
 */
@Mixin(TamableAnimal.class)
public abstract class TamedRosterMixin {
	@Inject(method = "tame", at = @At("TAIL"))
	private void betterCompanions$roster(Player player, CallbackInfo ci) {
		Roster.note((TamableAnimal) (Object) this);
	}
}
