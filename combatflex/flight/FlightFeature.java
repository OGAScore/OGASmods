package com.OGAS.combatflex.flight;

import com.OGAS.combatflex.CombatFlexMod;
import com.OGAS.combatflex.SkillData;
import com.OGAS.combatflex.SkillType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class FlightFeature {
    public static final String MOD_ID = CombatFlexMod.MOD_ID;

    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel NETWORK = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "flight_actions"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public static void init(IEventBus modEventBus) {
        modEventBus.addListener(FlightFeature::setup);
        MinecraftForge.EVENT_BUS.register(new FlightServerEvents());
    }

    private static void setup(final FMLCommonSetupEvent event) {
        FlightPackets.register(NETWORK);
    }

    public static boolean hasSlowFlightUnlocked(Player player) {
        return player != null
            && (player.isSpectator() || SkillData.hasSkill(player, SkillType.SLOW_FLIGHT));
    }

    public static boolean hasFastFlightUnlocked(Player player) {
        return player != null
            && (player.isSpectator()
                        || (SkillData.hasSkill(player, SkillType.SLOW_FLIGHT)
                                && SkillData.hasSkill(player, SkillType.FAST_FLIGHT)));
    }

    public static boolean canUseMode(Player player, int mode) {
        return switch (mode) {
            case 1 -> hasFastFlightUnlocked(player);
            case 2, 3, 99 -> hasSlowFlightUnlocked(player);
            default -> true;
        };
    }

    public static void syncFlightAccess(ServerPlayer player) {
        FlightServerEvents.syncFlightAccess(player);
    }
}