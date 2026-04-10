package com.OGAS.combatflex.flight;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = FlightFeature.MOD_ID, value = Dist.CLIENT)
final class FlightClientEvents {
    private static final int CLIENT_FLIGHT_BUFF_TICKS = 20 * 60 * 5;
    private static final int CLIENT_FAST_FLIGHT_BUFF_TICKS = 20 * 60 * 3;
    private static final int HOLD_MENU_TICKS = 7;
    private static final int TAKEOFF_WALL_IMMUNITY_TICKS = 20;
    private static final int QUICK_TAP_MODE = 3;
    private static final int HOVER_MODE = 2;
    private static final double SLOW_FLIGHT_SPEED = 2.7;
    private static final double FAST_FLIGHT_BASE_SPEED = 6.09;
    private static final double FAST_FLIGHT_MAX_SPEED = 35.28;
    private static final double SONIC_EFFECT_SPEED = FAST_FLIGHT_BASE_SPEED;
    private static final double FAST_FLIGHT_ACCELERATION_FACTOR = 0.5;
    private static final double QUICK_TAP_ACCELERATION_FACTOR = 0.35;
    private static final double FAST_TURN_RESPONSE_FACTOR = 0.12;
    private static final double SLOW_TURN_RESPONSE_FACTOR = 0.18;
    private static final double HOVER_VERTICAL_SPEED = 0.2;
    private static final int SLOW_LANDING_CLOUD_PARTICLES = 32;
    private static final int SLOW_LANDING_POOF_PARTICLES = 16;
    private static final int SLOW_LANDING_RING_PARTICLES = 24;
    private static final double SLOW_LANDING_RING_RADIUS = 1.55;
    private static final int SLOW_LANDING_SHAKE_TICKS = 5;
    private static final int SLOW_IMPACT_EFFECT_COOLDOWN_TICKS = 6;
    private static final float FAST_FLIGHT_SHAKE_BASE_PITCH = 0.12f;
    private static final float FAST_FLIGHT_SHAKE_BASE_YAW = 0.08f;
    private static final float FAST_FLIGHT_SHAKE_BASE_ROLL = 0.3f;
    private static final float SLOW_LANDING_SHAKE_PITCH = 1.2f;
    private static final float SLOW_LANDING_SHAKE_YAW = 0.12f;
    private static final float SLOW_LANDING_SHAKE_ROLL = 0.45f;

    private static int flightMode = 0;
    private static int activeMode = 1;
    private static double currentFlightSpeed = 0.0;
    private static double currentMach = 0.0;
    private static boolean hoverLockedByImpact = false;
    private static int slowLandingShakeTicks = 0;
    private static int slowImpactEffectCooldownTicks = 0;
    private static int clientFlightBuffTicks = 0;
    private static int clientFastFlightBuffTicks = 0;
    private static boolean clientNightVisionOverrideActive = false;
    private static double clientStoredGamma = 0.5;
    private static double clientStoredDarknessScale = 1.0;

    private static int keyHoldTicks = 0;
    private static boolean wasKeyDown = false;
    private static boolean menuOpenThisHold = false;
    private static int takeoffWallImmunityTicks = 0;

    private FlightClientEvents() {
    }

    @SubscribeEvent
    public static void onPlaySound(PlaySoundEvent event) {
        if (flightMode != 1 || currentMach < 1.0) {
            return;
        }
        if (event.getSound() == null || event.getSound().getLocation() == null) {
            return;
        }

        String path = event.getSound().getLocation().getPath();
        if (!path.contains("ui") && !path.contains("player") && !path.contains("damage")
                && !path.contains("hit") && !path.contains("swing")
                && !path.contains("riptide") && !path.contains("explode")) {
            event.setSound(null);
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) {
            slowLandingShakeTicks = 0;
            slowImpactEffectCooldownTicks = 0;
            clientFlightBuffTicks = 0;
            clientFastFlightBuffTicks = 0;
            restoreClientNightVisionOptions(mc);
            return;
        }

        if (slowLandingShakeTicks > 0) {
            slowLandingShakeTicks--;
        }
        if (slowImpactEffectCooldownTicks > 0) {
            slowImpactEffectCooldownTicks--;
        }
        if (takeoffWallImmunityTicks > 0) {
            takeoffWallImmunityTicks--;
        }
        if (clientFlightBuffTicks > 0) {
            clientFlightBuffTicks--;
        }
        if (clientFastFlightBuffTicks > 0) {
            clientFastFlightBuffTicks--;
        }
        updateClientNightVisionOverride(mc);

        boolean isKeyDown = isFlyKeyPhysicallyDown(mc);
        boolean slowFlightUnlocked = FlightFeature.hasSlowFlightUnlocked(player);
        if (!slowFlightUnlocked) {
            if (flightMode != 0) {
                stopFlight(false);
            }
        }

        if (flightMode == 1 && !FlightFeature.hasFastFlightUnlocked(player)) {
            stopFlight(false);
        }

        if (isKeyDown) {
            keyHoldTicks++;
            if (!menuOpenThisHold && keyHoldTicks >= HOLD_MENU_TICKS) {
                menuOpenThisHold = true;
                FlightMenuOverlay.open(mc);
            }
        } else {
            if (wasKeyDown) {
                if (menuOpenThisHold) {
                    int hoveredMode = FlightMenuOverlay.getHoveredMode(mc);
                    if (hoveredMode == 0) {
                        stopFlight();
                    } else if (hoveredMode != FlightMenuOverlay.NO_SELECTION_MODE) {
                        applyFlightMode(hoveredMode);
                    }
                    FlightMenuOverlay.close(mc);
                } else if (keyHoldTicks < HOLD_MENU_TICKS) {
                    if (!slowFlightUnlocked) {
                        notifyFlightLocked(player, "message.cbtflex.slow_flight_locked");
                    } else if (flightMode == 0 || flightMode == HOVER_MODE) {
                        applyQuickTapFlightMode();
                    } else {
                        applyHoverMode();
                    }
                } else {
                    FlightMenuOverlay.close(mc);
                }
            }

            keyHoldTicks = 0;
            menuOpenThisHold = false;
        }
        wasKeyDown = isKeyDown;

        if (!slowFlightUnlocked) {
            return;
        }

        if (player.isShiftKeyDown() && flightMode != 0 && flightMode != HOVER_MODE) {
            stopFlight();
        }

        if (flightMode == 1 || flightMode == 3) {
            player.startFallFlying();
            double altitudeFactor = Math.min(1.0, player.getY() / 500.0);
            double targetSpeed = flightMode == 3
                    ? SLOW_FLIGHT_SPEED
                    : (FAST_FLIGHT_BASE_SPEED + altitudeFactor * (FAST_FLIGHT_MAX_SPEED - FAST_FLIGHT_BASE_SPEED));
            double accelerationFactor = flightMode == QUICK_TAP_MODE
                    ? QUICK_TAP_ACCELERATION_FACTOR
                    : FAST_FLIGHT_ACCELERATION_FACTOR;

            currentFlightSpeed += (targetSpeed - currentFlightSpeed) * accelerationFactor;
                Vec3 desiredVelocity = player.getLookAngle().scale(currentFlightSpeed);
                Vec3 currentVelocity = player.getDeltaMovement();
                double turnResponseFactor = flightMode == QUICK_TAP_MODE
                    ? SLOW_TURN_RESPONSE_FACTOR
                    : FAST_TURN_RESPONSE_FACTOR;
                Vec3 steeredVelocity = currentVelocity.lengthSqr() > 1.0E-4
                    ? currentVelocity.lerp(desiredVelocity, turnResponseFactor)
                    : desiredVelocity;
                if (steeredVelocity.lengthSqr() > 1.0E-4) {
                steeredVelocity = steeredVelocity.normalize().scale(currentFlightSpeed);
                }
                player.setDeltaMovement(steeredVelocity);

            boolean slowGroundImpact = flightMode == 3 && player.verticalCollision && player.getDeltaMovement().y < 0.0;
            if (slowGroundImpact) {
                player.setDeltaMovement(Vec3.ZERO);
                triggerSlowImpactEffects(player, false);
                stopFlight(false);
                return;
            }

            if (player.horizontalCollision && takeoffWallImmunityTicks > 0) {
                if (flightMode == QUICK_TAP_MODE) {
                    player.setDeltaMovement(Vec3.ZERO);
                    triggerSlowImpactEffects(player, true);
                }
                return;
            }

            if ((player.horizontalCollision || player.verticalCollision) && currentFlightSpeed > 2.0) {
                player.setDeltaMovement(Vec3.ZERO);
                if (flightMode == QUICK_TAP_MODE) {
                    triggerSlowImpactEffects(player, player.horizontalCollision);
                    hoverLockedByImpact = true;
                    flightMode = HOVER_MODE;
                    FlightMenuOverlay.close(mc);
                    FlightFeature.NETWORK.sendToServer(new FlightPacket(HOVER_MODE, 0f));
                    currentFlightSpeed = 0;
                    return;
                }
                hoverLockedByImpact = true;
                flightMode = HOVER_MODE;
                FlightMenuOverlay.close(mc);
                FlightFeature.NETWORK.sendToServer(new FlightPacket(99, (float) currentFlightSpeed, true, player.getX(), player.getY(), player.getZ()));
                currentFlightSpeed = 0;
            }
        } else if (flightMode == 2) {
            if (player.onGround() && !hoverLockedByImpact) {
                stopFlight(false);
                return;
            }

            player.stopFallFlying();
            Vec3 move = player.getDeltaMovement();
            double verticalSpeed = 0.0;
            if (mc.options.keyShift.isDown()) {
                verticalSpeed -= HOVER_VERTICAL_SPEED;
            }
            if (mc.options.keyJump.isDown()) {
                verticalSpeed += HOVER_VERTICAL_SPEED;
            }

            Vec3 dampedMove = move.multiply(0.8, 0.6, 0.8);
            player.setDeltaMovement(dampedMove.x, verticalSpeed, dampedMove.z);
        }
    }

    @SubscribeEvent
    public static void onRenderGuiOverlay(RenderGuiOverlayEvent.Post event) {
        if (!FlightMenuOverlay.isVisible()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            FlightMenuOverlay.close(mc);
            return;
        }

        try {
            renderHoldMenu(event.getGuiGraphics(), mc);
        } catch (RuntimeException ex) {
            FlightMenuOverlay.close(mc);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !event.player.level().isClientSide()) {
            return;
        }

        Player player = event.player;
        double speed = player.getDeltaMovement().length();
        currentMach = flightMode == 1 ? speed / SONIC_EFFECT_SPEED : 0.0;
        if (flightMode == 1 && currentMach >= 1.0 && player.level().random.nextInt(5) == 0) {
            player.level().addParticle(ParticleTypes.POOF, player.getX(), player.getY(), player.getZ(), 0, 0, 0);
        }
    }

    @SubscribeEvent
    public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) {
            return;
        }

        float time = (float) (player.tickCount + event.getPartialTick());
        float pitchOffset = 0.0f;
        float yawOffset = 0.0f;
        float rollOffset = 0.0f;

        if (flightMode == 1) {
            double speedFactor = Math.min(1.0, Math.max(0.0, (currentFlightSpeed - FAST_FLIGHT_BASE_SPEED) / (FAST_FLIGHT_MAX_SPEED - FAST_FLIGHT_BASE_SPEED)));
            if (speedFactor > 0.0) {
                float intensity = (float) (0.35 + speedFactor * 0.65);
                pitchOffset += (float) (
                    Math.sin(time * 5.8f) * 0.65f
                        + Math.cos(time * 9.6f) * 0.35f
                ) * FAST_FLIGHT_SHAKE_BASE_PITCH * intensity;
                yawOffset += (float) (
                    Math.cos(time * 6.6f) * 0.6f
                        + Math.sin(time * 11.2f) * 0.4f
                ) * FAST_FLIGHT_SHAKE_BASE_YAW * intensity;
                rollOffset += (float) (
                    Math.sin(time * 7.8f) * 0.7f
                        + Math.cos(time * 13.5f) * 0.3f
                ) * FAST_FLIGHT_SHAKE_BASE_ROLL * intensity;
            }
        }

        if (slowLandingShakeTicks > 0) {
            float impactProgress = (float) Math.max(0.0, (slowLandingShakeTicks - event.getPartialTick()) / SLOW_LANDING_SHAKE_TICKS);
            float impactStrength = impactProgress * impactProgress;
            float impactTime = (float) ((SLOW_LANDING_SHAKE_TICKS - slowLandingShakeTicks) + event.getPartialTick());
            pitchOffset += -SLOW_LANDING_SHAKE_PITCH * impactStrength
                    + (float) Math.sin(impactTime * 14.0f) * 0.12f * impactStrength;
            yawOffset += (float) Math.cos(impactTime * 11.0f) * SLOW_LANDING_SHAKE_YAW * impactStrength;
            rollOffset += (float) Math.sin(impactTime * 16.0f) * SLOW_LANDING_SHAKE_ROLL * impactStrength;
        }

        if (pitchOffset == 0.0f && yawOffset == 0.0f && rollOffset == 0.0f) {
            return;
        }

        event.setPitch(event.getPitch() + pitchOffset);
        event.setYaw(event.getYaw() + yawOffset);
        event.setRoll(event.getRoll() + rollOffset);
    }

    static void applyFlightMode(int mode) {
        applyFlightMode(mode, true);
    }

    private static void applyQuickTapFlightMode() {
        applyFlightMode(QUICK_TAP_MODE, false);
    }

    private static void applyHoverMode() {
        applyFlightMode(HOVER_MODE, false);
    }

    private static void applyFlightMode(int mode, boolean rememberAsActiveMode) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (!FlightFeature.canUseMode(player, mode)) {
            if (player != null && mode != 0) {
                notifyFlightLocked(player,
                        mode == 1 ? "message.cbtflex.fast_flight_locked" : "message.cbtflex.slow_flight_locked");
            }
            return;
        }

        int previousMode = flightMode;
        if (rememberAsActiveMode) {
            activeMode = mode;
        }
        activateClientBuffs(mode);
        flightMode = mode;
        takeoffWallImmunityTicks = previousMode == 0 && (mode == 1 || mode == QUICK_TAP_MODE)
                ? TAKEOFF_WALL_IMMUNITY_TICKS
                : 0;
        hoverLockedByImpact = false;
        FlightMenuOverlay.close(minecraft);
        FlightFeature.NETWORK.sendToServer(new FlightPacket(mode, 0f));
    }

    private static void notifyFlightLocked(Player player, String translationKey) {
        player.playSound(SoundEvents.VILLAGER_NO, 0.7F, 0.85F);
        player.displayClientMessage(Component.translatable(translationKey), true);
    }

    private static boolean isFlyKeyPhysicallyDown(Minecraft mc) {
        return InputConstants.isKeyDown(mc.getWindow().getWindow(), FlightClientSetupEvents.FLY_KEY.getKey().getValue());
    }

    private static void activateClientBuffs(int mode) {
        if (mode == 0) {
            return;
        }
        clientFlightBuffTicks = CLIENT_FLIGHT_BUFF_TICKS;
        if (mode == 1) {
            clientFastFlightBuffTicks = CLIENT_FAST_FLIGHT_BUFF_TICKS;
        }
    }

    private static void updateClientNightVisionOverride(Minecraft mc) {
        if (clientFlightBuffTicks > 0) {
            if (!clientNightVisionOverrideActive) {
                clientStoredGamma = mc.options.gamma().get();
                clientStoredDarknessScale = mc.options.darknessEffectScale().get();
                clientNightVisionOverrideActive = true;
            }
            mc.options.gamma().set(1.0D);
            mc.options.darknessEffectScale().set(0.0D);
            return;
        }

        restoreClientNightVisionOptions(mc);
    }

    private static void restoreClientNightVisionOptions(Minecraft mc) {
        if (!clientNightVisionOverrideActive) {
            return;
        }
        mc.options.gamma().set(clientStoredGamma);
        mc.options.darknessEffectScale().set(clientStoredDarknessScale);
        clientNightVisionOverrideActive = false;
    }

    private static void stopFlight() {
        stopFlight(shouldUseGentleLanding());
    }

    private static void stopFlight(boolean gentleLanding) {
        flightMode = 0;
        takeoffWallImmunityTicks = 0;
        currentFlightSpeed = 0.0;
        hoverLockedByImpact = false;
        FlightMenuOverlay.close(Minecraft.getInstance());
        FlightFeature.NETWORK.sendToServer(new FlightPacket(0, 0f, gentleLanding));
    }

    private static boolean shouldUseGentleLanding() {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        return flightMode == HOVER_MODE && player != null && !player.onGround();
    }

    private static void triggerSlowImpactEffects(Player player, boolean wallImpact) {
        if (slowImpactEffectCooldownTicks > 0) {
            return;
        }
        slowImpactEffectCooldownTicks = SLOW_IMPACT_EFFECT_COOLDOWN_TICKS;
        playSlowImpactEffects(player, wallImpact);
    }

    private static void playSlowImpactEffects(Player player, boolean wallImpact) {
        slowLandingShakeTicks = SLOW_LANDING_SHAKE_TICKS;

        Vec3 velocity = player.getDeltaMovement();
        Vec3 reverseDirection = velocity.lengthSqr() > 1.0E-4
                ? velocity.normalize().scale(-1.0)
                : Vec3.ZERO;
        Vec3 surfaceAxisA = wallImpact ? new Vec3(0.0, 1.0, 0.0) : new Vec3(1.0, 0.0, 0.0);
        Vec3 surfaceAxisB = wallImpact ? getWallImpactTangentAxis(velocity) : new Vec3(0.0, 0.0, 1.0);
        Vec3 trailDirection = projectOntoImpactSurface(reverseDirection, surfaceAxisA, surfaceAxisB);
        if (trailDirection.lengthSqr() > 1.0E-4) {
            trailDirection = trailDirection.normalize().scale(wallImpact ? 0.34 : 0.28);
        } else {
            trailDirection = Vec3.ZERO;
        }
        double impactCenterY = player.getY() + (wallImpact ? player.getBbHeight() * 0.5 : 0.04);

        player.level().addParticle(ParticleTypes.POOF, player.getX(), impactCenterY, player.getZ(), trailDirection.x * 0.2, wallImpact ? 0.02 : 0.05, trailDirection.z * 0.2);
        player.level().addParticle(ParticleTypes.POOF, player.getX(), impactCenterY + (wallImpact ? 0.14 : 0.04), player.getZ(), trailDirection.x * 0.28, wallImpact ? 0.03 : 0.07, trailDirection.z * 0.28);
        player.level().addParticle(ParticleTypes.POOF, player.getX(), impactCenterY + (wallImpact ? 0.28 : 0.08), player.getZ(), trailDirection.x * 0.36, wallImpact ? 0.05 : 0.09, trailDirection.z * 0.36);

        for (int index = 0; index < SLOW_LANDING_RING_PARTICLES; index++) {
            double angle = (Math.PI * 2.0 * index) / SLOW_LANDING_RING_PARTICLES;
            Vec3 ringOffset = surfaceAxisA.scale(Math.cos(angle) * SLOW_LANDING_RING_RADIUS)
                    .add(surfaceAxisB.scale(Math.sin(angle) * SLOW_LANDING_RING_RADIUS));
            Vec3 ringVelocity = surfaceAxisA.scale(Math.cos(angle) * 0.19)
                    .add(surfaceAxisB.scale(Math.sin(angle) * 0.19))
                    .add(trailDirection.scale(0.45));
            player.level().addParticle(
                    ParticleTypes.CLOUD,
                    player.getX() + ringOffset.x,
                    impactCenterY + ringOffset.y,
                    player.getZ() + ringOffset.z,
                    ringVelocity.x,
                    wallImpact ? ringVelocity.y : 0.04,
                    ringVelocity.z
            );
            if ((index & 1) == 0) {
                Vec3 poofOffset = ringOffset.scale(0.9);
                Vec3 poofVelocity = surfaceAxisA.scale(Math.cos(angle) * 0.15)
                        .add(surfaceAxisB.scale(Math.sin(angle) * 0.15));
                player.level().addParticle(
                        ParticleTypes.POOF,
                        player.getX() + poofOffset.x,
                        impactCenterY + poofOffset.y,
                        player.getZ() + poofOffset.z,
                        poofVelocity.x,
                        wallImpact ? poofVelocity.y : 0.035,
                        poofVelocity.z
                );
            }
        }

        for (int index = 0; index < SLOW_LANDING_CLOUD_PARTICLES; index++) {
            double offsetA = (player.level().random.nextDouble() - 0.5) * 1.45;
            double offsetB = (player.level().random.nextDouble() - 0.5) * 1.45;
            Vec3 offset = surfaceAxisA.scale(offsetA).add(surfaceAxisB.scale(offsetB));
            Vec3 scatterVelocity = trailDirection
                    .add(surfaceAxisA.scale(offsetA * 0.085))
                    .add(surfaceAxisB.scale(offsetB * 0.085));
            player.level().addParticle(
                    ParticleTypes.CLOUD,
                    player.getX() + offset.x,
                    impactCenterY + offset.y,
                    player.getZ() + offset.z,
                    scatterVelocity.x,
                    wallImpact ? scatterVelocity.y : 0.024,
                    scatterVelocity.z
            );
        }

        for (int index = 0; index < SLOW_LANDING_POOF_PARTICLES; index++) {
            double offsetA = (player.level().random.nextDouble() - 0.5) * 1.05;
            double offsetB = (player.level().random.nextDouble() - 0.5) * 1.05;
            Vec3 offset = surfaceAxisA.scale(offsetA).add(surfaceAxisB.scale(offsetB));
            player.level().addParticle(
                    ParticleTypes.POOF,
                    player.getX() + offset.x,
                    impactCenterY + offset.y,
                    player.getZ() + offset.z,
                    trailDirection.x * 0.95,
                    wallImpact ? trailDirection.y * 0.95 : 0.04,
                    trailDirection.z * 0.95
            );
        }

        player.level().playSound(
                player,
                player.getX(),
                player.getY(),
                player.getZ(),
            SoundEvents.PLAYER_BIG_FALL,
                SoundSource.PLAYERS,
            1.05f,
            0.82f
        );
        player.level().playSound(
            player,
            player.getX(),
            player.getY(),
            player.getZ(),
            SoundEvents.GENERIC_EXPLODE,
            SoundSource.PLAYERS,
            0.28f,
            1.45f
        );
    }

    private static Vec3 getWallImpactTangentAxis(Vec3 velocity) {
        return Math.abs(velocity.x) >= Math.abs(velocity.z)
                ? new Vec3(0.0, 0.0, 1.0)
                : new Vec3(1.0, 0.0, 0.0);
    }

    private static Vec3 projectOntoImpactSurface(Vec3 vector, Vec3 axisA, Vec3 axisB) {
        return axisA.scale(vector.dot(axisA)).add(axisB.scale(vector.dot(axisB)));
    }

    private static void renderHoldMenu(GuiGraphics graphics, Minecraft mc) {
        FlightMenuOverlay.render(graphics, mc);
    }
}