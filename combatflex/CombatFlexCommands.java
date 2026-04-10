package com.OGAS.combatflex;

import com.OGAS.combatflex.flight.FlightFeature;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

final class CombatFlexCommands {
	private CombatFlexCommands() {
	}

	static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("cbtflex")
				.requires(CombatFlexCommands::canUseCreativeCommands)
				.then(Commands.literal("points")
						.then(Commands.literal("add")
								.then(Commands.argument("amount", IntegerArgumentType.integer(1))
										.executes(context -> addPoints(context.getSource().getPlayerOrException(),
												IntegerArgumentType.getInteger(context, "amount"))))))
				.then(Commands.literal("reset")
						.then(Commands.literal("skills")
								.executes(context -> resetSkills(context.getSource().getPlayerOrException())))
						.then(Commands.literal("progression")
								.executes(context -> resetProgression(context.getSource().getPlayerOrException())))));
	}

	private static boolean canUseCreativeCommands(CommandSourceStack source) {
		return source.getEntity() instanceof ServerPlayer player && player.isCreative();
	}

	private static int addPoints(ServerPlayer player, int amount) {
		int currentPoints = SkillData.addPoints(player, amount);
		CombatFlexMod.syncPlayerData(player);
		player.sendSystemMessage(Component.translatable("command.cbtflex.points_add.success", amount, currentPoints));
		return currentPoints;
	}

	private static int resetSkills(ServerPlayer player) {
		int removedSkills = SkillData.getUnlockedSkillCount(player);
		SkillData.resetSkills(player);
		FlightFeature.syncFlightAccess(player);
		CombatFlexMod.syncPlayerData(player);
		player.sendSystemMessage(Component.translatable("command.cbtflex.reset_skills.success", removedSkills));
		return removedSkills;
	}

	private static int resetProgression(ServerPlayer player) {
		SkillData.resetProgression(player);
		CombatFlexMod.syncPlayerData(player);
		player.sendSystemMessage(Component.translatable("command.cbtflex.reset_progression.success"));
		return 1;
	}
}