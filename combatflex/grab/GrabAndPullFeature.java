package com.OGAS.combatflex.grab;

import com.OGAS.combatflex.CombatFlexMod;
import com.OGAS.combatflex.SkillData;
import com.OGAS.combatflex.SkillType;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class GrabAndPullFeature {
	public static final String MOD_ID = CombatFlexMod.MOD_ID;
	private static final String PROTOCOL_VERSION = "1";
	static final int HEAVY_ATTACK_HOLD_TICKS = 6;
	static final int GRAB_HOLD_TICKS = 4;
	private static final double TARGET_RANGE = 5.0D;
	private static final double HEAVY_ATTACK_RANGE = 6.5D;
	private static final float HEAVY_EXECUTE_HEALTH_THRESHOLD = 0.25F;
	private static final double LIVING_GRAB_ANCHOR_DISTANCE = 2.25D;
	private static final double BLOCK_GRAB_ANCHOR_DISTANCE = 1.7D;
	private static final double BLOCK_GRAB_RIGHT_OFFSET = 1.0D;
	private static final double DEFAULT_GRAB_ANCHOR_DISTANCE = 2.5D;
	private static final double LARGE_GRAB_WIDTH_THRESHOLD = 1.2D;
	private static final double LARGE_GRAB_HEIGHT_THRESHOLD = 2.0D;
	private static final double LARGE_GRAB_WIDTH_DISTANCE_SCALE = 0.8D;
	private static final double LARGE_GRAB_HEIGHT_DISTANCE_SCALE = 0.3D;
	private static final double MAX_LARGE_GRAB_DISTANCE_BONUS = 3.0D;
	private static final double GRAB_SNAP_DISTANCE_SQR = 9.0D;
	private static final double MAX_GRAB_RECOVER_DISTANCE_SQR = 1024.0D;
	private static final double LIVING_GRAB_MAX_VELOCITY_LEAD = 12.0D;
	private static final double LIVING_GRAB_VELOCITY_SCALE = 12.0D;
	private static final double LIVING_GRAB_LOCK_STEP = 1.0D;
	private static final double BLOCK_GRAB_LERP_FACTOR = 0.28D;
	private static final double BLOCK_GRAB_MAX_STEP = 0.42D;
	private static final double BLOCK_GRAB_SNAP_DISTANCE_SQR = 16.0D;
	private static final double THROW_DISTANCE_MULTIPLIER = 32.0D;
	private static final double MAX_THROW_DISTANCE = 8.0D * THROW_DISTANCE_MULTIPLIER;
	private static final double MIN_ENTITY_THROW_DISTANCE = 2.0D * THROW_DISTANCE_MULTIPLIER;
	private static final double ENTITY_HEALTH_THROW_SCALE = 0.3D * THROW_DISTANCE_MULTIPLIER;
	private static final double MIN_BLOCK_THROW_DISTANCE = 2.5D * THROW_DISTANCE_MULTIPLIER;
	private static final double BLOCK_BLAST_THROW_SCALE = 2.25D * THROW_DISTANCE_MULTIPLIER;
	private static final double THROW_FORWARD_OFFSET = 1.2D;
	private static final double BLOCK_THROW_RIGHT_OFFSET = 0.0D;
	private static final double BLOCK_THROW_SPEED = 6.0D;
	private static final double ENTITY_THROW_SPEED = 4.6D;
	private static final float QUICK_HEAVY_ATTACK_DAMAGE = 24.0F;
	private static final float HEAVY_ATTACK_DAMAGE = 50.0F;
	private static final float LIVING_THROW_DAMAGE = 8.0F;
	private static final float NON_LIVING_THROW_DAMAGE = 20.0F;
	private static final float GRABBED_HEAVY_ATTACK_DAMAGE = 100.0F;
	private static final double SONIC_BURST_RADIUS = 2.25D;
	private static final float SONIC_BURST_DAMAGE = 16.0F;
	private static final int THROW_TRAIL_STEPS = 10;
	private static final double HEAVY_ATTACK_BURST_RADIUS = 2.4D;
	private static final float HEAVY_ATTACK_BURST_DAMAGE = 6.0F;
	private static final double HEAVY_ATTACK_BLOCK_BREAK_RADIUS = 2.75D;
	private static final int HEAVY_ATTACK_RING_PARTICLES = 18;
	private static final double BLOCK_EXPLOSION_BLOCK_RADIUS_SCALE = 0.82D;
	private static final float STONE_BLOCK_EXPLOSION_DAMAGE = 20.0F;
	private static final float OBSIDIAN_BLOCK_EXPLOSION_DAMAGE = 100.0F;
	private static final double BLOCK_EXPLOSION_MAX_DAMAGE_RADIUS = 30.0D;
	private static final double BLOCK_EXPLOSION_FULL_DAMAGE_PORTION = 0.2D;
	private static final double BLOCK_EXPLOSION_MIN_DAMAGE_MULTIPLIER = 0.2D;
	private static final double BLOCK_EXPLOSION_FATAL_RADIUS = 2.5D;
	private static final float BLOCK_EXPLOSION_FATAL_DAMAGE = 40.0F;
	private static final double BLOCK_EXPLOSION_MAX_KNOCKBACK = 2.4D;
	private static final double BLOCK_EXPLOSION_MIN_KNOCKBACK = 0.5D;
	private static final double BLOCK_EXPLOSION_IMPACT_KNOCKBACK_MULTIPLIER = 1.4D;
	private static final int BLOCK_EXPLOSION_IMPACT_SLOW_TICKS = 12;
	private static final int BLOCK_EXPLOSION_IMPACT_SLOW_AMPLIFIER = 2;
	private static final float NETHER_STAR_PROJECTILE_BLAST_POWER = OBSIDIAN_BLOCK_EXPLOSION_DAMAGE * 0.36F;
	private static final float NETHER_STAR_PROJECTILE_DAMAGE = OBSIDIAN_BLOCK_EXPLOSION_DAMAGE * 3.0F;
	private static final double NETHER_STAR_EXECUTE_RADIUS = 2.0D;
	private static final double NETHER_STAR_HEAVY_DAMAGE_RADIUS = 75.0D;
	private static final float NETHER_STAR_HEAVY_DAMAGE = 1000.0F;
	private static final float NETHER_STAR_MIN_EDGE_DAMAGE = 10.0F;
	private static final double NETHER_STAR_TERRAIN_HEMISPHERE_RADIUS = 75.0D;
	private static final double NETHER_STAR_TERRAIN_CENTER_HEIGHT_OFFSET = 5.0D;
	private static final double NETHER_STAR_TERRAIN_CYLINDER_LENGTH = 50.0D;
	private static final int NETHER_STAR_MUSHROOM_DURATION_TICKS = 8;
	private static final double NETHER_STAR_MUSHROOM_STEM_HEIGHT_SCALE = 0.42D;
	private static final double NETHER_STAR_MUSHROOM_CAP_RADIUS_SCALE = 0.52D;
	private static final double NETHER_STAR_MUSHROOM_EXPANSION_PER_TICK = 0.48D;
	private static final double NETHER_STAR_UPPER_AIR_RING_HEIGHT = 8.0D;
	private static final double NETHER_STAR_UPPER_AIR_RING_RADIUS = 20.0D;
	private static final int NETHER_STAR_UPPER_AIR_RING_BURSTS = 30;
	private static final float END_CRYSTAL_PROJECTILE_BLAST_POWER = OBSIDIAN_BLOCK_EXPLOSION_DAMAGE * 0.18F;
	private static final float END_CRYSTAL_PROJECTILE_DAMAGE = OBSIDIAN_BLOCK_EXPLOSION_DAMAGE * 1.5F;
	private static final float NETHERITE_BLOCK_PROJECTILE_BLAST_POWER = OBSIDIAN_BLOCK_EXPLOSION_DAMAGE * 0.12F;
	private static final double NETHERITE_BLOCK_DAMAGE_RADIUS = 2.0D;
	private static final float NETHERITE_BLOCK_DAMAGE = 200.0F;
	private static final double IRON_NUGGET_PIERCE_RADIUS = 2.0D;
	private static final double IRON_NUGGET_BLOCK_RADIUS = 1.0D;
	private static final float IRON_NUGGET_PIERCE_DAMAGE = 500.0F;
	private static final double IRON_NUGGET_STEP_DISTANCE = 0.55D;
	private static final double IRON_NUGGET_MAX_TRAVEL_DISTANCE = 300.0D;
	private static final int IRON_NUGGET_MAX_TRAVEL_TICKS = (int) Math.floor(IRON_NUGGET_MAX_TRAVEL_DISTANCE / 10.0D);
	private static final int IRON_NUGGET_MAX_DESTROYED_BLOCKS_PER_TICK = 48;
	private static final double NETHER_STAR_MAX_TERRAIN_RADIUS = 27.0D;
	private static final int NETHER_STAR_MAX_DESTROYED_BLOCKS = 10368;
	private static final double END_CRYSTAL_MAX_TERRAIN_RADIUS = 7.0D;
	private static final int END_CRYSTAL_MAX_DESTROYED_BLOCKS = 224;
	private static final double NETHERITE_BLOCK_MAX_TERRAIN_RADIUS = 16.0D;
	private static final int NETHERITE_BLOCK_MAX_DESTROYED_BLOCKS = 2048;
	private static final int BLOCK_EXPLOSION_DEBRIS_COUNT = 34;
	private static final int BLOCK_EXPLOSION_DUST_COUNT = 20;
	private static final int TERRAIN_DESTRUCTION_TOTAL_TICKS = 3;
	private static final double BLOCK_EXPLOSION_MUSHROOM_STEM_HEIGHT_SCALE = 0.28D;
	private static final double BLOCK_EXPLOSION_MUSHROOM_CAP_RADIUS_SCALE = 0.34D;
	private static final int BLOCK_EXPLOSION_MUSHROOM_RINGS = 3;
	private static final int MUSHROOM_CLOUD_DURATION_TICKS = 3;
	private static final double MUSHROOM_CLOUD_EXPANSION_PER_TICK = 0.22D;
	private static final float ENDER_DRAGON_DAMAGE_MULTIPLIER = 4.0F;
	private static final double CARRY_FIRE_SPEED_SQR = 100.0D;
	private static final float CARRY_FIRE_DAMAGE = 5.0F;
	private static final int THROWN_LINEAR_TICKS = 28;
	private static final int THROWN_BLOCK_MAX_TICKS = 80;

	static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
			.named(ResourceLocation.fromNamespaceAndPath(MOD_ID, "grab_actions"))
			.networkProtocolVersion(() -> PROTOCOL_VERSION)
			.clientAcceptedVersions(PROTOCOL_VERSION::equals)
			.serverAcceptedVersions(PROTOCOL_VERSION::equals)
			.simpleChannel();

	private static final Map<UUID, Integer> ACTIVE_GRABS = new HashMap<>();
	private static final Map<Integer, Float> GRABBED_BLOCK_BLASTS = new HashMap<>();
	private static final Map<Integer, Float> GRABBED_BLOCK_DAMAGES = new HashMap<>();
	private static final Map<Integer, Integer> THROWN_LINEAR_TIMERS = new HashMap<>();
	private static final Map<Integer, Vec3> THROWN_LINEAR_VELOCITIES = new HashMap<>();
	private static final Map<Integer, Float> THROWN_BLOCK_BLASTS = new HashMap<>();
	private static final Map<Integer, Float> THROWN_BLOCK_DAMAGES = new HashMap<>();
	private static final Map<Integer, Vec3> THROWN_BLOCK_LAST_POSITIONS = new HashMap<>();
	private static final Map<Integer, ResourceKey<Level>> THROWN_BLOCK_LEVELS = new HashMap<>();
	private static final Map<Integer, UUID> THROWN_BLOCK_OWNERS = new HashMap<>();
	private static final Map<Integer, SpecialThrownItemKind> THROWN_ITEM_KINDS = new HashMap<>();
	private static final Map<Integer, Vec3> THROWN_ITEM_LAST_POSITIONS = new HashMap<>();
	private static final Map<Integer, ResourceKey<Level>> THROWN_ITEM_LEVELS = new HashMap<>();
	private static final Map<Integer, UUID> THROWN_ITEM_OWNERS = new HashMap<>();
	private static final Map<Integer, Set<Integer>> THROWN_ITEM_HIT_ENTITIES = new HashMap<>();
	private static final List<MushroomCloudEffect> ACTIVE_MUSHROOM_CLOUDS = new ArrayList<>();
	private static final Deque<PendingTerrainDestruction> PENDING_TERRAIN_DESTRUCTIONS = new ArrayDeque<>();

	private record GrabbedBlockData(FallingBlockEntity entity, float blastPower, float explosionDamage) {
	}

	private static final class MushroomCloudEffect {
		private final ResourceKey<Level> levelKey;
		private final Vec3 origin;
		private final float blastPower;
		private final BlockState debrisState;
		private final int durationTicks;
		private final double stemHeightScale;
		private final double capRadiusScale;
		private final double expansionPerTick;
		private final double upperAirRingHeight;
		private final double upperAirRingRadius;
		private final int upperAirRingBursts;
		private int age;

		private MushroomCloudEffect(ResourceKey<Level> levelKey, Vec3 origin, float blastPower, BlockState debrisState, int durationTicks, double stemHeightScale, double capRadiusScale, double expansionPerTick, double upperAirRingHeight, double upperAirRingRadius, int upperAirRingBursts) {
			this.levelKey = levelKey;
			this.origin = origin;
			this.blastPower = blastPower;
			this.debrisState = debrisState;
			this.durationTicks = durationTicks;
			this.stemHeightScale = stemHeightScale;
			this.capRadiusScale = capRadiusScale;
			this.expansionPerTick = expansionPerTick;
			this.upperAirRingHeight = upperAirRingHeight;
			this.upperAirRingRadius = upperAirRingRadius;
			this.upperAirRingBursts = upperAirRingBursts;
		}
	}

	private static final class PendingTerrainDestruction {
		private final ResourceKey<Level> levelKey;
		private final List<BlockPos> targets;
		private final Integer sourceEntityId;
		private final boolean dropResources;
		private int nextIndex;
		private int ticksRemaining;

		private PendingTerrainDestruction(ResourceKey<Level> levelKey, List<BlockPos> targets, Integer sourceEntityId, boolean dropResources) {
			this.levelKey = levelKey;
			this.targets = targets;
			this.sourceEntityId = sourceEntityId;
			this.dropResources = dropResources;
			this.ticksRemaining = TERRAIN_DESTRUCTION_TOTAL_TICKS;
		}
	}

	public static void init(IEventBus modEventBus) {
		ModEntities.ENTITY_TYPES.register(modEventBus);
		modEventBus.addListener(GrabAndPullFeature::commonSetup);
	}

	private static void commonSetup(final FMLCommonSetupEvent event) {
		event.enqueueWork(() -> CHANNEL.messageBuilder(InputActionPacket.class, 0, NetworkDirection.PLAY_TO_SERVER)
				.encoder(InputActionPacket::encode)
				.decoder(InputActionPacket::decode)
				.consumerMainThread(InputActionPacket::handle)
				.add());
	}

	private static void performQuickHeavyAttack(ServerPlayer player) {
		if (!SkillData.hasSkill(player, SkillType.ACTIVE_HEAVY_ATTACK)) {
			return;
		}

		if (performGrabbedHeavyAttack(player)) {
			return;
		}

		if (player.getAttackStrengthScale(0.5F) < 0.9F) {
			return;
		}

		Entity target = findTargetEntity(player);
		if (target == null || !target.isAlive()) {
			destroyTargetBlock(player);
			return;
		}

		if (tryExecuteHeavyAttack(player, target)) {
			Vec3 look = player.getLookAngle().normalize();
			Vec3 impactPosition = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
			playHeavyAttackEffects(player, impactPosition, look);
			applyHeavyAttackBurst(player, target, impactPosition, look);
			player.swing(InteractionHand.MAIN_HAND, true);
			player.resetAttackStrengthTicker();
			return;
		}

		Vec3 look = player.getLookAngle().normalize();
		if (!damageTarget(target, player.damageSources().playerAttack(player), QUICK_HEAVY_ATTACK_DAMAGE)) {
			return;
		}

		target.push(look.x * 1.2D, 0.35D, look.z * 1.2D);
		target.hurtMarked = true;
		Vec3 impactPosition = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
		playHeavyAttackEffects(player, impactPosition, look);
		applyHeavyAttackBurst(player, target, impactPosition, look);
		player.swing(InteractionHand.MAIN_HAND, true);
		player.resetAttackStrengthTicker();

		Entity durabilityTarget = resolveMultipartParent(target);
		if (durabilityTarget instanceof LivingEntity livingTarget) {
			player.getMainHandItem().hurtEnemy(livingTarget, player);
		}
	}

	private static void performHeavyAttack(ServerPlayer player) {
		if (!SkillData.hasSkill(player, SkillType.ACTIVE_CHARGED_HEAVY)) {
			return;
		}

		if (performGrabbedHeavyAttack(player)) {
			return;
		}

		if (player.getAttackStrengthScale(0.5F) < 0.9F) {
			return;
		}

		Entity target = findTargetEntity(player);
		BlockHitResult blockHit = findTargetBlock(player, HEAVY_ATTACK_RANGE);
		Vec3 look = player.getLookAngle().normalize();
		Vec3 impactPosition = getHeavyAttackImpactPosition(player, target, blockHit, look);

		if (target != null && target.isAlive()) {
			if (tryExecuteHeavyAttack(player, target)) {
				target.push(look.x * 1.8D, 0.45D, look.z * 1.8D);
				target.hurtMarked = true;
			} else {
			damageTarget(target, player.damageSources().sonicBoom(player), HEAVY_ATTACK_DAMAGE);
			target.push(look.x * 1.5D, 0.45D, look.z * 1.5D);
			target.hurtMarked = true;
			}

			Entity durabilityTarget = resolveMultipartParent(target);
			if (durabilityTarget instanceof LivingEntity livingTarget) {
				player.getMainHandItem().hurtEnemy(livingTarget, player);
			}
		}

		destroyHeavyAttackBlocks(player, impactPosition);
		playHeavyAttackEffects(player, impactPosition, look);
		applyHeavyAttackBurst(player, target, impactPosition, look);
		player.swing(InteractionHand.MAIN_HAND, true);
		player.resetAttackStrengthTicker();
	}

	private static void destroyTargetBlock(ServerPlayer player) {
		BlockHitResult blockHit = findTargetBlock(player);
		if (blockHit == null) {
			return;
		}

		BlockPos blockPos = blockHit.getBlockPos();
		BlockState blockState = player.serverLevel().getBlockState(blockPos);
		if (!canAttackDestroyBlock(player.serverLevel(), blockPos, blockState)) {
			return;
		}

		if (player.serverLevel().destroyBlock(blockPos, true, player)) {
			Vec3 effectPos = Vec3.atCenterOf(blockPos);
			Vec3 look = player.getLookAngle().normalize();
			playHeavyAttackEffects(player, effectPos, look);
			applyHeavyAttackBurst(player, null, effectPos, look);
			player.swing(InteractionHand.MAIN_HAND, true);
			player.resetAttackStrengthTicker();
		}
	}

	private static boolean performGrabbedHeavyAttack(ServerPlayer player) {
		Integer targetId = ACTIVE_GRABS.get(player.getUUID());
		if (targetId == null) {
			return false;
		}

		Entity target = player.serverLevel().getEntity(targetId);
		if (target == null || !target.isAlive()) {
			ACTIVE_GRABS.remove(player.getUUID());
			return false;
		}

		Vec3 attackDirection = player.getLookAngle().normalize();
		Vec3 impactPosition = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
		if (!tryExecuteHeavyAttack(player, target)) {
			damageTarget(target, player.damageSources().sonicBoom(player), GRABBED_HEAVY_ATTACK_DAMAGE);
		}
		target.push(attackDirection.x * 1.6D, 0.2D, attackDirection.z * 1.6D);
		target.hurtMarked = true;
		playGrabbedHeavyAttackEffects(player, impactPosition, attackDirection);
		player.swing(InteractionHand.MAIN_HAND, true);
		player.resetAttackStrengthTicker();
		return true;
	}

	private static Vec3 getHeavyAttackImpactPosition(ServerPlayer player, Entity target, BlockHitResult blockHit, Vec3 direction) {
		if (target != null && target.isAlive()) {
			return target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
		}

		if (blockHit != null) {
			return blockHit.getLocation();
		}

		return player.getEyePosition().add(direction.scale(HEAVY_ATTACK_RANGE));
	}

	private static void destroyHeavyAttackBlocks(ServerPlayer player, Vec3 origin) {
		ServerLevel level = player.serverLevel();
		int blockRadius = (int) Math.ceil(HEAVY_ATTACK_BLOCK_BREAK_RADIUS);
		BlockPos center = BlockPos.containing(origin);
		for (int x = -blockRadius; x <= blockRadius; x++) {
			for (int y = -blockRadius; y <= blockRadius; y++) {
				for (int z = -blockRadius; z <= blockRadius; z++) {
					BlockPos currentPos = center.offset(x, y, z);
					BlockState state = level.getBlockState(currentPos);
					if (!canAttackDestroyBlock(level, currentPos, state)) {
						continue;
					}

					double distance = Vec3.atCenterOf(currentPos).distanceTo(origin);
					if (distance > HEAVY_ATTACK_BLOCK_BREAK_RADIUS) {
						continue;
					}

					level.destroyBlock(currentPos, true, player);
				}
			}
		}
	}

	private static void applyHeavyAttackBurst(ServerPlayer player, Entity primaryTarget, Vec3 origin, Vec3 direction) {
		ServerLevel level = player.serverLevel();
		AABB burstArea = new AABB(origin, origin).inflate(HEAVY_ATTACK_BURST_RADIUS);
		for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, burstArea, entity -> entity.isAlive() && entity != player && entity != primaryTarget)) {
			damageTarget(living, player.damageSources().sonicBoom(player), HEAVY_ATTACK_BURST_DAMAGE);
			living.push(direction.x * 0.6D, 0.18D, direction.z * 0.6D);
			living.hurtMarked = true;
		}
	}

	private static void startGrab(ServerPlayer player) {
		Entity target = findTargetEntity(player);
		if (target != null && target.isAlive() && target != player && canGrabEntity(target)
				&& SkillData.hasSkill(player, SkillType.ACTIVE_ENTITY_GRAB)) {
			applyGrabState(target);
			target.setDeltaMovement(Vec3.ZERO);
			forceTargetInFront(player, target);
			ACTIVE_GRABS.put(player.getUUID(), target.getId());
			playGrabEffects(player, target.position());
			return;
		}

		GrabbedBlockData grabbedBlock = grabTargetBlock(player);
		if (!SkillData.hasSkill(player, SkillType.ACTIVE_BLOCK_CONTROL)) {
			return;
		}
		if (grabbedBlock == null) {
			return;
		}

		ACTIVE_GRABS.put(player.getUUID(), grabbedBlock.entity().getId());
		GRABBED_BLOCK_BLASTS.put(grabbedBlock.entity().getId(), grabbedBlock.blastPower());
		GRABBED_BLOCK_DAMAGES.put(grabbedBlock.entity().getId(), grabbedBlock.explosionDamage());
		playGrabEffects(player, grabbedBlock.entity().position());
	}

	private static boolean throwHeldBlock(ServerPlayer player) {
		InteractionHand hand = findThrowableHand(player);
		if (hand == null) {
			return false;
		}

		ItemStack stack = player.getItemInHand(hand);
		SpecialThrownItemKind specialThrownItemKind = SpecialThrownItemKind.fromStack(stack);
		if (specialThrownItemKind != null) {
			if (!SkillData.hasSkill(player, SkillType.ACTIVE_SPECIAL_THROW)) {
				return false;
			}

			Vec3 throwOrigin = getThrowOrigin(player, false);
			Vec3 throwVelocity = getSpecialThrowVelocity(player, specialThrownItemKind);
			ThrownItemEntity thrownItem = spawnThrownItem(player.serverLevel(), stack.copyWithCount(1), specialThrownItemKind, throwOrigin, throwVelocity, player);
			playThrowEffects(player, throwOrigin, throwVelocity);
			player.swing(hand, true);

			if (!player.getAbilities().instabuild) {
				stack.shrink(1);
			}

			if (specialThrownItemKind != SpecialThrownItemKind.IRON_NUGGET) {
				applyThrowShockwave(player, thrownItem, throwOrigin, throwVelocity);
			}
			return true;
		}

		if (!(stack.getItem() instanceof BlockItem blockItem)) {
			return false;
		}

		if (!SkillData.hasSkill(player, SkillType.ACTIVE_BLOCK_CONTROL)) {
			return false;
		}

		BlockState blockState = blockItem.getBlock().defaultBlockState();
		if (blockState.isAir() || blockState.hasBlockEntity()) {
			return false;
		}

		Vec3 throwOrigin = getThrowOrigin(player, true);
		ServerLevel level = player.serverLevel();
		float blastPower = getBlockBlastPower(level, BlockPos.containing(player.position()), blockState);
		float explosionDamage = getBlockExplosionDamage(level, BlockPos.containing(player.position()), blockState);
		double throwDistance = getBlockThrowDistance(blastPower);
		Vec3 throwVelocity = getThrowVelocity(player, throwDistance, true);
		ThrownBlockEntity thrownBlock = spawnThrownBlock(level, blockState, throwOrigin, throwVelocity, blastPower, explosionDamage, player);
		applyThrowShockwave(player, thrownBlock, throwOrigin, throwVelocity);
		playThrowEffects(player, throwOrigin, throwVelocity);
		player.swing(hand, true);

		if (!player.getAbilities().instabuild) {
			stack.shrink(1);
		}

		return true;
	}

	private static ThrownItemEntity spawnThrownItem(ServerLevel level, ItemStack stack, SpecialThrownItemKind kind, Vec3 throwOrigin, Vec3 throwVelocity, ServerPlayer player) {
		ThrownItemEntity thrownItem = new ThrownItemEntity(ModEntities.THROWN_ITEM.get(), level);
		thrownItem.initialize(stack, kind, throwOrigin, throwVelocity, player.getYRot(), player.getXRot());
		level.addFreshEntity(thrownItem);

		int entityId = thrownItem.getId();
		THROWN_ITEM_KINDS.put(entityId, kind);
		THROWN_ITEM_LAST_POSITIONS.put(entityId, thrownItem.position());
		THROWN_ITEM_LEVELS.put(entityId, level.dimension());
		THROWN_ITEM_OWNERS.put(entityId, player.getUUID());
		if (kind == SpecialThrownItemKind.IRON_NUGGET) {
			THROWN_ITEM_HIT_ENTITIES.put(entityId, new HashSet<>());
		}
		return thrownItem;
	}

	private static ThrownBlockEntity spawnThrownBlock(ServerLevel level, BlockState blockState, Vec3 throwOrigin, Vec3 throwVelocity, float blastPower, float explosionDamage, ServerPlayer player) {
		ThrownBlockEntity thrownBlock = new ThrownBlockEntity(ModEntities.THROWN_BLOCK.get(), level);
		thrownBlock.initialize(blockState, throwOrigin, throwVelocity, player.getYRot(), player.getXRot());
		level.addFreshEntity(thrownBlock);

		int entityId = thrownBlock.getId();
		THROWN_BLOCK_BLASTS.put(entityId, blastPower);
		THROWN_BLOCK_DAMAGES.put(entityId, explosionDamage);
		THROWN_BLOCK_LEVELS.put(entityId, level.dimension());
		THROWN_BLOCK_LAST_POSITIONS.put(entityId, thrownBlock.position());
		THROWN_BLOCK_OWNERS.put(entityId, player.getUUID());
		return thrownBlock;
	}

	private static BlockPos findHeldBlockSpawnPos(ServerPlayer player, Vec3 throwOrigin) {
		ServerLevel level = player.serverLevel();
		BlockPos primaryPos = BlockPos.containing(throwOrigin);
		if (level.getBlockState(primaryPos).isAir()) {
			return primaryPos;
		}

		BlockPos elevatedPos = BlockPos.containing(throwOrigin.add(0.0D, 1.0D, 0.0D));
		if (level.getBlockState(elevatedPos).isAir()) {
			return elevatedPos;
		}

		BlockPos fallbackPos = player.blockPosition().above(2);
		return level.getBlockState(fallbackPos).isAir() ? fallbackPos : null;
	}

	private static InteractionHand findThrowableHand(ServerPlayer player) {
		for (InteractionHand hand : InteractionHand.values()) {
			ItemStack stack = player.getItemInHand(hand);
			if (stack.isEmpty()) {
				continue;
			}

			if (SpecialThrownItemKind.fromStack(stack) != null || stack.getItem() instanceof BlockItem) {
				return hand;
			}
		}

		return null;
	}

	private static void stopGrab(ServerPlayer player) {
		Integer targetId = ACTIVE_GRABS.remove(player.getUUID());
		if (targetId == null) {
			return;
		}

		Entity target = player.serverLevel().getEntity(targetId);
		if (target == null || !target.isAlive()) {
			GRABBED_BLOCK_BLASTS.remove(targetId);
			GRABBED_BLOCK_DAMAGES.remove(targetId);
			return;
		}

		if (target instanceof FallingBlockEntity fallingBlock) {
			Float storedBlastPower = GRABBED_BLOCK_BLASTS.remove(targetId);
			Float storedExplosionDamage = GRABBED_BLOCK_DAMAGES.remove(targetId);
			float blastPower = storedBlastPower == null ? 0.0F : storedBlastPower;
			float explosionDamage = storedExplosionDamage == null ? 0.0F : storedExplosionDamage;
			BlockState blockState = fallingBlock.getBlockState();
			double throwDistance = getBlockThrowDistance(blastPower);
			Vec3 throwVelocity = getThrowVelocity(player, throwDistance, true);
			Vec3 throwOrigin = getThrowOrigin(player, true);
			fallingBlock.discard();
			ThrownBlockEntity thrownBlock = spawnThrownBlock(player.serverLevel(), blockState, throwOrigin, throwVelocity, blastPower, explosionDamage, player);
			applyThrowShockwave(player, thrownBlock, throwOrigin, throwVelocity);
			playThrowEffects(player, throwOrigin, throwVelocity);
			return;
		}

		clearGrabState(target);

		double throwDistance = getThrowDistance(target, targetId);
		boolean isBlockThrow = target instanceof FallingBlockEntity;
		Vec3 throwVelocity = getThrowVelocity(player, throwDistance, isBlockThrow);
		int throwTicks = getThrowTicks(throwDistance, isBlockThrow);
		Vec3 throwOrigin = getThrowOrigin(player, isBlockThrow);
		moveTargetToThrowOrigin(target, throwOrigin, player);
		if (isBlockThrow) {
			THROWN_BLOCK_LAST_POSITIONS.put(targetId, throwOrigin);
		}
		float throwDamage = target instanceof LivingEntity ? LIVING_THROW_DAMAGE : NON_LIVING_THROW_DAMAGE;
		if (throwDamage > 0.0F) {
			damageTarget(target, player.damageSources().sonicBoom(player), throwDamage);
		}
		applyThrowShockwave(player, target, throwOrigin, throwVelocity);
		target.setNoGravity(true);
		THROWN_LINEAR_TIMERS.put(targetId, throwTicks);
		THROWN_LINEAR_VELOCITIES.put(targetId, throwVelocity);
		target.noPhysics = false;
		target.setDeltaMovement(throwVelocity);
		target.hurtMarked = true;
		playThrowEffects(player, throwOrigin, throwVelocity);
	}

	private static Vec3 getThrowVelocity(ServerPlayer player, double throwDistance, boolean isBlockThrow) {
		double speed = isBlockThrow ? BLOCK_THROW_SPEED : ENTITY_THROW_SPEED;
		return player.getLookAngle().normalize().scale(speed);
	}

	private static Vec3 getSpecialThrowVelocity(ServerPlayer player, SpecialThrownItemKind kind) {
		return player.getLookAngle().normalize().scale(kind.getThrowSpeed());
	}

	private static int getThrowTicks(double throwDistance, boolean isBlockThrow) {
		double speed = isBlockThrow ? BLOCK_THROW_SPEED : ENTITY_THROW_SPEED;
		return Math.max(1, (int) Math.ceil(throwDistance / speed));
	}

	private static Vec3 getThrowOrigin(ServerPlayer player, boolean isBlockThrow) {
		Vec3 look = player.getLookAngle().normalize();
		Vec3 origin = player.getEyePosition().add(look.scale(THROW_FORWARD_OFFSET));
		if (!isBlockThrow) {
			return origin;
		}

		return origin.add(getRightVector(player).scale(BLOCK_THROW_RIGHT_OFFSET));
	}

	private static Vec3 getRightVector(ServerPlayer player) {
		Vec3 right = new Vec3(0.0D, 1.0D, 0.0D).cross(player.getLookAngle().normalize());
		if (right.lengthSqr() < 1.0E-4D) {
			return new Vec3(1.0D, 0.0D, 0.0D);
		}

		return right.normalize();
	}

	private static void moveTargetToThrowOrigin(Entity target, Vec3 throwOrigin, ServerPlayer player) {
		double targetY = throwOrigin.y - (target.getBbHeight() * 0.5D);
		target.moveTo(throwOrigin.x, targetY, throwOrigin.z, player.getYRot(), player.getXRot());
		target.setYRot(player.getYRot());
		target.setXRot(player.getXRot());
		if (target instanceof FallingBlockEntity fallingBlock) {
			fallingBlock.setStartPos(BlockPos.containing(target.position()));
		}
	}

	private static double getThrowDistance(Entity target, int targetId) {
		if (target instanceof LivingEntity livingTarget) {
			double healthBasedDistance = MIN_ENTITY_THROW_DISTANCE + (livingTarget.getHealth() * ENTITY_HEALTH_THROW_SCALE);
			return Math.min(MAX_THROW_DISTANCE, healthBasedDistance);
		}

		if (target instanceof FallingBlockEntity) {
			float blastPower = GRABBED_BLOCK_BLASTS.getOrDefault(targetId, THROWN_BLOCK_BLASTS.getOrDefault(targetId, 0.0F));
			double blockDistance = MIN_BLOCK_THROW_DISTANCE + (blastPower * BLOCK_BLAST_THROW_SCALE);
			return Math.min(MAX_THROW_DISTANCE, blockDistance);
		}

		return Math.min(MAX_THROW_DISTANCE, 4.0D * THROW_DISTANCE_MULTIPLIER);
	}

	private static double getBlockThrowDistance(float blastPower) {
		double blockDistance = MIN_BLOCK_THROW_DISTANCE + (blastPower * BLOCK_BLAST_THROW_SCALE);
		return Math.min(MAX_THROW_DISTANCE, blockDistance);
	}

	private static void applyThrowShockwave(ServerPlayer player, Entity primaryTarget, Vec3 origin, Vec3 throwVelocity) {
		ServerLevel level = player.serverLevel();
		Vec3 direction = throwVelocity.normalize();
		Set<Integer> hitEntities = new HashSet<>();

		for (int index = 0; index < 6; index++) {
			Vec3 step = origin.add(direction.scale(index * 0.7D));
			AABB burstArea = new AABB(step, step).inflate(SONIC_BURST_RADIUS);
			for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, burstArea, entity -> entity.isAlive() && entity != player && entity != primaryTarget)) {
				if (!hitEntities.add(living.getId())) {
					continue;
				}

				damageTarget(living, player.damageSources().sonicBoom(player), SONIC_BURST_DAMAGE);
				living.push(direction.x * 1.1D, 0.3D, direction.z * 1.1D);
				living.hurtMarked = true;
			}
		}
	}

	private static Entity resolveMultipartParent(Entity target) {
		if (target instanceof EnderDragonPart dragonPart && dragonPart.parentMob != null) {
			return dragonPart.parentMob;
		}

		return target;
	}

	private static boolean canGrabEntity(Entity target) {
		return !(resolveMultipartParent(target) instanceof EnderDragon);
	}

	private static boolean tryExecuteHeavyAttack(ServerPlayer player, Entity target) {
		if (!SkillData.hasSkill(player, SkillType.ACTIVE_EXECUTION)) {
			return false;
		}

		Entity resolvedTarget = resolveMultipartParent(target);
		if (!(resolvedTarget instanceof LivingEntity livingTarget) || livingTarget.getMaxHealth() <= 0.0F) {
			return false;
		}

		if (livingTarget.getHealth() / livingTarget.getMaxHealth() > HEAVY_EXECUTE_HEALTH_THRESHOLD) {
			return false;
		}

		return damageTarget(resolvedTarget, player.damageSources().sonicBoom(player), Float.MAX_VALUE);
	}

	private static boolean damageTarget(Entity target, DamageSource damageSource, float damageAmount) {
		if (target == null || !target.isAlive()) {
			return false;
		}

		if (target instanceof EnderDragonPart dragonPart) {
			return dragonPart.parentMob != null && dragonPart.parentMob.hurt(dragonPart, damageSource, damageAmount * ENDER_DRAGON_DAMAGE_MULTIPLIER);
		}

		Entity resolvedTarget = resolveMultipartParent(target);
		float adjustedDamage = resolvedTarget instanceof EnderDragon ? damageAmount * ENDER_DRAGON_DAMAGE_MULTIPLIER : damageAmount;
		return resolvedTarget.hurt(damageSource, adjustedDamage);
	}

	private static void playGrabEffects(ServerPlayer player, Vec3 position) {
		ServerLevel level = player.serverLevel();
		level.playSound(null, position.x, position.y, position.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.75F, 1.35F);
		level.sendParticles(ParticleTypes.SMOKE, position.x, position.y + 0.4D, position.z, 12, 0.25D, 0.3D, 0.25D, 0.02D);
	}

	private static void playThrowEffects(ServerPlayer player, Vec3 position, Vec3 throwVelocity) {
		ServerLevel level = player.serverLevel();
		Vec3 normalizedVelocity = throwVelocity.normalize();
		level.playSound(null, position.x, position.y, position.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.2F, 0.8F);

		for (int index = 0; index < THROW_TRAIL_STEPS; index++) {
			if ((index & 1) != 0) {
				continue;
			}

			Vec3 step = position.add(normalizedVelocity.scale(index * 0.9D));
			level.sendParticles(ParticleTypes.LARGE_SMOKE, step.x, step.y + 0.2D, step.z, 1, 0.04D, 0.04D, 0.04D, 0.008D);
		}

		level.sendParticles(ParticleTypes.CLOUD, position.x, position.y + 0.2D, position.z, 10, 0.16D, 0.16D, 0.16D, 0.03D);
	}

	private static void playGrabbedHeavyAttackEffects(ServerPlayer player, Vec3 position, Vec3 direction) {
		ServerLevel level = player.serverLevel();
		level.playSound(null, position.x, position.y, position.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.35F, 0.7F);

		for (int index = 0; index < THROW_TRAIL_STEPS; index++) {
			Vec3 step = position.add(direction.scale(index * 0.55D));
			level.sendParticles(ParticleTypes.LARGE_SMOKE, step.x, step.y + 0.15D, step.z, 2, 0.06D, 0.06D, 0.06D, 0.005D);
		}

		for (int index = 0; index < 12; index++) {
			double angle = (Math.PI * 2.0D * index) / 12.0D;
			double ringX = Math.cos(angle) * 1.2D;
			double ringZ = Math.sin(angle) * 1.2D;
			level.sendParticles(ParticleTypes.CLOUD, position.x + ringX, position.y + 0.1D, position.z + ringZ, 1, 0.0D, 0.0D, 0.0D, 0.0D);
		}

		level.sendParticles(ParticleTypes.EXPLOSION, position.x, position.y, position.z, 2, 0.12D, 0.12D, 0.12D, 0.0D);
	}

	private static void playHeavyAttackEffects(ServerPlayer player, Vec3 position, Vec3 direction) {
		ServerLevel level = player.serverLevel();
		Vec3 normalizedDirection = direction.normalize();
		Vec3 right = getPerpendicularVector(normalizedDirection);
		Vec3 up = normalizedDirection.cross(right).normalize();
		level.playSound(null, position.x, position.y, position.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.3F, 0.85F);
		level.playSound(null, position.x, position.y, position.z, SoundEvents.DRAGON_FIREBALL_EXPLODE, SoundSource.PLAYERS, 1.2F, 0.7F);
		level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, position.x, position.y, position.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);

		for (int index = 0; index < 4; index++) {
			Vec3 step = position.add(normalizedDirection.scale(index * 0.55D));
			level.sendParticles(ParticleTypes.SONIC_BOOM, step.x, step.y + 0.15D, step.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
		}

		for (int index = 0; index < HEAVY_ATTACK_RING_PARTICLES; index++) {
			double angle = (Math.PI * 2.0D * index) / HEAVY_ATTACK_RING_PARTICLES;
			Vec3 ringOffset = right.scale(Math.cos(angle) * 1.9D).add(up.scale(Math.sin(angle) * 1.9D));
			Vec3 ringPos = position.add(ringOffset);
			level.sendParticles(ParticleTypes.CLOUD, ringPos.x, ringPos.y, ringPos.z, 1, 0.02D, 0.02D, 0.02D, 0.0D);
			level.sendParticles(ParticleTypes.LARGE_SMOKE, ringPos.x, ringPos.y, ringPos.z, 1, 0.01D, 0.01D, 0.01D, 0.0D);
		}

		level.sendParticles(ParticleTypes.CLOUD, position.x, position.y + 0.1D, position.z, 16, 0.35D, 0.2D, 0.35D, 0.04D);
		level.sendParticles(ParticleTypes.LARGE_SMOKE, position.x, position.y + 0.1D, position.z, 12, 0.22D, 0.16D, 0.22D, 0.02D);
	}

	private static Vec3 getPerpendicularVector(Vec3 direction) {
		Vec3 right = new Vec3(0.0D, 1.0D, 0.0D).cross(direction);
		if (right.lengthSqr() < 1.0E-4D) {
			return new Vec3(1.0D, 0.0D, 0.0D);
		}

		return right.normalize();
	}

	private static Entity findTargetEntity(ServerPlayer player) {
		Vec3 start = player.getEyePosition();
		Vec3 look = player.getLookAngle();
		Vec3 end = start.add(look.scale(TARGET_RANGE));

		HitResult blockHit = player.level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
		if (blockHit.getType() != HitResult.Type.MISS) {
			end = blockHit.getLocation();
		}

		double maxDistanceSqr = start.distanceToSqr(end);
		AABB searchBox = player.getBoundingBox().expandTowards(look.scale(TARGET_RANGE)).inflate(1.0D);
		EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(player, start, end, searchBox, entity -> entity.isPickable() && !entity.isSpectator() && entity != player, maxDistanceSqr);
		return entityHit == null ? null : entityHit.getEntity();
	}

	private static BlockHitResult findTargetBlock(ServerPlayer player) {
		return findTargetBlock(player, TARGET_RANGE);
	}

	private static BlockHitResult findTargetBlock(ServerPlayer player, double range) {
		HitResult hitResult = player.level().clip(new ClipContext(player.getEyePosition(), player.getEyePosition().add(player.getLookAngle().scale(range)), ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
		if (hitResult instanceof BlockHitResult blockHit && blockHit.getType() == HitResult.Type.BLOCK) {
			return blockHit;
		}

		return null;
	}

	private static GrabbedBlockData grabTargetBlock(ServerPlayer player) {
		BlockHitResult blockHit = findTargetBlock(player);
		if (blockHit == null) {
			return null;
		}

		BlockPos blockPos = blockHit.getBlockPos();
		BlockState blockState = player.serverLevel().getBlockState(blockPos);
		if (!canGrabBlock(player, blockPos, blockState)) {
			return null;
		}

		float blastPower = getBlockBlastPower(player.serverLevel(), blockPos, blockState);
		float explosionDamage = getBlockExplosionDamage(player.serverLevel(), blockPos, blockState);

		FallingBlockEntity fallingBlock = FallingBlockEntity.fall(player.serverLevel(), blockPos, blockState);
		fallingBlock.setNoGravity(true);
		fallingBlock.noPhysics = true;
		fallingBlock.setDeltaMovement(Vec3.ZERO);
		fallingBlock.hurtMarked = true;
		return new GrabbedBlockData(fallingBlock, blastPower, explosionDamage);
	}

	private static float getBlockBlastPower(ServerLevel level, BlockPos blockPos, BlockState blockState) {
		float hardness = Math.max(0.0F, blockState.getDestroySpeed(level, blockPos));
		if (hardness <= 0.6F) {
			return 0.0F;
		}
		if (hardness <= 1.5F) {
			return 0.3F + (((hardness - 0.6F) / 0.9F) * 2.0F);
		}
		if (hardness <= 5.0F) {
			double normalizedHardness = (hardness - 1.5F) / 3.5F;
			return (float) (2.3D + (Math.pow(normalizedHardness, 1.2D) * 3.7D));
		}

		double curvedPower = 6.0D + (Math.pow(Math.min(50.0D, hardness) - 5.0D, 1.08D) * 0.19D);
		return (float) Math.min(12.0D, curvedPower);
	}

	private static float getBlockExplosionDamage(ServerLevel level, BlockPos blockPos, BlockState blockState) {
		float hardness = Math.max(0.0F, blockState.getDestroySpeed(level, blockPos));
		if (hardness <= 0.6F) {
			return 0.0F;
		}
		if (hardness <= 1.5F) {
			double normalizedHardness = (hardness - 0.6D) / 0.9D;
			return (float) (4.0D + (normalizedHardness * (STONE_BLOCK_EXPLOSION_DAMAGE - 4.0D)));
		}

		double normalizedHardness = Math.min(1.0D, (hardness - 1.5D) / 48.5D);
		double curvedDamage = STONE_BLOCK_EXPLOSION_DAMAGE + (Math.pow(normalizedHardness, 1.18D) * (OBSIDIAN_BLOCK_EXPLOSION_DAMAGE - STONE_BLOCK_EXPLOSION_DAMAGE));
		return (float) curvedDamage;
	}

	private static void applyThrownBlockExplosionDamage(ServerLevel level, Vec3 pos, Entity source, float blastPower, float explosionDamage) {
		if (explosionDamage <= 0.0F) {
			return;
		}

		double radius = Math.max(2.0D, BLOCK_EXPLOSION_MAX_DAMAGE_RADIUS * (explosionDamage / OBSIDIAN_BLOCK_EXPLOSION_DAMAGE));
		AABB damageArea = new AABB(pos, pos).inflate(radius);
		for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, damageArea, entity -> entity.isAlive() && !(entity instanceof Player))) {
			double distance = living.position().distanceTo(pos);
			double distanceRatio = Math.min(1.0D, distance / radius);
			double damageMultiplier;
			if (distanceRatio <= BLOCK_EXPLOSION_FULL_DAMAGE_PORTION) {
				damageMultiplier = 1.0D;
			} else {
				double outerProgress = (distanceRatio - BLOCK_EXPLOSION_FULL_DAMAGE_PORTION) / (1.0D - BLOCK_EXPLOSION_FULL_DAMAGE_PORTION);
				damageMultiplier = 1.0D - ((1.0D - BLOCK_EXPLOSION_MIN_DAMAGE_MULTIPLIER) * outerProgress);
			}

			float dealtDamage = distance <= BLOCK_EXPLOSION_FATAL_RADIUS
					? BLOCK_EXPLOSION_FATAL_DAMAGE
					: (float) (explosionDamage * damageMultiplier);
			if (dealtDamage <= 0.0F) {
				continue;
			}

			damageTarget(living, level.damageSources().explosion(source, null), dealtDamage);

			Vec3 pushDirection = living.position().subtract(pos);
			if (pushDirection.lengthSqr() < 1.0E-4D) {
				pushDirection = new Vec3(0.0D, 1.0D, 0.0D);
			} else {
				pushDirection = pushDirection.normalize();
			}

			double knockback = (BLOCK_EXPLOSION_MIN_KNOCKBACK + ((damageMultiplier - BLOCK_EXPLOSION_MIN_DAMAGE_MULTIPLIER) / (1.0D - BLOCK_EXPLOSION_MIN_DAMAGE_MULTIPLIER) * (BLOCK_EXPLOSION_MAX_KNOCKBACK - BLOCK_EXPLOSION_MIN_KNOCKBACK)))
					* BLOCK_EXPLOSION_IMPACT_KNOCKBACK_MULTIPLIER;
			living.push(pushDirection.x * knockback, 0.12D + (0.22D * damageMultiplier), pushDirection.z * knockback);
			living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, BLOCK_EXPLOSION_IMPACT_SLOW_TICKS, BLOCK_EXPLOSION_IMPACT_SLOW_AMPLIFIER, false, false, false));
			living.hurtMarked = true;
		}
	}

	private static void applyThrownBlockTerrainDamage(ServerLevel level, Vec3 pos, float blastPower, Entity source) {
		applyThrownBlockTerrainDamage(level, pos, blastPower, source, Double.POSITIVE_INFINITY, Integer.MAX_VALUE, true);
	}

	private static void applyThrownBlockTerrainDamage(ServerLevel level, Vec3 pos, float blastPower, Entity source, double maxRadius, int maxDestroyedBlocks, boolean dropResources) {
		if (blastPower > 0.0F) {
			double radius = Math.max(1.25D, blastPower * BLOCK_EXPLOSION_BLOCK_RADIUS_SCALE);
			if (Double.isFinite(maxRadius)) {
				radius = Math.min(radius, maxRadius);
			}

			List<BlockPos> targets = collectTerrainDamageTargets(level, pos, radius, maxDestroyedBlocks);
			if (!targets.isEmpty()) {
				Integer sourceEntityId = source != null ? source.getId() : null;
				PENDING_TERRAIN_DESTRUCTIONS.add(new PendingTerrainDestruction(level.dimension(), targets, sourceEntityId, dropResources));
			}
		}

		playThrownBlockExplosionEffects(level, pos, blastPower, source);
	}

	private static void tickPendingTerrainDestructions(MinecraftServer server) {
		Iterator<PendingTerrainDestruction> iterator = PENDING_TERRAIN_DESTRUCTIONS.iterator();
		while (iterator.hasNext()) {
			PendingTerrainDestruction pending = iterator.next();
			ServerLevel level = server.getLevel(pending.levelKey);
			if (level == null) {
				iterator.remove();
				continue;
			}

			Entity source = pending.sourceEntityId == null ? null : level.getEntity(pending.sourceEntityId);
			int remainingBlocks = pending.targets.size() - pending.nextIndex;
			if (remainingBlocks <= 0) {
				iterator.remove();
				continue;
			}

			int ticksRemaining = Math.max(1, pending.ticksRemaining);
			int blocksThisTick = Math.max(1, (int) Math.ceil(remainingBlocks / (double) ticksRemaining));
			int processed = 0;
			while (pending.nextIndex < pending.targets.size() && processed < blocksThisTick) {
				BlockPos targetPos = pending.targets.get(pending.nextIndex++);
				BlockState state = level.getBlockState(targetPos);
				silentlyDestroyBlock(level, targetPos, state, source, pending.dropResources);
				processed++;
			}
			pending.ticksRemaining--;

			if (pending.nextIndex >= pending.targets.size()) {
				iterator.remove();
			}
		}
	}

	private static List<BlockPos> collectTerrainDamageTargets(ServerLevel level, Vec3 pos, double radius, int maxDestroyedBlocks) {
		int blockRadius = (int) Math.ceil(radius);
		BlockPos center = BlockPos.containing(pos);
		List<BlockPos> candidates = new ArrayList<>();
		double radiusSqr = radius * radius;

		for (int x = -blockRadius; x <= blockRadius; x++) {
			for (int y = -blockRadius; y <= blockRadius; y++) {
				for (int z = -blockRadius; z <= blockRadius; z++) {
					BlockPos currentPos = center.offset(x, y, z);
					BlockState state = level.getBlockState(currentPos);
					if (state.isAir() || state.hasBlockEntity() || state.getDestroySpeed(level, currentPos) < 0.0F) {
						continue;
					}

					double distanceSqr = Vec3.atCenterOf(currentPos).distanceToSqr(pos);
					if (distanceSqr > radiusSqr) {
						continue;
					}

					double distance = Math.sqrt(distanceSqr);
					double destructionChance = 1.0D - (distance / Math.max(radius, 0.001D));
					if (destructionChance >= 0.22D) {
						candidates.add(currentPos.immutable());
					}
				}
			}
		}

		candidates.sort(Comparator.comparingDouble(blockPos -> Vec3.atCenterOf(blockPos).distanceToSqr(pos)));
		if (candidates.size() <= maxDestroyedBlocks) {
			return candidates;
		}

		return new ArrayList<>(candidates.subList(0, maxDestroyedBlocks));
	}

	private static List<BlockPos> collectHemisphereWithCylinderTerrainDamageTargets(ServerLevel level, Vec3 sphereCenter, Vec3 openingNormal, double radius, double cylinderLength, int maxDestroyedBlocks) {
		Vec3 normalizedOpeningNormal = openingNormal.lengthSqr() < 1.0E-6D ? new Vec3(0.0D, 0.0D, -1.0D) : openingNormal.normalize();
		int blockRadius = (int) Math.ceil(Math.max(radius, cylinderLength));
		BlockPos center = BlockPos.containing(sphereCenter);
		List<BlockPos> candidates = new ArrayList<>();
		double radiusSqr = Math.max(1.0E-6D, radius * radius);

		for (int x = -blockRadius; x <= blockRadius; x++) {
			for (int y = -blockRadius; y <= blockRadius; y++) {
				for (int z = -blockRadius; z <= blockRadius; z++) {
					BlockPos currentPos = center.offset(x, y, z);
					BlockState state = level.getBlockState(currentPos);
					if (state.isAir() || state.hasBlockEntity() || state.getDestroySpeed(level, currentPos) < 0.0F) {
						continue;
					}

					Vec3 offset = Vec3.atCenterOf(currentPos).subtract(sphereCenter);
					double planeProjection = offset.dot(normalizedOpeningNormal);
					double radialDistanceSqr = Math.max(0.0D, offset.lengthSqr() - (planeProjection * planeProjection));
					boolean inHemisphere = offset.lengthSqr() <= radiusSqr && planeProjection <= 0.0D;
					boolean inCylinder = planeProjection >= 0.0D && planeProjection <= cylinderLength && radialDistanceSqr <= radiusSqr;
					if (!inHemisphere && !inCylinder) {
						continue;
					}

					double normalizedDistanceSqr = inHemisphere
						? (offset.lengthSqr() / radiusSqr)
						: Math.max(radialDistanceSqr / radiusSqr, (planeProjection * planeProjection) / Math.max(1.0E-6D, cylinderLength * cylinderLength));
					double destructionChance = 1.0D - Math.sqrt(Math.min(1.0D, normalizedDistanceSqr));
					if (destructionChance >= 0.22D) {
						candidates.add(currentPos.immutable());
					}
				}
			}
		}

		candidates.sort(Comparator.comparingDouble(blockPos -> {
			Vec3 offset = Vec3.atCenterOf(blockPos).subtract(sphereCenter);
			double planeProjection = offset.dot(normalizedOpeningNormal);
			double radialDistanceSqr = Math.max(0.0D, offset.lengthSqr() - (planeProjection * planeProjection));
			boolean inHemisphere = offset.lengthSqr() <= radiusSqr && planeProjection <= 0.0D;
			return inHemisphere
					? (offset.lengthSqr() / radiusSqr)
					: Math.max(radialDistanceSqr / radiusSqr, (planeProjection * planeProjection) / Math.max(1.0E-6D, cylinderLength * cylinderLength));
		}));
		if (candidates.size() <= maxDestroyedBlocks) {
			return candidates;
		}

		return new ArrayList<>(candidates.subList(0, maxDestroyedBlocks));
	}

	private static BlockState getExplosionDebrisState(Entity source) {
		if (source instanceof FallingBlockEntity fallingBlock) {
			return fallingBlock.getBlockState();
		}
		if (source instanceof ThrownBlockEntity thrownBlock) {
			return thrownBlock.getBlockState();
		}
		if (source instanceof ThrownItemEntity thrownItem && thrownItem.getItemStack().getItem() instanceof BlockItem blockItem) {
			return blockItem.getBlock().defaultBlockState();
		}

		return null;
	}

	private static void addMushroomCloudEffect(ServerLevel level, Vec3 pos, float blastPower, BlockState debrisState, int durationTicks, double stemHeightScale, double capRadiusScale, double expansionPerTick, double upperAirRingHeight, double upperAirRingRadius, int upperAirRingBursts) {
		ACTIVE_MUSHROOM_CLOUDS.add(new MushroomCloudEffect(level.dimension(), pos, blastPower, debrisState, durationTicks, stemHeightScale, capRadiusScale, expansionPerTick, upperAirRingHeight, upperAirRingRadius, upperAirRingBursts));
	}

	private static void silentlyDestroyBlock(ServerLevel level, BlockPos blockPos, BlockState blockState, Entity source) {
		silentlyDestroyBlock(level, blockPos, blockState, source, true);
	}

	private static void silentlyDestroyBlock(ServerLevel level, BlockPos blockPos, BlockState blockState, Entity source, boolean dropResources) {
		if (!canAttackDestroyBlock(level, blockPos, blockState)) {
			return;
		}

		if (dropResources) {
			Block.dropResources(blockState, level, blockPos, null, source, ItemStack.EMPTY);
		}
		level.setBlock(blockPos, Blocks.AIR.defaultBlockState(), 3);
	}

	private static boolean canAttackDestroyBlock(ServerLevel level, BlockPos blockPos, BlockState blockState) {
		return !blockState.isAir()
				&& blockState.getBlock() != Blocks.BEDROCK
				&& !blockState.hasBlockEntity()
				&& blockState.getDestroySpeed(level, blockPos) >= 0.0F
				&& blockState.getFluidState().isEmpty();
	}

	private static void playThrownBlockExplosionEffects(ServerLevel level, Vec3 pos, float blastPower, Entity source) {
		float volume = 1.0F + (float) Math.min(0.5D, blastPower * 0.08D);
		float pitch = 0.95F - (float) Math.min(0.2D, blastPower * 0.015D);
		int explosionCount = Math.max(2, (int) Math.ceil(Math.max(0.0D, blastPower) * 0.4D));
		double debrisSpread = Math.max(0.65D, blastPower * 0.16D);
		double dustSpread = Math.max(0.7D, blastPower * 0.22D);
		double mushroomStemHeight = Math.max(1.8D, blastPower * BLOCK_EXPLOSION_MUSHROOM_STEM_HEIGHT_SCALE);
		double mushroomCapRadius = Math.max(1.5D, blastPower * BLOCK_EXPLOSION_MUSHROOM_CAP_RADIUS_SCALE);
		level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, volume, pitch);
		level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.DRAGON_FIREBALL_EXPLODE, SoundSource.BLOCKS, Math.min(2.0F, volume + 0.35F), 0.65F);
		level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.WITHER_BREAK_BLOCK, SoundSource.BLOCKS, Math.min(2.0F, volume + 0.2F), 0.72F);
		level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, pos.x, pos.y, pos.z, explosionCount, 0.0D, 0.0D, 0.0D, 0.0D);
		broadcastParticles(level, ParticleTypes.EXPLOSION, pos.x, pos.y + 0.15D, pos.z, 4 + explosionCount, 0.24D, 0.16D, 0.24D, 0.02D);
		broadcastParticles(level, ParticleTypes.CLOUD, pos.x, pos.y + 0.1D, pos.z, 8 + (explosionCount * 4), dustSpread, 0.18D, dustSpread, 0.04D);
		broadcastParticles(level, ParticleTypes.LARGE_SMOKE, pos.x, pos.y + 0.35D, pos.z, BLOCK_EXPLOSION_DUST_COUNT + (explosionCount * 2), dustSpread, 0.35D, dustSpread, 0.03D);
		broadcastParticles(level, ParticleTypes.SMOKE, pos.x, pos.y + 0.2D, pos.z, BLOCK_EXPLOSION_DUST_COUNT + (explosionCount * 3), dustSpread * 1.2D, 0.45D, dustSpread * 1.2D, 0.04D);
		broadcastParticles(level, ParticleTypes.CAMPFIRE_COSY_SMOKE, pos.x, pos.y + (mushroomStemHeight * 0.4D), pos.z, 6 + explosionCount, 0.18D, mushroomStemHeight * 0.25D, 0.18D, 0.01D);
		broadcastParticles(level, ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, pos.x, pos.y + (mushroomStemHeight * 0.65D), pos.z, 8 + explosionCount, 0.22D, mushroomStemHeight * 0.3D, 0.22D, 0.01D);

		for (int ring = 0; ring < BLOCK_EXPLOSION_MUSHROOM_RINGS; ring++) {
			double ringProgress = ring / (double) Math.max(1, BLOCK_EXPLOSION_MUSHROOM_RINGS - 1);
			double ringY = pos.y + mushroomStemHeight + (ring * 0.45D);
			double ringRadius = mushroomCapRadius * (0.6D + (ringProgress * 0.5D));
			int ringBursts = 10 + explosionCount + (ring * 5);
			for (int index = 0; index < ringBursts; index++) {
				double angle = (Math.PI * 2.0D * index) / ringBursts;
				double ringX = pos.x + (Math.cos(angle) * ringRadius);
				double ringZ = pos.z + (Math.sin(angle) * ringRadius);
				broadcastParticles(level, ParticleTypes.CLOUD, ringX, ringY, ringZ, 1, 0.08D, 0.02D, 0.08D, 0.01D);
				broadcastParticles(level, ParticleTypes.LARGE_SMOKE, ringX, ringY + 0.12D, ringZ, 1, 0.06D, 0.04D, 0.06D, 0.01D);
			}
		}

		BlockState debrisState = getExplosionDebrisState(source);
		addMushroomCloudEffect(level, pos, blastPower, debrisState, MUSHROOM_CLOUD_DURATION_TICKS, BLOCK_EXPLOSION_MUSHROOM_STEM_HEIGHT_SCALE, BLOCK_EXPLOSION_MUSHROOM_CAP_RADIUS_SCALE, MUSHROOM_CLOUD_EXPANSION_PER_TICK, 0.0D, 0.0D, 0);

		if (debrisState != null) {
			BlockParticleOption debrisParticle = new BlockParticleOption(ParticleTypes.BLOCK, debrisState);
			broadcastParticles(level, debrisParticle, pos.x, pos.y + 0.2D, pos.z, BLOCK_EXPLOSION_DEBRIS_COUNT + (explosionCount * 6), debrisSpread, 0.3D, debrisSpread, 0.18D);
		}
	}

	private static void handleSpecialThrownItemImpact(ServerLevel level, Vec3 impactPos, ThrownItemEntity thrownItem, SpecialThrownItemKind kind, UUID ownerUuid) {
		switch (kind) {
			case NETHER_STAR -> {
				applyNetherStarImpactDamage(level, impactPos, thrownItem);
				applyNetherStarTerrainDamage(level, impactPos, thrownItem);
			}
			case END_CRYSTAL -> {
				applyThrownBlockExplosionDamage(level, impactPos, thrownItem, END_CRYSTAL_PROJECTILE_BLAST_POWER, END_CRYSTAL_PROJECTILE_DAMAGE);
				applyThrownBlockTerrainDamage(level, impactPos, END_CRYSTAL_PROJECTILE_BLAST_POWER, thrownItem, END_CRYSTAL_MAX_TERRAIN_RADIUS, END_CRYSTAL_MAX_DESTROYED_BLOCKS, false);
			}
			case IRON_NUGGET -> playThrownBlockExplosionEffects(level, impactPos, 2.0F, thrownItem);
			case NETHERITE_BLOCK -> {
				applyNetheriteBlockImpactDamage(level, impactPos, thrownItem);
				applyThrownBlockTerrainDamage(level, impactPos, NETHERITE_BLOCK_PROJECTILE_BLAST_POWER, thrownItem, NETHERITE_BLOCK_MAX_TERRAIN_RADIUS, NETHERITE_BLOCK_MAX_DESTROYED_BLOCKS, false);
				if (!level.isClientSide()) {
					level.addFreshEntity(new ItemEntity(level, impactPos.x, impactPos.y, impactPos.z, new ItemStack(Blocks.NETHERITE_BLOCK)));
				}
			}
		}
	}

	private static void applyNetherStarTerrainDamage(ServerLevel level, Vec3 impactPos, ThrownItemEntity thrownItem) {
		Vec3 sphereCenter = impactPos.add(0.0D, NETHER_STAR_TERRAIN_CENTER_HEIGHT_OFFSET, 0.0D);
		Vec3 openingNormal = thrownItem.getDeltaMovement().scale(-1.0D);
		List<BlockPos> targets = collectHemisphereWithCylinderTerrainDamageTargets(
				level,
				sphereCenter,
				openingNormal,
				NETHER_STAR_TERRAIN_HEMISPHERE_RADIUS,
				NETHER_STAR_TERRAIN_CYLINDER_LENGTH,
				NETHER_STAR_MAX_DESTROYED_BLOCKS);
		if (!targets.isEmpty()) {
			PENDING_TERRAIN_DESTRUCTIONS.add(new PendingTerrainDestruction(level.dimension(), targets, thrownItem.getId(), false));
		}

		playThrownBlockExplosionEffects(level, impactPos, NETHER_STAR_PROJECTILE_BLAST_POWER, thrownItem);
		addMushroomCloudEffect(level, impactPos, NETHER_STAR_PROJECTILE_BLAST_POWER, getExplosionDebrisState(thrownItem), NETHER_STAR_MUSHROOM_DURATION_TICKS, NETHER_STAR_MUSHROOM_STEM_HEIGHT_SCALE, NETHER_STAR_MUSHROOM_CAP_RADIUS_SCALE, NETHER_STAR_MUSHROOM_EXPANSION_PER_TICK, NETHER_STAR_UPPER_AIR_RING_HEIGHT, NETHER_STAR_UPPER_AIR_RING_RADIUS, NETHER_STAR_UPPER_AIR_RING_BURSTS);
	}

	private static void applyNetherStarImpactDamage(ServerLevel level, Vec3 impactPos, Entity source) {
		AABB damageArea = new AABB(impactPos, impactPos).inflate(NETHER_STAR_HEAVY_DAMAGE_RADIUS);
		for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, damageArea, entity -> entity.isAlive() && !(entity instanceof Player))) {
			double distance = living.position().distanceTo(impactPos);
			if (distance <= NETHER_STAR_EXECUTE_RADIUS) {
				damageTarget(living, level.damageSources().explosion(source, null), Float.MAX_VALUE);
				if (living.isAlive()) {
					living.kill();
				}
			} else if (distance <= NETHER_STAR_HEAVY_DAMAGE_RADIUS) {
				double outerProgress = (distance - NETHER_STAR_EXECUTE_RADIUS) / Math.max(0.001D, NETHER_STAR_HEAVY_DAMAGE_RADIUS - NETHER_STAR_EXECUTE_RADIUS);
				float scaledDamage = (float) (NETHER_STAR_HEAVY_DAMAGE - ((NETHER_STAR_HEAVY_DAMAGE - NETHER_STAR_MIN_EDGE_DAMAGE) * Math.min(1.0D, outerProgress)));
				damageTarget(living, level.damageSources().explosion(source, null), scaledDamage);
			}

			Vec3 pushDirection = living.position().subtract(impactPos);
			if (pushDirection.lengthSqr() < 1.0E-4D) {
				pushDirection = new Vec3(0.0D, 1.0D, 0.0D);
			} else {
				pushDirection = pushDirection.normalize();
			}

			living.push(pushDirection.x * 1.8D, 0.25D, pushDirection.z * 1.8D);
			living.hurtMarked = true;
		}
	}

	private static void applyNetheriteBlockImpactDamage(ServerLevel level, Vec3 impactPos, Entity source) {
		double radius = Math.max(2.0D, BLOCK_EXPLOSION_MAX_DAMAGE_RADIUS * (NETHERITE_BLOCK_DAMAGE / OBSIDIAN_BLOCK_EXPLOSION_DAMAGE));
		AABB damageArea = new AABB(impactPos, impactPos).inflate(radius);
		for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, damageArea, entity -> entity.isAlive() && !(entity instanceof Player))) {
			double distance = living.position().distanceTo(impactPos);
			if (distance > radius) {
				continue;
			}

			double distanceRatio = Math.min(1.0D, distance / radius);
			double damageMultiplier;
			if (distanceRatio <= BLOCK_EXPLOSION_FULL_DAMAGE_PORTION) {
				damageMultiplier = 1.0D;
			} else {
				double outerProgress = (distanceRatio - BLOCK_EXPLOSION_FULL_DAMAGE_PORTION) / (1.0D - BLOCK_EXPLOSION_FULL_DAMAGE_PORTION);
				damageMultiplier = 1.0D - ((1.0D - BLOCK_EXPLOSION_MIN_DAMAGE_MULTIPLIER) * outerProgress);
			}

			float dealtDamage = distance <= NETHERITE_BLOCK_DAMAGE_RADIUS
					? NETHERITE_BLOCK_DAMAGE
					: (distance <= BLOCK_EXPLOSION_FATAL_RADIUS ? BLOCK_EXPLOSION_FATAL_DAMAGE : (float) (NETHERITE_BLOCK_DAMAGE * damageMultiplier));
			if (dealtDamage <= 0.0F) {
				continue;
			}

			damageTarget(living, level.damageSources().explosion(source, null), dealtDamage);

			Vec3 pushDirection = living.position().subtract(impactPos);
			if (pushDirection.lengthSqr() < 1.0E-4D) {
				pushDirection = new Vec3(0.0D, 1.0D, 0.0D);
			} else {
				pushDirection = pushDirection.normalize();
			}

			double knockback = (BLOCK_EXPLOSION_MIN_KNOCKBACK + ((damageMultiplier - BLOCK_EXPLOSION_MIN_DAMAGE_MULTIPLIER) / (1.0D - BLOCK_EXPLOSION_MIN_DAMAGE_MULTIPLIER) * (BLOCK_EXPLOSION_MAX_KNOCKBACK - BLOCK_EXPLOSION_MIN_KNOCKBACK)))
					* BLOCK_EXPLOSION_IMPACT_KNOCKBACK_MULTIPLIER;
			living.push(pushDirection.x * knockback, 0.12D + (0.22D * damageMultiplier), pushDirection.z * knockback);
			living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, BLOCK_EXPLOSION_IMPACT_SLOW_TICKS, BLOCK_EXPLOSION_IMPACT_SLOW_AMPLIFIER, false, false, false));
			living.hurtMarked = true;
		}
	}

	private static void applyIronNuggetPierce(ServerLevel level, Vec3 start, Vec3 end, ThrownItemEntity thrownItem, UUID ownerUuid) {
		Vec3 segment = end.subtract(start);
		double segmentLength = segment.length();
		if (segmentLength < 1.0E-6D) {
			return;
		}

		Vec3 direction = segment.normalize();
		Set<Integer> hitEntities = THROWN_ITEM_HIT_ENTITIES.computeIfAbsent(thrownItem.getId(), ignored -> new HashSet<>());
		AABB damageArea = new AABB(start, end).inflate(IRON_NUGGET_PIERCE_RADIUS);
		for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, damageArea, entity -> entity.isAlive() && (ownerUuid == null || !entity.getUUID().equals(ownerUuid)))) {
			if (!hitEntities.add(living.getId())) {
				continue;
			}

			double distance = distanceToSegment(living.position().add(0.0D, living.getBbHeight() * 0.5D, 0.0D), start, end);
			if (distance > IRON_NUGGET_PIERCE_RADIUS) {
				continue;
			}

			damageTarget(living, level.damageSources().magic(), IRON_NUGGET_PIERCE_DAMAGE);
			living.push(direction.x * 0.8D, 0.05D, direction.z * 0.8D);
			living.hurtMarked = true;
		}

		int samples = Math.max(1, (int) Math.ceil(segmentLength / IRON_NUGGET_STEP_DISTANCE));
		int blockRadius = (int) Math.ceil(IRON_NUGGET_BLOCK_RADIUS);
		int destroyedBlocks = 0;
		outer:
		for (int sample = 0; sample <= samples; sample++) {
			double progress = sample / (double) samples;
			Vec3 point = start.add(segment.scale(progress));
			BlockPos center = BlockPos.containing(point);
			for (int x = -blockRadius; x <= blockRadius; x++) {
				for (int y = -blockRadius; y <= blockRadius; y++) {
					for (int z = -blockRadius; z <= blockRadius; z++) {
						BlockPos currentPos = center.offset(x, y, z);
						BlockState state = level.getBlockState(currentPos);
						if (state.getBlock() == Blocks.NETHERITE_BLOCK) {
							continue;
						}

						if (!canAttackDestroyBlock(level, currentPos, state)) {
							continue;
						}

						double distance = Vec3.atCenterOf(currentPos).distanceTo(point);
						if (distance <= IRON_NUGGET_BLOCK_RADIUS) {
							silentlyDestroyBlock(level, currentPos, state, thrownItem, false);
							destroyedBlocks++;
							if (destroyedBlocks >= IRON_NUGGET_MAX_DESTROYED_BLOCKS_PER_TICK) {
								break outer;
							}
						}
					}
				}
			}
		}
	}

	private static double distanceToSegment(Vec3 point, Vec3 start, Vec3 end) {
		Vec3 segment = end.subtract(start);
		double lengthSqr = segment.lengthSqr();
		if (lengthSqr < 1.0E-6D) {
			return point.distanceTo(start);
		}

		double projection = point.subtract(start).dot(segment) / lengthSqr;
		double clampedProjection = Math.max(0.0D, Math.min(1.0D, projection));
		Vec3 closestPoint = start.add(segment.scale(clampedProjection));
		return point.distanceTo(closestPoint);
	}

	private static ServerLevel findThrownItemLevel(MinecraftServer server, int entityId) {
		ResourceKey<Level> levelKey = THROWN_ITEM_LEVELS.get(entityId);
		return levelKey == null ? null : server.getLevel(levelKey);
	}

	private static <T extends ParticleOptions> void broadcastParticles(ServerLevel level, T particle, double x, double y, double z, int count, double offsetX, double offsetY, double offsetZ, double speed) {
		for (ServerPlayer serverPlayer : level.players()) {
			level.sendParticles(serverPlayer, particle, true, x, y, z, count, offsetX, offsetY, offsetZ, speed);
		}
	}

	private static void tickMushroomClouds(MinecraftServer server) {
		Iterator<MushroomCloudEffect> iterator = ACTIVE_MUSHROOM_CLOUDS.iterator();
		while (iterator.hasNext()) {
			MushroomCloudEffect effect = iterator.next();
			ServerLevel level = server.getLevel(effect.levelKey);
			if (level == null) {
				iterator.remove();
				continue;
			}

			effect.age++;
			if (effect.age > effect.durationTicks) {
				iterator.remove();
				continue;
			}

			double ageProgress = effect.age / (double) effect.durationTicks;
			double stemHeight = Math.max(1.8D, effect.blastPower * effect.stemHeightScale) + (effect.age * effect.expansionPerTick);
			double capRadius = Math.max(1.5D, effect.blastPower * effect.capRadiusScale) + (effect.age * effect.expansionPerTick * 1.4D);
			double ringY = effect.origin.y + stemHeight;
			int ringBursts = 14 + (int) Math.ceil(effect.blastPower * 0.45D) + effect.age;

			for (int index = 0; index < ringBursts; index++) {
				double angle = (Math.PI * 2.0D * index) / ringBursts;
				double ringX = effect.origin.x + (Math.cos(angle) * capRadius);
				double ringZ = effect.origin.z + (Math.sin(angle) * capRadius);
				broadcastParticles(level, ParticleTypes.CLOUD, ringX, ringY, ringZ, 1, 0.1D, 0.03D, 0.1D, 0.01D);
				broadcastParticles(level, ParticleTypes.LARGE_SMOKE, ringX, ringY + 0.16D, ringZ, 1, 0.08D, 0.04D, 0.08D, 0.01D);
			}

			if (effect.upperAirRingBursts > 0) {
				double upperRingY = effect.origin.y + stemHeight + effect.upperAirRingHeight;
				double upperRingRadius = effect.upperAirRingRadius + (ageProgress * 2.0D);
				int upperRingBursts = effect.upperAirRingBursts + effect.age;
				for (int index = 0; index < upperRingBursts; index++) {
					double angle = (Math.PI * 2.0D * index) / upperRingBursts;
					double ringX = effect.origin.x + (Math.cos(angle) * upperRingRadius);
					double ringZ = effect.origin.z + (Math.sin(angle) * upperRingRadius);
					broadcastParticles(level, ParticleTypes.CLOUD, ringX, upperRingY, ringZ, 1, 0.12D, 0.02D, 0.12D, 0.01D);
					broadcastParticles(level, ParticleTypes.SMOKE, ringX, upperRingY + 0.12D, ringZ, 1, 0.1D, 0.03D, 0.1D, 0.01D);
				}
			}

			broadcastParticles(level, ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, effect.origin.x, effect.origin.y + (stemHeight * 0.7D), effect.origin.z, 5 + effect.age, 0.25D + ageProgress, stemHeight * 0.12D, 0.25D + ageProgress, 0.005D);
			broadcastParticles(level, ParticleTypes.SMOKE, effect.origin.x, effect.origin.y + (stemHeight * 0.35D), effect.origin.z, 6 + effect.age, 0.3D + ageProgress, 0.18D, 0.3D + ageProgress, 0.01D);

			if (effect.debrisState != null && effect.age <= 6) {
				BlockParticleOption debrisParticle = new BlockParticleOption(ParticleTypes.BLOCK, effect.debrisState);
				broadcastParticles(level, debrisParticle, effect.origin.x, effect.origin.y + 0.25D + (effect.age * 0.08D), effect.origin.z, 8 + effect.age, 0.7D + ageProgress, 0.18D, 0.7D + ageProgress, 0.08D);
			}
		}
	}

	private static void explodeThrownBlock(ServerLevel level, Vec3 pos, Entity source, float blastPower, float explosionDamage) {
		applyThrownBlockExplosionDamage(level, pos, source, blastPower, explosionDamage);
		applyThrownBlockTerrainDamage(level, pos, blastPower, source);
	}

	private static ServerLevel findThrownBlockLevel(MinecraftServer server, int entityId) {
		ResourceKey<Level> levelKey = THROWN_BLOCK_LEVELS.get(entityId);
		return levelKey == null ? null : server.getLevel(levelKey);
	}

	private static boolean canGrabBlock(ServerPlayer player, BlockPos blockPos, BlockState blockState) {
		return !blockState.isAir()
				&& !blockState.hasBlockEntity()
				&& blockState.getDestroySpeed(player.serverLevel(), blockPos) >= 0.0F
				&& blockState.getFluidState().isEmpty();
	}

	private static void tickGrab(ServerPlayer player, Entity target) {
		if (!target.isAlive()) {
			ACTIVE_GRABS.remove(player.getUUID());
			GRABBED_BLOCK_BLASTS.remove(target.getId());
			GRABBED_BLOCK_DAMAGES.remove(target.getId());
			return;
		}

		double targetDistanceSqr = player.distanceToSqr(target);
		if (targetDistanceSqr > MAX_GRAB_RECOVER_DISTANCE_SQR) {
			ACTIVE_GRABS.remove(player.getUUID());
			GRABBED_BLOCK_BLASTS.remove(target.getId());
			GRABBED_BLOCK_DAMAGES.remove(target.getId());
			clearGrabState(target);
			return;
		}

		if (target.isPassenger()) {
			target.stopRiding();
		}

		if (target instanceof FallingBlockEntity fallingBlock) {
			applyGrabState(fallingBlock);
			tickBlockGrab(player, fallingBlock);
			applyCarryFire(player, target);
			target.fallDistance = 0.0F;
			target.hurtMarked = true;
			return;
		}

		applyGrabState(target);
		Vec3 desiredPos = getGrabbedTargetPosition(player, target);
		Vec3 currentPos = target.position();
		Vec3 step = desiredPos.subtract(currentPos);
		if (step.lengthSqr() > LIVING_GRAB_LOCK_STEP * LIVING_GRAB_LOCK_STEP) {
			step = step.normalize().scale(LIVING_GRAB_LOCK_STEP);
		}

		Vec3 nextPos = targetDistanceSqr > GRAB_SNAP_DISTANCE_SQR ? desiredPos : currentPos.add(step);
		moveGrabbedTarget(target, nextPos, player);
		target.setDeltaMovement(nextPos.subtract(currentPos));
		applyCarryFire(player, target);
		target.fallDistance = 0.0F;
		target.hurtMarked = true;
	}

	private static void tickBlockGrab(ServerPlayer player, FallingBlockEntity target) {
		applyGrabState(target);
		Vec3 desiredPos = getGrabbedTargetPosition(player, target);
		Vec3 currentPos = target.position();
		Vec3 delta = desiredPos.subtract(currentPos);
		Vec3 nextPos;

		if (delta.lengthSqr() > BLOCK_GRAB_SNAP_DISTANCE_SQR) {
			nextPos = desiredPos;
		} else {
			Vec3 step = delta.scale(BLOCK_GRAB_LERP_FACTOR);
			if (step.lengthSqr() > BLOCK_GRAB_MAX_STEP * BLOCK_GRAB_MAX_STEP) {
				step = step.normalize().scale(BLOCK_GRAB_MAX_STEP);
			}

			nextPos = delta.lengthSqr() <= 0.0004D ? desiredPos : currentPos.add(step);
		}

		moveGrabbedTarget(target, nextPos, player);
		target.setDeltaMovement(Vec3.ZERO);
		target.fallDistance = 0.0F;
	}

	private static Vec3 getGrabbedTargetPosition(ServerPlayer player, Entity target) {
		double anchorDistance = DEFAULT_GRAB_ANCHOR_DISTANCE + getLargeGrabDistanceBonus(target);
		Vec3 anchor = getGrabAnchor(player, anchorDistance, false);
		if (target instanceof FallingBlockEntity) {
			anchor = getGrabAnchor(player, BLOCK_GRAB_ANCHOR_DISTANCE, false).add(getRightVector(player).scale(BLOCK_GRAB_RIGHT_OFFSET));
		} else if (target instanceof LivingEntity) {
			anchor = getGrabAnchor(player, LIVING_GRAB_ANCHOR_DISTANCE + getLargeGrabDistanceBonus(target), true);
		}

		double targetY = anchor.y - (target.getBbHeight() * 0.5D);
		return new Vec3(anchor.x, targetY, anchor.z);
	}

	private static void moveGrabbedTarget(Entity target, Vec3 pos, ServerPlayer player) {
		target.moveTo(pos.x, pos.y, pos.z, player.getYRot(), player.getXRot());
		if (target instanceof LivingEntity livingTarget) {
			livingTarget.setYBodyRot(player.getYRot());
			livingTarget.setYHeadRot(player.getYRot());
		}
	}

	private static void applyGrabState(Entity target) {
		target.setNoGravity(true);
		target.noPhysics = shouldDisableGrabCollision(target);
	}

	private static void clearGrabState(Entity target) {
		target.setNoGravity(false);
		target.noPhysics = false;
	}

	private static boolean shouldDisableGrabCollision(Entity target) {
		return target instanceof FallingBlockEntity || isLargeGrabTarget(target);
	}

	private static boolean isLargeGrabTarget(Entity target) {
		return target.getBbWidth() > LARGE_GRAB_WIDTH_THRESHOLD || target.getBbHeight() > LARGE_GRAB_HEIGHT_THRESHOLD;
	}

	private static double getLargeGrabDistanceBonus(Entity target) {
		double widthBonus = Math.max(0.0D, target.getBbWidth() - LARGE_GRAB_WIDTH_THRESHOLD) * LARGE_GRAB_WIDTH_DISTANCE_SCALE;
		double heightBonus = Math.max(0.0D, target.getBbHeight() - LARGE_GRAB_HEIGHT_THRESHOLD) * LARGE_GRAB_HEIGHT_DISTANCE_SCALE;
		return Math.min(MAX_LARGE_GRAB_DISTANCE_BONUS, widthBonus + heightBonus);
	}

	private static Vec3 getGrabAnchor(ServerPlayer player, double anchorDistance, boolean includeVelocityLead) {
		Vec3 anchor = player.getEyePosition().add(player.getLookAngle().normalize().scale(anchorDistance));
		if (!includeVelocityLead) {
			return anchor;
		}

		Vec3 lead = player.getDeltaMovement().scale(LIVING_GRAB_VELOCITY_SCALE);
		if (lead.lengthSqr() > LIVING_GRAB_MAX_VELOCITY_LEAD * LIVING_GRAB_MAX_VELOCITY_LEAD) {
			lead = lead.normalize().scale(LIVING_GRAB_MAX_VELOCITY_LEAD);
		}

		return anchor.add(lead);
	}

	private static void forceTargetInFront(ServerPlayer player, Entity target) {
		moveGrabbedTarget(target, getGrabbedTargetPosition(player, target), player);
	}

	private static EntityHitResult findThrownEntityHit(Entity projectile, Vec3 start, Vec3 end, UUID excludedUuid) {
		AABB searchBox = projectile.getBoundingBox().expandTowards(end.subtract(start)).inflate(1.0D);
		double maxDistanceSqr = start.distanceToSqr(end);
		return ProjectileUtil.getEntityHitResult(projectile, start, end, searchBox,
				entity -> entity.isAlive() && entity.isPickable() && !entity.isSpectator() && entity != projectile && (excludedUuid == null || !entity.getUUID().equals(excludedUuid)),
				maxDistanceSqr);
	}

	private static boolean isEntityImpactCloser(Vec3 start, BlockHitResult blockHit, EntityHitResult entityHit) {
		if (entityHit == null) {
			return false;
		}

		if (blockHit == null) {
			return true;
		}

		return start.distanceToSqr(entityHit.getLocation()) <= start.distanceToSqr(blockHit.getLocation());
	}

	private static void tickThrownBlocks(MinecraftServer server) {
		Iterator<Map.Entry<Integer, Integer>> linearIterator = THROWN_LINEAR_TIMERS.entrySet().iterator();
		while (linearIterator.hasNext()) {
			Map.Entry<Integer, Integer> entry = linearIterator.next();
			int entityId = entry.getKey();
			Entity entity = findEntityById(server, entry.getKey());
			if (entity == null || !entity.isAlive()) {
				clearThrownLinearState(entityId);
				linearIterator.remove();
				continue;
			}

			Vec3 throwVelocity = THROWN_LINEAR_VELOCITIES.get(entityId);
			if (throwVelocity != null) {
				if (entity instanceof FallingBlockEntity fallingBlock) {
					ServerLevel level = (ServerLevel) fallingBlock.level();
					Vec3 currentPos = fallingBlock.position();
					Vec3 previousPos = THROWN_BLOCK_LAST_POSITIONS.getOrDefault(entityId, currentPos.subtract(throwVelocity));
					HitResult hitResult = level.clip(new ClipContext(previousPos, currentPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, fallingBlock));
					BlockHitResult blockHit = hitResult instanceof BlockHitResult candidate && candidate.getType() == HitResult.Type.BLOCK ? candidate : null;
					EntityHitResult entityHit = findThrownEntityHit(fallingBlock, previousPos, currentPos, null);
					if (isEntityImpactCloser(previousPos, blockHit, entityHit)) {
						float blastPower = THROWN_BLOCK_BLASTS.getOrDefault(entityId, 0.0F);
						float explosionDamage = THROWN_BLOCK_DAMAGES.getOrDefault(entityId, 0.0F);
						Vec3 impactPos = entityHit.getLocation();
						THROWN_BLOCK_LAST_POSITIONS.put(entityId, impactPos);
						explodeThrownBlock(level, impactPos, fallingBlock, blastPower, explosionDamage);
						clearThrownState(entityId);
						fallingBlock.discard();
						linearIterator.remove();
						continue;
					}

					if (blockHit != null) {
						float blastPower = THROWN_BLOCK_BLASTS.getOrDefault(entityId, 0.0F);
						float explosionDamage = THROWN_BLOCK_DAMAGES.getOrDefault(entityId, 0.0F);
						Vec3 impactPos = blockHit.getLocation();
						THROWN_BLOCK_LAST_POSITIONS.put(entityId, impactPos);
						explodeThrownBlock(level, impactPos, fallingBlock, blastPower, explosionDamage);
						clearThrownState(entityId);
						fallingBlock.discard();
						linearIterator.remove();
						continue;
					}

					THROWN_BLOCK_LAST_POSITIONS.put(entityId, currentPos);
					fallingBlock.setNoGravity(true);
					fallingBlock.noPhysics = false;
					fallingBlock.setDeltaMovement(throwVelocity);
					fallingBlock.hurtMarked = true;
					continue;
				}

				entity.setNoGravity(true);
				entity.setDeltaMovement(throwVelocity);
				entity.hurtMarked = true;
			}

			int remainingTicks = entry.getValue() - 1;
			if (remainingTicks <= 0) {
				entity.setNoGravity(false);
				clearThrownLinearState(entityId);
				linearIterator.remove();
			} else {
				entry.setValue(remainingTicks);
			}
		}

		Iterator<Map.Entry<Integer, Float>> iterator = THROWN_BLOCK_BLASTS.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<Integer, Float> entry = iterator.next();
			int entityId = entry.getKey();
			Entity entity = findEntityById(server, entityId);
			if (!(entity instanceof ThrownBlockEntity thrownBlock) || !thrownBlock.isAlive()) {
				ServerLevel level = entity != null ? (ServerLevel) entity.level() : findThrownBlockLevel(server, entityId);
				Vec3 fallbackPos = THROWN_BLOCK_LAST_POSITIONS.get(entityId);
				float explosionDamage = THROWN_BLOCK_DAMAGES.getOrDefault(entityId, 0.0F);
				if (level != null && fallbackPos != null) {
					explodeThrownBlock(level, fallbackPos, entity, entry.getValue(), explosionDamage);
				}
				clearThrownBlockAuxState(entityId);
				iterator.remove();
				continue;
			}

			ServerLevel level = (ServerLevel) thrownBlock.level();
			Vec3 currentPos = thrownBlock.position();
			Vec3 previousPos = THROWN_BLOCK_LAST_POSITIONS.getOrDefault(entityId, currentPos.subtract(thrownBlock.getDeltaMovement()));
			HitResult hitResult = level.clip(new ClipContext(previousPos, currentPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, thrownBlock));
			BlockHitResult blockHit = hitResult instanceof BlockHitResult candidate && candidate.getType() == HitResult.Type.BLOCK ? candidate : null;
			EntityHitResult entityHit = findThrownEntityHit(thrownBlock, previousPos, currentPos, THROWN_BLOCK_OWNERS.get(entityId));
			if (isEntityImpactCloser(previousPos, blockHit, entityHit)) {
				Vec3 impactPos = entityHit.getLocation();
				THROWN_BLOCK_LAST_POSITIONS.put(entityId, impactPos);
				float explosionDamage = THROWN_BLOCK_DAMAGES.getOrDefault(entityId, 0.0F);
				explodeThrownBlock(level, impactPos, thrownBlock, entry.getValue(), explosionDamage);
				clearThrownBlockAuxState(entityId);
				thrownBlock.discard();
				iterator.remove();
				continue;
			}

			if (blockHit != null) {
				Vec3 impactPos = blockHit.getLocation();
				THROWN_BLOCK_LAST_POSITIONS.put(entityId, impactPos);
				float explosionDamage = THROWN_BLOCK_DAMAGES.getOrDefault(entityId, 0.0F);
				explodeThrownBlock(level, impactPos, thrownBlock, entry.getValue(), explosionDamage);
				clearThrownBlockAuxState(entityId);
				thrownBlock.discard();
				iterator.remove();
				continue;
			}

			THROWN_BLOCK_LAST_POSITIONS.put(entityId, currentPos);
			if (thrownBlock.tickCount > THROWN_BLOCK_MAX_TICKS) {
				float explosionDamage = THROWN_BLOCK_DAMAGES.getOrDefault(entityId, 0.0F);
				explodeThrownBlock(level, currentPos, thrownBlock, entry.getValue(), explosionDamage);
				clearThrownBlockAuxState(entityId);
				thrownBlock.discard();
				iterator.remove();
			}
		}
	}

	private static void tickThrownItems(MinecraftServer server) {
		Iterator<Map.Entry<Integer, SpecialThrownItemKind>> iterator = THROWN_ITEM_KINDS.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<Integer, SpecialThrownItemKind> entry = iterator.next();
			int entityId = entry.getKey();
			SpecialThrownItemKind kind = entry.getValue();
			Entity entity = findEntityById(server, entityId);
			if (!(entity instanceof ThrownItemEntity thrownItem) || !thrownItem.isAlive()) {
				clearThrownItemState(entityId);
				iterator.remove();
				continue;
			}

			ServerLevel level = (ServerLevel) thrownItem.level();
			Vec3 currentPos = thrownItem.position();
			Vec3 previousPos = THROWN_ITEM_LAST_POSITIONS.getOrDefault(entityId, currentPos.subtract(thrownItem.getDeltaMovement()));
			UUID ownerUuid = THROWN_ITEM_OWNERS.get(entityId);

			if (kind == SpecialThrownItemKind.IRON_NUGGET) {
				applyIronNuggetPierce(level, previousPos, currentPos, thrownItem, ownerUuid);
				if (thrownItem.tickCount >= IRON_NUGGET_MAX_TRAVEL_TICKS) {
					handleSpecialThrownItemImpact(level, currentPos, thrownItem, kind, ownerUuid);
					clearThrownItemState(entityId);
					thrownItem.discard();
					iterator.remove();
					continue;
				}
			}

			HitResult hitResult = level.clip(new ClipContext(previousPos, currentPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, thrownItem));
			BlockHitResult blockHit = hitResult instanceof BlockHitResult candidate && candidate.getType() == HitResult.Type.BLOCK ? candidate : null;
			EntityHitResult entityHit = kind == SpecialThrownItemKind.IRON_NUGGET ? null : findThrownEntityHit(thrownItem, previousPos, currentPos, ownerUuid);
			if (isEntityImpactCloser(previousPos, blockHit, entityHit)) {
				Vec3 impactPos = entityHit.getLocation();
				THROWN_ITEM_LAST_POSITIONS.put(entityId, impactPos);
				handleSpecialThrownItemImpact(level, impactPos, thrownItem, kind, ownerUuid);
				clearThrownItemState(entityId);
				thrownItem.discard();
				iterator.remove();
				continue;
			}

			if (blockHit != null) {
				Vec3 impactPos = blockHit.getLocation();
				THROWN_ITEM_LAST_POSITIONS.put(entityId, impactPos);
				handleSpecialThrownItemImpact(level, impactPos, thrownItem, kind, ownerUuid);
				clearThrownItemState(entityId);
				thrownItem.discard();
				iterator.remove();
				continue;
			}

			THROWN_ITEM_LAST_POSITIONS.put(entityId, currentPos);
			if (thrownItem.tickCount > kind.getMaxTicks()) {
				handleSpecialThrownItemImpact(level, currentPos, thrownItem, kind, ownerUuid);
				clearThrownItemState(entityId);
				thrownItem.discard();
				iterator.remove();
			}
		}
	}

	private static void clearThrownLinearState(int entityId) {
		THROWN_LINEAR_VELOCITIES.remove(entityId);
	}

	private static void clearThrownState(int entityId) {
		clearThrownLinearState(entityId);
		clearThrownBlockAuxState(entityId);
		THROWN_BLOCK_BLASTS.remove(entityId);
	}

	private static void clearThrownBlockAuxState(int entityId) {
		THROWN_BLOCK_DAMAGES.remove(entityId);
		THROWN_BLOCK_LAST_POSITIONS.remove(entityId);
		THROWN_BLOCK_LEVELS.remove(entityId);
		THROWN_BLOCK_OWNERS.remove(entityId);
	}

	private static void clearThrownItemState(int entityId) {
		THROWN_ITEM_LAST_POSITIONS.remove(entityId);
		THROWN_ITEM_LEVELS.remove(entityId);
		THROWN_ITEM_OWNERS.remove(entityId);
		THROWN_ITEM_HIT_ENTITIES.remove(entityId);
	}

	private static Entity findEntityById(MinecraftServer server, int entityId) {
		for (ServerLevel level : server.getAllLevels()) {
			Entity entity = level.getEntity(entityId);
			if (entity != null) {
				return entity;
			}
		}

		return null;
	}

	private static void applyCarryFire(ServerPlayer player, Entity target) {
		Vec3 playerVelocity = player.getDeltaMovement();
		if (playerVelocity.lengthSqr() < CARRY_FIRE_SPEED_SQR) {
			return;
		}

		target.setSecondsOnFire(4);

		ServerLevel level = player.serverLevel();
		Vec3 position = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
		level.sendParticles(ParticleTypes.FLAME, position.x, position.y, position.z, 8, 0.18D, 0.18D, 0.18D, 0.01D);
		level.sendParticles(ParticleTypes.SMALL_FLAME, position.x, position.y, position.z, 5, 0.12D, 0.12D, 0.12D, 0.005D);

		if (player.tickCount % 5 != 0) {
			return;
		}

		damageTarget(target, player.damageSources().inFire(), CARRY_FIRE_DAMAGE);
		target.hurtMarked = true;
	}

	static void handleInputAction(ServerPlayer player, InputAction action) {
		switch (action) {
			case QUICK_HEAVY_ATTACK -> performQuickHeavyAttack(player);
			case HEAVY_ATTACK -> performHeavyAttack(player);
			case DIRECT_THROW -> throwHeldBlock(player);
			case START_GRAB -> startGrab(player);
			case STOP_GRAB -> stopGrab(player);
		}
	}

	static void onServerPlayerTick(ServerPlayer player) {
		Integer targetId = ACTIVE_GRABS.get(player.getUUID());
		if (targetId == null || !player.isAlive()) {
			return;
		}

		Entity target = player.serverLevel().getEntity(targetId);
		if (target == null || target == player) {
			ACTIVE_GRABS.remove(player.getUUID());
			GRABBED_BLOCK_BLASTS.remove(targetId);
			return;
		}

		tickGrab(player, target);
	}

	static void onServerTick(MinecraftServer server) {
		if (THROWN_BLOCK_BLASTS.isEmpty() && THROWN_LINEAR_TIMERS.isEmpty() && THROWN_ITEM_KINDS.isEmpty() && ACTIVE_MUSHROOM_CLOUDS.isEmpty() && PENDING_TERRAIN_DESTRUCTIONS.isEmpty()) {
			return;
		}

		tickThrownBlocks(server);
		tickThrownItems(server);
		tickPendingTerrainDestructions(server);
		tickMushroomClouds(server);
	}
}
