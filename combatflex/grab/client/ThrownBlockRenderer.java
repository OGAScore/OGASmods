package com.OGAS.combatflex.grab;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;

public class ThrownBlockRenderer extends EntityRenderer<ThrownBlockEntity> {
	private final BlockRenderDispatcher blockRenderer;

	public ThrownBlockRenderer(EntityRendererProvider.Context context) {
		super(context);
		this.blockRenderer = context.getBlockRenderDispatcher();
		this.shadowRadius = 0.7F;
	}

	@Override
	public void render(ThrownBlockEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
		BlockState blockState = entity.getBlockState();
		if (blockState.getRenderShape() != RenderShape.MODEL) {
			return;
		}

		poseStack.pushPose();
		poseStack.translate(-0.5D, 0.0D, -0.5D);
		this.blockRenderer.renderSingleBlock(blockState, poseStack, buffer, packedLight, OverlayTexture.NO_OVERLAY);
		poseStack.popPose();
		super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
	}

	@Override
	public ResourceLocation getTextureLocation(ThrownBlockEntity entity) {
		return InventoryMenu.BLOCK_ATLAS;
	}
}