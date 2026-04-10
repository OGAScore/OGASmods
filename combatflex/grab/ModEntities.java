package com.OGAS.combatflex.grab;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {
	static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, GrabAndPullFeature.MOD_ID);

	static final RegistryObject<EntityType<ThrownBlockEntity>> THROWN_BLOCK = ENTITY_TYPES.register(
			"thrown_block",
			() -> EntityType.Builder.<ThrownBlockEntity>of(ThrownBlockEntity::new, MobCategory.MISC)
					.sized(0.98F, 0.98F)
					.clientTrackingRange(10)
					.updateInterval(1)
					.build(GrabAndPullFeature.MOD_ID + ":thrown_block")
	);

	static final RegistryObject<EntityType<ThrownItemEntity>> THROWN_ITEM = ENTITY_TYPES.register(
			"thrown_item",
			() -> EntityType.Builder.<ThrownItemEntity>of(ThrownItemEntity::new, MobCategory.MISC)
					.sized(0.35F, 0.35F)
					.clientTrackingRange(10)
					.updateInterval(1)
					.build(GrabAndPullFeature.MOD_ID + ":thrown_item")
	);

	private ModEntities() {
	}
}