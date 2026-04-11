package com.OGAS.combatflex.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public final class ClientKeyMappings {
	public static final String CATEGORY = "key.categories.cbtflex";
	public static final KeyMapping OPEN_SKILL_TREE = new KeyMapping("key.cbtflex.open_skill_tree",
			InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_I, CATEGORY);
	public static final KeyMapping OPEN_SPECIAL_SKILLS = new KeyMapping("key.cbtflex.open_special_skills",
			InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_TAB, CATEGORY);

	private ClientKeyMappings() {
	}

	public static boolean isSpecialSkillsKeyHeld(Minecraft minecraft) {
		if (minecraft == null || minecraft.getWindow() == null) {
			return false;
		}

		InputConstants.Key key = OPEN_SPECIAL_SKILLS.getKey();
		long window = minecraft.getWindow().getWindow();
		return switch (key.getType()) {
			case KEYSYM -> InputConstants.isKeyDown(window, key.getValue());
			case MOUSE -> GLFW.glfwGetMouseButton(window, key.getValue()) == GLFW.GLFW_PRESS;
			default -> false;
		};
	}
}