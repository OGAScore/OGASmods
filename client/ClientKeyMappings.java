package com.OGAS.combatflex.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public final class ClientKeyMappings {
	public static final String CATEGORY = "key.categories.cbtflex";
	public static final KeyMapping OPEN_SKILL_TREE = new KeyMapping("key.cbtflex.open_skill_tree",
			InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_I, CATEGORY);

	private ClientKeyMappings() {
	}
}