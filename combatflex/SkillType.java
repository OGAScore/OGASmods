package com.OGAS.combatflex;

import java.util.Arrays;
import java.util.Optional;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public enum SkillType {
	EMERGENCY_RESTORE("i_immortal", "screen.cbtflex.skill.emergency_restore.name",
			"screen.cbtflex.skill.emergency_restore.desc", Items.TOTEM_OF_UNDYING, 1, 20L * 60L, "emergency_restore"),
	FIRST_HIT_GUARD("i_guard", "screen.cbtflex.skill.first_hit_guard.name",
			"screen.cbtflex.skill.first_hit_guard.desc", Items.SHIELD, 1, 20L * 60L, "first_hit_guard"),
	REPULSION_BURST("you_leave", "screen.cbtflex.skill.repulsion_burst.name",
			"screen.cbtflex.skill.repulsion_burst.desc", Items.PISTON, 1, 20L * 30L, "repulsion_burst"),
	RETALIATION_CRIT("the_black_horse", "screen.cbtflex.skill.retaliation_crit.name",
			"screen.cbtflex.skill.retaliation_crit.desc", Items.DIAMOND_SWORD, 1, 0L, "retaliation_crit"),
	NO_KNOCKBACK("i_stand", "screen.cbtflex.skill.no_knockback.name",
			"screen.cbtflex.skill.no_knockback.desc", Items.NETHERITE_CHESTPLATE, 2, 0L, "no_knockback"),
	DOT_IMMUNITY("i_adapt", "screen.cbtflex.skill.dot_immunity.name",
			"screen.cbtflex.skill.dot_immunity.desc", Items.MILK_BUCKET, 2, 0L, "dot_immunity"),
	HALF_DAMAGE("i_split", "screen.cbtflex.skill.half_damage.name",
			"screen.cbtflex.skill.half_damage.desc", Items.NETHER_STAR, 3, 0L, "half_damage"),
	SECOND_WIND("hell_on_wheels", "screen.cbtflex.skill.second_wind.name",
			"screen.cbtflex.skill.second_wind.desc", Items.ENCHANTED_GOLDEN_APPLE, 2, 20L * 300L, "second_wind"),
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
		COMBAT,
		FLIGHT
	}
}