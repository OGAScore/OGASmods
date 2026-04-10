package com.OGAS.combatflex;

import java.util.ArrayList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class SkillData {
	private static final String ROOT_KEY = "cbtflex_skill_data";
	private static final String POINTS_KEY = "points";
	private static final String DAMAGE_PROGRESS_KEY = "damage_progress";
	private static final String TOTAL_DAMAGE_KEY = "total_damage";
	private static final String LOW_HEALTH_SURVIVAL_TICKS_KEY = "low_health_survival_ticks";
	private static final String SKILLS_KEY = "skills";
	private static final String COOLDOWNS_KEY = "cooldowns";
	private static final String DAMAGE_ADAPTATION_KEY = "damage_adaptation";
	private static final String PENDING_FULL_HEAL_KEY = "pending_full_heal";
	private static final String PENDING_SECOND_WIND_BUFF_KEY = "pending_second_wind_buff";
	private static final String PENDING_CRIT_KEY = "pending_crit";
	private static final String PENDING_MOTION_X_KEY = "pending_motion_x";
	private static final String PENDING_MOTION_Y_KEY = "pending_motion_y";
	private static final String PENDING_MOTION_Z_KEY = "pending_motion_z";
	private static final String PENDING_MOTION_TICK_KEY = "pending_motion_tick";
	private static final String ADAPT_COUNT_KEY = "count";
	private static final String ADAPT_LAST_TICK_KEY = "last_tick";
	private static final float DAMAGE_PER_POINT = 100.0F;
	private static final float SKILL_TREE_UNLOCK_DAMAGE = 200.0F;
	private static final float MAX_REASONABLE_TOTAL_DAMAGE = 1_000_000.0F;
	private static final float LOW_DPS_IMMUNITY_THRESHOLD = 2.0F;
	private static final int LOW_HEALTH_POINT_TICKS = 20 * 60;
	private static final long ADAPTATION_DECAY_DELAY_TICKS = 20L * 60L;
	private static final long ADAPTATION_DECAY_DURATION_TICKS = 20L * 60L;

	private SkillData() {
	}

	public static int getPoints(Player player) {
		return getRoot(player).getInt(POINTS_KEY);
	}

	public static float getDamageProgress(Player player) {
		return getRoot(player).getFloat(DAMAGE_PROGRESS_KEY);
	}

	public static float getTotalReceivedDamage(Player player) {
		return getRoot(player).getFloat(TOTAL_DAMAGE_KEY);
	}

	public static boolean canOpenSkillTree(Player player) {
		return getTotalReceivedDamage(player) >= SKILL_TREE_UNLOCK_DAMAGE;
	}

	public static float getDamagePerPointThreshold() {
		return DAMAGE_PER_POINT;
	}

	public static float getSkillTreeUnlockDamageThreshold() {
		return SKILL_TREE_UNLOCK_DAMAGE;
	}

	public static boolean hasSkill(Player player, SkillType skillType) {
		return getSkillsTag(player).getBoolean(skillType.id());
	}

	public static int getUnlockedSkillCountByCost(Player player, int cost) {
		int unlockedCount = 0;
		for (SkillType skillType : SkillType.values()) {
			if (skillType.cost() == cost && hasSkill(player, skillType)) {
				unlockedCount++;
			}
		}
		return unlockedCount;
	}

	public static int getUnlockedSkillCountByTier(Player player, SkillType.SkillBranch branch, int tier) {
		int unlockedCount = 0;
		for (SkillType skillType : SkillType.values()) {
			if (skillType.branch() == branch && skillType.tier() == tier && hasSkill(player, skillType)) {
				unlockedCount++;
			}
		}
		return unlockedCount;
	}

	public static int getUnlockedSkillCount(Player player) {
		int unlockedCount = 0;
		for (SkillType skillType : SkillType.values()) {
			if (hasSkill(player, skillType)) {
				unlockedCount++;
			}
		}
		return unlockedCount;
	}

	public static boolean meetsUnlockRequirements(Player player, SkillType skillType) {
		if (skillType.branch() == SkillType.SkillBranch.FLIGHT) {
			if (skillType.tier() == 2) {
				return true;
			}

			if (skillType.tier() >= 3) {
				return hasUnlockedAllOtherSkillsInBranch(player, skillType);
			}

			return true;
		}

		if (skillType.tier() == 2) {
			return getUnlockedSkillCountByTier(player, SkillType.SkillBranch.COMBAT, 1) >= 2;
		}

		if (skillType.tier() >= 3) {
			return hasUnlockedAllOtherSkillsInBranch(player, skillType);
		}

		return true;
	}

	public static boolean unlockSkill(Player player, SkillType skillType) {
		if (hasSkill(player, skillType)
				|| getPoints(player) < skillType.cost()
				|| !meetsUnlockRequirements(player, skillType)) {
			return false;
		}

		CompoundTag root = getRoot(player);
		root.putInt(POINTS_KEY, root.getInt(POINTS_KEY) - skillType.cost());
		getSkillsTag(root).putBoolean(skillType.id(), true);
		return true;
	}

	public static boolean addReceivedDamage(Player player, float amount) {
		float sanitizedAmount = sanitizeIncomingDamage(amount);
		if (sanitizedAmount <= 0.0F) {
			return false;
		}

		CompoundTag root = getRoot(player);
		float previousTotalDamage = root.getFloat(TOTAL_DAMAGE_KEY);
		float newTotalDamage = clampRecordedDamage(previousTotalDamage + sanitizedAmount);
		root.putFloat(TOTAL_DAMAGE_KEY, newTotalDamage);

		float progress = sanitizeProgress(root.getFloat(DAMAGE_PROGRESS_KEY)) + sanitizedAmount;
		int gainedPoints = (int) (progress / DAMAGE_PER_POINT);
		if (gainedPoints > 0) {
			long updatedPoints = (long) root.getInt(POINTS_KEY) + gainedPoints;
			root.putInt(POINTS_KEY, clampPoints(updatedPoints));
			progress -= DAMAGE_PER_POINT * gainedPoints;
		}

		root.putFloat(DAMAGE_PROGRESS_KEY, sanitizeProgress(progress));
		return previousTotalDamage < SKILL_TREE_UNLOCK_DAMAGE && newTotalDamage >= SKILL_TREE_UNLOCK_DAMAGE;
	}

	public static boolean tickLowHealthSurvival(Player player) {
		CompoundTag root = getRoot(player);
		if (player.isDeadOrDying() || player.getHealth() > 1.0F) {
			root.putInt(LOW_HEALTH_SURVIVAL_TICKS_KEY, 0);
			return false;
		}

		int survivedTicks = Math.max(0, root.getInt(LOW_HEALTH_SURVIVAL_TICKS_KEY)) + 1;
		if (survivedTicks < LOW_HEALTH_POINT_TICKS) {
			root.putInt(LOW_HEALTH_SURVIVAL_TICKS_KEY, survivedTicks);
			return false;
		}

		root.putInt(LOW_HEALTH_SURVIVAL_TICKS_KEY, survivedTicks - LOW_HEALTH_POINT_TICKS);
		root.putInt(POINTS_KEY, clampPoints((long) root.getInt(POINTS_KEY) + 1L));
		return true;
	}

	public static void resetLifeProgress(Player player) {
		CompoundTag root = getRoot(player);
		root.putFloat(DAMAGE_PROGRESS_KEY, 0.0F);
		root.putInt(LOW_HEALTH_SURVIVAL_TICKS_KEY, 0);
		root.putBoolean(PENDING_CRIT_KEY, false);
	}

	public static boolean consumeCooldownIfReady(Player player, SkillType skillType, long cooldownTicks, long gameTime) {
		if (!hasSkill(player, skillType) || gameTime < getCooldownEnd(player, skillType)) {
			return false;
		}

		getCooldownsTag(player).putLong(skillType.id(), gameTime + cooldownTicks);
		return true;
	}

	public static boolean isPendingFullHeal(Player player) {
		return getRoot(player).getBoolean(PENDING_FULL_HEAL_KEY);
	}

	public static void setPendingFullHeal(Player player, boolean pendingFullHeal) {
		getRoot(player).putBoolean(PENDING_FULL_HEAL_KEY, pendingFullHeal);
	}

	public static boolean isPendingSecondWindBuff(Player player) {
		return getRoot(player).getBoolean(PENDING_SECOND_WIND_BUFF_KEY);
	}

	public static void setPendingSecondWindBuff(Player player, boolean pendingSecondWindBuff) {
		getRoot(player).putBoolean(PENDING_SECOND_WIND_BUFF_KEY, pendingSecondWindBuff);
	}

	public static boolean hasPendingCrit(Player player) {
		return getRoot(player).getBoolean(PENDING_CRIT_KEY);
	}

	public static void setPendingCrit(Player player, boolean pendingCrit) {
		getRoot(player).putBoolean(PENDING_CRIT_KEY, pendingCrit);
	}

	public static boolean consumePendingCrit(Player player) {
		if (!hasPendingCrit(player)) {
			return false;
		}

		setPendingCrit(player, false);
		return true;
	}

	public static void setPendingHurtMotion(Player player, long gameTime, Vec3 motion) {
		CompoundTag root = getRoot(player);
		root.putDouble(PENDING_MOTION_X_KEY, motion.x);
		root.putDouble(PENDING_MOTION_Y_KEY, motion.y);
		root.putDouble(PENDING_MOTION_Z_KEY, motion.z);
		root.putLong(PENDING_MOTION_TICK_KEY, gameTime);
	}

	public static boolean applyPendingHurtMotion(Player player, long gameTime) {
		CompoundTag root = getRoot(player);
		long pendingTick = root.getLong(PENDING_MOTION_TICK_KEY);
		if (pendingTick <= 0L || pendingTick > gameTime) {
			return false;
		}

		player.setDeltaMovement(root.getDouble(PENDING_MOTION_X_KEY), root.getDouble(PENDING_MOTION_Y_KEY),
				root.getDouble(PENDING_MOTION_Z_KEY));
		root.remove(PENDING_MOTION_X_KEY);
		root.remove(PENDING_MOTION_Y_KEY);
		root.remove(PENDING_MOTION_Z_KEY);
		root.remove(PENDING_MOTION_TICK_KEY);
		return true;
	}

	public static CompoundTag createSyncTag(Player player) {
		return getRoot(player).copy();
	}

	public static void applySyncTag(Player player, CompoundTag data) {
		player.getPersistentData().put(ROOT_KEY, data.copy());
	}

	public static void copyTo(Player source, Player target) {
		applySyncTag(target, createSyncTag(source));
	}

	public static void copyTo(Player source, Player target, boolean resetLifeProgress) {
		copyTo(source, target);
		if (resetLifeProgress) {
			resetLifeProgress(target);
		}
	}

	public static long getCooldownEnd(Player player, SkillType skillType) {
		return getCooldownsTag(player).getLong(skillType.id());
	}

	public static long getRemainingCooldownTicks(Player player, SkillType skillType, long gameTime) {
		return Math.max(0L, getCooldownEnd(player, skillType) - gameTime);
	}

	public static float adaptRepeatedDamage(Player player, String signature, float amount, long gameTime) {
		CompoundTag adaptationTag = getAdaptationTag(player);
		String key = buildAdaptationKey(signature, amount);
		pruneAdaptationEntries(adaptationTag, gameTime);
		CompoundTag entry = adaptationTag.contains(key, Tag.TAG_COMPOUND) ? adaptationTag.getCompound(key) : new CompoundTag();
		long lastMatchTick = entry.getLong(ADAPT_LAST_TICK_KEY);
		float effectiveCount = getEffectiveAdaptationCount(entry, gameTime);
		long deltaTicks = lastMatchTick > 0L ? Math.max(1L, gameTime - lastMatchTick) : Long.MAX_VALUE;
		float nextCount = effectiveCount + 1.0F;

		entry.putFloat(ADAPT_COUNT_KEY, nextCount);
		entry.putLong(ADAPT_LAST_TICK_KEY, gameTime);
		adaptationTag.put(key, entry);

		if (lastMatchTick > 0L && amount * 20.0F / deltaTicks < LOW_DPS_IMMUNITY_THRESHOLD) {
			return 0.0F;
		}

		if (nextCount >= 10.0F) {
			return 0.0F;
		}

		if (nextCount <= 3.0F) {
			return amount;
		}

		float multiplier = Math.max(0.0F, (10.0F - nextCount) / 7.0F);
		return amount * multiplier;
	}

	private static boolean hasUnlockedAllOtherSkillsInBranch(Player player, SkillType targetSkill) {
		for (SkillType skillType : SkillType.values()) {
			if (skillType == targetSkill || skillType.branch() != targetSkill.branch()) {
				continue;
			}

			if (!hasSkill(player, skillType)) {
				return false;
			}
		}

		return true;
	}

	private static CompoundTag getRoot(Player player) {
		CompoundTag persistentData = player.getPersistentData();
		if (!persistentData.contains(ROOT_KEY, Tag.TAG_COMPOUND)) {
			persistentData.put(ROOT_KEY, new CompoundTag());
		}

		CompoundTag root = persistentData.getCompound(ROOT_KEY);
		ensureChildren(root);
		migrateLegacySkillIds(root);
		sanitizeStoredValues(root);
		return root;
	}

	private static CompoundTag getSkillsTag(Player player) {
		return getSkillsTag(getRoot(player));
	}

	private static CompoundTag getSkillsTag(CompoundTag root) {
		ensureChildren(root);
		return root.getCompound(SKILLS_KEY);
	}

	private static CompoundTag getCooldownsTag(Player player) {
		CompoundTag root = getRoot(player);
		ensureChildren(root);
		return root.getCompound(COOLDOWNS_KEY);
	}

	private static CompoundTag getAdaptationTag(Player player) {
		CompoundTag root = getRoot(player);
		ensureChildren(root);
		return root.getCompound(DAMAGE_ADAPTATION_KEY);
	}

	private static float getEffectiveAdaptationCount(CompoundTag entry, long gameTime) {
		float storedCount = entry.getFloat(ADAPT_COUNT_KEY);
		long lastDamageTick = entry.getLong(ADAPT_LAST_TICK_KEY);
		if (storedCount <= 0.0F || lastDamageTick <= 0L) {
			return 0.0F;
		}

		long idleTicks = Math.max(0L, gameTime - lastDamageTick);
		if (idleTicks <= ADAPTATION_DECAY_DELAY_TICKS) {
			return storedCount;
		}

		long decayTicks = idleTicks - ADAPTATION_DECAY_DELAY_TICKS;
		if (decayTicks >= ADAPTATION_DECAY_DURATION_TICKS) {
			return 0.0F;
		}

		float retention = 1.0F - (decayTicks / (float) ADAPTATION_DECAY_DURATION_TICKS);
		return storedCount * retention;
	}

	private static void pruneAdaptationEntries(CompoundTag adaptationTag, long gameTime) {
		for (String key : new ArrayList<>(adaptationTag.getAllKeys())) {
			CompoundTag entry = adaptationTag.getCompound(key);
			if (getEffectiveAdaptationCount(entry, gameTime) <= 0.0F) {
				adaptationTag.remove(key);
			}
		}
	}

	private static String buildAdaptationKey(String signature, float amount) {
		return signature + "|damage=" + normalizeDamageAmount(amount);
	}

	private static void ensureChildren(CompoundTag root) {
		if (!root.contains(SKILLS_KEY, Tag.TAG_COMPOUND)) {
			root.put(SKILLS_KEY, new CompoundTag());
		}

		if (!root.contains(COOLDOWNS_KEY, Tag.TAG_COMPOUND)) {
			root.put(COOLDOWNS_KEY, new CompoundTag());
		}

		if (!root.contains(DAMAGE_ADAPTATION_KEY, Tag.TAG_COMPOUND)) {
			root.put(DAMAGE_ADAPTATION_KEY, new CompoundTag());
		}
	}

	private static void migrateLegacySkillIds(CompoundTag root) {
		CompoundTag skillsTag = root.getCompound(SKILLS_KEY);
		CompoundTag cooldownsTag = root.getCompound(COOLDOWNS_KEY);
		for (SkillType skillType : SkillType.values()) {
			for (String legacyId : skillType.legacyIds()) {
				if (skillsTag.contains(legacyId, Tag.TAG_BYTE)) {
					if (skillsTag.getBoolean(legacyId)) {
						skillsTag.putBoolean(skillType.id(), true);
					}
					skillsTag.remove(legacyId);
				}

				if (cooldownsTag.contains(legacyId, Tag.TAG_LONG)) {
					long migratedCooldown = Math.max(cooldownsTag.getLong(skillType.id()), cooldownsTag.getLong(legacyId));
					cooldownsTag.putLong(skillType.id(), migratedCooldown);
					cooldownsTag.remove(legacyId);
				}
			}
		}
	}

	private static float normalizeDamageAmount(float amount) {
		return Math.round(amount * 1000.0F) / 1000.0F;
	}

	private static void sanitizeStoredValues(CompoundTag root) {
		root.putInt(POINTS_KEY, clampPoints(root.getInt(POINTS_KEY)));
		root.putFloat(DAMAGE_PROGRESS_KEY, sanitizeProgress(root.getFloat(DAMAGE_PROGRESS_KEY)));
		root.putFloat(TOTAL_DAMAGE_KEY, clampRecordedDamage(root.getFloat(TOTAL_DAMAGE_KEY)));
		root.putInt(LOW_HEALTH_SURVIVAL_TICKS_KEY,
				sanitizeLowHealthSurvivalTicks(root.getInt(LOW_HEALTH_SURVIVAL_TICKS_KEY)));
	}

	private static int clampPoints(long points) {
		if (points <= 0L) {
			return 0;
		}

		return points >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) points;
	}

	private static float sanitizeIncomingDamage(float amount) {
		if (!Float.isFinite(amount) || amount <= 0.0F) {
			return 0.0F;
		}

		return Math.min(amount, MAX_REASONABLE_TOTAL_DAMAGE);
	}

	private static float sanitizeProgress(float progress) {
		if (!Float.isFinite(progress) || progress <= 0.0F) {
			return 0.0F;
		}

		return progress % DAMAGE_PER_POINT;
	}

	private static float clampRecordedDamage(float amount) {
		if (!Float.isFinite(amount) || amount <= 0.0F) {
			return 0.0F;
		}

		return Math.min(amount, MAX_REASONABLE_TOTAL_DAMAGE);
	}

	private static int sanitizeLowHealthSurvivalTicks(int ticks) {
		if (ticks <= 0) {
			return 0;
		}

		return ticks % LOW_HEALTH_POINT_TICKS;
	}
}