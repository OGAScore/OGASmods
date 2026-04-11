package com.OGAS.combatflex;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class SkillData {
	private static final String ROOT_KEY = "cbtflex_skill_data";
	private static final String POINTS_KEY = "points";
	private static final String TOTAL_POINTS_EARNED_KEY = "total_points_earned";
	private static final String DAMAGE_PROGRESS_KEY = "damage_progress";
	private static final String TOTAL_DAMAGE_KEY = "total_damage";
	private static final String SKILL_TREE_UNLOCKED_KEY = "skill_tree_unlocked";
	private static final String PASSIVE_SURGE_UNTIL_TICK_KEY = "passive_surge_until_tick";
	private static final String DAMAGE_FATIGUE_KEY = "damage_fatigue";
	private static final String LOW_HEALTH_SURVIVAL_TICKS_KEY = "low_health_survival_ticks";
	private static final String SKILLS_KEY = "skills";
	private static final String SPECIAL_SKILLS_KEY = "special_skills";
	private static final String COOLDOWNS_KEY = "cooldowns";
	private static final String SPECIAL_COOLDOWNS_KEY = "special_cooldowns";
	private static final String DAMAGE_ADAPTATION_KEY = "damage_adaptation";
	private static final String DODGE_UNTIL_TICK_KEY = "dodge_until_tick";
	private static final String PENDING_FULL_HEAL_KEY = "pending_full_heal";
	private static final String PENDING_SECOND_WIND_BUFF_KEY = "pending_second_wind_buff";
	private static final String PENDING_CRIT_KEY = "pending_crit";
	private static final String PENDING_MOTION_X_KEY = "pending_motion_x";
	private static final String PENDING_MOTION_Y_KEY = "pending_motion_y";
	private static final String PENDING_MOTION_Z_KEY = "pending_motion_z";
	private static final String PENDING_MOTION_TICK_KEY = "pending_motion_tick";
	private static final String ADAPT_COUNT_KEY = "count";
	private static final String ADAPT_LAST_TICK_KEY = "last_tick";
	private static final String DAMAGE_FATIGUE_LABEL_KEY = "label";
	private static final float DAMAGE_PER_POINT = 100.0F;
	private static final float SKILL_TREE_UNLOCK_DAMAGE = 200.0F;
	private static final int PASSIVE_SURGE_MAX_LEVEL = 8;
	private static final float MAX_REASONABLE_TOTAL_DAMAGE = 1_000_000.0F;
	private static final float MAX_DAMAGE_FATIGUE_DAMAGE = 25.0F;
	private static final long DAMAGE_FATIGUE_DISPLAY_WINDOW_TICKS = 20L * 10L;
	private static final float LOW_DPS_IMMUNITY_THRESHOLD = 2.0F;
	private static final int LOW_HEALTH_POINT_TICKS = 20 * 60;
	private static final long ADAPTATION_DECAY_DELAY_TICKS = 20L * 60L;
	private static final long ADAPTATION_DECAY_DURATION_TICKS = 20L * 60L;
	private static final long DAMAGE_FATIGUE_DECAY_DELAY_TICKS = 20L * 6L;
	private static final long DAMAGE_FATIGUE_DECAY_FIRST_PHASE_TICKS = 20L * 18L;
	private static final long DAMAGE_FATIGUE_DECAY_SECOND_PHASE_TICKS = 20L * 36L;

	private SkillData() {
	}

	public static int getPoints(Player player) {
		return getRoot(player).getInt(POINTS_KEY);
	}

	public static int getTotalPointsEarned(Player player) {
		return getRoot(player).getInt(TOTAL_POINTS_EARNED_KEY);
	}

	public static float getDamageProgress(Player player) {
		return getRoot(player).getFloat(DAMAGE_PROGRESS_KEY);
	}

	public static float getTotalReceivedDamage(Player player) {
		return getSkillTreeUnlockProgress(player);
	}

	public static boolean isSkillTreeUnlocked(Player player) {
		return getRoot(player).getBoolean(SKILL_TREE_UNLOCKED_KEY);
	}

	public static float getSkillTreeUnlockProgress(Player player) {
		return getRoot(player).getFloat(TOTAL_DAMAGE_KEY);
	}

	public static boolean canOpenSkillTree(Player player) {
		return isSkillTreeUnlocked(player);
	}

	public static int getPassiveSurgeLevel(Player player) {
		if (!isSkillTreeUnlocked(player)) {
			return 0;
		}

		return Math.min(PASSIVE_SURGE_MAX_LEVEL, Math.max(0, getTotalPointsEarned(player) / 3));
	}

	public static void activatePassiveSurge(Player player, long gameTime, long durationTicks) {
		getRoot(player).putLong(PASSIVE_SURGE_UNTIL_TICK_KEY, gameTime + durationTicks);
	}

	public static boolean hasActivePassiveSurge(Player player, long gameTime) {
		return isSkillTreeUnlocked(player) && getRoot(player).getLong(PASSIVE_SURGE_UNTIL_TICK_KEY) > gameTime;
	}

	public static long getRemainingPassiveSurgeTicks(Player player, long gameTime) {
		return Math.max(0L, getRoot(player).getLong(PASSIVE_SURGE_UNTIL_TICK_KEY) - gameTime);
	}

	public static float getDamagePerPointThreshold() {
		return DAMAGE_PER_POINT;
	}

	public static float getSkillTreeUnlockDamageThreshold() {
		return SKILL_TREE_UNLOCK_DAMAGE;
	}

	public static float getDamageGainEfficiency(Player player) {
		CompoundTag root = getRoot(player);
		long gameTime = player.level() == null ? 0L : player.level().getGameTime();
		return getLowestDamageGainMultiplier(root, gameTime);
	}

	public static float getDamageFatiguePercent(Player player) {
		return (1.0F - getDamageGainEfficiency(player)) * 100.0F;
	}

	public static boolean hasActiveDamageFatigue(Player player) {
		return getDamageFatiguePercent(player) >= 0.5F;
	}

	public static List<DamageFatigueEntry> getRecentDamageFatigueEntries(Player player) {
		long gameTime = player.level() == null ? 0L : player.level().getGameTime();
		CompoundTag fatigueTag = getDamageFatigueTag(getRoot(player));
		pruneDamageFatigueEntries(fatigueTag, gameTime);
		List<DamageFatigueEntry> entries = new ArrayList<>();
		for (String key : fatigueTag.getAllKeys()) {
			CompoundTag entry = fatigueTag.getCompound(key);
			long lastDamageTick = entry.getLong(ADAPT_LAST_TICK_KEY);
			if (gameTime - lastDamageTick > DAMAGE_FATIGUE_DISPLAY_WINDOW_TICKS) {
				continue;
			}

			float fatiguePercent = computeDamageFatiguePercent(getEffectiveDamageFatigueDamage(entry, gameTime));
			if (fatiguePercent < 0.5F) {
				continue;
			}

			entries.add(new DamageFatigueEntry(sanitizeDamageFatigueLabel(entry.getString(DAMAGE_FATIGUE_LABEL_KEY)),
					fatiguePercent, lastDamageTick));
		}

		entries.sort(Comparator.comparingLong(DamageFatigueEntry::lastDamageTick).reversed());
		if (entries.size() > 5) {
			entries = new ArrayList<>(entries.subList(0, 5));
		}
		entries.sort(Comparator.comparingLong(DamageFatigueEntry::lastDamageTick));
		return entries;
	}

	public static boolean hasSkill(Player player, SkillType skillType) {
		return getSkillsTag(player).getBoolean(skillType.id());
	}

	public static boolean hasSpecialSkill(Player player, SpecialSkillType skillType) {
		return getSpecialSkillsTag(player).getBoolean(skillType.id());
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

	public static int getUnlockedSpecialSkillCount(Player player) {
		int unlockedCount = 0;
		for (SpecialSkillType skillType : SpecialSkillType.values()) {
			if (hasSpecialSkill(player, skillType)) {
				unlockedCount++;
			}
		}
		return unlockedCount;
	}

	public static boolean meetsUnlockRequirements(Player player, SkillType skillType) {
		if (!isSkillTreeUnlocked(player)) {
			return false;
		}

		if (skillType.branch() == SkillType.SkillBranch.ACTIVE) {
			if (skillType.tier() <= 1) {
				return true;
			}

			return getUnlockedSkillCountByTier(player, SkillType.SkillBranch.ACTIVE, skillType.tier() - 1) >= 1;
		}

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
			return getUnlockedSkillCountByTier(player, skillType.branch(), 1) >= 2;
		}

		if (skillType.tier() >= 3) {
			return hasUnlockedAllOtherSkillsInBranch(player, skillType);
		}

		return true;
	}

	public static boolean meetsSpecialUnlockRequirements(Player player, SpecialSkillType skillType) {
		return isSkillTreeUnlocked(player)
				&& getUnlockedSkillCountByTier(player, SkillType.SkillBranch.ACTIVE, skillType.tier()) >= 1;
	}

	public static boolean unlockSkillTree(Player player) {
		CompoundTag root = getRoot(player);
		if (root.getBoolean(SKILL_TREE_UNLOCKED_KEY)) {
			return false;
		}

		root.putBoolean(SKILL_TREE_UNLOCKED_KEY, true);
		root.putFloat(TOTAL_DAMAGE_KEY, SKILL_TREE_UNLOCK_DAMAGE);
		root.putFloat(DAMAGE_PROGRESS_KEY, 0.0F);
		root.put(DAMAGE_FATIGUE_KEY, new CompoundTag());
		grantPoints(root, 1);
		return true;
	}

	public static boolean forceUnlockSkillTree(Player player) {
		CompoundTag root = getRoot(player);
		boolean changed = !root.getBoolean(SKILL_TREE_UNLOCKED_KEY)
				|| root.getFloat(TOTAL_DAMAGE_KEY) < SKILL_TREE_UNLOCK_DAMAGE;
		root.putBoolean(SKILL_TREE_UNLOCKED_KEY, true);
		root.putFloat(TOTAL_DAMAGE_KEY, SKILL_TREE_UNLOCK_DAMAGE);
		if (root.getFloat(DAMAGE_PROGRESS_KEY) < 0.0F) {
			root.putFloat(DAMAGE_PROGRESS_KEY, 0.0F);
		}
		return changed;
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

	public static boolean unlockSpecialSkill(Player player, SpecialSkillType skillType) {
		if (hasSpecialSkill(player, skillType)
				|| getPoints(player) < skillType.cost()
				|| !meetsSpecialUnlockRequirements(player, skillType)) {
			return false;
		}

		CompoundTag root = getRoot(player);
		root.putInt(POINTS_KEY, root.getInt(POINTS_KEY) - skillType.cost());
		getSpecialSkillsTag(root).putBoolean(skillType.id(), true);
		return true;
	}

	public static boolean forceUnlockSkill(Player player, SkillType skillType) {
		CompoundTag root = getRoot(player);
		forceUnlockSkillTree(player);
		CompoundTag skillsTag = getSkillsTag(root);
		if (skillsTag.getBoolean(skillType.id())) {
			return false;
		}

		skillsTag.putBoolean(skillType.id(), true);
		return true;
	}

	public static boolean forceUnlockSpecialSkill(Player player, SpecialSkillType skillType) {
		CompoundTag root = getRoot(player);
		forceUnlockSkillTree(player);
		CompoundTag skillsTag = getSpecialSkillsTag(root);
		if (skillsTag.getBoolean(skillType.id())) {
			return false;
		}

		skillsTag.putBoolean(skillType.id(), true);
		return true;
	}

	public static int addPoints(Player player, int amount) {
		if (amount <= 0) {
			return getPoints(player);
		}

		CompoundTag root = getRoot(player);
		grantPoints(root, amount);
		return root.getInt(POINTS_KEY);
	}

	public static void resetSkills(Player player) {
		CompoundTag root = getRoot(player);
		root.put(SKILLS_KEY, new CompoundTag());
		root.put(SPECIAL_SKILLS_KEY, new CompoundTag());
		root.put(COOLDOWNS_KEY, new CompoundTag());
		root.put(SPECIAL_COOLDOWNS_KEY, new CompoundTag());
		root.put(DAMAGE_ADAPTATION_KEY, new CompoundTag());
		root.putLong(DODGE_UNTIL_TICK_KEY, 0L);
		clearPendingState(root);
	}

	public static void resetSpecialSkills(Player player) {
		CompoundTag root = getRoot(player);
		root.put(SPECIAL_SKILLS_KEY, new CompoundTag());
		root.put(SPECIAL_COOLDOWNS_KEY, new CompoundTag());
		root.putLong(DODGE_UNTIL_TICK_KEY, 0L);
	}

	public static void resetProgression(Player player) {
		CompoundTag root = getRoot(player);
		root.putBoolean(SKILL_TREE_UNLOCKED_KEY, false);
		root.putInt(POINTS_KEY, 0);
		root.putInt(TOTAL_POINTS_EARNED_KEY, 0);
		root.putFloat(DAMAGE_PROGRESS_KEY, 0.0F);
		root.putFloat(TOTAL_DAMAGE_KEY, 0.0F);
		root.putLong(PASSIVE_SURGE_UNTIL_TICK_KEY, 0L);
		root.put(DAMAGE_FATIGUE_KEY, new CompoundTag());
		root.putInt(LOW_HEALTH_SURVIVAL_TICKS_KEY, 0);
	}

	public static boolean addReceivedDamage(Player player, float amount, long gameTime, String fatigueSourceKey,
			String fatigueSourceLabel) {
		float sanitizedAmount = sanitizeIncomingDamage(amount);
		if (sanitizedAmount <= 0.0F) {
			return false;
		}

		CompoundTag root = getRoot(player);
		if (!root.getBoolean(SKILL_TREE_UNLOCKED_KEY)) {
			float previousUnlockDamage = root.getFloat(TOTAL_DAMAGE_KEY);
			float newUnlockDamage = clampUnlockProgress(previousUnlockDamage + sanitizedAmount);
			root.putFloat(TOTAL_DAMAGE_KEY, newUnlockDamage);
			if (previousUnlockDamage < SKILL_TREE_UNLOCK_DAMAGE && newUnlockDamage >= SKILL_TREE_UNLOCK_DAMAGE) {
				return unlockSkillTree(player);
			}
			return false;
		}

		float effectiveDamage = applyDamageFatigue(root, fatigueSourceKey, fatigueSourceLabel, sanitizedAmount, gameTime);
		float progress = sanitizeProgress(root.getFloat(DAMAGE_PROGRESS_KEY)) + effectiveDamage;
		int gainedPoints = (int) (progress / DAMAGE_PER_POINT);
		if (gainedPoints > 0) {
			grantPoints(root, gainedPoints);
			progress -= DAMAGE_PER_POINT * gainedPoints;
		}

		root.putFloat(DAMAGE_PROGRESS_KEY, sanitizeProgress(progress));
		return false;
	}

	public static boolean tickLowHealthSurvival(Player player) {
		CompoundTag root = getRoot(player);
		if (!root.getBoolean(SKILL_TREE_UNLOCKED_KEY) || player.isDeadOrDying() || player.getHealth() > 1.0F) {
			root.putInt(LOW_HEALTH_SURVIVAL_TICKS_KEY, 0);
			return false;
		}

		int survivedTicks = Math.max(0, root.getInt(LOW_HEALTH_SURVIVAL_TICKS_KEY)) + 1;
		if (survivedTicks < LOW_HEALTH_POINT_TICKS) {
			root.putInt(LOW_HEALTH_SURVIVAL_TICKS_KEY, survivedTicks);
			return false;
		}

		root.putInt(LOW_HEALTH_SURVIVAL_TICKS_KEY, survivedTicks - LOW_HEALTH_POINT_TICKS);
		grantPoints(root, 1);
		return true;
	}

	public static void resetLifeProgress(Player player) {
		CompoundTag root = getRoot(player);
		root.putFloat(DAMAGE_PROGRESS_KEY, 0.0F);
		root.put(DAMAGE_FATIGUE_KEY, new CompoundTag());
		root.putInt(LOW_HEALTH_SURVIVAL_TICKS_KEY, 0);
		root.putLong(PASSIVE_SURGE_UNTIL_TICK_KEY, 0L);
		root.putBoolean(PENDING_CRIT_KEY, false);
		root.putLong(DODGE_UNTIL_TICK_KEY, 0L);
	}

	public static boolean consumeCooldownIfReady(Player player, SkillType skillType, long cooldownTicks, long gameTime) {
		if (!hasSkill(player, skillType) || gameTime < getCooldownEnd(player, skillType)) {
			return false;
		}

		getCooldownsTag(player).putLong(skillType.id(), gameTime + cooldownTicks);
		return true;
	}

	public static boolean consumeSpecialCooldownIfReady(Player player, SpecialSkillType skillType, long gameTime) {
		if (!hasSpecialSkill(player, skillType) || gameTime < getSpecialCooldownEnd(player, skillType)) {
			return false;
		}

		getSpecialCooldownsTag(player).putLong(skillType.id(), gameTime + skillType.cooldownTicks());
		return true;
	}

	public static void activateDodge(Player player, long gameTime, long durationTicks) {
		getRoot(player).putLong(DODGE_UNTIL_TICK_KEY, gameTime + durationTicks);
	}

	public static boolean isDodgeActive(Player player, long gameTime) {
		return getRoot(player).getLong(DODGE_UNTIL_TICK_KEY) > gameTime;
	}

	public static long getRemainingDodgeTicks(Player player, long gameTime) {
		return Math.max(0L, getRoot(player).getLong(DODGE_UNTIL_TICK_KEY) - gameTime);
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

	public record DamageFatigueEntry(String label, float fatiguePercent, long lastDamageTick) {
	}

	public static long getCooldownEnd(Player player, SkillType skillType) {
		return getCooldownsTag(player).getLong(skillType.id());
	}

	public static long getSpecialCooldownEnd(Player player, SpecialSkillType skillType) {
		return getSpecialCooldownsTag(player).getLong(skillType.id());
	}

	public static long getRemainingCooldownTicks(Player player, SkillType skillType, long gameTime) {
		return Math.max(0L, getCooldownEnd(player, skillType) - gameTime);
	}

	public static long getRemainingSpecialCooldownTicks(Player player, SpecialSkillType skillType, long gameTime) {
		return Math.max(0L, getSpecialCooldownEnd(player, skillType) - gameTime);
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

	private static CompoundTag getSpecialSkillsTag(Player player) {
		return getSpecialSkillsTag(getRoot(player));
	}

	private static CompoundTag getSpecialSkillsTag(CompoundTag root) {
		ensureChildren(root);
		return root.getCompound(SPECIAL_SKILLS_KEY);
	}

	private static CompoundTag getCooldownsTag(Player player) {
		CompoundTag root = getRoot(player);
		ensureChildren(root);
		return root.getCompound(COOLDOWNS_KEY);
	}

	private static CompoundTag getSpecialCooldownsTag(Player player) {
		CompoundTag root = getRoot(player);
		ensureChildren(root);
		return root.getCompound(SPECIAL_COOLDOWNS_KEY);
	}

	private static CompoundTag getAdaptationTag(Player player) {
		CompoundTag root = getRoot(player);
		ensureChildren(root);
		return root.getCompound(DAMAGE_ADAPTATION_KEY);
	}

	private static CompoundTag getDamageFatigueTag(CompoundTag root) {
		ensureChildren(root);
		return root.getCompound(DAMAGE_FATIGUE_KEY);
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

		if (!root.contains(SPECIAL_SKILLS_KEY, Tag.TAG_COMPOUND)) {
			root.put(SPECIAL_SKILLS_KEY, new CompoundTag());
		}

		if (!root.contains(COOLDOWNS_KEY, Tag.TAG_COMPOUND)) {
			root.put(COOLDOWNS_KEY, new CompoundTag());
		}

		if (!root.contains(SPECIAL_COOLDOWNS_KEY, Tag.TAG_COMPOUND)) {
			root.put(SPECIAL_COOLDOWNS_KEY, new CompoundTag());
		}

		if (!root.contains(DAMAGE_ADAPTATION_KEY, Tag.TAG_COMPOUND)) {
			root.put(DAMAGE_ADAPTATION_KEY, new CompoundTag());
		}

		if (!root.contains(DAMAGE_FATIGUE_KEY, Tag.TAG_COMPOUND)) {
			root.put(DAMAGE_FATIGUE_KEY, new CompoundTag());
		}
	}

	private static void clearPendingState(CompoundTag root) {
		root.putBoolean(PENDING_FULL_HEAL_KEY, false);
		root.putBoolean(PENDING_SECOND_WIND_BUFF_KEY, false);
		root.putBoolean(PENDING_CRIT_KEY, false);
		root.remove(PENDING_MOTION_X_KEY);
		root.remove(PENDING_MOTION_Y_KEY);
		root.remove(PENDING_MOTION_Z_KEY);
		root.remove(PENDING_MOTION_TICK_KEY);
	}

	private static void migrateLegacySkillIds(CompoundTag root) {
		CompoundTag skillsTag = root.getCompound(SKILLS_KEY);
		CompoundTag cooldownsTag = root.getCompound(COOLDOWNS_KEY);
		if (skillsTag.getBoolean("active_assault")) {
			skillsTag.putBoolean(SkillType.ACTIVE_HEAVY_ATTACK.id(), true);
			skillsTag.putBoolean(SkillType.ACTIVE_ENTITY_GRAB.id(), true);
			skillsTag.remove("active_assault");
		}
		if (skillsTag.getBoolean("active_control")) {
			skillsTag.putBoolean(SkillType.ACTIVE_BLOCK_CONTROL.id(), true);
			skillsTag.putBoolean(SkillType.ACTIVE_CHARGED_HEAVY.id(), true);
			skillsTag.remove("active_control");
		}
		if (skillsTag.getBoolean("active_mastery")) {
			skillsTag.putBoolean(SkillType.ACTIVE_SPECIAL_THROW.id(), true);
			skillsTag.putBoolean(SkillType.ACTIVE_EXECUTION.id(), true);
			skillsTag.remove("active_mastery");
		}
		cooldownsTag.remove("active_assault");
		cooldownsTag.remove("active_control");
		cooldownsTag.remove("active_mastery");
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
		if (!root.contains(SKILL_TREE_UNLOCKED_KEY, Tag.TAG_BYTE)
				&& root.getFloat(TOTAL_DAMAGE_KEY) >= SKILL_TREE_UNLOCK_DAMAGE) {
			root.putBoolean(SKILL_TREE_UNLOCKED_KEY, true);
		}

		if (root.getBoolean(SKILL_TREE_UNLOCKED_KEY)) {
			root.putFloat(TOTAL_DAMAGE_KEY, SKILL_TREE_UNLOCK_DAMAGE);
		}

		root.putInt(POINTS_KEY, clampPoints(root.getInt(POINTS_KEY)));
		root.putInt(TOTAL_POINTS_EARNED_KEY, clampPoints(root.getInt(TOTAL_POINTS_EARNED_KEY)));
		root.putFloat(DAMAGE_PROGRESS_KEY, sanitizeProgress(root.getFloat(DAMAGE_PROGRESS_KEY)));
		root.putFloat(TOTAL_DAMAGE_KEY, clampUnlockProgress(root.getFloat(TOTAL_DAMAGE_KEY)));
		sanitizeCooldowns(root.getCompound(COOLDOWNS_KEY));
		sanitizeCooldowns(root.getCompound(SPECIAL_COOLDOWNS_KEY));
		if (root.getLong(PASSIVE_SURGE_UNTIL_TICK_KEY) < 0L) {
			root.putLong(PASSIVE_SURGE_UNTIL_TICK_KEY, 0L);
		}
		if (root.getLong(DODGE_UNTIL_TICK_KEY) < 0L) {
			root.putLong(DODGE_UNTIL_TICK_KEY, 0L);
		}
		pruneDamageFatigueEntries(getDamageFatigueTag(root), Long.MAX_VALUE);
		root.putInt(LOW_HEALTH_SURVIVAL_TICKS_KEY,
				sanitizeLowHealthSurvivalTicks(root.getInt(LOW_HEALTH_SURVIVAL_TICKS_KEY)));
	}

	private static void sanitizeCooldowns(CompoundTag cooldownsTag) {
		for (String key : new ArrayList<>(cooldownsTag.getAllKeys())) {
			if (cooldownsTag.getLong(key) < 0L) {
				cooldownsTag.putLong(key, 0L);
			}
		}
	}

	private static float applyDamageFatigue(CompoundTag root, String fatigueSourceKey, String fatigueSourceLabel,
			float amount, long gameTime) {
		CompoundTag fatigueTag = getDamageFatigueTag(root);
		pruneDamageFatigueEntries(fatigueTag, gameTime);
		String key = buildDamageFatigueKey(fatigueSourceKey);
		CompoundTag entry = fatigueTag.contains(key, Tag.TAG_COMPOUND) ? fatigueTag.getCompound(key) : new CompoundTag();
		float nextDamage = Math.min(MAX_DAMAGE_FATIGUE_DAMAGE, getEffectiveDamageFatigueDamage(entry, gameTime) + amount);
		entry.putFloat(ADAPT_COUNT_KEY, nextDamage);
		entry.putLong(ADAPT_LAST_TICK_KEY, gameTime);
		entry.putString(DAMAGE_FATIGUE_LABEL_KEY, sanitizeDamageFatigueLabel(fatigueSourceLabel));
		fatigueTag.put(key, entry);
		return amount * computeDamageGainMultiplier(nextDamage);
	}

	private static float getLowestDamageGainMultiplier(CompoundTag root, long gameTime) {
		CompoundTag fatigueTag = getDamageFatigueTag(root);
		pruneDamageFatigueEntries(fatigueTag, gameTime);
		float lowestMultiplier = 1.0F;
		for (String key : fatigueTag.getAllKeys()) {
			CompoundTag entry = fatigueTag.getCompound(key);
			lowestMultiplier = Math.min(lowestMultiplier,
					computeDamageGainMultiplier(getEffectiveDamageFatigueDamage(entry, gameTime)));
		}

		return lowestMultiplier;
	}

	private static float getEffectiveDamageFatigueDamage(CompoundTag entry, long gameTime) {
		float storedDamage = clampDamageFatigueDamage(entry.getFloat(ADAPT_COUNT_KEY));
		long lastDamageTick = entry.getLong(ADAPT_LAST_TICK_KEY);
		if (storedDamage <= 0.0F || lastDamageTick <= 0L) {
			return 0.0F;
		}

		long idleTicks = Math.max(0L, gameTime - lastDamageTick);
		if (idleTicks <= DAMAGE_FATIGUE_DECAY_DELAY_TICKS) {
			return storedDamage;
		}

		long decayTicks = idleTicks - DAMAGE_FATIGUE_DECAY_DELAY_TICKS;
		if (decayTicks <= DAMAGE_FATIGUE_DECAY_FIRST_PHASE_TICKS) {
			float firstPhaseProgress = decayTicks / (float) DAMAGE_FATIGUE_DECAY_FIRST_PHASE_TICKS;
			return clampDamageFatigueDamage(storedDamage * (1.0F - firstPhaseProgress * 0.5F));
		}

		long secondPhaseTicks = decayTicks - DAMAGE_FATIGUE_DECAY_FIRST_PHASE_TICKS;
		if (secondPhaseTicks >= DAMAGE_FATIGUE_DECAY_SECOND_PHASE_TICKS) {
			return 0.0F;
		}

		float secondPhaseProgress = secondPhaseTicks / (float) DAMAGE_FATIGUE_DECAY_SECOND_PHASE_TICKS;
		return clampDamageFatigueDamage(storedDamage * (0.5F - secondPhaseProgress * 0.5F));
	}

	private static void pruneDamageFatigueEntries(CompoundTag fatigueTag, long gameTime) {
		for (String key : new ArrayList<>(fatigueTag.getAllKeys())) {
			CompoundTag entry = fatigueTag.getCompound(key);
			if (gameTime == Long.MAX_VALUE) {
				entry.putFloat(ADAPT_COUNT_KEY, clampDamageFatigueDamage(entry.getFloat(ADAPT_COUNT_KEY)));
				if (entry.getLong(ADAPT_LAST_TICK_KEY) < 0L) {
					entry.putLong(ADAPT_LAST_TICK_KEY, 0L);
				}
				entry.putString(DAMAGE_FATIGUE_LABEL_KEY,
						sanitizeDamageFatigueLabel(entry.getString(DAMAGE_FATIGUE_LABEL_KEY)));
				continue;
			}

			if (getEffectiveDamageFatigueDamage(entry, gameTime) <= 0.0F) {
				fatigueTag.remove(key);
			}
		}
	}

	private static String buildDamageFatigueKey(String fatigueSourceKey) {
		return fatigueSourceKey == null || fatigueSourceKey.isBlank() ? "unknown" : fatigueSourceKey;
	}

	private static float computeDamageGainMultiplier(float repeatedDamage) {
		if (repeatedDamage <= 0.0F) {
			return 1.0F;
		}

		float fatigueRatio = Mth.clamp(repeatedDamage / MAX_DAMAGE_FATIGUE_DAMAGE, 0.0F, 1.0F);
		return Math.max(0.0F, 1.0F - (float) Math.pow(fatigueRatio, 1.35D));
	}

	private static float computeDamageFatiguePercent(float repeatedDamage) {
		return (1.0F - computeDamageGainMultiplier(repeatedDamage)) * 100.0F;
	}

	private static int clampPoints(long points) {
		if (points <= 0L) {
			return 0;
		}

		return points >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) points;
	}

	private static void grantPoints(CompoundTag root, int amount) {
		if (amount <= 0) {
			return;
		}

		root.putInt(POINTS_KEY, clampPoints((long) root.getInt(POINTS_KEY) + amount));
		root.putInt(TOTAL_POINTS_EARNED_KEY, clampPoints((long) root.getInt(TOTAL_POINTS_EARNED_KEY) + amount));
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

	private static float clampUnlockProgress(float amount) {
		if (!Float.isFinite(amount) || amount <= 0.0F) {
			return 0.0F;
		}

		return Math.min(amount, SKILL_TREE_UNLOCK_DAMAGE);
	}

	private static float clampDamageFatigueDamage(float fatigueDamage) {
		if (!Float.isFinite(fatigueDamage) || fatigueDamage <= 0.0F) {
			return 0.0F;
		}

		return Math.min(fatigueDamage, MAX_DAMAGE_FATIGUE_DAMAGE);
	}

	private static String sanitizeDamageFatigueLabel(String label) {
		if (label == null) {
			return "Unknown";
		}

		String trimmed = label.trim();
		return trimmed.isBlank() ? "Unknown" : trimmed;
	}

	private static int sanitizeLowHealthSurvivalTicks(int ticks) {
		if (ticks <= 0) {
			return 0;
		}

		return ticks % LOW_HEALTH_POINT_TICKS;
	}
}