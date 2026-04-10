package com.OGAS.combatflex.grab;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

public class ThrownBlockEntity extends Entity {
	private static final EntityDataAccessor<Integer> BLOCK_STATE_ID = SynchedEntityData.defineId(ThrownBlockEntity.class, EntityDataSerializers.INT);

	public ThrownBlockEntity(EntityType<? extends ThrownBlockEntity> entityType, Level level) {
		super(entityType, level);
		this.noPhysics = true;
	}

	public void initialize(BlockState blockState, Vec3 centerPos, Vec3 velocity, float yaw, float pitch) {
		setBlockState(blockState);
		moveTo(centerPos.x, centerPos.y - (getBbHeight() * 0.5D), centerPos.z, yaw, pitch);
		setDeltaMovement(velocity);
		setNoGravity(true);
		this.noPhysics = true;
		this.hurtMarked = true;
	}

	public BlockState getBlockState() {
		return Block.stateById(this.entityData.get(BLOCK_STATE_ID));
	}

	public void setBlockState(BlockState blockState) {
		this.entityData.set(BLOCK_STATE_ID, Block.getId(blockState));
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
		this.entityData.define(BLOCK_STATE_ID, Block.getId(Blocks.STONE.defaultBlockState()));
	}

	@Override
	protected void readAdditionalSaveData(CompoundTag tag) {
		setBlockState(Block.stateById(tag.getInt("BlockStateId")));
		if (tag.contains("MotionX")) {
			setDeltaMovement(tag.getDouble("MotionX"), tag.getDouble("MotionY"), tag.getDouble("MotionZ"));
		}
	}

	@Override
	protected void addAdditionalSaveData(CompoundTag tag) {
		tag.putInt("BlockStateId", Block.getId(getBlockState()));
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