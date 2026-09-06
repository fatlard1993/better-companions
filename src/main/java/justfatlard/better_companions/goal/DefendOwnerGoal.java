package justfatlard.better_companions.goal;

import justfatlard.better_companions.Companions;
import justfatlard.better_companions.Fed;
import justfatlard.better_companions.Friends;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.player.Player;

/**
 * Taking the owner's side, in both directions.
 *
 * <p>One goal rather than the game's two, because the question is the same either way: is there
 * something my person is fighting. Whether they started it does not change the answer.
 *
 * <p>It will not take the side of a fight against a friend. Somebody on the owner's team, or named
 * a friend by command, is never a target no matter who swung first - which is the difference
 * between a companion and a weapon.
 */
public class DefendOwnerGoal extends TargetGoal {

	private final Mob companion;
	private LivingEntity quarry;
	private int lastOwnerHurtStamp;

	public DefendOwnerGoal(Mob companion) {
		super(companion, false);
		this.companion = companion;
		setFlags(java.util.EnumSet.of(Flag.TARGET));
	}

	@Override
	public boolean canUse() {
		Player owner = Companions.findOwner(companion);
		if (owner == null) return false;
		if (Companions.isSitting(companion) && !StayGoal.hasSettled(companion)) return false;

		LivingEntity attacker = owner.getLastHurtByMob();
		LivingEntity victim = owner.getLastHurtMob();

		// Whichever is the more recent piece of news.
		quarry = owner.getLastHurtByMobTimestamp() >= owner.getLastHurtMobTimestamp() ? attacker : victim;
		if (quarry == null || quarry == companion) return false;
		if (Friends.isFriendly(quarry, owner)) return false;
		// A cow the owner is hitting is a farm, not a fight, if they fed it lately - or if they
		// have said the pack stays out of it with animals altogether.
		if (Companions.isPassive(quarry) && (Companions.sparesPassive(owner)
				|| Fed.recently(owner, quarry, companion.level().getGameTime()))) {
			return false;
		}

		int stamp = Math.max(owner.getLastHurtByMobTimestamp(), owner.getLastHurtMobTimestamp());
		if (stamp == lastOwnerHurtStamp) return false;
		lastOwnerHurtStamp = stamp;

		return canAttack(quarry, net.minecraft.world.entity.ai.targeting.TargetingConditions.DEFAULT);
	}

	@Override
	public void start() {
		companion.setTarget(quarry);
		super.start();
	}
}
