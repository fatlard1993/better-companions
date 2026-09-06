package justfatlard.better_companions.mixin;

import justfatlard.better_companions.Companions;
import justfatlard.better_companions.Fed;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The game's own tamed animals keep the game's own targeting, and the game does not know who
 * an owner's friends are. A wolf took its owner's side against a named friend, and one tap from
 * a friend was answered with the whole pack: the friend list was checked by every goal this mod
 * installs, and not by the four vanilla ones a wolf runs on instead.
 *
 * <p>Every one of those goals ends in {@code setTarget}, so that is where the owner's rules
 * are applied: a friend is never a target, and a passive mob is not one either when the owner
 * has fed it lately or asked for animals to be left alone - unless it is the one that struck
 * the animal, which is its own business to answer.
 */
@Mixin(Mob.class)
public abstract class TamedTargetMixin {
	@Inject(method = "setTarget", at = @At("HEAD"), cancellable = true)
	private void betterCompanions$ownersRules(LivingEntity target, CallbackInfo ci) {
		Mob self = (Mob) (Object) this;
		if (target == null || !(self instanceof TamableAnimal) || self.level().isClientSide()) return;
		Player owner = Companions.findOwner(self);
		if (owner == null) return;

		if (justfatlard.better_companions.Friends.isFriendly(target, owner)) {
			ci.cancel();
			return;
		}
		if (Companions.isPassive(target) && target != self.getLastHurtByMob()
				&& (Companions.sparesPassive(owner) || Fed.recently(owner, target, self.level().getGameTime()))) {
			ci.cancel();
		}
	}
}
