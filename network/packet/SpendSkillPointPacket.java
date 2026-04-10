package com.OGAS.combatflex.network.packet;

import com.OGAS.combatflex.SkillData;
import com.OGAS.combatflex.SkillType;
import com.OGAS.combatflex.CombatFlexMod;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public class SpendSkillPointPacket {
	private final String skillId;

	public SpendSkillPointPacket(String skillId) {
		this.skillId = skillId;
	}

	public static void encode(SpendSkillPointPacket packet, FriendlyByteBuf buffer) {
		buffer.writeUtf(packet.skillId);
	}

	public static SpendSkillPointPacket decode(FriendlyByteBuf buffer) {
		return new SpendSkillPointPacket(buffer.readUtf());
	}

	public static void handle(SpendSkillPointPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> {
			ServerPlayer player = context.getSender();
			if (player == null) {
				return;
			}

			SkillType.fromId(packet.skillId).ifPresent(skillType -> {
				if (SkillData.unlockSkill(player, skillType)) {
					CombatFlexMod.syncPlayerData(player);
				}
			});
		});
		context.setPacketHandled(true);
	}
}