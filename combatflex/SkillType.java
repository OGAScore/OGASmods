package com.OGAS.combatflex;

import java.util.Arrays;
import java.util.Optional;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public enum SkillType {
	EMERGENCY_RESTORE("i_immortal", "screen.cbtflex.skill.emergency_restore.name",
			"screen.cbtflex.skill.emergency_restore.desc", Items.TOTEM_OF_UNDYING, 1, 1, SkillBranch.SURVIVAL,
			20L * 60L, "emergency_restore"),
	FIRST_HIT_GUARD("i_guard", "screen.cbtflex.skill.first_hit_guard.name",
			"screen.cbtflex.skill.first_hit_guard.desc", Items.SHIELD, 1, 1, SkillBranch.SURVIVAL, 20L * 60L,
			"first_hit_guard"),
	REPULSION_BURST("you_leave", "screen.cbtflex.skill.repulsion_burst.name",
			"screen.cbtflex.skill.repulsion_burst.desc", Items.PISTON, 1, 1, SkillBranch.COMBAT, 20L * 30L,
			"repulsion_burst"),
	RETALIATION_CRIT("the_black_horse", "screen.cbtflex.skill.retaliation_crit.name",
			"screen.cbtflex.skill.retaliation_crit.desc", Items.DIAMOND_SWORD, 1, 1, SkillBranch.COMBAT, 0L,
			"retaliation_crit"),
	NO_KNOCKBACK("i_stand", "screen.cbtflex.skill.no_knockback.name",
			"screen.cbtflex.skill.no_knockback.desc", Items.NETHERITE_CHESTPLATE, 2, 2, SkillBranch.SURVIVAL, 0L,
			"no_knockback"),
	DOT_IMMUNITY("i_adapt", "screen.cbtflex.skill.dot_immunity.name",
			"screen.cbtflex.skill.dot_immunity.desc", Items.MILK_BUCKET, 2, 2, SkillBranch.SURVIVAL, 0L,
			"dot_immunity"),
	SECOND_WIND("hell_on_wheels", "screen.cbtflex.skill.second_wind.name",
			"screen.cbtflex.skill.second_wind.desc", Items.ENCHANTED_GOLDEN_APPLE, 2, 2, SkillBranch.SURVIVAL,
			20L * 300L, "second_wind"),
	BATTLE_MOMENTUM("battle_momentum", "screen.cbtflex.skill.battle_momentum.name",
			"screen.cbtflex.skill.battle_momentum.desc", Items.RABBIT_FOOT, 2, 2, SkillBranch.COMBAT, 0L),
	ACTIVE_HEAVY_ATTACK("active_heavy_attack", "screen.cbtflex.skill.active_heavy_attack.name",
			"screen.cbtflex.skill.active_heavy_attack.desc", Items.IRON_AXE, 1, 1, SkillBranch.ACTIVE, 0L),
	ACTIVE_ENTITY_GRAB("active_entity_grab", "screen.cbtflex.skill.active_entity_grab.name",
			"screen.cbtflex.skill.active_entity_grab.desc", Items.LEAD, 1, 1, SkillBranch.ACTIVE, 0L),
	ACTIVE_BLOCK_CONTROL("active_block_control", "screen.cbtflex.skill.active_block_control.name",
			"screen.cbtflex.skill.active_block_control.desc", Items.PISTON, 2, 2, SkillBranch.ACTIVE, 0L),
	ACTIVE_CHARGED_HEAVY("active_charged_heavy", "screen.cbtflex.skill.active_charged_heavy.name",
			"screen.cbtflex.skill.active_charged_heavy.desc", Items.DIAMOND_AXE, 2, 2, SkillBranch.ACTIVE, 0L),
	HALF_DAMAGE("i_split", "screen.cbtflex.skill.half_damage.name",
			"screen.cbtflex.skill.half_damage.desc", Items.NETHER_STAR, 3, 3, SkillBranch.SURVIVAL, 0L,
			"half_damage"),
	EXECUTIONER("executioner", "screen.cbtflex.skill.executioner.name",
			"screen.cbtflex.skill.executioner.desc", Items.NETHERITE_AXE, 3, 3, SkillBranch.COMBAT, 0L),
	ACTIVE_SPECIAL_THROW("active_special_throw", "screen.cbtflex.skill.active_special_throw.name",
			"screen.cbtflex.skill.active_special_throw.desc", Items.FIRE_CHARGE, 3, 3, SkillBranch.ACTIVE, 0L),
	ACTIVE_EXECUTION("active_execution", "screen.cbtflex.skill.active_execution.name",
			"screen.cbtflex.skill.active_execution.desc", Items.WITHER_SKELETON_SKULL, 3, 3, SkillBranch.ACTIVE, 0L),
	SLOW_FLIGHT("slow_flight", "screen.cbtflex.skill.slow_flight.name",
			"screen.cbtflex.skill.slow_flight.desc", Items.FEATHER, 2, 2, SkillBranch.FLIGHT, 0L),
	FAST_FLIGHT("fast_flight", "screen.cbtflex.skill.fast_flight.name",
			"screen.cbtflex.skill.fast_flight.desc", Items.ELYTRA, 3, 3, SkillBranch.FLIGHT, 0L);

	private final String id;
	private final String nameKey;
	private final String descriptionKey;
	private final Item iconItem;
	private final int cost;
	private final int tier;
	private final SkillBranch branch;
	private final long cooldownTicks;
	private final String[] legacyIds;

	SkillType(String id, String nameKey, String descriptionKey, Item iconItem, int cost, long cooldownTicks,
			String... legacyIds) {
		this(id, nameKey, descriptionKey, iconItem, cost, cost, SkillBranch.COMBAT, cooldownTicks, legacyIds);
	}

	SkillType(String id, String nameKey, String descriptionKey, Item iconItem, int cost, int tier,
			SkillBranch branch, long cooldownTicks, String... legacyIds) {
		this.id = id;
		this.nameKey = nameKey;
		this.descriptionKey = descriptionKey;
		this.iconItem = iconItem;
		this.cost = cost;
		this.tier = tier;
		this.branch = branch;
		this.cooldownTicks = cooldownTicks;
		this.legacyIds = legacyIds;
	}

	public String id() {
		return this.id;
	}

	public String nameKey() {
		return this.nameKey;
	}

	public String descriptionKey() {
		return this.descriptionKey;
	}

	public ItemStack iconStack() {
		return new ItemStack(this.iconItem);
	}

	public int cost() {
		return this.cost;
	}

	public int tier() {
		return this.tier;
	}

	public SkillBranch branch() {
		return this.branch;
	}

	public long cooldownTicks() {
		return this.cooldownTicks;
	}

	public String[] legacyIds() {
		return this.legacyIds;
	}

	public boolean matchesId(String id) {
		return this.id.equals(id) || Arrays.asList(this.legacyIds).contains(id);
	}

	public static Optional<SkillType> fromId(String id) {
		return Arrays.stream(values()).filter(skillType -> skillType.matchesId(id)).findFirst();
	}

	public enum SkillBranch {
		SURVIVAL,
		COMBAT,
		ACTIVE,
		FLIGHT
	}
}