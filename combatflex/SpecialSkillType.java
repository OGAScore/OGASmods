package com.OGAS.combatflex;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public enum SpecialSkillType {
	WAR_CRY("war_cry", "screen.cbtflex.special_skill.war_cry.name",
			"screen.cbtflex.special_skill.war_cry.desc", Items.GOAT_HORN, 1, 1, 20L * 15L),
	SHOCKWAVE("shockwave", "screen.cbtflex.special_skill.shockwave.name",
			"screen.cbtflex.special_skill.shockwave.desc", Items.FIRE_CHARGE, 1, 2, 20L * 30L),
	DODGE("dodge", "screen.cbtflex.special_skill.dodge.name",
			"screen.cbtflex.special_skill.dodge.desc", Items.RABBIT_FOOT, 1, 3, 20L * 60L);

	private final String id;
	private final String nameKey;
	private final String descriptionKey;
	private final Item iconItem;
	private final int cost;
	private final int tier;
	private final long cooldownTicks;

	SpecialSkillType(String id, String nameKey, String descriptionKey, Item iconItem, int cost, int tier,
			long cooldownTicks) {
		this.id = id;
		this.nameKey = nameKey;
		this.descriptionKey = descriptionKey;
		this.iconItem = iconItem;
		this.cost = cost;
		this.tier = tier;
		this.cooldownTicks = cooldownTicks;
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

	public long cooldownTicks() {
		return this.cooldownTicks;
	}

	public static SpecialSkillType fromId(String id) {
		for (SpecialSkillType specialSkillType : values()) {
			if (specialSkillType.id.equals(id)) {
				return specialSkillType;
			}
		}
		return null;
	}
}