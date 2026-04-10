package com.OGAS.combatflex.flight;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

final class FlightServerEvents {
    private static final double FLIGHT_PATH_DAMAGE_MIN_SPEED = 0.6;
    private static final double FLIGHT_PATH_DAMAGE_WIDTH = 1.45;
    private static final double FLIGHT_PATH_DAMAGE_FORWARD_SCALE = 1.35;
    private static final float FLIGHT_PATH_DAMAGE_BASE = 2.0f;
    private static final float FLIGHT_PATH_DAMAGE_MAX = 7.5f;
    private static final float FLIGHT_PATH_KNOCKBACK = 0.55f;
    private static final double FLIGHT_PATH_LIFT = 0.08;
    private static final int FLIGHT_PATH_DAMAGE_INTERVAL = 1;
    private static final int FLIGHT_PATH_TARGET_COOLDOWN = 4;
    private static final double FLIGHT_HEAVY_IMPACT_SPEED = 1.9;
    private static final int FLIGHT_LIGHT_IMPACT_CRIT_PARTICLES = 8;
    private static final int FLIGHT_LIGHT_IMPACT_POOF_PARTICLES = 6;
    private static final int FLIGHT_HEAVY_IMPACT_CRIT_PARTICLES = 18;
    private static final int FLIGHT_HEAVY_IMPACT_POOF_PARTICLES = 12;
    private static final int FLIGHT_HEAVY_IMPACT_SWEEP_PARTICLES = 8;
    private static final int FLIGHT_BUFF_DURATION_TICKS = 20 * 60 * 5;
    private static final int FAST_FLIGHT_BUFF_DURATION_TICKS = 20 * 60 * 3;
    private static final int SATURATION_REFRESH_INTERVAL_TICKS = 40;
    private static final int BASE_REGEN_INTERVAL_TICKS = 50;
    private static final int FAST_REGEN_INTERVAL_TICKS = 14;
    private static final float BASE_RESISTANCE_MULTIPLIER = 0.6f;
    private static final float FAST_RESISTANCE_MULTIPLIER = 0.2f;
    private static final double FAST_HEALTH_BOOST_AMOUNT = 20.0;
    private static final UUID FAST_HEALTH_BOOST_MODIFIER_ID = UUID.fromString("7d15f301-82c2-4be6-9e11-5b924b3b5056");
    private static final int FAST_TAKEOFF_TERRAIN_RADIUS = 2;
    private static final int FAST_TAKEOFF_PLANT_HEIGHT = 2;
    private static final int FAST_TAKEOFF_BASE_CLOUD_PARTICLES = 20;
    private static final int FAST_TAKEOFF_BASE_POOF_PARTICLES = 10;
    private static final float PROJECTILE_RETURN_CHANCE = 0.35f;
    private static final double PROJECTILE_DEFLECT_MIN_SPEED = 1.2;
    private static final String PROJECTILE_DEFLECT_COOLDOWN_TAG = "flightProjectileDeflectUntil";

    private static final Map<UUID, Integer> SERVER_FLIGHT_MODES = new HashMap<>();
    private static final Map<UUID, Boolean> SERVER_MENU_OPEN = new HashMap<>();
    private static final Map<UUID, Long> SERVER_FLIGHT_BUFF_UNTIL = new HashMap<>();
    private static final Map<UUID, Long> SERVER_FAST_FLIGHT_BUFF_UNTIL = new HashMap<>();

    private final Map<ServerPlayer, Map<Integer, Integer>> flightPathDamageCooldowns = new WeakHashMap<>();

    static void setServerFlightMode(UUID playerId, int mode) {
        if (mode == 0) {
            SERVER_FLIGHT_MODES.remove(playerId);
            return;
        }
        SERVER_FLIGHT_MODES.put(playerId, mode);
    }

    static int getServerFlightMode(UUID playerId) {
        return SERVER_FLIGHT_MODES.getOrDefault(playerId, 0);
    }

    static void setMenuOpen(UUID playerId, boolean open) {
        if (open) {
            SERVER_MENU_OPEN.put(playerId, true);
            return;
        }
        SERVER_MENU_OPEN.remove(playerId);
    }

    static void applyFastTakeoffTerrainBurst(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        BlockPos center = player.blockPosition().below();
        int changedGroundBlocks = 0;
        int clearedPlants = 0;

        for (int offsetX = -FAST_TAKEOFF_TERRAIN_RADIUS; offsetX <= FAST_TAKEOFF_TERRAIN_RADIUS; offsetX++) {
            for (int offsetZ = -FAST_TAKEOFF_TERRAIN_RADIUS; offsetZ <= FAST_TAKEOFF_TERRAIN_RADIUS; offsetZ++) {
                if ((offsetX * offsetX) + (offsetZ * offsetZ) > FAST_TAKEOFF_TERRAIN_RADIUS * FAST_TAKEOFF_TERRAIN_RADIUS + 1) {
                    continue;
                }

                BlockPos groundPos = center.offset(offsetX, 0, offsetZ);
                BlockState groundState = level.getBlockState(groundPos);
                if (groundState.is(Blocks.GRASS_BLOCK)) {
                    level.setBlock(groundPos, Blocks.DIRT.defaultBlockState(), Block.UPDATE_ALL);
                    changedGroundBlocks++;
                }

                for (int offsetY = 1; offsetY <= FAST_TAKEOFF_PLANT_HEIGHT; offsetY++) {
                    BlockPos plantPos = groundPos.above(offsetY);
                    BlockState plantState = level.getBlockState(plantPos);
                    if (isFastTakeoffClearedPlant(plantState)) {
                        level.destroyBlock(plantPos, false, player);
                        clearedPlants++;
                    }
                }
            }
        }

        if (changedGroundBlocks > 0 || clearedPlants > 0) {
            spawnFastTakeoffTerrainEffects(level, player, changedGroundBlocks, clearedPlants);
        }
    }

    @SubscribeEvent
    public void onLivingAttack(LivingAttackEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }

        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) {
            return;
        }

        if (hasFastFlightBuff(serverPlayer) && event.getSource().is(DamageTypeTags.IS_FIRE)) {
            event.setCanceled(true);
            return;
        }

        if (!event.getSource().is(DamageTypeTags.IS_PROJECTILE)) {
            return;
        }

        if (getServerFlightMode(serverPlayer.getUUID()) == 0) {
            return;
        }

        Entity directEntity = event.getSource().getDirectEntity();
        if (directEntity instanceof Projectile projectile) {
            deflectProjectile(serverPlayer, projectile);
        }

        event.setCanceled(true);
    }

    @SubscribeEvent
    public void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }

        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) {
            return;
        }

        if (event.getSource().is(DamageTypeTags.BYPASSES_RESISTANCE)) {
            return;
        }

        if (hasFastFlightBuff(serverPlayer)) {
            event.setAmount(event.getAmount() * FAST_RESISTANCE_MULTIPLIER);
            return;
        }

        if (hasFlightBuff(serverPlayer)) {
            event.setAmount(event.getAmount() * BASE_RESISTANCE_MULTIPLIER);
        }
    }

    private void deflectProjectile(ServerPlayer serverPlayer, Projectile projectile) {
        long gameTime = serverPlayer.level().getGameTime();
        if (projectile.getPersistentData().getLong(PROJECTILE_DEFLECT_COOLDOWN_TAG) > gameTime) {
            return;
        }
        projectile.getPersistentData().putLong(PROJECTILE_DEFLECT_COOLDOWN_TAG, gameTime + 2L);

        Vec3 velocity = projectile.getDeltaMovement();
        Vec3 reverseDirection = velocity.lengthSqr() > 1.0E-4
                ? velocity.normalize().scale(-1.0)
                : serverPlayer.getLookAngle().normalize();
        boolean returnShot = serverPlayer.getRandom().nextFloat() < PROJECTILE_RETURN_CHANCE;

        Vec3 deflectDirection = reverseDirection;
        if (!returnShot) {
            Vec3 sideDirection = reverseDirection.cross(new Vec3(0.0, 1.0, 0.0));
            if (sideDirection.lengthSqr() < 1.0E-4) {
                sideDirection = new Vec3(1.0, 0.0, 0.0);
            }
            double sideStrength = (serverPlayer.getRandom().nextDouble() - 0.5) * 0.9;
            deflectDirection = reverseDirection.add(sideDirection.normalize().scale(sideStrength)).add(0.0, 0.18, 0.0).normalize();
        }

        double speed = Math.max(PROJECTILE_DEFLECT_MIN_SPEED, velocity.length());
        projectile.setOwner(serverPlayer);
        projectile.setDeltaMovement(deflectDirection.scale(speed * (returnShot ? 1.15 : 0.9)));
        projectile.setPos(
                projectile.getX() + deflectDirection.x * 0.6,
                projectile.getY() + deflectDirection.y * 0.6,
                projectile.getZ() + deflectDirection.z * 0.6
        );
        projectile.hurtMarked = true;
        projectile.hasImpulse = true;

        ServerLevel level = serverPlayer.serverLevel();
        level.sendParticles(
                ParticleTypes.SWEEP_ATTACK,
                projectile.getX(),
                projectile.getY(),
                projectile.getZ(),
                returnShot ? 4 : 2,
                0.08,
                0.08,
                0.08,
                0.0
        );
        level.sendParticles(
                ParticleTypes.CRIT,
                projectile.getX(),
                projectile.getY(),
                projectile.getZ(),
                returnShot ? 8 : 5,
                0.18,
                0.18,
                0.18,
                0.02
        );
        level.playSound(
                null,
                projectile.getX(),
                projectile.getY(),
                projectile.getZ(),
                returnShot ? SoundEvents.PLAYER_ATTACK_SWEEP : SoundEvents.SHIELD_BLOCK,
                SoundSource.PLAYERS,
                returnShot ? 0.7f : 0.55f,
                returnShot ? 1.15f : 1.35f
        );
    }

    @SubscribeEvent
    public void onServerPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide()) {
            return;
        }

        if (!(event.player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        Map<Integer, Integer> cooldowns = flightPathDamageCooldowns.computeIfAbsent(serverPlayer, ignored -> new HashMap<>());
        cooldowns.entrySet().removeIf(entry -> entry.getValue() <= serverPlayer.tickCount);

        long gameTime = serverPlayer.level().getGameTime();
        int serverFlightMode = SERVER_FLIGHT_MODES.getOrDefault(serverPlayer.getUUID(), 0);
        if (serverFlightMode != 0) {
            SERVER_FLIGHT_BUFF_UNTIL.put(serverPlayer.getUUID(), gameTime + FLIGHT_BUFF_DURATION_TICKS);
            if (serverFlightMode == 1) {
                SERVER_FAST_FLIGHT_BUFF_UNTIL.put(serverPlayer.getUUID(), gameTime + FAST_FLIGHT_BUFF_DURATION_TICKS);
            }
        }

        int baseBuffRemainingTicks = getRemainingBuffTicks(SERVER_FLIGHT_BUFF_UNTIL, serverPlayer, gameTime);
        if (baseBuffRemainingTicks > 0) {
            clearLegacyFlightPotionEffects(serverPlayer);
            applyBaseFlightBuffEffects(serverPlayer);
        } else {
            SERVER_FLIGHT_BUFF_UNTIL.remove(serverPlayer.getUUID());
        }

        int fastBuffRemainingTicks = getRemainingBuffTicks(SERVER_FAST_FLIGHT_BUFF_UNTIL, serverPlayer, gameTime);
        if (fastBuffRemainingTicks > 0) {
            applyFastFlightBuffEffects(serverPlayer);
        } else {
            removeFastHealthBoost(serverPlayer);
            SERVER_FAST_FLIGHT_BUFF_UNTIL.remove(serverPlayer.getUUID());
        }

        if (SERVER_MENU_OPEN.getOrDefault(serverPlayer.getUUID(), false)) {
            return;
        }
        if (serverPlayer.tickCount % FLIGHT_PATH_DAMAGE_INTERVAL != 0 || serverFlightMode != 1) {
            return;
        }

        Vec3 velocity = serverPlayer.getDeltaMovement();
        if (velocity.lengthSqr() < FLIGHT_PATH_DAMAGE_MIN_SPEED * FLIGHT_PATH_DAMAGE_MIN_SPEED) {
            return;
        }

        double speed = velocity.length();
        Vec3 direction = velocity.normalize();
        AABB hitArea = serverPlayer.getBoundingBox()
                .inflate(FLIGHT_PATH_DAMAGE_WIDTH)
                .expandTowards(velocity.scale(FLIGHT_PATH_DAMAGE_FORWARD_SCALE));
        float damageAmount = Math.min(FLIGHT_PATH_DAMAGE_MAX,
                FLIGHT_PATH_DAMAGE_BASE + (float) speed * 0.6f);

        for (Entity target : serverPlayer.level().getEntities(serverPlayer, hitArea,
                entity -> entity.isAlive() && entity != serverPlayer && !entity.isPassengerOfSameVehicle(serverPlayer))) {
            if (cooldowns.containsKey(target.getId())) {
                continue;
            }

            boolean damaged = target.hurt(serverPlayer.damageSources().playerAttack(serverPlayer), damageAmount);
            if (target.isPushable()) {
                if (target instanceof LivingEntity livingTarget) {
                    livingTarget.knockback(FLIGHT_PATH_KNOCKBACK, -direction.x, -direction.z);
                }
                target.push(direction.x * FLIGHT_PATH_KNOCKBACK, FLIGHT_PATH_LIFT, direction.z * FLIGHT_PATH_KNOCKBACK);
                target.hurtMarked = true;
            }

            if (damaged || target.isPushable()) {
                spawnFlightImpactEffects(serverPlayer, target, speed);
                cooldowns.put(target.getId(), serverPlayer.tickCount + FLIGHT_PATH_TARGET_COOLDOWN);
            }
        }
    }

    private boolean hasFlightBuff(ServerPlayer serverPlayer) {
        return SERVER_FLIGHT_BUFF_UNTIL.getOrDefault(serverPlayer.getUUID(), 0L) > serverPlayer.level().getGameTime();
    }

    private boolean hasFastFlightBuff(ServerPlayer serverPlayer) {
        return SERVER_FAST_FLIGHT_BUFF_UNTIL.getOrDefault(serverPlayer.getUUID(), 0L) > serverPlayer.level().getGameTime();
    }

    private int getRemainingBuffTicks(Map<UUID, Long> buffMap, ServerPlayer serverPlayer, long gameTime) {
        return (int) Math.max(0L, buffMap.getOrDefault(serverPlayer.getUUID(), 0L) - gameTime);
    }

    private static boolean isFastTakeoffClearedPlant(BlockState state) {
        if (state.isAir()) {
            return false;
        }
        if (state.is(BlockTags.FLOWERS)) {
            return true;
        }

        Block block = state.getBlock();
        return block == Blocks.GRASS
                || block == Blocks.TALL_GRASS
                || block == Blocks.FERN
                || block == Blocks.LARGE_FERN
                || block == Blocks.DEAD_BUSH
                || block instanceof DoublePlantBlock;
    }

    private static void spawnFastTakeoffTerrainEffects(ServerLevel level, ServerPlayer player, int changedGroundBlocks, int clearedPlants) {
        int cloudParticles = FAST_TAKEOFF_BASE_CLOUD_PARTICLES + changedGroundBlocks * 3 + clearedPlants * 2;
        int poofParticles = FAST_TAKEOFF_BASE_POOF_PARTICLES + clearedPlants * 2;
        double effectX = player.getX();
        double effectY = player.getY() + 0.05;
        double effectZ = player.getZ();

        level.sendParticles(
            ParticleTypes.CLOUD,
                effectX,
                effectY,
                effectZ,
            cloudParticles,
                0.9,
                0.15,
                0.9,
            0.035
        );
        level.sendParticles(
                ParticleTypes.POOF,
                effectX,
                effectY + 0.1,
                effectZ,
                poofParticles,
                0.8,
                0.12,
                0.8,
                0.04
        );

        level.playSound(null, effectX, effectY, effectZ, SoundEvents.PLAYER_BIG_FALL, SoundSource.PLAYERS, 0.68f, 0.8f);
        level.playSound(null, effectX, effectY, effectZ, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.12f, 1.65f);
    }

    private void applyBaseFlightBuffEffects(ServerPlayer serverPlayer) {
        if (!hasFastFlightBuff(serverPlayer) && serverPlayer.tickCount % BASE_REGEN_INTERVAL_TICKS == 0 && serverPlayer.getHealth() < serverPlayer.getMaxHealth()) {
            serverPlayer.heal(1.0f);
        }
        if (serverPlayer.tickCount % SATURATION_REFRESH_INTERVAL_TICKS == 0) {
            FoodData foodData = serverPlayer.getFoodData();
            if (foodData.getFoodLevel() < 20 || foodData.getSaturationLevel() < 20.0f) {
                foodData.setFoodLevel(20);
                foodData.setSaturation(20.0f);
            }
        }
    }

    private void applyFastFlightBuffEffects(ServerPlayer serverPlayer) {
        ensureFastHealthBoost(serverPlayer);
        if (serverPlayer.tickCount % FAST_REGEN_INTERVAL_TICKS == 0 && serverPlayer.getHealth() < serverPlayer.getMaxHealth()) {
            serverPlayer.heal(1.0f);
        }
    }

    private void ensureFastHealthBoost(ServerPlayer serverPlayer) {
        AttributeInstance maxHealthAttribute = serverPlayer.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealthAttribute == null) {
            return;
        }

        AttributeModifier currentModifier = maxHealthAttribute.getModifier(FAST_HEALTH_BOOST_MODIFIER_ID);
        if (currentModifier != null && currentModifier.getAmount() == FAST_HEALTH_BOOST_AMOUNT) {
            return;
        }

        double previousMaxHealth = serverPlayer.getMaxHealth();
        if (currentModifier != null) {
            maxHealthAttribute.removeModifier(currentModifier);
        }
        maxHealthAttribute.addTransientModifier(new AttributeModifier(FAST_HEALTH_BOOST_MODIFIER_ID, "flight_fast_health_boost", FAST_HEALTH_BOOST_AMOUNT, AttributeModifier.Operation.ADDITION));
        double currentHealth = serverPlayer.getHealth();
        serverPlayer.setHealth((float) Math.min(serverPlayer.getMaxHealth(), currentHealth + (serverPlayer.getMaxHealth() - previousMaxHealth)));
    }

    private void removeFastHealthBoost(ServerPlayer serverPlayer) {
        AttributeInstance maxHealthAttribute = serverPlayer.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealthAttribute == null) {
            return;
        }

        AttributeModifier currentModifier = maxHealthAttribute.getModifier(FAST_HEALTH_BOOST_MODIFIER_ID);
        if (currentModifier == null) {
            return;
        }

        maxHealthAttribute.removeModifier(currentModifier);
        if (serverPlayer.getHealth() > serverPlayer.getMaxHealth()) {
            serverPlayer.setHealth(serverPlayer.getMaxHealth());
        }
    }

    private void clearLegacyFlightPotionEffects(ServerPlayer serverPlayer) {
        clearLegacyFlightPotionEffect(serverPlayer, MobEffects.DAMAGE_RESISTANCE, 0, FLIGHT_BUFF_DURATION_TICKS);
        clearLegacyFlightPotionEffect(serverPlayer, MobEffects.DAMAGE_RESISTANCE, 1, FLIGHT_BUFF_DURATION_TICKS);
        clearLegacyFlightPotionEffect(serverPlayer, MobEffects.DAMAGE_RESISTANCE, 3, FAST_FLIGHT_BUFF_DURATION_TICKS);
        clearLegacyFlightPotionEffect(serverPlayer, MobEffects.REGENERATION, 0, FLIGHT_BUFF_DURATION_TICKS);
        clearLegacyFlightPotionEffect(serverPlayer, MobEffects.REGENERATION, 2, FAST_FLIGHT_BUFF_DURATION_TICKS);
        clearLegacyFlightPotionEffect(serverPlayer, MobEffects.NIGHT_VISION, 0, FLIGHT_BUFF_DURATION_TICKS);
        clearLegacyFlightPotionEffect(serverPlayer, MobEffects.FIRE_RESISTANCE, 0, FAST_FLIGHT_BUFF_DURATION_TICKS);
        clearLegacyFlightPotionEffect(serverPlayer, MobEffects.HEALTH_BOOST, 4, FAST_FLIGHT_BUFF_DURATION_TICKS);
        clearLegacyFlightPotionEffect(serverPlayer, MobEffects.SATURATION, 0, 40);
    }

    private void clearLegacyFlightPotionEffect(ServerPlayer serverPlayer, net.minecraft.world.effect.MobEffect effect, int amplifier, int expectedDuration) {
        MobEffectInstance currentEffect = serverPlayer.getEffect(effect);
        if (currentEffect == null) {
            return;
        }
        if (currentEffect.getAmplifier() != amplifier) {
            return;
        }
        if (currentEffect.isVisible() || currentEffect.showIcon() || currentEffect.getDuration() <= expectedDuration + 40) {
            serverPlayer.removeEffect(effect);
        }
    }

    private void spawnFlightImpactEffects(ServerPlayer attacker, Entity target, double speed) {
        ServerLevel level = attacker.serverLevel();
        double effectX = target.getX();
        double effectY = target.getY() + (target.getBbHeight() * 0.5);
        double effectZ = target.getZ();
        boolean heavyImpact = speed >= FLIGHT_HEAVY_IMPACT_SPEED;

        level.sendParticles(
                ParticleTypes.CRIT,
                effectX,
                effectY,
                effectZ,
                heavyImpact ? FLIGHT_HEAVY_IMPACT_CRIT_PARTICLES : FLIGHT_LIGHT_IMPACT_CRIT_PARTICLES,
                heavyImpact ? 0.45 : 0.25,
                heavyImpact ? 0.45 : 0.3,
                heavyImpact ? 0.45 : 0.25,
                heavyImpact ? 0.16 : 0.08
        );
        level.sendParticles(
                ParticleTypes.POOF,
                effectX,
                effectY,
                effectZ,
                heavyImpact ? FLIGHT_HEAVY_IMPACT_POOF_PARTICLES : FLIGHT_LIGHT_IMPACT_POOF_PARTICLES,
                heavyImpact ? 0.35 : 0.2,
                heavyImpact ? 0.35 : 0.2,
                heavyImpact ? 0.35 : 0.2,
                heavyImpact ? 0.06 : 0.02
        );
        if (heavyImpact) {
            level.sendParticles(
                    ParticleTypes.SWEEP_ATTACK,
                    effectX,
                    effectY,
                    effectZ,
                    FLIGHT_HEAVY_IMPACT_SWEEP_PARTICLES,
                    0.18,
                    0.12,
                    0.18,
                    0.0
            );
        }

        level.playSound(
                null,
                effectX,
                effectY,
                effectZ,
                heavyImpact ? SoundEvents.PLAYER_ATTACK_STRONG : SoundEvents.PLAYER_ATTACK_CRIT,
                SoundSource.PLAYERS,
                heavyImpact ? 0.8f : 0.45f,
                heavyImpact ? 0.85f : (float) (1.0 + Math.min(speed, 1.5) * 0.08)
        );
        if (heavyImpact) {
            level.playSound(null, effectX, effectY, effectZ, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.55f, 0.9f);
        }
    }
}