package justfatlard.better_companions.mixin;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Reaches the target selector, which is protected where the goal selector is not.
 *
 * <p>Vanilla exposes {@code getGoalSelector()} and nothing for the other list, an asymmetry with no
 * meaning behind it - a mod adding "what should I attack" goals needs the same access as one adding
 * "what should I do" goals.
 */
@Mixin(Mob.class)
public interface MobAccessor {

	@Accessor("targetSelector")
	GoalSelector betterCompanions$targetSelector();
}
