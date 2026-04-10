package com.OGAS.combatflex.grab;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

public class ThrownItemEntity extends Entity {
	private static final EntityDataAccessor<ItemStack> ITEM_STACK = SynchedEntityData.defineId(ThrownItemEntity.class, EntityDataSerializers.ITEM_STACK);
	private static final EntityDataAccessor<Integer> KIND_ID = SynchedEntityData.defineId(ThrownItemEntity.class, EntityDataSerializers.INT);

	public ThrownItemEntity(EntityType<? extends ThrownItemEntity> entityType, Level level) {
		super(entityType, level);
		this.noPhysics = true;
	}

	public void initialize(ItemStack stack, SpecialThrownItemKind kind, Vec3 centerPos, Vec3 velocity, float yaw, float pitch) {
		setItemStack(stack.copyWithCount(1));
		setThrownKind(kind);
		moveTo(centerPos.x, centerPos.y - (getBbHeight() * 0.5D), centerPos.z, yaw, pitch);
		setDeltaMovement(velocity);
		setNoGravity(true);
		this.noPhysics = true;
		this.hurtMarked = true;
	}

	public ItemStack getItemStack() {
		return this.entityData.get(ITEM_STACK);
	}

	public void setItemStack(ItemStack stack) {
		this.entityData.set(ITEM_STACK, stack.copyWithCount(1));
	}

	public SpecialThrownItemKind getThrownKind() {
		return SpecialThrownItemKind.fromId(this.entityData.get(KIND_ID));
	}

	public void setThrownKind(SpecialThrownItemKind kind) {
		this.entityData.set(KIND_ID, kind.ordinal());
	}

	@Override
	public void tick() {
		super.tick();
		if (level().isClientSide()) {
			return;
		}

		Vec3 velocity = getDeltaMovement();
		if (velocity.lengthSqr() > 1.0E-6D) {
			setPos(getX() + velocity.x, getY() + velocity.y, getZ() + velocity.z);
			this.hurtMarked = true;
		}
	}

	@Override
	protected void defineSynchedData() {
		this.entityData.define(ITEM_STACK, ItemStack.EMPTY);
		this.entityData.define(KIND_ID, SpecialThrownItemKind.NETHER_STAR.ordinal());
	}

	@Override
	protected void readAdditionalSaveData(CompoundTag tag) {
		setItemStack(ItemStack.of(tag.getCompound("Item")));
		setThrownKind(SpecialThrownItemKind.fromId(tag.getInt("KindId")));
		if (tag.contains("MotionX")) {
			setDeltaMovement(tag.getDouble("MotionX"), tag.getDouble("MotionY"), tag.getDouble("MotionZ"));
		}
	}

	@Override
	protected void addAdditionalSaveData(CompoundTag tag) {
		tag.put("Item", getItemStack().save(new CompoundTag()));
		tag.putInt("KindId", getThrownKind().ordinal());
		Vec3 velocity = getDeltaMovement();
		tag.putDouble("MotionX", velocity.x);
		tag.putDouble("MotionY", velocity.y);
		tag.putDouble("MotionZ", velocity.z);
	}

	@Override
	public Packet<ClientGamePacketListener> getAddEntityPacket() {
		return NetworkHooks.getEntitySpawningPacket(this);
	}
}