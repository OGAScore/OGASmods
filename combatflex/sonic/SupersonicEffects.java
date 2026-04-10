package com.OGAS.combatflex.sonic;

import com.OGAS.combatflex.CombatFlexMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;

@Mod.EventBusSubscriber(modid = CombatFlexMod.MOD_ID, value = Dist.CLIENT)
public class SupersonicEffects {

    private static final double MACH_ONE_SPEED_SQR = 6.0;
    private static final double MACH_ENTER_SPEED_SQR = MACH_ONE_SPEED_SQR;
    private static final double MACH_EXIT_SPEED_SQR = 4.0;
    private static final double MACH_TRANSITION_MIN_SPEED_SQR = 5.4;
    private static final double MACH_TRANSITION_MAX_SPEED_SQR = 6.6;
    private static final double FIRE_EFFECT_SPEED_SQR = 8.0;
    private static final double OWN_SOUND_MAX_DISTANCE_SQR = 9.0;

    private static final Map<Player, Boolean> supersonicState = new WeakHashMap<>();
    private static double currentMach = 0.0;

    // ==========================================
    // 【物理细节】超音速静音结界
    // ==========================================
    @SubscribeEvent
    public static void onPlaySound(PlaySoundEvent event) {
        if (event.getSound() == null) {
            return;
        }

        // 1. 彻底屏蔽原版烦人的鞘翅风噪声
        if (event.getSound() instanceof net.minecraft.client.resources.sounds.ElytraOnPlayerSoundInstance) {
            event.setSound(null);
            return;
        }

        // 2. 超过 1 马赫后，只保留来自本地玩家自己的声音
        if (currentMach >= 1.0) {
            Minecraft mc = Minecraft.getInstance();
            Player localPlayer = mc.player;
            if (localPlayer == null || !isOwnPlayerSound(event.getSound(), localPlayer)) {
                event.setSound(null);
            }
        }
    }

    private static boolean isOwnPlayerSound(net.minecraft.client.resources.sounds.SoundInstance sound, Player localPlayer) {
        if (sound.getSource() != SoundSource.PLAYERS) {
            return false;
        }

        if (sound.isRelative()) {
            return false;
        }

        double dx = sound.getX() - localPlayer.getX();
        double dy = sound.getY() - (localPlayer.getY() + 1.0);
        double dz = sound.getZ() - localPlayer.getZ();
        return dx * dx + dy * dy + dz * dz <= OWN_SOUND_MAX_DISTANCE_SQR;
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Player player = event.player;
        Level level = player.level();
        if (!level.isClientSide()) return;

        Minecraft mc = Minecraft.getInstance();
        Player localPlayer = mc.player;
        if (localPlayer == null) return;

        boolean isMe = (player == localPlayer);
        if (!isMe && player.distanceToSqr(localPlayer) > 1024.0) return;

        double dx = player.getX() - player.xo;
        double dy = player.getY() - player.yo;
        double dz = player.getZ() - player.zo;
        double speedSqr = dx * dx + dy * dy + dz * dz;

        if (isMe) {
            currentMach = Math.sqrt(speedSqr / MACH_ONE_SPEED_SQR);
        }

        boolean wasFast = supersonicState.getOrDefault(player, false);
        boolean isFastNow = wasFast;

        if (speedSqr >= MACH_ENTER_SPEED_SQR && speedSqr < 100.0) {
            isFastNow = true;
        } else if (speedSqr < MACH_EXIT_SPEED_SQR) {
            isFastNow = false;
        }

        if (speedSqr > 0.1) {
            Vec3 dir = new Vec3(dx, dy, dz).normalize();
            Vec3 up = (Math.abs(dir.y) > 0.9) ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
            Vec3 right = dir.cross(up).normalize();
            Vec3 top = dir.cross(right).normalize();

            if (isFastNow && !wasFast) {
                double spawnX = player.getX() - dir.x;
                double spawnY = player.getY() + 1.0 - dir.y;
                double spawnZ = player.getZ() - dir.z;

                level.addParticle(ParticleTypes.SONIC_BOOM, spawnX, spawnY, spawnZ, 0, 0, 0);

                int ringCount = isMe ? 36 : 12;
                for (int i = 0; i < ringCount; i++) {
                    double angle = i * Math.PI * 2 / ringCount;
                    double cx = right.x * Math.cos(angle) + top.x * Math.sin(angle);
                    double cy = right.y * Math.cos(angle) + top.y * Math.sin(angle);
                    double cz = right.z * Math.cos(angle) + top.z * Math.sin(angle);
                    
                    level.addParticle(ParticleTypes.CLOUD, spawnX + cx * 2.5, spawnY + cy * 2.5, spawnZ + cz * 2.5, cx * 0.4, cy * 0.4, cz * 0.4);
                    level.addParticle(ParticleTypes.POOF, spawnX + cx * 2.0, spawnY + cy * 2.0, spawnZ + cz * 2.0, cx * 0.8, cy * 0.8, cz * 0.8);
                }

                int sparkCount = isMe ? 25 : 8;
                for (int i = 0; i < sparkCount; i++) {
                    double offsetX = (level.random.nextDouble() - 0.5) * 2.0;
                    double offsetY = (level.random.nextDouble() - 0.5) * 2.0;
                    double offsetZ = (level.random.nextDouble() - 0.5) * 2.0;
                    level.addParticle(ParticleTypes.ELECTRIC_SPARK, spawnX + offsetX, spawnY + offsetY, spawnZ + offsetZ, -dir.x * 0.5, -dir.y * 0.5, -dir.z * 0.5);
                }

                if (isMe) {
                    level.playLocalSound(spawnX, spawnY, spawnZ, SoundEvents.TRIDENT_RIPTIDE_3, SoundSource.PLAYERS, 0.8f, 1.0f, false);
                    level.playLocalSound(spawnX, spawnY, spawnZ, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.5f, 1.2f, false);
                }
            }

            if (speedSqr >= MACH_TRANSITION_MIN_SPEED_SQR && speedSqr <= MACH_TRANSITION_MAX_SPEED_SQR) {
                int coneCount = isMe ? 10 : 3;
                for (int i = 0; i < coneCount; i++) {
                    double angle = level.random.nextDouble() * Math.PI * 2;
                    double cx = right.x * Math.cos(angle) + top.x * Math.sin(angle);
                    double cy = right.y * Math.cos(angle) + top.y * Math.sin(angle);
                    double cz = right.z * Math.cos(angle) + top.z * Math.sin(angle);

                    double radius = 1.0;
                    double px = player.getX() - dir.x * 0.5 + cx * radius;
                    double py = player.getY() + 1.0 - dir.y * 0.5 + cy * radius;
                    double pz = player.getZ() - dir.z * 0.5 + cz * radius;

                    level.addParticle(ParticleTypes.POOF, px, py, pz, cx * 0.15, cy * 0.15, cz * 0.15);
                }
            }

            if (isFastNow) {
                int trailCount = isMe ? 4 : 1;
                for (int i = 0; i < trailCount; i++) {
                    double trailX = player.getX() - dir.x * 1.5 + (level.random.nextDouble() - 0.5) * 0.6;
                    double trailY = player.getY() + 1.0 - dir.y * 1.5 + (level.random.nextDouble() - 0.5) * 0.6;
                    double trailZ = player.getZ() - dir.z * 1.5 + (level.random.nextDouble() - 0.5) * 0.6;

                    level.addParticle(ParticleTypes.FLAME, trailX, trailY, trailZ, -dir.x * 0.1, -dir.y * 0.1, -dir.z * 0.1);
                }

                if (speedSqr >= FIRE_EFFECT_SPEED_SQR) {
                    int fireCount = isMe ? 12 : 4;
                    for (int i = 0; i < fireCount; i++) {
                        double px = player.getX() + (level.random.nextDouble() - 0.5) * player.getBbWidth() * 1.5;
                        double py = player.getY() + level.random.nextDouble() * player.getBbHeight();
                        double pz = player.getZ() + (level.random.nextDouble() - 0.5) * player.getBbWidth() * 1.5;
                        
                        level.addParticle(ParticleTypes.FLAME, px, py, pz, 0, 0.05, 0);
                    }

                    if (isMe) {
                        Vec3 eyePos = player.getEyePosition();
                        Vec3 look = player.getLookAngle();
                        
                        Vec3 screenRight = look.cross(new Vec3(0, 1, 0)).normalize();
                        if (screenRight.lengthSqr() < 0.1) screenRight = new Vec3(1, 0, 0);
                        Vec3 screenUp = screenRight.cross(look).normalize();

                        for (int i = 0; i < 2; i++) {
                            double dist = 2.0;
                            double radius = 1.6 + level.random.nextDouble() * 0.5;
                            double angle = level.random.nextDouble() * Math.PI * 2;

                            double px = eyePos.x + look.x * dist + screenRight.x * Math.cos(angle) * radius + screenUp.x * Math.sin(angle) * radius;
                            double py = eyePos.y + look.y * dist + screenRight.y * Math.cos(angle) * radius + screenUp.y * Math.sin(angle) * radius;
                            double pz = eyePos.z + look.z * dist + screenRight.z * Math.cos(angle) * radius + screenUp.z * Math.sin(angle) * radius;

                            level.addParticle(ParticleTypes.POOF, px, py, pz, -dir.x * 0.4, -dir.y * 0.4, -dir.z * 0.4);
                        }
                    }
                }
            }
        }

        supersonicState.put(player, isFastNow);
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiOverlayEvent.Post event) {
        if (!event.getOverlay().id().equals(VanillaGuiOverlay.HOTBAR.id())) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui) return; 

        if (currentMach > 0.3) {
            GuiGraphics graphics = event.getGuiGraphics();
            Font font = mc.font;

            int color = 0xFFFFFF; 
            if (currentMach >= 1.6) {
                color = 0xFF5500; 
            } else if (currentMach >= 1.0) {
                color = 0x00FFFF; 
            }

            Component text = Component.translatable("hud.cbtflex.mach", String.format(Locale.US, "%.2f", currentMach));

            int screenHeight = event.getWindow().getGuiScaledHeight();
            int x = 15;
            int y = screenHeight - 25;

            graphics.drawString(font, text, x, y, color, true);
        }
    }
}