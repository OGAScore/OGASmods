package com.OGAS.combatflex;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class SpecialSkillFeature {
	private static final Map<java.util.UUID, Long> PENDING_WAR_CRIES = new HashMap<>();
	private static final long WAR_CRY_CHARGE_TICKS = 20L;
	private static final double WAR_CRY_RANGE = 16.0D;
	private static final double WAR_CRY_STEP = 1.4D;
	private static final double WAR_CRY_RADIUS = 1.35D;
	private static final float WAR_CRY_DAMAGE = 10.0F;
	private static final double WAR_CRY_PUSH = 1.15D;
	private static final double SHOCKWAVE_RADIUS = 8.0D;
	private static final double SHOCKWAVE_PUSH_STRENGTH = 8.0D;
	private static final long DODGE_DURATION_TICKS = 20L * 5L;

	private SpecialSkillFeature() {
	}

	public static boolean isUnlocked(Player player, SpecialSkillType skillType) {
		return player != null && SkillData.hasSpecialSkill(player, skillType);
	}

	public static long getRemainingCooldownTicks(Player player, SpecialSkillType skillType) {
		if (player == null || player.level() == null) {
			return 0L;
		}

		return SkillData.getRemainingSpecialCooldownTicks(player, skillType, player.level().getGameTime());
	}

	public static UseResult use(ServerPlayer player, SpecialSkillType skillType) {
		if (!isUnlocked(player, skillType)) {
			player.displayClientMessage(Component.translatable("message.cbtflex.special_skill_not_learned"), true);
			return UseResult.LOCKED;
		}

		long gameTime = player.level().getGameTime();
		if (!SkillData.consumeSpecialCooldownIfReady(player, skillType, gameTime)) {
			double remainingSeconds = SkillData.getRemainingSpecialCooldownTicks(player, skillType, gameTime) / 20.0D;
			player.displayClientMessage(Component.translatable("message.cbtflex.special_skill_cooldown",
					String.format(Locale.ROOT, "%.1f", remainingSeconds)), true);
			return UseResult.COOLDOWN;
		}

		switch (skillType) {
			case WAR_CRY -> beginWarCryCharge(player, gameTime);
			case SHOCKWAVE -> castShockwave(player);
			case DODGE -> castDodge(player);
		}

		CombatFlexMod.syncPlayerData(player);
		return UseResult.SUCCESS;
	}

	public static void tick(ServerPlayer player) {
		Long releaseTick = PENDING_WAR_CRIES.get(player.getUUID());
		if (releaseTick == null) {
			return;
		}

		if (!player.isAlive() || player.isDeadOrDying()) {
			PENDING_WAR_CRIES.remove(player.getUUID());
			return;
		}

		long gameTime = player.level().getGameTime();
		if (gameTime < releaseTick) {
			return;
		}

		Vec3 eyePosition = player.getEyePosition();
		Vec3 look = player.getLookAngle().normalize();
		PENDING_WAR_CRIES.remove(player.getUUID());
		castWarCry(player, eyePosition, look);
	}

	public static void cancelPending(ServerPlayer player) {
		PENDING_WAR_CRIES.remove(player.getUUID());
	}

	private static void beginWarCryCharge(ServerPlayer player, long gameTime) {
		PENDING_WAR_CRIES.put(player.getUUID(), gameTime + WAR_CRY_CHARGE_TICKS);
		player.displayClientMessage(Component.translatable("message.cbtflex.special_skill_charging",
				Component.translatable(SpecialSkillType.WAR_CRY.nameKey())), true);
		player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDER_DRAGON_AMBIENT,
				SoundSource.PLAYERS, 0.7F, 1.2F);
		player.serverLevel().sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY(1.0D), player.getZ(), 8,
				0.2D, 0.25D, 0.2D, 0.01D);
	}

	private static void castWarCry(ServerPlayer player, Vec3 eyePosition, Vec3 look) {
		ServerLevel level = player.serverLevel();
		Set<Integer> hitEntities = new HashSet<>();

		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDER_DRAGON_GROWL,
				SoundSource.PLAYERS, 1.55F, 1.05F);
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.DRAGON_FIREBALL_EXPLODE,
				SoundSource.PLAYERS, 0.8F, 0.7F);

		int steps = (int) Math.ceil(WAR_CRY_RANGE / WAR_CRY_STEP);
		for (int index = 1; index <= steps; index++) {
			Vec3 point = eyePosition.add(look.scale(index * WAR_CRY_STEP));
			level.sendParticles(ParticleTypes.SONIC_BOOM, point.x, point.y, point.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
			level.sendParticles(ParticleTypes.CLOUD, point.x, point.y, point.z, 2, 0.04D, 0.04D, 0.04D, 0.01D);

			AABB area = new AABB(point, point).inflate(WAR_CRY_RADIUS);
			for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, area,
					entity -> entity.isAlive() && entity != player)) {
				if (!hitEntities.add(living.getId())) {
					continue;
				}

				living.hurt(player.damageSources().sonicBoom(player), WAR_CRY_DAMAGE);
				living.push(look.x * WAR_CRY_PUSH, 0.18D, look.z * WAR_CRY_PUSH);
				living.hurtMarked = true;
			}
		}
	}

	private static void castShockwave(ServerPlayer player) {
		ServerLevel level = player.serverLevel();
		Vec3 origin = player.position();
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.WARDEN_SONIC_BOOM,
				SoundSource.PLAYERS, 1.0F, 0.85F);
		level.sendParticles(ParticleTypes.EXPLOSION, player.getX(), player.getY(0.5D), player.getZ(), 3, 0.15D, 0.1D,
				0.15D, 0.02D);

		AABB area = player.getBoundingBox().inflate(SHOCKWAVE_RADIUS);
		for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, area,
				entity -> entity.isAlive() && entity != player)) {
			Vec3 direction = living.position().subtract(origin);
			if (direction.lengthSqr() < 1.0E-4D) {
				direction = new Vec3(0.0D, 0.0D, 1.0D);
			} else {
				direction = direction.normalize();
			}

			living.knockback(SHOCKWAVE_PUSH_STRENGTH, player.getX() - living.getX(), player.getZ() - living.getZ());
			living.push(direction.x * 1.2D, 0.28D, direction.z * 1.2D);
			living.hurtMarked = true;
		}
	}

	private static void castDodge(ServerPlayer player) {
		SkillData.activateDodge(player, player.level().getGameTime(), DODGE_DURATION_TICKS);
		player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDERMAN_TELEPORT,
				SoundSource.PLAYERS, 0.8F, 1.25F);
		player.serverLevel().sendParticles(ParticleTypes.PORTAL, player.getX(), player.getY(0.8D), player.getZ(), 24,
				0.25D, 0.5D, 0.25D, 0.06D);
	}

	public enum UseResult {
		SUCCESS,
		LOCKED,
		COOLDOWN
	}
}