package com.OGAS.combatflex.grab;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

record InputActionPacket(InputAction action) {
	static void encode(InputActionPacket packet, FriendlyByteBuf buffer) {
		buffer.writeEnum(packet.action);
	}

	static InputActionPacket decode(FriendlyByteBuf buffer) {
		return new InputActionPacket(buffer.readEnum(InputAction.class));
	}

	static void handle(InputActionPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
		ServerPlayer player = contextSupplier.get().getSender();
		if (player == null) {
			return;
		}

		GrabAndPullFeature.handleInputAction(player, packet.action());
	}
}