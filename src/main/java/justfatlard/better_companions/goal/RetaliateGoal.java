package justfatlard.better_companions.goal;

import justfatlard.better_companions.Companions;
import justfatlard.better_companions.Friends;
import justfatlard.better_companions.Grudges;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;

/**
 * Fighting back, but not over one accident.
 *
 * <p>Everything that is not a person is answered straight away. A person gets the first one free -
 * see {@link Grudges} - and never gets it held against them at all if they own the animal or are on
 * its owner's side.
 */
public class RetaliateGoal extends TargetGoal {

	private final Mob companion;

	public RetaliateGoal(Mob companion) {
		super(companion, true);
		this.companion = companion;
		setFlags(java.util.EnumSet.of(Flag.TARGET));
	}

	@Override
	public boolean canUse() {
		// Goals are built when an animal loads and torn down when it unloads, so a released one
		// carries this until it next sleeps. Every other companion goal asks who the owner is and
		// falls silent when there is nobody; this one did not, and a let-go cow went on fighting
		// like a dog for the rest of the afternoon.
		if (!Companions.hasOwner(companion)) return false;

		LivingEntity attacker = companion.getLastHurtByMob();
		if (attacker == null || attacker == companion) return false;

		Player owner = Companions.findOwner(companion);
		if (owner != null && Friends.isFriendly(attacker, owner)) return false;

		if (attacker instanceof Player player) {
			if (Companions.isOwnedBy(companion, player)) return false;
			if (!Grudges.holdsAgainst(companion, player, companion.level().getGameTime())) return false;
		}

		return canAttack(attacker, TargetingConditions.DEFAULT);
	}

	@Override
	public void start() {
		companion.setTarget(companion.getLastHurtByMob());
		super.start();
	}
}
