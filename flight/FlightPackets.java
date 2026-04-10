package com.OGAS.combatflex.flight;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.Supplier;

final class FlightPackets {
    private FlightPackets() {
    }

    static void register(SimpleChannel network) {
        network.registerMessage(0, FlightPacket.class, FlightPacket::encode, FlightPacket::decode, FlightPacket::handle);
        network.registerMessage(1, MenuStatePacket.class, MenuStatePacket::encode, MenuStatePacket::decode, MenuStatePacket::handle);
    }

    static void disableFlight(ServerPlayer player) {
        FlightServerEvents.setServerFlightMode(player.getUUID(), 0);
        player.setDeltaMovement(Vec3.ZERO);
        player.stopFallFlying();
        if (!player.isCreative() && !player.isSpectator()) {
            player.getAbilities().mayfly = false;
            player.getAbilities().flying = false;
        }
    }
}

final class FlightPacket {
    int mode;
    float speed;
    boolean gentleLanding;
    double impactX;
    double impactY;
    double impactZ;

    FlightPacket(int mode, float speed) {
        this(mode, speed, false, Double.NaN, Double.NaN, Double.NaN);
    }

    FlightPacket(int mode, float speed, boolean gentleLanding) {
        this(mode, speed, gentleLanding, Double.NaN, Double.NaN, Double.NaN);
    }

    FlightPacket(int mode, float speed, boolean gentleLanding, double impactX, double impactY, double impactZ) {
        this.mode = mode;
        this.speed = speed;
        this.gentleLanding = gentleLanding;
        this.impactX = impactX;
        this.impactY = impactY;
        this.impactZ = impactZ;
    }

    static void encode(FlightPacket message, FriendlyByteBuf buffer) {
        buffer.writeInt(message.mode);
        buffer.writeFloat(message.speed);
        buffer.writeBoolean(message.gentleLanding);
        buffer.writeDouble(message.impactX);
        buffer.writeDouble(message.impactY);
        buffer.writeDouble(message.impactZ);
    }

    static FlightPacket decode(FriendlyByteBuf buffer) {
        return new FlightPacket(
                buffer.readInt(),
                buffer.readFloat(),
                buffer.readBoolean(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble()
        );
    }

    static void handle(FlightPacket message, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) {
                return;
            }

            if (!FlightFeature.canUseMode(player, message.mode)) {
                FlightPackets.disableFlight(player);
                player.onUpdateAbilities();
                return;
            }

            int previousMode = FlightServerEvents.getServerFlightMode(player.getUUID());

            if (message.mode == 99) {
                double impactX = Double.isNaN(message.impactX) ? player.getX() : message.impactX;
                double impactY = Double.isNaN(message.impactY) ? player.getY() : message.impactY;
                double impactZ = Double.isNaN(message.impactZ) ? player.getZ() : message.impactZ;
                if (FlightServerEvents.getServerFlightMode(player.getUUID()) != 3) {
                    player.level().explode(player, impactX, impactY, impactZ, Math.min(message.speed * 0.5f, 10f), Level.ExplosionInteraction.TNT);
                }
                player.teleportTo(impactX, impactY, impactZ);
                player.setDeltaMovement(Vec3.ZERO);
                player.stopFallFlying();
                if (message.gentleLanding) {
                    FlightServerEvents.setServerFlightMode(player.getUUID(), 2);
                    player.getAbilities().mayfly = true;
                    player.getAbilities().flying = true;
                    player.resetFallDistance();
                } else if (!player.isCreative() && !player.isSpectator()) {
                    FlightServerEvents.setServerFlightMode(player.getUUID(), 0);
                    player.getAbilities().mayfly = false;
                    player.getAbilities().flying = false;
                }
            } else if (message.mode == 1 || message.mode == 3) {
                FlightServerEvents.setServerFlightMode(player.getUUID(), message.mode);
                if (message.mode == 1 && previousMode == 0 && player.onGround()) {
                    FlightServerEvents.applyFastTakeoffTerrainBurst(player);
                }
                player.startFallFlying();
                player.getAbilities().mayfly = true;
                player.getAbilities().flying = false;
            } else if (message.mode == 2) {
                FlightServerEvents.setServerFlightMode(player.getUUID(), 2);
                player.setDeltaMovement(Vec3.ZERO);
                player.stopFallFlying();
                player.getAbilities().mayfly = true;
                player.getAbilities().flying = true;
            } else {
                FlightPackets.disableFlight(player);
                if (message.gentleLanding) {
                    player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 60, 0, false, false, false));
                    player.resetFallDistance();
                }
            }

            player.onUpdateAbilities();
        });
        ctx.get().setPacketHandled(true);
    }
}

final class MenuStatePacket {
    boolean open;

    MenuStatePacket(boolean open) {
        this.open = open;
    }

    static void encode(MenuStatePacket message, FriendlyByteBuf buffer) {
        buffer.writeBoolean(message.open);
    }

    static MenuStatePacket decode(FriendlyByteBuf buffer) {
        return new MenuStatePacket(buffer.readBoolean());
    }

    static void handle(MenuStatePacket message, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                FlightServerEvents.setMenuOpen(player.getUUID(), message.open && FlightFeature.hasSlowFlightUnlocked(player));
            }
        });
        ctx.get().setPacketHandled(true);
    }
}