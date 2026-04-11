package com.OGAS.combatflex.network.packet;

import com.OGAS.combatflex.SpecialSkillFeature;
import com.OGAS.combatflex.SpecialSkillType;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public class UseSpecialSkillPacket {
	private final String skillId;

	public UseSpecialSkillPacket(String skillId) {
		this.skillId = skillId;
	}

	public static void encode(UseSpecialSkillPacket packet, FriendlyByteBuf buffer) {
		buffer.writeUtf(packet.skillId);
	}

	public static UseSpecialSkillPacket decode(FriendlyByteBuf buffer) {
		return new UseSpecialSkillPacket(buffer.readUtf());
	}

	public static void handle(UseSpecialSkillPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> {
			ServerPlayer player = context.getSender();
			if (player == null) {
				return;
			}

			SpecialSkillType skillType = SpecialSkillType.fromId(packet.skillId);
			if (skillType != null) {
				SpecialSkillFeature.use(player, skillType);
			}
		});
		context.setPacketHandled(true);
	}
}