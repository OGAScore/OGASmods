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
				.requires(CombatFlexCommands::canUseOperatorCommands)
				.then(Commands.literal("unlock")
						.then(Commands.literal("tree")
								.executes(context -> unlockTree(context.getSource().getPlayerOrException())))
						.then(Commands.literal("active")
								.executes(context -> unlockActive(context.getSource().getPlayerOrException())))
						.then(Commands.literal("all")
								.executes(context -> unlockAll(context.getSource().getPlayerOrException()))))
				.then(Commands.literal("points")
						.then(Commands.literal("add")
								.then(Commands.argument("amount", IntegerArgumentType.integer(1))
										.executes(context -> addPoints(context.getSource().getPlayerOrException(),
												IntegerArgumentType.getInteger(context, "amount"))))))
				.then(Commands.literal("reset")
						.then(Commands.literal("skills")
								.executes(context -> resetSkills(context.getSource().getPlayerOrException())))
						.then(Commands.literal("special")
								.executes(context -> resetSpecialSkills(context.getSource().getPlayerOrException())))
						.then(Commands.literal("progression")
								.executes(context -> resetProgression(context.getSource().getPlayerOrException())))));
	}

	private static boolean canUseOperatorCommands(CommandSourceStack source) {
		return source.hasPermission(2) && source.getEntity() instanceof ServerPlayer;
	}

	private static int addPoints(ServerPlayer player, int amount) {
		int currentPoints = SkillData.addPoints(player, amount);
		CombatFlexMod.syncPlayerData(player);
		player.sendSystemMessage(Component.translatable("command.cbtflex.points_add.success", amount, currentPoints));
		return currentPoints;
	}

	private static int unlockTree(ServerPlayer player) {
		if (!SkillData.unlockSkillTree(player)) {
			player.sendSystemMessage(Component.translatable("command.cbtflex.unlock_tree.already_unlocked"));
			return 0;
		}

		CombatFlexMod.syncPlayerData(player);
		player.sendSystemMessage(Component.translatable("command.cbtflex.unlock_tree.success", SkillData.getPoints(player)));
		return 1;
	}

	private static int unlockActive(ServerPlayer player) {
		int unlockedCount = 0;
		SkillData.forceUnlockSkillTree(player);
		for (SkillType skillType : SkillType.values()) {
			if (skillType.branch() == SkillType.SkillBranch.ACTIVE && SkillData.forceUnlockSkill(player, skillType)) {
				unlockedCount++;
			}
		}

		for (SpecialSkillType skillType : SpecialSkillType.values()) {
			if (SkillData.forceUnlockSpecialSkill(player, skillType)) {
				unlockedCount++;
			}
		}

		CombatFlexMod.syncPlayerData(player);
		player.sendSystemMessage(Component.translatable("command.cbtflex.unlock_active.success", unlockedCount));
		return unlockedCount;
	}

	private static int unlockAll(ServerPlayer player) {
		int unlockedCount = 0;
		SkillData.forceUnlockSkillTree(player);
		for (SkillType skillType : SkillType.values()) {
			if (SkillData.forceUnlockSkill(player, skillType)) {
				unlockedCount++;
			}
		}

		for (SpecialSkillType skillType : SpecialSkillType.values()) {
			if (SkillData.forceUnlockSpecialSkill(player, skillType)) {
				unlockedCount++;
			}
		}

		FlightFeature.syncFlightAccess(player);
		CombatFlexMod.syncPlayerData(player);
		player.sendSystemMessage(Component.translatable("command.cbtflex.unlock_all.success", unlockedCount));
		return unlockedCount;
	}

	private static int resetSkills(ServerPlayer player) {
		int removedSkills = SkillData.getUnlockedSkillCount(player);
		SkillData.resetSkills(player);
		FlightFeature.syncFlightAccess(player);
		CombatFlexMod.syncPlayerData(player);
		player.sendSystemMessage(Component.translatable("command.cbtflex.reset_skills.success", removedSkills));
		return removedSkills;
	}

	private static int resetSpecialSkills(ServerPlayer player) {
		int removedSkills = SkillData.getUnlockedSpecialSkillCount(player);
		SkillData.resetSpecialSkills(player);
		CombatFlexMod.syncPlayerData(player);
		player.sendSystemMessage(Component.translatable("command.cbtflex.reset_special.success", removedSkills));
		return removedSkills;
	}

	private static int resetProgression(ServerPlayer player) {
		SkillData.resetProgression(player);
		CombatFlexMod.syncPlayerData(player);
		player.sendSystemMessage(Component.translatable("command.cbtflex.reset_progression.success"));
		return 1;
	}
}