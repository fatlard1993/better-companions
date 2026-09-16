package justfatlard.better_companions.integration;

import java.util.ArrayList;
import java.util.List;
import justfatlard.better_companions.Companions;
import justfatlard.village_quests.api.DialogueRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import java.util.concurrent.ThreadLocalRandom;

/**
 * What a village makes of an animal that has decided it belongs to somebody.
 *
 * <p>Registered with Village Quests when that mod is present. The whole remark
 * rests on one fact this mod created and nothing in the game will comment on:
 * a cow that follows a person. Every villager here works with animals for a
 * living and every one of them knows that animals do not do that, so the thing
 * at your heel is a small daily impossibility standing in their square.
 *
 * <p>Dogs are exempt on purpose. Wolves, cats and parrots were tameable before
 * this mod and a villager would not look twice, so the gate skips anything the
 * game already tames and fires only for the sixteen this mod added. The lines
 * name the animal actually following you, because "a cow" is the joke and
 * "an animal" is not.
 *
 * <p>This class must only be touched behind a mod-loaded check. It refers to
 * Village Quests types directly, so loading it without that mod present throws.
 */
public final class CompanionRemarks {
	private CompanionRemarks() {}

	/** Close enough that the villager is looking at it while they talk to you. */
	private static final double SIGHT = 16.0;

	/** Sometimes. A village that comments on your dog every single time is a village of one joke. */
	private static final double REMARK_CHANCE = 0.3;

	/** What is at your heel right now, or null. */
	private record Retinue(String animal, int count) {}

	private static Retinue retinue(Villager villager, ServerPlayer player) {
		if (!(villager.level() instanceof ServerLevel world)) return null;

		List<Animal> owned = world.getEntities(EntityTypeTest.forClass(Animal.class),
			new AABB(villager.blockPosition()).inflate(SIGHT),
			animal -> !(animal instanceof TamableAnimal) && Companions.isOwnedBy(animal, player));
		if (owned.isEmpty()) return null;

		Entity nearest = owned.get(0);
		double best = Double.MAX_VALUE;
		for (Animal animal : owned) {
			double distance = animal.distanceToSqr(villager);
			if (distance < best) {
				best = distance;
				nearest = animal;
			}
		}

		return new Retinue(nearest.getType().getDescription().getString().toLowerCase(), owned.size());
	}

	private record Node(String text, String walkAway, List<Branch> branches) {}

	private record Branch(String label, Node node) {}

	/** A line the villager opens with, built from whatever is actually following you. */
	private interface Opening {
		Node build(String animal, int count);
	}

	private record Topic(String id, int minReputation, String question, Opening opening) {}

	private static Node close(String text, String walkAway) {
		return new Node(text, walkAway, List.of());
	}

	private static Node node(String text, String walkAway, Branch... branches) {
		return new Node(text, walkAway, List.of(branches));
	}

	private static Branch then(String label, Node node) {
		return new Branch(label, node);
	}

	/** "the cow", or "the three of them" once it stops being one animal and starts being a following. */
	private static String them(String animal, int count) {
		return count > 2 ? "that lot behind you" : "that " + animal;
	}

	public static void register() {
		topics("shepherd", List.of(
			new Topic("bc_follows", 0, "You keep looking past me.", (animal, count) ->
				node("Because there is a " + animal + " stood behind you waiting for you to move, and I have spent my whole life "
						+ "getting animals to do that on purpose and failing.",
					"It just started following me.",
					then("Is that so hard?",
						close("I have a crook, a dog and a whistle. You have apparently got whatever you have got. "
								+ "Do not tell me what it is. I would rather think you were born with it.",
							"I'll let you think that.")),
					then("You can have it back.",
						close("It is not mine and it would not come. That is rather the trouble with what you have done to it.",
							"Fair enough.")))),

			new Topic("bc_wait", 25, "Does it bother you? " + "The following.", (animal, count) ->
				node("It bothers me that it waits. I have watched " + them(animal, count) + " stand in one spot for an hour "
						+ "because you told it to. Mine will not stand still while I am holding the feed.",
					"They're patient.",
					then("What would you do with one?",
						close("Nothing useful, and that is what stops me asking you how. An animal that waits is an animal you can "
								+ "leave somewhere, and everything I own is worth exactly what it is worth because it cannot be.",
							"That's a hard way to put it."))))));

		topics("farmer", List.of(
			new Topic("bc_pen", 0, "Something wrong?", (animal, count) ->
				node("I have spent two seasons on that fence. Post, rail, gate, the lot. And you have walked a " + animal
						+ " through the middle of my village without so much as a bit of string.",
					"Sorry about the fence.",
					then("The fence still works.",
						close("The fence works on animals that want out. Nothing I have built has ever had to work on one that "
								+ "wanted to be somewhere else in particular.",
							"Hadn't thought of it like that.")),
					then("Would you want one?",
						close("*looks at " + them(animal, count) + " for a while* No. I would spend the whole season wondering what "
								+ "it was thinking, and the wheat would not care for that at all.",
							"Probably wise."))))));

		topics("butcher", List.of(
			new Topic("bc_stock", 20, "You've gone quiet.", (animal, count) ->
				node("*at the block, not working* That " + animal + " came in with you and stood where you stopped. "
						+ "I have had a thousand through here and not one of them has ever chosen a person.",
					"It's not stock.",
					then("Does that change anything?",
						close("Not for the ones in the pen. It changes what I have to be careful not to think about, "
								+ "which is a different job and one I do not get paid for.",
							"I'll keep it out of your way.")),
					then("*say nothing*",
						close("*goes back to the block after a moment* Good. That was the right amount to say about it.",
							"*nod*"))))));

		justfatlard.better_companions.Main.LOGGER.info("Registered companion remarks with Village Quests");

		topics("cartographer", List.of(
			new Topic("bc_whistle", 30, "You've been watching them come and go.", (animal, count) ->
				node("You went east this morning without " + them(animal, count) + ", and now they are here and you are here. "
						+ "I have been trying to work out which of you did the walking.",
					"They found their way.",
					then("How far will they come?",
						close("That is exactly what I want to know and exactly what nobody will tell me. I have started marking "
								+ "where I see them and where I last saw you. The lines are not sensible.",
							"Keep marking."))))));
	}

	/**
	 * One profession's remarks, offered only while something is actually
	 * following the player, and only sometimes even then.
	 */
	private static void topics(String profession, List<Topic> topics) {
		for (Topic topic : topics) {
			DialogueRegistry.registerRichDialogueHandler(topic.id(), (villager, player, id) -> {
				Retinue seen = retinue(villager, player);
				// Gone between the screen opening and the click. They can hardly
				// talk about a thing that has wandered off.
				if (seen == null) {
					return DialogueRegistry.Reply.of("*looks past you again, then back* ...It has gone. Never mind.")
						.walkAway("Never mind.");
				}
				return reply(topic.opening().build(seen.animal(), seen.count()));
			});
		}

		DialogueRegistry.registerProfessionDialogue(profession, (villager, player, reputation) -> {
			ThreadLocalRandom rng = ThreadLocalRandom.current();
			if (rng.nextDouble() >= REMARK_CHANCE) return List.of();
			if (retinue(villager, player) == null) return List.of();

			List<Topic> fitting = new ArrayList<>();
			for (Topic topic : topics) {
				if (reputation >= topic.minReputation()) fitting.add(topic);
			}
			if (fitting.isEmpty()) return List.of();

			Topic picked = fitting.get(rng.nextInt(fitting.size()));
			return List.of(new DialogueRegistry.DialogueOption(picked.id(),
				Component.literal(picked.question()), picked.minReputation(), Integer.MAX_VALUE));
		});
	}

	private static DialogueRegistry.Reply reply(Node node) {
		DialogueRegistry.Reply reply = DialogueRegistry.Reply.of(node.text());
		if (node.walkAway() != null) reply.walkAway(node.walkAway());
		for (Branch branch : node.branches()) {
			reply.option(branch.label(), (villager, player, id) -> reply(branch.node()));
		}
		return reply;
	}
}
