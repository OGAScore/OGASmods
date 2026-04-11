package com.OGAS.combatflex.network;

import com.OGAS.combatflex.CombatFlexMod;
import com.OGAS.combatflex.network.packet.SpendSkillPointPacket;
import com.OGAS.combatflex.network.packet.SpendSpecialSkillPointPacket;
import com.OGAS.combatflex.network.packet.SyncSkillDataPacket;
import com.OGAS.combatflex.network.packet.UseSpecialSkillPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class NetworkHandler {
	private static final String PROTOCOL_VERSION = "1";
	private static int packetId = 0;
	public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
			ResourceLocation.fromNamespaceAndPath(CombatFlexMod.MOD_ID, "skill_sync"),
			() -> PROTOCOL_VERSION,
			PROTOCOL_VERSION::equals,
			PROTOCOL_VERSION::equals);
	private static boolean registered;

	private NetworkHandler() {
	}

	public static void register() {
		if (registered) {
			return;
		}

		CHANNEL.registerMessage(packetId++, SpendSkillPointPacket.class, SpendSkillPointPacket::encode,
				SpendSkillPointPacket::decode, SpendSkillPointPacket::handle);
		CHANNEL.registerMessage(packetId++, SpendSpecialSkillPointPacket.class, SpendSpecialSkillPointPacket::encode,
				SpendSpecialSkillPointPacket::decode, SpendSpecialSkillPointPacket::handle);
		CHANNEL.registerMessage(packetId++, SyncSkillDataPacket.class, SyncSkillDataPacket::encode,
				SyncSkillDataPacket::decode, SyncSkillDataPacket::handle);
		CHANNEL.registerMessage(packetId++, UseSpecialSkillPacket.class, UseSpecialSkillPacket::encode,
				UseSpecialSkillPacket::decode, UseSpecialSkillPacket::handle);
		registered = true;
	}

	public static void sendToServer(Object message) {
		CHANNEL.sendToServer(message);
	}

	public static void sendToPlayer(ServerPlayer player, Object message) {
		CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
	}
}