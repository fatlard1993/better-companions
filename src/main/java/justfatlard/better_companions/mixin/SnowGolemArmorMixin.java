package justfatlard.better_companions.mixin;

import justfatlard.better_companions.CompanionArmor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.animal.golem.SnowGolem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A snow golem in companion armour does not melt.
 *
 * <p>A golem goes to slush in any warm place and in the rain, which makes one a companion for
 * the tundra and nowhere else. Armour is a coat as much as a plate: with a companion's barding
 * on, the heat does not get at it and the rain runs off, so it can follow its owner anywhere the
 * owner goes. Take the armour off and it is a snow golem again.
 */
@Mixin(SnowGolem.class)
public abstract class SnowGolemArmorMixin {
	@Redirect(method = "aiStep", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/entity/animal/golem/SnowGolem;hurtServer(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
	private boolean betterCompanions$noMelting(SnowGolem golem, ServerLevel level, DamageSource source, float amount) {
		if (CompanionArmor.isCompanionArmor(CompanionArmor.worn(golem))) return false;
		return golem.hurtServer(level, source, amount);
	}

	@Inject(method = "isSensitiveToWater", at = @At("HEAD"), cancellable = true)
	private void betterCompanions$coatKeepsRainOff(CallbackInfoReturnable<Boolean> cir) {
		SnowGolem golem = (SnowGolem) (Object) this;
		if (CompanionArmor.isCompanionArmor(CompanionArmor.worn(golem))) cir.setReturnValue(false);
	}
}
