package com.OGAS.combatflex.network.packet;

import com.OGAS.combatflex.CombatFlexMod;
import com.OGAS.combatflex.SkillData;
import com.OGAS.combatflex.SpecialSkillType;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.network.NetworkEvent;

public class SpendSpecialSkillPointPacket {
	private final String skillId;

	public SpendSpecialSkillPointPacket(String skillId) {
		this.skillId = skillId;
	}

	public static void encode(SpendSpecialSkillPointPacket packet, FriendlyByteBuf buffer) {
		buffer.writeUtf(packet.skillId);
	}

	public static SpendSpecialSkillPointPacket decode(FriendlyByteBuf buffer) {
		return new SpendSpecialSkillPointPacket(buffer.readUtf());
	}

	public static void handle(SpendSpecialSkillPointPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> {
			ServerPlayer player = context.getSender();
			if (player == null) {
				return;
			}

			SpecialSkillType skillType = SpecialSkillType.fromId(packet.skillId);
			if (skillType != null && SkillData.unlockSpecialSkill(player, skillType)) {
				player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_LEVELUP,
						SoundSource.PLAYERS, 0.45F, 1.5F);
				CombatFlexMod.syncPlayerData(player);
			}
		});
		context.setPacketHandled(true);
	}
}