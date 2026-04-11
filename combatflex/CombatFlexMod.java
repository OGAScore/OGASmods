package com.OGAS.combatflex;

import com.OGAS.combatflex.flight.FlightFeature;
import com.OGAS.combatflex.grab.GrabAndPullFeature;
import com.OGAS.combatflex.network.NetworkHandler;
import com.OGAS.combatflex.network.packet.SyncSkillDataPacket;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(CombatFlexMod.MOD_ID)
public class CombatFlexMod {
	public static final String MOD_ID = "cbtflex";
	private static final int BATTLE_MOMENTUM_TICKS = 20 * 3;
	private static final int EFFECT_DURATION_TICKS = 40;
	private static final int REFRESH_THRESHOLD_TICKS = 10;
	private static final int SECOND_WIND_EFFECT_TICKS = 20 * 20;
	private static final long ONE_MINUTE_TICKS = 20L * 60L;
	private static final long THIRTY_SECONDS_TICKS = 20L * 30L;
	private static final long FIVE_MINUTES_TICKS = 20L * 300L;
	private static final float EXECUTIONER_HEALTH_THRESHOLD = 0.35F;
	private static final float EXECUTIONER_DAMAGE_MULTIPLIER = 1.35F;

	public CombatFlexMod(FMLJavaModLoadingContext context) {
		NetworkHandler.register();
		GrabAndPullFeature.init(context.getModEventBus());
		FlightFeature.init(context.getModEventBus());
		MinecraftForge.EVENT_BUS.register(this);
	}

	@SubscribeEvent
	public void onPlayerTick(TickEvent.PlayerTickEvent event) {
		if (event.phase != TickEvent.Phase.END) {
			return;
		}

		Player player = event.player;
		if (player.level().isClientSide()) {
			return;
		}

		if (player instanceof ServerPlayer serverPlayer) {
			SpecialSkillFeature.tick(serverPlayer);
		}

		SkillData.applyPendingHurtMotion(player, player.level().getGameTime());

		if (SkillData.isPendingFullHeal(player)) {
			player.setHealth(player.getMaxHealth());
			SkillData.setPendingFullHeal(player, false);
			if (SkillData.isPendingSecondWindBuff(player)) {
				player.addEffect(new MobEffectInstance(MobEffects.HEALTH_BOOST, SECOND_WIND_EFFECT_TICKS, 4, false, false,
						false));
				player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, SECOND_WIND_EFFECT_TICKS, 3, false, false,
						false));
				SkillData.setPendingSecondWindBuff(player, false);
			}
			syncPlayerData((ServerPlayer) player);
		}

		if (SkillData.tickLowHealthSurvival(player)) {
			syncPlayerData((ServerPlayer) player);
		}

		float missingHealth = Math.max(0.0F, player.getMaxHealth() - player.getHealth());
		int resistanceLevel = (int) (missingHealth / 6.0F);
		int regenerationLevel = (int) (missingHealth / 5.0F);

		syncEffect(player, MobEffects.DAMAGE_RESISTANCE, resistanceLevel);
		syncEffect(player, MobEffects.REGENERATION, regenerationLevel);
	}

	@SubscribeEvent
	public void onLivingHurt(LivingHurtEvent event) {
		if (event.getAmount() <= 0.0F) {
			return;
		}

		if (event.getEntity() instanceof ServerPlayer dodgingPlayer
				&& SkillData.isDodgeActive(dodgingPlayer, dodgingPlayer.level().getGameTime())) {
			event.setAmount(0.0F);
			return;
		}

		ServerPlayer attackingPlayer = resolvePlayerAttacker(event.getSource());
		if (attackingPlayer != null
				&& attackingPlayer != event.getEntity()
				&& SkillData.consumePendingCrit(attackingPlayer)) {
			event.setAmount(event.getAmount() * 1.5F);
			attackingPlayer.level().playSound(null, attackingPlayer.getX(), attackingPlayer.getY(), attackingPlayer.getZ(),
					SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 0.85F, 1.0F);
			if (event.getEntity().level() instanceof ServerLevel serverLevel) {
				serverLevel.sendParticles(ParticleTypes.CRIT, event.getEntity().getX(), event.getEntity().getY(0.5D),
						event.getEntity().getZ(), 12, 0.25D, 0.35D, 0.25D, 0.05D);
			}
			syncPlayerData(attackingPlayer);
		}

		if (attackingPlayer != null && attackingPlayer != event.getEntity()) {
			LivingEntity target = event.getEntity();
			if (SkillData.hasSkill(attackingPlayer, SkillType.BATTLE_MOMENTUM)) {
				attackingPlayer.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, BATTLE_MOMENTUM_TICKS, 0,
						false, false, false));
			}

			if (SkillData.hasSkill(attackingPlayer, SkillType.EXECUTIONER)
					&& target.getMaxHealth() > 0.0F
					&& target.getHealth() / target.getMaxHealth() <= EXECUTIONER_HEALTH_THRESHOLD) {
				event.setAmount(event.getAmount() * EXECUTIONER_DAMAGE_MULTIPLIER);
			}
		}

		if (!(event.getEntity() instanceof ServerPlayer player)) {
			return;
		}

		long gameTime = player.level().getGameTime();
		if (SkillData.hasSkill(player, SkillType.NO_KNOCKBACK)) {
			SkillData.setPendingHurtMotion(player, gameTime, player.getDeltaMovement());
		}

		if (SkillData.hasSkill(player, SkillType.DOT_IMMUNITY)) {
			float adaptedDamage = SkillData.adaptRepeatedDamage(player, buildDamageSignature(event.getSource()),
					event.getAmount(), gameTime);
			if (adaptedDamage <= 0.0F) {
				event.setAmount(0.0F);
				return;
			}

			if (adaptedDamage < event.getAmount()) {
				event.setAmount(adaptedDamage);
			}
		}

		if (SkillData.hasSkill(player, SkillType.HALF_DAMAGE)) {
			event.setAmount(event.getAmount() * 0.5F);
		}

		boolean syncRequired = false;
		if (SkillData.consumeCooldownIfReady(player, SkillType.FIRST_HIT_GUARD, ONE_MINUTE_TICKS, gameTime)) {
			syncPlayerData(player);
			event.setAmount(0.0F);
			return;
		}

		LivingEntity attacker = resolveAttacker(event.getSource());
		if (attacker != null
				&& SkillData.consumeCooldownIfReady(player, SkillType.REPULSION_BURST, THIRTY_SECONDS_TICKS, gameTime)) {
			attacker.knockback(1.5D, player.getX() - attacker.getX(), player.getZ() - attacker.getZ());
			syncRequired = true;
		}

		if (event.getAmount() > 10.0F
				&& SkillData.consumeCooldownIfReady(player, SkillType.EMERGENCY_RESTORE, ONE_MINUTE_TICKS, gameTime)) {
			SkillData.setPendingFullHeal(player, true);
			syncRequired = true;
			float safeDamage = Math.max(0.0F, player.getHealth() - 1.0F);
			if (event.getAmount() > safeDamage) {
				event.setAmount(safeDamage);
			}
		}

		if (event.getAmount() >= player.getHealth()
				&& SkillData.consumeCooldownIfReady(player, SkillType.SECOND_WIND, FIVE_MINUTES_TICKS, gameTime)) {
			SkillData.setPendingFullHeal(player, true);
			SkillData.setPendingSecondWindBuff(player, true);
			syncRequired = true;
			float safeDamage = Math.max(0.0F, player.getHealth() - 1.0F);
			event.setAmount(safeDamage);
		}

		if (event.getAmount() > 0.0F
				&& SkillData.hasSkill(player, SkillType.RETALIATION_CRIT)
				&& !SkillData.hasPendingCrit(player)
				&& player.getRandom().nextFloat() < 0.5F) {
			SkillData.setPendingCrit(player, true);
			syncRequired = true;
		}

		if (syncRequired) {
			syncPlayerData(player);
		}
	}

	@SubscribeEvent
	public void onLivingKnockBack(LivingKnockBackEvent event) {
		if (event.getEntity() instanceof ServerPlayer player && SkillData.hasSkill(player, SkillType.NO_KNOCKBACK)) {
			event.setCanceled(true);
		}
	}

	@SubscribeEvent
	public void onLivingDamage(LivingDamageEvent event) {
		if (!(event.getEntity() instanceof ServerPlayer player) || event.getAmount() <= 0.0F) {
			return;
		}

		float actualDamageTaken = Math.min(event.getAmount(), player.getHealth());
		boolean unlockedSkillTree = SkillData.addReceivedDamage(player, actualDamageTaken, player.level().getGameTime(),
				buildDamageFatigueSignature(event.getSource()), buildDamageFatigueLabel(event.getSource()));
		if (unlockedSkillTree) {
			player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.TOTEM_USE,
					SoundSource.PLAYERS, 1.0F, 1.0F);
		}

		syncPlayerData(player);
	}

	@SubscribeEvent
	public void onPlayerClone(PlayerEvent.Clone event) {
		if (event.getOriginal() instanceof ServerPlayer oldPlayer) {
			SpecialSkillFeature.cancelPending(oldPlayer);
		}
		SkillData.copyTo(event.getOriginal(), event.getEntity(), event.isWasDeath());
		if (event.getEntity() instanceof ServerPlayer newPlayer) {
			SpecialSkillFeature.cancelPending(newPlayer);
		}
	}

	@SubscribeEvent
	public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
		if (event.getEntity() instanceof ServerPlayer player) {
			syncPlayerData(player);
		}
	}

	@SubscribeEvent
	public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
		if (event.getEntity() instanceof ServerPlayer player) {
			syncPlayerData(player);
		}
	}

	@SubscribeEvent
	public void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
		if (event.getEntity() instanceof ServerPlayer player) {
			syncPlayerData(player);
		}
	}

	@SubscribeEvent
	public void onRegisterCommands(RegisterCommandsEvent event) {
		CombatFlexCommands.register(event.getDispatcher());
	}

	public static void syncPlayerData(ServerPlayer player) {
		NetworkHandler.sendToPlayer(player, new SyncSkillDataPacket(SkillData.createSyncTag(player)));
	}

	private static void syncEffect(Player player, MobEffect effect, int level) {
		MobEffectInstance currentEffect = player.getEffect(effect);

		if (level <= 0) {
			if (isManagedEffect(currentEffect)) {
				player.removeEffect(effect);
			}
			return;
		}

		int amplifier = level - 1;
		if (currentEffect != null) {
			if (currentEffect.getAmplifier() == amplifier
					&& currentEffect.getDuration() > REFRESH_THRESHOLD_TICKS) {
				return;
			}

			if (currentEffect.getAmplifier() > amplifier && !isManagedEffect(currentEffect)) {
				return;
			}
		}

		player.addEffect(new MobEffectInstance(effect, EFFECT_DURATION_TICKS, amplifier, false, false, false));
	}

	private static boolean isManagedEffect(MobEffectInstance effectInstance) {
		return effectInstance != null
				&& !effectInstance.isAmbient()
				&& !effectInstance.isVisible()
				&& !effectInstance.showIcon()
				&& effectInstance.getDuration() <= EFFECT_DURATION_TICKS;
	}

	private static LivingEntity resolveAttacker(DamageSource damageSource) {
		Entity directEntity = damageSource.getDirectEntity();
		if (directEntity instanceof LivingEntity livingEntity) {
			return livingEntity;
		}

		Entity sourceEntity = damageSource.getEntity();
		if (sourceEntity instanceof LivingEntity livingEntity) {
			return livingEntity;
		}

		return null;
	}

	private static ServerPlayer resolvePlayerAttacker(DamageSource damageSource) {
		if (damageSource.getEntity() instanceof ServerPlayer player) {
			return player;
		}

		return null;
	}

	private static String buildDamageSignature(DamageSource damageSource) {
		StringBuilder signature = new StringBuilder(damageSource.getMsgId());
		Entity directEntity = damageSource.getDirectEntity();
		Entity attacker = damageSource.getEntity() != null ? damageSource.getEntity() : damageSource.getDirectEntity();
		if (directEntity != null) {
			signature.append("|direct=").append(BuiltInRegistries.ENTITY_TYPE.getKey(directEntity.getType()));
		} else {
			signature.append("|direct=none");
		}

		if (attacker != null) {
			signature.append("|attacker=").append(BuiltInRegistries.ENTITY_TYPE.getKey(attacker.getType()));
			signature.append("|tool=").append(resolveToolKind(attacker));
		} else {
			signature.append("|attacker=environment");
			signature.append("|tool=none");
		}

		return signature.toString();
	}

	private static String buildDamageFatigueSignature(DamageSource damageSource) {
		Entity fatigueEntity = damageSource.getEntity() != null ? damageSource.getEntity() : damageSource.getDirectEntity();
		if (fatigueEntity != null) {
			return "entity=" + fatigueEntity.getUUID();
		}

		return "environment=" + damageSource.getMsgId();
	}

	private static String buildDamageFatigueLabel(DamageSource damageSource) {
		Entity fatigueEntity = damageSource.getEntity() != null ? damageSource.getEntity() : damageSource.getDirectEntity();
		if (fatigueEntity != null) {
			return fatigueEntity.getDisplayName().getString();
		}

		String messageId = damageSource.getMsgId();
		if (messageId == null || messageId.isBlank()) {
			return "Unknown";
		}

		String[] parts = messageId.replace('-', '_').replace('.', '_').split("_");
		StringBuilder builder = new StringBuilder();
		for (String part : parts) {
			if (part.isBlank()) {
				continue;
			}

			if (!builder.isEmpty()) {
				builder.append(' ');
			}
			builder.append(Character.toUpperCase(part.charAt(0)));
			if (part.length() > 1) {
				builder.append(part.substring(1));
			}
		}
		return builder.isEmpty() ? "Unknown" : builder.toString();
	}

	private static String resolveToolKind(Entity attacker) {
		if (!(attacker instanceof LivingEntity livingEntity)) {
			return "none";
		}

		ItemStack mainHandItem = livingEntity.getMainHandItem();
		ItemStack offHandItem = livingEntity.getOffhandItem();
		ItemStack activeItem = livingEntity.getUseItem();
		ItemStack selectedItem = !activeItem.isEmpty() ? activeItem : mainHandItem;

		if (selectedItem.isEmpty() && livingEntity.getMainArm() == HumanoidArm.LEFT) {
			selectedItem = offHandItem;
		}

		if (selectedItem.isEmpty()) {
			return "unarmed";
		}

		if (selectedItem.getItem() instanceof SwordItem) {
			return "sword";
		}

		if (selectedItem.getItem() instanceof AxeItem) {
			return "axe";
		}

		if (selectedItem.getItem() instanceof PickaxeItem) {
			return "pickaxe";
		}

		if (selectedItem.getItem() instanceof ShovelItem) {
			return "shovel";
		}

		if (selectedItem.getItem() instanceof HoeItem) {
			return "hoe";
		}

		if (selectedItem.getItem() instanceof BowItem) {
			return "bow";
		}

		if (selectedItem.getItem() instanceof CrossbowItem) {
			return "crossbow";
		}

		if (selectedItem.getItem() instanceof TridentItem) {
			return "trident";
		}

		if (selectedItem.getItem() instanceof ShieldItem) {
			return "shield";
		}

		if (selectedItem.getItem() instanceof ShearsItem) {
			return "shears";
		}

		return BuiltInRegistries.ITEM.getKey(selectedItem.getItem()).toString();
	}
}
