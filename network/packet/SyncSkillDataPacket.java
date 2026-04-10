package com.OGAS.combatflex.network.packet;

import com.OGAS.combatflex.SkillData;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

public class SyncSkillDataPacket {
	private final CompoundTag data;

	public SyncSkillDataPacket(CompoundTag data) {
		this.data = data;
	}

	public static void encode(SyncSkillDataPacket packet, FriendlyByteBuf buffer) {
		buffer.writeNbt(packet.data);
	}

	public static SyncSkillDataPacket decode(FriendlyByteBuf buffer) {
		CompoundTag data = buffer.readNbt();
		return new SyncSkillDataPacket(data == null ? new CompoundTag() : data);
	}

	public static void handle(SyncSkillDataPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> {
			Minecraft minecraft = Minecraft.getInstance();
			Player player = minecraft.player;
			if (player != null) {
				SkillData.applySyncTag(player, packet.data);
			}
		});
		context.setPacketHandled(true);
	}
}