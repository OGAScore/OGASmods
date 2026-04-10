package com.OGAS.combatflex.flight;

import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = FlightFeature.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
final class FlightClientSetupEvents {
    static final KeyMapping FLY_KEY = new KeyMapping("key.cbtflex.flight_mode_menu", GLFW.GLFW_KEY_R, "key.categories.cbtflex");

    private FlightClientSetupEvents() {
    }

    @SubscribeEvent
    static void onKeyRegister(RegisterKeyMappingsEvent event) {
        event.register(FLY_KEY);
    }
}