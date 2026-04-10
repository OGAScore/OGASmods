package com.OGAS.combatflex.grab;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

enum SpecialThrownItemKind {
	NETHER_STAR(6.0D, 32),
	END_CRYSTAL(6.0D, 28),
	IRON_NUGGET(10.0D, 80),
	NETHERITE_BLOCK(6.0D, 28);

	private final double throwSpeed;
	private final int maxTicks;

	SpecialThrownItemKind(double throwSpeed, int maxTicks) {
		this.throwSpeed = throwSpeed;
		this.maxTicks = maxTicks;
	}

	double getThrowSpeed() {
		return throwSpeed;
	}

	int getMaxTicks() {
		return maxTicks;
	}

	boolean preservesItemOnImpact() {
		return this == NETHERITE_BLOCK;
	}

	static SpecialThrownItemKind fromStack(ItemStack stack) {
		Item item = stack.getItem();
		if (item == Items.NETHER_STAR) {
			return NETHER_STAR;
		}
		if (item == Items.END_CRYSTAL) {
			return END_CRYSTAL;
		}
		if (item == Items.IRON_NUGGET) {
			return IRON_NUGGET;
		}
		if (item == Blocks.NETHERITE_BLOCK.asItem()) {
			return NETHERITE_BLOCK;
		}

		return null;
	}

	static SpecialThrownItemKind fromId(int id) {
		SpecialThrownItemKind[] values = values();
		return id >= 0 && id < values.length ? values[id] : NETHER_STAR;
	}
}